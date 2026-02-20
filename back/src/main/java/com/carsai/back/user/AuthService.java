package com.carsai.back.user;

import com.carsai.back.common.exception.EmailAlreadyExistsException;
import com.carsai.back.common.exception.InvalidCredentialsException;
import com.carsai.back.security.JwtTokenService;
import com.carsai.back.user.dto.AuthResponse;
import com.carsai.back.user.dto.LoginRequest;
import com.carsai.back.user.dto.RegisterRequest;
import com.carsai.back.user.dto.UserDto;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис аутентификации — бизнес-логика регистрации и логина.
 *
 * Service-слой содержит бизнес-правила и оркестрирует работу:
 * Controller (HTTP) → Service (логика) → Repository (БД)
 *
 * Аналог в Express: authService.js, который вызывается из authController.js.
 * Аналог в Angular: сервис с бизнес-логикой, внедрённый в компонент.
 *
 * @Service — помечает класс как Spring-компонент бизнес-логики.
 * Spring создаёт singleton и регистрирует в DI-контейнере.
 * Аналог Angular: @Injectable({ providedIn: 'root' })
 *
 * @RequiredArgsConstructor — Lombok генерирует конструктор из final-полей.
 * Spring видит конструктор и автоматически инжектит зависимости (DI).
 * Аналог Angular: constructor(private userRepo: UserRepository) в компоненте.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * JwtTokenService — создан на этапе 6.
     * Генерирует токен после успешной регистрации/логина.
     */
    private final JwtTokenService jwtTokenService;

    /**
     * Регистрация нового пользователя.
     *
     * Алгоритм:
     * 1. Нормализовать email (toLowerCase + trim), trim name
     * 2. Проверить уникальность email (бросить исключение, если занят)
     * 3. Захешировать пароль через BCrypt
     * 4. Создать Entity с ролью CLIENT
     * 5. Сохранить в БД
     * 6. Сгенерировать JWT и вернуть AuthResponse
     *
     * @Transactional — оборачивает метод в транзакцию БД.
     * Если что-то пойдёт не так (например, ошибка save()), все изменения откатятся.
     * В Express аналог: knex.transaction() → trx.commit() / trx.rollback().
     * Spring делает это автоматически через аннотацию.
     *
     * @param request DTO с именем, email и паролем
     * @return AuthResponse с UserDto и JWT-токеном
     * @throws EmailAlreadyExistsException если email уже занят
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Нормализация: email в нижний регистр и без пробелов по краям.
        // "Ivan@Example.COM " → "ivan@example.com"
        // Это предотвращает дублирование при разных регистрах.
        String normalizedEmail = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .name(request.name().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.CLIENT)
                .build();

        User saved = userRepository.save(user);
        return generateAuthResponse(saved);
    }

    /**
     * Аутентификация пользователя (логин).
     *
     * Алгоритм:
     * 1. Нормализовать email
     * 2. Найти пользователя по email
     * 3. Проверить пароль через BCrypt
     * 4. Сгенерировать JWT и вернуть AuthResponse
     *
     * Сообщение об ошибке намеренно общее — не раскрываем,
     * что именно неверно (email или пароль), чтобы не помогать перебору.
     *
     * @param request DTO с email и паролем
     * @return AuthResponse с UserDto и JWT-токеном
     * @throws InvalidCredentialsException если email не найден или пароль неверный
     */
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        // passwordEncoder.matches() сравнивает открытый пароль с BCrypt-хешем.
        // matches("password123", "$2a$10$Nq3...") → true/false
        // Нельзя "расшифровать" хеш — BCrypt однонаправленный.
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return generateAuthResponse(user);
    }

    /**
     * Генерация AuthResponse из User Entity.
     *
     * Конвертирует Entity в DTO и добавляет JWT-токен.
     * Выделен в приватный метод, так как используется и в register(), и в login().
     *
     * @param user Entity пользователя из БД
     * @return AuthResponse для отправки клиенту
     */
    private AuthResponse generateAuthResponse(User user) {
        String token = jwtTokenService.generateToken(user);
        return new AuthResponse(UserDto.from(user), token);
    }
}
