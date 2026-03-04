package com.carsai.back.llm.dto;

import com.carsai.back.car.dto.CarSearchCriteria;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Результат Extract-режима LLM.
 * LLM возвращает JSON этой структуры при извлечении критериев поиска.
 *
 * readyToSearch: true если цена + минимум 2 дополнительных критерия
 * criteria: извлечённые критерии (только упомянутые, остальные null)
 * clarificationQuestion: вопрос если criteria недостаточно (null при readyToSearch: true)
 * extractedInfo: краткое human-readable описание найденных критериев
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractResult {

    @JsonProperty("readyToSearch")
    private boolean readyToSearch;

    @JsonProperty("criteria")
    private CarSearchCriteria criteria;

    @JsonProperty("clarificationQuestion")
    private String clarificationQuestion;

    @JsonProperty("extractedInfo")
    private String extractedInfo;

    public static ExtractResult needsClarification(String question) {
        return ExtractResult.builder()
                .readyToSearch(false)
                .clarificationQuestion(question)
                .build();
    }
}
