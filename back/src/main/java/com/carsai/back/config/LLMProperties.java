package com.carsai.back.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Типизированная конфигурация LLM-интеграции.
 * Заменяет разрозненные @Value-аннотации на единый класс с final-полями,
 * пригодными для constructor injection и unit-тестирования без Spring-контекста.
 */
@ConfigurationProperties(prefix = "llm")
@Data
public class LLMProperties {

    private String apiKey;
    private String baseUrl;
    private String model = "deepseek-chat";
    private String provider = "deepseek";
    private Duration timeout = Duration.ofSeconds(60);
    private int maxTokens = 1024;

    private Temperature temperature = new Temperature();
    private Retry retry = new Retry();

    @Data
    public static class Temperature {
        private double extract = 0.3;
        private double format = 0.7;
    }

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofSeconds(1);
    }
}
