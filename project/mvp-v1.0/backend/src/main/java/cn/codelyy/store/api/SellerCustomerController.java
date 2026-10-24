package cn.codelyy.store.api;
import cn.codelyy.store.api.ApiModels.*;
import cn.codelyy.store.service.CustomerService;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/seller/customers") @Validated
public class SellerCustomerController {
    private final CustomerService customers;
    public SellerCustomerController(CustomerService customers){this.customers=customers;}
    @GetMapping
    public PageView<CustomerView> list(@RequestParam(defaultValue="") @Size(max=100) String q,
        @RequestParam(defaultValue="0") @Min(0) @Max(1000000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return customers.list(q,page,size);}
    @GetMapping("/{id}") public CustomerView detail(@PathVariable Long id){return customers.customer(id);}
    @GetMapping("/{id}/orders")
    public PageView<CustomerOrderView> orders(@PathVariable Long id,@RequestParam(defaultValue="0") @Min(0) @Max(1000000) int page,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return customers.orders(id,page,size,false);}
}
