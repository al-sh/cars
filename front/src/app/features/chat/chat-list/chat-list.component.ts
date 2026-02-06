import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { ChatService } from '../../../core/services/chat.service';
import { ChatItemComponent } from './chat-item/chat-item.component';

/**
 * ChatListComponent — боковая панель со списком чатов.
 *
 * На этапе 6 мы подключили компонент к ChatService:
 * - Данные приходят из сервиса, а не из прямого импорта моков
 * - Выбор чата обновляет состояние в сервисе
 * - Компонент реагирует на изменения через Signals
 */
@Component({
  selector: 'app-chat-list',
  standalone: true,
  imports: [ChatItemComponent],
  templateUrl: './chat-list.component.html',
  styleUrl: './chat-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatListComponent {
  /**
   * inject() — современный способ внедрения зависимостей (DI) в Angular.
   *
   * Раньше зависимости передавались через конструктор:
   *   constructor(private chatService: ChatService) {}
   *
   * Теперь можно использовать inject() напрямую в свойстве класса.
   * Преимущества:
   * - Меньше boilerplate кода
   * - Работает не только в классах, но и в функциях
   * - Проще для понимания — зависимость объявлена там, где используется
   *
   * private — сервис используется только внутри компонента.
   * readonly — защита от случайного переопределения.
   */
  private readonly chatService = inject(ChatService);
  private readonly router = inject(Router);

  /**
   * Геттер для списка чатов из сервиса.
   *
   * chatService.chats — это Signal (readonly).
   * В шаблоне мы вызываем его как функцию: chats()
   *
   * Почему геттер, а не просто свойство?
   * Чтобы в шаблоне писать chats() вместо chatService.chats().
   * Это инкапсуляция — шаблон не знает про сервис напрямую.
   */
  get chats() {
    return this.chatService.chats;
  }

  /**
   * Геттер для ID текущего выбранного чата.
   * Используется для подсветки активного элемента в списке.
   */
  get currentChatId() {
    return this.chatService.currentChatId;
  }

  /**
   * Создание нового чата.
   * Пока заглушка — полная реализация на этапе 13.
   */
  onCreateNewChat(): void {
    // TODO: Этап 13 — создание чата и навигация к нему
    console.log('Создание нового чата (этап 13)');
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
