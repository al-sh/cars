package com.carsai.back.llm;

import com.carsai.back.car.dto.SearchResult;
import com.carsai.back.config.LLMProperties;
import com.carsai.back.llm.dto.ExtractResult;
import com.carsai.back.llm.dto.GuardResult;
import com.carsai.back.llm.dto.LLMResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Сервис оркестрации LLM-запросов.
 *
 * Инкапсулирует промты и парсинг ответов, скрывая детали LLM API от бизнес-логики.
 * Используется MessageService для обработки сообщений пользователя.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {

    private final LLMProvider llmProvider;
    private final ObjectMapper objectMapper;
    private final LLMProperties props;

    // ===== Extract mode =====

    /**
     * Извлекает критерии поиска из сообщения пользователя.
     */
    public ExtractResult extractCriteria(String userMessage) {
        LLMResponse response = llmProvider.chat(EXTRACT_PROMPT, userMessage,
                props.getTemperature().getExtract());
        return parseExtractResult(response.getContent());
    }

    /**
     * Извлекает критерии с учётом накопленного контекста диалога.
     * Формирует user message: "Ранее известно: ... Текущее сообщение: {userMessage}"
     */
    public ExtractResult extractCriteria(String userMessage, String accumulatedSummary) {
        if (accumulatedSummary == null || accumulatedSummary.isBlank()) {
            return extractCriteria(userMessage);
        }
        String contextualMessage = "Ранее известно: " + accumulatedSummary + ".\n"
                + "Текущее сообщение пользователя: " + userMessage;
        LLMResponse response = llmProvider.chat(EXTRACT_PROMPT, contextualMessage,
                props.getTemperature().getExtract());
        return parseExtractResult(response.getContent());
    }

    // ===== Format mode =====

    /**
     * Форматирует результаты поиска в читаемый текст для пользователя.
     */
    public String formatResults(String userCriteria, SearchResult searchResult) {
        String input = buildFormatInput(userCriteria, searchResult);
        LLMResponse response = llmProvider.chat(FORMAT_PROMPT, input,
                props.getTemperature().getFormat());
        return response.getContent();
    }

    // ===== Guard mode =====

    /**
     * Проверяет, относится ли запрос к теме подбора автомобилей.
     */
    public GuardResult checkRelevance(String userMessage) {
        LLMResponse response = llmProvider.chat(GUARD_PROMPT, userMessage, 0.3);
        return parseGuardResult(response.getContent());
    }

    // ===== Title generation =====

    /**
     * Генерирует короткое название для чата на основе первого сообщения.
     */
    public String generateTitle(String firstMessage) {
        String systemPrompt = "Придумай короткое название (3-5 слов) для чата о подборе автомобиля. "
                + "Ответь только названием, без кавычек и пояснений.";
        LLMResponse response = llmProvider.chat(systemPrompt, firstMessage, 0.7);
        return response.getContent().trim();
    }

    // ===== Private helpers =====

    private ExtractResult parseExtractResult(String raw) {
        try {
            String json = extractJson(raw);
            return objectMapper.readValue(json, ExtractResult.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse LLM Extract response as JSON: {}", raw);
            return ExtractResult.needsClarification(
                    "Не удалось понять запрос. Пожалуйста, уточните, "
                    + "какой автомобиль вы ищете и на какую сумму рассчитываете?"
            );
        }
    }

    private GuardResult parseGuardResult(String raw) {
        try {
            String json = extractJson(raw);
            return objectMapper.readValue(json, GuardResult.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse LLM Guard response as JSON: {}", raw);
            // При ошибке парсинга — считаем запрос релевантным, чтобы не блокировать пользователей
            return GuardResult.builder().relevant(true).build();
        }
    }

    /**
     * Извлекает JSON из ответа LLM.
     * LLM может обернуть JSON в markdown-блок: ```json {...} ``` или добавить текст вокруг.
     */
    private String extractJson(String raw) {
        if (raw == null) return "{}";

        // Убираем markdown-блок ```json ... ```
        String stripped = raw.strip();
        if (stripped.startsWith("```")) {
            int start = stripped.indexOf('\n');
            int end = stripped.lastIndexOf("```");
            if (start >= 0 && end > start) {
                stripped = stripped.substring(start + 1, end).strip();
            }
        }

        // Берём подстроку от первой { до последней }
        int first = stripped.indexOf('{');
        int last = stripped.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return stripped.substring(first, last + 1);
        }

        return stripped;
    }

    @SuppressWarnings("unchecked")
    private String buildFormatInput(String userCriteria, SearchResult searchResult) {
        List<Map<String, Object>> results = searchResult.items().stream()
                .<Map<String, Object>>map(car -> objectMapper.convertValue(car, Map.class))
                .toList();

        Map<String, Object> input = Map.of(
                "userCriteria", userCriteria,
                "resultsCount", searchResult.count(),
                "results", results
        );

        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize format input", e);
            return "{\"resultsCount\":0,\"results\":[]}";
        }
    }

    // ===== Промты =====

    private static final String EXTRACT_PROMPT = """
            Ты — модуль извлечения критериев для сервиса подбора автомобилей CarsAI.

            ## Твоя задача
            Проанализировать сообщение пользователя и извлечь критерии поиска автомобиля.

            ## Правила

            ### Обязательный критерий:
            - priceMax — максимальная цена

            ### Дополнительные критерии (нужно минимум 2 для поиска):
            - priceMin — минимальная цена
            - bodyType — тип кузова (sedan, suv, hatchback, wagon, minivan, coupe, pickup)
            - engineType — тип двигателя (petrol, diesel, hybrid, electric)
            - brand — марка автомобиля
            - seats — количество мест
            - transmission — тип КПП (manual, automatic, robot, cvt)
            - drive — тип привода (fwd, rwd, awd)
            - yearMin — год выпуска от
            - yearMax — год выпуска до

            ### Логика работы:
            1. Если указана цена И минимум 2 дополнительных критерия — установи readyToSearch: true
            2. Если цена НЕ указана ИЛИ меньше 2 дополнительных критериев — readyToSearch: false и задай уточняющий вопрос
            3. Извлекай только явно указанные критерии
            4. НЕ додумывай критерии, которые пользователь не указал
            5. Задавай не более 2 вопросов за раз

            ### Нормализация значений:
            - "3 млн", "3 миллиона", "3000000" → 3000000
            - "кроссовер", "паркетник", "SUV" → "suv"
            - "автомат", "АКПП" → "automatic"
            - "механика", "МКПП" → "manual"
            - "робот", "роботизированная" → "robot"
            - "вариатор", "CVT" → "cvt"
            - "полный привод", "4WD", "AWD" → "awd"

            ## Формат ответа (строго JSON, без текста вокруг)
            {"readyToSearch": true|false, "criteria": {"priceMin": number|null, "priceMax": number|null, "bodyType": string|null, "engineType": string|null, "brand": string|null, "seats": number|null, "transmission": string|null, "drive": string|null, "yearMin": number|null, "yearMax": number|null}, "clarificationQuestion": string|null, "extractedInfo": string}
            """;

    private static final String FORMAT_PROMPT = """
            Ты — модуль форматирования ответов для сервиса подбора автомобилей CarsAI.

            ## Твоя задача
            Сформировать понятный и полезный ответ пользователю на основе результатов поиска.

            ## Правила
            1. Отвечай на русском языке
            2. Будь вежливым и профессиональным
            3. Используй данные ТОЛЬКО из предоставленных результатов
            4. НЕ выдумывай характеристики или цены
            5. Форматируй цены с разделителями (3 500 000 ₽)
            6. Выделяй ключевые характеристики каждого автомобиля

            ## Формат ответа
            - Краткое вступление (1 предложение)
            - Нумерованный список автомобилей (до 5 штук)
            - Для каждого: название, цена, ключевые характеристики
            - Заключительный вопрос или предложение

            ## Если результатов нет
            Вежливо сообщи, что по заданным критериям ничего не найдено, и предложи:
            - Увеличить бюджет
            - Расширить критерии поиска
            - Рассмотреть альтернативные варианты
            """;

    private static final String GUARD_PROMPT = """
            Ты — модуль проверки релевантности запросов для сервиса подбора автомобилей.

            ## Задача
            Определить, относится ли запрос пользователя к теме подбора автомобилей.

            ## Ответ (строго JSON, без текста вокруг)
            {"relevant": true|false, "rejectionResponse": string|null}

            rejectionResponse заполняется только если relevant: false.
            """;
}
