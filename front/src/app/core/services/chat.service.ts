import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, of, tap } from 'rxjs';

import { Chat } from '../models/chat.model';
import { Message } from '../models/message.model';

export const SAVED_CHAT_ID = 'saved';

const API_BASE_URL = 'http://localhost:8080/api/v1';

type PagedResponse<T> = {
  items: T[];
  total: number;
  page: number;
  perPage: number;
};

/**
 * Локальный чат "Сохранённые сообщения" — существует только на клиенте,
 * не имеет аналога в бэкенде.
 */
const SAVED_CHAT: Chat = {
  id: SAVED_CHAT_ID,
  title: 'Saved Messages',
  userId: '',
  messageCount: 0,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

/**
 * ChatService — центральный сервис для управления состоянием чатов и сообщений.
 *
 * Чаты загружаются с бэкенда через HTTP.
 * Сообщения пока хранятся локально (бэкенд-эндпоинты сообщений — этап 11).
 */
@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);

  // ============================================================================
  // ПРИВАТНЫЕ SIGNALS (источники состояния)
  // ============================================================================

  private chatsSignal = signal<Chat[]>([SAVED_CHAT]);
  private messagesSignal = signal<Map<string, Message[]>>(new Map());
  private currentChatIdSignal = signal<string | null>(null);

  // ============================================================================
  // ПУБЛИЧНЫЕ READONLY SIGNALS (для чтения компонентами)
  // ============================================================================

  readonly chats = this.chatsSignal.asReadonly();
  readonly currentChatId = this.currentChatIdSignal.asReadonly();

  // ============================================================================
  // COMPUTED SIGNALS (производные данные)
  // ============================================================================

  readonly currentChat = computed(() => {
    const chatId = this.currentChatId();
    if (!chatId) return null;
    return this.chats().find((chat) => chat.id === chatId) ?? null;
  });

  readonly currentMessages = computed(() => {
    const chatId = this.currentChatId();
    if (!chatId) return [];
    return this.messagesSignal().get(chatId) ?? [];
  });

  // ============================================================================
  // МЕТОДЫ (действия над состоянием)
  // ============================================================================

  /**
   * Загружает список чатов пользователя с бэкенда.
   * Вызывается при инициализации чат-страницы.
   */
  loadChats(): void {
    this.http
      .get<PagedResponse<Chat>>(`${API_BASE_URL}/chats?perPage=100`)
      .pipe(
        catchError(err => {
          console.error('Ошибка загрузки чатов:', err);
          return of({ items: [] as Chat[], total: 0, page: 1, perPage: 100 });
        }),
      )
      .subscribe(response => {
        this.chatsSignal.set([SAVED_CHAT, ...response.items]);
      });
  }

  selectChat(chatId: string): void {
    this.currentChatIdSignal.set(chatId);
  }

  clearSelection(): void {
    this.currentChatIdSignal.set(null);
  }

  getMessages(chatId: string): Message[] {
    return this.messagesSignal().get(chatId) ?? [];
  }

  /**
   * Отправляет сообщение пользователя в чат (локально).
   * TODO: Этап 11 — заменить на HTTP-запрос к бэкенду.
   */
  sendMessage(chatId: string, content: string): void {
    const newMessage: Message = {
      id: crypto.randomUUID(),
      chatId: chatId,
      role: 'user',
      content: content.trim(),
      createdAt: new Date().toISOString(),
    };

    this.messagesSignal.update((messagesMap) => {
      const newMap = new Map(messagesMap);
      const chatMessages = newMap.get(chatId) ?? [];
      newMap.set(chatId, [...chatMessages, newMessage]);
      return newMap;
    });

    this.chatsSignal.update((chats) =>
      chats.map((chat) =>
        chat.id === chatId
          ? {
              ...chat,
              messageCount: chat.messageCount + 1,
              updatedAt: new Date().toISOString(),
            }
          : chat,
      ),
    );
  }

  saveMessage(message: Message): void {
    const sourceChatTitle = this.chats().find(
      (chat) => chat.id === message.chatId,
    )?.title;

    this.messagesSignal.update((messagesMap) => {
      const newMap = new Map(messagesMap);
      const savedMessages = newMap.get(SAVED_CHAT_ID) ?? [];
      const forwardedMessage: Message = {
        ...message,
        id: crypto.randomUUID(),
        chatId: SAVED_CHAT_ID,
        forwardedFrom: sourceChatTitle ?? 'Unknown Chat',
        createdAt: new Date().toISOString(),
      };
      newMap.set(SAVED_CHAT_ID, [...savedMessages, forwardedMessage]);
      return newMap;
    });
  }

  /**
   * Создаёт новый чат через бэкенд.
   * Добавляет приветственное сообщение локально до появления LLM (этап 17).
   *
   * @returns Observable с созданным чатом
   */
  createChat(): Observable<Chat> {
    return this.http.post<Chat>(`${API_BASE_URL}/chats`, {}).pipe(
      tap(chat => {
        const welcomeMessage: Message = {
          id: crypto.randomUUID(),
          chatId: chat.id,
          role: 'assistant',
          content:
            'Здравствуйте! Я ИИ-помощник по подбору автомобилей.\n\n' +
            'Расскажите, какой автомобиль вы ищете, и я помогу выбрать ' +
            'оптимальный вариант. Можете указать:\n' +
            '• Бюджет\n' +
            '• Тип кузова (седан, кроссовер, хэтчбек...)\n' +
            '• Для каких целей нужен автомобиль\n\n' +
            'Или просто опишите свои пожелания своими словами!',
          createdAt: new Date().toISOString(),
        };

        this.chatsSignal.update(chats => [chat, ...chats]);
        this.messagesSignal.update(messagesMap => {
          const newMap = new Map(messagesMap);
          newMap.set(chat.id, [welcomeMessage]);
          return newMap;
        });
      }),
      catchError(err => {
        console.error('Ошибка создания чата:', err);
        throw err;
      }),
    );
  }

  /**
   * Обновляет заголовок чата через бэкенд.
   *
   * @param chatId - ID чата
   * @param updates - Объект с полями для обновления (только title)
   * @returns Observable с обновлённым чатом
   */
  updateChat(chatId: string, updates: Partial<Pick<Chat, 'title'>>): Observable<Chat> {
    if (chatId === SAVED_CHAT_ID) {
      console.warn('Нельзя редактировать чат Saved Messages');
      return of(SAVED_CHAT);
    }

    return this.http.patch<Chat>(`${API_BASE_URL}/chats/${chatId}`, updates).pipe(
      tap(updatedChat => {
        this.chatsSignal.update(chats =>
          chats.map(chat => (chat.id === chatId ? updatedChat : chat)),
        );
      }),
      catchError(err => {
        console.error('Ошибка обновления чата:', err);
        throw err;
      }),
    );
  }

  /**
   * Удаляет чат через бэкенд (soft delete).
   *
   * @param chatId - ID чата
   * @returns Observable<void>
   */
  deleteChat(chatId: string): Observable<void> {
    if (chatId === SAVED_CHAT_ID) {
      console.warn('Нельзя удалить чат Saved Messages');
      return of(undefined);
    }

    return this.http.delete<void>(`${API_BASE_URL}/chats/${chatId}`).pipe(
      tap(() => {
        this.chatsSignal.update(chats => chats.filter(chat => chat.id !== chatId));
        this.messagesSignal.update(messagesMap => {
          const newMap = new Map(messagesMap);
          newMap.delete(chatId);
          return newMap;
        });

        if (this.currentChatId() === chatId) {
          this.clearSelection();
        }
      }),
      catchError(err => {
        console.error('Ошибка удаления чата:', err);
        throw err;
      }),
    );
  }
}
