# План разработки Backend на Spring Boot

## Цель

Разработать backend API пошагово, изучая Java 21 и Spring Boot 3 параллельно. Скоуп первой фазы: **аутентификация + чаты/сообщения** (без LLM и каталога автомобилей). Каждый этап занимает 30–60 минут.

---

## Стиль комментариев и объяснений

**ВАЖНО:** При реализации каждого этапа необходимо писать подробные комментарии в коде и давать развёрнутые объяснения, как будто senior объясняет джуну.

### Комментарии в коде

1. **Аннотации Spring:**
   - Объяснять назначение каждой аннотации (`@RestController`, `@Service`, `@Entity`, `@Transactional`)
   - Пояснять параметры (`@Column(nullable = false)`, `@RequestParam(defaultValue = "20")`)
   - Сравнивать с аналогами из Angular и Node.js/Express

2. **Архитектурные решения:**
   - Почему DTO отделены от Entity
   - Зачем нужны слои Controller → Service → Repository
   - Почему используем records для DTO

3. **Spring Security:**
   - Объяснять filter chain и как запрос проходит через фильтры
   - Зачем stateless сессии для REST API
   - Как JWT заменяет сессии

### Принципы

- **Подробность:** Лучше больше объяснений, чем меньше
- **Контекст:** Всегда объяснять "почему", а не только "что"
- **Сравнения:** Использовать аналогии с Angular-фронтендом и Node.js/Express, где уместно
- **Примеры:** Приводить примеры запросов/ответов для каждого эндпоинта

---

## Стек

| Технология      | Версия | Назначение                   |
| --------------- | ------ | ---------------------------- |
| Java            | 21     | Язык                         |
| Spring Boot     | 3.x    | Фреймворк                    |
| Maven           | —      | Сборка (pom.xml)             |
| PostgreSQL      | 16     | База данных (через Docker)   |
| Flyway          | —      | Миграции БД                  |
| Spring Data JPA | —      | ORM (Hibernate)              |
| Spring Security | —      | Аутентификация и авторизация |
| JJWT            | 0.12.x | Генерация и валидация JWT    |
| Bean Validation | —      | Валидация входных данных     |
| Lombok          | —      | Сокращение boilerplate-кода  |

---

## Архитектура (слоистая)

```
┌─────────────────────────────────────────────────┐
│                   Controller                      │
│  Принимает HTTP-запросы, валидирует входные       │
│  данные, вызывает Service, возвращает DTO          │
│  Аналогия Angular: роутинг + компонент-контейнер  │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│                    Service                        │
│  Бизнес-логика: проверки, преобразования,         │
│  оркестрация. Работает с Entity, возвращает DTO   │
│  Аналогия Angular: ChatService, AuthService       │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│                   Repository                      │
│  Доступ к БД через Spring Data JPA.               │
│  Интерфейс — Spring генерирует реализацию         │
│  Аналогия Angular: моки из mock-data.ts           │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
                PostgreSQL
```

**Почему слоистая архитектура?**

- **Controller** не знает про БД — только принимает/отдаёт данные
- **Service** не знает про HTTP — только бизнес-логика
- **Repository** не знает про бизнес-правила — только SQL-запросы
- Можно тестировать каждый слой отдельно

---

## Структура проекта

```
back/
├── pom.xml                          # Maven — описание зависимостей
├── docker-compose.yml               # PostgreSQL для разработки
├── src/
│   ├── main/
│   │   ├── java/com/carsai/back/
│   │   │   ├── BackApplication.java # Точка входа Spring Boot
│   │   │   │
│   │   │   ├── config/              # Конфигурация Spring
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── CorsConfig.java
│   │   │   │
│   │   │   ├── security/            # JWT и фильтры
│   │   │   │   ├── JwtTokenService.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── UserPrincipal.java
│   │   │   │
│   │   │   ├── user/                # Модуль пользователей
│   │   │   │   ├── User.java              # Entity
│   │   │   │   ├── UserRole.java          # Enum
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── AuthController.java
│   │   │   │   └── dto/
│   │   │   │       ├── RegisterRequest.java
│   │   │   │       ├── LoginRequest.java
│   │   │   │       ├── AuthResponse.java
│   │   │   │       └── UserDto.java
│   │   │   │
│   │   │   ├── chat/                # Модуль чатов
│   │   │   │   ├── Chat.java              # Entity
│   │   │   │   ├── ChatRepository.java
│   │   │   │   ├── ChatService.java
│   │   │   │   ├── ChatController.java
│   │   │   │   └── dto/
│   │   │   │       ├── ChatDto.java
│   │   │   │       ├── CreateChatRequest.java
│   │   │   │       └── UpdateChatRequest.java
│   │   │   │
│   │   │   ├── message/             # Модуль сообщений
│   │   │   │   ├── Message.java           # Entity
│   │   │   │   ├── MessageRole.java       # Enum
│   │   │   │   ├── MessageRepository.java
│   │   │   │   ├── MessageService.java
│   │   │   │   ├── MessageController.java
│   │   │   │   └── dto/
│   │   │   │       ├── MessageDto.java
│   │   │   │       └── SendMessageRequest.java
│   │   │   │
│   │   │   ├── common/              # Общие классы
│   │   │   │   ├── dto/
│   │   │   │   │   ├── PagedResponse.java
│   │   │   │   │   ├── CursorResponse.java
│   │   │   │   │   └── ErrorResponse.java
│   │   │   │   └── exception/
│   │   │   │       ├── GlobalExceptionHandler.java
│   │   │   │       ├── EmailAlreadyExistsException.java
│   │   │   │       ├── InvalidCredentialsException.java
│   │   │   │       ├── InvalidTokenException.java
│   │   │   │       ├── ChatNotFoundException.java
│   │   │   │       └── MessageNotFoundException.java
│   │   │   │
│   │   │   └── ... (будущие модули: car/, llm/)
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/        # Flyway миграции
│   │           ├── V1__create_users_table.sql
│   │           ├── V2__create_chats_table.sql
│   │           └── V3__create_messages_table.sql
│   │
│   └── test/java/com/carsai/back/
│       ├── user/
│       │   ├── AuthServiceTest.java
│       │   └── AuthControllerTest.java
│       ├── chat/
│       │   ├── ChatServiceTest.java
│       │   └── ChatControllerTest.java
│       └── message/
│           └── MessageServiceTest.java
```

**Почему package-by-feature, а не package-by-layer?**

```
# ❌ package-by-layer (всё вперемешку):
controller/AuthController.java
controller/ChatController.java
service/AuthService.java
service/ChatService.java
entity/User.java
entity/Chat.java

# ✅ package-by-feature (модули):
user/AuthController.java
user/AuthService.java
user/User.java
chat/ChatController.java
chat/ChatService.java
chat/Chat.java
```

Package-by-feature группирует связанный код вместе. Открыл папку `user/` — видишь всё, что связано с пользователями. Не нужно прыгать между десятью папками.

---

## Этапы разработки

### Этап 1: Инициализация проекта и PostgreSQL

**Время:** ~45 мин

**Цель:** Создать Spring Boot проект, настроить PostgreSQL через Docker, убедиться что приложение запускается.

**Задачи:**

1. Создать проект через Spring Initializr (start.spring.io) или вручную:
   - Group: `com.carsai`
   - Artifact: `back`
   - Java 21, Spring Boot 3.x
   - Зависимости: Spring Web, Spring Data JPA, PostgreSQL Driver, Flyway, Lombok, Validation

2. Настроить `pom.xml` с зависимостями

3. Создать `docker-compose.yml` для PostgreSQL:

   ```yaml
   services:
     postgres:
       image: postgres:16-alpine
       environment:
         POSTGRES_DB: carsai
         POSTGRES_USER: carsai
         POSTGRES_PASSWORD: carsai
       ports:
         - "5432:5432"
       volumes:
         - postgres_data:/var/lib/postgresql/data
   volumes:
     postgres_data:
   ```

4. Настроить `application.yml`:

   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/carsai
       username: carsai
       password: carsai
     jpa:
       hibernate:
         ddl-auto: validate # Flyway управляет схемой
     flyway:
       enabled: true
   ```

5. Создать пустую миграцию `V1__init.sql` (чтобы Flyway не падал)

6. Проверить: `mvn spring-boot:run` → приложение стартует, подключается к PostgreSQL

**Изучаемые концепции:**

- **Maven (pom.xml):** Аналог `package.json` в Node.js. Описывает зависимости, версии, плагины сборки. `<dependencies>` — это как `dependencies` в package.json. `mvn` — как `npm`. `mvn spring-boot:run` — как `npm start`.
- **Spring Boot Starter:** `spring-boot-starter-web` тянет за собой всё для REST API (встроенный Tomcat, Jackson для JSON, Spring MVC). Это как `ng add` в Angular или `npm install express body-parser cors` в Node.js — одна строка вместо десяти. Tomcat — встроенный HTTP-сервер, аналог того, что Express сам слушает порт.
- **application.yml:** Конфигурация приложения. Аналог `environment.ts` в Angular или `.env` + `dotenv` в Express. Spring автоматически читает этот файл при запуске.
- **Docker Compose:** Поднимает PostgreSQL одной командой. Не нужно устанавливать БД на свою машину.
- **Flyway:** Инструмент миграций БД. Каждый файл `V{N}__description.sql` выполняется ровно один раз, по порядку. Flyway запоминает, какие миграции уже применены. Аналог `knex migrate` или `prisma migrate` в Node.js — git для схемы базы данных.

---

### Этап 2: Entity и миграции для пользователей

**Время:** ~45 мин

**Цель:** Создать таблицу `users` в БД, описать JPA Entity.

**Задачи:**

1. Flyway миграция `V1__create_users_table.sql`:

   ```sql
   CREATE TABLE users (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       email VARCHAR(255) NOT NULL,
       name VARCHAR(255) NOT NULL,
       password_hash VARCHAR(255) NOT NULL,
       role VARCHAR(20) NOT NULL DEFAULT 'CLIENT',
       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
       deleted BOOLEAN NOT NULL DEFAULT false,
       CONSTRAINT uk_users_email UNIQUE (email),
       CONSTRAINT chk_users_role CHECK (role IN ('CLIENT', 'MANAGER', 'ADMIN'))
   );
   CREATE INDEX idx_users_email ON users(email);
   ```

2. Создать enum `UserRole`:

   ```java
   public enum UserRole {
       CLIENT, MANAGER, ADMIN
   }
   ```

3. Создать Entity `User`:

   ```java
   @Entity                           // Говорит JPA: этот класс — таблица в БД
   @Table(name = "users")            // Имя таблицы (по умолчанию было бы "user", но это зарезервированное слово в SQL)
   @Data                             // Lombok: генерирует getters, setters, toString, equals, hashCode
   @NoArgsConstructor                // Lombok: конструктор без аргументов (нужен JPA)
   @AllArgsConstructor               // Lombok: конструктор со всеми аргументами
   @Builder                          // Lombok: паттерн Builder для удобного создания объектов
   public class User {
       @Id                           // Первичный ключ
       @GeneratedValue(strategy = GenerationType.UUID)  // БД сама генерирует UUID
       private UUID id;

       @Column(nullable = false, unique = true)
       private String email;
       // ... остальные поля
   }
   ```

4. Проверить: приложение стартует, таблицы созданы в PostgreSQL

**Изучаемые концепции:**

- **Entity:** Класс Java, который маппится на таблицу в БД. Каждое поле — колонка. Каждый объект — строка. JPA (Hibernate) автоматически конвертирует между Java-объектами и SQL. Аналог модели в Prisma (`model User { id String @id }`) или Mongoose-схемы в Node.js, но с аннотациями вместо отдельного файла схемы.
- **Lombok:** Библиотека, которая генерирует boilerplate-код (getters/setters/конструкторы) во время компиляции. `@Data` заменяет ~50 строк кода. В JavaScript/TypeScript этой проблемы нет — объекты и так доступны напрямую. Lombok компенсирует многословность Java.
- **@Builder:** Паттерн для создания объектов: `User.builder().email("a@b.com").name("Иван").build()` — читабельнее, чем конструктор с 7 параметрами. В JS аналог — передача объекта: `new User({ email: "a@b.com", name: "Иван" })`.
- **UUID vs auto-increment:** UUID безопаснее (нельзя перебрать ID), не зависит от сервера (можно генерировать на клиенте). `gen_random_uuid()` — встроенная функция PostgreSQL. На фронте мы тоже используем `crypto.randomUUID()`.

---

### Этап 3: Repository слой

**Время:** ~30 мин

**Цель:** Создать интерфейсы для доступа к данным.

**Задачи:**

1. Создать `UserRepository`:

   ```java
   // Интерфейс! Не класс. Spring Data сам создаёт реализацию.
   // JpaRepository<User, UUID> — работаем с Entity "User", тип первичного ключа — UUID.
   // Получаем бесплатно: save(), findById(), findAll(), delete(), count() и другие.
   @Repository
   public interface UserRepository extends JpaRepository<User, UUID> {
       // Магия Spring Data: по имени метода генерирует SQL.
       // findByEmail → SELECT * FROM users WHERE email = ?
       Optional<User> findByEmail(String email);

       // existsByEmail → SELECT COUNT(*) > 0 FROM users WHERE email = ?
       boolean existsByEmail(String email);
   }
   ```

2. Проверить: приложение стартует без ошибок

**Изучаемые концепции:**

- **Spring Data JPA:** Ты пишешь интерфейс, Spring пишет реализацию. `findByEmail(String email)` — Spring разбирает имя метода, понимает "найти по полю email" и генерирует SQL. В Node.js аналог — Prisma (`prisma.user.findUnique({ where: { email } })`) или Mongoose (`User.findOne({ email })`), но в Spring даже метод писать не надо — достаточно объявить сигнатуру.
- **JpaRepository:** Базовый интерфейс с ~20 готовыми методами (`save`, `findById`, `findAll`, `delete`, `count`). Не нужно писать ни строки SQL для стандартных операций. Как готовый CRUD в Prisma Client.
- **Optional:** Java-класс для обработки null. `findByEmail()` возвращает `Optional<User>` вместо `User | null`. Заставляет явно обрабатывать случай "не нашли". Аналогия: как `strictNullChecks` в TypeScript или optional chaining `?.` в JS.
- **Derived Query Methods:** Spring генерирует запрос из имени метода. `findByEmailAndDeletedFalse` → `WHERE email = ? AND deleted = false`. Если ошибёшься в имени поля — ошибка при запуске (быстрая обратная связь). В Express пришлось бы писать raw SQL или query builder.

---

### Этап 4: DTO и валидация

**Время:** ~30 мин

**Цель:** Создать объекты запросов/ответов с валидацией.

**Задачи:**

1. Создать request DTO как Java records:

   ```java
   // Java record — компактный класс для хранения данных.
   // Автоматически создаёт: конструктор, getters, equals, hashCode, toString.
   // Аналогия: как `type` в TypeScript, но с конструктором.
   //
   // Это DTO (Data Transfer Object) — объект для передачи данных
   // между клиентом и сервером. Отделён от Entity, потому что:
   // 1. Entity содержит passwordHash — его нельзя отдавать клиенту
   // 2. Клиент отправляет password — его нет в Entity
   // 3. DTO определяет контракт API, Entity — структуру БД
   public record RegisterRequest(
       // @NotBlank — не null, не пустая строка, не только пробелы
       // message — текст ошибки, который вернётся клиенту
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
   ```

2. Создать `LoginRequest`

3. Создать response DTO:

   ```java
   public record AuthResponse(
       UserDto user,
       String token
   ) {}

   public record UserDto(
       UUID id,
       String email,
       String name,
       UserRole role,
       Instant createdAt
   ) {
       // Статический фабричный метод для конвертации Entity → DTO
       public static UserDto from(User user) {
           return new UserDto(
               user.getId(),
               user.getEmail(),
               user.getName(),
               user.getRole(),
               user.getCreatedAt()
           );
       }
   }
   ```

4. Создать `ErrorResponse`:

   ```java
   public record ErrorResponse(
       String code,
       String message
   ) {
       public static ErrorResponse of(String code, String message) {
           return new ErrorResponse(code, message);
       }
   }
   ```

5. Создать `PagedResponse<T>` и `CursorResponse<T>`

**Изучаемые концепции:**

- **Java Records (Java 16+):** Иммутабельные классы данных. `record UserDto(UUID id, String name)` — это ~30 строк обычного класса в одной строке. Как `type` в TypeScript или простой объект в JS `{ id, name }`, но records — полноценные классы (можно добавлять методы).
- **DTO паттерн:** Entity — внутренняя модель (с passwordHash, deleted). DTO — внешняя модель (только то, что видит клиент). В Express ты бы делал то же самое: `const { passwordHash, ...userDto } = user` перед отправкой ответа. В Angular: модель `Chat` на фронте не совпадает со схемой БД. DTO защищает от случайной утечки данных.
- **Bean Validation:** Аннотации `@NotBlank`, `@Email`, `@Size` автоматически проверяют данные. Если валидация не прошла — Spring вернёт 400 без вашего кода. Как `Validators.required` и `Validators.email` в Angular Reactive Forms. В Express аналог — `express-validator` или `joi`: `body('email').isEmail()`, но в Spring валидация встроена через аннотации.

---

### Этап 5: AuthService — регистрация и логин

**Время:** ~45 мин

**Цель:** Реализовать бизнес-логику аутентификации.

**Задачи:**

1. Создать кастомные исключения:

   ```java
   // RuntimeException — unchecked exception.
   // Не нужно объявлять в throws — Spring сам поймает через @RestControllerAdvice.
   public class EmailAlreadyExistsException extends RuntimeException {
       public EmailAlreadyExistsException() {
           super("Пользователь с таким email уже существует");
       }
   }

   public class InvalidCredentialsException extends RuntimeException { ... }
   public class InvalidTokenException extends RuntimeException { ... }
   ```

2. Создать `AuthService`:

   ```java
   @Service  // Помечает класс как Spring-сервис (бизнес-логика).
             // Spring создаёт один экземпляр и внедряет через DI.
             // Аналогия: @Injectable({ providedIn: 'root' }) в Angular.
   @RequiredArgsConstructor  // Lombok: конструктор из final-полей. Spring через него внедрит зависимости.
   public class AuthService {

       // DI через конструктор — Spring автоматически передаст реализацию.
       // В Angular аналог: private chatService = inject(ChatService);
       private final UserRepository userRepository;
       private final PasswordEncoder passwordEncoder;
       // JwtTokenService создадим на следующем этапе
       // private final JwtTokenService jwtTokenService;

       // @Transactional — если что-то пойдёт не так в середине метода,
       // все изменения в БД откатятся. Как ctrl+Z для базы данных.
       @Transactional
       public AuthResponse register(RegisterRequest request) {
           // 1. Проверить уникальность email
           if (userRepository.existsByEmail(request.email())) {
               throw new EmailAlreadyExistsException();
           }

           // 2. Создать пользователя с хешированным паролем
           User user = User.builder()
               .email(request.email().toLowerCase().trim())
               .name(request.name().trim())
               .passwordHash(passwordEncoder.encode(request.password()))
               .role(UserRole.CLIENT)
               .build();

           user = userRepository.save(user);

           // 3. Сгенерировать JWT токен (пока заглушка)
           return generateAuthResponse(user);
       }

       public AuthResponse login(LoginRequest request) {
           // 1. Найти пользователя по email
           User user = userRepository.findByEmail(request.email().toLowerCase())
               .orElseThrow(InvalidCredentialsException::new);
           // 2. Проверить пароль
           // 3. Сгенерировать токены
       }
   }
   ```

3. Зарегистрировать `PasswordEncoder` bean (временно в отдельном конфиге):

   ```java
   @Configuration
   public class SecurityConfig {
       @Bean
       public PasswordEncoder passwordEncoder() {
           return new BCryptPasswordEncoder();
       }
   }
   ```

4. Проверить: написать простой тест или вызвать метод из main

**Изучаемые концепции:**

- **Dependency Injection (DI):** Spring сам создаёт объекты и передаёт в конструктор. Ты пишешь `private final UserRepository userRepository` — Spring находит реализацию и подставляет. В Angular то же самое: `private chatService = inject(ChatService)`. В Express DI обычно нет — ты вручную импортируешь модули (`const userRepo = require('./userRepo')`). Spring DI делает код более тестируемым: в тестах легко подменить реальный репозиторий на мок.
- **@Transactional:** Оборачивает метод в транзакцию БД. Если внутри метода бросилось исключение — все изменения откатятся. В Express пришлось бы вручную: `const trx = await knex.transaction()` → `trx.commit()` / `trx.rollback()`. Spring делает это автоматически через аннотацию.
- **BCrypt:** Алгоритм хеширования паролей. `encode("password123")` → `"$2a$10$Nq3..."` (60 символов). Необратимый — нельзя восстановить пароль из хеша. `matches("password123", hash)` → `true`. Каждый раз другой хеш — даже для одного пароля (соль). В Node.js аналог — пакет `bcrypt`: `bcrypt.hash(password, 10)` и `bcrypt.compare(password, hash)`.
- **Builder паттерн:** `User.builder().email("a@b.com").name("Иван").build()` — создаём объект пошагово. Читабельнее, чем `new User(null, "a@b.com", "Иван", null, "CLIENT", ...)`. В JS просто передали бы объект `{ email: "a@b.com", name: "Иван" }`. Lombok генерирует Builder из `@Builder`.

---

### Этап 6: JWT-токены

**Время:** ~45 мин

**Цель:** Реализовать генерацию и валидацию JWT-токенов.

**Задачи:**

1. Добавить зависимость JJWT в pom.xml

2. Создать `JwtTokenService`:

   ```java
   @Service
   public class JwtTokenService {

       @Value("${jwt.secret}")  // Читаем значение из application.yml
       private String secret;

       @Value("${jwt.token-expiration}")
       private Duration tokenExpiration;  // например, 7 дней

       // Генерация JWT токена для доступа к API.
       // Клиент отправляет его в заголовке Authorization: Bearer <token>
       public String generateToken(User user) {
           return Jwts.builder()
               .subject(user.getId().toString())          // Кто владелец токена
               .claim("email", user.getEmail())            // Доп. данные
               // В JSON контракте роли/enum — строки lower-case, поэтому кладём в claim lower-case.
               .claim("role", user.getRole().name().toLowerCase()) // Роль для авторизации
               .issuedAt(new Date())                       // Когда создан
               .expiration(Date.from(Instant.now().plus(tokenExpiration)))  // Когда истечёт
               .signWith(getSigningKey())                  // Подпись ключом
               .compact();                                 // Собрать в строку
       }

       // Валидация — проверяет подпись и срок действия.
       // Если токен подделан или истёк — бросает JwtException.
       public UserPrincipal validateToken(String token) {
           Claims claims = Jwts.parser()
               .verifyWith(getSigningKey())
               .build()
               .parseSignedClaims(token)
               .getPayload();
           // ...вернуть UserPrincipal из claims
       }
   }
   ```

3. Создать `UserPrincipal` — объект текущего пользователя в контексте запроса

4. Настроить `jwt` секцию в `application.yml`:

   ```yaml
   jwt:
     # Важно: значение должно приходить из env. Не хардкодим «осмысленный» дефолт,
     # чтобы случайно не утащить его в прод.
     secret: ${JWT_SECRET}
     token-expiration: 7d
   ```

5. Интегрировать `JwtTokenService` в `AuthService.generateAuthResponse()`

**Изучаемые концепции:**

- **JWT (JSON Web Token):** Токен из трёх частей: `header.payload.signature`. Header — алгоритм подписи. Payload — данные (userId, role, expiration). Signature — подпись секретным ключом. Сервер подписывает токен при логине. При каждом запросе клиент отправляет токен — сервер проверяет подпись и извлекает данные. Не нужно хранить сессию на сервере. В Node.js используется пакет `jsonwebtoken`: `jwt.sign(payload, secret)` и `jwt.verify(token, secret)`. В Java — библиотека JJWT с builder-паттерном.
- **Один токен:** Для MVP используем один JWT токен. Это проще в реализации, но с компромиссом по безопасности (при компрометации токен живёт до истечения срока). Если понадобится — позже можно вернуться к access/refresh.
- **@Value:** Инъекция значения из конфигурации. `@Value("${jwt.secret}")` — Spring подставит значение из application.yml. `${JWT_SECRET:default}` — сначала ищет переменную окружения, если нет — берёт default. Аналогия: `environment.ts` в Angular или `process.env.JWT_SECRET || 'default'` в Express.
- **HMAC подпись:** `signWith(key)` подписывает токен секретным ключом. Если кто-то изменит payload — подпись не совпадёт, и валидация упадёт. Как цифровая печать на документе.

---

### Этап 7: Spring Security и JWT-фильтр

**Время:** ~60 мин

**Цель:** Настроить Spring Security для JWT-аутентификации.

**Задачи:**

1. Создать `JwtAuthenticationFilter`:

   ```java
   // OncePerRequestFilter — гарантирует, что фильтр выполнится
   // ровно один раз на каждый HTTP-запрос.
   //
   // Это как middleware в Express.js или HTTP Interceptor в Angular.
   // Перехватывает каждый запрос ПЕРЕД тем, как он попадёт в Controller.
   @Component
   @RequiredArgsConstructor
   public class JwtAuthenticationFilter extends OncePerRequestFilter {

       private final JwtTokenService jwtTokenService;

       @Override
       protected void doFilterInternal(
           HttpServletRequest request,
           HttpServletResponse response,
           FilterChain filterChain     // Цепочка фильтров — передаём дальше
       ) throws ServletException, IOException {

           // 1. Извлечь токен из заголовка Authorization: Bearer <token>
           String authHeader = request.getHeader("Authorization");

           if (authHeader != null && authHeader.startsWith("Bearer ")) {
               String token = authHeader.substring(7);
               try {
                   // 2. Валидировать и извлечь данные пользователя
                   UserPrincipal principal = jwtTokenService.validateToken(token);

                   // 3. Установить аутентификацию в контекст запроса
                   // После этого Spring знает: "этот запрос от пользователя X с ролью Y"
                   var authentication = new UsernamePasswordAuthenticationToken(
                       principal, null, principal.getAuthorities()
                   );
                   SecurityContextHolder.getContext().setAuthentication(authentication);
               } catch (JwtException e) {
                   // Токен невалиден — продолжаем без аутентификации.
                   // Если эндпоинт требует auth — Spring сам вернёт 401.
               }
           }

           // 4. Передать запрос дальше по цепочке
           filterChain.doFilter(request, response);
       }
   }
   ```

2. Создать `SecurityConfig`:

   ```java
   @Configuration
   @EnableWebSecurity       // Включает Spring Security
   @EnableMethodSecurity    // Разрешает @PreAuthorize на методах
   @RequiredArgsConstructor
   public class SecurityConfig {

       private final JwtAuthenticationFilter jwtAuthFilter;

       @Bean
       public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
           return http
               // CSRF отключаем — у нас stateless REST API с JWT.
               // CSRF нужен только для session-based аутентификации с cookies.
               .csrf(csrf -> csrf.disable())

               // Без сессий — каждый запрос аутентифицируется заново через JWT.
               .sessionManagement(session ->
                   session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

               // Правила доступа:
               .authorizeHttpRequests(auth -> auth
                   // /auth/** — доступно без токена (логин, регистрация)
                   .requestMatchers("/api/v1/auth/**").permitAll()
                   // Всё остальное — только с валидным JWT
                   .anyRequest().authenticated()
               )

               // Наш JWT-фильтр выполняется ПЕРЕД стандартным фильтром Spring.
               .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
               .build();
       }
   }
   ```

3. Создать `UserPrincipal` с `getAuthorities()` для ролей

4. Проверить: запросы без токена на `/api/v1/auth/*` проходят, на остальные — 401

**Изучаемые концепции:**

- **Spring Security Filter Chain:** Каждый HTTP-запрос проходит через цепочку фильтров. Наш `JwtAuthenticationFilter` — один из них. Порядок: наш JWT-фильтр → стандартные фильтры Spring → Controller. В Express это middleware: `app.use(authMiddleware)` → `app.use('/api', router)`. Разница: в Express ты сам определяешь порядок middleware через порядок `app.use()`. В Spring — через `addFilterBefore/After`.
- **SecurityFilterChain:** Конфигурирует правила: кто куда может ходить. `requestMatchers("/api/v1/auth/**").permitAll()` — любой может. `.anyRequest().authenticated()` — остальное только с токеном. В Express аналог — выборочное применение middleware: `app.use('/api/chats', authMiddleware, chatRouter)`. В Angular — `canActivate` guard в Router.
- **Stateless:** Сервер не хранит сессии. Каждый запрос — самодостаточный (несёт JWT с собой). Это позволяет масштабировать backend горизонтально (несколько серверов). В Express это как отказ от `express-session` в пользу JWT. В Angular мы тоже храним состояние на клиенте (localStorage + signals).
- **SecurityContextHolder:** Глобальное хранилище текущего пользователя для запроса. После `setAuthentication()` любой код может получить текущего пользователя через `@AuthenticationPrincipal`. В Express аналог — `req.user`, который устанавливает `passport.js` middleware. В Angular — `AuthService.user()` signal.

---

### Этап 8: AuthController и обработка ошибок

**Время:** ~45 мин

**Цель:** Создать REST-эндпоинты для аутентификации и единую обработку ошибок.

**Задачи:**

1. Создать `AuthController`:

   ```java
   @RestController                          // Каждый метод возвращает JSON (не HTML-страницу)
   @RequestMapping("/api/v1/auth")           // Префикс URL для всех методов контроллера
   @RequiredArgsConstructor
   public class AuthController {

       private final AuthService authService;

       // POST /api/v1/auth/register
       @PostMapping("/register")
       @ResponseStatus(HttpStatus.CREATED)   // Вернёт 201 вместо 200
       public AuthResponse register(
           @Valid                             // Активирует Bean Validation из RegisterRequest
           @RequestBody                       // Парсит JSON из тела запроса в объект
           RegisterRequest request
       ) {
           return authService.register(request);
       }

       @PostMapping("/login")
       public AuthResponse login(@Valid @RequestBody LoginRequest request) {
           return authService.login(request);
       }
   }
   ```

2. Создать `GlobalExceptionHandler`:

   ```java
   // @RestControllerAdvice — перехватывает исключения из ВСЕХ контроллеров.
   // Вместо try-catch в каждом методе — одно место для обработки ошибок.
   // Аналогия: в Angular это как ErrorHandler или HTTP Interceptor для ошибок.
   @RestControllerAdvice
   public class GlobalExceptionHandler {

       @ExceptionHandler(EmailAlreadyExistsException.class)
       @ResponseStatus(HttpStatus.CONFLICT)  // 409
       public ErrorResponse handleEmailExists(EmailAlreadyExistsException ex) {
           return ErrorResponse.of("email_exists", ex.getMessage());
       }

       @ExceptionHandler(InvalidCredentialsException.class)
       @ResponseStatus(HttpStatus.UNAUTHORIZED)  // 401
       public ErrorResponse handleInvalidCredentials(InvalidCredentialsException ex) {
           return ErrorResponse.of("invalid_credentials", "Неверный email или пароль");
       }

       // Ошибки валидации (когда @Valid не прошла)
       @ExceptionHandler(MethodArgumentNotValidException.class)
       @ResponseStatus(HttpStatus.BAD_REQUEST)  // 400
       public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
           String message = ex.getBindingResult().getFieldErrors().stream()
               .map(error -> error.getField() + ": " + error.getDefaultMessage())
               .collect(Collectors.joining(", "));
           return ErrorResponse.of("validation_error", message);
       }
   }
   ```

3. Проверить через curl или Postman:

   ```bash
   # Регистрация
   curl -X POST http://localhost:8080/api/v1/auth/register \
     -H "Content-Type: application/json" \
     -d '{"name":"Иван","email":"ivan@test.com","password":"password123"}'

   # Логин
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"ivan@test.com","password":"password123"}'
   ```

**Изучаемые концепции:**

- **@RestController:** Комбинация `@Controller` + `@ResponseBody`. Каждый метод автоматически сериализует возвращаемый объект в JSON (через Jackson). В Express аналог — `router.post('/register', (req, res) => { res.json(result) })`. Разница: в Spring маршрут определяется аннотацией `@PostMapping`, а JSON-сериализация автоматическая (не нужен `res.json()`).
- **@RequestBody:** Spring берёт JSON из тела HTTP-запроса и десериализует в Java-объект. `{ "email": "a@b.com" }` → `LoginRequest("a@b.com", ...)`. В Express это делает `body-parser` middleware: `req.body.email`. Обратная операция — автоматическая: возвращённый объект → JSON.
- **@Valid:** Триггерит Bean Validation. Если `@NotBlank` или `@Email` не прошла — Spring бросает `MethodArgumentNotValidException` ДО вызова метода. Мы ловим это в `GlobalExceptionHandler`. В Express аналог — `express-validator` middleware перед обработчиком: `check('email').isEmail()`. В Angular — `form.invalid`.
- **@RestControllerAdvice:** Центральное место для обработки ошибок. Вместо try-catch в каждом методе — один класс ловит все исключения и конвертирует в JSON-ответ с правильным HTTP-статусом. В Express аналог — error-handling middleware: `app.use((err, req, res, next) => { res.status(err.status).json({ error: err.message }) })`. Паттерн "единый формат ошибок".
- **@AuthenticationPrincipal:** Извлекает текущего пользователя из SecurityContext (который установил JWT-фильтр). `@AuthenticationPrincipal UserPrincipal user` — Spring подставит объект пользователя из JWT. В Express — `req.user` (от `passport.js`). В Angular — `authService.user()`.

---

### Этап 9: Chat и Message Entity + миграции

**Время:** ~45 мин

**Цель:** Создать таблицы и Entity для чатов и сообщений.

**Задачи:**

1. Flyway миграция `V3__create_chats_table.sql`:

   ```sql
   CREATE TABLE chats (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       user_id UUID NOT NULL,
       title VARCHAR(255),
       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
       updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
       deleted BOOLEAN NOT NULL DEFAULT false,
       CONSTRAINT fk_chats_user
           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
   );
   CREATE INDEX idx_chats_user_id ON chats(user_id) WHERE deleted = false;
   CREATE INDEX idx_chats_updated_at ON chats(updated_at DESC) WHERE deleted = false;
   ```

2. Flyway миграция `V4__create_messages_table.sql`

3. Создать enum `MessageRole`

5. Создать Entity `Chat`:

   ```java
   @Entity
   @Table(name = "chats")
   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   @Builder
   public class Chat {
       @Id
       @GeneratedValue(strategy = GenerationType.UUID)
       private UUID id;

       // Связь: каждый чат принадлежит пользователю.
       // @ManyToOne — много чатов у одного пользователя.
       // FetchType.LAZY — не загружать User из БД, пока явно не попросят.
       // @JoinColumn — какая колонка в таблице chats ссылается на users.
       @ManyToOne(fetch = FetchType.LAZY)
       @JoinColumn(name = "user_id", nullable = false)
       private User user;

       private String title;

       @Column(name = "created_at", nullable = false, updatable = false)
       private Instant createdAt;

       @Column(name = "updated_at", nullable = false)
       private Instant updatedAt;

       @Column(nullable = false)
       private boolean deleted;

       @PrePersist
       protected void onCreate() {
           createdAt = Instant.now();
           updatedAt = Instant.now();
       }

       @PreUpdate
       protected void onUpdate() {
           updatedAt = Instant.now();
       }
   }
   ```

6. Создать Entity `Message`

7. Создать `ChatRepository` и `MessageRepository`

8. Создать DTO: `ChatDto`, `MessageDto`, `CreateChatRequest`, `UpdateChatRequest`, `SendMessageRequest`

**Изучаемые концепции:**

- **@ManyToOne / @OneToMany:** Описывают связи между таблицами. `@ManyToOne` на Chat — "много чатов принадлежат одному пользователю". JPA автоматически делает JOIN при загрузке. В Prisma аналог — `user User @relation(fields: [userId], references: [id])`. В Mongoose — `ref: 'User'` + `populate()`.
- **FetchType.LAZY vs EAGER:** LAZY — загружать связанный объект только когда обращаешься к нему. EAGER — загружать сразу. LAZY экономит ресурсы: если нужен только title чата, зачем тянуть все данные пользователя? Правило: всегда начинай с LAZY. В Mongoose аналог: по умолчанию LAZY, `populate('user')` — EAGER.
- **Soft Delete:** Поле `deleted = true` вместо реального удаления. Данные не теряются, можно восстановить. В запросах добавляем `WHERE deleted = false`. Часто используется и в Node.js-проектах — `paranoid: true` в Sequelize.
- **@PrePersist:** Метод вызывается автоматически ПЕРЕД сохранением в БД. Идеален для `createdAt`. В Mongoose аналог — `pre('save')` hook. В Prisma — `$use` middleware. Код выполняется при каждом сохранении автоматически.
- **Instant:** Java-тип для даты/времени в UTC. Аналог `Date` в JavaScript или `new Date().toISOString()`. Всегда храним в UTC, конвертируем в локальное время на фронте.

---

### Этап 10: ChatService и ChatController

**Время:** ~45 мин

**Цель:** Реализовать CRUD операции для чатов.

**Задачи:**

1. Создать `ChatNotFoundException`

2. Создать `ChatService`:

   ```java
   @Service
   @RequiredArgsConstructor
   public class ChatService {

       private final ChatRepository chatRepository;
       private final UserRepository userRepository;

       // Список чатов пользователя с пагинацией и поиском
       public PagedResponse<ChatDto> getUserChats(
               UUID userId, int page, int perPage, String search) {
           // Specification — динамический построитель WHERE-условий.
           // Аналогия: computed() в Angular, который собирает фильтры.
           Specification<Chat> spec = Specification.where(
               (root, query, cb) -> cb.equal(root.get("user").get("id"), userId)
           ).and(
               (root, query, cb) -> cb.isFalse(root.get("deleted"))
           );

           if (search != null && !search.isBlank()) {
               spec = spec.and((root, query, cb) ->
                   cb.like(cb.lower(root.get("title")),
                           "%" + search.toLowerCase() + "%"));
           }

           // PageRequest — настройки пагинации.
           // page - 1: Spring считает страницы с 0, мы с 1.
           Page<Chat> chats = chatRepository.findAll(spec,
               PageRequest.of(page - 1, perPage,
                   Sort.by("updatedAt").descending()));

           return PagedResponse.from(chats, ChatDto::from);
       }

       @Transactional
       public ChatDto createChat(UUID userId, String title) { ... }

       @Transactional
       public ChatDto updateChat(UUID userId, UUID chatId, String title) {
           // Проверка владельца: пользователь может менять только свои чаты
           Chat chat = chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId)
               .orElseThrow(() -> new ChatNotFoundException(chatId));
           chat.setTitle(title.trim());
           return ChatDto.from(chatRepository.save(chat));
       }

       @Transactional
       public void deleteChat(UUID userId, UUID chatId) {
           // Soft delete — не удаляем из БД, а помечаем
           Chat chat = chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId)
               .orElseThrow(() -> new ChatNotFoundException(chatId));
           chat.setDeleted(true);
           chatRepository.save(chat);
       }
   }
   ```

3. Создать `ChatController`:

   ```java
   @RestController
   @RequestMapping("/api/v1/chats")
   @RequiredArgsConstructor
   public class ChatController {

       private final ChatService chatService;

       @GetMapping
       public PagedResponse<ChatDto> getChats(
           @AuthenticationPrincipal UserPrincipal user,
           @RequestParam(defaultValue = "1") int page,
           @RequestParam(defaultValue = "20") int perPage,
           @RequestParam(required = false) String search
       ) {
           return chatService.getUserChats(user.getId(), page, perPage, search);
       }

       @PostMapping
       @ResponseStatus(HttpStatus.CREATED)
       public ChatDto createChat(
           @AuthenticationPrincipal UserPrincipal user,
           @RequestBody(required = false) CreateChatRequest request
       ) {
           String title = request != null ? request.title() : null;
           return chatService.createChat(user.getId(), title);
       }

       // PATCH, DELETE и GET /{id} ...
   }
   ```

4. Обновить `ChatRepository` — добавить методы с Specification

5. Добавить обработку `ChatNotFoundException` в `GlobalExceptionHandler`

**Изучаемые концепции:**

- **Specification (JPA):** Динамический построитель SQL-запросов. Вместо написания разных методов в Repository для каждой комбинации фильтров — собираем условия программно. В Node.js аналог — Knex query builder: `knex('chats').where('userId', id).andWhere('title', 'like', '%search%')`. В Angular — `computed()` + `filter()` для фильтрации чатов, но на уровне SQL.
- **PageRequest и Page:** Spring Data автоматически добавляет `LIMIT/OFFSET` и считает `total`. `Page<Chat>` содержит данные + мета-информацию (total, totalPages, hasNext). В Express пришлось бы писать два запроса: один для данных (`LIMIT/OFFSET`), второй для `COUNT(*)`.
- **@RequestParam:** Извлекает query-параметры из URL. `GET /chats?page=2&search=авто` → `page=2, search="авто"`. В Express это `req.query.page`, `req.query.search`. Spring дополнительно конвертирует типы (`String` → `int`) и задаёт default-значения через аннотацию.
- **@AuthenticationPrincipal:** Извлекает текущего пользователя из JWT. Каждый метод знает, кто сделал запрос. В Express — `req.user.id`. Так мы проверяем, что пользователь видит/меняет только СВОИ чаты.

---

### Этап 11: MessageService и MessageController

**Время:** ~45 мин

**Цель:** Реализовать CRUD для сообщений с cursor-пагинацией.

**Задачи:**

1. Создать `MessageService`:

   ```java
   @Service
   @RequiredArgsConstructor
   public class MessageService {

       private final MessageRepository messageRepository;
       private final ChatRepository chatRepository;

       // Cursor-based пагинация — для infinite scroll.
       // Вместо page/perPage используем "загрузить N сообщений до ID X".
       // Более надёжная для чатов: если появилось новое сообщение,
       // обычная пагинация "сдвинется", cursor — нет.
       public CursorResponse<MessageDto> getMessages(
               UUID userId, UUID chatId, int limit, UUID before) {
           // Проверка доступа
           chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId)
               .orElseThrow(() -> new ChatNotFoundException(chatId));

           List<Message> messages;
           if (before != null) {
               // Cursor = ID сообщения. Сначала находим опорное сообщение, чтобы получить createdAt.
               Message anchor = messageRepository.findById(before)
                   .orElseThrow(() -> new MessageNotFoundException(before));
               // Загрузить сообщения ДО anchor.createdAt (для подгрузки истории)
               messages = messageRepository.findByChatIdAndCreatedAtBefore(
                   chatId, anchor.getCreatedAt(), PageRequest.of(0, limit + 1));
           } else {
               // Последние N сообщений
               messages = messageRepository.findByChatId(chatId, PageRequest.of(0, limit + 1));
           }

           boolean hasMore = messages.size() > limit;
           if (hasMore) messages = messages.subList(0, limit);

           return new CursorResponse<>(
               messages.stream().map(MessageDto::from).toList(),
               hasMore
           );
       }

       @Transactional
       public MessageDto sendMessage(UUID userId, UUID chatId, String content) {
           // Проверка доступа к чату, сохранение сообщения
           // Пока без LLM-ответа — просто сохраняем user message
       }
   }
   ```

2. Создать `MessageController`

3. Добавить custom queries в `MessageRepository`:

   ```java
   @Repository
   public interface MessageRepository extends JpaRepository<Message, UUID> {
       List<Message> findByChatId(UUID chatId, Pageable pageable);

       List<Message> findByChatIdAndCreatedAtBefore(UUID chatId, Instant before, Pageable pageable);
   }
   ```

4. Добавить rate limiting **10 сообщений/мин на пользователя** (in-memory, ConcurrentHashMap)

**Изучаемые концепции:**

- **Cursor-based пагинация:** Обычная пагинация (page=2, perPage=20) ломается в реальном времени: пока ты смотришь страницу 2, кто-то добавил сообщение — и элементы сдвинулись. Cursor-based: "покажи 20 сообщений до ID X" — стабильно, не зависит от новых данных. Используется в Telegram, WhatsApp, Slack. В Prisma аналог — `cursor: { id: lastId }, take: 20`.
- **@Query:** Когда имени метода не хватает для сложного запроса — пишем JPQL (Java Persistence Query Language). Похож на SQL, но работает с Entity-классами, а не с таблицами. `Message m` вместо `messages`. Spring подставляет параметры через `@Param`. В Express пришлось бы писать raw SQL через `pg` или query builder через Knex.
- **Rate Limiting:** Ограничение частоты запросов. Защита от спама и злоупотреблений. Простая реализация: `Map<userId, {count, timestamp}>`. Если превышен лимит — 429 Too Many Requests. В Express аналог — пакет `express-rate-limit`.

---

### Этап 12: CORS и интеграция с фронтендом

**Время:** ~30 мин

**Цель:** Настроить CORS, чтобы Angular-фронтенд мог обращаться к API.

**Задачи:**

1. Создать CORS-конфигурацию:

   ```java
   @Configuration
   public class CorsConfig implements WebMvcConfigurer {

       // CORS (Cross-Origin Resource Sharing) — механизм, который разрешает
       // или запрещает запросы с другого домена.
       //
       // Проблема: Angular dev-сервер работает на http://localhost:4200,
       // а backend — на http://localhost:8080. Браузер считает это "разными
       // источниками" и блокирует запросы (Same-Origin Policy).
       //
       // Решение: backend явно разрешает запросы с localhost:4200.
       @Override
       public void addCorsMappings(CorsRegistry registry) {
           registry.addMapping("/api/**")
               .allowedOrigins("http://localhost:4200")
               .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
               .allowedHeaders("*")
               .allowCredentials(true);
       }
   }
   ```

2. Обновить `SecurityConfig` — добавить `.cors(Customizer.withDefaults())` в цепочку

3. Создать профиль `application-dev.yml` с настройками для разработки

4. Проверить интеграцию: Angular-фронт → Spring API

**Изучаемые концепции:**

- **CORS:** Браузерная защита. Если фронт (`:4200`) делает запрос к бэку (`:8080`) — браузер сначала отправляет OPTIONS-запрос (preflight) и проверяет, разрешает ли сервер такой запрос. Без CORS-конфигурации — все запросы блокируются. В production (через nginx) проблемы нет — всё на одном домене. В Express: пакет `cors` — `app.use(cors({ origin: 'http://localhost:4200', credentials: true }))`. В Spring — аналогично, но через `WebMvcConfigurer`.
- **Профили Spring:** `application.yml` — общие настройки. `application-dev.yml` — для разработки (CORS открыт). `application-docker.yml` — для Docker. Активируется через `SPRING_PROFILES_ACTIVE=dev`. Аналогия Angular: `environment.ts` vs `environment.prod.ts`. Аналогия Express: `dotenv` + `NODE_ENV` — `if (process.env.NODE_ENV === 'development') { ... }`.

---

### Этап 13: Единая обработка ошибок (финализация)

**Время:** ~30 мин

**Цель:** Довести обработку ошибок до единого формата из контракта API.

**Задачи:**

1. Расширить `GlobalExceptionHandler`:
   - `ChatNotFoundException` → 404
   - `MessageNotFoundException` → 404
   - `InvalidTokenException` → 401
   - `AccessDeniedException` → 403
   - `MethodArgumentNotValidException` → 400
   - `HttpMessageNotReadableException` → 400 (невалидный JSON)
   - `Exception` → 500 (fallback, логируем стектрейс)

2. Формат ответа — по контракту:

   ```json
   {
     "code": "not_found",
     "message": "Чат не найден"
   }
   ```

3. Убедиться что все ошибки ловятся и не возвращают стектрейсы клиенту

**Изучаемые концепции:**

- **Иерархия исключений:** Базовые исключения → конкретные. `NotFoundException` ← `ChatNotFoundException`, `MessageNotFoundException`. Один `@ExceptionHandler(NotFoundException.class)` ловит все подтипы. В Express: создаёшь классы ошибок вручную (`class NotFoundError extends AppError`) и обрабатываешь в error middleware. В Spring — `@ControllerAdvice` делает это автоматически (как глобальный error middleware, но с типизацией по классам исключений).
- **Безопасность ошибок:** Стектрейс содержит внутренние детали (пути файлов, имена классов). НИКОГДА не отправляем клиенту. Логируем на сервере, клиенту — только `code` + `message`. В Express: `app.use((err, req, res, next) => { console.error(err.stack); res.status(500).json({ code: 'server_error', message: '...' }) })`. В Spring: `@ExceptionHandler(Exception.class)` + логирование через SLF4J — тот же принцип.

---

### Этап 14: Тестирование

**Время:** ~60 мин

**Цель:** Написать unit и integration тесты для ключевых сценариев.

**Задачи:**

1. Unit тесты `AuthService`:

   ```java
   @ExtendWith(MockitoExtension.class)
   class AuthServiceTest {

       @Mock  // Создаёт мок-объект. Все методы возвращают null/default.
       private UserRepository userRepository;
       @Mock
       private PasswordEncoder passwordEncoder;
       @Mock
       private JwtTokenService jwtTokenService;

       @InjectMocks  // Создаёт AuthService и подставляет моки в конструктор.
       private AuthService authService;

       @Test
       void register_shouldCreateUser_whenEmailIsNew() {
           // given — подготовка
           var request = new RegisterRequest("Иван", "ivan@test.com", "password123");
           when(userRepository.existsByEmail("ivan@test.com")).thenReturn(false);
           when(passwordEncoder.encode("password123")).thenReturn("hashed");
           when(userRepository.save(any())).thenAnswer(inv -> {
               User user = inv.getArgument(0);
               user.setId(UUID.randomUUID());
               return user;
           });

           // when — действие
           AuthResponse response = authService.register(request);

           // then — проверка
           assertThat(response.user().email()).isEqualTo("ivan@test.com");
           verify(userRepository).save(any(User.class));
       }

       @Test
       void register_shouldThrow_whenEmailExists() {
           var request = new RegisterRequest("Иван", "ivan@test.com", "password123");
           when(userRepository.existsByEmail("ivan@test.com")).thenReturn(true);

           assertThatThrownBy(() -> authService.register(request))
               .isInstanceOf(EmailAlreadyExistsException.class);
       }
   }
   ```

2. Integration тесты `AuthController`:

   ```java
   @SpringBootTest
   @AutoConfigureMockMvc
   class AuthControllerTest {

       @Autowired
       private MockMvc mockMvc;  // Эмулирует HTTP-запросы без запуска сервера

       @Test
       void register_shouldReturn201() throws Exception {
           mockMvc.perform(post("/api/v1/auth/register")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content("""
                       {"name":"Иван","email":"ivan@test.com","password":"password123"}
                       """))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.user.email").value("ivan@test.com"))
               .andExpect(jsonPath("$.accessToken").isNotEmpty());
       }

       @Test
       void register_shouldReturn400_whenInvalidEmail() throws Exception {
           mockMvc.perform(post("/api/v1/auth/register")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content("""
                       {"name":"Иван","email":"not-an-email","password":"password123"}
                       """))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.code").value("validation_error"));
       }
   }
   ```

3. Тест `ChatService` — CRUD операции, проверка владельца

4. Настроить Testcontainers для PostgreSQL в тестах:
   ```java
   @Testcontainers
   @SpringBootTest
   class AuthControllerTest {
       @Container
       static PostgreSQLContainer<?> postgres =
           new PostgreSQLContainer<>("postgres:16-alpine");
   }
   ```

**Изучаемые концепции:**

- **Mockito:** Библиотека для мок-объектов. `@Mock` создаёт заглушку, `when(...).thenReturn(...)` настраивает поведение, `verify(...)` проверяет вызовы. Аналогия Angular: `jasmine.createSpy()`. Аналогия Express: `jest.fn()` / `sinon.stub()` — те же моки, только в JavaScript. `when(repo.save(any())).thenReturn(user)` ≈ `repo.save = jest.fn().mockResolvedValue(user)`.
- **MockMvc:** Эмулирует HTTP-запросы без поднятия сервера. Быстрее интеграционных тестов. `perform(post(...))` → `andExpect(status().isCreated())` — отправляем запрос и проверяем ответ. Аналогия Express: `supertest` — `request(app).post('/api/auth/register').expect(201)`. Принцип одинаковый: тестируем HTTP без реального запуска сервера.
- **Testcontainers:** Поднимает PostgreSQL в Docker-контейнере специально для теста. Тест работает с реальной БД, не с моками. Контейнер удаляется после теста. Гарантирует, что SQL-запросы действительно работают. В Node.js: библиотека `testcontainers` (npm) работает аналогично, или используют `docker-compose` для тестовой БД.
- **Given-When-Then:** Паттерн организации тестов. Given — подготовка данных. When — действие. Then — проверка результата. Каждый тест проверяет ОДИН сценарий. Паттерн универсален — работает одинаково в JUnit, Jest, Jasmine.

---

## Порядок интеграции с фронтендом

После завершения backend потребуется обновить фронтенд:

1. Заменить мок `AuthService` на HTTP-запросы к `/api/v1/auth/*`
2. Заменить мок `ChatService` на HTTP-запросы к `/api/v1/chats/*`
3. Добавить HTTP Interceptor для JWT (Authorization header)
4. Добавить обработку refresh token

Это отдельная фаза, не входящая в текущий план.

---

## Команды

```bash
# Запуск PostgreSQL
docker compose up -d

# Запуск приложения
mvn spring-boot:run

# Запуск с профилем
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Тесты
mvn test

# Сборка
mvn package -DskipTests

# Остановка PostgreSQL
docker compose down
```

---

## Проверка каждого этапа

| Этап | Как проверить                                                    |
| ---- | ---------------------------------------------------------------- |
| 1    | `mvn spring-boot:run` стартует, подключается к PostgreSQL        |
| 2    | Таблицы `users`, `refresh_tokens` созданы в БД                   |
| 3    | Приложение стартует без ошибок                                   |
| 4    | Компилируется без ошибок                                         |
| 5    | Unit-тест: register/login работают                               |
| 6    | Unit-тест: JWT генерируется и валидируется                       |
| 7    | `curl` на защищённый URL → 401. С токеном → 200                  |
| 8    | `curl` register → 201. login → 200. Невалидные данные → 400      |
| 9    | Таблицы `chats`, `messages` созданы, триггеры работают           |
| 10   | CRUD чатов через curl: создание, получение, обновление, удаление |
| 11   | Отправка/получение сообщений, cursor-пагинация                   |
| 12   | Angular-фронт может делать запросы к API                         |
| 13   | Все ошибки возвращают единый JSON-формат                         |
| 14   | `mvn test` — все тесты зелёные                                   |

