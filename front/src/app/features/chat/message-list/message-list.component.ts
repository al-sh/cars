import {
  AfterViewChecked,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  ViewChild,
} from '@angular/core';

import { ChatService } from '../../../core/services/chat.service';
import { MessageItemComponent } from '../message-item/message-item.component';

/**
 * MessageListComponent — отображает список сообщений текущего чата.
 *
 * Ключевые концепции:
 *
 * 1. @ViewChild — позволяет получить ссылку на DOM-элемент из шаблона.
 *    В React аналог — useRef:
 *      const scrollRef = useRef<HTMLDivElement>(null);
 *
 * 2. AfterViewChecked — lifecycle hook, вызывается ПОСЛЕ каждого обновления view.
 *    Это нужно для автоскролла: после добавления нового сообщения DOM обновляется,
 *    и мы прокручиваем контейнер вниз.
 *
 *    В React аналог — useEffect с зависимостью от массива сообщений:
 *      useEffect(() => { scrollToBottom(); }, [messages]);
 *
 *    Разница: в Angular lifecycle hooks — это методы интерфейса,
 *    в React — это хуки, вызываемые внутри функционального компонента.
 */
@Component({
  selector: 'app-message-list',
  standalone: true,
  imports: [MessageItemComponent],
  templateUrl: './message-list.component.html',
  styleUrl: './message-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MessageListComponent implements AfterViewChecked {
  private readonly chatService = inject(ChatService);

  /**
   * @ViewChild('scrollContainer') — находит элемент с #scrollContainer в шаблоне.
   *
   * ElementRef — обёртка Angular над нативным DOM-элементом.
   * Через .nativeElement получаем обычный HTMLElement.
   *
   * В React: const ref = useRef<HTMLDivElement>(null); → ref.current
   * В Angular: @ViewChild('name') ref!: ElementRef; → ref.nativeElement
   */
  @ViewChild('scrollContainer') scrollContainer!: ElementRef<HTMLDivElement>;

  get messages() {
    return this.chatService.currentMessages;
  }

  /**
   * ngAfterViewChecked — вызывается после каждой проверки view компонента.
   *
   * Мы используем его для автоскролла: после добавления сообщения
   * Angular обновит DOM (добавит новый MessageItem), и сразу после этого
   * вызовется ngAfterViewChecked — мы прокрутим список вниз.
   *
   * Важно: этот hook вызывается часто (после каждого change detection цикла),
   * но scrollToBottom — дешёвая операция (просто установка scrollTop).
   */
  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  /**
   * Прокручивает контейнер сообщений к последнему сообщению.
   *
   * scrollTop = scrollHeight перемещает скролл в самый низ.
   * Это стандартный DOM API, не специфичный для Angular.
   */
  private scrollToBottom(): void {
    const el = this.scrollContainer?.nativeElement;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }
}
