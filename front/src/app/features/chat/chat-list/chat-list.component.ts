import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MOCK_CHATS } from '../../../core/mocks/mock-data';
import { Chat } from '../../../core/models/chat.model';
import { ChatItemComponent } from './chat-item/chat-item.component';

@Component({
  selector: 'app-chat-list',
  standalone: true,
  imports: [ChatItemComponent],
  templateUrl: './chat-list.component.html',
  styleUrl: './chat-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChatListComponent {
  // Импортируем мок-данные напрямую.
  // На этапе 4 мы используем статичные данные без сервиса.
  // Позже (этап 6) мы заменим это на данные из ChatService.
  // 
  // readonly означает, что это свойство нельзя изменить после инициализации.
  // Это хорошая практика для данных, которые не должны изменяться напрямую.
  readonly chats: Chat[] = MOCK_CHATS;

  // Метод для обработки клика на кнопку "+ Новый чат".
  // Пока это заглушка - на этапе 13 мы реализуем реальное создание чата.
  // Но уже сейчас мы показываем структуру: метод существует и готов к расширению.
  onCreateNewChat(): void {
    console.log('Создание нового чата (функционал будет добавлен на этапе 13)');
    // TODO: Реализовать создание нового чата на этапе 13
  }

  // Метод для обработки выбора чата из списка.
  // ChatItemComponent "выбросит" событие selected с ID чата,
  // и мы его здесь обработаем. Пока просто логируем.
  // На этапе 6 мы подключим это к ChatService.
  onChatSelected(chatId: string): void {
    console.log('Выбран чат:', chatId);
    // TODO: Подключить к ChatService.selectChat() на этапе 6
  }
}
