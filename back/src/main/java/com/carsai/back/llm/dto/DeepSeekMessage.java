package com.carsai.back.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сообщение в формате DeepSeek/OpenAI Chat Completions API.
 * role: "system", "user" или "assistant"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekMessage {
    private String role;
    private String content;
}
