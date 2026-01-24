# REST API Контракт

Base URL: `/api/v1`

Аутентификация: Bearer JWT token в заголовке `Authorization`

---

## Аутентификация

### POST /auth/register
Регистрация нового пользователя.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "name": "Иван Петров"
}
```

**Response 201:**
```json
{
  "user": User,
  "access_token": "eyJhbG...",
  "refresh_token": "eyJhbG..."
}
```

**Errors:** 400 (validation), 409 (email exists)

### POST /auth/login
Вход в систему.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response 200:**
```json
{
  "user": User,
  "access_token": "eyJhbG...",
  "refresh_token": "eyJhbG..."
}
```

**Errors:** 401 (invalid credentials)

### POST /auth/refresh
Обновление access token.

**Request:**
```json
{
  "refresh_token": "eyJhbG..."
}
```

**Response 200:**
```json
{
  "access_token": "eyJhbG...",
  "refresh_token": "eyJhbG..."
}
```

### POST /auth/logout
Выход (инвалидация refresh token).

**Response 204:** No content

---

## Чаты

### GET /chats
Список чатов текущего пользователя.

**Query params:**
| Параметр | Тип | Default | Описание |
|----------|-----|---------|----------|
| page | int | 1 | Номер страницы |
| per_page | int | 20 | Элементов на странице (max: 50) |
| search | string | — | Поиск по названию |

**Response 200:**
```json
{
  "items": [Chat],
  "total": 42,
  "page": 1,
  "per_page": 20
}
```

### POST /chats
Создать новый чат.

**Request:** (опционально)
```json
{
  "title": "Подбор кроссовера"
}
```

**Response 201:** `Chat`

### GET /chats/{id}
Получить чат по ID.

**Response 200:** `Chat`

**Errors:** 404

### PATCH /chats/{id}
Обновить чат (переименование).

**Request:**
```json
{
  "title": "Новое название"
}
```

**Response 200:** `Chat`

### DELETE /chats/{id}
Удалить чат (soft delete).

**Response 204:** No content

---

## Сообщения

### GET /chats/{id}/messages
История сообщений чата.

**Query params:**
| Параметр | Тип | Default | Описание |
|----------|-----|---------|----------|
| limit | int | 50 | Количество сообщений |
| before | string | — | ID сообщения для пагинации вверх |

**Response 200:**
```json
{
  "items": [Message],
  "has_more": true
}
```

### POST /chats/{id}/messages
Отправить сообщение пользователя.

**Request:**
```json
{
  "content": "Ищу кроссовер до 3 млн"
}
```

**Response 201:**
```json
{
  "user_message": Message,
  "stream_url": "/chats/{id}/stream?message_id=msg_789"
}
```

**Errors:** 400 (empty/too long), 429 (rate limit)

---

## Пользователи (для менеджера)

### GET /users
Список пользователей (только для роли manager).

**Query params:**
| Параметр | Тип | Default | Описание |
|----------|-----|---------|----------|
| page | int | 1 | Номер страницы |
| per_page | int | 20 | Элементов на странице |
| search | string | — | Поиск по имени/email |
| role | string | — | Фильтр по роли |

**Response 200:**
```json
{
  "items": [User],
  "total": 100,
  "page": 1,
  "per_page": 20
}
```

### GET /users/{id}
Получить пользователя по ID.

**Response 200:** `User`

### GET /users/{id}/chats
Чаты конкретного пользователя (для менеджера).

**Response 200:**
```json
{
  "items": [Chat],
  "total": 5
}
```

---

## Справочник автомобилей

### GET /cars
Поиск автомобилей.

**Query params:**
| Параметр | Тип | Описание |
|----------|-----|----------|
| min_price | int | Минимальная цена |
| max_price | int | Максимальная цена |
| body_type | string | Тип кузова |
| engine_type | string | Тип двигателя |
| brand | string | Марка |
| min_year | int | Год от |
| max_year | int | Год до |
| seats | int | Количество мест |
| transmission | string | Тип КПП |
| drive | string | Тип привода |
| page | int | Страница |
| per_page | int | На странице |

**Response 200:**
```json
{
  "items": [Car],
  "total": 150,
  "page": 1,
  "per_page": 20
}
```

### GET /cars/{id}
Получить автомобиль по ID.

**Response 200:** `Car`

---

## Коды ошибок

| Код | Описание |
|-----|----------|
| 400 | Bad Request — невалидные данные |
| 401 | Unauthorized — токен отсутствует или невалиден |
| 403 | Forbidden — нет прав на операцию |
| 404 | Not Found — ресурс не найден |
| 409 | Conflict — конфликт (например, email занят) |
| 429 | Too Many Requests — превышен rate limit |
| 500 | Internal Server Error |
| 503 | Service Unavailable — LLM недоступен |

## Формат ошибки

```json
{
  "error": {
    "code": "validation_error",
    "message": "Сообщение не может быть пустым",
    "details": {
      "field": "content",
      "reason": "required"
    }
  }
}
```
