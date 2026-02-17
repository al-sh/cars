package com.carsai.back.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Роли пользователей в системе.
 *
 * Enum в Java — это не просто набор строк (как в TypeScript),
 * а полноценный класс с фиксированным набором экземпляров.
 * Каждое значение (CLIENT, MANAGER, ADMIN) — это singleton-объект.
 *
 * В БД хранится как UPPER_CASE строка ('CLIENT', 'MANAGER', 'ADMIN') благодаря
 * аннотации @Enumerated(EnumType.STRING) в Entity.
 *
 * В JSON (для фронтенда) сериализуется в lower-case: "client", "manager", "admin" —
 * согласно контракту (types.md). Это реализовано через Jackson-аннотации:
 * - @JsonValue — при сериализации (Java → JSON): CLIENT → "client"
 * - @JsonCreator — при десериализации (JSON → Java): "client" → CLIENT
 */
public enum UserRole {
    CLIENT,   // Обычный пользователь — подбирает автомобили через чат
    MANAGER,  // Менеджер — может просматривать чаты клиентов
    ADMIN;    // Администратор — полный доступ к системе

    /**
     * Сериализация в JSON: CLIENT → "client".
     *
     * @JsonValue говорит Jackson: "при конвертации в JSON используй результат этого метода".
     * Без этой аннотации Jackson отдавал бы "CLIENT" (UPPER_CASE).
     * Контракт API требует lower-case.
     */
    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    /**
     * Десериализация из JSON: "client" → CLIENT.
     *
     * @JsonCreator говорит Jackson: "при конвертации из JSON используй этот метод".
     * Принимает строку из JSON и находит соответствующее значение enum.
     * Регистронезависимый поиск — "client", "CLIENT", "Client" все работают.
     *
     * @param value строка из JSON
     * @return значение enum
     * @throws IllegalArgumentException если строка не соответствует ни одному значению
     */
    @JsonCreator
    public static UserRole fromJson(String value) {
        return valueOf(value.toUpperCase());
    }
}
