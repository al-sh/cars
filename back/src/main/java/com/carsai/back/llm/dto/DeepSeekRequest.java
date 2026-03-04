package com.carsai.back.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Тело запроса к DeepSeek Chat Completions API.
 * DeepSeek использует OpenAI-совместимый формат.
 */
@Data
@Builder
public class DeepSeekRequest {

    private String model;

    private List<DeepSeekMessage> messages;

    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Boolean stream;
}
