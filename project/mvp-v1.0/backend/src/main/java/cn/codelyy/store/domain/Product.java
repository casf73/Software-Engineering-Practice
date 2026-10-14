package cn.codelyy.store.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = @Index(name = "idx_product_status_created", columnList = "status, created_at"))
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(length = 500)
    private String description;
    @Column(name = "image_urls", length = 3000)
    private String imageUrls;
    @Column(name = "active_slot", unique = true)
    private Boolean activeSlot;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ProductStatus status;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { price = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String value) { imageUrls = value; }
    public Boolean getActiveSlot() { return activeSlot; }
    public void setActiveSlot(Boolean value) { activeSlot = value; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus value) { status = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
