package com.carsai.back.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для запроса регистрации нового пользователя.
 *
 * Java record — компактный иммутабельный класс для хранения данных (Java 16+).
 * Автоматически создаёт: конструктор, getters (name(), email(), password()),
 * equals(), hashCode(), toString(). Поля нельзя изменить после создания.
 *
 * ВАЖНО: Это DTO (Data Transfer Object) — объект для передачи данных между
 * клиентом и сервером. Отделён от Entity (User.java), потому что:
 * 1. Entity содержит passwordHash — его нельзя отдавать клиенту
 * 2. Клиент отправляет password (открытый) — его нет в Entity
 * 3. DTO определяет контракт API, Entity — структуру БД
 * В Express аналог — валидация через express-validator:
 *   body('email').isEmail(), body('name').isLength({ min: 2 })
 *
 * Пример JSON запроса:
 * POST /api/v1/auth/register
 * {
 *   "name": "Иван",
 *   "email": "ivan@example.com",
 *   "password": "password123"
 * }
 */
public record RegisterRequest(

        // @NotBlank — значение не null, не пустая строка, не строка из одних пробелов.
        // Строже, чем @NotNull (который пропускает "") и @NotEmpty (который пропускает "   ").
        // message — текст ошибки, который вернётся клиенту при невалидных данных.
        // Аналог Angular: Validators.required (но не проверяет пробелы).
        @NotBlank(message = "Имя обязательно")
        // @Size — ограничение длины строки. min и max включительно.
        // Аналог Angular: Validators.minLength(2), Validators.maxLength(100)
        // Аналог Express: body('name').isLength({ min: 2, max: 100 })
        @Size(min = 2, max = 100, message = "Имя от 2 до 100 символов")
        String name,

        @NotBlank(message = "Email обязателен")
        // @Email — проверяет формат email (содержит @, домен и т.д.).
        // Аналог Angular: Validators.email
        // Аналог Express: body('email').isEmail()
        @Email(message = "Некорректный формат email")
        String email,

        @NotBlank(message = "Пароль обязателен")
        // Минимум 6 символов — компромисс между безопасностью и удобством для MVP.
        @Size(min = 6, max = 100, message = "Пароль от 6 до 100 символов")
        String password
) {}
