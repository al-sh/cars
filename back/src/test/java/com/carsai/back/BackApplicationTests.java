package com.carsai.back;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke-тест — проверяет, что Spring-контекст поднимается без ошибок.
 *
 * @SpringBootTest — запускает полный Spring-контекст (все бины, конфигурация, БД).
 * Если какой-то бин не может быть создан — тест упадёт с понятной ошибкой.
 *
 * @Testcontainers — активирует управление Docker-контейнерами через Testcontainers.
 * Перед тестами запускает PostgreSQL-контейнер, после — останавливает.
 *
 * @ActiveProfiles("dev") — загружает application-dev.yml поверх application.yml.
 * Нужно для jwt.secret: dev-профиль задаёт тестовое значение вместо ${JWT_SECRET}.
 *
 * @ServiceConnection — Spring Boot 3.1+: автоматически настраивает datasource
 * из запущенного контейнера. Заменяет @DynamicPropertySource — не нужно
 * вручную регистрировать spring.datasource.url/username/password.
 * Spring читает JDBC URL, логин, пароль напрямую из PostgreSQLContainer.
 *
 * Аналог Angular: ng test — компилирует весь модуль и проверяет, что он собирается.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("dev")
class BackApplicationTests {

    // @Container + static — один контейнер на весь класс (не пересоздаётся между тестами).
    // @ServiceConnection — читает JDBC URL, username, password из контейнера
    // и автоматически перекрывает spring.datasource.* свойства.
    // Порт PostgreSQL динамический (Testcontainers выбирает свободный) —
    // @ServiceConnection решает это без ручной регистрации через @DynamicPropertySource.
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void contextLoads() {
        // Пустой тест — сам факт успешного запуска контекста и есть проверка.
        // Если Spring не сможет поднять контекст — тест упадёт с ошибкой.
    }
}
