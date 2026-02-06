import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { Chat } from '../../../../core/models/chat.model';

/**
 * ChatItemComponent — элемент списка чатов.
 *
 * Это "презентационный" (dumb) компонент:
 * - Не знает про сервисы и бизнес-логику
 * - Получает все данные через @Input
 * - Сообщает о действиях через @Output
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
   * Данные чата. required: true означает, что родитель обязан передать это свойство.
   * Восклицательный знак (!) говорит TypeScript: "я знаю, что значение будет".
   */
  @Input({ required: true }) chat!: Chat;

  /**
   * Флаг активного (выбранного) чата.
   * Родительский компонент вычисляет это значение, сравнивая ID.
   *
   * Почему передаём готовый boolean, а не ID для сравнения?
   * - Компонент не должен знать про currentChatId
   * - Проще тестировать: передал true — должен быть класс active
   * - Single Responsibility: логика выбора в родителе, отображение здесь
   */
  @Input() isActive = false;

  /**
   * Событие выбора чата.
   * EventEmitter — это Observable, который можно слушать в шаблоне через (selected)="...".
   */
  @Output() selected = new EventEmitter<string>();

  /**
   * Обработчик клика — эмитит ID чата наверх.
   */
  onSelect(): void {
    this.selected.emit(this.chat.id);
  }
}
