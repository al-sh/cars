import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { Chat } from '../../../../core/models/chat.model';

@Component({
  selector: 'app-chat-item',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './chat-item.component.html',
  styleUrl: './chat-item.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChatItemComponent {
  // Получаем данные, это как props в react
  @Input({ required: true }) chat!: Chat;

  // @Output() - это декоратор для создания события, которое компонент
  // может "выбросить" наружу, чтобы родительский компонент мог на него подписаться.
  // EventEmitter<string> означает, что событие будет передавать строку (ID чата).
  // В React это было бы callback-функция, переданная через props.
  @Output() selected = new EventEmitter<string>();

  // Метод для обработки клика по элементу чата
  // Когда пользователь кликает на чат, мы "эмитим" (выбрасываем) событие
  // с ID этого чата. Родительский компонент (ChatListComponent) подпишется
  // на это событие и обработает выбор чата.
  onSelect(): void {
    this.selected.emit(this.chat.id);
  }
}
