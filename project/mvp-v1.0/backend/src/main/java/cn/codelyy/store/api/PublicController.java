package cn.codelyy.store.api;

import cn.codelyy.store.api.ApiModels.*;
import cn.codelyy.store.service.StoreService;
import cn.codelyy.store.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    private final StoreService store;
    private final CustomerService customers;
    public PublicController(StoreService store, CustomerService customers) { this.store = store; this.customers = customers; }

    @GetMapping("/products/current")
    public ProductView currentProduct() { return store.currentProduct(); }

    @PostMapping("/intents")
    @ResponseStatus(HttpStatus.CREATED)
    public IssuedIntentView submitIntent(@Valid @RequestBody IntentRequest request, HttpServletRequest httpRequest) {
        var customer = customers.requireCustomer(httpRequest);
        return store.submitCustomerIntent(null,
                new CustomerIntentRequest(request.name(), request.phone(), customer.getDefaultLocation()), customer);
    }

    @GetMapping("/intents/{passcode}")
    public BuyerIntentView queryIntent(@PathVariable String passcode) {
        return store.queryIntent(passcode);
    }

    @PutMapping("/intents/{passcode}")
    public BuyerIntentView updateIntentContact(@PathVariable String passcode, @Valid @RequestBody IntentRequest request) {
        return store.updateIntentContact(passcode, request);
    }

    @DeleteMapping("/intents/{passcode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelIntent(@PathVariable String passcode) {
        store.cancelIntent(passcode);
    }
}
