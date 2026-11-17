package cn.codelyy.store.api;

import cn.codelyy.store.api.ApiModels.PageView;
import cn.codelyy.store.api.CommerceModels.*;
import cn.codelyy.store.commerce.CommerceService;
import cn.codelyy.store.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class CommerceController {
    private final CommerceService commerce;
    private final CustomerService customers;

    public CommerceController(CommerceService commerce, CustomerService customers) {
        this.commerce = commerce;
        this.customers = customers;
    }

    @GetMapping("/customer/cart")
    public List<Item> cart(HttpServletRequest request) { return commerce.cart(customer(request)); }

    @PutMapping("/customer/cart/{id}")
    public Item quantity(@PathVariable Long id, @Valid @RequestBody Quantity body, HttpServletRequest request) {
        return commerce.setQuantity(customer(request), id, body.quantity());
    }

    @DeleteMapping("/customer/cart/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCart(@PathVariable Long id, HttpServletRequest request) { commerce.removeCart(customer(request), id); }

    @PostMapping("/customer/cart/favorites")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void move(@Valid @RequestBody Selection body, HttpServletRequest request) {
        commerce.moveToFavorites(customer(request), body.productIds());
    }

    @GetMapping("/customer/favorites")
    public List<Item> favorites(HttpServletRequest request) { return commerce.favorites(customer(request)); }

    @PutMapping("/customer/favorites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void favorite(@PathVariable Long id, HttpServletRequest request) { commerce.favorite(customer(request), id); }

    @DeleteMapping("/customer/favorites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(@PathVariable Long id, HttpServletRequest request) { commerce.removeFavorite(customer(request), id); }

    @PostMapping("/customer/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public Order checkout(@Valid @RequestBody Checkout body, HttpServletRequest request) {
        return commerce.checkout(customer(request), body);
    }

    @PostMapping("/customer/products/{id}/custom-orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Order customOrder(@PathVariable Long id, @Valid @RequestBody CustomCheckout body, HttpServletRequest request) {
        return commerce.customCheckout(customer(request), id, body);
    }

    @GetMapping("/customer/shop-orders")
    public PageView<Order> customerOrders(@RequestParam(defaultValue="0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size, HttpServletRequest request) {
        return commerce.orders(customer(request), page, size);
    }

    @PostMapping("/customer/shop-orders/{id}/cancel")
    public Order cancelCustomer(@PathVariable Long id, HttpServletRequest request) {
        return commerce.cancel(id, customer(request));
    }

    @GetMapping("/seller/shop-orders")
    public PageView<Order> sellerOrders(@RequestParam(defaultValue="0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return commerce.orders(null, page, size);
    }

    @PutMapping("/seller/shop-orders/{id}/status")
    public Order advance(@PathVariable Long id, @Valid @RequestBody Advance body) {
        return commerce.advance(id, body.status());
    }

    @PostMapping("/seller/shop-orders/{id}/cancel")
    public Order cancelSeller(@PathVariable Long id) { return commerce.cancel(id, null); }

    @PutMapping("/seller/products/{id}/customization")
    public CustomSettings configure(@PathVariable Long id, @Valid @RequestBody CustomSettings body) {
        return commerce.configure(id, body);
    }

    @GetMapping("/seller/products/{id}/customization")
    public CustomSettings sellerCustomization(@PathVariable Long id) { return commerce.sellerCustomization(id); }

    @GetMapping("/public/products/{id}/customization")
    public CustomSettings customization(@PathVariable Long id) { return commerce.customization(id); }

    private Long customer(HttpServletRequest request) { return customers.requireCustomer(request).getId(); }
}
