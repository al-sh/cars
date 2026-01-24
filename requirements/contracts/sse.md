# SSE Протокол стриминга

Server-Sent Events используется для потоковой передачи ответов ИИ-ассистента.

---

## Endpoint

```
GET /api/v1/chats/{chat_id}/stream?message_id={message_id}
```

**Headers:**
```
Accept: text/event-stream
Authorization: Bearer {token}
```

---

## События

### message_start

Начало генерации ответа.

```
event: message_start
data: {"message_id": "msg_789", "chat_id": "chat_123"}
```

### content_delta

Порция текста ответа (приходит много раз).

```
event: content_delta
data: {"delta": "Отличный"}

event: content_delta
data: {"delta": " выбор! Для "}

event: content_delta
data: {"delta": "уточнения..."}
```

### tool_call

ИИ вызывает инструмент (поиск по базе).

```
event: tool_call
data: {"id": "call_001", "name": "search_cars", "arguments": {"max_price": 3000000, "body_type": "suv"}}
```

### tool_result

Результат выполнения инструмента (информационно для UI).

```
event: tool_result
data: {"id": "call_001", "result": {"count": 15, "sample": [{"brand": "Toyota", "model": "RAV4"}]}}
```

### message_end

Завершение генерации.

```
event: message_end
data: {"message_id": "msg_789", "finish_reason": "stop", "usage": {"prompt_tokens": 450, "completion_tokens": 120}}
```

**finish_reason:**
- `stop` — нормальное завершение
- `length` — достигнут лимит токенов
- `tool_calls` — ответ содержит только вызов инструмента (продолжение следует)

### error

Ошибка во время генерации.

```
event: error
data: {"code": "llm_timeout", "message": "Превышено время ожидания ответа"}
```

**Коды ошибок:**
| Код | Описание |
|-----|----------|
| llm_timeout | Таймаут LLM (60 сек) |
| llm_unavailable | LLM сервис недоступен |
| rate_limit | Превышен лимит запросов |
| internal_error | Внутренняя ошибка сервера |

### ping

Keep-alive сообщение (каждые 15 сек).

```
event: ping
data: {}
```

---

## Пример полной сессии

```
→ GET /api/v1/chats/chat_123/stream?message_id=msg_456
← Accept: text/event-stream

event: message_start
data: {"message_id": "msg_789", "chat_id": "chat_123"}

event: content_delta
data: {"delta": "Понял, вы ищете "}

event: content_delta
data: {"delta": "кроссовер до 3 млн. "}

event: tool_call
data: {"id": "call_001", "name": "search_cars", "arguments": {"max_price": 3000000, "body_type": "suv"}}

event: tool_result
data: {"id": "call_001", "result": {"count": 24}}

event: content_delta
data: {"delta": "Нашёл 24 варианта. "}

event: content_delta
data: {"delta": "Уточните тип двигателя..."}

event: message_end
data: {"message_id": "msg_789", "finish_reason": "stop"}
```

---

## Обработка на клиенте

### TypeScript (Angular)

```typescript
interface SSEEvent {
  event: 'message_start' | 'content_delta' | 'tool_call' | 'tool_result' | 'message_end' | 'error' | 'ping';
  data: unknown;
}

interface MessageStartData {
  message_id: string;
  chat_id: string;
}

interface ContentDeltaData {
  delta: string;
}

interface ToolCallData {
  id: string;
  name: string;
  arguments: Record<string, unknown>;
}

interface MessageEndData {
  message_id: string;
  finish_reason: 'stop' | 'length' | 'tool_calls';
  usage?: {
    prompt_tokens: number;
    completion_tokens: number;
  };
}

interface SSEErrorData {
  code: string;
  message: string;
}
```

### Рекомендации по реализации

1. **Буферизация:** Накапливать `content_delta` и обновлять UI с debounce ~50ms
2. **Reconnect:** При обрыве соединения — retry через 3 сек (max 3 попытки)
3. **Timeout:** Если нет событий 30 сек (кроме ping) — считать ошибкой
4. **Отмена:** При уходе со страницы — закрыть EventSource

---

## Таймауты

| Параметр | Значение |
|----------|----------|
| Ping interval | 15 секунд |
| Connection timeout | 60 секунд |
| Max response time | 120 секунд |
| Reconnect delay | 3 секунды |
| Max reconnect attempts | 3 |
