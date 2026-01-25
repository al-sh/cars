# Chat Service

> **Зависимости:** CONTEXT.md, contracts/api.md, contracts/types.md, contracts/sse.md, domain/llm-prompts.md, infrastructure/database.md

Сервис управления чатами и интеграции с LLM.

---

## Ответственность

- CRUD операции с чатами
- CRUD операции с сообщениями
- Извлечение критериев поиска через LLM
- Поиск автомобилей в БД
- Форматирование ответов через LLM
- Стриминг ответов через SSE

---

## Архитектура

**Принцип:** Backend управляет всем процессом. LLM не имеет доступа к БД. Направление запросов только Backend → LLM.

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
│                    ┌─────────┴─────────┐                │
│                    ▼                   ▼                │
│              LLMService          CarService             │
│          (Extract/Format)        (Search DB)            │
└─────────────────┬───────────────────┬───────────────────┘
                  │                   │
┌─────────────────▼───────────────────▼───────────────────┐
│                     Repository                           │
│  ChatRepository    MessageRepository    CarRepository   │
└─────────────────────────────────────────────────────────┘
                              │
                              ▼
                         PostgreSQL
```

---

## Поток обработки сообщения

```
┌─────────────────────────────────────────────────────────────────┐
│                    MessageService.processMessage()               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Сохранить user message в БД                                 │
│                          │                                       │
│                          ▼                                       │
│  2. LLM (Guard) ──► Проверка релевантности                      │
│                          │                                       │
│            ┌─────────────┴─────────────┐                        │
│            ▼                           ▼                        │
│      is_relevant: false          is_relevant: true              │
│            │                           │                        │
│            ▼                           ▼                        │
│      Return rejection         3. LLM (Extract)                  │
│                                       │                         │
│                          ┌────────────┴────────────┐            │
│                          ▼                         ▼            │
│                 ready_to_search: false    ready_to_search: true │
│                          │                         │            │
│                          ▼                         ▼            │
│                 Return clarification      4. CarService.search()│
│                 question                          │             │
│                                                   ▼             │
│                                    ┌──────────────┴──────────┐  │
│                                    ▼                         ▼  │
│                              results > 0              results = 0│
│                                    │                         │  │
│                                    ▼                         ▼  │
│                           5. LLM (Format)      LLM (Format empty)│
│                                    │                         │  │
│                                    └─────────┬───────────────┘  │
│                                              ▼                   │
│                                  6. Сохранить assistant message │
│                                              │                   │
│                                              ▼                   │
│                                  7. Stream response to client   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
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
    private final LLMService llmService;
    
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
    
    @Async
    public void generateTitle(UUID chatId, String firstMessage) {
        // Асинхронно через LLM: "Придумай короткое название для чата: {firstMessage}"
        String title = llmService.generateTitle(firstMessage);
        chatRepository.updateTitle(chatId, title);
    }
}
```

### MessageService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final LLMService llmService;
    private final CarService carService;
    private final RateLimiter rateLimiter;
    
    public CursorResponse<MessageDto> getMessages(UUID userId, UUID chatId, int limit, UUID before) {
        // Проверка доступа, пагинация cursor-based
    }
    
    @Transactional
    public SendMessageResponse sendMessage(UUID userId, UUID chatId, String content) {
        // 1. Проверить rate limit
        rateLimiter.checkLimit(userId);
        
        // 2. Валидировать content (длина)
        validateContent(content);
        
        // 3. Проверить доступ к чату
        Chat chat = chatRepository.findByIdAndUserId(chatId, userId)
            .orElseThrow(() -> new ChatNotFoundException(chatId));
        
        // 4. Сохранить user message
        Message userMessage = messageRepository.save(
            Message.builder()
                .chatId(chatId)
                .role(MessageRole.USER)
                .content(content)
                .build()
        );
        
        // 5. Вернуть user_message + stream_url
        return new SendMessageResponse(
            MessageDto.from(userMessage),
            "/api/v1/chats/" + chatId + "/stream?message_id=" + userMessage.getId()
        );
    }
    
    public Flux<ServerSentEvent<String>> streamResponse(UUID userId, UUID chatId, UUID userMessageId) {
        return Flux.create(sink -> {
            try {
                processMessageAsync(userId, chatId, userMessageId, sink);
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }
    
    private void processMessageAsync(UUID userId, UUID chatId, UUID userMessageId, 
                                     FluxSink<ServerSentEvent<String>> sink) {
        // Получить user message
        Message userMessage = messageRepository.findById(userMessageId)
            .orElseThrow(() -> new MessageNotFoundException(userMessageId));
        
        // Emit start event
        sink.next(sseEvent("message_start", Map.of("message_id", UUID.randomUUID())));
        
        // Step 1: Guard check (опционально, можно пропустить для простоты)
        // GuardResult guardResult = llmService.checkRelevance(userMessage.getContent());
        // if (!guardResult.isRelevant()) {
        //     streamText(sink, guardResult.getRejectionResponse());
        //     return;
        // }
        
        // Step 2: Extract criteria
        sink.next(sseEvent("status", Map.of("stage", "extracting")));
        ExtractResult extractResult = llmService.extractCriteria(userMessage.getContent());
        
        String responseText;
        
        if (!extractResult.isReadyToSearch()) {
            // Нужны уточнения — возвращаем вопрос
            responseText = extractResult.getClarificationQuestion();
        } else {
            // Step 3: Search in DB
            sink.next(sseEvent("status", Map.of("stage", "searching")));
            CarSearchCriteria criteria = extractResult.toCriteria();
            List<Car> cars = carService.search(criteria);
            
            // Step 4: Format response
            sink.next(sseEvent("status", Map.of("stage", "formatting")));
            responseText = llmService.formatResults(
                extractResult.getExtractedInfo(),
                cars
            );
        }
        
        // Step 5: Stream response text
        streamText(sink, responseText);
        
        // Step 6: Save assistant message
        Message assistantMessage = messageRepository.save(
            Message.builder()
                .chatId(chatId)
                .role(MessageRole.ASSISTANT)
                .content(responseText)
                .build()
        );
        
        // Emit end event
        sink.next(sseEvent("message_end", Map.of(
            "message_id", assistantMessage.getId(),
            "finish_reason", "stop"
        )));
        
        sink.complete();
    }
    
    private void streamText(FluxSink<ServerSentEvent<String>> sink, String text) {
        // Имитация стриминга: разбиваем на слова/фразы
        String[] words = text.split("(?<=\\s)");
        for (String word : words) {
            sink.next(sseEvent("content_delta", Map.of("delta", word)));
            // Небольшая задержка для эффекта печатания
            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
        }
    }
    
    private ServerSentEvent<String> sseEvent(String event, Object data) {
        return ServerSentEvent.<String>builder()
            .event(event)
            .data(toJson(data))
            .build();
    }
}
```

### LLMService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {
    
    private final WebClient ollamaClient;
    private final ObjectMapper objectMapper;
    
    @Value("${llm.model}")
    private String model;
    
    @Value("${llm.timeout}")
    private Duration timeout;
    
    /**
     * Извлечение критериев поиска из сообщения пользователя.
     * Режим: Extract (см. domain/llm-prompts.md)
     */
    public ExtractResult extractCriteria(String userMessage) {
        String systemPrompt = loadPrompt("extract");
        
        String response = chat(systemPrompt, userMessage);
        
        return parseExtractResult(response);
    }
    
    /**
     * Форматирование результатов поиска.
     * Режим: Format (см. domain/llm-prompts.md)
     */
    public String formatResults(String userCriteria, List<Car> cars) {
        String systemPrompt = loadPrompt("format");
        
        String input = buildFormatInput(userCriteria, cars);
        
        return chat(systemPrompt, input);
    }
    
    /**
     * Проверка релевантности запроса (опционально).
     * Режим: Guard (см. domain/llm-prompts.md)
     */
    public GuardResult checkRelevance(String userMessage) {
        String systemPrompt = loadPrompt("guard");
        
        String response = chat(systemPrompt, userMessage);
        
        return parseGuardResult(response);
    }
    
    /**
     * Генерация названия чата.
     */
    public String generateTitle(String firstMessage) {
        String systemPrompt = "Придумай короткое название (3-5 слов) для чата о подборе автомобиля. " +
                              "Ответь только названием, без кавычек и пояснений.";
        
        return chat(systemPrompt, firstMessage).trim();
    }
    
    private String chat(String systemPrompt, String userMessage) {
        OllamaRequest request = OllamaRequest.builder()
            .model(model)
            .messages(List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userMessage)
            ))
            .stream(false)
            .build();
        
        return ollamaClient.post()
            .uri("/api/chat")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OllamaResponse.class)
            .timeout(timeout)
            .map(OllamaResponse::getContent)
            .onErrorMap(TimeoutException.class, e -> new LLMTimeoutException())
            .onErrorMap(WebClientException.class, e -> new LLMUnavailableException())
            .block();
    }
    
    private ExtractResult parseExtractResult(String json) {
        try {
            // Извлекаем JSON из ответа (LLM может добавить текст вокруг)
            String cleanJson = extractJson(json);
            return objectMapper.readValue(cleanJson, ExtractResult.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse LLM response as JSON: {}", json);
            // Fallback: считаем что нужно уточнение
            return ExtractResult.needsClarification(
                "Не удалось понять запрос. Пожалуйста, уточните, какой автомобиль вы ищете и какой у вас бюджет?"
            );
        }
    }
    
    private String buildFormatInput(String userCriteria, List<Car> cars) {
        Map<String, Object> input = Map.of(
            "user_criteria", userCriteria,
            "results_count", cars.size(),
            "results", cars.stream()
                .map(this::carToMap)
                .limit(10)
                .toList()
        );
        return toJson(input);
    }
}
```

### DTO для LLM

```java
@Data
@Builder
public class ExtractResult {
    private boolean readyToSearch;
    private CarSearchCriteria criteria;
    private String clarificationQuestion;
    private String extractedInfo;
    
    public static ExtractResult needsClarification(String question) {
        return ExtractResult.builder()
            .readyToSearch(false)
            .clarificationQuestion(question)
            .build();
    }
    
    public CarSearchCriteria toCriteria() {
        return criteria;
    }
}

@Data
@Builder
public class GuardResult {
    private boolean relevant;
    private String rejectionResponse;
}

@Data
@Builder
public class CarSearchCriteria {
    private Integer budgetMin;
    private Integer budgetMax;
    private String bodyType;
    private String engineType;
    private String brand;
    private Integer seats;
    private String transmission;
    private String drive;
    private Integer yearMin;
    private Integer yearMax;
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

### CarSearchCriteria валидация

```java
@Component
public class CriteriaValidator {
    
    private static final Set<String> ALLOWED_BODY_TYPES = 
        Set.of("sedan", "suv", "hatchback", "wagon", "minivan", "coupe", "pickup");
    
    private static final Set<String> ALLOWED_ENGINE_TYPES = 
        Set.of("petrol", "diesel", "hybrid", "electric");
    
    public void validate(CarSearchCriteria criteria) {
        if (criteria.getBudgetMax() != null && criteria.getBudgetMax() < 100000) {
            throw new InvalidCriteriaException("Минимальный бюджет: 100 000 ₽");
        }
        
        if (criteria.getBodyType() != null && 
            !ALLOWED_BODY_TYPES.contains(criteria.getBodyType())) {
            throw new InvalidCriteriaException("Неизвестный тип кузова");
        }
        
        // ... другие проверки
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
    
    @ExceptionHandler(InvalidCriteriaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidCriteria(InvalidCriteriaException ex) {
        return ErrorResponse.of("invalid_criteria", ex.getMessage());
    }
}
```

---

## Конфигурация

```yaml
# application.yml

llm:
  base-url: http://ollama:11434
  model: qwen2.5:7b
  timeout: 60s
  
  # Температура для разных режимов
  temperature:
    extract: 0.3  # Низкая для структурированных ответов
    format: 0.7   # Выше для креативного форматирования

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
    
    public void recordLLMCall(String mode, Duration duration) {
        registry.timer("chat.llm.latency", "mode", mode).record(duration);
    }
    
    public void recordLLMError(String errorType) {
        registry.counter("chat.llm.errors", "type", errorType).increment();
    }
    
    public void recordSearchResults(int count) {
        registry.summary("chat.search.results").record(count);
    }
    
    public void recordClarificationNeeded() {
        registry.counter("chat.clarification.needed").increment();
    }
}
```

---

## Тестирование

### Unit tests
- ChatService: CRUD операции, проверка владельца
- MessageService: валидация, rate limiting, flow обработки
- LLMService: парсинг JSON ответов, обработка ошибок
- CriteriaValidator: валидация критериев

### Integration tests
- ChatController: HTTP статусы, валидация, авторизация
- SSE streaming: получение событий
- Full flow: user message → extract → search → format → response

### Mock LLM responses

```java
@TestConfiguration
public class LLMTestConfig {
    
    @Bean
    @Primary
    public LLMService mockLLMService() {
        LLMService mock = Mockito.mock(LLMService.class);
        
        // Mock extract
        when(mock.extractCriteria(contains("кроссовер")))
            .thenReturn(ExtractResult.builder()
                .readyToSearch(true)
                .criteria(CarSearchCriteria.builder()
                    .bodyType("suv")
                    .budgetMax(3000000)
                    .build())
                .build());
        
        // Mock format
        when(mock.formatResults(any(), anyList()))
            .thenReturn("Вот подходящие варианты...");
        
        return mock;
    }
}
```

### Тестовые данные
- Фикстуры для чатов и сообщений
- Фикстуры для автомобилей
- Testcontainers для PostgreSQL
