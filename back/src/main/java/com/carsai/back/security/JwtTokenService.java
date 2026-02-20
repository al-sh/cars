package com.carsai.back.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.carsai.back.common.exception.InvalidTokenException;
import com.carsai.back.user.User;
import com.carsai.back.user.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Сервис генерации и валидации JWT-токенов.
 *
 * JWT (JSON Web Token) — токен из трёх частей: header.payload.signature
 * - Header: алгоритм подписи (HS256)
 * - Payload: claims — данные (userId, email, role, expiration)
 * - Signature: HMAC-SHA256 подпись секретным ключом
 *
 * Схема работы:
 * 1. Логин/регистрация → generateToken(user) → клиент сохраняет токен
 * 2. Каждый запрос → клиент отправляет Authorization: Bearer <token>
 * 3. JwtAuthenticationFilter вызывает validateToken(token) → UserPrincipal
 * 4. UserPrincipal помещается в SecurityContext → доступен
 * через @AuthenticationPrincipal
 *
 * Аналог Node.js:
 * - generateToken: jwt.sign(payload, secret, { expiresIn: '7d' })
 * - validateToken: jwt.verify(token, secret) → payload
 *
 * @Service — singleton, Spring управляет жизненным циклом.
 */
@Service
public class JwtTokenService {

    /**
     * Секретный ключ в Base64-формате. Читается из application.yml.
     *
     * @Value("${jwt.secret}") — инъекция значения из конфига.
     * Spring ищет ключ jwt.secret и подставляет значение.
     *
     * В application.yml: jwt.secret: ${JWT_SECRET}
     * → Spring сначала смотрит переменную окружения JWT_SECRET,
     * если нет — бросает ошибку (намеренно: не хардкодим дефолт).
     *
     * Аналог Express: process.env.JWT_SECRET
     * Аналог Angular: environment.ts → но там секрет НЕ хранят (клиент доступен
     * всем)
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Срок жизни токена. Spring Boot парсит "7d" как Duration.ofDays(7).
     * Поддерживаемые суффиксы: ns, us, ms, s, m, h, d.
     */
    @Value("${jwt.token-expiration}")
    private Duration tokenExpiration;

    /**
     * Генерация JWT-токена для пользователя.
     *
     * Структура payload (claims):
     * - sub (subject): UUID пользователя — стандартный claim "кто владелец"
     * - email: для удобства (чтобы не ходить в БД за email)
     * - role: роль в lower-case ("client", "manager", "admin")
     * - iat (issuedAt): когда создан — стандартный claim
     * - exp (expiration): когда истечёт — стандартный claim
     *
     * Пример токена (после decode payload):
     * { "sub": "550e8400...", "email": "ivan@test.com", "role": "client",
     * "iat": 1705312200, "exp": 1705917000 }
     *
     * @param user Entity пользователя из БД
     * @return подписанный JWT-токен в виде строки
     */
    public String generateToken(User user) {
        return Jwts.builder()
                // sub — кто владелец токена. Стандартный claim JWT.
                // Используем UUID как строку (UUID не сериализуется напрямую в JWT).
                .subject(user.getId().toString())

                // Кастомные claims — дополнительные данные в токене.
                // Кладём email и role, чтобы не ходить в БД при каждом запросе.
                .claim("email", user.getEmail())
                // Роль в lower-case согласно контракту API (types.md).
                .claim("role", user.getRole().name().toLowerCase())

                // Стандартные временные claims.
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(tokenExpiration)))

                // Подпись HMAC-SHA256 секретным ключом.
                // Если кто-то изменит payload — подпись не совпадёт, токен отклонится.
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Валидация токена и извлечение данных пользователя.
     *
     * Что проверяется автоматически:
     * - Подпись: payload не был изменён
     * - Срок действия: exp > текущее время
     *
     * Если проверка не прошла — JwtException → оборачиваем в InvalidTokenException.
     * JwtAuthenticationFilter ловит InvalidTokenException и продолжает без
     * аутентификации.
     *
     * @param token JWT-токен из заголовка Authorization
     * @return UserPrincipal — объект текущего пользователя
     * @throws InvalidTokenException если токен невалиден или истёк
     */
    public UserPrincipal validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Извлекаем данные из claims и собираем UserPrincipal.
            // Эти данные были записаны в generateToken() — они надёжны (подпись проверена).
            UUID id = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            // role хранится как lower-case строка, fromJson() конвертирует в enum.
            UserRole role = UserRole.fromJson(claims.get("role", String.class));

            return new UserPrincipal(id, email, role);

        } catch (JwtException | IllegalArgumentException e) {
            // JwtException: невалидная подпись, истёкший токен, неверный формат
            // IllegalArgumentException: невалидный UUID или role в claims
            throw new InvalidTokenException("Невалидный или истёкший токен");
        }
    }

    /**
     * Создание криптографического ключа из Base64-строки.
     *
     * HMAC-SHA256 требует ключ минимум 256 бит (32 байта).
     * Мы храним ключ в Base64, декодируем байты и создаём SecretKey.
     *
     * Keys.hmacShaKeyFor() проверяет длину и выбирает правильный алгоритм:
     * - 256 бит → HS256
     * - 384 бит → HS384
     * - 512 бит → HS512
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
