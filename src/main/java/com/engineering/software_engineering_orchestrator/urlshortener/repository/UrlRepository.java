package com.engineering.software_engineering_orchestrator.urlshortener.repository;

import com.engineering.software_engineering_orchestrator.urlshortener.domain.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlEntity, Long> {

    Optional<UrlEntity> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);
}