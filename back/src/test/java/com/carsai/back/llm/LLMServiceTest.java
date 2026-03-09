package com.carsai.back.llm;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.carsai.back.car.BodyType;
import com.carsai.back.car.DriveType;
import com.carsai.back.car.EngineType;
import com.carsai.back.car.Transmission;
import com.carsai.back.car.dto.CarSearchCriteria;
import com.carsai.back.car.dto.CarShortDto;
import com.carsai.back.car.dto.SearchResult;
import com.carsai.back.config.LLMProperties;
import com.carsai.back.llm.dto.ExtractResult;
import com.carsai.back.llm.dto.GuardResult;
import com.carsai.back.llm.dto.LLMResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

/**
 * Unit-тесты LLMService — проверяем парсинг ответов LLM и логику оркестрации.
 * LLMProvider — мок, реальных HTTP-запросов нет.
 */
@ExtendWith(MockitoExtension.class)
class LLMServiceTest {

    @Mock
    private LLMProvider llmProvider;

    @Mock
    private LlmLogService llmLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID TEST_CHAT_ID = UUID.randomUUID();
    private static final UUID TEST_MESSAGE_ID = UUID.randomUUID();

    private LLMService serviceUnderTest() {
        return new LLMService(llmProvider, objectMapper, defaultProps(), llmLogService);
    }

    private LLMProperties defaultProps() {
        LLMProperties props = new LLMProperties();
        props.setTemperature(new LLMProperties.Temperature());
        props.setRetry(new LLMProperties.Retry());
        return props;
    }

    // ===== extractCriteria =====

    @Test
    void extractCriteria_shouldParseReadyToSearch() {
        // given
        String json = """
                {
                  "readyToSearch": true,
                  "criteria": {
                    "priceMax": 3000000,
                    "bodyType": "suv",
                    "engineType": "petrol",
                    "transmission": "automatic"
                  },
                  "clarificationQuestion": null,
                  "extractedInfo": "Кроссовер, бензин, автомат, до 3 млн"
                }
                """;
        when(llmProvider.chat(anyString(), anyString(), anyDouble()))
                .thenReturn(LLMResponse.builder().content(json).build());

        // when
        ExtractResult result = serviceUnderTest().extractCriteria("Кроссовер до 3 млн, бензин, автомат", TEST_CHAT_ID, TEST_MESSAGE_ID);

        // then
        assertThat(result.isReadyToSearch()).isTrue();
        assertThat(result.getCriteria().getBodyType()).isEqualTo(BodyType.SUV);
        assertThat(result.getCriteria().getEngineType()).isEqualTo(EngineType.PETROL);
        assertThat(result.getCriteria().getPriceMax()).isEqualTo(3_000_000);
        assertThat(result.getClarificationQuestion()).isNull();
    }

    @Test
    void extractCriteria_shouldParseNotReadyToSearch() {
        // given
        String json = """
                {
                  "readyToSearch": false,
                  "criteria": {"priceMax": 3000000, "bodyType": "suv"},
                  "clarificationQuestion": "Уточните тип двигателя?",
                  "extractedInfo": "Кроссовер до 3 млн"
                }
                """;
        when(llmProvider.chat(anyString(), anyString(), anyDouble()))
                .thenReturn(LLMResponse.builder().content(json).build());

        // when
        ExtractResult result = serviceUnderTest().extractCriteria("Кроссовер до 3 млн", TEST_CHAT_ID, TEST_MESSAGE_ID);

        // then
        assertThat(result.isReadyToSearch()).isFalse();
        assertThat(result.getClarificationQuestion()).isEqualTo("Уточните тип двигателя?");
    }

    @Test
    void extractCriteria_shouldReturnFallback_whenJsonInvalid() {
        // given — LLM вернул не JSON (сломанный ответ)
        when(llmProvider.chat(anyString(), anyString(), anyDouble()))
                .thenReturn(LLMResponse.builder().content("Извините, не понимаю вопрос").build());

        // when
        ExtractResult result = serviceUnderTest().extractCriteria("любой запрос", TEST_CHAT_ID, TEST_MESSAGE_ID);

        // then — fallback: просим уточнить
        assertThat(result.isReadyToSearch()).isFalse();
        assertThat(result.getClarificationQuestion()).isNotBlank();
    }

    @Test
    void extractCriteria_shouldParseJson_wrappedInMarkdownBlock() {
        // given — LLM обернул ответ в ```json ... ```
        String wrapped = """
                ```json
                {"readyToSearch": false, "criteria": {}, "clarificationQuestion": "Что ищете?", "extractedInfo": ""}
                ```
                """;
        when(llmProvider.chat(anyString(), anyString(), anyDouble()))
                .thenReturn(LLMResponse.builder().content(wrapped).build());

        // when
        ExtractResult result = serviceUnderTest().extractCriteria("Хочу машину", TEST_CHAT_ID, TEST_MESSAGE_ID);

        // then
        assertThat(result.isReadyToSearch()).isFalse();
        assertThat(result.getClarificationQuestion()).isEqualTo("Что ищете?");
    }

    @Test
    void extractCriteria_withAccumulatedSummary_shouldIncludeContextInMessage() {
        // given
        String json = "{\"readyToSearch\": true, \"criteria\": {\"priceMax\": 3000000, \"bodyType\": \"suv\", \"engineType\": \"petrol\", \"transmission\": \"automatic\"}, \"clarificationQuestion\": null, \"extractedInfo\": \"\"}";
        when(llmProvider.chat(anyString(), contains("Ранее известно"), anyDouble()))
                .thenReturn(LLMResponse.builder().content(json).build());

        // when
        serviceUnderTest().extractCriteria("Бензин, автомат", "кроссовер до 3 000 000 ₽", TEST_CHAT_ID, TEST_MESSAGE_ID);

        // then — убеждаемся что LLM получил контекстуальное сообщение с "Ранее известно"
        verify(llmProvider).chat(anyString(), contains("Ранее известно"), anyDouble());
    }

    // ===== checkRelevance =====

    @Test
    void checkRelevance_shouldReturnRelevant_whenAboutCars() {
        // given — JSON-ключ "relevant" (совпадает с именем поля в GuardResult)
        String json = "{\"relevant\": true, \"rejectionResponse\": null}";
        when(llmProvider.chat(anyString(), anyString(), anyDouble()))
                .thenReturn(LLMResponse.builder().content(json).build());

        // when
        GuardResult result = serviceUnderTest().checkRelevance("Хочу купить кроссовер", TEST_CHAT_ID, TEST_MESSAGE_ID);

        // then
        assertThat(result.isRelevant()).isTrue();
        assertThat(result.getRejectionResponse()).isNull();
    }

    @Test
    void checkRelevance_shouldReturnNotRelevant_whenOffTopic() {
        // given
        String json = "{\"relevant\": false, \"rejectionResponse\": \"Я помогаю с подбором авто.\"}";
        when(llmProvider.chat(anyString(), anyString(), anyDouble()))
                .thenReturn(LLMResponse.builder().content(json).build());

        // when
        GuardResult result = serviceUnderTest().checkRelevance("Реши задачу по математике", TEST_CHAT_ID, TEST_MESSAGE_ID);

        // then
        assertThat(result.isRelevant()).isFalse();
        assertThat(result.getRejectionResponse()).isEqualTo("Я помогаю с подбором авто.");
    }

    @Test
    void checkRelevance_shouldReturnRelevant_whenGuardJsonInvalid() {
        // given — ошибка парсинга → безопасный fallback (пропустить запрос)
        when(llmProvider.chat(anyString(), anyString(), anyDouble()))
                .thenReturn(LLMResponse.builder().content("не JSON").build());

        // when
        GuardResult result = serviceUnderTest().checkRelevance("что-то", TEST_CHAT_ID, TEST_MESSAGE_ID);

        // then — при ошибке Guard пропускаем запрос (не блокируем пользователей)
        assertThat(result.isRelevant()).isTrue();
    }

    // ===== formatResults =====

    @Test
    void formatResults_shouldReturnLLMContent() {
        // given
        String expectedText = "Нашёл 1 кроссовер:\n1. Toyota RAV4 2022 — 2 900 000 ₽";
        when(llmProvider.chat(anyString(), anyString(), anyDouble()))
                .thenReturn(LLMResponse.builder().content(expectedText).build());

        SearchResult searchResult = SearchResult.builder()
                .count(1)
                .items(List.of(new CarShortDto(
                        UUID.randomUUID(), "Toyota", "RAV4", 2022, 2_900_000,
                        BodyType.SUV, EngineType.PETROL, 199, Transmission.AUTOMATIC, DriveType.AWD
                )))
                .build();

        // when
        String result = serviceUnderTest().formatResults("Кроссовер до 3 млн, автомат", searchResult, CarSearchCriteria.builder().build(), TEST_CHAT_ID, TEST_MESSAGE_ID);

        // then
        assertThat(result).isEqualTo(expectedText);
    }

    // ===== generateTitle =====

    @Test
    void generateTitle_shouldTrimWhitespace() {
        // given — LLM иногда добавляет пробелы/переносы вокруг ответа
        when(llmProvider.chat(anyString(), anyString(), anyDouble()))
                .thenReturn(LLMResponse.builder().content("  Подбор кроссовера до 3 млн  \n").build());

        // when
        String title = serviceUnderTest().generateTitle("Хочу кроссовер до 3 млн, бензин, автомат", TEST_CHAT_ID);

        // then
        assertThat(title).isEqualTo("Подбор кроссовера до 3 млн");
    }
}
