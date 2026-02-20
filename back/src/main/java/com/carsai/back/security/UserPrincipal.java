package com.carsai.back.security;

import com.carsai.back.user.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Объект аутентифицированного пользователя в контексте одного запроса.
 *
 * Создаётся из JWT-claims в JwtTokenService.validateToken()
 * и помещается в SecurityContextHolder на время запроса.
 *
 * Реализует UserDetails — стандартный интерфейс Spring Security.
 * Spring использует его в:
 * - SecurityContextHolder: хранит текущего пользователя
 * - @AuthenticationPrincipal: внедряет в методы контроллеров
 * - UsernamePasswordAuthenticationToken: оборачивает при создании Authentication
 *
 * Аналогия Express: объект req.user, который устанавливает passport.js middleware.
 * Аналогия Angular: сигнал authService.user() — кто сейчас залогинен.
 *
 * ВАЖНО: UserPrincipal содержит только данные из JWT (id, email, role).
 * Он НЕ ходит в базу данных при каждом запросе — именно в этом смысл JWT.
 */
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final UserRole role;

    public UserPrincipal(UUID id, String email, UserRole role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    /** UUID пользователя — используется в сервисах для фильтрации данных по владельцу. */
    public UUID getId() {
        return id;
    }

    /**
     * Права/роли пользователя для Spring Security.
     *
     * "ROLE_" prefix — соглашение Spring Security.
     * @PreAuthorize("hasRole('ADMIN')") ищет "ROLE_ADMIN" в authorities.
     * SimpleGrantedAuthority — простая реализация: просто строка.
     *
     * В нашем случае у каждого пользователя одна роль: CLIENT, MANAGER или ADMIN.
     *
     * Аналог в Express: req.user.role — проверяется вручную в middleware.
     * Аналог в Angular: authService.user()?.role — проверяется в guard-ах.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Имя пользователя для Spring Security — используем email.
     * В нашей системе email уникален и служит идентификатором.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Пароль не нужен — аутентификация через JWT, не через пароль.
     * Spring Security требует этот метод, возвращаем null.
     */
    @Override
    public String getPassword() {
        return null;
    }

    // Методы ниже — флаги состояния аккаунта.
    // В MVP все пользователи активны. При необходимости можно подключить
    // поле в БД (например, banned) и возвращать false здесь.

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
