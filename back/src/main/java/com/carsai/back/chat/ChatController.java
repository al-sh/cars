package com.carsai.back.chat;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.carsai.back.chat.dto.ChatDto;
import com.carsai.back.chat.dto.CreateChatRequest;
import com.carsai.back.chat.dto.UpdateChatRequest;
import com.carsai.back.common.dto.PagedResponse;
import com.carsai.back.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * REST-контроллер чатов.
 *
 * Эндпоинты:
 *   GET    /api/v1/chats              — список чатов (пагинация + поиск)
 *   GET    /api/v1/chats/{id}         — один чат по ID
 *   POST   /api/v1/chats              — создать чат
 *   PATCH  /api/v1/chats/{id}         — обновить заголовок
 *   DELETE /api/v1/chats/{id}         — удалить чат (soft delete)
 *
 * Все эндпоинты требуют JWT-токена (настроено в SecurityConfig: anyRequest().authenticated()).
 * @AuthenticationPrincipal UserPrincipal — Spring извлекает текущего пользователя из токена.
 *
 * Контроллер не содержит бизнес-логику — только маршрутизация и делегирование в ChatService.
 * Аналог Express: controller вызывает service, а не пишет логику сам.
 */
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Получить список чатов текущего пользователя.
     *
     * GET /api/v1/chats?page=1&perPage=20&search=кроссовер
     *
     * @RequestParam(defaultValue = "1") — если параметр не передан, используем дефолт.
     * Spring автоматически конвертирует строку "1" → int 1.
     * Аналог Express: const page = parseInt(req.query.page) || 1
     *
     * @RequestParam(required = false) — поисковый параметр опционален.
     * Если search не передан — возвращаем все чаты без фильтрации.
     *
     * Пример ответа 200:
     * {
     *   "items": [{ "id": "...", "title": "Ищу кроссовер", "messageCount": 5, ... }],
     *   "total": 42,
     *   "page": 1,
     *   "perPage": 20
     * }
     */
    @GetMapping
    public PagedResponse<ChatDto> getChats(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage,
            @RequestParam(required = false) String search
    ) {
        return chatService.getUserChats(user.getId(), page, perPage, search);
    }

    /**
     * Получить один чат по ID.
     *
     * GET /api/v1/chats/550e8400-e29b-41d4-a716-446655440000
     *
     * @PathVariable — извлекает UUID из пути URL.
     * Spring автоматически конвертирует строку → UUID.
     * Если UUID невалиден (не тот формат) — Spring вернёт 400 автоматически.
     * Аналог Express: req.params.id
     *
     * Ответ 200 — ChatDto
     * Ответ 404 — если чат не найден или чужой (ChatNotFoundException → GlobalExceptionHandler)
     */
    @GetMapping("/{id}")
    public ChatDto getChatById(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id
    ) {
        return chatService.getChatById(user.getId(), id);
    }

    /**
     * Создать новый чат.
     *
     * POST /api/v1/chats
     * Body: {} или {"title": "Подбор седана"}
     *
     * @RequestBody(required = false) — тело запроса опционально.
     * Можно создать чат без title — название сгенерируется позже (этап LLM).
     *
     * @ResponseStatus(CREATED) — 201 Created вместо дефолтного 200.
     * Стандарт REST: успешное создание ресурса → 201.
     *
     * Ответ 201 — ChatDto созданного чата
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatDto createChat(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody(required = false) CreateChatRequest request
    ) {
        // request может быть null если тело не передано
        String title = request != null ? request.title() : null;
        return chatService.createChat(user.getId(), title);
    }

    /**
     * Обновить заголовок чата.
     *
     * PATCH /api/v1/chats/550e8400-...
     * Body: {"title": "Новый заголовок"}
     *
     * PATCH (не PUT) — частичное обновление.
     * PUT заменяет весь ресурс, PATCH — только переданные поля.
     * Мы обновляем только title — PATCH семантически точнее.
     * Аналог в Angular: HTTP-запрос patch() — обновляет только часть данных.
     *
     * Ответ 200 — ChatDto с обновлёнными данными
     * Ответ 404 — если чат не найден или чужой
     * Ответ 400 — если title пустой (Bean Validation)
     */
    @PatchMapping("/{id}")
    public ChatDto updateChat(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateChatRequest request
    ) {
        return chatService.updateChat(user.getId(), id, request.title());
    }

    /**
     * Удалить чат (soft delete).
     *
     * DELETE /api/v1/chats/550e8400-...
     *
     * @ResponseStatus(NO_CONTENT) — 204 No Content.
     * Стандарт REST: успешное удаление без тела ответа → 204.
     * Метод возвращает void — Spring автоматически вернёт пустой ответ.
     *
     * Soft delete: данные остаются в БД, deleted = true.
     * Сообщения в чате НЕ удаляются — они привязаны к chat_id и тоже "скрываются".
     *
     * Ответ 204 — успешно удалён
     * Ответ 404 — если чат не найден или чужой
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChat(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id
    ) {
        chatService.deleteChat(user.getId(), id);
    }
}
