package com.carsai.back.common.exception;

public class LLMRateLimitException extends RuntimeException {
    public LLMRateLimitException(String message) {
        super(message);
    }
}
