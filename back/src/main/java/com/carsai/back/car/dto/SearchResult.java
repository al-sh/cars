package com.carsai.back.car.dto;

import lombok.Builder;

import java.util.List;

/**
 * Результат поиска для MessageService/LLM.
 * Содержит общее количество совпадений и список авто для форматирования.
 */
@Builder
public record SearchResult(
        int count,
        List<CarShortDto> items
) {}
