package com.engineering.software_engineering_orchestrator.urlshortener.exception;

public class ExpiredUrlException extends RuntimeException {

    public ExpiredUrlException(String message) {
        super(message);
    }
}