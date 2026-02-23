package com.carsai.back.config;

import com.carsai.back.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Конфигурация Spring Security.
 *
 * На этом этапе (7) настраиваем полноценную JWT-аутентификацию:
 * - JWT-фильтр перехватывает все запросы и устанавливает аутентификацию
 * - /api/v1/auth/** доступен без токена (регистрация, логин)
 * - Все остальные эндпоинты требуют валидный JWT
 * - Stateless-сессии: сервер не хранит состояние между запросами
 *
 * @EnableWebSecurity — включает Spring Security. Без этой аннотации
 * SecurityFilterChain не будет применён.
 *
 * @EnableMethodSecurity — разрешает защиту на уровне методов через аннотации:
 * - @PreAuthorize("hasRole('ADMIN')") — только для администраторов
 * - @PreAuthorize("isAuthenticated()") — только для залогиненных
 * В будущем это пригодится для manager-эндпоинтов.
 *
 * @RequiredArgsConstructor — Lombok инжектит JwtAuthenticationFilter через конструктор.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Наш JWT-фильтр — внедряется через конструктор и регистрируется в цепочке.
     * Spring автоматически обнаружит JwtAuthenticationFilter (@Component)
     * и предоставит его сюда через DI.
     */
    private final JwtAuthenticationFilter jwtAuthFilter;

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
     * Конфигурация Security Filter Chain — главный bean Spring Security.
     *
     * Filter Chain — это цепочка фильтров, через которую проходит каждый запрос.
     * Мы настраиваем правила этой цепочки здесь.
     *
     * Аналог Express:
     *   app.use(corsMiddleware)
     *   app.use(jwtMiddleware)
     *   app.use('/api/auth', authRouter)          // публичный
     *   app.use('/api', requireAuth, apiRouter)   // защищённый
     *
     * Аналог Angular Router:
     *   { path: 'auth', component: AuthComponent }             // публичный
     *   { path: 'chat', canActivate: [AuthGuard], ... }        // защищённый
     *
     * @param http строитель конфигурации Spring Security
     * @return сконфигурированная цепочка фильтров
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CORS — передаём управление нашему CorsConfig (WebMvcConfigurer).
                //
                // Customizer.withDefaults() означает: использовать CORS-конфигурацию,
                // которую Spring нашёл в контексте — то есть наш CorsConfig.addCorsMappings().
                //
                // Почему это нужно помимо CorsConfig?
                // Spring Security стоит ПЕРЕД Spring MVC в цепочке фильтров.
                // Без этой строки Security перехватит preflight OPTIONS-запрос и вернёт
                // 401 Unauthorized раньше, чем CorsConfig успеет ответить разрешением.
                // .cors() говорит Security: "пропусти CORS-обработку через MVC".
                //
                // Аналог Express: app.use(cors(...)) должен стоять ПЕРЕД app.use(authMiddleware).
                .cors(Customizer.withDefaults())

                // CSRF (Cross-Site Request Forgery) отключаем.
                //
                // CSRF-атаки возможны только при session-based аутентификации с cookies:
                // злоумышленник заставляет браузер отправить запрос с куками жертвы.
                //
                // У нас stateless JWT в заголовке Authorization — браузер не отправляет
                // его автоматически, поэтому CSRF-атака невозможна.
                //
                // В Express с JWT: app.use(cors()) без csrf-защиты — та же логика.
                .csrf(csrf -> csrf.disable())

                // Stateless сессии — сервер не создаёт HTTP-сессии.
                // Каждый запрос аутентифицируется заново через JWT.
                //
                // Преимущества:
                // - Горизонтальное масштабирование: любой сервер обработает запрос
                // - Нет проблем с синхронизацией сессий между серверами
                // - JWT содержит все нужные данные (id, role)
                //
                // В Express аналог: отказ от express-session в пользу JWT.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Правила доступа к эндпоинтам.
                // Правила проверяются по порядку — первое совпадение побеждает.
                .authorizeHttpRequests(auth -> auth
                        // /api/v1/auth/** — публичные эндпоинты регистрации и логина.
                        // "**" — любой путь после /auth/ (register, login, etc.)
                        // permitAll() — доступно всем без токена.
                        //
                        // AntPathRequestMatcher используем явно вместо строки, потому что
                        // requestMatchers(String) в Spring Security 6 при наличии Spring MVC
                        // использует MvcRequestMatcher — он требует зарегистрированный
                        // MVC-хендлер для совпадения. Если контроллера ещё нет (AuthController
                        // создаётся на этапе 8), matcher не сработает и запрос упадёт на
                        // anyRequest().authenticated() → 401.
                        // AntPathRequestMatcher работает по пути независимо от контроллеров.
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/auth/**")).permitAll()

                        // Swagger UI и OpenAPI спецификация — открываем для удобства разработки.
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/swagger-ui.html"),
                                AntPathRequestMatcher.antMatcher("/swagger-ui/**"),
                                AntPathRequestMatcher.antMatcher("/v3/api-docs/**")
                        ).permitAll()

                        // Все остальные запросы требуют аутентификации.
                        // authenticated() — нужен валидный JWT.
                        // Если токена нет — Spring вернёт 401 Unauthorized автоматически.
                        .anyRequest().authenticated()
                )

                // Явно указываем поведение при ошибках аутентификации.
                //
                // По умолчанию Spring Security 6 использует Http403ForbiddenEntryPoint —
                // возвращает 403 даже для неаутентифицированных запросов. Это неверная
                // семантика для REST API: 403 означает "нет прав", 401 — "нет токена".
                //
                // HttpStatusEntryPoint(UNAUTHORIZED) возвращает пустой 401 без тела.
                // Тело ошибки (JSON) добавим на этапе 8 в GlobalExceptionHandler.
                //
                // Аналог Express: res.status(401).json({ code: 'unauthorized' })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                // Добавляем наш JWT-фильтр в цепочку ПЕРЕД стандартным фильтром
                // UsernamePasswordAuthenticationFilter (обрабатывает form-login).
                //
                // Порядок важен: наш фильтр должен сработать первым, чтобы
                // установить аутентификацию до того, как Spring проверит права доступа.
                //
                // addFilterBefore(A, B) — добавить фильтр A перед фильтром B в цепочке.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}
