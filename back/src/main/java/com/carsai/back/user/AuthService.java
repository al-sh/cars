package com.carsai.back.user;

import com.carsai.back.common.exception.EmailAlreadyExistsException;
import com.carsai.back.common.exception.InvalidCredentialsException;
import com.carsai.back.user.dto.AuthResponse;
import com.carsai.back.user.dto.LoginRequest;
import com.carsai.back.user.dto.RegisterRequest;
import com.carsai.back.user.dto.UserDto;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
     * Регистрация нового пользователя.
     *
     * Алгоритм:
     * 1. Проверить уникальность email (бросить исключение, если занят)
     * 2. Захешировать пароль через BCrypt
     * 3. Создать Entity с ролью CLIENT
     * 4. Сохранить в БД
     * 5. Вернуть AuthResponse с данными пользователя и токеном
     *
     * @param request DTO с именем, email и паролем
     * @return AuthResponse с UserDto и токеном
     * @throws EmailAlreadyExistsException если email уже занят
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .name(request.name())
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
     * 1. Найти пользователя по email
     * 2. Проверить пароль через BCrypt
     * 3. Вернуть AuthResponse
     *
     * Сообщение об ошибке намеренно общее — не раскрываем,
     * что именно неверно (email или пароль), чтобы не помогать перебору.
     *
     * @param request DTO с email и паролем
     * @return AuthResponse с UserDto и токеном
     * @throws InvalidCredentialsException если email не найден или пароль неверный
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return generateAuthResponse(user);
    }

    /**
     * Генерация AuthResponse из User Entity.
     *
     * Приватный helper — конвертирует Entity в DTO и добавляет токен.
     * Токен пока заглушка — реальный JWT будет на этапе 6.
     *
     * @param user Entity пользователя из БД
     * @return AuthResponse для отправки клиенту
     */
    private AuthResponse generateAuthResponse(User user) {
        return new AuthResponse(
                UserDto.from(user),
                "jwt-placeholder"
        );
    }
}
