package com.carsai.back.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO для запроса авторизации (логина).
 *
 * Содержит только email и password — минимум для идентификации пользователя.
 * Валидация проверяет формат данных (не пустые, email валиден),
 * но НЕ проверяет существование пользователя — это делает AuthService.
 *
 * Пример JSON запроса:
 * POST /api/v1/auth/login
 * {
 *   "email": "ivan@example.com",
 *   "password": "password123"
 * }
 */
public record LoginRequest(
        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        String email,

        @NotBlank(message = "Пароль обязателен")
        String password
) {}
