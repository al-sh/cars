# Правила стиля кода

## TypeScript

### Типы и интерфейсы

- ✅ **Использовать только `type`** для определения типов
- ❌ **Избегать `interface`** (кроме случаев, когда требуется declaration merging)

**Примеры:**

```typescript
// ✅ Правильно
export type Chat = {
  id: string;
  title: string | null;
};

export type MessageRole = 'user' | 'assistant' | 'system';

// ❌ Неправильно
export interface Chat {
  id: string;
  title: string | null;
}
```

**Причина:** Консистентность и гибкость. `type` поддерживает union types, intersection types и другие продвинутые возможности TypeScript.

---

## Именование

### Файлы

- **Компоненты:** `kebab-case.component.ts` (например, `chat-list.component.ts`)
- **Сервисы:** `kebab-case.service.ts` (например, `chat.service.ts`)
- **Модели:** `kebab-case.model.ts` (например, `chat.model.ts`)
- **Утилиты:** `kebab-case.util.ts` или `kebab-case.ts`
- **Константы/моки:** `kebab-case.ts` (например, `mock-data.ts`)

### Переменные и функции

- **Переменные:** `camelCase` (например, `currentChatId`, `isLoading`)
- **Константы:** `UPPER_SNAKE_CASE` (например, `MOCK_CHATS`, `API_BASE_URL`)
- **Функции:** `camelCase` (например, `getChatById`, `handleSubmit`)
- **Классы:** `PascalCase` (например, `ChatService`, `ChatListComponent`)
- **Типы:** `PascalCase` (например, `Chat`, `MessageRole`)

### Приватные свойства

- **Без префикса `_`** для приватных свойств и методов
- Использовать модификатор `private` или `protected`

```typescript
// ✅ Правильно
private chatService = inject(ChatService);
private readonly isLoading = signal(false);

// ❌ Неправильно
private _chatService = inject(ChatService);
private _isLoading = signal(false);
```

### Селекторы компонентов

- **Формат:** `app-feature-name` (например, `app-chat-list`, `app-message-item`)
- Префикс `app-` для всех компонентов приложения

---

## Angular

### Standalone компоненты

- ✅ **Всегда использовать standalone компоненты**
- ✅ **Явно указывать `standalone: true`**

**Пример:**

```typescript
@Component({
  selector: 'app-chat-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './chat-list.component.html',
  styleUrl: './chat-list.component.css'
})
export class ChatListComponent {
  // ...
}
```

---

## Импорты

### Группировка и порядок

1. **Angular core** (`@angular/core`, `@angular/common`, и т.д.)
2. **Angular features** (`@angular/router`, `@angular/forms`, и т.д.)
3. **RxJS** (`rxjs`, `rxjs/operators`)
4. **Сторонние библиотеки**
5. **Локальные импорты** (относительные пути)
   - Сервисы
   - Модели
   - Утилиты
   - Компоненты

**Пример:**

```typescript
// Angular core
import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

// Angular features
import { Router } from '@angular/router';
import { FormBuilder, FormControl, ReactiveFormsModule } from '@angular/forms';

// RxJS
import { Observable } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

// Локальные
import { ChatService } from '../services/chat.service';
import { Chat } from '../models/chat.model';
import { ChatItemComponent } from '../chat-item/chat-item.component';
```

### Сортировка

- ✅ **Алфавитная сортировка** внутри каждой группы
- ✅ **Пустая строка** между группами

---

## Форматирование

### Общие правила

- ✅ **Точки с запятой:** Обязательны
- ✅ **Кавычки:** Одинарные `'` для строк
- ✅ **Trailing commas:** Да (в объектах, массивах, параметрах функций)
- ✅ **Длина строки:** 100 символов (мягкий лимит, можно превышать при необходимости)
- ✅ **Отступы:** 2 пробела (не табы)

### TypeScript

```typescript
// ✅ Правильно
export type Chat = {
  id: string;
  title: string | null;
  created_at: string;
};

function processChat(
  chat: Chat,
  options: { includeMessages: boolean },
): Observable<Chat> {
  // ...
}

// ❌ Неправильно
export type Chat = {
  id: string
  title: string | null
  created_at: string
}
```

### HTML шаблоны

- ✅ **Отступы:** 2 пробела
- ✅ **Закрывающие теги:** Всегда явно (не self-closing для HTML элементов)
- ✅ **Атрибуты:** Кавычки двойные `"` в шаблонах

```html
<!-- ✅ Правильно -->
<div class="chat-list">
  @for (chat of chats(); track chat.id) {
    <app-chat-item [chat]="chat" />
  }
</div>

<!-- ❌ Неправильно -->
<div class="chat-list">
  @for(chat of chats();track chat.id){
    <app-chat-item [chat]=chat />
  }
</div>
```

### CSS

- ✅ **Отступы:** 2 пробела
- ✅ **Кавычки:** Двойные для значений атрибутов
- ✅ **Порядок свойств:** Позиционирование → Размеры → Отступы → Стили → Анимации

```css
/* ✅ Правильно */
.chat-item {
  position: relative;
  display: flex;
  width: 100%;
  padding: 12px 16px;
  background-color: var(--color-bg);
  border-radius: 8px;
  transition: background-color 0.2s ease;
}
```

---

## Комментарии

### JSDoc

- ✅ **JSDoc для публичных методов** сервисов и утилит
- ✅ **JSDoc для сложной логики** в компонентах
- ❌ **Не комментировать очевидный код**

```typescript
// ✅ Правильно
/**
 * Получает чат по ID с загрузкой сообщений
 * @param chatId - ID чата
 * @param includeMessages - Загружать ли сообщения
 * @returns Observable с данными чата
 */
getChatById(chatId: string, includeMessages = false): Observable<Chat> {
  // ...
}

// ❌ Неправильно
// Получает чат
getChatById(chatId: string) {
  // ...
}
```

### Inline комментарии

- ✅ **Комментарии на русском языке** (для внутренней документации)
- ✅ **TODO комментарии** с форматом: `// TODO: [дата] [автор] Описание`

```typescript
// TODO: 2024-01-15 ash Добавить обработку ошибок при загрузке чатов
// FIXME: При удалении чата не обновляется список
```

---

## Git

### Коммиты

- ✅ **Сообщения коммитов на русском языке**
- ✅ **Описание:** Краткое и понятное описание изменений
- ✅ **Начинать с заглавной буквы**

**Примеры:**

```bash
# ✅ Правильно
git commit -m "Реализован этап 2 - роутинг и ChatLayout компонент"
git commit -m "Исправлена ошибка отображения chatId в шаблоне"
git commit -m "Добавлен readonly модификатор к chatId signal"
git commit -m "Обновлена документация по использованию Signals"

# ❌ Неправильно
git commit -m "реализован этап 2 - роутинг и ChatLayout компонент"
git commit -m "feat: implement stage 2 - routing and ChatLayout component"
git commit -m "fix bug"
git commit -m "update"
```

---

## Обработка ошибок

### Try-Catch

- ✅ **Обрабатывать ошибки** в сервисах и компонентах
- ✅ **Логировать ошибки** в консоль для разработки
- ✅ **Показывать пользователю** понятные сообщения

```typescript
// ✅ Правильно
async loadChats(): Promise<void> {
  try {
    this.isLoading.set(true);
    const chats = await this.http.get<Chat[]>('/api/chats').toPromise();
    this.chatsSignal.set(chats);
  } catch (error) {
    console.error('Ошибка загрузки чатов:', error);
    // TODO: Показать toast уведомление пользователю
  } finally {
    this.isLoading.set(false);
  }
}
```

### Валидация

- ✅ **Валидировать данные** перед использованием
- ✅ **Использовать TypeScript strict mode** (уже включен в tsconfig.json)

---

## Тестирование

### Именование тестов

- ✅ **Формат:** `describe('ComponentName', () => { ... })`
- ✅ **Тесты:** `it('should ...', () => { ... })`

```typescript
// ✅ Правильно
describe('ChatListComponent', () => {
  it('should display list of chats', () => {
    // ...
  });
  
  it('should filter chats by search query', () => {
    // ...
  });
});
```

### Покрытие

- ✅ **Тестировать критичную логику** (сервисы, утилиты)
- ✅ **Тестировать компоненты** с важной бизнес-логикой
- ⚠️ **Простые presentational компоненты** можно не тестировать

---

## Исключения

Если правило не подходит для конкретного случая, добавьте комментарий с объяснением:

```typescript
// Исключение: Используем interface для расширения глобального типа Window
declare global {
  interface Window {
    customProperty: string;
  }
}
```
