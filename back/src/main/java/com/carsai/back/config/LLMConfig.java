package com.carsai.back.config;

import com.carsai.back.llm.DeepSeekLLMProvider;
import com.carsai.back.llm.LLMProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация LLM-интеграции.
 *
 * DeepSeekLLMProvider намеренно не помечен @Service — создаётся здесь через @Bean,
 * чтобы избежать двойной регистрации в контексте Spring.
 */
@Configuration
@EnableConfigurationProperties(LLMProperties.class)
@Slf4j
public class LLMConfig {

    /**
     * WebClient для запросов к DeepSeek API.
     * Authorization header задаётся здесь один раз — не в каждом запросе.
     */
    @Bean
    public WebClient deepseekClient(LLMProperties props) {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    /**
     * Выбор активного LLMProvider на основе llm.provider.
     * @Primary — используется при инжекции LLMProvider по типу.
     * В тестах переопределяется через @TestConfiguration.
     */
    @Bean
    @Primary
    public LLMProvider llmProvider(LLMProperties props, WebClient deepseekClient) {
        return switch (props.getProvider().toLowerCase()) {
            case "deepseek" -> new DeepSeekLLMProvider(deepseekClient, props);
            default -> {
                log.warn("Unknown LLM provider: '{}', falling back to deepseek", props.getProvider());
                yield new DeepSeekLLMProvider(deepseekClient, props);
            }
        };
    }
}
