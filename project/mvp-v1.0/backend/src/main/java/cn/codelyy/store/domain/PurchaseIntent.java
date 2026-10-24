package cn.codelyy.store.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_intents", indexes = {
        @Index(name = "idx_intent_product_queue", columnList = "product_id, status, queue_at"),
        @Index(name = "idx_intent_passcode", columnList = "passcode", unique = true),
        @Index(name = "idx_intent_customer_submitted", columnList = "customer_id, submitted_at, id")
})
public class PurchaseIntent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="customer_id")
    private Customer customer;
    @Column(name="product_name_snapshot",length=80) private String productNameSnapshot;
    @Column(name="price_snapshot",precision=12,scale=2) private java.math.BigDecimal priceSnapshot;
    @Column(name="meeting_location",length=200) private String meetingLocation;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(nullable = false, length = 30)
    private String phone;
    @Column(length = 24, unique = true)
    private String passcode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private IntentStatus status;
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    @Column(name = "queue_at", nullable = false)
    private LocalDateTime queueAt;

    @PrePersist void onCreate() { submittedAt = LocalDateTime.now(); if (queueAt == null) queueAt = submittedAt; }
    public Customer getCustomer(){return customer;} public void setCustomer(Customer v){customer=v;}
    public String getProductNameSnapshot(){return productNameSnapshot;} public void setProductNameSnapshot(String v){productNameSnapshot=v;}
    public java.math.BigDecimal getPriceSnapshot(){return priceSnapshot;} public void setPriceSnapshot(java.math.BigDecimal v){priceSnapshot=v;}
    public String getMeetingLocation(){return meetingLocation;} public void setMeetingLocation(String v){meetingLocation=v;}
    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product value) { product = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public String getPasscode() { return passcode; }
    public void setPasscode(String value) { passcode = value; }
    public IntentStatus getStatus() { return status; }
    public void setStatus(IntentStatus value) { status = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getQueueAt() { return queueAt; }
    public void setQueueAt(LocalDateTime value) { queueAt = value; }
}
