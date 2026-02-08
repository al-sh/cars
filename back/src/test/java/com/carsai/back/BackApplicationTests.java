package com.carsai.back;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke-тест — проверяет, что Spring-контекст поднимается без ошибок.
 *
 * @SpringBootTest — запускает полный Spring-контекст (все бины, конфигурация).
 * Если какой-то бин не может быть создан (например, ошибка в конфигурации) — тест упадёт.
 *
 * Аналог: в Angular — `TestBed.configureTestingModule({}).compileComponents()` —
 * проверка, что модуль собирается без ошибок.
 */
@SpringBootTest
class BackApplicationTests {

    @Test
    void contextLoads() {
        // Пустой тест — сам факт успешного запуска контекста и есть проверка.
        // Если Spring не сможет поднять контекст — тест упадёт с ошибкой.
    }
}
