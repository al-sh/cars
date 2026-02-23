package com.carsai.back.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration-тесты AuthController — тестируем полный стек:
 * HTTP-запрос → Controller → Service → Repository → PostgreSQL.
 *
 * Чем отличается от AuthServiceTest:
 * - AuthServiceTest: только Service в изоляции, моки вместо БД. Быстро.
 * - AuthControllerTest: весь стек с реальной БД. Медленно, но покрывает больше.
 *
 * @SpringBootTest — поднимает полный Spring-контекст.
 * @AutoConfigureMockMvc — создаёт MockMvc для HTTP-запросов без запуска сервера.
 * @Testcontainers — управляет Docker-контейнером с PostgreSQL.
 * @ActiveProfiles("dev") — загружает jwt.secret из application-dev.yml.
 *
 * MockMvc — эмулирует HTTP без реального сетевого стека.
 * Быстрее настоящего HTTP, не нужен работающий сервер.
 * Аналог Express/Node.js: supertest — request(app).post('/api/auth').expect(201)
 *
 * @ServiceConnection — Spring Boot 3.1+: автоматически настраивает datasource
 * из контейнера. Заменяет @DynamicPropertySource.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("dev")
class AuthControllerTest {

    // static — один контейнер на весь класс, реиспользуется между тестами.
    // Запуск Docker-контейнера занимает ~2-5 секунд — делаем это один раз.
    // @ServiceConnection читает JDBC URL напрямую из PostgreSQLContainer
    // и перекрывает spring.datasource.* без @DynamicPropertySource.
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    // Spring автоматически создаёт MockMvc из @AutoConfigureMockMvc.
    // MockMvc — объект для выполнения HTTP-запросов и проверки ответов.
    @Autowired
    private MockMvc mockMvc;

    // ===== POST /api/v1/auth/register =====

    @Test
    void register_shouldReturn201_whenValidData() throws Exception {
        // MockMvc DSL:
        // perform(request) — выполнить HTTP-запрос
        // andExpect(condition) — проверить условие в ответе
        // jsonPath("$.field") — проверить поле в JSON-ответе ($ = корень)
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Иван","email":"reg201@test.com","password":"password123"}
                                """))
                .andExpect(status().isCreated())                         // 201
                .andExpect(jsonPath("$.user.email").value("reg201@test.com"))
                .andExpect(jsonPath("$.user.role").value("client"))       // UserRole.toJson() → "client"
                .andExpect(jsonPath("$.token").isNotEmpty());             // JWT-токен присутствует
    }

    @Test
    void register_shouldReturn400_whenInvalidEmail() throws Exception {
        // Bean Validation: @Email в RegisterRequest отклоняет "not-an-email"
        // → MethodArgumentNotValidException → GlobalExceptionHandler → 400
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Иван","email":"not-an-email","password":"password123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_error"));
    }

    @Test
    void register_shouldReturn400_whenPasswordTooShort() throws Exception {
        // Bean Validation: @Size(min=6) в RegisterRequest отклоняет "123"
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Иван","email":"short@test.com","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_error"));
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        // Уникальные email-адреса для каждого теста, чтобы тесты не мешали друг другу.
        // Первая регистрация — успешная
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Иван","email":"dup@test.com","password":"password123"}
                                """))
                .andExpect(status().isCreated());

        // Вторая регистрация с тем же email → 409 Conflict
        // EmailAlreadyExistsException → GlobalExceptionHandler → 409
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Иван","email":"dup@test.com","password":"password123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_exists"));
    }

    // ===== POST /api/v1/auth/login =====

    @Test
    void login_shouldReturn200_whenValidCredentials() throws Exception {
        // Сначала регистрируем — логин требует существующего пользователя
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Иван","email":"login200@test.com","password":"password123"}
                                """))
                .andExpect(status().isCreated());

        // Затем логинимся с правильными данными
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login200@test.com","password":"password123"}
                                """))
                .andExpect(status().isOk())                              // 200 (не 201 — мы ничего не создаём)
                .andExpect(jsonPath("$.user.email").value("login200@test.com"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_shouldReturn401_whenWrongPassword() throws Exception {
        // Регистрируем пользователя
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Иван","email":"wrongpwd@test.com","password":"password123"}
                                """))
                .andExpect(status().isCreated());

        // Пробуем войти с неверным паролем
        // passwordEncoder.matches(wrong, hash) → false → InvalidCredentialsException → 401
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wrongpwd@test.com","password":"wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    // ===== Защита эндпоинтов =====

    @Test
    void protectedEndpoint_shouldReturn401_withoutToken() throws Exception {
        // GET /api/v1/chats — защищённый эндпоинт (anyRequest().authenticated())
        // Без JWT → JwtAuthenticationFilter не устанавливает аутентификацию
        // → HttpStatusEntryPoint(UNAUTHORIZED) в SecurityConfig → 401
        //
        // Проверяем, что SecurityConfig настроен правильно:
        // неаутентифицированные запросы получают 401, не 403 и не редирект.
        mockMvc.perform(get("/api/v1/chats"))
                .andExpect(status().isUnauthorized());
    }
}
