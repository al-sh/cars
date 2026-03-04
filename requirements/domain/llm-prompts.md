# LLM: Промты и режимы работы

> **Зависимости:** CONTEXT.md

Конфигурация промтов для языковой модели. Архитектура взаимодействия описана в `backend/chat-service.md`.

**Режимы работы LLM:**
1. **[Extract](prompts/extract.md)** — извлечение критериев поиска из сообщения пользователя
2. **[Format](prompts/format.md)** — форматирование результатов поиска в читаемый текст
3. **[Guard](prompts/guard.md)** — проверка релевантности запроса (опционально)

---

## Примеры полного цикла

### Пример 1: Полный запрос (без уточнений)

**User:** Ищу семейный 7-местный кроссовер, бензин, до 7 млн, автомат

**Backend → LLM (Guard):** → isRelevant: true

**Backend → LLM (Extract):**
```json
{
  "readyToSearch": true,
  "criteria": {
    "priceMax": 7000000,
    "bodyType": "suv",
    "engineType": "petrol",
    "seats": 7,
    "transmission": "automatic"
  },
  "clarificationQuestion": null,
  "extractedInfo": "7-местный кроссовер, бензин, автомат, до 7 000 000 ₽"
}
```
*5 дополнительных критериев → readyToSearch: true*

**Backend → PostgreSQL:** SELECT * FROM cars WHERE ...

**Backend → LLM (Format):** { results: [...] }

**Backend → User (SSE stream):**
```
Нашёл 3 семейных кроссовера под ваши требования:

1. **Toyota Highlander 2023** — 6 500 000 ₽
   3.5 л бензин, 249 л.с., автомат, полный привод, 7 мест

2. **Kia Sorento 2023** — 5 200 000 ₽
   2.5 л бензин, 180 л.с., автомат, полный привод, 7 мест

Хотите подробнее о каком-то варианте?
```

---

### Пример 2: Требуется уточнение (мало критериев)

**User:** Кроссовер до 3 млн

**Backend → LLM (Guard):** → isRelevant: true

**Backend → LLM (Extract):**
```json
{
  "readyToSearch": false,
  "criteria": {
    "priceMax": 3000000,
    "bodyType": "suv"
  },
  "clarificationQuestion": "Кроссовер до 3 млн — хороший выбор! Уточните:\n1. Бензин, дизель или гибрид?\n2. Автомат или механика?",
  "extractedInfo": "Кроссовер до 3 000 000 ₽ (1 доп. критерий, нужно минимум 2)"
}
```
*Только 1 дополнительный критерий → readyToSearch: false*

**Backend → User:** (возвращает clarificationQuestion)

---

### Пример 3: Требуется уточнение (нет цены)

**User:** Хочу машину для семьи

**Backend → LLM (Guard):** → isRelevant: true

**Backend → LLM (Extract):**
```json
{
  "readyToSearch": false,
  "criteria": {},
  "clarificationQuestion": "Помогу подобрать семейный автомобиль! Подскажите:\n1. На какую сумму рассчитываете?\n2. Сколько мест нужно — 5 или 7?",
  "extractedInfo": "Семейный автомобиль, критерии не указаны"
}
```

**Backend → User:** (возвращает clarificationQuestion)

---

### Пример 4: Нет результатов

**User:** Электромобиль до 1 млн

**Backend → LLM (Extract):** readyToSearch: true, criteria: {...}

**Backend → PostgreSQL:** 0 results

**Backend → LLM (Format):** { results: [], userCriteria: "..." }

**Backend → User (SSE stream):**
```
К сожалению, в нашей базе нет электромобилей в бюджете до 1 000 000 ₽.

Могу предложить:
- Увеличить бюджет до 3-4 млн — появятся варианты вроде Zeekr или б/у Tesla
- Рассмотреть гибриды — они доступнее и тоже экономичны

Хотите скорректировать критерии?
```

---

## Формирование контекста

### Структура сообщений

```json
[
  {"role": "system", "content": "<системный промт режима>"},
  {"role": "user", "content": "<запрос или данные>"}
]
```

**Важно:** Каждый запрос к LLM — независимый. LLM не получает полную историю диалога. Вместо этого Backend передаёт summary накопленных критериев вместе с текущим сообщением (см. "Накопление контекста диалога" ниже).

### Ограничения

- Системный промт: ~500 токенов
- Запрос пользователя: до 4000 символов
- Результаты поиска: до 10 автомобилей
- Ответ LLM: до 1024 токенов

---

## Накопление контекста диалога

### Проблема

Пользователь может давать ответы по частям в нескольких сообщениях:

```
User: "Хочу кроссовер до 3 млн"        → criteria: {bodyType: "suv", priceMax: 3000000}
Assistant: "Уточните тип двигателя и КПП?"
User: "Бензин, автомат"                  → criteria: {engineType: "petrol", transmission: "automatic"}
```

Без контекста LLM Extract получит только "Бензин, автомат" и не узнает про кроссовер и цену. Критерии не накопятся.

### Решение: накопление на бэкенде (подход A)

Backend хранит `accumulatedCriteria` (JSON) в поле таблицы `chats` для каждого чата. При каждом сообщении:

1. Backend формирует summary из `accumulatedCriteria` и передаёт в LLM Extract вместе с текущим сообщением
2. LLM Extract возвращает **только новые/обновлённые критерии** из текущего сообщения
3. Backend мержит результат: `accumulatedCriteria = {...old, ...newNonNull}`
4. Backend проверяет readyToSearch по **объединённым** критериям

### Формат user message для LLM Extract

Если в чате уже есть накопленные критерии, Backend формирует user message в формате:

```
Ранее известно: кроссовер до 3 000 000 ₽ (bodyType: suv, priceMax: 3000000).
Текущее сообщение пользователя: Бензин, автомат
```

Если накопленных критериев нет (первое сообщение), передаётся только текущее сообщение.

### Правила merge

- Новые non-null критерии перезаписывают старые: `{priceMax: 3000000} + {priceMax: 4000000}` → `{priceMax: 4000000}`
- null-значения в ответе LLM **не** стирают старые: LLM возвращает null для критериев, которые пользователь не упомянул в текущем сообщении
- readyToSearch проверяется по объединённым критериям, а не по ответу LLM

### Пример полного цикла с накоплением

**Сообщение 1:**
```
User: "Хочу кроссовер до 3 млн"
→ LLM Extract получает: "Хочу кроссовер до 3 млн"
→ LLM возвращает: criteria: {priceMax: 3000000, bodyType: "suv"}, readyToSearch: false
→ Backend сохраняет accumulatedCriteria: {priceMax: 3000000, bodyType: "suv"}
→ Объединённые критерии: priceMax + 1 доп. (bodyType) → не готов к поиску
→ Ответ: clarificationQuestion
```

**Сообщение 2:**
```
User: "Бензин, автомат"
→ LLM Extract получает:
  "Ранее известно: кроссовер до 3 000 000 ₽ (bodyType: suv, priceMax: 3000000).
   Текущее сообщение пользователя: Бензин, автомат"
→ LLM возвращает: criteria: {engineType: "petrol", transmission: "automatic"}, readyToSearch: true
→ Backend мержит: {priceMax: 3000000, bodyType: "suv", engineType: "petrol", transmission: "automatic"}
→ Объединённые критерии: priceMax + 3 доп. → готов к поиску
→ Backend выполняет поиск в БД по объединённым критериям
```

### Хранение в БД

Поле `accumulated_criteria` (JSONB) в таблице `chats`:

```sql
ALTER TABLE chats ADD COLUMN accumulated_criteria JSONB;
```

```java
// В Chat entity
@Type(JsonBinaryType.class)
@Column(name = "accumulated_criteria", columnDefinition = "jsonb")
private Map<String, Object> accumulatedCriteria;
```

### Сброс критериев

Критерии сбрасываются только при:
- Создании нового чата (accumulatedCriteria = null)
- Явном запросе пользователя "начать заново" / "сбросить критерии" (определяется через Guard или Extract)

---

## Конфигурация модели

```yaml
# DeepSeek API конфигурация
model: deepseek-chat  # Основная модель для чата
# Альтернатива: deepseek-reasoner для сложных задач, требующих рассуждений

temperature: 0.3   # Низкая для структурированных ответов (Extract)
                   # 0.7 для форматирования (Format)
max_tokens: 1024
top_p: 0.9
```

### Модели DeepSeek API

| Модель | Описание | Использование |
|--------|----------|---------------|
| deepseek-chat | Основная модель для чата | Extract, Format, Guard режимы |
| deepseek-reasoner | Модель с улучшенными способностями к рассуждению | Сложные задачи, требующие логики |

### Преимущества DeepSeek API

- ✅ Не требует локальных ресурсов (CPU/GPU/память)
- ✅ Хорошая поддержка русского языка
- ✅ Быстрая скорость ответа
- ✅ Автоматическое масштабирование
- ✅ Не нужно управлять моделями и обновлениями

### Ограничения

- ⚠️ Требуется стабильное интернет-соединение
- ⚠️ Зависимость от внешнего сервиса
- ⚠️ Rate limiting согласно тарифу
- ⚠️ Требуется API ключ (безопасное хранение)
