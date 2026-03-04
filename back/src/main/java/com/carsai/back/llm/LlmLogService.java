package com.carsai.back.llm;

import com.carsai.back.llm.dto.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmLogService {

    private final LlmLogRepository llmLogRepository;

    @Async
    public void log(UUID chatId, UUID messageId, String requestType,
                    String systemPrompt, String userMessage,
                    String responseContent, TokenUsage tokenUsage,
                    long durationMs, String error) {
        try {
            LlmLog llmLog = LlmLog.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .requestType(requestType)
                    .systemPrompt(systemPrompt)
                    .userMessage(userMessage)
                    .responseContent(responseContent)
                    .promptTokens(tokenUsage != null ? tokenUsage.getPromptTokens() : null)
                    .completionTokens(tokenUsage != null ? tokenUsage.getCompletionTokens() : null)
                    .totalTokens(tokenUsage != null ? tokenUsage.getTotalTokens() : null)
                    .durationMs(durationMs)
                    .error(error)
                    .build();
            llmLogRepository.save(llmLog);
        } catch (Exception e) {
            log.warn("Failed to save LLM log: {}", e.getMessage());
        }
    }
}
