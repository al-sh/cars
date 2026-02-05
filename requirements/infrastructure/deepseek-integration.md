# Интеграция с DeepSeek API

> **Зависимости:** CONTEXT.md, backend/chat-service.md

Требования к интеграции с внешним LLM провайдером DeepSeek.

---

## Обзор

DeepSeek API используется как основной LLM-провайдер для:
- Извлечения критериев поиска из сообщений пользователя
- Форматирования ответов с результатами поиска
- Генерации названий чатов

---

## Конфигурация

### Переменные окружения

```bash
# Обязательные
DEEPSEEK_API_KEY=sk-xxx          # API ключ (не коммитить!)
DEEPSEEK_BASE_URL=https://api.deepseek.com

# Опциональные
DEEPSEEK_MODEL=deepseek-chat     # Модель по умолчанию
DEEPSEEK_TIMEOUT=60s             # Таймаут запроса
DEEPSEEK_MAX_TOKENS=1024         # Максимум токенов в ответе
```

### application.yml

```yaml
llm:
  provider: deepseek
  api-key: ${DEEPSEEK_API_KEY}
  base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
  model: ${DEEPSEEK_MODEL:deepseek-chat}
  timeout: ${DEEPSEEK_TIMEOUT:60s}
  max-tokens: ${DEEPSEEK_MAX_TOKENS:1024}
  
  temperature:
    extract: 0.3    # Низкая для структурированных ответов
    format: 0.7     # Выше для естественного текста
  
  retry:
    max-attempts: 3
    initial-delay: 1s
    multiplier: 2   # Exponential backoff
```

---

## API эндпоинт

```
POST https://api.deepseek.com/v1/chat/completions
```

### Заголовки

```
Authorization: Bearer {api_key}
Content-Type: application/json
```

### Формат запроса

```json
{
  "model": "deepseek-chat",
  "messages": [
    { "role": "system", "content": "..." },
    { "role": "user", "content": "..." }
  ],
  "temperature": 0.7,
  "max_tokens": 1024,
  "stream": false
}
```

### Формат ответа

```json
{
  "id": "chatcmpl-xxx",
  "object": "chat.completion",
  "created": 1234567890,
  "model": "deepseek-chat",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 100,
    "completion_tokens": 50,
    "total_tokens": 150
  }
}
```

---

## Обработка ошибок

| HTTP код | Причина | Действие |
|----------|---------|----------|
| 400 | Невалидный запрос | Логировать, вернуть ошибку пользователю |
| 401 | Неверный API ключ | Логировать CRITICAL, вернуть 503 пользователю |
| 429 | Rate limit | Retry с exponential backoff |
| 500 | Ошибка сервера | Retry (до 3 попыток) |
| 503 | Сервис недоступен | Retry, затем fallback |
| Timeout | Превышен таймаут | Retry (до 2 попыток), затем fallback |

### Retry стратегия

```
Попытка 1: немедленно
Попытка 2: через 1 сек
Попытка 3: через 2 сек (exponential backoff)
```

### Fallback сообщения

При недоступности API после всех retry:

```
Извините, сервис временно недоступен. Пожалуйста, попробуйте позже.
```

---

## Мониторинг затрат

### Логирование токенов

Каждый запрос к API должен логировать:

```
[LLM] chat_id={} mode={} prompt_tokens={} completion_tokens={} total_tokens={}
```

### Метрики (Micrometer)

```java
// Счётчик токенов
llm.tokens.prompt (counter)
llm.tokens.completion (counter)

// Время ответа
llm.latency (timer, tags: mode=[extract|format|title])

// Ошибки
llm.errors (counter, tags: type=[timeout|rate_limit|unavailable])
```

### Алерты (опционально)

- Превышение дневного лимита токенов
- Частые ошибки rate limiting
- Высокая латентность (>30 сек)

---

## Безопасность

### API ключи

- **Никогда** не коммитить в репозиторий
- Хранить в переменных окружения или секретах
- Использовать разные ключи для dev/prod
- Ротация ключей при компрометации

### .gitignore

```
.env
.env.local
application-local.yml
```

### Пример .env.example

```bash
# DeepSeek API
DEEPSEEK_API_KEY=your-api-key-here
```

---

## Rate Limits

Согласно тарифу DeepSeek (уточнить актуальные значения):

| Лимит | Значение |
|-------|----------|
| Запросов в минуту | ~60 RPM |
| Токенов в минуту | ~100K TPM |

### Рекомендации

- Реализовать retry с exponential backoff
- Не делать параллельные запросы без необходимости
- Кэшировать повторяющиеся запросы (если уместно)

---

## Абстракция провайдера

Для возможности смены провайдера используется интерфейс `LLMProvider`.

См. `backend/chat-service.md` → раздел "Абстракция LLM провайдера".

Это позволяет:
- Легко переключиться на другой API (OpenAI, Anthropic)
- Использовать mock для тестирования
- Добавить fallback провайдер

---

## Тестирование

### Unit tests

- Mock `LLMProvider` для изоляции
- Тесты парсинга ответов
- Тесты обработки ошибок

### Integration tests

```java
@TestConfiguration
public class LLMTestConfig {
    @Bean
    @Primary
    public LLMProvider mockLLMProvider() {
        // Mock с предопределёнными ответами
    }
}
```

### Ручное тестирование

```bash
curl -X POST https://api.deepseek.com/v1/chat/completions \
  -H "Authorization: Bearer $DEEPSEEK_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-chat",
    "messages": [{"role": "user", "content": "Привет!"}]
  }'
```

---

## Checklist при настройке

- [ ] Получить API ключ на platform.deepseek.com
- [ ] Добавить переменные окружения
- [ ] Проверить подключение (curl)
- [ ] Настроить логирование токенов
- [ ] Настроить метрики (опционально)
- [ ] Проверить retry логику
- [ ] Добавить .env.example в репозиторий
