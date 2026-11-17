package cn.codelyy.store.commerce;

import cn.codelyy.store.domain.Customer;
import cn.codelyy.store.domain.Product;
import jakarta.persistence.*;

@Entity
@Table(name="shopping_entries", uniqueConstraints=@UniqueConstraint(columnNames={"customer_id","product_id","kind"}))
class ShoppingEntry {
    enum Kind { CART, FAVORITE }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="customer_id") Customer customer;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="product_id") Product product;
    @Enumerated(EnumType.STRING) @Column(nullable=false) Kind kind;
    Integer quantity;

    protected ShoppingEntry() {}
    ShoppingEntry(Customer customer, Product product, Kind kind, Integer quantity) {
        this.customer = customer;
        this.product = product;
        this.kind = kind;
        this.quantity = quantity;
    }
}
