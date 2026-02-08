import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
} from '@angular/core';

import { Message } from '../../../core/models/message.model';
import {
  ChatService,
  SAVED_CHAT_ID,
} from '../../../core/services/chat.service';

/**
 * MessageItemComponent — отображает одно сообщение в чате.
 *
 * Использует input() — новый синтаксис Angular 17+ для входных данных.
 * Это замена @Input() декоратору:
 *
 * Старый способ:
 *   @Input({ required: true }) message!: Message;
 *
 * Новый способ:
 *   message = input.required<Message>();
 *
 * Преимущества input():
 * - Возвращает Signal — автоматическая интеграция с OnPush
 * - Типобезопасность без ! (non-null assertion)
 * - required встроен в API
 *
 * В React аналог — просто props:
 *   function MessageItem({ message }: { message: Message }) { ... }
 */
@Component({
  selector: 'app-message-item',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './message-item.component.html',
  styleUrl: './message-item.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MessageItemComponent {
  private readonly chatService = inject(ChatService);

  /**
   * input.required() — входные данные, которые обязательны.
   * Если родитель не передаст message — Angular выбросит ошибку при компиляции.
   *
   * message() возвращает текущее значение (это Signal).
   * В шаблоне: message().content, message().role и т.д.
   */
  readonly message = input.required<Message>();

  get isFav() {
    return this.chatService.currentChatId() === SAVED_CHAT_ID;
  }

  onFavClick() {
    this.chatService.saveMessage(this.message());
  }
}
