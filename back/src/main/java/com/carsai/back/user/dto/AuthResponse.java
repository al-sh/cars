package com.carsai.back.user.dto;

/**
 * DTO ответа на регистрацию и логин.
 *
 * Содержит данные пользователя и JWT-токен для последующих запросов.
 * Клиент сохраняет токен (в localStorage или memory) и отправляет
 * в заголовке Authorization: Bearer <token> при каждом запросе.
 *
 * Пример JSON ответа:
 * {
 *   "user": {
 *     "id": "550e8400-e29b-41d4-a716-446655440000",
 *     "email": "ivan@example.com",
 *     "name": "Иван",
 *     "role": "client",
 *     "createdAt": "2025-01-15T10:30:00Z"
 *   },
 *   "token": "eyJhbGciOiJIUzI1NiIs..."
 * }
 */
public record AuthResponse(
        UserDto user,
        String token
) {}
