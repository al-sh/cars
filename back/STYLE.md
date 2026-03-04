# Правила стиля кода — Backend (Java / Spring Boot)

## Структура пакетов

Используется **domain-based packaging** (по доменам, не по слоям):

```
com.carsai.back/
├── chat/           # всё что относится к чатам: Chat, ChatService, ChatController, dto/
├── message/        # Message, MessageService, MessageController, dto/
├── car/            # Car, CarService, CarController, dto/
├── user/           # User, AuthService, AuthController, dto/
├── llm/            # LLMProvider, DeepSeekLLMProvider, LLMService, dto/
├── config/         # Spring-конфиги (Security, CORS, LLM, Enums)
├── security/       # JWT, UserPrincipal, JwtAuthenticationFilter
└── common/         # Общее: GlobalExceptionHandler, exception/, dto/ (PagedResponse, ErrorResponse)
```

❌ **Не создавать** плоские пакеты `controllers/`, `services/`, `repositories/` — всё группируется по домену.

---

## Entity-классы

```java
@Entity
@Table(name = "chats")
@Data               // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // JPA требует конструктор без аргументов
@AllArgsConstructor // Удобен для тестов
@Builder            // Chat.builder().user(user).title("...").build()
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)   // Всегда LAZY по умолчанию
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() { /* устанавливать timestamps */ }

    @PreUpdate
    protected void onUpdate() { /* обновлять updatedAt */ }
}
```

**Правила:**
- Всегда `fetch = FetchType.LAZY` для связей (`@ManyToOne`, `@OneToMany`)
- Temporal поля — `Instant` (не `LocalDateTime`, не `Date`)
- Soft delete через поле `boolean deleted`, без физического удаления
- `@GeneratedValue(strategy = GenerationType.UUID)` для первичных ключей

---

## DTO-классы

```java
// DTO — Java record (не class)
public record ChatDto(
        UUID id,
        UUID userId,
        String title,       // nullable
        Instant createdAt,
        Instant updatedAt,
        int messageCount
) {
    // Статический фабричный метод from() для конвертации Entity → DTO
    public static ChatDto from(Chat chat, int messageCount) {
        return new ChatDto(
                chat.getId(),
                chat.getUser().getId(),
                chat.getTitle(),
                chat.getCreatedAt(),
                chat.getUpdatedAt(),
                messageCount
        );
    }
}
```

**Правила:**
- DTO — `record`, не `class`
- Конвертация Entity → DTO через статический метод `from(Entity entity, ...extras)`
- Request DTO (входящие данные) — тоже `record`, с Bean Validation аннотациями:

```java
public record UpdateChatRequest(
        @NotBlank String title
) {}
```

---

## Сервисы

```java
@Service
@RequiredArgsConstructor  // Lombok: конструктор из final-полей (вместо @Autowired)
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatDto createChat(UUID userId, String title) { ... }
}
```

**Правила:**
- `@Service @RequiredArgsConstructor` — обязательная пара
- Все зависимости — `final` поля (constructor injection через Lombok)
- ❌ Не использовать `@Autowired` напрямую
- `@Transactional` — только на методах, изменяющих состояние (create, update, delete)
- Всегда проверять права доступа перед операцией (пользователь не может трогать чужие данные)

---

## Контроллеры

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
    @ResponseStatus(HttpStatus.CREATED)    // 201 при создании ресурса
    public ChatDto createChat(...) { ... }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 при удалении
    public void deleteChat(...) { ... }
}
```

**Правила:**
- Контроллер не содержит бизнес-логику — только маршрутизация и делегирование в Service
- `@AuthenticationPrincipal UserPrincipal user` — получать текущего пользователя так
- `@Valid` + `@RequestBody` для входящих DTO с валидацией
- HTTP статусы: `201` при создании, `204` при удалении (без тела ответа), `200` по умолчанию
- URL в формате `/api/v1/{domain}` (версионирование через путь)

---

## Исключения

```java
// Доменное исключение в пакете common/exception/
public class ChatNotFoundException extends RuntimeException {
    public ChatNotFoundException(UUID chatId) {
        super("Chat not found: " + chatId);
    }
}
```

- Специфичные исключения для каждого домена (`ChatNotFoundException`, `CarNotFoundException`)
- Все исключения наследуются от `RuntimeException`
- Обработка централизована в `GlobalExceptionHandler` (`common/exception/`)
- ❌ Не перехватывать исключения в контроллерах/сервисах без необходимости

---

## Именование

| Элемент | Формат | Пример |
|---------|--------|--------|
| Классы | `PascalCase` | `ChatService`, `MessageController` |
| Методы, поля | `camelCase` | `getUserChats`, `createdAt` |
| Константы | `UPPER_SNAKE_CASE` | `MAX_MESSAGE_LENGTH` |
| Таблицы БД | `snake_case` | `chats`, `chat_messages` |
| Колонки БД | `snake_case` | `user_id`, `created_at` |
| JSON поля API | `camelCase` | `userId`, `createdAt`, `messageCount` |
| Пакеты | `lowercase` | `com.carsai.back.chat` |

**Порядок слов в именах:** сущность → уточнение
- ✅ `yearMin`, `yearMax`, `priceMax`, `createdAt`
- ❌ `minYear`, `maxYear`, `maxPrice`, `atCreated`

---

## Комментарии

Стиль комментариев такой же, как в frontend (см. `front/plan.md`) — подробные, с объяснением "почему", аналогиями с JavaScript/Express/Mongoose:

```java
// ✅ Хорошо
// getReferenceById() — получаем "прокси" объекта User без загрузки из БД.
// Нам не нужны данные пользователя — только его ID для связи @ManyToOne.
// Экономит один SQL-запрос при создании чата.
User userRef = userRepository.getReferenceById(userId);

// ❌ Плохо
// Get user reference
User userRef = userRepository.getReferenceById(userId);
```

- Комментарии к классам — Javadoc-блок с описанием ответственности, эндпоинтов (в контроллерах), ключевых решений
- Комментарии к методам — Javadoc с `@param`, `@return`, `@throws` для публичных методов сервисов
- Inline-комментарии — на русском языке для нетривиальной логики
- Аналогии с JS/Express/Mongoose — для лучшего понимания паттернов

---

## Миграции БД

- Файлы в `back/src/main/resources/db/migration/`
- Формат имён: `V{номер}__{snake_case_описание}.sql`
- ✅ `V5__add_soft_delete_to_chats.sql`
- ❌ `addSoftDelete.sql`, `migration5.sql`
- Каждая миграция — одно логическое изменение схемы
- Не изменять уже применённые миграции

---

## Git

Правила коммитов — те же, что в `front/STYLE.md`:
- Сообщения на русском языке
- Краткое описание (до 100 символов), начинать с заглавной буквы
- Одна строка, без подробностей в теле
- Без `Co-Authored-By`

```bash
# ✅ Правильно
git commit -m "Добавлен MessageService с интеграцией LLM"
git commit -m "Исправлена проверка прав в ChatService.deleteChat"

# ❌ Неправильно
git commit -m "feat: add MessageService"
git commit -m "fix bug"
```
