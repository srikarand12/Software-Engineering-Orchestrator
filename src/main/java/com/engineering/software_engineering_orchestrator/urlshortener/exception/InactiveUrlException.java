package com.engineering.software_engineering_orchestrator.urlshortener.exception;

public class InactiveUrlException extends RuntimeException {

    public InactiveUrlException(String message) {
        super(message);
    }
}