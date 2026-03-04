package com.carsai.back.llm;

import com.carsai.back.common.exception.LLMConfigurationException;
import com.carsai.back.common.exception.LLMRateLimitException;
import com.carsai.back.common.exception.LLMTimeoutException;
import com.carsai.back.common.exception.LLMUnavailableException;
import com.carsai.back.config.LLMProperties;
import com.carsai.back.llm.dto.DeepSeekMessage;
import com.carsai.back.llm.dto.DeepSeekRequest;
import com.carsai.back.llm.dto.DeepSeekResponse;
import com.carsai.back.llm.dto.LLMResponse;
import com.carsai.back.llm.dto.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Реализация LLMProvider для DeepSeek API.
 *
 * Особенности:
 * - Retry с exponential backoff для 429/502/503 через Reactor (без Thread.sleep и рекурсии)
 * - Логирование токен-статистики для мониторинга затрат
 * - Таймаут и параметры retry настраиваются через LLMProperties
 *
 * Намеренно не помечен @Service — создаётся через @Bean в LLMConfig,
 * чтобы избежать двойной регистрации в Spring-контексте.
 */
@RequiredArgsConstructor
@Slf4j
public class DeepSeekLLMProvider implements LLMProvider {

    private final WebClient deepseekClient;
    private final LLMProperties props;

    @Override
    public LLMResponse chat(String systemPrompt, String userMessage, double temperature) {
        DeepSeekRequest request = DeepSeekRequest.builder()
                .model(props.getModel())
                .messages(List.of(
                        new DeepSeekMessage("system", systemPrompt),
                        new DeepSeekMessage("user", userMessage)
                ))
                .temperature(temperature)
                .maxTokens(props.getMaxTokens())
                .stream(false)
                .build();

        try {
            DeepSeekResponse response = deepseekClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(DeepSeekResponse.class)
                    .timeout(props.getTimeout())
                    .retryWhen(Retry.backoff(
                                    props.getRetry().getMaxAttempts(),
                                    props.getRetry().getInitialDelay())
                            .filter(this::isRetriable)
                            .doBeforeRetry(signal -> log.warn(
                                    "LLM API error — retrying (attempt {}/{})",
                                    signal.totalRetries() + 1,
                                    props.getRetry().getMaxAttempts()))
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                    .block();

            if (response == null
                    || response.getChoices() == null
                    || response.getChoices().isEmpty()) {
                throw new LLMUnavailableException("Пустой ответ от DeepSeek API");
            }

            String content = response.getChoices().get(0).getMessage().getContent();

            TokenUsage tokenUsage = null;
            if (response.getUsage() != null) {
                tokenUsage = TokenUsage.builder()
                        .promptTokens(response.getUsage().getPromptTokens())
                        .completionTokens(response.getUsage().getCompletionTokens())
                        .totalTokens(response.getUsage().getTotalTokens())
                        .build();
                log.info("LLM token usage — prompt: {}, completion: {}, total: {}",
                        tokenUsage.getPromptTokens(),
                        tokenUsage.getCompletionTokens(),
                        tokenUsage.getTotalTokens());
            }

            return LLMResponse.builder()
                    .content(content)
                    .tokenUsage(tokenUsage)
                    .build();

        } catch (LLMTimeoutException | LLMRateLimitException
                 | LLMUnavailableException | LLMConfigurationException e) {
            throw e;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private boolean isRetriable(Throwable e) {
        if (e instanceof WebClientResponseException httpEx) {
            int status = httpEx.getStatusCode().value();
            return status == 429 || status == 502 || status == 503;
        }
        return false;
    }

    private LLMResponse handleException(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;

        if (cause instanceof TimeoutException || e instanceof TimeoutException) {
            log.warn("LLM request timed out");
            throw new LLMTimeoutException();
        }

        if (e instanceof WebClientResponseException httpEx) {
            int status = httpEx.getStatusCode().value();
            if (status == 429) {
                throw new LLMRateLimitException("Превышен лимит запросов к DeepSeek API");
            }
            if (status == 502 || status == 503) {
                throw new LLMUnavailableException("DeepSeek API временно недоступен");
            }
            if (status == 401) {
                log.error("Invalid DeepSeek API key. Check DEEPSEEK_API_KEY configuration.");
                throw new LLMConfigurationException("Неверная конфигурация API ключа");
            }
            log.error("Unexpected HTTP error from DeepSeek API: {}", status);
            throw new LLMUnavailableException("Ошибка API: " + status);
        }

        log.error("Network error calling DeepSeek API", e);
        throw new LLMUnavailableException("Ошибка сети при обращении к DeepSeek API");
    }
}
