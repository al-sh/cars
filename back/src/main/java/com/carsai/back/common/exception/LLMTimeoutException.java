package com.carsai.back.common.exception;

public class LLMTimeoutException extends RuntimeException {
    public LLMTimeoutException() {
        super("Превышено время ожидания ответа от LLM");
    }
}
