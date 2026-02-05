# Требования CarsAI

Документация требований к сервису подбора автомобилей.

---

## Навигация

### Общий контекст
- **[CONTEXT.md](CONTEXT.md)** — описание проекта, роли, стек технологий, ограничения

### Контракты (API, типы)
- [contracts/api.md](contracts/api.md) — REST API эндпоинты
- [contracts/types.md](contracts/types.md) — общие типы данных
- [contracts/sse.md](contracts/sse.md) — SSE протокол стриминга

### Бизнес-логика
- [domain/car-selection.md](domain/car-selection.md) — правила подбора автомобиля
- [domain/llm-prompts.md](domain/llm-prompts.md) — системный промт, tools

### Frontend
- [frontend/chat.md](frontend/chat.md) — UI чата с ИИ
- [frontend/manager.md](frontend/manager.md) — панель менеджера
- [frontend/auth.md](frontend/auth.md) — аутентификация

### Backend
- [backend/chat-service.md](backend/chat-service.md) — сервис чатов и LLM
- [backend/user-service.md](backend/user-service.md) — аутентификация и пользователи
- [backend/car-service.md](backend/car-service.md) — справочник автомобилей

### Инфраструктура
- [infrastructure/database.md](infrastructure/database.md) — схема БД
- [infrastructure/deployment.md](infrastructure/deployment.md) — Docker, деплой
- [infrastructure/deepseek-integration.md](infrastructure/deepseek-integration.md) — интеграция с DeepSeek API

---

## Как использовать с LLM

### Агенты проекта

| Агент | Файл | Назначение |
|-------|------|------------|
| Архитектор | `.cursor/architecture-agent.md` | Проектирование, разработка, ответы на вопросы |
| Критик | `.cursor/architecture-critic.md` | Ревью требований, поиск дублирования и неконсистентности |

Агенты автоматически активируются через `.cursorrules`.

### Для работы с архитектурой и требованиями

```
@.cursor/architecture-agent.md
@requirements/CONTEXT.md
@requirements/contracts/types.md
@requirements/frontend/chat.md (или другие релевантные требования)

[Ваш вопрос по архитектуре или требованиям]
```

### Для ревью требований

```
@.cursor/architecture-critic.md
@requirements/CONTEXT.md
@requirements/contracts/types.md

Проведи ревью документа @requirements/frontend/chat.md
```

**Критик проверяет:**
- Структуру и организацию требований
- Дублирование информации между документами
- Неконсистентность (типы, нейминг, ограничения)
- Избыточную сложность (YAGNI, gold plating)

### Для генерации UI чата

```
@requirements/CONTEXT.md
@requirements/contracts/api.md
@requirements/contracts/types.md
@requirements/contracts/sse.md
@requirements/frontend/chat.md

Сгенерируй Angular компоненты для чата согласно требованиям.
```

### Для генерации Backend сервиса чатов

```
@requirements/CONTEXT.md
@requirements/contracts/api.md
@requirements/contracts/types.md
@requirements/contracts/sse.md
@requirements/domain/llm-prompts.md
@requirements/infrastructure/database.md
@requirements/backend/chat-service.md

Сгенерируй Spring Boot сервис для чата согласно требованиям.
```

### Для генерации схемы БД

```
@requirements/CONTEXT.md
@requirements/contracts/types.md
@requirements/infrastructure/database.md

Сгенерируй Flyway миграции для PostgreSQL.
```

---

## Архив

- [_archive_requirements.md](_archive_requirements.md) — исходный файл требований
