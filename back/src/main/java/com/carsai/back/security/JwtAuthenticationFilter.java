package com.carsai.back.security;

import com.carsai.back.common.exception.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT-фильтр аутентификации.
 *
 * Перехватывает каждый HTTP-запрос и пытается аутентифицировать пользователя
 * по JWT-токену в заголовке Authorization.
 *
 * Место в цепочке обработки запроса:
 *   HTTP-запрос
 *     → JwtAuthenticationFilter (наш фильтр — здесь)
 *     → UsernamePasswordAuthenticationFilter (стандартный Spring)
 *     → ... другие фильтры
 *     → Controller
 *
 * Аналог в Express:
 *   app.use((req, res, next) => {
 *     const token = req.headers.authorization?.split(' ')[1];
 *     if (token) req.user = jwtService.verify(token);
 *     next();
 *   });
 *
 * Аналог в Angular: HTTP Interceptor, который добавляет/проверяет заголовки.
 *
 * OncePerRequestFilter — базовый класс Spring, гарантирующий, что
 * doFilterInternal() вызывается РОВНО ОДИН РАЗ на каждый запрос.
 * Важно: без этого некоторые сервлет-контейнеры могут вызвать фильтр дважды
 * (например, при forward-редиректах внутри сервера).
 *
 * @Component — Spring обнаружит этот класс и зарегистрирует как bean.
 * @RequiredArgsConstructor — Lombok генерирует конструктор из final-полей.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Сервис для валидации JWT и извлечения UserPrincipal из claims.
     * Внедряется через конструктор (DI).
     */
    private final JwtTokenService jwtTokenService;

    /**
     * Основная логика фильтра — вызывается один раз на каждый запрос.
     *
     * Алгоритм:
     * 1. Извлечь токен из заголовка Authorization: Bearer <token>
     * 2. Если токен есть — провалидировать и получить UserPrincipal
     * 3. Установить аутентификацию в SecurityContextHolder
     * 4. Передать запрос дальше по цепочке (filterChain.doFilter)
     *
     * Ключевая идея: если токена нет или он невалиден — мы НЕ бросаем исключение
     * и НЕ возвращаем 401 здесь. Мы просто не устанавливаем аутентификацию.
     * Spring Security сам вернёт 401, если запрошенный эндпоинт требует auth.
     * Это позволяет публичным эндпоинтам (например, /auth/register) работать без токена.
     *
     * @param request     входящий HTTP-запрос
     * @param response    исходящий HTTP-ответ
     * @param filterChain цепочка фильтров — нужно вызвать doFilter, чтобы передать дальше
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Извлекаем заголовок Authorization.
        // Стандарт RFC 6750: токен передаётся как "Bearer <token>".
        // Пример: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIi...
        String authHeader = request.getHeader("Authorization");

        // Проверяем, что заголовок есть и начинается с "Bearer ".
        // Без этого preflight OPTIONS-запросы и публичные эндпоинты работают без токена.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Токена нет — передаём запрос дальше без аутентификации.
            // Если эндпоинт публичный — пройдёт. Если требует auth — Spring вернёт 401.
            filterChain.doFilter(request, response);
            return;
        }

        // Вырезаем сам токен: "Bearer eyJhbG..." → "eyJhbG..."
        // substring(7) — пропускаем первые 7 символов ("Bearer ")
        String token = authHeader.substring(7);

        try {
            // Валидируем токен и извлекаем данные пользователя.
            // validateToken проверяет подпись и срок действия.
            // Если токен валиден — возвращает UserPrincipal с id, email, role.
            UserPrincipal principal = jwtTokenService.validateToken(token);

            // Создаём объект аутентификации для Spring Security.
            //
            // UsernamePasswordAuthenticationToken — стандартный класс Spring Security.
            // Параметры:
            //   1. principal — кто аутентифицирован (UserPrincipal)
            //   2. credentials — null (пароль не нужен, есть JWT)
            //   3. authorities — роли пользователя (из getAuthorities())
            //
            // Именно наличие authorities (3-й параметр) говорит Spring Security,
            // что аутентификация прошла успешно. Без authorities — "анонимный".
            //
            // Аналог Express: req.user = { id, email, role }
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            );

            // Помещаем аутентификацию в SecurityContextHolder.
            //
            // SecurityContextHolder — thread-local хранилище: у каждого потока
            // (каждого запроса) своё хранилище. Это важно для thread-safety.
            //
            // После этой строки любой код в рамках этого запроса может получить
            // текущего пользователя через:
            // - @AuthenticationPrincipal UserPrincipal user — в Controller-методах
            // - SecurityContextHolder.getContext().getAuthentication() — в любом месте
            //
            // Аналог Express: req.user = verified — потом req.user доступен везде
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (InvalidTokenException e) {
            // Токен невалиден (подпись не совпала, истёк, неверный формат).
            // Не устанавливаем аутентификацию — запрос пройдёт как анонимный.
            // Spring Security сам вернёт 401, если эндпоинт требует auth.
            //
            // Не возвращаем 401 здесь явно — это позволяет избежать ситуации,
            // когда невалидный токен блокирует даже публичные эндпоинты.
        }

        // Передаём запрос дальше по цепочке фильтров.
        // Этот вызов ОБЯЗАТЕЛЕН — без него запрос не дойдёт до Controller.
        // В Express аналог: next()
        filterChain.doFilter(request, response);
    }
}
