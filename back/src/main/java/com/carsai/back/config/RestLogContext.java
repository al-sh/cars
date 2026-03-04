package com.carsai.back.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ThreadLocal-контекст для накопления данных LLM-вызовов в рамках одного REST-запроса.
 *
 * Используется связкой:
 *   LLMService.chatWithLogging() → добавляет вызов через addLlmCall()
 *   RestLoggingAspect           → читает getLlmCalls(), после — вызывает clear()
 */
public final class RestLogContext {

    private RestLogContext() {}

    private static final ThreadLocal<List<LlmCallEntry>> LLM_CALLS = ThreadLocal.withInitial(ArrayList::new);

    public record LlmCallEntry(
            String requestType,
            String userMessage,
            String responseContent,
            long durationMs,
            String error
    ) {}

    public static void addLlmCall(String requestType, String userMessage,
                                   String responseContent, long durationMs, String error) {
        LLM_CALLS.get().add(new LlmCallEntry(requestType, userMessage, responseContent, durationMs, error));
    }

    public static List<LlmCallEntry> getLlmCalls() {
        return Collections.unmodifiableList(LLM_CALLS.get());
    }

    public static void clear() {
        LLM_CALLS.remove();
    }
}
