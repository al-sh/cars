package com.carsai.back.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.carsai.back.user.User;
import com.carsai.back.user.UserRole;

/**
 * DTO пользователя для отправки клиенту.
 *
 * Содержит только публичные данные — БЕЗ passwordHash и deleted.
 * Entity User — это внутренняя модель с чувствительными данными.
 * UserDto — это проекция (view), которую видит клиент.
 *
 * Поле role сериализуется в lower-case ("client", "manager", "admin")
 * благодаря @JsonValue в UserRole enum.
 *
 * Пример JSON:
 * {
 *   "id": "550e8400-e29b-41d4-a716-446655440000",
 *   "email": "ivan@example.com",
 *   "name": "Иван",
 *   "role": "client",
 *   "createdAt": "2025-01-15T10:30:00Z"
 * }
 */
public record UserDto(
        UUID id,
        String email,
        String name,
        UserRole role,
        Instant createdAt
) {

    /**
     * Фабричный метод: конвертирует Entity → DTO.
     *
     * Статический метод вместо конструктора — читабельнее:
     *   UserDto.from(user)  vs  new UserDto(user.getId(), user.getEmail(), ...)
     *
     * Используется в Service-слое при подготовке ответа:
     *   return new AuthResponse(UserDto.from(user), token);
     *
     * @param user Entity из БД
     * @return DTO для отправки клиенту (без чувствительных данных)
     */
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
