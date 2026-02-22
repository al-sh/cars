package com.carsai.back.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурация CORS (Cross-Origin Resource Sharing).
 *
 * Проблема: браузер реализует политику Same-Origin (одинаковый хост + порт + протокол).
 * Angular dev-сервер работает на http://localhost:4200,
 * а наш backend — на http://localhost:8080.
 * Разные порты = разные источники → браузер блокирует запросы.
 *
 * Решение: сервер явно указывает, каким источникам он доверяет.
 *
 * Как работает CORS для "сложных" запросов (PATCH, DELETE, с заголовками):
 * 1. Браузер отправляет preflight OPTIONS-запрос с заголовком Origin
 * 2. Сервер отвечает какие методы/заголовки разрешены (Access-Control-Allow-*)
 * 3. Если разрешено — браузер отправляет настоящий запрос
 *
 * Аналог Express:
 *   const cors = require('cors');
 *   app.use(cors({ origin: 'http://localhost:4200', credentials: true }));
 *
 * WebMvcConfigurer — интерфейс для кастомизации Spring MVC без полной замены конфигурации.
 * implements вместо extends: мы дополняем поведение, а не заменяем.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * Настраивает разрешённые источники, методы и заголовки для CORS.
     *
     * addMapping("/api/**") — применяем CORS ко всем путям под /api/.
     * Не применяем ко всему (/**), чтобы не трогать статику или другие ресурсы.
     *
     * allowedOrigins — список доверенных источников.
     * localhost:4200 — Angular dev-сервер. В production здесь будет домен продукта.
     *
     * allowedMethods — какие HTTP-методы разрешены из другого источника.
     * OPTIONS — обязательно для preflight-запросов.
     *
     * allowedHeaders("*") — принимаем любые заголовки от клиента.
     * Это нужно, чтобы Angular мог отправлять Authorization: Bearer <token>.
     *
     * allowCredentials(true) — разрешаем отправку credentials (cookies, Authorization).
     * Важно: при allowCredentials нельзя использовать allowedOrigins("*").
     * Нужно указывать конкретные источники — именно поэтому выше localhost:4200.
     *
     * maxAge(3600) — браузер кэширует результат preflight на 1 час.
     * Без кэша preflight OPTIONS отправлялся бы перед каждым запросом.
     *
     * @param registry реестр CORS-маппингов, в который добавляем настройки
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
