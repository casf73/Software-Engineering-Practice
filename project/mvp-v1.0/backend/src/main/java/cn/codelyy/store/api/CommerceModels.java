package cn.codelyy.store.api;

import cn.codelyy.store.domain.ProductStatus;
import cn.codelyy.store.commerce.ShopOrder.Status;
import com.fasterxml.jackson.annotation.JsonInclude;
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
                         @NotBlank @Size(max=12) String motif,
                         @NotBlank @Pattern(regexp="ink|rose|forest|#[0-9A-Fa-f]{6}") String color,
                         @JsonInclude(JsonInclude.Include.NON_NULL) String motifLabel,
                         @JsonInclude(JsonInclude.Include.NON_NULL) String colorLabel) {}
    public record CustomCheckout(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,64}") String requestId,
                                 @NotNull @Valid Design design) {}
    public record CustomSettings(boolean customizable, @Size(max=60) String artisanName,
                                 @Size(max=500) String artisanBio,
                                 @Valid CustomOptions options) {}
    public record CustomOptions(
            @NotEmpty @Size(max=3) List<@NotNull @Pattern(regexp="classic|modern|craft") String> templates,
            @NotEmpty @Size(max=12) List<@NotNull @Valid CustomChoice> motifs,
            @NotEmpty @Size(max=12) List<@NotNull @Valid CustomChoice> colors,
            boolean allowText, @Size(max=100) String textHint) {}
    public record CustomChoice(@NotBlank @Size(max=20) String label,
                               @NotBlank @Size(max=12) String value) {}
    public record Advance(@NotNull Status status) {}
    public record Item(Long productId, String name, BigDecimal price, int stock, ProductStatus status,
                       Integer quantity, String thumbnailUrl) {}
    public record OrderLine(Long productId, String name, BigDecimal unitPrice, int quantity,
                            Design design, String artisanName) {}
    public record Event(Status status, LocalDateTime time) {}
    public record Order(Long id, Status status, BigDecimal total, LocalDateTime createdAt, String customer,
                        List<OrderLine> items, List<Event> timeline) {}
}
