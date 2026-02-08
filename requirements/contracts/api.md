# REST API Контракт (индекс)

Этот документ **не дублирует эндпоинты**. Единственный источник истины по REST API —
`requirements/contracts/openapi.yaml`.

## Как пользоваться

- **OpenAPI**: `requirements/contracts/openapi.yaml`
- **Типы/правила сериализации**: `requirements/contracts/types.md`
- **SSE протокол**: `requirements/contracts/sse.md`

## Базовые договорённости

- **Base URL**: `/api/v1`
- **Auth**: `Authorization: Bearer {token}`
- **JSON поля**: camelCase
- **Enum в JSON**: строки lower-case (см. `types.md`)

## Формат ошибки (общий)

```json
{
  "code": "validation_error",
  "message": "Сообщение не может быть пустым"
}
```
