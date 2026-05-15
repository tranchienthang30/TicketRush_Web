package com.example.ticket.config;

import vn.payos.PayOS;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PayOSProperties.class)
public class PayOSConfig {
    @Bean
    PayOS payOS(PayOSProperties properties) {
        return new PayOS(
                properties.getClientId(),
                properties.getApiKey(),
                properties.getChecksumKey()
        );
    }
}
