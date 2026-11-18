package cn.codelyy.store.api;

import cn.codelyy.store.domain.IntentStatus;
import cn.codelyy.store.domain.ProductStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiModels {
    private ApiModels() {}
    public record CustomerRegisterRequest(
        @NotBlank @Pattern(regexp="^[\\p{L}\\p{N}_-]{3,32}$",message="用户名须为3至32位文字、数字、下划线或连字符") String username,
        @NotBlank @Size(min=8,max=128) String password,
        @NotBlank @Pattern(regexp="^(?=.*[0-9])[0-9+() -]{6,30}$",message="请输入有效联系电话") String phone,
        @NotBlank @Size(max=200) String defaultLocation) {}
    public record CustomerLoginRequest(@NotBlank @Size(max=32) String username,@NotBlank @Size(max=128) String password) {}
    public record CustomerIntentRequest(@NotBlank @Size(max=80) String name,
        @NotBlank @Pattern(regexp="^(?=.*[0-9])[0-9+() -]{6,30}$",message="请输入有效联系电话") String phone,
        @NotBlank @Size(max=200) String meetingLocation) {}
    public record CustomerView(Long id,String username,String phone,String defaultLocation,LocalDateTime createdAt) {}
    public record CustomerSessionView(boolean authenticated,CustomerView customer) {}
    public record CustomerOrderView(Long id,Long productId,String productName,BigDecimal price,String name,String phone,
        String meetingLocation,IntentStatus status,LocalDateTime submittedAt,Integer position,String passcode,ProductStatus productStatus) {}
    public record CategoryRequest(@NotBlank @Size(max=80) String name, Long parentId) {}
    public record CategoryView(Long id, String name, Long parentId) {}
    public record PageView<T>(List<T> items, long total, int page, int size) {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record IntentRequest(@NotBlank @Size(max = 80) String name, @NotBlank @Pattern(regexp = "^(?=.*[0-9])[0-9+() -]{6,30}$", message = "请输入有效联系电话") String phone) {}
    public record ProductRequest(@NotBlank @Size(max = 80) String name, @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price, @Size(max = 20000) String description, @Size(max = 9, message = "商品图片最多上传 9 张") List<@NotBlank @Size(max = 500) String> images, @Min(0) @Max(1000000) Integer stock, Long categoryId) {}
    public record ChangePasswordRequest(
            @NotBlank @Size(max = 128) String currentPassword,
            @NotBlank @Size(min = 8, max = 128, message = "新密码须为 8 至 128 位") String newPassword) {}
    public record IntentActionRequest(@NotBlank String action) {}
    public record TradeResultRequest(@NotNull Boolean success, String disposition) {}
    public record ProductView(Long id, String name, BigDecimal price, String description, List<String> images, ProductStatus status, LocalDateTime createdAt, int stock, Long categoryId, String categoryName, Long parentCategoryId) {}
    public record IntentView(Long id, String name, String phone, IntentStatus status, Integer position, LocalDateTime queueAt, String meetingLocation, String customerUsername) {}
    public record BuyerIntentView(String name, String phone, IntentStatus status, Integer position, ProductStatus productStatus) {}
    public record IssuedIntentView(String passcode, Integer position) {}
    public record WorkbenchView(ProductView product, List<IntentView> intents, int waitingCount, IntentView activeIntent) {}
}
