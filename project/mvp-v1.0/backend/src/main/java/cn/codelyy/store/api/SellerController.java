package cn.codelyy.store.api;

import cn.codelyy.store.api.ApiModels.*;
import cn.codelyy.store.config.SellerAuthInterceptor;
import cn.codelyy.store.config.SellerProperties;
import cn.codelyy.store.service.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller")
public class SellerController {
    private final SellerProperties sellerProperties;
    private final StoreService store;
    private final Path uploadPath;

    public SellerController(SellerProperties sellerProperties, StoreService store,
                            @Value("${app.upload-directory}") String uploadDirectory) {
        this.sellerProperties = sellerProperties;
        this.store = store;
        this.uploadPath = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @PostMapping("/login")
    public Map<String, Boolean> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        if (!sellerProperties.username().equals(request.username()) || !sellerProperties.password().equals(request.password())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        HttpSession old = servletRequest.getSession(false);
        if (old != null) old.invalidate();
        servletRequest.getSession(true).setAttribute(SellerAuthInterceptor.SESSION_KEY, true);
        return Map.of("authenticated", true);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }

    @GetMapping("/session")
    public Map<String, Boolean> session(HttpServletRequest request) {
        var session = request.getSession(false);
        return Map.of("authenticated", session != null && Boolean.TRUE.equals(session.getAttribute(SellerAuthInterceptor.SESSION_KEY)));
    }

    @GetMapping("/workbench")
    public WorkbenchView workbench() { return store.workbench(); }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView createProduct(@Valid @RequestBody ProductRequest request) {
        return store.createProduct(request);
    }

    @PutMapping("/products/current")
    public ProductView updateProduct(@Valid @RequestBody ProductRequest request) {
        return store.updateCurrentProduct(request);
    }

    @PostMapping("/products/image")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw ApiException.badRequest("请选择一张图片");
        if (file.getSize() > 5L * 1024 * 1024) throw ApiException.badRequest("图片不能超过 5 MB");
        String contentType = file.getContentType();
        String extension;
        if ("image/jpeg".equals(contentType)) extension = ".jpg";
        else if ("image/png".equals(contentType)) extension = ".png";
        else throw ApiException.badRequest("只支持 JPG 或 PNG 图片");

        Files.createDirectories(uploadPath);
        String fileName = UUID.randomUUID() + extension;
        Path destination = uploadPath.resolve(fileName).normalize();
        if (!destination.startsWith(uploadPath)) throw ApiException.badRequest("图片文件名无效");
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return Map.of("url", "/uploads/" + fileName);
    }

    @PostMapping("/trades/start")
    public IntentView startTrade() { return store.startTrade(); }

    @PostMapping("/trades/{intentId}/result")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finishTrade(@PathVariable Long intentId, @Valid @RequestBody TradeResultRequest request) {
        store.finishTrade(intentId, request);
    }

    @PostMapping("/intents/{intentId}/action")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void processIntent(@PathVariable Long intentId, @Valid @RequestBody IntentActionRequest request) {
        store.processIntent(intentId, request.action());
    }

    @PostMapping("/products/current/delist")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delistProduct() { store.delistCurrentProduct(); }
}
