package com.example.ticket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.seatsio")
public class SeatsioProperties {
    private String region = "eu";
    private String adminKey = "";
    private boolean sandbox = false;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAdminKey() {
        return adminKey;
    }

    public void setAdminKey(String adminKey) {
        this.adminKey = adminKey;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }

    public boolean isConfigured() {
        return adminKey != null && !adminKey.isBlank();
    }

    public String apiBaseUrl() {
        return "https://api-" + normalizedRegion() + ".seatsio.net";
    }

    public String cdnUrl() {
        return "https://cdn-" + normalizedRegion() + ".seatsio.net/chart.js";
    }

    private String normalizedRegion() {
        if (region == null || region.isBlank()) {
            return "eu";
        }
        return region.trim().toLowerCase();
    }
}
