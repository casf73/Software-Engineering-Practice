package cn.codelyy.store.service;

import cn.codelyy.store.api.ApiException;
import cn.codelyy.store.api.ApiModels.*;
import cn.codelyy.store.domain.*;
import cn.codelyy.store.repository.IntentRepository;
import cn.codelyy.store.repository.ProductRepository;
import cn.codelyy.store.repository.CategoryRepository;
import cn.codelyy.store.repository.SellerAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class StoreService {
    private static final List<ProductStatus> LIVE_STATUSES = List.of(ProductStatus.ON_SALE, ProductStatus.MANUALLY_FROZEN, ProductStatus.IN_TRADE);
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final ProductRepository products;
    private final IntentRepository intents;
    private final CategoryRepository categories;
    private final SellerAccountRepository seller;
    private final SecureRandom random = new SecureRandom();

    public StoreService(ProductRepository products, IntentRepository intents, CategoryRepository categories, SellerAccountRepository seller) {
        this.products = products;
        this.intents = intents;
        this.categories=categories;this.seller=seller;
    }

    @Transactional(readOnly = true)
    public ProductView currentProduct() {
        return findLiveProduct().map(this::productView)
                .orElseThrow(() -> ApiException.notFound("当前没有在售商品"));
    }

    @Transactional
    public IssuedIntentView submitIntent(IntentRequest request) { return submitIntent(null,request); }

    @Transactional
    public IssuedIntentView submitIntent(Long productId, IntentRequest request) {
        Product product = lockProduct(productId);
        if (product.getStatus() != ProductStatus.ON_SALE) throw ApiException.conflict(product.getStatus() == ProductStatus.MANUALLY_FROZEN
                ? "商品已手动冻结，暂不接受新的购买意向" : "商品正在交易中，暂不接受新的购买意向");
        if(product.getStock()<=0)throw ApiException.conflict("商品库存不足");
        PurchaseIntent intent = new PurchaseIntent();
        intent.setProduct(product);
        intent.setName(request.name().trim());
        intent.setPhone(request.phone().trim());
        intent.setPasscode(newPasscode());
        intent.setStatus(IntentStatus.WAITING);
        intents.saveAndFlush(intent);
        return new IssuedIntentView(intent.getPasscode(), waitingIntents(product.getId()).size());
    }

    @Transactional(readOnly = true)
    public BuyerIntentView queryIntent(String passcode) {
        PurchaseIntent intent = intents.findByPasscode(normalizeCode(passcode))
                .orElseThrow(() -> ApiException.notFound("口令码无效或已失效"));
        Integer position = null;
        if (intent.getStatus() == IntentStatus.WAITING) {
            List<PurchaseIntent> waiting = waitingIntents(intent.getProduct().getId());
            position = indexOf(waiting, intent.getId()) + 1;
        }
        return new BuyerIntentView(intent.getName(), intent.getPhone(), intent.getStatus(), position, intent.getProduct().getStatus());
    }

    @Transactional
    public BuyerIntentView updateIntentContact(String passcode, IntentRequest request) {
        String code = normalizeCode(passcode);
        Long productId = intents.findProductIdByPasscode(code)
                .orElseThrow(() -> ApiException.notFound("口令码无效或已失效"));
        // 与开始交易使用相同的商品锁，锁定后重新读取意向，避免覆盖已进入交易的资料。
        products.findLockedById(productId).orElseThrow(() -> ApiException.notFound("商品不存在"));
        PurchaseIntent intent = intents.findByPasscode(code)
                .orElseThrow(() -> ApiException.notFound("口令码无效或已失效"));
        if (intent.getStatus() != IntentStatus.WAITING) {
            throw ApiException.conflict("只有仍在排队的买家可以修改联系信息");
        }
        intent.setName(request.name().trim());
        intent.setPhone(request.phone().trim());
        // 不修改 submittedAt、queueAt、passcode 和状态，原排位保持不变。
        return queryIntent(code);
    }

    @Transactional
    public ProductView freezeCurrentProduct() { return freezeProduct(null); }
    @Transactional public ProductView freezeProduct(Long productId) {
        Product product = lockProduct(productId);
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw ApiException.conflict("只有在售商品可以手动冻结");
        }
        product.setStatus(ProductStatus.MANUALLY_FROZEN);
        return productView(product);
    }

    @Transactional
    public ProductView unfreezeCurrentProduct() { return unfreezeProduct(null); }
    @Transactional public ProductView unfreezeProduct(Long productId) {
        Product product = lockProduct(productId);
        if (product.getStatus() != ProductStatus.MANUALLY_FROZEN) {
            throw ApiException.conflict("只有手动冻结的商品可以解冻；交易中请先登记交易结果");
        }
        product.setStatus(ProductStatus.ON_SALE);
        return productView(product);
    }

    @Transactional
    public void cancelIntent(String passcode) {
        Long productId = intents.findProductIdByPasscode(normalizeCode(passcode))
                .orElseThrow(() -> ApiException.notFound("口令码无效或已失效"));
        Product product = products.findLockedById(productId)
                .orElseThrow(() -> ApiException.notFound("商品不存在"));
        PurchaseIntent intent = intents.findByPasscode(normalizeCode(passcode))
                .orElseThrow(() -> ApiException.notFound("口令码无效或已失效"));
        if (intent.getStatus() != IntentStatus.WAITING) {
            throw ApiException.conflict("已进入交易的买家不能撤销；只有仍在排队的意向可以撤销");
        }
        intent.setStatus(IntentStatus.CANCELLED);
        intent.setPasscode(null);
    }

    @Transactional(readOnly = true)
    public WorkbenchView workbench() { return workbench(null); }
    @Transactional(readOnly=true) public WorkbenchView workbench(Long productId) {
        Product product = productId==null ? findLiveProduct().orElse(null) : products.findById(productId).orElseThrow(()->ApiException.notFound("商品不存在"));
        if (product == null) return new WorkbenchView(null, List.of(), 0, null);
        List<PurchaseIntent> waiting = waitingIntents(product.getId());
        Optional<PurchaseIntent> active = intents.findFirstByProductIdAndStatusOrderByQueueAtAscIdAsc(product.getId(), IntentStatus.IN_TRADE);
        List<IntentView> rows = new ArrayList<>();
        active.ifPresent(value -> rows.add(intentView(value, null)));
        for (int i = 0; i < waiting.size(); i++) rows.add(intentView(waiting.get(i), i + 1));
        return new WorkbenchView(productView(product), rows, waiting.size(), active.map(value -> intentView(value, null)).orElse(null));
    }

    @Transactional
    public ProductView createProduct(ProductRequest request) {
        seller.findLockedById(1L).orElseThrow();
        Product product = new Product();
        applyProductRequest(product, request);
        product.setStatus(product.getStock()>0?ProductStatus.ON_SALE:ProductStatus.DELISTED);
        product.setActiveSlot(null);
        return productView(products.save(product));
    }

    @Transactional
    public ProductView updateCurrentProduct(ProductRequest request) { return updateProduct(null,request); }
    @Transactional public ProductView updateProduct(Long productId,ProductRequest request) {
        seller.findLockedById(1L).orElseThrow();
        Product product = lockProduct(productId);
        if (product.getStatus() == ProductStatus.IN_TRADE) throw ApiException.conflict("交易进行中，暂时不能修改商品");
        applyProductRequest(product, request);
        if(product.getStock()==0)closeProduct(product,IntentStatus.PRODUCT_DELISTED);
        return productView(product);
    }

    @Transactional
    public void processIntent(Long intentId, String action) {
        Long productId = intents.findProductIdByIntentId(intentId).orElseThrow(() -> ApiException.notFound("购买意向不存在"));
        Product product = products.findLockedById(productId)
                .orElseThrow(() -> ApiException.notFound("商品不存在"));
        PurchaseIntent intent = intents.findById(intentId).orElseThrow(() -> ApiException.notFound("购买意向不存在"));
        if (intent.getStatus() != IntentStatus.WAITING) throw ApiException.conflict("只能处理仍在排队中的购买意向");
        if ("VOID".equals(action)) {
            intent.setStatus(IntentStatus.VOIDED);
            intent.setPasscode(null);
        } else if ("REQUEUE".equals(action)) {
            intent.setQueueAt(nextQueueTime(product.getId()));
        } else {
            throw ApiException.badRequest("不支持的意向处理方式");
        }
    }

    @Transactional
    public IntentView startTrade() { return startTrade(null); }
    @Transactional public IntentView startTrade(Long productId) {
        Product product = lockProduct(productId);
        if (product.getStatus() != ProductStatus.ON_SALE) throw ApiException.conflict("商品当前不在可开始交易的状态");
        if(product.getStock()<=0)throw ApiException.conflict("商品库存不足");
        PurchaseIntent first = intents.findFirstByProductIdAndStatusOrderByQueueAtAscIdAsc(product.getId(), IntentStatus.WAITING)
                .orElseThrow(() -> ApiException.conflict("当前没有等待处理的购买意向"));
        product.setStatus(ProductStatus.IN_TRADE);
        first.setStatus(IntentStatus.IN_TRADE);
        return intentView(first, null);
    }

    @Transactional
    public void finishTrade(Long intentId, TradeResultRequest request) {
        Long productId = intents.findProductIdByIntentId(intentId).orElseThrow(() -> ApiException.notFound("交易意向不存在"));
        Product product = products.findLockedById(productId)
                .orElseThrow(() -> ApiException.notFound("商品不存在"));
        if (product.getStatus() != ProductStatus.IN_TRADE) throw ApiException.conflict("商品当前没有进行中的交易");
        PurchaseIntent active = intents.findById(intentId).orElseThrow(() -> ApiException.notFound("交易意向不存在"));
        if (active.getStatus() != IntentStatus.IN_TRADE) throw ApiException.conflict("该买家并非当前交易对象");

        if (Boolean.TRUE.equals(request.success())) {
            active.setStatus(IntentStatus.TRADE_SUCCESS);
            active.setPasscode(null);
            if(product.getStock()<=0)throw ApiException.conflict("商品库存不足");
            product.setStock(product.getStock()-1);
            if(product.getStock()==0)closeProduct(product,IntentStatus.TRADE_FAILED);
            else product.setStatus(ProductStatus.ON_SALE);
            return;
        }
        if ("REQUEUE".equals(request.disposition())) {
            active.setStatus(IntentStatus.WAITING);
            active.setQueueAt(nextQueueTime(product.getId()));
        } else if ("VOID".equals(request.disposition())) {
            active.setStatus(IntentStatus.TRADE_FAILED);
            active.setPasscode(null);
        } else {
            throw ApiException.badRequest("交易失败时请选择作废意向或排到队尾");
        }
        product.setStatus(ProductStatus.ON_SALE);
    }

    @Transactional
    public void delistCurrentProduct() { delistProduct(null); }
    @Transactional public void delistProduct(Long productId) {
        Product product = lockProduct(productId);
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw ApiException.conflict("只有在售商品可以主动下架，请先解冻或登记交易结果");
        }
        for (PurchaseIntent waiting : waitingIntents(product.getId())) {
            waiting.setStatus(IntentStatus.PRODUCT_DELISTED);
            waiting.setPasscode(null);
        }
        product.setStatus(ProductStatus.DELISTED);
        product.setActiveSlot(null);
    }

    @Transactional(readOnly=true) public ProductView getProduct(Long id){
        Product p=products.findById(id).orElseThrow(()->ApiException.notFound("商品不存在"));
        if(p.getStatus()==ProductStatus.DELISTED)throw ApiException.notFound("商品已下架");
        return productView(p);
    }
    @Transactional(readOnly=true) public PageView<ProductView> catalog(String q,Long categoryId,int page,int size,boolean includeDelisted){
        String keyword=q==null?"":q.trim().toLowerCase(Locale.ROOT);
        var all=products.findAll().stream().filter(p->includeDelisted||LIVE_STATUSES.contains(p.getStatus()))
            .filter(p->keyword.isEmpty()||p.getName().toLowerCase(Locale.ROOT).contains(keyword)||p.getDescription()!=null&&p.getDescription().replaceAll("<[^>]*>"," ").toLowerCase(Locale.ROOT).contains(keyword))
            .filter(p->categoryId==null||p.getCategory()!=null&&(categoryId.equals(p.getCategory().getId())||p.getCategory().getParent()!=null&&categoryId.equals(p.getCategory().getParent().getId())))
            .sorted(Comparator.comparing(Product::getId).reversed()).toList();
        return new PageView<>(all.stream().skip((long)page*size).limit(size).map(this::productView).toList(),all.size(),page,size);
    }
    @Transactional public ProductView relistProduct(Long id){
        Product p=lockProduct(id);
        if(p.getStatus()!=ProductStatus.DELISTED||p.getStock()<=0)throw ApiException.conflict("仅有库存的下架商品可重新上架");
        p.setStatus(ProductStatus.ON_SALE);return productView(p);
    }
    private void closeProduct(Product p,IntentStatus status){
        for(var i:waitingIntents(p.getId())){i.setStatus(status);i.setPasscode(null);}
        p.setStatus(ProductStatus.DELISTED);p.setActiveSlot(null);
    }
    private Optional<Product> findLiveProduct() {
        var live=products.findByStatusInOrderByCreatedAtDesc(LIVE_STATUSES);
        if(live.size()>1)throw ApiException.conflict("当前有多个商品，请使用带商品编号的接口");
        return live.stream().findFirst();
    }

    private Product lockProduct(Long id){
        if(id==null)return lockCurrentProduct();
        return products.findLockedById(id).orElseThrow(()->ApiException.notFound("商品不存在"));
    }
    private Product lockCurrentProduct() {
        Product current = findLiveProduct().orElseThrow(() -> ApiException.notFound("当前没有在售商品"));
        return products.findLockedById(current.getId()).orElseThrow(() -> ApiException.notFound("当前商品不存在"));
    }

    private List<PurchaseIntent> waitingIntents(Long productId) {
        return intents.findByProductIdAndStatusOrderByQueueAtAscIdAsc(productId, IntentStatus.WAITING);
    }

    private LocalDateTime nextQueueTime(Long productId) {
        LocalDateTime now = LocalDateTime.now();
        List<PurchaseIntent> waiting = waitingIntents(productId);
        if (waiting.isEmpty()) return now;
        LocalDateTime afterTail = waiting.get(waiting.size() - 1).getQueueAt().plusNanos(1_000_000);
        return afterTail.isAfter(now) ? afterTail : now;
    }

    private void applyProductRequest(Product product, ProductRequest request) {
        if (request.price().compareTo(BigDecimal.ZERO) <= 0) throw ApiException.badRequest("商品价格必须大于 0");
        product.setName(request.name().trim());
        product.setPrice(request.price().setScale(2));
        String description=RichDescription.clean(request.description());
        if(description.length()>20000)throw ApiException.badRequest("格式化后的商品描述不能超过 20000 字符");
        product.setDescription(description);
        if(request.stock()!=null)product.setStock(request.stock());
        product.setCategory(request.categoryId()==null?null:categories.findById(request.categoryId()).orElseThrow(()->ApiException.notFound("分类不存在")));
        List<String> images = request.images() == null ? List.of() : request.images().stream()
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (images.size() > 9) throw ApiException.badRequest("商品图片最多上传 9 张");
        if (images.stream().anyMatch(image -> !image.matches("/uploads/[a-zA-Z0-9_-]+\\.(?:jpg|png)"))) {
            throw ApiException.badRequest("商品图片地址无效");
        }
        product.setImageUrls(String.join("|", images));
    }

    private ProductView productView(Product product) {
        return new ProductView(product.getId(), product.getName(), product.getPrice(), product.getDescription(),
                product.getImageUrls() == null || product.getImageUrls().isBlank()
                        ? List.of() : Arrays.stream(product.getImageUrls().split(Pattern.quote("|"))).toList(),
                product.getStatus(), product.getCreatedAt(), product.getStock(),product.getCategory()==null?null:product.getCategory().getId(),product.getCategory()==null?null:product.getCategory().getName(),product.getCategory()==null||product.getCategory().getParent()==null?null:product.getCategory().getParent().getId());
    }

    private IntentView intentView(PurchaseIntent intent, Integer position) {
        return new IntentView(intent.getId(), intent.getName(), intent.getPhone(), intent.getStatus(), position, intent.getQueueAt());
    }

    private int indexOf(List<PurchaseIntent> list, Long id) {
        for (int i = 0; i < list.size(); i++) if (Objects.equals(list.get(i).getId(), id)) return i;
        return -1;
    }

    private String newPasscode() {
        String code;
        do {
            StringBuilder value = new StringBuilder(10);
            for (int i = 0; i < 10; i++) value.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            code = value.toString();
        } while (intents.findByPasscode(code).isPresent());
        return code;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) throw ApiException.notFound("口令码无效或已失效");
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
