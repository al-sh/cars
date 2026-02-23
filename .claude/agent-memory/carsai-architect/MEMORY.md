# CarsAI Architect Memory

## Структура проекта
- Frontend: `/Users/ash/Developer/cars/front/` — Angular 19
- Backend: `/Users/ash/Developer/cars/back/` — Spring Boot 3, Java 21
- Requirements: `/Users/ash/Developer/cars/requirements/`

## Текущий статус (2026-02-23)
- Frontend: этапы 1-4 из 19 завершены (план в `front/plan.md`), интегрирован с бэкендом
- Backend: Auth (JWT), Chat CRUD, Message entity — реализованы
- LLM интеграция: **не реализована** — ни на бэкенде, ни на фронтенде
- Нет: LLMService, LLMProvider, CarService, Car entity, SSE streaming, MessageController
- Следующий этап: **L1 — Car Entity и CarService**

## Принятые архитектурные решения (2026-02-23)

### Накопление контекста диалога (РЕШЕНО)
- **Подход A**: Backend хранит `accumulatedCriteria` (JSONB) в таблице chats
- Backend формирует summary + текущее сообщение для LLM Extract
- LLM возвращает только новые критерии, Backend мержит non-null значения
- readyToSearch проверяется по объединённым критериям
- Описано в: `requirements/domain/llm-prompts.md` -> "Накопление контекста диалога"

### Авторизация SSE (РЕШЕНО)
- JWT передаётся как query parameter `token` (EventSource не поддерживает кастомные headers)
- URL: `GET /api/v1/chats/{chatId}/messages/stream?messageId={id}&token={jwt}`
- Описано в: `requirements/contracts/sse.md` -> "Авторизация"

### Генерация title чата (РЕШЕНО)
- Вызывается @Async после первого user message, если chat.title == null
- Не перезаписывает title, заданный пользователем вручную
- Фронтенд узнаёт через SSE событие `title_updated`
- Описано в: `requirements/backend/chat-service.md` -> "Генерация title чата"

### Множественные бренды (РЕШЕНО)
- **MVP: один бренд** (`brand: string`), расширение до массива — после MVP

## Ключевые архитектурные решения
- LLM не имеет доступа к БД; Backend управляет всем процессом
- 3 режима LLM: Extract, Format, Guard (см. `domain/llm-prompts.md`)
- SSE для стриминга ответов (см. `contracts/sse.md`)
- Абстракция LLMProvider для смены провайдера
- camelCase для JSON контрактов; type вместо interface на фронте

## Паттерны в коде
- Frontend: inject() вместо constructor DI, Signals для состояния
- Backend: @RequiredArgsConstructor, Specification для динамических запросов
- Commit messages на русском, одна строка, без Co-Authored-By

## LLM-интеграция: план этапов
- L1: Car Entity + CarService (backend) — **СЛЕДУЮЩИЙ**
- L2: LLM Provider + LLMService (backend) — параллельно с L1
- L3: MessageService + SSE Streaming (backend) — зависит от L1, L2
- L4: SSE-клиент (frontend) — зависит от L3
- L5: UI стриминга (frontend) — зависит от L4
- L6: Полировка и edge cases (full-stack)
