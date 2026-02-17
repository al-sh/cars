package com.carsai.back.common.dto;

/**
 * Единый формат ошибок API — согласно контракту.
 *
 * Все ошибки (валидация, аутентификация, not found, серверные) возвращаются
 * в одном формате. Клиент всегда знает структуру ответа при ошибке.
 *
 * Поля:
 * - code — машиночитаемый код ошибки (для switch/if на фронте)
 * - message — человекочитаемое описание (для отображения пользователю)
 *
 *
 * Примеры ответов:
 * 400: { "code": "validation_error", "message": "email: Некорректный формат email" }
 * 401: { "code": "invalid_credentials", "message": "Неверный email или пароль" }
 * 404: { "code": "not_found", "message": "Чат не найден" }
 * 409: { "code": "email_exists", "message": "Пользователь с таким email уже существует" }
 * 500: { "code": "server_error", "message": "Внутренняя ошибка сервера" }
 */
public record ErrorResponse(
        String code,
        String message
) {

    /**
     * Фабричный метод для удобного создания.
     *
     * ErrorResponse.of("not_found", "Чат не найден")
     * vs
     * new ErrorResponse("not_found", "Чат не найден")
     *
     * Читабельнее, особенно когда используется в @ExceptionHandler.
     */
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }
}
