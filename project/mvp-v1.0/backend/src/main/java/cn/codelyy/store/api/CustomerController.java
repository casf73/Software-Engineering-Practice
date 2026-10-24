package cn.codelyy.store.api;
import cn.codelyy.store.api.ApiModels.*;
import cn.codelyy.store.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/customer") @Validated
public class CustomerController {
    private final CustomerService customers;private final StoreService store;
    public CustomerController(CustomerService customers,StoreService store){this.customers=customers;this.store=store;}
    private void establishSession(HttpServletRequest request,Long id){
        if(request.getSession(false)!=null)request.changeSessionId();
        request.getSession(true).setAttribute(CustomerService.SESSION_KEY,id);
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public CustomerSessionView register(@Valid @RequestBody CustomerRegisterRequest body,HttpServletRequest request){
        CustomerView customer=customers.register(body);establishSession(request,customer.id());return new CustomerSessionView(true,customer);
    }
    @PostMapping("/login")
    public CustomerSessionView login(@Valid @RequestBody CustomerLoginRequest body,HttpServletRequest request){
        CustomerView customer=customers.login(body);establishSession(request,customer.id());return new CustomerSessionView(true,customer);
    }
    @GetMapping("/session") public CustomerSessionView session(HttpServletRequest request){return customers.session(request);}
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request){var session=request.getSession(false);if(session!=null)session.removeAttribute(CustomerService.SESSION_KEY);}
    @GetMapping("/orders")
    public PageView<CustomerOrderView> orders(HttpServletRequest request,
        @RequestParam(defaultValue="0") @Min(0) @Max(1000000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){
        return customers.orders(customers.requireCustomer(request).getId(),page,size,true);
    }
    @PostMapping("/products/{id}/intents") @ResponseStatus(HttpStatus.CREATED)
    public IssuedIntentView submit(@PathVariable Long id,@Valid @RequestBody CustomerIntentRequest body,HttpServletRequest request){
        return store.submitCustomerIntent(id,body,customers.requireCustomer(request));
    }
}
