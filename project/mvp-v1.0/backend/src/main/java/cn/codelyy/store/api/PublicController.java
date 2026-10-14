package cn.codelyy.store.api;

import cn.codelyy.store.api.ApiModels.*;
import cn.codelyy.store.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    private final StoreService store;
    public PublicController(StoreService store) { this.store = store; }

    @GetMapping("/products/current")
    public ProductView currentProduct() { return store.currentProduct(); }

    @PostMapping("/intents")
    @ResponseStatus(HttpStatus.CREATED)
    public IssuedIntentView submitIntent(@Valid @RequestBody IntentRequest request) {
        return store.submitIntent(request);
    }

    @GetMapping("/intents/{passcode}")
    public BuyerIntentView queryIntent(@PathVariable String passcode) {
        return store.queryIntent(passcode);
    }

    @DeleteMapping("/intents/{passcode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelIntent(@PathVariable String passcode) {
        store.cancelIntent(passcode);
    }
}
