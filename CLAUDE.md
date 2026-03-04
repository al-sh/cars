# CLAUDE.md

Этот файл содержит руководство для Claude Code (claude.ai/code) при работе с кодом в этом репозитории.

## Обзор проекта

CarsAI — ИИ-ассистент для подбора автомобилей. Пользователи описывают требования в чате, ИИ задаёт уточняющие вопросы и предлагает подходящие варианты из базы данных.

**Стек:** Angular 19 + TypeScript (frontend), Java 21 + Spring Boot 3 (backend), PostgreSQL, DeepSeek API.

**Текущий статус:** Frontend MVP завершён (все 19 этапов). Backend в активной разработке.

## Команды

Все основные команды вынесены в `Makefile` в корне проекта:

```bash
make dev          # Запустить frontend + backend одновременно
make back         # Запустить только backend (Spring Boot, порт 8080)
make front        # Запустить только frontend (Angular, порт 4200)
make db           # Поднять PostgreSQL в Docker (back/docker-compose.yml)
make test         # Запустить тесты backend (mvn test)
make back-stop    # Остановить backend
make front-stop   # Остановить frontend
make db-stop      # Остановить PostgreSQL
```

```bash
# Frontend (выполнять из /front)
npm start         # Dev-сервер (аналог make front)
npm test          # Тесты
npm run build     # Production сборка
```

## Структура проекта

```
front/src/app/
├── core/           # Модели, сервисы, моки
├── features/       # Feature-модули (chat/)
└── shared/         # Общие компоненты

back/src/main/java/com/carsai/back/
├── chat/           # Chat entity, ChatService, ChatController, dto/
├── message/        # Message entity, MessageService, MessageController, dto/
├── car/            # Car entity, CarService, CarController, dto/
├── user/           # User entity, AuthService, AuthController, dto/
├── llm/            # LLMProvider, DeepSeekLLMProvider, LLMService, dto/
├── config/         # SecurityConfig, CorsConfig, LLMConfig, LLMProperties
├── security/       # JwtTokenService, JwtAuthenticationFilter, UserPrincipal
└── common/         # GlobalExceptionHandler, exception/, dto/ (PagedResponse, ErrorResponse)

back/src/main/resources/
└── db/migration/   # Flyway миграции (V1__, V2__, ...)

requirements/       # Документация по архитектуре и требованиям
```

## Документация

**ОБЯЗАТЕЛЬНО** изучи релевантные документы перед внесением изменений:

| Документ | Содержимое |
|----------|------------|
| `requirements/CONTEXT.md` | Скоуп проекта, ограничения, стек |
| `requirements/contracts/types.md` | Общие типы данных (frontend ↔ backend) |
| `requirements/contracts/api.md` | REST API контракты |
| `requirements/contracts/sse.md` | SSE для стриминга LLM |
| `requirements/backend/chat-service.md` | Архитектура чат-сервиса, LLM flow |
| `requirements/backend/car-service.md` | Поиск автомобилей |
| `requirements/backend/user-service.md` | Аутентификация |
| `requirements/frontend/chat.md` | UI чата |
| `requirements/frontend/auth.md` | UI аутентификации |
| `requirements/frontend/manager.md` | Панель менеджера |
| `requirements/domain/car-selection.md` | Логика подбора авто |
| `requirements/domain/llm-prompts.md` | Промпты для LLM |
| `requirements/infrastructure/database.md` | Схема БД |
| `requirements/infrastructure/deployment.md` | Docker Compose деплой |
| `requirements/infrastructure/deepseek-integration.md` | Интеграция с DeepSeek |
| `front/STYLE.md` | Правила стиля Angular/TypeScript |
| `back/STYLE.md` | Правила стиля Java/Spring Boot |
| `front/plan.md` | Этапы разработки frontend, паттерны Angular |

## Правила для Claude Code

- **Не добавлять `Co-Authored-By` в коммиты**
- **Отвечать на русском языке**, на английском допустимы только термины, которые не принято переводить
- **Читать документацию перед изменениями** — не вносить правки без изучения соответствующего документа из таблицы выше
- **Соблюдать правила нейминга** — camelCase для полей API/контрактов, нарушение ломает совместимость frontend ↔ backend; подробности в `front/STYLE.md` и `back/STYLE.md`
- **Не менять код за пределами задачи** — не рефакторить, не добавлять фичи, не чистить соседний код без явной просьбы
