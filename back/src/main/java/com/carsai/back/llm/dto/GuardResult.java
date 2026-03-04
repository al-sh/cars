package com.carsai.back.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Результат Guard-режима LLM.
 * LLM возвращает JSON этой структуры при проверке релевантности запроса.
 *
 * relevant: true если запрос относится к подбору автомобилей
 * rejectionResponse: ответ пользователю если запрос нерелевантен (null при relevant: true)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardResult {

    // Java-поле: relevant → Lombok-геттер: isRelevant() (стандарт для boolean)
    // JSON-ключ: "relevant" — совпадает с именем поля, @JsonProperty не нужен
    private boolean relevant;

    private String rejectionResponse;
}
