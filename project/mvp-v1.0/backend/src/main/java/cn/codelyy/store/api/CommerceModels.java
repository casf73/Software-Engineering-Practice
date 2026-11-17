package cn.codelyy.store.api;

import cn.codelyy.store.domain.ProductStatus;
import cn.codelyy.store.commerce.ShopOrder.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class CommerceModels {
    private CommerceModels() {}

    public record Quantity(@NotNull @Positive Integer quantity) {}
    public record Selection(@NotEmpty List<@NotNull @Positive Long> productIds) {}
    public record Checkout(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,64}") String requestId,
                           @NotEmpty List<@NotNull @Positive Long> productIds) {}
    public record Design(@NotBlank @Pattern(regexp="classic|modern|craft") String template,
                         @NotNull @Size(max=60) String text,
                         @NotBlank @Pattern(regexp="none|heart|star|leaf") String motif,
                         @NotBlank @Pattern(regexp="ink|rose|forest") String color) {}
    public record CustomCheckout(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,64}") String requestId,
                                 @NotNull @Valid Design design) {}
    public record CustomSettings(boolean customizable, @Size(max=60) String artisanName,
                                 @Size(max=500) String artisanBio) {}
    public record Advance(@NotNull Status status) {}
    public record Item(Long productId, String name, BigDecimal price, int stock, ProductStatus status,
                       Integer quantity) {}
    public record OrderLine(Long productId, String name, BigDecimal unitPrice, int quantity,
                            Design design, String artisanName) {}
    public record Event(Status status, LocalDateTime time) {}
    public record Order(Long id, Status status, BigDecimal total, LocalDateTime createdAt, String customer,
                        List<OrderLine> items, List<Event> timeline) {}
}
