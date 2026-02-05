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

**Абстракция провайдера:** Используется интерфейс `LLMProvider` для возможности смены провайдера в будущем без изменения бизнес-логики.

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
│                    │                                     │
│                    ▼                                     │
│            LLMProvider (interface)                      │
│                    │                                     │
│                    ▼                                     │
│         DeepSeekLLMProvider (implementation)            │
│                    │                                     │
│                    ▼                                     │
│            DeepSeek API (external)                      │
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
        
        // Step 1: Guard check — проверка релевантности запроса
        GuardResult guardResult = llmService.checkRelevance(userMessage.getContent());
        if (!guardResult.isRelevant()) {
            streamText(sink, guardResult.getRejectionResponse());
            sink.next(sseEvent("message_end", Map.of("finish_reason", "guard")));
            sink.complete();
            return;
        }
        
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

## Абстракция LLM провайдера

Для возможности смены провайдера в будущем используется интерфейс `LLMProvider`.

```java
public interface LLMProvider {
    /**
     * Выполняет запрос к LLM API.
     * 
     * @param systemPrompt системный промт
     * @param userMessage сообщение пользователя
     * @param temperature температура (0.0-1.0)
     * @return ответ от LLM
     * @throws LLMTimeoutException при таймауте
     * @throws LLMRateLimitException при превышении rate limit
     * @throws LLMUnavailableException при недоступности сервиса
     */
    LLMResponse chat(String systemPrompt, String userMessage, double temperature);
    
    /**
     * Возвращает статистику использования токенов для последнего запроса.
     */
    TokenUsage getLastTokenUsage();
}

@Data
@Builder
public class LLMResponse {
    private String content;
    private TokenUsage tokenUsage;
}

@Data
@Builder
public class TokenUsage {
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
}
```

### DeepSeekLLMProvider

Реализация для DeepSeek API с retry логикой и мониторингом.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DeepSeekLLMProvider implements LLMProvider {
    
    private final WebClient deepseekClient;
    private final ObjectMapper objectMapper;
    
    @Value("${llm.api-key}")
    private String apiKey;
    
    @Value("${llm.model}")
    private String model;
    
    @Value("${llm.timeout}")
    private Duration timeout;
    
    @Value("${llm.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${llm.retry.initial-delay:1s}")
    private Duration initialRetryDelay;
    
    private TokenUsage lastTokenUsage;
    
    @Override
    public LLMResponse chat(String systemPrompt, String userMessage, double temperature) {
        return chatWithRetry(systemPrompt, userMessage, temperature, 0);
    }
    
    private LLMResponse chatWithRetry(String systemPrompt, String userMessage, 
                                     double temperature, int attempt) {
        try {
            DeepSeekRequest request = DeepSeekRequest.builder()
                .model(model)
                .messages(List.of(
                    new ChatMessage("system", systemPrompt),
                    new ChatMessage("user", userMessage)
                ))
                .temperature(temperature)
                .maxTokens(1024)
                .stream(false)
                .build();
            
            DeepSeekResponse response = deepseekClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DeepSeekResponse.class)
                .timeout(timeout)
                .block();
            
            // Сохраняем статистику токенов для мониторинга затрат
            if (response.getUsage() != null) {
                lastTokenUsage = TokenUsage.builder()
                    .promptTokens(response.getUsage().getPromptTokens())
                    .completionTokens(response.getUsage().getCompletionTokens())
                    .totalTokens(response.getUsage().getTotalTokens())
                    .build();
                
                // Логируем для мониторинга затрат
                log.info("LLM token usage - prompt: {}, completion: {}, total: {}",
                    lastTokenUsage.getPromptTokens(),
                    lastTokenUsage.getCompletionTokens(),
                    lastTokenUsage.getTotalTokens());
            }
            
            String content = response.getChoices().get(0).getMessage().getContent();
            return LLMResponse.builder()
                .content(content)
                .tokenUsage(lastTokenUsage)
                .build();
                
        } catch (TimeoutException e) {
            log.warn("LLM request timeout (attempt {}/{})", attempt + 1, maxRetryAttempts);
            throw new LLMTimeoutException();
            
        } catch (HttpStatusCodeException e) {
            int statusCode = e.getStatusCode().value();
            
            if (statusCode == 429) {
                // Rate limit - retry с exponential backoff
                if (attempt < maxRetryAttempts) {
                    Duration delay = calculateRetryDelay(attempt);
                    log.warn("Rate limit hit, retrying after {} (attempt {}/{})", 
                        delay, attempt + 1, maxRetryAttempts);
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new LLMRateLimitException("Retry interrupted");
                    }
                    return chatWithRetry(systemPrompt, userMessage, temperature, attempt + 1);
                }
                throw new LLMRateLimitException("Превышен лимит запросов к DeepSeek API");
                
            } else if (statusCode == 401) {
                // Неверный API ключ - не retry, логируем ошибку
                log.error("Invalid DeepSeek API key. Check DEEPSEEK_API_KEY configuration.");
                throw new LLMConfigurationException("Неверная конфигурация API ключа");
                
            } else if (statusCode == 503 || statusCode == 502) {
                // Сервис недоступен - retry
                if (attempt < maxRetryAttempts) {
                    Duration delay = calculateRetryDelay(attempt);
                    log.warn("Service unavailable ({}), retrying after {} (attempt {}/{})",
                        statusCode, delay, attempt + 1, maxRetryAttempts);
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new LLMUnavailableException("Retry interrupted");
                    }
                    return chatWithRetry(systemPrompt, userMessage, temperature, attempt + 1);
                }
                throw new LLMUnavailableException("DeepSeek API временно недоступен");
                
            } else {
                log.error("Unexpected HTTP error from DeepSeek API: {}", statusCode);
                throw new LLMUnavailableException("Ошибка API: " + statusCode);
            }
            
        } catch (WebClientException e) {
            log.error("Network error calling DeepSeek API", e);
            throw new LLMUnavailableException("Ошибка сети при обращении к DeepSeek API");
        }
    }
    
    private Duration calculateRetryDelay(int attempt) {
        // Exponential backoff: 1s, 2s, 4s
        long delayMillis = initialRetryDelay.toMillis() * (1L << attempt);
        return Duration.ofMillis(delayMillis);
    }
    
    @Override
    public TokenUsage getLastTokenUsage() {
        return lastTokenUsage;
    }
}
```

### LLMService

Основной сервис, использующий абстракцию провайдера.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {
    
    private final LLMProvider llmProvider;
    private final ObjectMapper objectMapper;
    
    @Value("${llm.temperature.extract:0.3}")
    private double extractTemperature;
    
    @Value("${llm.temperature.format:0.7}")
    private double formatTemperature;
    
    /**
     * Извлечение критериев поиска из сообщения пользователя.
     * Режим: Extract (см. domain/llm-prompts.md)
     */
    public ExtractResult extractCriteria(String userMessage) {
        String systemPrompt = loadPrompt("extract");
        
        LLMResponse response = llmProvider.chat(systemPrompt, userMessage, extractTemperature);
        
        return parseExtractResult(response.getContent());
    }
    
    /**
     * Форматирование результатов поиска.
     * Режим: Format (см. domain/llm-prompts.md)
     */
    public String formatResults(String userCriteria, List<Car> cars) {
        String systemPrompt = loadPrompt("format");
        
        String input = buildFormatInput(userCriteria, cars);
        
        LLMResponse response = llmProvider.chat(systemPrompt, input, formatTemperature);
        
        return response.getContent();
    }
    
    /**
     * Проверка релевантности запроса (опционально).
     * Режим: Guard (см. domain/llm-prompts.md)
     */
    public GuardResult checkRelevance(String userMessage) {
        String systemPrompt = loadPrompt("guard");
        
        LLMResponse response = llmProvider.chat(systemPrompt, userMessage, 0.3);
        
        return parseGuardResult(response.getContent());
    }
    
    /**
     * Генерация названия чата.
     */
    public String generateTitle(String firstMessage) {
        String systemPrompt = "Придумай короткое название (3-5 слов) для чата о подборе автомобиля. " +
                              "Ответь только названием, без кавычек и пояснений.";
        
        LLMResponse response = llmProvider.chat(systemPrompt, firstMessage, 0.7);
        
        return response.getContent().trim();
    }
    
    /**
     * Получить статистику использования токенов для последнего запроса.
     */
    public TokenUsage getLastTokenUsage() {
        return llmProvider.getLastTokenUsage();
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
                "Не удалось понять запрос. Пожалуйста, уточните, какой автомобиль вы ищете и на какую сумму рассчитываете?"
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

### DTO для DeepSeek API

```java
@Data
@Builder
public class DeepSeekRequest {
    private String model;
    private List<ChatMessage> messages;
    private Double temperature;
    private Integer maxTokens;
    private Boolean stream;
}

@Data
@Builder
public class ChatMessage {
    private String role;  // "system", "user", "assistant"
    private String content;
}

@Data
public class DeepSeekResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    
    @Data
    public static class Choice {
        private Integer index;
        private Message message;
        private String finishReason;
    }
    
    @Data
    public static class Message {
        private String role;
        private String content;
    }
    
    @Data
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}
```

### Конфигурация Spring для выбора провайдера

```java
@Configuration
public class LLMConfig {
    
    @Value("${llm.base-url}")
    private String baseUrl;
    
    @Value("${llm.provider:deepseek}")
    private String provider;
    
    @Bean
    public WebClient deepseekClient() {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", "application/json")
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024)) // 10MB для больших ответов
            .build();
    }
    
    /**
     * Выбор реализации LLMProvider в зависимости от конфигурации.
     * Позволяет легко переключиться на другого провайдера в будущем.
     */
    @Bean
    @Primary
    public LLMProvider llmProvider(DeepSeekLLMProvider deepSeekProvider) {
        return switch (provider.toLowerCase()) {
            case "deepseek" -> deepSeekProvider;
            // В будущем можно добавить другие провайдеры:
            // case "openai" -> openAIProvider;
            // case "anthropic" -> anthropicProvider;
            default -> {
                log.warn("Unknown LLM provider: {}, using deepseek", provider);
                yield deepSeekProvider;
            }
        };
    }
}
```

### DTO для LLM

```java
@Data
@Builder
public class ExtractResult {
    private boolean readyToSearch;
    private CarSearchCriteria criteria;  // См. contracts/types.md
    private String clarificationQuestion;
    private String extractedInfo;
    
    public static ExtractResult needsClarification(String question) {
        return ExtractResult.builder()
            .readyToSearch(false)
            .clarificationQuestion(question)
            .build();
    }
}

@Data
@Builder
public class GuardResult {
    private boolean relevant;
    private String rejectionResponse;
}
```

**CarSearchCriteria** определён в `contracts/types.md` — единый источник истины для критериев поиска.

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
    
    // Допустимые значения — см. contracts/types.md
    
    public void validate(CarSearchCriteria criteria) {
        if (criteria.getMaxPrice() != null && criteria.getMaxPrice() < 100000) {
            throw new InvalidCriteriaException("Минимальная цена: 100 000 ₽");
        }
        
        // Валидация enum-значений делегируется на уровень БД (CHECK constraints)
        // или через enum-типы Java
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

## Исключения

```java
public class LLMTimeoutException extends RuntimeException {
    public LLMTimeoutException() {
        super("Превышено время ожидания ответа от LLM");
    }
}

public class LLMUnavailableException extends RuntimeException {
    public LLMUnavailableException(String message) {
        super(message);
    }
}

public class LLMRateLimitException extends RuntimeException {
    public LLMRateLimitException(String message) {
        super(message);
    }
}

public class LLMConfigurationException extends RuntimeException {
    public LLMConfigurationException(String message) {
        super(message);
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
    
    @ExceptionHandler(LLMRateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleLLMRateLimit(LLMRateLimitException ex) {
        return ErrorResponse.of("llm_rate_limit", ex.getMessage());
    }
    
    @ExceptionHandler(LLMConfigurationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleLLMConfiguration(LLMConfigurationException ex) {
        // Не показываем детали пользователю, только логируем
        log.error("LLM configuration error: {}", ex.getMessage());
        return ErrorResponse.of("llm_config_error", "Ошибка конфигурации сервиса");
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
  provider: deepseek  # Выбор провайдера (deepseek, в будущем: openai, anthropic)
  api-key: ${DEEPSEEK_API_KEY}  # Обязательно: API ключ из переменной окружения
  base-url: https://api.deepseek.com
  model: deepseek-chat  # или deepseek-reasoner для сложных задач
  timeout: 60s
  
  # Температура для разных режимов
  temperature:
    extract: 0.3  # Низкая для структурированных ответов
    format: 0.7   # Выше для креативного форматирования
  
  # Retry настройки для обработки временных ошибок
  retry:
    max-attempts: 3        # Максимум попыток при ошибке
    initial-delay: 1s     # Начальная задержка (exponential backoff: 1s, 2s, 4s)

rate-limit:
  messages-per-minute: 10

chat:
  max-message-length: 4000
  default-page-size: 20
  max-page-size: 50
```

---

## Тестирование

### Unit tests
- ChatService: CRUD операции, проверка владельца
- MessageService: валидация, rate limiting, flow обработки
- LLMService: парсинг JSON ответов, обработка ошибок, Guard mode
- DeepSeekLLMProvider: retry логика, обработка ошибок
- CriteriaValidator: валидация критериев, проверка минимум 2 дополнительных параметров

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
        
        // Mock extract — нужно минимум 2 дополнительных критерия
        when(mock.extractCriteria(contains("кроссовер бензин автомат")))
            .thenReturn(ExtractResult.builder()
                .readyToSearch(true)
                .criteria(CarSearchCriteria.builder()
                    .bodyType(BodyType.SUV)
                    .engineType(EngineType.PETROL)
                    .transmission(Transmission.AUTOMATIC)
                    .maxPrice(3000000)
                    .build())
                .build());
        
        // Mock extract — недостаточно критериев
        when(mock.extractCriteria(contains("кроссовер до 3 млн")))
            .thenReturn(ExtractResult.builder()
                .readyToSearch(false)
                .clarificationQuestion("Уточните тип двигателя и КПП")
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
