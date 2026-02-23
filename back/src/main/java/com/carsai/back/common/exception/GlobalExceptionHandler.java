package com.carsai.back.common.exception;

import com.carsai.back.common.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений для всех контроллеров.
 *
 * Паттерн: вместо try-catch в каждом методе контроллера —
 * одно централизованное место для обработки всех типов ошибок.
 * Каждое исключение → единый JSON-формат ErrorResponse → правильный HTTP-статус.
 *
 * @RestControllerAdvice — аннотация-комбинация:
 * - @ControllerAdvice: перехватывает исключения из ВСЕХ контроллеров
 * - @ResponseBody: каждый @ExceptionHandler метод возвращает JSON (не HTML)
 *
 * Аналог в Express:
 *   app.use((err, req, res, next) => {
 *     if (err instanceof EmailAlreadyExistsException) {
 *       return res.status(409).json({ code: 'email_exists', message: err.message })
 *     }
 *     res.status(500).json({ code: 'server_error', message: 'Внутренняя ошибка' })
 *   })
 *
 * Аналог Angular: HttpErrorResponse в HTTP Interceptor на стороне клиента.
 *
 * ВАЖНО: Стектрейсы никогда не отправляем клиенту — только логируем.
 * Это безопасность: стектрейс раскрывает структуру кода.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // SLF4J Logger — стандартный логгер в Spring Boot.
    // Аналог console.error() в Node.js, но с уровнями: trace, debug, info, warn, error.
    // LoggerFactory.getLogger(X.class) привязывает логи к классу для фильтрации.
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Email уже зарегистрирован.
     *
     * 409 Conflict — стандартный статус для конфликта с текущим состоянием ресурса.
     * В REST: "этот email уже занят" — именно конфликт, не ошибка клиента.
     *
     * @ExceptionHandler(X.class) — перехватывает только исключения типа X.
     * Если бросилось EmailAlreadyExistsException — вызовется этот метод.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailExists(EmailAlreadyExistsException ex) {
        // Не логируем — это ожидаемая ситуация, не ошибка системы.
        return ErrorResponse.of("email_exists", "Пользователь с таким email уже существует");
    }

    /**
     * Неверный email или пароль при логине.
     *
     * 401 Unauthorized — клиент не аутентифицирован.
     * Сообщение намеренно общее — не раскрываем, что именно неверно.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentials(InvalidCredentialsException ex) {
        return ErrorResponse.of("invalid_credentials", "Неверный email или пароль");
    }

    /**
     * Невалидный или истёкший JWT-токен.
     *
     * 401 Unauthorized — токен есть, но не принят.
     * Клиент должен разлогиниться и запросить новый токен.
     */
    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidToken(InvalidTokenException ex) {
        return ErrorResponse.of("invalid_token", "Невалидный или истёкший токен");
    }

    /**
     * Чат не найден или не принадлежит текущему пользователю.
     *
     * 404 Not Found — стандартный статус для отсутствующего ресурса.
     * Намеренно не различаем "не существует" и "чужой" — оба дают 404.
     * Это предотвращает enumeration-атаку: злоумышленник не узнает,
     * существует ли чат с таким UUID.
     */
    @ExceptionHandler(ChatNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleChatNotFound(ChatNotFoundException ex) {
        return ErrorResponse.of("not_found", "Чат не найден");
    }

    /**
     * Сообщение не найдено или не принадлежит текущему пользователю.
     *
     * 404 Not Found — намеренно не различаем "не существует" и "чужое",
     * чтобы не раскрывать факт существования чужих сообщений (enumeration-защита).
     */
    @ExceptionHandler(MessageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleMessageNotFound(MessageNotFoundException ex) {
        return ErrorResponse.of("not_found", "Сообщение не найдено");
    }

    /**
     * Доступ запрещён — пользователь аутентифицирован, но не имеет прав.
     *
     * Срабатывает при @PreAuthorize("hasRole('ADMIN')"), когда роль не совпадает.
     * 403 Forbidden — "я знаю кто ты, но тебе нельзя".
     *
     * ВАЖНО: отличие от 401 (Unauthorized):
     * - 401 — пользователь не аутентифицирован (нет токена)
     * - 403 — пользователь аутентифицирован, но прав недостаточно
     *
     * Случай "нет токена" обрабатывается в SecurityConfig через HttpStatusEntryPoint(401),
     * а не здесь, потому что Spring Security блокирует запрос до вызова контроллера.
     * Этот @ExceptionHandler ловит AccessDeniedException из @PreAuthorize-проверок.
     *
     * В Express аналог: if (req.user.role !== 'admin') return res.status(403).json(...)
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return ErrorResponse.of("forbidden", "Доступ запрещён");
    }

    /**
     * Ошибки валидации Bean Validation (@NotBlank, @Email, @Size и др.).
     *
     * Бросается Spring, когда @Valid не прошла для @RequestBody.
     * 400 Bad Request — клиент прислал некорректные данные.
     *
     * MethodArgumentNotValidException содержит список FieldError —
     * каждый FieldError это одна провалившаяся проверка.
     *
     * Формируем читаемое сообщение: "email: Некорректный формат email, password: ..."
     *
     * Аналог Angular: form.errors, control.errors — список ошибок валидации.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        // getBindingResult() — результат валидации со всеми ошибками.
        // getFieldErrors() — список ошибок по конкретным полям.
        // Собираем в одну строку: "email: текст ошибки, name: текст ошибки"
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ErrorResponse.of("validation_error", message);
    }

    /**
     * Невалидный JSON в теле запроса.
     *
     * Бросается при ошибке десериализации: клиент прислал не JSON,
     * или JSON с неверными типами данных.
     *
     * 400 Bad Request — синтаксическая ошибка на стороне клиента.
     *
     * Примеры: забытая кавычка, неверный тип поля, пустое тело.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNotReadable(HttpMessageNotReadableException ex) {
        return ErrorResponse.of("bad_request", "Некорректный формат запроса");
    }

    /**
     * Fallback — перехватывает все необработанные исключения.
     *
     * 500 Internal Server Error — что-то пошло не так на сервере.
     *
     * ВАЖНО: Логируем полный стектрейс (log.error с ex) для диагностики,
     * но клиенту отдаём только общее сообщение без деталей.
     * Стектрейс содержит пути файлов, имена классов — это потенциальная
     * утечка информации об архитектуре системы.
     *
     * Exception.class в иерархии — суперкласс всех исключений,
     * поэтому срабатывает как catch-all.
     * Spring выбирает наиболее специфичный @ExceptionHandler,
     * поэтому этот метод вызывается только если ни один из выше не совпал.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAll(Exception ex) {
        // log.error — уровень ERROR, будет виден в логах даже в production.
        // Второй аргумент ex — Spring логирует стектрейс автоматически.
        log.error("Необработанное исключение: {}", ex.getMessage(), ex);
        return ErrorResponse.of("server_error", "Внутренняя ошибка сервера");
    }
}
