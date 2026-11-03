package cn.codelyy.store.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final SellerAuthInterceptor authInterceptor;
    private final CustomerIntentAuthInterceptor customerIntentAuthInterceptor;
    private final String allowedOrigin;
    private final String uploadDirectory;

    public WebConfig(SellerAuthInterceptor authInterceptor,
                     CustomerIntentAuthInterceptor customerIntentAuthInterceptor,
                     @Value("${app.allowed-origin}") String allowedOrigin,
                     @Value("${app.upload-directory}") String uploadDirectory) {
        this.authInterceptor = authInterceptor;
        this.customerIntentAuthInterceptor = customerIntentAuthInterceptor;
        this.allowedOrigin = allowedOrigin;
        this.uploadDirectory = uploadDirectory;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(customerIntentAuthInterceptor).addPathPatterns(
                "/api/public/intents", "/api/public/products/*/intents", "/api/customer/products/*/intents");
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/seller/**")
                .excludePathPatterns("/api/seller/login", "/api/seller/session");
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true).allowedHeaders("*");
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDirectory).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
