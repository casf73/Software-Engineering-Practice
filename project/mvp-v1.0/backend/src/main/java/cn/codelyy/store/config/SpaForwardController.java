package cn.codelyy.store.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({"/seller/login", "/seller/dashboard"})
    public String forwardSellerPages() {
        return "forward:/index.html";
    }
}
