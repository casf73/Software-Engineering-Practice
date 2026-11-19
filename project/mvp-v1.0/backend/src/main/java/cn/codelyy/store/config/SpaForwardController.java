package cn.codelyy.store.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({"/customization", "/seller/login", "/seller/dashboard", "/customer/login", "/customer/register", "/customer/account", "/customer/cart", "/customer/favorites", "/customer/shop-orders", "/seller/shop-orders"})
    public String forwardSellerPages() {
        return "forward:/index.html";
    }
}
