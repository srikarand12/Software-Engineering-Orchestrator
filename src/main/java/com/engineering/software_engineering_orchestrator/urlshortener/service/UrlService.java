package com.engineering.software_engineering_orchestrator.urlshortener.service;

import com.engineering.software_engineering_orchestrator.urlshortener.domain.UrlEntity;
import com.engineering.software_engineering_orchestrator.urlshortener.dto.UrlRequest;
import com.engineering.software_engineering_orchestrator.urlshortener.dto.UrlResponse;
import com.engineering.software_engineering_orchestrator.urlshortener.exception.DuplicateAliasException;
import com.engineering.software_engineering_orchestrator.urlshortener.exception.ExpiredUrlException;
import com.engineering.software_engineering_orchestrator.urlshortener.exception.InactiveUrlException;
import com.engineering.software_engineering_orchestrator.urlshortener.exception.UrlNotFoundException;
import com.engineering.software_engineering_orchestrator.urlshortener.repository.UrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class UrlService {

    private static final String CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int SHORT_CODE_LENGTH = 7;

    private final UrlRepository urlRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @Transactional
    public UrlResponse createShortUrl(UrlRequest request) {

        if (request.getExpiresAt() != null &&
                request.getExpiresAt().isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Expiration date must be in the future"
            );
        }

        String shortCode;

        if (request.getCustomAlias() != null &&
                !request.getCustomAlias().isBlank()) {

            shortCode = request.getCustomAlias();

            if (urlRepository.existsByShortCode(shortCode)) {
                throw new DuplicateAliasException(
                        "Custom alias already exists"
                );
            }

        } else {
            shortCode = generateUniqueShortCode();
        }

        UrlEntity entity = UrlEntity.builder()
                .originalUrl(request.getUrl())
                .shortCode(shortCode)
                .expiresAt(request.getExpiresAt())
                .createdAt(LocalDateTime.now())
                .clickCount(0L)
                .active(true)
                .build();

        UrlEntity saved = urlRepository.save(entity);

        return toResponse(saved);
    }

    @Transactional
    public String resolveShortUrl(String shortCode) {

        UrlEntity entity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new UrlNotFoundException("Short URL not found"));

        if (!Boolean.TRUE.equals(entity.getActive())) {
            throw new InactiveUrlException("Short URL is inactive");
        }

        if (entity.getExpiresAt() != null &&
                entity.getExpiresAt().isBefore(LocalDateTime.now())) {

            throw new ExpiredUrlException("Short URL has expired");
        }

        entity.setClickCount(entity.getClickCount() + 1);

        urlRepository.save(entity);

        return entity.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlResponse getUrlDetails(String shortCode) {

        UrlEntity entity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new UrlNotFoundException("Short URL not found"));

        return toResponse(entity);
    }

    @Transactional
    public void deleteShortUrl(String shortCode) {

        UrlEntity entity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new UrlNotFoundException("Short URL not found"));

        entity.setActive(false);

        urlRepository.save(entity);
    }

    private String generateUniqueShortCode() {

        String code;

        do {
            code = generateShortCode();
        } while (urlRepository.existsByShortCode(code));

        return code;
    }

    private String generateShortCode() {

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(CHARACTERS.length());
            builder.append(CHARACTERS.charAt(index));
        }

        return builder.toString();
    }

    private UrlResponse toResponse(UrlEntity entity) {

        UrlResponse response = new UrlResponse();

        response.setOriginalUrl(entity.getOriginalUrl());
        response.setShortCode(entity.getShortCode());
        response.setShortUrl(
                "http://localhost:8080/" + entity.getShortCode()
        );
        response.setCreatedAt(entity.getCreatedAt());
        response.setExpiresAt(entity.getExpiresAt());
        response.setClickCount(entity.getClickCount());
        response.setActive(entity.getActive());

        return response;
    }
}