package com.carsai.back.common.exception;

public class LLMUnavailableException extends RuntimeException {
    public LLMUnavailableException(String message) {
        super(message);
    }
}
