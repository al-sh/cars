package com.carsai.back.user;

import com.carsai.back.user.dto.AuthResponse;
import com.carsai.back.user.dto.LoginRequest;
import com.carsai.back.user.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST-контроллер аутентификации.
 *
 * Принимает HTTP-запросы, валидирует входные данные через @Valid,
 * делегирует бизнес-логику в AuthService, возвращает DTO.
 *
 * Контроллер не знает ничего о БД — только принимает и отдаёт данные.
 * Это соблюдение принципа слоистой архитектуры.
 *
 * @RestController — комбинация @Controller + @ResponseBody.
 * Каждый метод автоматически сериализует возвращаемый объект в JSON
 * через Jackson. Не нужно писать res.json() как в Express.
 *
 * @RequestMapping("/api/v1/auth") — все методы контроллера
 * будут доступны по URL-префиксу /api/v1/auth.
 *
 * Аналог Express:
 *   const router = express.Router()
 *   app.use('/api/v1/auth', router)
 *
 * @RequiredArgsConstructor — Lombok инжектит AuthService через конструктор.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Регистрация нового пользователя.
     *
     * POST /api/v1/auth/register
     *
     * Запрос:
     * {
     *   "name": "Иван",
     *   "email": "ivan@example.com",
     *   "password": "password123"
     * }
     *
     * Ответ 201:
     * {
     *   "user": { "id": "...", "email": "...", "name": "...", "role": "client", "createdAt": "..." },
     *   "token": "eyJhbGci..."
     * }
     *
     * @Valid — активирует Bean Validation из RegisterRequest.
     * Spring проверяет @NotBlank, @Email, @Size ДО вызова метода.
     * Если проверка не прошла — бросается MethodArgumentNotValidException,
     * которое перехватывается в GlobalExceptionHandler → 400 Bad Request.
     * Аналог Angular: form.invalid → не отправляем запрос.
     * Аналог Express: express-validator → validationResult(req).
     *
     * @RequestBody — десериализует JSON из тела запроса в RegisterRequest.
     * Content-Type: application/json обязателен.
     * Аналог Express: req.body (с body-parser middleware).
     *
     * @ResponseStatus(CREATED) — переопределяет дефолтный статус 200 на 201.
     * 201 Created — стандарт HTTP для успешного создания ресурса.
     * В Express: res.status(201).json(result).
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Аутентификация пользователя (логин).
     *
     * POST /api/v1/auth/login
     *
     * Запрос:
     * {
     *   "email": "ivan@example.com",
     *   "password": "password123"
     * }
     *
     * Ответ 200:
     * {
     *   "user": { ... },
     *   "token": "eyJhbGci..."
     * }
     *
     * Ошибка 401 (неверные данные):
     * {
     *   "code": "invalid_credentials",
     *   "message": "Неверный email или пароль"
     * }
     *
     * Возвращает 200, не 201 — мы ничего не создаём, а только аутентифицируемся.
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
