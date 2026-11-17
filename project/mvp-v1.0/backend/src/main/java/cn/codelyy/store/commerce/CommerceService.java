package cn.codelyy.store.commerce;

import cn.codelyy.store.api.ApiException;
import cn.codelyy.store.api.ApiModels.PageView;
import cn.codelyy.store.api.CommerceModels.*;
import cn.codelyy.store.commerce.ShopOrder.Status;
import cn.codelyy.store.commerce.ShoppingEntry.Kind;
import cn.codelyy.store.domain.*;
import cn.codelyy.store.repository.IntentRepository;
import cn.codelyy.store.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class CommerceService {
    private final EntityManager db;
    private final ProductRepository products;
    private final IntentRepository intents;

    public CommerceService(EntityManager db, ProductRepository products, IntentRepository intents) {
        this.db = db;
        this.products = products;
        this.intents = intents;
    }

    @Transactional(readOnly=true)
    public List<Item> cart(Long customerId) { return listEntries(customerId, Kind.CART); }

    @Transactional(readOnly=true)
    public List<Item> favorites(Long customerId) { return listEntries(customerId, Kind.FAVORITE); }

    public Item setQuantity(Long customerId, Long productId, int quantity) {
        Customer customer = lockCustomer(customerId);
        Product product = product(productId);
        var entry = entry(customerId, productId, Kind.CART);
        if (entry.isPresent()) {
            entry.get().quantity = quantity;
            return view(entry.get());
        }
        if (product.getStatus() != ProductStatus.ON_SALE) throw ApiException.conflict("只有在售商品可加入购物车");
        ShoppingEntry created = new ShoppingEntry(customer, product, Kind.CART, quantity);
        db.persist(created);
        return view(created);
    }

    public void removeCart(Long customerId, Long productId) {
        lockCustomer(customerId);
        entry(customerId, productId, Kind.CART).ifPresent(db::remove);
    }

    public void favorite(Long customerId, Long productId) {
        Customer customer = lockCustomer(customerId);
        addFavorite(customer, product(productId));
    }

    public void removeFavorite(Long customerId, Long productId) {
        lockCustomer(customerId);
        entry(customerId, productId, Kind.FAVORITE).ifPresent(db::remove);
    }

    public void moveToFavorites(Long customerId, List<Long> ids) {
        Customer customer = lockCustomer(customerId);
        List<ShoppingEntry> selected = selectedCart(customerId, uniqueIds(ids));
        for (ShoppingEntry row : selected) {
            addFavorite(customer, row.product);
            db.remove(row);
        }
    }

    public Order checkout(Long customerId, Checkout request) {
        Customer customer = lockCustomer(customerId);
        List<Long> ids = uniqueIds(request.productIds());
        String signature = "cart:" + ids;
        Optional<ShopOrder> existing = replay(customerId, request.requestId(), signature);
        if (existing.isPresent()) return view(existing.get());

        List<ShoppingEntry> selected = selectedCart(customerId, ids);
        var order = new ShopOrder(customer, request.requestId(), signature);
        for (ShoppingEntry row : selected) {
            Product product = lockProduct(row.product.getId());
            reserve(product, row.quantity);
            order.addLine(product, row.quantity, null);
            db.remove(row);
        }
        db.persist(order);
        return view(order);
    }

    public Order customCheckout(Long customerId, Long productId, CustomCheckout request) {
        Customer customer = lockCustomer(customerId);
        Design design = request.design();
        if (design.text().isBlank() && design.motif().equals("none"))
            throw ApiException.badRequest("请至少添加文字或图案");
        String signature = String.join(":", "custom", productId.toString(), design.template(),
                design.color(), design.motif(), design.text());
        Optional<ShopOrder> existing = replay(customerId, request.requestId(), signature);
        if (existing.isPresent()) return view(existing.get());

        Product product = lockProduct(productId);
        if (!product.isCustomizable()) throw ApiException.conflict("此商品未开放定制");
        reserve(product, 1);
        ShopOrder order = new ShopOrder(customer, request.requestId(), signature);
        order.addLine(product, 1, design);
        db.persist(order);
        return view(order);
    }

    @Transactional(readOnly=true)
    public PageView<Order> orders(Long customerId, int page, int size) {
        String filter = customerId == null ? "" : " where o.customer.id = :id";
        var query = db.createQuery("select o from ShopOrder o" + filter + " order by o.id desc", ShopOrder.class);
        var count = db.createQuery("select count(o) from ShopOrder o" + filter, Long.class);
        if (customerId != null) {
            query.setParameter("id", customerId);
            count.setParameter("id", customerId);
        }
        return new PageView<>(query.setFirstResult(page * size).setMaxResults(size)
                .getResultList().stream().map(this::view).toList(), count.getSingleResult(), page, size);
    }

    public Order advance(Long id, Status next) {
        ShopOrder order = lockOrder(id);
        if (order.status.next() != next) throw ApiException.conflict("订单只能按流程推进一步");
        order.changeStatus(next);
        return view(order);
    }

    public Order cancel(Long id, Long customerId) {
        ShopOrder order = lockOrder(id);
        if (customerId != null && !customerId.equals(order.customer.getId())) throw ApiException.notFound("订单不存在");
        if (order.status == Status.CANCELLED) return view(order);
        if (order.status == Status.COMPLETED || (customerId != null && order.status == Status.SHIPPING))
            throw ApiException.conflict("订单当前不可取消");
        for (var line : order.items.stream().sorted(Comparator.comparing(x -> x.productId)).toList()) {
            Product product = lockProduct(line.productId);
            product.setStock(Math.addExact(product.getStock(), line.quantity));
            if (product.isOrderSoldOut()) {
                product.setStatus(ProductStatus.ON_SALE);
                product.setOrderSoldOut(false);
            }
        }
        order.changeStatus(Status.CANCELLED);
        return view(order);
    }

    public CustomSettings configure(Long id, CustomSettings settings) {
        Product product = lockProduct(id);
        product.setCustomizable(settings.customizable());
        product.setArtisanName(clean(settings.artisanName()));
        product.setArtisanBio(clean(settings.artisanBio()));
        if (product.getArtisanName().isEmpty() && !product.getArtisanBio().isEmpty())
            throw ApiException.badRequest("填写介绍时请填写匠人名称");
        return settings(product);
    }

    @Transactional(readOnly=true)
    public CustomSettings customization(Long id) {
        Product product = product(id);
        if (product.getStatus() == ProductStatus.DELISTED) throw ApiException.notFound("商品已下架");
        return settings(product);
    }

    @Transactional(readOnly=true)
    public CustomSettings sellerCustomization(Long id) { return settings(product(id)); }

    private void reserve(Product product, int quantity) {
        if (product.getStatus() != ProductStatus.ON_SALE || product.getStock() < quantity)
            throw ApiException.conflict("商品库存不足或不可购买：" + product.getName());
        product.setStock(product.getStock() - quantity);
        if (product.getStock() == 0) {
            product.setStatus(ProductStatus.DELISTED);
            product.setOrderSoldOut(true);
            product.setActiveSlot(null);
            for (var intent : intents.findByProductIdAndStatusOrderByQueueAtAscIdAsc(product.getId(), IntentStatus.WAITING)) {
                intent.setStatus(IntentStatus.TRADE_FAILED);
                intent.setPasscode(null);
            }
        }
    }

    private List<Long> uniqueIds(List<Long> ids) {
        // Prevent oversized SQL/request signatures without imposing a cart quantity limit.
        var sorted = ids.stream().distinct().sorted().toList();
        if (ids.size() != sorted.size() || sorted.size() > 100)
            throw ApiException.badRequest("请选择不重复的商品，单次最多100种");
        return sorted;
    }

    private List<ShoppingEntry> selectedCart(Long customerId, List<Long> ids) {
        return ids.stream().map(id -> entry(customerId, id, Kind.CART)
                .orElseThrow(() -> ApiException.conflict("所选商品已不在购物车"))).toList();
    }

    private Optional<ShopOrder> replay(Long customerId, String requestId, String signature) {
        var existing = db.createQuery("select o from ShopOrder o where o.customer.id = :id and o.requestId = :key", ShopOrder.class)
                .setParameter("id", customerId).setParameter("key", requestId).getResultStream().findFirst();
        if (existing.isPresent() && !existing.get().requestSignature.equals(signature))
            throw ApiException.conflict("提交编号已用于另一笔订单");
        return existing;
    }

    private Customer lockCustomer(Long id) {
        Customer customer = db.find(Customer.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (customer == null) throw ApiException.notFound("客户不存在");
        return customer;
    }

    private ShopOrder lockOrder(Long id) {
        ShopOrder order = db.find(ShopOrder.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (order == null) throw ApiException.notFound("订单不存在");
        return order;
    }

    private Product lockProduct(Long id) {
        // Cart queries may already have loaded a reference. Refresh under the same lock used by the intent flow.
        Product product = products.findLockedById(id).orElseThrow(() -> ApiException.notFound("商品不存在"));
        db.refresh(product, LockModeType.PESSIMISTIC_WRITE);
        return product;
    }

    private Product product(Long id) {
        return products.findById(id).orElseThrow(() -> ApiException.notFound("商品不存在"));
    }

    private Optional<ShoppingEntry> entry(Long customerId, Long productId, Kind kind) {
        return db.createQuery("select e from ShoppingEntry e where e.customer.id = :id and e.product.id = :product and e.kind = :kind", ShoppingEntry.class)
                .setParameter("id", customerId).setParameter("product", productId).setParameter("kind", kind)
                .getResultStream().findFirst();
    }

    private List<Item> listEntries(Long customerId, Kind kind) {
        return db.createQuery("select e from ShoppingEntry e join fetch e.product where e.customer.id = :id and e.kind = :kind order by e.id desc", ShoppingEntry.class)
                .setParameter("id", customerId).setParameter("kind", kind).getResultList().stream().map(this::view).toList();
    }

    private void addFavorite(Customer customer, Product product) {
        if (entry(customer.getId(), product.getId(), Kind.FAVORITE).isEmpty())
            db.persist(new ShoppingEntry(customer, product, Kind.FAVORITE, null));
    }

    private Item view(ShoppingEntry row) {
        Product product = row.product;
        return new Item(product.getId(), product.getName(), product.getPrice(), product.getStock(), product.getStatus(), row.quantity);
    }

    private Order view(ShopOrder order) {
        return new Order(order.id, order.status, order.total, order.createdAt, order.customer.getUsername(),
                order.items.stream().map(line -> new OrderLine(line.productId, line.name, line.unitPrice,
                        line.quantity, line.design(), line.artisanName)).toList(),
                order.timeline.stream().map(event -> new Event(event.status, event.time)).toList());
    }

    private CustomSettings settings(Product product) {
        return new CustomSettings(product.isCustomizable(), clean(product.getArtisanName()), clean(product.getArtisanBio()));
    }

    private String clean(String value) { return value == null ? "" : value.strip(); }
}
