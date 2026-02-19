package com.carsai.back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Конфигурация Spring Security.
 *
 * На текущем этапе (5) — минимальная конфигурация:
 * - Bean PasswordEncoder для хеширования паролей (BCrypt)
 * - Все запросы разрешены (JWT-фильтр появится на этапе 7)
 * - Stateless-сессии (REST API не использует HTTP-сессии)
 * - CSRF отключён (защита не нужна для stateless JWT API)
 *
 * На этапе 7 будет добавлен JWT-фильтр и ограничения по эндпоинтам.
 *
 * @EnableWebSecurity — включает Spring Security и позволяет
 * кастомизировать SecurityFilterChain.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Bean для хеширования паролей.
     *
     * BCrypt — адаптивный алгоритм хеширования:
     * - Автоматически добавляет salt (не нужно хранить отдельно)
     * - Настраиваемая «стоимость» (по умолчанию 10 раундов)
     * - Результат: "$2a$10$..." (60 символов)
     *
     * Используется в AuthService:
     * - register: passwordEncoder.encode(password) → hash
     * - login: passwordEncoder.matches(password, hash) → true/false
     *
     * Аналог Node.js: bcrypt.hash(password, 10) / bcrypt.compare(password, hash)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Конфигурация Security Filter Chain.
     *
     * Временная конфигурация — все запросы разрешены.
     * На этапе 7 здесь появятся правила:
     * - POST /api/v1/auth/** → permitAll (регистрация, логин)
     * - Остальные → authenticated (нужен JWT)
     * - JWT-фильтр перед UsernamePasswordAuthenticationFilter
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .build();
    }
}
