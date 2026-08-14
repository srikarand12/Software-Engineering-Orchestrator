package com.engineering.software_engineering_orchestrator.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UrlRequest {

    @NotBlank(message = "URL is required")
    @Pattern(
            regexp = "^(https?://).+",
            message = "URL must start with http:// or https://"
    )
    private String url;

    @Size(
            min = 3,
            max = 20,
            message = "Custom alias must be between 3 and 20 characters"
    )
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]*$",
            message = "Custom alias can contain only letters, numbers, hyphen, and underscore"
    )
    private String customAlias;

    private LocalDateTime expiresAt;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}