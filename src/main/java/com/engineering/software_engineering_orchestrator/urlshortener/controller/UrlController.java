package com.engineering.software_engineering_orchestrator.urlshortener.controller;

import com.engineering.software_engineering_orchestrator.urlshortener.dto.UrlRequest;
import com.engineering.software_engineering_orchestrator.urlshortener.dto.UrlResponse;
import com.engineering.software_engineering_orchestrator.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/api/v1/urls")
    public ResponseEntity<UrlResponse> createShortUrl(
            @Valid @RequestBody UrlRequest request) {

        UrlResponse response = urlService.createShortUrl(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode) {

        String originalUrl = urlService.resolveShortUrl(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    @GetMapping("/api/v1/urls/{shortCode}")
    public ResponseEntity<UrlResponse> getUrlDetails(
            @PathVariable String shortCode) {

        return ResponseEntity.ok(
                urlService.getUrlDetails(shortCode)
        );
    }

    @DeleteMapping("/api/v1/urls/{shortCode}")
    public ResponseEntity<Void> deleteShortUrl(
            @PathVariable String shortCode) {

        urlService.deleteShortUrl(shortCode);

        return ResponseEntity.noContent().build();
    }
}