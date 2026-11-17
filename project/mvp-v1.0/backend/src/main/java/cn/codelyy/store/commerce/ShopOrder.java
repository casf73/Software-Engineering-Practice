package cn.codelyy.store.commerce;

import cn.codelyy.store.api.CommerceModels.Design;
import cn.codelyy.store.domain.Customer;
import cn.codelyy.store.domain.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="shop_orders", uniqueConstraints=@UniqueConstraint(columnNames={"customer_id","request_id"}))
public class ShopOrder {
    public enum Status {
        PLACED, CONFIRMED, PREPARED, SHIPPING, COMPLETED, CANCELLED;

        Status next() {
            return switch (this) {
                case PLACED -> CONFIRMED;
                case CONFIRMED -> PREPARED;
                case PREPARED -> SHIPPING;
                case SHIPPING -> COMPLETED;
                case COMPLETED, CANCELLED -> null;
            };
        }
    }

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="customer_id") Customer customer;
    @Column(name="request_id", nullable=false, length=64) String requestId;
    @Column(nullable=false, length=4096) String requestSignature;
    @Enumerated(EnumType.STRING) @Column(nullable=false) Status status;
    @Column(nullable=false, precision=24, scale=2) BigDecimal total = BigDecimal.ZERO;
    @Column(nullable=false) LocalDateTime createdAt;
    @ElementCollection @CollectionTable(name="shop_order_lines", joinColumns=@JoinColumn(name="order_id"))
    @OrderColumn(name="line_index") List<Line> items = new ArrayList<>();
    @ElementCollection @CollectionTable(name="shop_order_events", joinColumns=@JoinColumn(name="order_id"))
    @OrderColumn(name="event_index") List<Milestone> timeline = new ArrayList<>();

    protected ShopOrder() {}

    ShopOrder(Customer customer, String requestId, String requestSignature) {
        this.customer = customer;
        this.requestId = requestId;
        this.requestSignature = requestSignature;
        createdAt = LocalDateTime.now();
        changeStatus(Status.PLACED);
    }

    void changeStatus(Status next) {
        status = next;
        timeline.add(new Milestone(next, LocalDateTime.now()));
    }

    void addLine(Product product, int quantity, Design design) {
        items.add(new Line(product, quantity, design));
        total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
    }

    @Embeddable
    public static class Line {
        @Column(nullable=false) Long productId;
        @Column(nullable=false, length=80) String name;
        @Column(nullable=false, precision=12, scale=2) BigDecimal unitPrice;
        @Column(nullable=false) int quantity;
        @Column(length=20) String template;
        @Column(name="design_text", length=60) String text;
        @Column(length=20) String motif;
        @Column(length=20) String color;
        @Column(length=60) String artisanName;

        protected Line() {}

        Line(Product product, int quantity, Design design) {
            productId = product.getId();
            name = product.getName();
            unitPrice = product.getPrice();
            this.quantity = quantity;
            if (design != null) {
                template = design.template();
                text = design.text();
                motif = design.motif();
                color = design.color();
                artisanName = product.getArtisanName();
            }
        }

        Design design() {
            return template == null ? null : new Design(template, text, motif, color);
        }
    }

    @Embeddable
    public static class Milestone {
        @Enumerated(EnumType.STRING) @Column(name="event_status", nullable=false) Status status;
        @Column(name="event_time", nullable=false) LocalDateTime time;

        protected Milestone() {}
        Milestone(Status status, LocalDateTime time) { this.status = status; this.time = time; }
    }
}
