# План реализации MVP: полный цикл сообщения

## Текущее состояние

### Что уже работает
- JWT-аутентификация (register/login)
- CRUD чатов с пагинацией
- Cursor-пагинация сообщений (GET)
- Каталог автомобилей (поиск, фильтры)
- LLMService: Extract, Format, Guard, generateTitle — все методы реализованы
- DeepSeekLLMProvider с retry/timeout/error handling
- Поле `accumulated_criteria` (JSONB) в Chat entity и БД
- Frontend: отправка сообщений только локально, ответ от LLM не приходит

### Что отсутствует (scope MVP)
1. **POST /api/v1/chats/{chatId}/messages** — эндпоинт не реализован
2. **MessageService.sendMessage()** — бизнес-логика обработки сообщения отсутствует
3. **Накопление accumulatedCriteria** — поле есть, логика мержа нет
4. **Генерация title** после первого сообщения — метод LLM есть, вызов нет
5. **Логирование LLM запросов/ответов в БД** — отсутствует полностью
6. **Frontend sendMessage** — локальный, нужно подключить к бэкенду + получать ответ

---

## Принятые решения для MVP

### Синхронный ответ вместо SSE
MVP работает **без SSE-стриминга**. POST эндпоинт:
1. Сохраняет user message
2. Обрабатывает через LLM pipeline (Guard → Extract → Search → Format)
3. Сохраняет assistant message
4. Возвращает оба сообщения в ответе

SSE — отдельная задача после MVP.

### Формат ответа POST /api/v1/chats/{chatId}/messages
```json
{
  "userMessage": { "id": "...", "chatId": "...", "role": "user", "content": "...", "createdAt": "..." },
  "assistantMessage": { "id": "...", "chatId": "...", "role": "assistant", "content": "...", "createdAt": "..." }
}
```

### Логирование LLM — таблица `llm_logs`
Каждый вызов DeepSeek API записывается в БД: запрос, ответ, токены, длительность, тип (guard/extract/format/title).

---

## Этапы реализации

### Этап 0: Обновление документации
**Файлы:** `requirements/backend/chat-service.md`, `requirements/infrastructure/database.md`
- Добавить описание синхронного MVP-варианта POST endpoint
- Добавить таблицу `llm_logs` в документацию БД
- Добавить DTO `SendMessageResponse` в документацию

### Этап 1: Flyway-миграция — таблица `llm_logs`
**Файл:** `back/src/main/resources/db/migration/V10__create_llm_logs_table.sql`

```sql
CREATE TABLE llm_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id     UUID REFERENCES chats(id) ON DELETE SET NULL,
    message_id  UUID REFERENCES messages(id) ON DELETE SET NULL,
    request_type VARCHAR(20) NOT NULL,  -- 'guard', 'extract', 'format', 'title'
    system_prompt TEXT NOT NULL,
    user_message TEXT NOT NULL,
    response_content TEXT,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    duration_ms BIGINT,
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_llm_logs_chat_id ON llm_logs(chat_id);
CREATE INDEX idx_llm_logs_created_at ON llm_logs(created_at DESC);
```

### Этап 2: LLM Logging — entity + repository + интеграция
**Новые файлы:**
- `back/src/main/java/com/carsai/back/llm/LlmLog.java` — entity
- `back/src/main/java/com/carsai/back/llm/LlmLogRepository.java` — repository
- `back/src/main/java/com/carsai/back/llm/LlmLogService.java` — сервис логирования

**Изменяемые файлы:**
- `back/src/main/java/com/carsai/back/llm/LLMService.java` — обернуть каждый вызов LLMProvider в логирование: сохранять запрос, ответ, токены, длительность, ошибки

**Логика:**
- `LlmLogService.log(chatId, messageId, requestType, systemPrompt, userMessage, response, tokenUsage, durationMs, error)`
- Логирование асинхронное (`@Async`) чтобы не блокировать основной поток
- При ошибке логирования — только warn в лог, не ломаем основной flow

### Этап 3: DTO — SendMessageResponse
**Новый файл:** `back/src/main/java/com/carsai/back/message/dto/SendMessageResponse.java`

```java
public record SendMessageResponse(
    MessageDto userMessage,
    MessageDto assistantMessage
) {}
```

### Этап 4: MessageService.sendMessage() — основная бизнес-логика
**Файл:** `back/src/main/java/com/carsai/back/message/MessageService.java`

Добавить метод `sendMessage(UUID userId, UUID chatId, String content)`:

```
1. Проверить доступ к чату (findByIdAndUserIdAndDeletedFalse)
2. Сохранить user message (role=USER) в БД
3. Вызвать processMessage(chat, userMessage.content)
4. Сохранить assistant message (role=ASSISTANT) в БД
5. Обновить accumulatedCriteria в чате
6. Если первое сообщение и title==null — вызвать generateTitle асинхронно
7. Вернуть SendMessageResponse(userMessageDto, assistantMessageDto)
```

**Метод processMessage(chat, content) → String:**
```
1. Guard: llmService.checkRelevance(content)
   - Если !relevant → вернуть rejectionResponse
2. Extract: llmService.extractCriteria(content, accumulatedSummary)
   - accumulatedSummary = buildSummary(chat.accumulatedCriteria)
3. Если !readyToSearch → вернуть clarificationQuestion
   - Мержим частичные критерии в accumulatedCriteria
4. Если readyToSearch:
   - Мержим финальные критерии
   - Собираем объединённые criteria (accumulated + новые)
   - carService.searchForChat(mergedCriteria, 10)
   - llmService.formatResults(extractedInfo, searchResult) → вернуть текст
```

**Логика мержа accumulatedCriteria:**
```java
Map<String, Object> merged = new HashMap<>(chat.getAccumulatedCriteria() != null
    ? chat.getAccumulatedCriteria() : Map.of());
// Добавляем только ненулевые поля из extractResult.criteria
if (criteria.getPriceMax() != null) merged.put("priceMax", criteria.getPriceMax());
if (criteria.getBodyType() != null) merged.put("bodyType", criteria.getBodyType().name().toLowerCase());
// ... аналогично для всех полей
chat.setAccumulatedCriteria(merged);
chatRepository.save(chat);
```

**Зависимости MessageService (добавить):**
- `LLMService`
- `CarService`
- `ChatService` или напрямую `ChatRepository`

### Этап 5: MessageController — POST endpoint
**Файл:** `back/src/main/java/com/carsai/back/message/MessageController.java`

Добавить:
```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public SendMessageResponse sendMessage(
    @PathVariable UUID chatId,
    @AuthenticationPrincipal UserPrincipal user,
    @Valid @RequestBody SendMessageRequest request
) {
    return messageService.sendMessage(user.getId(), chatId, request.content());
}
```

### Этап 6: ChatService — генерация title
**Файл:** `back/src/main/java/com/carsai/back/chat/ChatService.java`

Добавить метод:
```java
@Async
public void generateTitleAsync(UUID chatId, String firstMessage) {
    try {
        String title = llmService.generateTitle(firstMessage);
        chatRepository.findById(chatId).ifPresent(chat -> {
            if (chat.getTitle() == null) {
                chat.setTitle(title);
                chatRepository.save(chat);
            }
        });
    } catch (Exception e) {
        log.warn("Failed to generate title for chat {}: {}", chatId, e.getMessage());
    }
}
```

**Зависимость:** добавить `LLMService` в `ChatService`.

### Этап 7: Включить @Async в Spring
**Файл:** `back/src/main/java/com/carsai/back/BackApplication.java` (или отдельный `AsyncConfig`)
- Добавить `@EnableAsync`

### Этап 8: Frontend — отправка сообщения через HTTP
**Файл:** `front/src/app/core/services/chat.service.ts`

Изменить `sendMessage()`:
```typescript
sendMessage(chatId: string, content: string): void {
  // 1. Оптимистичное добавление user message в UI
  const tempUserMessage: Message = { id: crypto.randomUUID(), chatId, role: 'user', content: content.trim(), createdAt: new Date().toISOString() };
  this.addMessage(chatId, tempUserMessage);

  // 2. Показать индикатор загрузки (typing indicator)
  this.setAssistantLoading(chatId, true);

  // 3. POST на бэкенд
  this.http.post<{ userMessage: Message; assistantMessage: Message }>(
    `${API_BASE_URL}/chats/${chatId}/messages`,
    { content: content.trim() }
  ).pipe(
    catchError(err => {
      this.setAssistantLoading(chatId, false);
      this.addMessage(chatId, { id: crypto.randomUUID(), chatId, role: 'assistant', content: 'Произошла ошибка. Попробуйте ещё раз.', createdAt: new Date().toISOString() });
      return EMPTY;
    })
  ).subscribe(response => {
    this.setAssistantLoading(chatId, false);
    // Заменить temp user message на реальный от бэкенда
    this.replaceMessage(chatId, tempUserMessage.id, response.userMessage);
    // Добавить assistant message
    this.addMessage(chatId, response.assistantMessage);
  });
}
```

Добавить вспомогательные методы:
- `addMessage(chatId, message)` — добавляет сообщение в messagesSignal
- `replaceMessage(chatId, tempId, realMessage)` — заменяет temp на реальное
- `setAssistantLoading(chatId, loading)` — signal для typing indicator

### Этап 9: Frontend — индикатор загрузки (typing indicator)
**Файлы:**
- `front/src/app/core/services/chat.service.ts` — `isAssistantLoading` signal
- `front/src/app/features/chat/components/message-list/message-list.component.ts` — показывать "ИИ печатает..."
- `front/src/app/features/chat/components/message-input/message-input.component.ts` — блокировать input во время ожидания ответа

### Этап 10: Тестирование end-to-end
1. Запустить `make db` + `make back` + `make front`
2. Зарегистрироваться/войти
3. Создать чат
4. Отправить "Ищу кроссовер до 3 миллионов, бензин, автомат"
5. Проверить: Guard пропускает → Extract находит criteria → Search в БД → Format → ответ с машинами
6. Отправить неполный запрос "Хочу кроссовер" → проверить clarification
7. Проверить `llm_logs` в БД — все запросы залогированы
8. Проверить автогенерацию title чата

---

## Карта файлов

### Новые файлы (5)
| Файл | Описание |
|------|----------|
| `back/src/main/resources/db/migration/V10__create_llm_logs_table.sql` | Миграция для таблицы логов |
| `back/src/main/java/com/carsai/back/llm/LlmLog.java` | Entity для логов LLM |
| `back/src/main/java/com/carsai/back/llm/LlmLogRepository.java` | Repository для логов |
| `back/src/main/java/com/carsai/back/llm/LlmLogService.java` | Сервис логирования |
| `back/src/main/java/com/carsai/back/message/dto/SendMessageResponse.java` | DTO ответа |

### Изменяемые файлы (7-8)
| Файл | Изменения |
|------|-----------|
| `back/src/main/java/com/carsai/back/message/MessageService.java` | +sendMessage(), +processMessage(), +mergeAccumulatedCriteria() |
| `back/src/main/java/com/carsai/back/message/MessageController.java` | +POST endpoint |
| `back/src/main/java/com/carsai/back/llm/LLMService.java` | +логирование через LlmLogService |
| `back/src/main/java/com/carsai/back/chat/ChatService.java` | +generateTitleAsync(), +LLMService dependency |
| `back/src/main/java/com/carsai/back/BackApplication.java` | +@EnableAsync |
| `front/src/app/core/services/chat.service.ts` | sendMessage() → HTTP, +loading state |
| `front/src/app/features/chat/components/message-list/message-list.component.ts` | +typing indicator |
| `front/src/app/features/chat/components/message-input/message-input.component.ts` | +disable во время loading |

### Документация (обновить)
| Файл | Изменения |
|------|-----------|
| `requirements/backend/chat-service.md` | Описание синхронного MVP endpoint |
| `requirements/infrastructure/database.md` | Таблица llm_logs |

---

## Порядок выполнения

```
Этап 0 → Этап 1 → Этап 2 → Этап 3 → Этап 4 → Этап 5 → Этап 6 → Этап 7 → Этап 8 → Этап 9 → Этап 10
 docs     migration  logging   DTO     service  controller title    async   frontend  loading   test
```

Этапы 1-3 могут выполняться параллельно (независимые файлы).
Этап 4 зависит от 2 и 3.
Этап 5 зависит от 4.
Этапы 6-7 могут выполняться параллельно с 5.
Этапы 8-9 зависят от 5 (бэкенд endpoint должен существовать).
