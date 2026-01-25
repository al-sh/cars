# План разработки чата на Angular с мок-данными

## Цель

Разработать UI чата пошагово, изучая Angular 19 параллельно. Использовать мок-данные без интеграций с бэкендом. Каждый этап занимает 30-60 минут.

## Архитектура компонентов

```
AppComponent
└── ChatLayout (container)
    ├── Header
    ├── ChatList (sidebar)
    │   ├── ChatSearch
    │   └── ChatItem[]
    └── ChatWindow
        ├── ChatHeader
        │   └── DropdownMenu
        ├── MessageList
        │   └── MessageItem[]
        └── MessageInput

Shared:
├── ConfirmModal
└── DropdownMenu
```

## Структура файлов

```
front/src/app/
├── core/
│   ├── models/
│   │   ├── chat.model.ts
│   │   └── message.model.ts
│   ├── services/
│   │   └── chat.service.ts
│   └── mocks/
│       └── mock-data.ts
├── features/
│   └── chat/
│       ├── chat-layout/
│       ├── chat-list/
│       ├── chat-item/
│       ├── chat-window/
│       ├── chat-header/
│       ├── message-list/
│       ├── message-item/
│       └── message-input/
├── shared/
│   └── components/
│       ├── header/
│       ├── confirm-modal/
│       └── dropdown-menu/
├── app.component.ts
├── app.routes.ts
└── app.config.ts
```

---

## Этапы разработки

### Этап 1: Типы и мок-данные

**Время:** ~30 мин

**Цель:** Настроить типы данных и создать мок-данные для разработки.

**Задачи:**

1. Создать структуру папок: `core/models/`, `core/mocks/`
2. Создать `chat.model.ts`:
   ```typescript
   export interface Chat {
     id: string;
     user_id: string;
     title: string | null;
     created_at: string;
     updated_at: string;
     message_count: number;
   }
   ```

3. Создать `message.model.ts`:
   ```typescript
   export type MessageRole = 'user' | 'assistant' | 'system';
   
   export interface Message {
     id: string;
     chat_id: string;
     role: MessageRole;
     content: string;
     created_at: string;
   }
   ```

4. Создать `mock-data.ts` с 3-4 чатами и сообщениями для каждого
5. Настроить CSS переменные в `styles.css`:
   ```css
   :root {
     --color-primary: #3b82f6;
     --color-bg: #ffffff;
     --color-sidebar: #f8fafc;
     --color-user-msg: #3b82f6;
     --color-assistant-msg: #f1f5f9;
     --color-text: #1e293b;
     --color-text-secondary: #64748b;
     --sidebar-width: 280px;
   }
   ```

**Изучаемые концепции:**

- Структура Angular проекта
- TypeScript интерфейсы и типы
- CSS custom properties

---

### Этап 2: Роутинг и ChatLayout

**Время:** ~45 мин

**Цель:** Настроить роутинг и создать основной layout.

**Задачи:**

1. Обновить `app.component.html` - оставить только `<router-outlet />`
2. Настроить роутинг в `app.routes.ts`:
   ```typescript
   export const routes: Routes = [
     { path: '', redirectTo: 'chat', pathMatch: 'full' },
     { path: 'chat', component: ChatLayoutComponent },
     { path: 'chat/:id', component: ChatLayoutComponent },
   ];
   ```

3. Создать `ChatLayoutComponent` в `features/chat/chat-layout/`:
   - HTML: grid layout с двумя колонками
   - CSS: sidebar 280px, content - остальное
   ```html
   <div class="chat-layout">
     <aside class="sidebar">
       <!-- ChatList будет здесь -->
       <p>Sidebar placeholder</p>
     </aside>
     <main class="content">
       <!-- ChatWindow будет здесь -->
       <p>Content placeholder</p>
     </main>
   </div>
   ```

**Изучаемые концепции:**

- Angular Router (standalone)
- `RouterOutlet`
- Компоненты: декоратор `@Component`, selector, imports
- CSS Grid для layout

---

### Этап 3: Header компонент

**Время:** ~30 мин

**Цель:** Создать шапку приложения.

**Задачи:**

1. Создать `HeaderComponent` в `shared/components/header/`
2. Реализовать по wireframe из требований:
   ```
   ┌──────────────────────────────────────────────────────────┐
   │  🚗 CarsAI                                   [Иван ▼]   │
   └──────────────────────────────────────────────────────────┘
   ```

3. Добавить мок-данные пользователя (имя)
4. Подготовить место для hamburger меню (mobile)
5. Интегрировать в `ChatLayoutComponent`

**Изучаемые концепции:**

- Создание standalone компонентов
- Интерполяция `{{ }}`
- Базовая стилизация компонентов

---

### Этап 4: ChatList (статичный)

**Время:** ~45 мин

**Цель:** Отобразить статичный список чатов.

**Задачи:**

1. Создать `ChatListComponent` в `features/chat/chat-list/`
2. Создать `ChatItemComponent` в `features/chat/chat-item/`
3. В `ChatListComponent`:
   - Импортировать мок-данные напрямую
   - Кнопка "+ Новый чат" (пока без функционала)
   - Поле поиска (пока без функционала)
   - Список чатов через `@for`

4. В `ChatItemComponent`:
   - `@Input() chat: Chat`
   - `@Output() selected = new EventEmitter<string>()`
   - Отображение: название, дата (через `DatePipe`)
   - Индикатор активного чата

**Изучаемые концепции:**

- `@Input()` для передачи данных в компонент
- `@Output()` и `EventEmitter` для событий
- Новый control flow: `@for (chat of chats; track chat.id)`
- `DatePipe` для форматирования дат
- Event binding: `(click)="onSelect()"`

---

### Этап 5: ChatService с Signals

**Время:** ~45 мин

**Цель:** Создать сервис для работы с данными используя Signals.

**Задачи:**

1. Создать `ChatService` в `core/services/`:
   ```typescript
   @Injectable({ providedIn: 'root' })
   export class ChatService {
     // Signals для состояния
     private chatsSignal = signal<Chat[]>(MOCK_CHATS);
     private messagesSignal = signal<Map<string, Message[]>>(new Map());
     
     // Публичные readonly signals
     readonly chats = this.chatsSignal.asReadonly();
     
     // Computed signal для текущего чата
     readonly currentChatId = signal<string | null>(null);
     readonly currentChat = computed(() => 
       this.chats().find(c => c.id === this.currentChatId())
     );
     readonly currentMessages = computed(() => 
       this.messagesSignal().get(this.currentChatId() ?? '') ?? []
     );
     
     // Методы
     selectChat(id: string): void { ... }
     getMessages(chatId: string): Message[] { ... }
     sendMessage(chatId: string, content: string): void { ... }
   }
   ```

2. Использовать `inject()` вместо constructor DI

**Изучаемые концепции:**

- Angular Signals: `signal()`, `computed()`
- `inject()` функция (современный DI)
- `@Injectable()` и providedIn
- Управление состоянием через Signals

---

### Этап 6: ChatList (динамический)

**Время:** ~30 мин

**Цель:** Подключить ChatList к сервису.

**Задачи:**

1. В `ChatListComponent`:
   - Инжектировать `ChatService` через `inject()`
   - Использовать `service.chats()` в шаблоне
   - Убрать прямой импорт мок-данных

2. Обработать выбор чата:
   - При клике вызывать `service.selectChat(id)`
   - Добавить индикатор активного чата (сравнение с `currentChatId()`)

**Изучаемые концепции:**

- Использование Signals в шаблонах
- Реактивное обновление UI
- Связь компонентов через сервис

---

### Этап 7: ChatWindow и роутинг

**Время:** ~45 мин

**Цель:** Создать окно чата с навигацией.

**Задачи:**

1. Создать `ChatWindowComponent` в `features/chat/chat-window/`
2. Создать `ChatHeaderComponent` в `features/chat/chat-header/`:
   - Название чата
   - Заглушка для меню (три точки)

3. Реализовать логику:
   - Получать `chatId` из `ActivatedRoute`
   - Синхронизировать с `ChatService.selectChat()`
   - Пустое состояние если чат не выбран

4. Добавить навигацию при выборе чата:
   ```typescript
   private router = inject(Router);
   
   onChatSelect(chatId: string) {
     this.router.navigate(['/chat', chatId]);
   }
   ```

**Изучаемые концепции:**

- `ActivatedRoute` и `paramMap`
- `Router.navigate()`
- `@if` / `@else` для условного рендеринга
- `effect()` для реакции на изменения Signals

---

### Этап 8: MessageList

**Время:** ~45 мин

**Цель:** Отобразить список сообщений.

**Задачи:**

1. Создать `MessageListComponent` в `features/chat/message-list/`
2. Создать `MessageItemComponent` в `features/chat/message-item/`
3. В `MessageListComponent`:
   - Получать сообщения из `ChatService.currentMessages()`
   - Автоскролл вниз при новых сообщениях
   ```typescript
   @ViewChild('scrollContainer') scrollContainer!: ElementRef;
   
   ngAfterViewChecked() {
     this.scrollToBottom();
   }
   ```

4. В `MessageItemComponent`:
   - `@Input() message: Message`
   - Разные стили для user/assistant
   - Форматирование времени

**Изучаемые концепции:**

- `@ViewChild` и `ElementRef`
- Lifecycle hooks: `ngAfterViewChecked`
- Условные CSS классы: `[class.user]="message.role === 'user'"`

---

### Этап 9: MessageInput (базовый)

**Время:** ~45 мин

**Цель:** Создать базовую форму ввода сообщений.

**Задачи:**

1. Создать `MessageInputComponent` в `features/chat/message-input/`
2. Использовать Reactive Forms:
   ```typescript
   private fb = inject(FormBuilder);
   
   messageForm = this.fb.group({
     content: ['', [Validators.required, Validators.maxLength(4000)]]
   });
   ```

3. Шаблон:
   ```html
   <form [formGroup]="messageForm" (ngSubmit)="onSubmit()">
     <textarea formControlName="content" 
               placeholder="Введите сообщение...">
     </textarea>
     <button type="submit" [disabled]="messageForm.invalid">
       Отправить
     </button>
   </form>
   ```

4. Интеграция с `ChatService.sendMessage()`

**Изучаемые концепции:**

- `ReactiveFormsModule`
- `FormBuilder`, `FormGroup`, `FormControl`
- `Validators` для валидации
- `formGroup`, `formControlName` директивы

---

### Этап 10: MessageInput (улучшения)

**Время:** ~45 мин

**Цель:** Добавить валидацию, горячие клавиши и авто-resize.

**Задачи:**

1. Счётчик символов (показывать при >3500):
   ```html
   @if (content.value.length > 3500) {
     <span class="char-count">{{ content.value.length }}/4000</span>
   }
   ```

2. Горячие клавиши:
   ```typescript
   onKeydown(event: KeyboardEvent) {
     if (event.key === 'Enter' && !event.shiftKey) {
       event.preventDefault();
       this.onSubmit();
     }
   }
   ```

3. Авто-resize textarea (CSS):
   ```css
   textarea {
     field-sizing: content; /* современный способ */
     min-height: 40px;
     max-height: 120px;
   }
   ```

4. Disabled состояние во время "отправки"

**Изучаемые концепции:**

- Обработка событий клавиатуры
- Доступ к FormControl: `this.messageForm.get('content')`
- Динамические стили и классы

---

### Этап 11: Поиск чатов

**Время:** ~30 мин

**Цель:** Реализовать фильтрацию списка чатов.

**Задачи:**

1. Добавить `FormControl` для поиска в `ChatListComponent`
2. Создать computed signal для фильтрации:
   ```typescript
   searchQuery = signal('');
   
   filteredChats = computed(() => {
     const query = this.searchQuery().toLowerCase();
     if (!query) return this.chatService.chats();
     return this.chatService.chats().filter(chat => 
       chat.title?.toLowerCase().includes(query)
     );
   });
   ```

3. Связать input с signal:
   ```html
   <input type="text" 
          placeholder="Поиск..."
          (input)="searchQuery.set($event.target.value)">
   ```

4. Пустое состояние при отсутствии результатов

**Изучаемые концепции:**

- `computed()` для производных данных
- Связывание input с Signals
- Фильтрация массивов

---

### Этап 12: Создание чата

**Время:** ~30 мин

**Цель:** Реализовать создание нового чата.

**Задачи:**

1. Добавить метод в `ChatService`:
   ```typescript
   createChat(): Chat {
     const newChat: Chat = {
       id: crypto.randomUUID(),
       user_id: 'mock-user',
       title: null,
       created_at: new Date().toISOString(),
       updated_at: new Date().toISOString(),
       message_count: 0
     };
     this.chatsSignal.update(chats => [newChat, ...chats]);
     return newChat;
   }
   ```

2. В `ChatListComponent`:
   - Обработчик клика на "+ Новый чат"
   - Создание чата и навигация

3. Приветственное сообщение для нового чата (из требований):
   ```
   Здравствуйте! Я ИИ-помощник по подбору автомобилей.
   
   Расскажите, какой автомобиль вы ищете, и я помогу выбрать 
   оптимальный вариант. Можете указать:
   • Бюджет
   • Тип кузова (седан, кроссовер, хэтчбек...)
   • Для каких целей нужен автомобиль
   
   Или просто опишите свои пожелания своими словами!
   ```

**Изучаемые концепции:**

- `signal.update()` для обновления состояния
- Иммутабельные обновления массивов
- Навигация после действия

---

### Этап 13: Inline-редактирование названия

**Время:** ~45 мин

**Цель:** Редактирование названия чата по двойному клику.

**Задачи:**

1. В `ChatItemComponent`:
   - Состояние `isEditing = signal(false)`
   - Двойной клик переключает в режим редактирования
   ```html
   @if (isEditing()) {
     <input #editInput
            [value]="chat.title"
            (blur)="saveTitle($event)"
            (keydown.enter)="saveTitle($event)"
            (keydown.escape)="cancelEdit()">
   } @else {
     <span (dblclick)="startEdit()">{{ chat.title }}</span>
   }
   ```

2. Автофокус на input при редактировании:
   ```typescript
   @ViewChild('editInput') editInput?: ElementRef<HTMLInputElement>;
   
   startEdit() {
     this.isEditing.set(true);
     setTimeout(() => this.editInput?.nativeElement.focus());
   }
   ```

3. Добавить метод `updateChat()` в `ChatService`

**Изучаемые концепции:**

- Локальные Signals в компонентах
- `@ViewChild` с template reference
- События: `dblclick`, `blur`, `keydown.enter`

---

### Этап 14: Удаление чата с модалкой

**Время:** ~45 мин

**Цель:** Создать переиспользуемую модалку подтверждения.

**Задачи:**

1. Создать `ConfirmModalComponent` в `shared/components/`:
   ```typescript
   @Component({
     selector: 'app-confirm-modal',
     template: `
       <div class="modal-backdrop" (click)="onCancel()">
         <div class="modal" (click)="$event.stopPropagation()">
           <h3>{{ title() }}</h3>
           <p>{{ message() }}</p>
           <div class="actions">
             <button (click)="onCancel()">Отмена</button>
             <button class="danger" (click)="onConfirm()">{{ confirmText() }}</button>
           </div>
         </div>
       </div>
     `
   })
   export class ConfirmModalComponent {
     title = input('Подтверждение');
     message = input('Вы уверены?');
     confirmText = input('Удалить');
     
     confirmed = output<void>();
     cancelled = output<void>();
   }
   ```

2. В `ChatItemComponent`:
   - Иконка удаления
   - Показ модалки при клике

3. Добавить метод `deleteChat()` в `ChatService`
4. Навигация на `/chat` после удаления активного чата

**Изучаемые концепции:**

- Новый синтаксис: `input()`, `output()`
- Модальные окна и overlay
- Остановка всплытия событий: `$event.stopPropagation()`

---

### Этап 15: Состояния загрузки

**Время:** ~30 мин

**Цель:** Добавить loading, empty, error состояния.

**Задачи:**

1. В `ChatService` добавить signal `isLoading`:
   ```typescript
   isLoading = signal(false);
   
   async loadChats() {
     this.isLoading.set(true);
     await delay(500); // имитация загрузки
     this.isLoading.set(false);
   }
   ```

2. В `ChatListComponent`:
   - Skeleton loader при загрузке
   - "Создайте первый чат, чтобы начать" при пустом списке

3. В `MessageListComponent`:
   - Skeleton при загрузке сообщений
   - Приветственное сообщение для пустого чата

4. В `MessageInputComponent`:
   - Disabled при отправке

**Изучаемые концепции:**

- `@if` / `@else` для состояний
- Skeleton loaders (CSS анимация)
- Управление состоянием загрузки

---

### Этап 16: Стилизация (desktop)

**Время:** ~60 мин

**Цель:** Стилизовать интерфейс для desktop.

**Задачи:**

1. Глобальные стили в `styles.css`:
   - Reset и базовые стили
   - Типографика
   - CSS переменные для цветов

2. `ChatList`:
   - Sidebar с тенью
   - Кнопка "+ Новый чат" (primary стиль)
   - Hover эффекты на элементах

3. `MessageList`:
   - Сообщения user: справа, синий фон, белый текст
   - Сообщения assistant: слева, серый фон
   - Скругленные углы, отступы

4. `MessageInput`:
   - Textarea с рамкой
   - Кнопка отправки (иконка)
   - Focus стили

**Изучаемые концепции:**

- Component styles (scoped)
- CSS переменные в компонентах
- Flexbox для позиционирования
- Псевдоклассы: `:hover`, `:focus`, `:disabled`

---

### Этап 17: Адаптивность (mobile/tablet)

**Время:** ~60 мин

**Цель:** Сделать интерфейс адаптивным.

**Задачи:**

1. Определить breakpoints:
   - Mobile: < 768px
   - Tablet: 768px - 1024px
   - Desktop: > 1024px

2. Mobile layout:
   - Sidebar как full-screen drawer
   - Hamburger меню в Header
   - Signal `isSidebarOpen` в `ChatLayoutComponent`

3. Tablet layout:
   - Sidebar collapsible (overlay)
   - Toggle кнопка

4. В `ChatLayoutComponent`:
   ```typescript
   isSidebarOpen = signal(false);
   isMobile = signal(window.innerWidth < 768);
   
   @HostListener('window:resize')
   onResize() {
     this.isMobile.set(window.innerWidth < 768);
   }
   
   toggleSidebar() {
     this.isSidebarOpen.update(v => !v);
   }
   ```

**Изучаемые концепции:**

- CSS Media queries
- `@HostListener` для window events
- Условные классы для layout
- Mobile-first подход

---

## Дополнительные этапы (опционально)

### Этап 18: Dropdown меню в ChatHeader

- Компонент `DropdownMenuComponent`
- Меню с пунктами: "Переименовать", "Удалить"
- Закрытие по клику вне меню

### Этап 19: Typing indicator

- Анимация "●●●" при ожидании ответа
- Имитация streaming ответа (посимвольный вывод)

### Этап 20: Markdown в сообщениях

- Установка библиотеки `ngx-markdown` или `marked`
- Базовое форматирование: **bold**, *italic*, списки, код

### Этап 21: Toast уведомления

- Сервис `ToastService` с Signals
- Компонент `ToastContainer`
- Уведомления при ошибках и успешных действиях

---

## Ключевые отличия Angular 19 от React

| Концепция | Angular 19 | React |
|-----------|------------|-------|
| Состояние | Signals (`signal()`, `computed()`) | `useState`, `useMemo` |
| Эффекты | `effect()` | `useEffect` |
| DI | `inject()` функция | Context или props |
| Шаблоны | HTML с `@if`, `@for` | JSX |
| Формы | Reactive Forms | Controlled components |
| Стили | Scoped CSS по умолчанию | CSS Modules / styled |
| Роутинг | Декларативный Router | React Router |

## Полезные команды Angular CLI

```bash
# Создать компонент
ng generate component features/chat/chat-list --standalone

# Создать сервис
ng generate service core/services/chat

# Запустить dev server
ng serve

# Сборка
ng build
```

## Примечания

- Все данные хранятся в памяти через Signals
- При перезагрузке страницы данные сбросятся к начальным
- Фокус на изучении Angular, не на интеграциях
- Каждый этап можно тестировать независимо
