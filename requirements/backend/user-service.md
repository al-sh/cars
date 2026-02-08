# User Service

> **Зависимости:** CONTEXT.md, contracts/api.md, contracts/types.md, infrastructure/database.md

Сервис аутентификации и управления пользователями.

---

## Ответственность

- Регистрация и аутентификация пользователей
- Управление JWT токенами
- CRUD пользователей (для менеджера)
- Управление ролями

---

## Архитектура

```
┌─────────────────────────────────────────────────────────┐
│                      Controller                          │
│  AuthController              UserController              │
└─────────────────┬───────────────────┬───────────────────┘
                  │                   │
┌─────────────────▼───────────────────▼───────────────────┐
│                       Service                            │
│  AuthService                 UserService                 │
│       │                                                  │
│       ▼                                                  │
│  JwtTokenService                                         │
└─────────────────┬───────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────┐
│                     Repository                           │
│  UserRepository                                        │
└─────────────────────────────────────────────────────────┘
                              │
                              ▼
                         PostgreSQL
```

---

## Слой Controller

### AuthController

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
    
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
```

### UserController

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    public PagedResponse<UserDto> getUsers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int perPage,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) UserRole role
    ) {
        return userService.getUsers(page, perPage, search, role);
    }
    
    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable UUID id) {
        return userService.getUser(id);
    }
    
    @GetMapping("/{id}/chats")
    public PagedResponse<ChatDto> getUserChats(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int perPage
    ) {
        return userService.getUserChats(id, page, perPage);
    }
    
    @PatchMapping("/{id}")
    public UserDto updateUser(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.updateUser(id, request);
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
    }
}
```

---

## Слой Service

### AuthService

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Проверить, что email не занят
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException();
        }
        
        // 2. Создать пользователя
        User user = User.builder()
            .email(request.email().toLowerCase().trim())
            .name(request.name().trim())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(UserRole.CLIENT) // По умолчанию клиент
            .build();
        
        user = userRepository.save(user);
        
        // 3. Сгенерировать JWT токен
        return generateAuthResponse(user);
    }
    
    public AuthResponse login(LoginRequest request) {
        // 1. Найти пользователя
        User user = userRepository.findByEmail(request.email().toLowerCase())
            .orElseThrow(InvalidCredentialsException::new);
        
        // 2. Проверить пароль
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        
        // 3. Сгенерировать JWT токен
        return generateAuthResponse(user);
    }
    
    private AuthResponse generateAuthResponse(User user) {
        return new AuthResponse(
            UserDto.from(user),
            jwtTokenService.generateToken(user)
        );
    }
}
```

### JwtTokenService

```java
@Service
public class JwtTokenService {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.token-expiration}")
    private Duration tokenExpiration; // например, 7 дней
    
    public String generateToken(User user) {
        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            // В JSON контракте роли передаются lower-case, поэтому кладём lower-case.
            .claim("role", user.getRole().name().toLowerCase())
            .setIssuedAt(new Date())
            .setExpiration(Date.from(Instant.now().plus(tokenExpiration)))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }
        
    public UserPrincipal validateToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        
        return new UserPrincipal(
            UUID.fromString(claims.getSubject()),
            claims.get("email", String.class),
            // Claim role приходит lower-case, а Java enum — UPPER_CASE
            UserRole.valueOf(claims.get("role", String.class).toUpperCase())
        );
    }
    
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
```

### UserService

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    
    public PagedResponse<UserDto> getUsers(int page, int perPage, String search, UserRole role) {
        Specification<User> spec = Specification.where(null);
        
        if (search != null && !search.isBlank()) {
            spec = spec.and(UserSpecifications.searchByNameOrEmail(search));
        }
        
        if (role != null) {
            spec = spec.and(UserSpecifications.hasRole(role));
        }
        
        Page<User> users = userRepository.findAll(spec, 
            PageRequest.of(page - 1, perPage, Sort.by("createdAt").descending()));
        
        return PagedResponse.from(users, UserDto::from);
    }
    
    public UserDto getUser(UUID id) {
        return userRepository.findById(id)
            .map(UserDto::from)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
    
    public PagedResponse<ChatDto> getUserChats(UUID userId, int page, int perPage) {
        // Проверить существование пользователя
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        
        Page<Chat> chats = chatRepository.findByUserId(userId,
            PageRequest.of(page - 1, perPage, Sort.by("updatedAt").descending()));
        
        return PagedResponse.from(chats, ChatDto::from);
    }
    
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        
        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        
        return UserDto.from(userRepository.save(user));
    }
    
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        
        user.setDeleted(true);
        userRepository.save(user);
    }
}
```

---

## Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/users/**").hasRole("MANAGER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### JwtAuthenticationFilter

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenService jwtTokenService;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                UserPrincipal principal = jwtTokenService.validateToken(token);
                
                var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException e) {
                // Token invalid - continue without authentication
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## Валидация

```java
public record RegisterRequest(
    @NotBlank(message = "Имя обязательно")
    @Size(min = 2, max = 100, message = "Имя от 2 до 100 символов")
    String name,
    
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    String email,
    
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 100, message = "Пароль минимум 6 символов")
    String password
) {}

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}

public record UpdateUserRequest(
    @Size(min = 2, max = 100)
    String name
) {}
```

---

## Конфигурация

```yaml
# application.yml

jwt:
  secret: ${JWT_SECRET}
  token-expiration: 7d

spring:
  security:
    user:
      password: ${ADMIN_PASSWORD:admin}  # Для создания первого admin
```

---

## Обработка ошибок

```java
@RestControllerAdvice
public class AuthExceptionHandler {
    
    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailExists(EmailAlreadyExistsException ex) {
        return ErrorResponse.of("email_exists", 
            "Пользователь с таким email уже существует");
    }
    
    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentials(InvalidCredentialsException ex) {
        return ErrorResponse.of("invalid_credentials", 
            "Неверный email или пароль");
    }
    
    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidToken(InvalidTokenException ex) {
        return ErrorResponse.of("invalid_token", 
            "Токен недействителен или истёк");
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return ErrorResponse.of("access_denied", 
            "Недостаточно прав для выполнения операции");
    }
}
```

---

## Безопасность

### Пароли
- BCrypt с cost factor 10
- Минимум 6 символов
- Не логировать пароли

### JWT
- HS256 алгоритм
- Secret минимум 256 бит
- Один JWT токен для MVP (без refresh)

### Защита от атак
- Rate limiting на /auth/* endpoints
- Не раскрывать, существует ли email (одинаковая ошибка)
- Secure headers (via Spring Security)
