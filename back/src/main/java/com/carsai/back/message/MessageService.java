package com.carsai.back.message;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.carsai.back.chat.ChatRepository;
import com.carsai.back.common.dto.CursorResponse;
import com.carsai.back.common.exception.ChatNotFoundException;
import com.carsai.back.message.dto.MessageDto;

import lombok.RequiredArgsConstructor;

/**
 * Сервис сообщений — загрузка истории чата с cursor-based пагинацией.
 *
 * Cursor-пагинация vs offset:
 *   Offset: LIMIT 50 OFFSET 100 — нестабильно при новых сообщениях
 *   Cursor: WHERE id < cursorId ORDER BY created_at DESC LIMIT 50 — стабильно
 *
 * Контракт: GET /api/v1/chats/{chatId}/messages?limit=50&before={messageId}
 * Ответ: { items: MessageDto[], hasMore: boolean }
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;

    /**
     * Загружает сообщения чата с cursor-based пагинацией.
     *
     * Алгоритм:
     * 1. Проверяем, что чат принадлежит пользователю.
     * 2. Если before == null — загружаем последние N сообщений.
     * 3. Если before задан — находим createdAt курсора и загружаем N сообщений до него.
     * 4. Запрашиваем limit + 1 элемент: если вернулось больше limit — есть ещё (hasMore = true).
     * 5. Разворачиваем список: БД возвращает DESC (свежие первые), UI ожидает ASC (старые первые).
     *
     * @param chatId UUID чата
     * @param userId UUID пользователя из JWT (для проверки доступа)
     * @param limit  количество сообщений на странице (max 200)
     * @param before UUID сообщения-курсора (загрузить сообщения до него), null для первой страницы
     * @return CursorResponse с сообщениями и флагом hasMore
     */
    public CursorResponse<MessageDto> getMessages(UUID chatId, UUID userId, int limit, UUID before) {
        // Проверяем доступ: чат должен существовать и принадлежать пользователю.
        chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId)
                .orElseThrow(() -> new ChatNotFoundException(chatId));

        // Запрашиваем limit + 1 чтобы определить hasMore без лишнего COUNT запроса.
        // Паттерн "limit + 1": если вернулось > limit элементов — ещё есть данные.
        Pageable pageable = PageRequest.of(0, limit + 1, Sort.by("createdAt").descending());

        List<Message> messages;
        if (before == null) {
            // Первая загрузка: берём последние limit+1 сообщений чата.
            messages = messageRepository.findByChatId(chatId, pageable);
        } else {
            // Подгрузка истории: находим createdAt курсора, берём сообщения до него.
            // Если курсор не найден — возвращаем пустой список (не ошибку).
            var cursor = messageRepository.findById(before);
            if (cursor.isEmpty()) {
                return new CursorResponse<>(List.of(), false);
            }
            messages = messageRepository.findByChatIdAndCreatedAtBefore(
                    chatId, cursor.get().getCreatedAt(), pageable);
        }

        boolean hasMore = messages.size() > limit;
        // Обрезаем лишний элемент (limit+1 → limit).
        List<Message> page = hasMore ? messages.subList(0, limit) : messages;

        // БД вернула DESC (свежие первые) — разворачиваем в ASC для UI (старые первые).
        List<MessageDto> items = page.reversed().stream()
                .map(MessageDto::from)
                .toList();

        return new CursorResponse<>(items, hasMore);
    }
}
