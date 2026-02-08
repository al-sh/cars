import { Injectable, computed, signal } from '@angular/core';

import { MOCK_CHATS, MOCK_MESSAGES } from '../mocks/mock-data';
import { Chat } from '../models/chat.model';
import { Message } from '../models/message.model';

export const SAVED_CHAT_ID = 'saved';

/**
 * ChatService — центральный сервис для управления состоянием чатов и сообщений.
 */
@Injectable({ providedIn: 'root' })
export class ChatService {
  // ============================================================================
  // ПРИВАТНЫЕ SIGNALS (источники состояния)
  // ============================================================================

  /**
   * signal() — создаёт реактивное значение, похоже на useState в React.
   *
   * Отличия от useState:
   * - Читаем значение через вызов функции: chatsSignal() вместо chats
   * - Устанавливаем через .set() вместо setChats()
   * - Обновляем через .update(prev => newValue) — как functional update в React
   *
   * Почему приватный? Чтобы компоненты не могли напрямую менять состояние,
   * а использовали методы сервиса (инкапсуляция).
   */
  private chatsSignal = signal<Chat[]>([
    {
      id: SAVED_CHAT_ID,
      title: 'Saved Messages',
      user_id: 'mock-user',
      message_count: 0,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    },
    ...MOCK_CHATS,
  ]);

  /**
   * Храним сообщения в Map: chatId → массив сообщений.
   * Map эффективнее объекта для динамических ключей и имеет удобные методы.
   *
   * Инициализируем из мок-данных, конвертируя Record в Map.
   */
  private messagesSignal = signal<Map<string, Message[]>>(
    new Map(Object.entries(MOCK_MESSAGES)),
  );

  /**
   * ID текущего выбранного чата. null = чат не выбран.
   * Этот signal публичный, но readonly — компоненты могут читать,
   * но менять только через selectChat().
   */
  private currentChatIdSignal = signal<string | null>(null);

  // ============================================================================
  // ПУБЛИЧНЫЕ READONLY SIGNALS (для чтения компонентами)
  // ============================================================================

  /**
   * asReadonly() — создаёт версию signal, которую можно только читать.
   * Компоненты смогут вызвать chats() для получения значения,
   * но не смогут вызвать chats.set() — такого метода просто нет.
   *
   * Это паттерн "readonly public, writable private" — защищает состояние
   * от случайных изменений извне сервиса.
   */
  readonly chats = this.chatsSignal.asReadonly();

  /**
   * Публичный доступ к ID текущего чата.
   */
  readonly currentChatId = this.currentChatIdSignal.asReadonly();

  // ============================================================================
  // COMPUTED SIGNALS (производные данные)
  // ============================================================================

  /**
   * computed() — создаёт signal, который автоматически пересчитывается
   * при изменении зависимостей. Похоже на useMemo в React, но:
   *
   * 1. Не нужно указывать массив зависимостей — Angular отслеживает их сам
   * 2. Вычисление ленивое — происходит только при чтении
   * 3. Результат кэшируется до изменения зависимостей
   *
   * Здесь мы находим объект текущего чата по его ID.
   * Если ID изменится или список чатов обновится — computed пересчитается.
   */
  readonly currentChat = computed(() => {
    const chatId = this.currentChatId();
    if (!chatId) return null;
    return this.chats().find((chat) => chat.id === chatId) ?? null;
  });

  /**
   * Сообщения текущего чата.
   * Зависит от currentChatId и messagesSignal — при изменении любого
   * из них автоматически пересчитается.
   */
  readonly currentMessages = computed(() => {
    const chatId = this.currentChatId();
    if (!chatId) return [];
    return this.messagesSignal().get(chatId) ?? [];
  });

  // ============================================================================
  // МЕТОДЫ (действия над состоянием)
  // ============================================================================

  /**
   * Выбирает чат по ID.
   *
   * Почему метод, а не прямой доступ к signal?
   * 1. Инкапсуляция — логика выбора в одном месте
   * 2. Можно добавить побочные эффекты (загрузка сообщений, аналитика)
   * 3. Легче отлаживать — все изменения проходят через методы
   *
   * @param chatId - ID чата для выбора
   */
  selectChat(chatId: string): void {
    this.currentChatIdSignal.set(chatId);
  }

  /**
   * Снимает выбор с текущего чата.
   */
  clearSelection(): void {
    this.currentChatIdSignal.set(null);
  }

  /**
   * Получает сообщения чата по ID.
   * Это синхронный метод для получения данных без подписки на обновления.
   *
   * @param chatId - ID чата
   * @returns Массив сообщений или пустой массив
   */
  getMessages(chatId: string): Message[] {
    return this.messagesSignal().get(chatId) ?? [];
  }

  /**
   * Отправляет сообщение в чат.
   *
   * Пока это мок-реализация — добавляем сообщение пользователя локально.
   * В будущем здесь будет HTTP-запрос к backend и SSE для получения ответа.
   *
   * Обрати внимание на update() — это как functional update в React:
   * setMessages(prev => [...prev, newMessage])
   *
   * Мы не мутируем Map напрямую, а создаём новый экземпляр — это важно
   * для корректной работы change detection.
   *
   * @param chatId - ID чата
   * @param content - Текст сообщения
   */
  sendMessage(chatId: string, content: string): void {
    const newMessage: Message = {
      id: crypto.randomUUID(),
      chat_id: chatId,
      role: 'user',
      content: content.trim(),
      created_at: new Date().toISOString(),
    };

    // update() получает текущее значение и должен вернуть новое
    this.messagesSignal.update((messagesMap) => {
      // Создаём новый Map чтобы Angular увидел изменение
      const newMap = new Map(messagesMap);
      const chatMessages = newMap.get(chatId) ?? [];
      // Добавляем сообщение в конец массива (иммутабельно)
      newMap.set(chatId, [...chatMessages, newMessage]);
      return newMap;
    });

    // Обновляем счётчик сообщений в чате
    this.chatsSignal.update((chats) =>
      chats.map((chat) =>
        chat.id === chatId
          ? {
              ...chat,
              message_count: chat.message_count + 1,
              updated_at: new Date().toISOString(),
            }
          : chat,
      ),
    );

    // TODO: Этап 17 — добавить имитацию ответа ассистента
  }

  saveMessage(message: Message): void {
    const sourceChatTitle = this.chats().find(
      (chat) => chat.id === message.chat_id,
    )?.title;
    this.messagesSignal.update((messagesMap) => {
      const newMap = new Map(messagesMap);
      const savedMessages = newMap.get(SAVED_CHAT_ID) ?? [];
      const forwardedMessage: Message = {
        ...message,
        id: crypto.randomUUID(),
        chat_id: SAVED_CHAT_ID,
        forwardedFrom: sourceChatTitle ?? 'Unknown Chat',
        created_at: new Date().toISOString(),
      };
      newMap.set(SAVED_CHAT_ID, [...savedMessages, forwardedMessage]);
      return newMap;
    });
  }

  /**
   * Создаёт новый чат.
   *
   * Возвращает созданный чат, чтобы можно было сразу перейти к нему.
   *
   * @returns Новый объект Chat
   */
  createChat(): Chat {
    const newChat: Chat = {
      id: crypto.randomUUID(),
      user_id: 'mock-user', // TODO: Заменить на реального пользователя
      title: null, // Название появится после первого сообщения
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
      message_count: 0,
    };

    // Добавляем в начало списка (новые чаты сверху)
    this.chatsSignal.update((chats) => [newChat, ...chats]);

    // Добавляем приветственное сообщение от ассистента
    const welcomeMessage: Message = {
      id: crypto.randomUUID(),
      chat_id: newChat.id,
      role: 'assistant',
      content:
        'Здравствуйте! Я ИИ-помощник по подбору автомобилей.\n\n' +
        'Расскажите, какой автомобиль вы ищете, и я помогу выбрать ' +
        'оптимальный вариант. Можете указать:\n' +
        '• Бюджет\n' +
        '• Тип кузова (седан, кроссовер, хэтчбек...)\n' +
        '• Для каких целей нужен автомобиль\n\n' +
        'Или просто опишите свои пожелания своими словами!',
      created_at: new Date().toISOString(),
    };

    this.messagesSignal.update((messagesMap) => {
      const newMap = new Map(messagesMap);
      newMap.set(newChat.id, [welcomeMessage]);
      return newMap;
    });

    return newChat;
  }

  /**
   * Обновляет данные чата (например, название).
   *
   * Partial<Chat> означает, что можно передать только те поля,
   * которые нужно обновить — остальные останутся прежними.
   *
   * @param chatId - ID чата для обновления
   * @param updates - Объект с полями для обновления
   */
  updateChat(chatId: string, updates: Partial<Chat>): void {
    if (chatId === SAVED_CHAT_ID) {
      console.warn('Нельзя редактировать чат Saved Messages');
      return;
    }

    this.chatsSignal.update((chats) =>
      chats.map((chat) =>
        chat.id === chatId
          ? { ...chat, ...updates, updated_at: new Date().toISOString() }
          : chat,
      ),
    );
  }

  /**
   * Удаляет чат и все его сообщения.
   *
   * @param chatId - ID чата для удаления
   */
  deleteChat(chatId: string): void {
    if (chatId === SAVED_CHAT_ID) {
      console.warn('Нельзя удалить чат Saved Messages');
      return;
    }

    // Удаляем чат из списка
    this.chatsSignal.update((chats) =>
      chats.filter((chat) => chat.id !== chatId),
    );

    // Удаляем сообщения чата
    this.messagesSignal.update((messagesMap) => {
      const newMap = new Map(messagesMap);
      newMap.delete(chatId);
      return newMap;
    });

    // Если удалили текущий чат — снимаем выбор
    if (this.currentChatId() === chatId) {
      this.clearSelection();
    }
  }
}
