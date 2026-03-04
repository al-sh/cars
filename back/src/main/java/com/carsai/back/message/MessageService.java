package com.carsai.back.message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsai.back.car.CarService;
import com.carsai.back.car.dto.CarSearchCriteria;
import com.carsai.back.car.dto.SearchResult;
import com.carsai.back.chat.Chat;
import com.carsai.back.chat.ChatRepository;
import com.carsai.back.chat.ChatService;
import com.carsai.back.common.dto.CursorResponse;
import com.carsai.back.common.exception.ChatNotFoundException;
import com.carsai.back.llm.LLMService;
import com.carsai.back.llm.dto.ExtractResult;
import com.carsai.back.llm.dto.GuardResult;
import com.carsai.back.message.dto.MessageDto;
import com.carsai.back.message.dto.SendMessageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис сообщений — загрузка истории и обработка новых сообщений.
 *
 * Поток обработки: Guard → Extract → Search → Format → сохранение.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final LLMService llmService;
    private final CarService carService;
    private final ChatService chatService;

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

    /**
     * Обрабатывает сообщение пользователя: Guard → Extract → Search → Format.
     * Сохраняет user message и assistant message в БД.
     */
    @Transactional
    public SendMessageResponse sendMessage(UUID userId, UUID chatId, String content) {
        Chat chat = chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId)
                .orElseThrow(() -> new ChatNotFoundException(chatId));

        // 1. Сохранить user message
        Message userMessage = messageRepository.save(
                Message.builder()
                        .chat(chat)
                        .role(MessageRole.USER)
                        .content(content)
                        .build()
        );

        // 2. Обработать через LLM pipeline
        String responseText = processMessage(chat, content, userMessage.getId());

        // 3. Сохранить assistant message
        Message assistantMessage = messageRepository.save(
                Message.builder()
                        .chat(chat)
                        .role(MessageRole.ASSISTANT)
                        .content(responseText)
                        .build()
        );

        // 4. Генерация title для нового чата (асинхронно)
        if (chat.getTitle() == null) {
            chatService.generateTitleAsync(chatId, content);
        }

        return new SendMessageResponse(
                MessageDto.from(userMessage),
                MessageDto.from(assistantMessage)
        );
    }

    /**
     * Основной pipeline обработки сообщения.
     * Guard → Extract (с accumulated_criteria) → Search → Format.
     */
    private String processMessage(Chat chat, String content, UUID messageId) {
        UUID chatId = chat.getId();

        // Step 1: Guard — проверка релевантности
        GuardResult guardResult = llmService.checkRelevance(content, chatId, messageId);
        if (!guardResult.isRelevant()) {
            return guardResult.getRejectionResponse();
        }

        // Step 2: Extract — извлечение критериев с учётом накопленных
        String accumulatedSummary = buildAccumulatedSummary(chat.getAccumulatedCriteria());
        ExtractResult extractResult = llmService.extractCriteria(content, accumulatedSummary,
                chatId, messageId);

        // Мерж критериев (даже частичных) в accumulated_criteria
        if (extractResult.getCriteria() != null) {
            mergeAccumulatedCriteria(chat, extractResult.getCriteria());
        }

        // Step 3: Если недостаточно критериев — вернуть уточняющий вопрос
        if (!extractResult.isReadyToSearch()) {
            return extractResult.getClarificationQuestion() != null
                    ? extractResult.getClarificationQuestion()
                    : "Пожалуйста, уточните параметры автомобиля.";
        }

        // Step 4: Собрать объединённые критерии и выполнить поиск
        CarSearchCriteria mergedCriteria = buildMergedCriteria(chat.getAccumulatedCriteria());
        SearchResult searchResult = carService.searchForChat(mergedCriteria, 10);

        // Step 5: Format — форматирование результатов через LLM
        String extractedInfo = extractResult.getExtractedInfo() != null
                ? extractResult.getExtractedInfo()
                : accumulatedSummary;
        return llmService.formatResults(extractedInfo, searchResult, mergedCriteria, chatId, messageId);
    }

    /**
     * Формирует текстовое описание накопленных критериев для LLM Extract.
     */
    private String buildAccumulatedSummary(Map<String, Object> accumulated) {
        if (accumulated == null || accumulated.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        accumulated.forEach((key, value) -> {
            if (value != null) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(key).append(": ").append(value);
            }
        });
        return sb.toString();
    }

    /**
     * Мержит новые критерии в accumulated_criteria чата.
     * Null-значения в новых критериях НЕ стирают старые.
     */
    private void mergeAccumulatedCriteria(Chat chat, CarSearchCriteria criteria) {
        Map<String, Object> merged = new HashMap<>(
                chat.getAccumulatedCriteria() != null ? chat.getAccumulatedCriteria() : Map.of()
        );

        if (criteria.getPriceMin() != null) merged.put("priceMin", criteria.getPriceMin());
        if (criteria.getPriceMax() != null) merged.put("priceMax", criteria.getPriceMax());
        if (criteria.getBodyType() != null) merged.put("bodyType", criteria.getBodyType().name().toLowerCase());
        if (criteria.getEngineType() != null) merged.put("engineType", criteria.getEngineType().name().toLowerCase());
        if (criteria.getBrand() != null) merged.put("brand", criteria.getBrand());
        if (criteria.getSeats() != null) merged.put("seats", criteria.getSeats());
        if (criteria.getTransmission() != null) merged.put("transmission", criteria.getTransmission().name().toLowerCase());
        if (criteria.getDrive() != null) merged.put("drive", criteria.getDrive().name().toLowerCase());
        if (criteria.getYearMin() != null) merged.put("yearMin", criteria.getYearMin());
        if (criteria.getYearMax() != null) merged.put("yearMax", criteria.getYearMax());

        chat.setAccumulatedCriteria(merged);
        chatRepository.save(chat);
    }

    /**
     * Строит CarSearchCriteria из accumulated_criteria (Map → typed object).
     */
    private CarSearchCriteria buildMergedCriteria(Map<String, Object> accumulated) {
        if (accumulated == null) return CarSearchCriteria.builder().build();

        return CarSearchCriteria.builder()
                .priceMin(getInt(accumulated, "priceMin"))
                .priceMax(getInt(accumulated, "priceMax"))
                .bodyType(getEnum(accumulated, "bodyType", com.carsai.back.car.BodyType.class))
                .engineType(getEnum(accumulated, "engineType", com.carsai.back.car.EngineType.class))
                .brand((String) accumulated.get("brand"))
                .seats(getInt(accumulated, "seats"))
                .transmission(getEnum(accumulated, "transmission", com.carsai.back.car.Transmission.class))
                .drive(getEnum(accumulated, "drive", com.carsai.back.car.DriveType.class))
                .yearMin(getInt(accumulated, "yearMin"))
                .yearMax(getInt(accumulated, "yearMax"))
                .build();
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number n) return n.intValue();
        return null;
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> E getEnum(Map<String, Object> map, String key, Class<E> enumClass) {
        Object value = map.get(key);
        if (value instanceof String s) {
            try {
                return Enum.valueOf(enumClass, s.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}
