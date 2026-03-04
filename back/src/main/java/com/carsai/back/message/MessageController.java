package com.carsai.back.message;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carsai.back.common.dto.CursorResponse;
import com.carsai.back.message.dto.MessageDto;
import com.carsai.back.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * REST-контроллер сообщений.
 *
 * Эндпоинты:
 *   GET /api/v1/chats/{chatId}/messages — список сообщений (cursor pagination)
 *
 * Вложенный маршрут /chats/{chatId}/messages — сообщения принадлежат чату.
 * Доступ ограничен JWT (SecurityConfig: anyRequest().authenticated()).
 */
@RestController
@RequestMapping("/api/v1/chats/{chatId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * Загрузить сообщения чата с cursor-based пагинацией.
     *
     * GET /api/v1/chats/{chatId}/messages?limit=50
     * GET /api/v1/chats/{chatId}/messages?limit=50&before={messageId}
     *
     * @param chatId UUID чата из пути
     * @param user   текущий пользователь из JWT
     * @param limit  кол-во сообщений (по умолчанию 50, максимум 200)
     * @param before UUID сообщения-курсора для подгрузки более старой истории (опционально)
     * @return { items: MessageDto[], hasMore: boolean }
     */
    @GetMapping
    public CursorResponse<MessageDto> getMessages(
            @PathVariable UUID chatId,
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) UUID before
    ) {
        int clampedLimit = Math.min(limit, 200);
        return messageService.getMessages(chatId, user.getId(), clampedLimit, before);
    }
}
