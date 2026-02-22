package com.carsai.back.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Роль отправителя сообщения в чате.
 *
 * Определяет, кто написал сообщение:
 * - USER      — пользователь приложения (человек)
 * - ASSISTANT — ИИ-ассистент (DeepSeek / другая LLM)
 * - SYSTEM    — системный промпт (инструкции для ИИ, скрыт от пользователя)
 *
 * В БД хранится как UPPER_CASE строка ('USER', 'ASSISTANT', 'SYSTEM')
 * через аннотацию @Enumerated(EnumType.STRING) в Entity.
 *
 * В JSON (для фронтенда) сериализуется в lower-case: "user", "assistant", "system"
 * согласно контракту (types.md).
 *
 * Аналогия TypeScript:
 *   type MessageRole = 'user' | 'assistant' | 'system';
 * В Java enum — это класс с фиксированным набором экземпляров + методы.
 */
public enum MessageRole {
    USER,       // Сообщение от пользователя — запрос, вопрос, уточнение
    ASSISTANT,  // Ответ ИИ-ассистента — рекомендация, вопрос для уточнения
    SYSTEM;     // Системный промпт — контекст для ИИ (не показывается пользователю)

    /**
     * Сериализация в JSON: USER → "user".
     *
     * @JsonValue — Jackson использует этот метод при конвертации enum → JSON.
     * Без аннотации Jackson отдавал бы "USER" (UPPER_CASE имя константы).
     * Контракт API требует lower-case строки.
     */
    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    /**
     * Десериализация из JSON: "user" → USER.
     *
     * @JsonCreator — Jackson вызывает этот метод при конвертации JSON → enum.
     * Принимает строку "user", "assistant" или "system" и возвращает константу enum.
     * toUpperCase() делает парсинг регистронезависимым.
     *
     * @param value строка из JSON ("user", "assistant", "system")
     * @return соответствующее значение enum
     * @throws IllegalArgumentException если строка не соответствует ни одному значению
     */
    @JsonCreator
    public static MessageRole fromJson(String value) {
        return valueOf(value.toUpperCase());
    }
}
