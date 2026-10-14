package cn.codelyy.store.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seller")
public record SellerProperties(String username, String password) {}
