package cn.codelyy.store.config;

import cn.codelyy.store.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Protect every HTTP entry point that creates a purchase intent. */
@Component
public class CustomerIntentAuthInterceptor implements HandlerInterceptor {
    private final CustomerService customers;

    public CustomerIntentAuthInterceptor(CustomerService customers) {
        this.customers = customers;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("POST".equalsIgnoreCase(request.getMethod())) customers.requireCustomer(request);
        return true;
    }
}
