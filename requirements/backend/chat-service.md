# Chat Service

> **Зависимости:** CONTEXT.md, contracts/api.md, contracts/types.md, contracts/sse.md, domain/llm-prompts.md, infrastructure/database.md

Сервис управления чатами и интеграции с LLM.

---

## Ответственность

- CRUD операции с чатами
- CRUD операции с сообщениями
- Интеграция с LLM (Ollama)
- Стриминг ответов через SSE
- Выполнение tool calls (search_cars)

---

## Архитектура

```
┌─────────────────────────────────────────────────────────┐
│                      Controller                          │
│  ChatController          MessageController               │
└─────────────────┬───────────────────┬───────────────────┘
                  │                   │
┌─────────────────▼───────────────────▼───────────────────┐
│                       Service                            │
│  ChatService            MessageService                   │
│                              │                           │
│                              ▼                           │
│                        LLMService                        │
│                              │                           │
│                              ▼                           │
│                      ToolExecutor                        │
└─────────────────┬───────────────────┬───────────────────┘
                  │                   │
┌─────────────────▼───────────────────▼───────────────────┐
│                     Repository                           │
│  ChatRepository         MessageRepository                │
└─────────────────────────────────────────────────────────┘
                              │
                              ▼
                         PostgreSQL
```

---

## Слой Controller

### ChatController

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
    ) { ... }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatDto createChat(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestBody(required = false) CreateChatRequest request
    ) { ... }
    
    @GetMapping("/{id}")
    public ChatDto getChat(
        @AuthenticationPrincipal UserPrincipal user,
        @PathVariable UUID id
    ) { ... }
    
    @PatchMapping("/{id}")
    public ChatDto updateChat(
        @AuthenticationPrincipal UserPrincipal user,
        @PathVariable UUID id,
        @RequestBody UpdateChatRequest request
    ) { ... }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChat(
        @AuthenticationPrincipal UserPrincipal user,
        @PathVariable UUID id
    ) { ... }
}
```

### MessageController

```java
@RestController
@RequestMapping("/api/v1/chats/{chatId}/messages")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    
    @GetMapping
    public CursorResponse<MessageDto> getMessages(
        @AuthenticationPrincipal UserPrincipal user,
        @PathVariable UUID chatId,
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(required = false) UUID before
    ) { ... }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SendMessageResponse sendMessage(
        @AuthenticationPrincipal UserPrincipal user,
        @PathVariable UUID chatId,
        @Valid @RequestBody SendMessageRequest request
    ) { ... }
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamResponse(
        @AuthenticationPrincipal UserPrincipal user,
        @PathVariable UUID chatId,
        @RequestParam UUID messageId
    ) { ... }
}
```

---

## Слой Service

### ChatService

```java
@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatRepository chatRepository;
    
    public PagedResponse<ChatDto> getUserChats(UUID userId, int page, int perPage, String search) {
        // Пагинация, фильтрация по search, сортировка по updated_at desc
    }
    
    public ChatDto createChat(UUID userId, String title) {
        // Создание с title = null (генерируется позже)
    }
    
    public ChatDto updateChat(UUID userId, UUID chatId, String title) {
        // Проверка владельца, обновление title
    }
    
    public void deleteChat(UUID userId, UUID chatId) {
        // Soft delete (deleted = true)
    }
    
    public void generateTitle(UUID chatId, String firstMessage) {
        // Асинхронно через LLM: "Придумай короткое название для чата: {firstMessage}"
    }
}
```

### MessageService

```java
@Service
@RequiredArgsConstructor
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final LLMService llmService;
    private final RateLimiter rateLimiter;
    
    public CursorResponse<MessageDto> getMessages(UUID userId, UUID chatId, int limit, UUID before) {
        // Проверка доступа, пагинация cursor-based
    }
    
    @Transactional
    public SendMessageResponse sendMessage(UUID userId, UUID chatId, String content) {
        // 1. Проверить rate limit
        // 2. Валидировать content (длина)
        // 3. Проверить доступ к чату
        // 4. Сохранить user message
        // 5. Вернуть user_message + stream_url
    }
    
    public Flux<ServerSentEvent<String>> streamResponse(UUID userId, UUID chatId, UUID userMessageId) {
        // 1. Собрать контекст (system prompt + история)
        // 2. Вызвать LLM stream
        // 3. Обрабатывать tool_calls
        // 4. Сохранить финальный ответ
        // 5. Emit SSE events
    }
}
```

### LLMService

```java
@Service
@RequiredArgsConstructor
public class LLMService {
    
    private final WebClient ollamaClient;
    private final ToolExecutor toolExecutor;
    
    @Value("${llm.model}")
    private String model;
    
    @Value("${llm.system-prompt}")
    private String systemPrompt;
    
    public Flux<LLMChunk> chat(List<ChatMessage> messages, List<Tool> tools) {
        // Формирование запроса к Ollama /api/chat
        // Стриминг ответа
    }
    
    public Mono<String> generateTitle(String content) {
        // Короткий запрос для генерации названия чата
    }
    
    private List<ChatMessage> buildContext(List<Message> history) {
        // System prompt + последние N сообщений
        // Ограничение по токенам
    }
}
```

### ToolExecutor

```java
@Service
@RequiredArgsConstructor
public class ToolExecutor {
    
    private final CarRepository carRepository;
    
    public ToolResult execute(ToolCall toolCall) {
        return switch (toolCall.name()) {
            case "search_cars" -> executeSearchCars(toolCall.arguments());
            default -> ToolResult.error("Unknown tool: " + toolCall.name());
        };
    }
    
    private ToolResult executeSearchCars(Map<String, Object> args) {
        // Валидация аргументов
        // Построение Specification
        // Запрос к CarRepository
        // Формирование результата (count + items)
    }
}
```

---

## Валидация

### SendMessageRequest

```java
public record SendMessageRequest(
    @NotBlank(message = "Сообщение не может быть пустым")
    @Size(max = 4000, message = "Максимум 4000 символов")
    String content
) {}
```

### Tool call arguments

```java
public class SearchCarsValidator {
    
    private static final Set<String> ALLOWED_BODY_TYPES = 
        Set.of("sedan", "suv", "hatchback", "wagon", "minivan", "coupe", "pickup");
    
    public SearchCarsArgs validate(Map<String, Object> raw) {
        // Проверка типов
        // Проверка диапазонов (цена > 0, год разумный)
        // Проверка enum значений
        // Возврат типизированного объекта или исключение
    }
}
```

---

## Rate Limiting

```java
@Component
public class MessageRateLimiter {
    
    private final Map<UUID, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Value("${rate-limit.messages-per-minute}")
    private int limit; // default: 10
    
    public void checkLimit(UUID userId) {
        Bucket bucket = buckets.computeIfAbsent(userId, this::createBucket);
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Превышен лимит сообщений");
        }
    }
    
    private Bucket createBucket(UUID userId) {
        return Bucket.builder()
            .addLimit(Bandwidth.simple(limit, Duration.ofMinutes(1)))
            .build();
    }
}
```

---

## Обработка ошибок

```java
@RestControllerAdvice
public class ChatExceptionHandler {
    
    @ExceptionHandler(ChatNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ChatNotFoundException ex) {
        return ErrorResponse.of("not_found", ex.getMessage());
    }
    
    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleRateLimit(RateLimitExceededException ex) {
        return ErrorResponse.of("rate_limit", ex.getMessage());
    }
    
    @ExceptionHandler(LLMTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public ErrorResponse handleLLMTimeout(LLMTimeoutException ex) {
        return ErrorResponse.of("llm_timeout", "Превышено время ожидания ответа");
    }
    
    @ExceptionHandler(LLMUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleLLMUnavailable(LLMUnavailableException ex) {
        return ErrorResponse.of("llm_unavailable", "Сервис временно недоступен");
    }
}
```

---

## Конфигурация

```yaml
# application.yml

llm:
  base-url: http://ollama:11434
  model: llama3.1:8b
  timeout: 60s
  max-context-messages: 20
  max-context-tokens: 4000

rate-limit:
  messages-per-minute: 10

chat:
  max-message-length: 4000
  default-page-size: 20
  max-page-size: 50
```

---

## Метрики (Micrometer)

```java
@Component
@RequiredArgsConstructor
public class ChatMetrics {
    
    private final MeterRegistry registry;
    
    public void recordMessageSent() {
        registry.counter("chat.messages.sent").increment();
    }
    
    public void recordLLMLatency(Duration duration) {
        registry.timer("chat.llm.latency").record(duration);
    }
    
    public void recordLLMError(String errorType) {
        registry.counter("chat.llm.errors", "type", errorType).increment();
    }
    
    public void recordToolCall(String toolName) {
        registry.counter("chat.tool.calls", "tool", toolName).increment();
    }
}
```

---

## Тестирование

### Unit tests
- ChatService: CRUD операции, проверка владельца
- MessageService: валидация, rate limiting
- ToolExecutor: парсинг аргументов, формирование запросов
- LLMService: формирование контекста

### Integration tests
- ChatController: HTTP статусы, валидация, авторизация
- SSE streaming: получение событий
- Tool calling flow: полный цикл с mock LLM

### Тестовые данные
- Фикстуры для чатов и сообщений
- Mock ответы LLM
- Testcontainers для PostgreSQL
