package cn.codelyy.store.api;
import cn.codelyy.store.api.ApiModels.*;
import cn.codelyy.store.service.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
@RestController @RequestMapping("/api") @Validated
public class CatalogController {
 private final StoreService store;private final CatalogService catalog;private final CustomerService customers;
 public CatalogController(StoreService s,CatalogService c,CustomerService customers){store=s;catalog=c;this.customers=customers;}
 @GetMapping("/public/products") public PageView<ProductView> browse(@RequestParam(defaultValue="") @Size(max=100) String q,@RequestParam(required=false) Long categoryId,
  @RequestParam(defaultValue="0") @Min(0) @Max(1000000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return store.catalog(q,categoryId,page,size,false);}
 @GetMapping("/seller/products") public PageView<ProductView> all(@RequestParam(defaultValue="") @Size(max=100) String q,@RequestParam(required=false) Long categoryId,
  @RequestParam(defaultValue="0") @Min(0) @Max(1000000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return store.catalog(q,categoryId,page,size,true);}
 @GetMapping("/public/products/{id}") public ProductView product(@PathVariable Long id){return store.getProduct(id);}
 @PostMapping("/public/products/{id}/intents") @ResponseStatus(HttpStatus.CREATED)
 public IssuedIntentView submit(@PathVariable Long id,@Valid @RequestBody IntentRequest r,HttpServletRequest request){
  var customer=customers.requireCustomer(request);
  return store.submitCustomerIntent(id,new CustomerIntentRequest(r.name(),r.phone(),customer.getDefaultLocation()),customer);
 }
 @GetMapping("/public/categories") public List<CategoryView> categories(){return catalog.list();}
 @PostMapping("/seller/categories") @ResponseStatus(HttpStatus.CREATED) public CategoryView createCategory(@Valid @RequestBody CategoryRequest r){return catalog.save(null,r);}
 @PutMapping("/seller/categories/{id}") public CategoryView updateCategory(@PathVariable Long id,@Valid @RequestBody CategoryRequest r){return catalog.save(id,r);}
 @DeleteMapping("/seller/categories/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteCategory(@PathVariable Long id){catalog.delete(id);}
 @GetMapping("/seller/products/{id}/workbench") public WorkbenchView workbench(@PathVariable Long id){return store.workbench(id);}
 @PutMapping("/seller/products/{id}") public ProductView update(@PathVariable Long id,@Valid @RequestBody ProductRequest r){return store.updateProduct(id,r);}
 @PostMapping("/seller/products/{id}/freeze") public ProductView freeze(@PathVariable Long id){return store.freezeProduct(id);}
 @PostMapping("/seller/products/{id}/unfreeze") public ProductView unfreeze(@PathVariable Long id){return store.unfreezeProduct(id);}
 @PostMapping("/seller/products/{id}/relist") public ProductView relist(@PathVariable Long id){return store.relistProduct(id);}
 @PostMapping("/seller/products/{id}/delist") @ResponseStatus(HttpStatus.NO_CONTENT) public void delist(@PathVariable Long id){store.delistProduct(id);}
 @PostMapping("/seller/products/{id}/trades/start") public IntentView start(@PathVariable Long id){return store.startTrade(id);}
}
