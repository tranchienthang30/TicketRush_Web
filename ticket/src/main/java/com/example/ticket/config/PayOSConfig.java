package com.example.ticket.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PayOSProperties.class)
public class PayOSConfig {
    @Bean
    vn.payos.PayOS payOS(PayOSProperties properties) {
        return new vn.payos.PayOS(
                properties.getClientId(),
                properties.getApiKey(),
                properties.getChecksumKey()
        );
    }
}
