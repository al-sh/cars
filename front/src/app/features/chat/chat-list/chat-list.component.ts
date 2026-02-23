import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { toSignal } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, map } from 'rxjs';

import { ChatService, SAVED_CHAT_ID } from '../../../core/services/chat.service';
import { ChatItemComponent } from './chat-item/chat-item.component';

/**
 * ChatListComponent — боковая панель со списком чатов.
 *
 * На этапе 12 добавлена фильтрация чатов по названию:
 * - FormControl для поля поиска
 * - RxJS операторы (debounceTime, distinctUntilChanged) для оптимизации
 * - toSignal() для конвертации Observable → Signal
 * - computed() для реактивной фильтрации списка
 */
@Component({
  selector: 'app-chat-list',
  standalone: true,
  imports: [ChatItemComponent, ReactiveFormsModule],
  templateUrl: './chat-list.component.html',
  styleUrl: './chat-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatListComponent {
  private readonly chatService = inject(ChatService);
  private readonly router = inject(Router);

  readonly SAVED_CHAT_ID = SAVED_CHAT_ID;

  /**
   * FormControl для поля поиска.
   *
   * FormControl — это объект из Reactive Forms, который отслеживает
   * значение и состояние одного поля формы.
   *
   * Почему FormControl, а не обычный [(ngModel)]?
   * - FormControl предоставляет valueChanges — Observable, на который
   *   можно навесить RxJS операторы (debounce, distinct и т.д.)
   * - В React аналог — это controlled input + useEffect с debounce
   *
   * Для работы FormControl в шаблоне нужен ReactiveFormsModule в imports.
   */
  readonly searchControl = new FormControl('');

  /**
   * Signal с обработанным поисковым запросом (с debounce).
   *
   * toSignal() — мост между RxJS Observable и Angular Signals.
   * Конвертирует Observable в Signal, чтобы использовать в шаблонах
   * и в computed().
   *
   * Цепочка обработки:
   * 1. searchControl.valueChanges — Observable, эмитит при каждом изменении
   * 2. debounceTime(300) — ждёт 300мс тишины перед эмитом.
   *    Без debounce при вводе "BMW" произойдёт 3 поиска: "B", "BM", "BMW".
   *    С debounce — только 1 поиск: "BMW" (через 300мс после последней буквы).
   * 3. distinctUntilChanged() — не эмитит, если значение не изменилось.
   *    Пример: пользователь набрал "a", стёр, снова набрал "a" — поиск не повторится.
   * 4. map() — очищает значение (trim + lowercase) для корректного сравнения.
   *
   * { initialValue: '' } — начальное значение signal до первого эмита Observable.
   *
   * В React аналог:
   *   const [search, setSearch] = useState('');
   *   const debouncedSearch = useDebounce(search, 300);
   */
  private readonly searchQuery = toSignal(
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      map(value => value?.trim().toLowerCase() ?? ''),
    ),
    { initialValue: '' },
  );

  /**
   * Отфильтрованный список чатов — computed signal.
   *
   * computed() автоматически пересчитывается, когда любая из зависимостей
   * (searchQuery или chatService.chats) изменится.
   *
   * Логика:
   * - Если запрос пустой — возвращаем все чаты
   * - Иначе — фильтруем по совпадению title (регистронезависимо)
   *
   * В React аналог:
   *   const filteredChats = useMemo(() =>
   *     query ? chats.filter(...) : chats,
   *     [query, chats]
   *   );
   */
  readonly filteredChats = computed(() => {
    const query = this.searchQuery();
    const chats = this.chatService.chats();
    if (!query) return chats;
    return chats.filter(chat =>
      chat.title?.toLowerCase().includes(query),
    );
  });

  /**
   * Геттер для ID текущего выбранного чата.
   * Используется для подсветки активного элемента в списке.
   */
  get currentChatId() {
    return this.chatService.currentChatId;
  }

  /**
   * Создание нового чата и навигация к нему.
   *
   * createChat() делает POST /api/v1/chats и возвращает Observable<Chat>.
   * После получения ответа переходим к новому чату.
   */
  onCreateNewChat(): void {
    this.chatService.createChat().subscribe(newChat => {
      this.router.navigate(['/chat', newChat.id]);
    });
  }

  /**
   * Обработчик выбора чата.
   *
   * Вызывается когда ChatItemComponent эмитит событие selected.
   * Мы передаём ID в сервис, который обновляет currentChatId.
   * Благодаря Signals все подписанные компоненты автоматически обновятся.
   *
   * @param chatId - ID выбранного чата
   */
  onChatSelected(chatId: string): void {
    this.router.navigate(['/chat', chatId]);
  }
}
