package cn.codelyy.store.service;

import cn.codelyy.store.api.ApiException;
import cn.codelyy.store.api.ApiModels.*;
import cn.codelyy.store.domain.*;
import cn.codelyy.store.repository.IntentRepository;
import cn.codelyy.store.repository.ProductRepository;
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
    private static final List<ProductStatus> LIVE_STATUSES = List.of(ProductStatus.ON_SALE, ProductStatus.IN_TRADE);
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final ProductRepository products;
    private final IntentRepository intents;
    private final SecureRandom random = new SecureRandom();

    public StoreService(ProductRepository products, IntentRepository intents) {
        this.products = products;
        this.intents = intents;
    }

    @Transactional(readOnly = true)
    public ProductView currentProduct() {
        return products.findFirstByStatusInOrderByCreatedAtDesc(LIVE_STATUSES).map(this::productView)
                .orElseThrow(() -> ApiException.notFound("当前没有在售商品"));
    }

    @Transactional
    public IssuedIntentView submitIntent(IntentRequest request) {
        Product product = lockCurrentProduct();
        if (product.getStatus() != ProductStatus.ON_SALE) throw ApiException.conflict("商品正在交易中，暂不接受新的购买意向");
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
        return new BuyerIntentView(intent.getName(), intent.getStatus(), position, intent.getProduct().getStatus());
    }

    @Transactional
    public void cancelIntent(String passcode) {
        PurchaseIntent target = intents.findByPasscode(normalizeCode(passcode))
                .orElseThrow(() -> ApiException.notFound("口令码无效或已失效"));
        Product product = products.findLockedById(target.getProduct().getId())
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
    public WorkbenchView workbench() {
        Product product = findLiveProduct().orElse(null);
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
        if (findLiveProduct().isPresent()) throw ApiException.conflict("当前已有一件商品，请先下架后再发布");
        Product product = new Product();
        applyProductRequest(product, request);
        product.setStatus(ProductStatus.ON_SALE);
        product.setActiveSlot(true);
        return productView(products.save(product));
    }

    @Transactional
    public ProductView updateCurrentProduct(ProductRequest request) {
        Product product = lockCurrentProduct();
        if (product.getStatus() == ProductStatus.IN_TRADE) throw ApiException.conflict("交易进行中，暂时不能修改商品");
        applyProductRequest(product, request);
        return productView(product);
    }

    @Transactional
    public void processIntent(Long intentId, String action) {
        PurchaseIntent reference = intents.findById(intentId).orElseThrow(() -> ApiException.notFound("购买意向不存在"));
        Product product = products.findLockedById(reference.getProduct().getId())
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
    public IntentView startTrade() {
        Product product = lockCurrentProduct();
        if (product.getStatus() != ProductStatus.ON_SALE) throw ApiException.conflict("商品当前不在可开始交易的状态");
        PurchaseIntent first = intents.findFirstByProductIdAndStatusOrderByQueueAtAscIdAsc(product.getId(), IntentStatus.WAITING)
                .orElseThrow(() -> ApiException.conflict("当前没有等待处理的购买意向"));
        product.setStatus(ProductStatus.IN_TRADE);
        first.setStatus(IntentStatus.IN_TRADE);
        return intentView(first, null);
    }

    @Transactional
    public void finishTrade(Long intentId, TradeResultRequest request) {
        PurchaseIntent reference = intents.findById(intentId).orElseThrow(() -> ApiException.notFound("交易意向不存在"));
        Product product = products.findLockedById(reference.getProduct().getId())
                .orElseThrow(() -> ApiException.notFound("商品不存在"));
        if (product.getStatus() != ProductStatus.IN_TRADE) throw ApiException.conflict("商品当前没有进行中的交易");
        PurchaseIntent active = intents.findById(intentId).orElseThrow(() -> ApiException.notFound("交易意向不存在"));
        if (active.getStatus() != IntentStatus.IN_TRADE) throw ApiException.conflict("该买家并非当前交易对象");

        if (Boolean.TRUE.equals(request.success())) {
            active.setStatus(IntentStatus.TRADE_SUCCESS);
            active.setPasscode(null);
            for (PurchaseIntent waiting : waitingIntents(product.getId())) {
                waiting.setStatus(IntentStatus.TRADE_FAILED);
                waiting.setPasscode(null);
            }
            product.setStatus(ProductStatus.DELISTED);
            product.setActiveSlot(null);
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
    public void delistCurrentProduct() {
        Product product = lockCurrentProduct();
        if (product.getStatus() == ProductStatus.IN_TRADE) throw ApiException.conflict("交易进行中，不能下架商品");
        for (PurchaseIntent waiting : waitingIntents(product.getId())) {
            waiting.setStatus(IntentStatus.PRODUCT_DELISTED);
            waiting.setPasscode(null);
        }
        product.setStatus(ProductStatus.DELISTED);
        product.setActiveSlot(null);
    }

    private Optional<Product> findLiveProduct() {
        return products.findFirstByStatusInOrderByCreatedAtDesc(LIVE_STATUSES);
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
        product.setDescription(request.description() == null ? "" : request.description().trim());
        List<String> images = request.images() == null ? List.of() : request.images().stream()
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (images.size() > 1) throw ApiException.badRequest("商品图片最多上传 1 张");
        if (images.stream().anyMatch(image -> !image.startsWith("/uploads/"))) {
            throw ApiException.badRequest("商品图片地址无效");
        }
        product.setImageUrls(String.join("|", images));
    }

    private ProductView productView(Product product) {
        return new ProductView(product.getId(), product.getName(), product.getPrice(), product.getDescription(),
                product.getImageUrls() == null || product.getImageUrls().isBlank()
                        ? List.of() : Arrays.stream(product.getImageUrls().split(Pattern.quote("|"))).limit(1).toList(),
                product.getStatus(), product.getCreatedAt());
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
