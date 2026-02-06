import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ChatService } from '../../../core/services/chat.service';

/**
 * MessageInputComponent — компонент для ввода и отправки сообщений.
 *
 * Использует Reactive Forms — декларативный подход Angular к формам.
 * В React аналог — это controlled components (useState + onChange),
 * но в Angular форма описывается через FormGroup/FormControl объекты.
 *
 * Reactive Forms дают:
 * 1. Валидацию из коробки (required, maxLength и т.д.)
 * 2. Реактивный доступ к состоянию формы (valid, dirty, touched)
 * 3. Синхронный доступ к значениям без лишних рендеров
 *
 * Для работы Reactive Forms нужен `ReactiveFormsModule` в imports компонента.
 */
@Component({
  selector: 'app-message-input',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './message-input.component.html',
  styleUrl: './message-input.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MessageInputComponent {
  private readonly chatService = inject(ChatService);

  /**
   * FormBuilder — утилита для удобного создания FormGroup и FormControl.
   *
   * inject(FormBuilder) — это современный Angular DI (Dependency Injection).
   * В React аналога нет — там формы управляются через useState/useReducer.
   */
  private readonly fb = inject(FormBuilder);

  /**
   * messageForm — FormGroup, описывающая структуру формы.
   *
   * fb.group() создаёт FormGroup — контейнер для группы FormControl'ов.
   * Каждый элемент массива: [начальноеЗначение, [валидаторы]].
   *
   * Validators.required — поле не может быть пустым.
   * Validators.maxLength(4000) — максимальная длина 4000 символов.
   *
   * В React это было бы:
   *   const [content, setContent] = useState('');
   *   const isValid = content.trim().length > 0 && content.length <= 4000;
   *
   * Но Angular Reactive Forms дают это из коробки + много дополнительных возможностей.
   */
  messageForm = this.fb.group({
    content: ['', [Validators.required, Validators.maxLength(4000)]],
  });

  /**
   * Максимальная длина сообщения.
   * Вынесено в константу, чтобы использовать и в валидаторе, и в счётчике символов.
   */
  readonly maxLength = 4000;

  /**
   * Порог, после которого показываем счётчик символов.
   * Показываем заранее (при 3500+), чтобы пользователь видел приближение к лимиту.
   */
  readonly charCountThreshold = 3500;

  /**
   * Signal для блокировки формы во время "отправки".
   *
   * В реальном приложении отправка — асинхронная операция (HTTP запрос).
   * Пока ответ не пришёл, форму нужно заблокировать, чтобы предотвратить
   * повторную отправку. Сейчас это мок — блокируем на 300мс для демонстрации.
   *
   * signal(false) — начальное значение: не отправляется.
   * В React аналог: const [isSending, setIsSending] = useState(false);
   */
  readonly isSending = signal(false);

  /**
   * Геттер для текущей длины текста.
   *
   * this.messageForm.get('content') — получает FormControl по имени.
   * .value — текущее значение контрола.
   *
   * В React аналог: content.length
   */
  get contentLength(): number {
    return (this.messageForm.get('content')?.value ?? '').length;
  }

  /**
   * Обработчик нажатия Enter в textarea.
   *
   * Enter — отправить сообщение.
   * Shift+Enter — перенос строки (стандартное поведение textarea).
   *
   * preventDefault() предотвращает вставку символа новой строки при отправке.
   * Без него после отправки в пустое поле добавлялся бы лишний перенос строки.
   *
   * В React аналог:
   *   const handleKeyDown = (e: React.KeyboardEvent) => {
   *     if (e.key === 'Enter' && !e.shiftKey) {
   *       e.preventDefault();
   *       handleSubmit();
   *     }
   *   };
   */
  onKeydown(event: Event): void {
    const keyboardEvent = event as KeyboardEvent;
    if (!keyboardEvent.shiftKey) {
      keyboardEvent.preventDefault();
      this.onSubmit();
    }
    // Если Shift зажат — ничего не делаем, textarea вставит перенос строки
  }

  /**
   * Авто-resize textarea при вводе текста.
   *
   * Это JS fallback для браузеров без поддержки CSS `field-sizing: content`.
   * Сбрасываем height в 'auto', затем устанавливаем scrollHeight (реальная высота контента).
   * Math.min ограничивает максимальную высоту (120px = ~5 строк).
   *
   * (input) в шаблоне вызывает этот метод при каждом изменении текста.
   * В React аналог: onChange с ref на textarea.
   */
  onInput(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px';
    // Показываем скроллбар только когда контент превышает max-height
    textarea.style.overflowY = textarea.scrollHeight > 120 ? 'auto' : 'hidden';
  }

  /**
   * Обработчик отправки формы.
   *
   * (ngSubmit) в шаблоне привязан к этому методу.
   * В React аналог — onSubmit={handleSubmit} на <form>.
   *
   * Проверяем messageForm.invalid — если форма невалидна, ничего не делаем.
   * getRawValue() возвращает текущие значения всех контролов формы.
   */
  onSubmit(): void {
    if (this.messageForm.invalid || this.isSending()) return;

    const { content } = this.messageForm.getRawValue();
    // content может быть null (из-за типизации FormBuilder), проверяем
    if (!content?.trim()) return;

    const chatId = this.chatService.currentChatId();
    if (!chatId) return;

    // Блокируем форму на время отправки
    this.isSending.set(true);

    this.chatService.sendMessage(chatId, content);

    // Сбрасываем форму после отправки.
    // reset() очищает значения и сбрасывает состояния (dirty, touched).
    // В React это было бы setContent('').
    this.messageForm.reset();

    // Снимаем блокировку. В реальном приложении это будет в callback/subscribe
    // после успешного HTTP-ответа. Сейчас — мок с небольшой задержкой.
    // TODO: Убрать setTimeout при подключении реального API
    setTimeout(() => this.isSending.set(false), 300);
  }
}
