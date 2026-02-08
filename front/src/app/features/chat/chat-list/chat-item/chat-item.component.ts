import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';

import { Chat } from '../../../../core/models/chat.model';

/**
 * ChatItemComponent — элемент списка чатов.
 *
 * Это презентационный (dumb) компонент:
 * - Не знает про сервисы и бизнес-логику
 * - Получает все данные через input()
 * - Сообщает о действиях через output()
 *
 * Такой подход упрощает тестирование и переиспользование.
 */
@Component({
  selector: 'app-chat-item',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './chat-item.component.html',
  styleUrl: './chat-item.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatItemComponent {
  /**
   * input.required() — входные данные, которые обязательны.
   * Возвращает Signal — типобезопасно и без ! (non-null assertion).
   */
  readonly chat = input.required<Chat>();

  /**
   * Флаг активного (выбранного) чата.
   * Родитель вычисляет, сравнивая ID — компонент только отображает.
   */
  readonly isActive = input(false);

  /**
   * Флаг чата "Избранное".
   * Передаётся родителем, чтобы компонент не зависел от ChatService.
   */
  readonly isFavChat = input(false);

  /**
   * output() — новый API Angular 17+ для событий (замена @Output + EventEmitter).
   * В шаблоне родителя используется так же: (selected)="onChatSelected($event)"
   */
  readonly selected = output<string>();

  onSelect(): void {
    this.selected.emit(this.chat().id);
  }
}
