package com.carsai.back.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.carsai.back.common.exception.EmailAlreadyExistsException;
import com.carsai.back.common.exception.InvalidCredentialsException;
import com.carsai.back.security.JwtTokenService;
import com.carsai.back.user.dto.AuthResponse;
import com.carsai.back.user.dto.LoginRequest;
import com.carsai.back.user.dto.RegisterRequest;

/**
 * Unit-тесты для AuthService — тестируем бизнес-логику в изоляции от БД.
 *
 * @ExtendWith(MockitoExtension.class) — подключает Mockito к JUnit 5.
 * Mockito автоматически создаёт @Mock объекты и подставляет их в @InjectMocks.
 * Аналог Angular: TestBed.configureTestingModule({ providers: [AuthService, mockDeps] })
 *
 * Unit-тесты vs Integration-тесты:
 * - Unit: один класс в изоляции, зависимости заменены моками. Быстро.
 * - Integration: несколько слоёв вместе + реальная БД. Медленно, но надёжнее.
 *
 * Паттерн Given-When-Then (AAA: Arrange-Act-Assert):
 * - given/Arrange: настраиваем моки, создаём тестовые данные
 * - when/Act:      вызываем тестируемый метод
 * - then/Assert:   проверяем результат и взаимодействие с зависимостями
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Mock — создаёт мок-объект. Все методы по умолчанию возвращают null/0/false.
    // Поведение настраивается через when(...).thenReturn(...).
    // Аналог Jest: const userRepository = { existsByEmail: jest.fn() }
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    // @InjectMocks — создаёт AuthService и передаёт моки через конструктор.
    // Lombok @RequiredArgsConstructor генерирует конструктор из final-полей,
    // Mockito находит этот конструктор и подставляет @Mock объекты.
    @InjectMocks
    private AuthService authService;

    // ===== register() тесты =====

    @Test
    void register_shouldCreateUser_whenEmailIsNew() {
        // given
        var request = new RegisterRequest("Иван", "ivan@test.com", "password123");

        when(userRepository.existsByEmail("ivan@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed_password");

        // thenAnswer — возвращает динамический результат на основе аргументов.
        // Симулируем то, что делает JPA при INSERT:
        //   - @GeneratedValue устанавливает id
        //   - @PrePersist устанавливает createdAt
        // В Unit-тестах Hibernate не запускается, поэтому устанавливаем вручную.
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            return saved;
        });
        when(jwtTokenService.generateToken(any(User.class))).thenReturn("jwt_token_abc");

        // when
        AuthResponse response = authService.register(request);

        // then
        assertThat(response.user().email()).isEqualTo("ivan@test.com");
        assertThat(response.user().name()).isEqualTo("Иван");
        assertThat(response.user().role()).isEqualTo(UserRole.CLIENT);
        assertThat(response.token()).isEqualTo("jwt_token_abc");

        // verify — проверяем, что метод был вызван именно с такими аргументами.
        // Аналог Jest: expect(userRepository.save).toHaveBeenCalledWith(...)
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_shouldThrow_whenEmailExists() {
        // given — email уже занят
        var request = new RegisterRequest("Иван", "ivan@test.com", "password123");
        when(userRepository.existsByEmail("ivan@test.com")).thenReturn(true);

        // when & then — assertThatThrownBy проверяет, что метод бросает исключение.
        // Аналог Jest: expect(() => authService.register(request)).toThrow(EmailAlreadyExistsException)
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        // При дублирующемся email — пользователь НЕ должен сохраняться.
        // never() — метод не должен быть вызван ни разу.
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldNormalizeEmail_beforeSaving() {
        // given — email с пробелами и верхним регистром
        var request = new RegisterRequest("Иван", "  IVAN@TEST.COM  ", "password123");

        when(userRepository.existsByEmail("ivan@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            return saved;
        });
        when(jwtTokenService.generateToken(any())).thenReturn("token");

        // when
        authService.register(request);

        // then — проверяем, что email нормализован ПЕРЕД проверкой уникальности:
        // "  IVAN@TEST.COM  " → toLowerCase().trim() → "ivan@test.com"
        verify(userRepository).existsByEmail("ivan@test.com");
    }

    // ===== login() тесты =====

    @Test
    void login_shouldReturnToken_whenValidCredentials() {
        // given
        var request = new LoginRequest("ivan@test.com", "password123");

        // User.builder() — паттерн Builder (Lombok @Builder).
        // Аналог JS: const user = { id: uuid(), email: "ivan@test.com", ... }
        var user = User.builder()
                .id(UUID.randomUUID())
                .email("ivan@test.com")
                .name("Иван")
                .passwordHash("$2a$10$hashed_password")
                .role(UserRole.CLIENT)
                .createdAt(Instant.now())
                .deleted(false)
                .build();

        when(userRepository.findByEmail("ivan@test.com")).thenReturn(Optional.of(user));
        // matches(plaintext, hash) → true — пароль верный
        when(passwordEncoder.matches("password123", "$2a$10$hashed_password")).thenReturn(true);
        when(jwtTokenService.generateToken(user)).thenReturn("jwt_login_token");

        // when
        AuthResponse response = authService.login(request);

        // then
        assertThat(response.user().email()).isEqualTo("ivan@test.com");
        assertThat(response.token()).isEqualTo("jwt_login_token");
    }

    @Test
    void login_shouldThrow_whenEmailNotFound() {
        // given — пользователь с таким email не существует
        var request = new LoginRequest("notfound@test.com", "password123");
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        // При несуществующем email — токен не генерируем
        verify(jwtTokenService, never()).generateToken(any());
    }

    @Test
    void login_shouldThrow_whenPasswordWrong() {
        // given
        var request = new LoginRequest("ivan@test.com", "wrongpassword");
        var user = User.builder()
                .id(UUID.randomUUID())
                .email("ivan@test.com")
                .name("Иван")
                .passwordHash("$2a$10$hashed_password")
                .role(UserRole.CLIENT)
                .createdAt(Instant.now())
                .deleted(false)
                .build();

        when(userRepository.findByEmail("ivan@test.com")).thenReturn(Optional.of(user));
        // matches(wrongpassword, hash) → false — пароль неверный
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashed_password")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        // При неверном пароле — JWT не выдаём
        verify(jwtTokenService, never()).generateToken(any());
    }
}
