# SSE Протокол стриминга

Server-Sent Events используется для потоковой передачи ответов ассистента.

---

## Endpoint

```
GET /api/v1/chats/{chatId}/messages/stream?messageId={messageId}&token={jwt}
```

**Headers:**
```
Accept: text/event-stream
```

---

## Авторизация

Браузерный `EventSource` API не поддерживает кастомные HTTP-заголовки (в том числе `Authorization`). Поэтому JWT передаётся как query parameter.

**Решение:** передавать token через query parameter `token`.

```
GET /api/v1/chats/{chatId}/messages/stream?messageId={messageId}&token={jwt}
```

**Backend:**
- Для SSE эндпоинта: извлекать JWT из query parameter `token` (а не из заголовка `Authorization`)
- Для всех остальных эндпоинтов: авторизация через заголовок `Authorization: Bearer {token}` как обычно
- Реализация: отдельный фильтр или условие в существующем `JwtAuthenticationFilter`

**Frontend:**
- При подключении к SSE добавлять `token` в URL
- Не использовать `Authorization` заголовок для SSE-запросов

**Безопасность:**
- Query parameters могут попадать в логи сервера и прокси -- убедиться, что access log nginx не логирует query params полностью, или использовать redaction
- Токен в URL менее безопасен, чем в заголовке, но это стандартный workaround для SSE/WebSocket и приемлемо для MVP

---

## События

### message_start

Начало генерации ответа.

```
event: message_start
data: {"messageId": "msg_789", "chatId": "chat_123"}
```

### status

Текущий этап обработки (информационное событие).

```
event: status
data: {"stage": "extracting"}

event: status
data: {"stage": "searching"}

event: status
data: {"stage": "formatting"}
```

**Возможные stage:**
| Stage | Описание |
|-------|----------|
| extracting | Извлечение критериев из запроса |
| searching | Поиск автомобилей в базе |
| formatting | Форматирование ответа |

### content_delta

Порция текста ответа (приходит много раз).

```
event: content_delta
data: {"delta": "Нашёл "}

event: content_delta
data: {"delta": "5 вариантов "}

event: content_delta
data: {"delta": "в вашем бюджете:"}
```

### message_end

Завершение генерации.

```
event: message_end
data: {"messageId": "msg_789", "finishReason": "stop"}
```

**finishReason:**
- `stop` — нормальное завершение
- `length` — достигнут лимит токенов

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

### title_updated

Обновление title чата (приходит при первом сообщении, если title был null).

```
event: title_updated
data: {"chatId": "chat_123", "title": "Подбор кроссовера до 3 млн"}
```

Событие может прийти в любой момент во время стриминга (генерация title асинхронна). Frontend должен обновить title в списке чатов и в заголовке.

### ping

Keep-alive сообщение (каждые 15 сек).

```
event: ping
data: {}
```

---

## Пример полной сессии

### Успешный поиск

```
→ GET /api/v1/chats/chat_123/messages/stream?messageId=msg_456&token=eyJ...
← Accept: text/event-stream

event: message_start
data: {"messageId": "msg_789", "chatId": "chat_123"}

event: status
data: {"stage": "extracting"}

event: status
data: {"stage": "searching"}

event: status
data: {"stage": "formatting"}

event: content_delta
data: {"delta": "Нашёл "}

event: content_delta
data: {"delta": "3 кроссовера "}

event: content_delta
data: {"delta": "в вашем бюджете:\n\n"}

event: content_delta
data: {"delta": "1. **Toyota RAV4 2023** — 2 900 000 ₽\n"}

event: content_delta
data: {"delta": "   2.5 л бензин, 199 л.с., автомат\n\n"}

event: content_delta
data: {"delta": "Хотите подробнее о каком-то варианте?"}

event: message_end
data: {"messageId": "msg_789", "finishReason": "stop"}
```

### Требуется уточнение (нет поиска)

```
event: message_start
data: {"messageId": "msg_790", "chatId": "chat_123"}

event: status
data: {"stage": "extracting"}

event: content_delta
data: {"delta": "Помогу подобрать автомобиль! "}

event: content_delta
data: {"delta": "Подскажите, пожалуйста:\n"}

event: content_delta
data: {"delta": "1. На какую сумму рассчитываете?\n"}

event: content_delta
data: {"delta": "2. Какой тип кузова предпочитаете?"}

event: message_end
data: {"messageId": "msg_790", "finishReason": "stop"}
```

### Ошибка

```
event: message_start
data: {"messageId": "msg_791", "chatId": "chat_123"}

event: status
data: {"stage": "extracting"}

event: error
data: {"code": "llm_timeout", "message": "Превышено время ожидания ответа"}
```

---

## Обработка на клиенте

### TypeScript (Angular)

```typescript
// Типы событий
type SSEEventType =
  | 'message_start'
  | 'status'
  | 'content_delta'
  | 'message_end'
  | 'title_updated'
  | 'error'
  | 'ping';

interface MessageStartData {
  messageId: string;
  chatId: string;
}

interface StatusData {
  stage: 'extracting' | 'searching' | 'formatting';
}

interface ContentDeltaData {
  delta: string;
}

interface MessageEndData {
  messageId: string;
  finishReason: 'stop' | 'length';
}

interface TitleUpdatedData {
  chatId: string;
  title: string;
}

interface SSEErrorData {
  code: string;
  message: string;
}
```

### Пример использования (Angular)

```typescript
@Injectable()
export class ChatService {
  private authService = inject(AuthService);

  streamResponse(chatId: string, messageId: string): Observable<SSEEvent> {
    const token = this.authService.getToken();
    const url = `/api/v1/chats/${chatId}/messages/stream?messageId=${messageId}&token=${token}`;
    
    return new Observable(observer => {
      const eventSource = new EventSource(url);
      
      eventSource.addEventListener('message_start', (e) => {
        observer.next({ type: 'message_start', data: JSON.parse(e.data) });
      });
      
      eventSource.addEventListener('status', (e) => {
        observer.next({ type: 'status', data: JSON.parse(e.data) });
      });
      
      eventSource.addEventListener('content_delta', (e) => {
        observer.next({ type: 'content_delta', data: JSON.parse(e.data) });
      });
      
      eventSource.addEventListener('message_end', (e) => {
        observer.next({ type: 'message_end', data: JSON.parse(e.data) });
        eventSource.close();
        observer.complete();
      });
      
      eventSource.addEventListener('error', (e) => {
        if (e.data) {
          observer.next({ type: 'error', data: JSON.parse(e.data) });
        }
        eventSource.close();
        observer.complete();
      });
      
      return () => eventSource.close();
    });
  }
}
```

### Рекомендации по реализации

1. **Буферизация:** Накапливать `content_delta` и обновлять UI с debounce ~50ms
2. **Статусы:** Показывать индикатор текущего этапа (extracting → searching → formatting)
3. **Reconnect:** При обрыве соединения — retry через 3 сек (max 3 попытки)
4. **Timeout:** Если нет событий 30 сек (кроме ping) — считать ошибкой
5. **Отмена:** При уходе со страницы — закрыть EventSource

---

## UI отображение статусов

| Stage | Текст для пользователя |
|-------|------------------------|
| extracting | "Анализирую запрос..." |
| searching | "Ищу автомобили..." |
| formatting | "Подготавливаю ответ..." |

---

## Таймауты

| Параметр | Значение |
|----------|----------|
| Ping interval | 15 секунд |
| Connection timeout | 60 секунд |
| Max response time | 120 секунд |
| Reconnect delay | 3 секунды |
| Max reconnect attempts | 3 |
