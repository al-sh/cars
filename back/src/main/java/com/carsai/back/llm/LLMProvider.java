package com.carsai.back.llm;

import com.carsai.back.llm.dto.LLMResponse;

/**
 * Абстракция LLM-провайдера.
 * Позволяет менять DeepSeek на другой провайдер без изменения бизнес-логики.
 */
public interface LLMProvider {

    /**
     * Выполняет запрос к LLM API.
     *
     * @param systemPrompt системный промт (инструкции для LLM)
     * @param userMessage  сообщение пользователя / данные для обработки
     * @param temperature  температура (0.0 — детерминированный, 1.0 — творческий)
     * @return ответ от LLM с токен-статистикой
     * @throws com.carsai.back.common.exception.LLMTimeoutException      при таймауте
     * @throws com.carsai.back.common.exception.LLMRateLimitException    при rate limit API
     * @throws com.carsai.back.common.exception.LLMUnavailableException  при недоступности API
     */
    LLMResponse chat(String systemPrompt, String userMessage, double temperature);
}
