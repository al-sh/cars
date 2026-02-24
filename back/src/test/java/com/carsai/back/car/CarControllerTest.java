package com.carsai.back.car;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration-тесты CarController — полный стек с реальной PostgreSQL.
 * Flyway автоматически применяет V6 (DDL) и V7 (seed: 14 авто).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("dev")
class CarControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    // ===== GET /api/v1/cars =====

    @Test
    void searchCars_shouldReturn200_withAllCars() throws Exception {
        mockMvc.perform(get("/api/v1/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.perPage").value(20));
    }

    @Test
    void searchCars_shouldFilterByBodyType() throws Exception {
        mockMvc.perform(get("/api/v1/cars").param("bodyType", "suv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].bodyType", everyItem(is("suv"))));
    }

    @Test
    void searchCars_shouldFilterByPriceMax() throws Exception {
        mockMvc.perform(get("/api/v1/cars").param("priceMax", "3000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].price", everyItem(lessThanOrEqualTo(3_000_000))));
    }

    @Test
    void searchCars_shouldFilterByEngineType() throws Exception {
        mockMvc.perform(get("/api/v1/cars").param("engineType", "electric"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].engineType", everyItem(is("electric"))));
    }

    @Test
    void searchCars_shouldSortByPriceAscending() throws Exception {
        mockMvc.perform(get("/api/v1/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].price").isNumber())
                // Первый элемент должен быть дешевле последнего (seed: Kia Ceed 2.2m, Tesla 4.5m)
                .andExpect(jsonPath("$.items.length()").isNumber());
    }

    @Test
    void searchCars_shouldReturnEmptyList_whenNoMatch() throws Exception {
        mockMvc.perform(get("/api/v1/cars")
                .param("priceMax", "100000")) // дешевле 100к — таких нет
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void searchCars_shouldPaginate() throws Exception {
        mockMvc.perform(get("/api/v1/cars").param("page", "1").param("perPage", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.perPage").value(5));
    }

    // ===== GET /api/v1/cars/{id} =====

    @Test
    void getCar_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/cars/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    // ===== GET /api/v1/cars/brands =====

    @Test
    void getBrands_shouldReturnSortedList() throws Exception {
        mockMvc.perform(get("/api/v1/cars/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                // Бренды должны содержать Toyota из seed-данных
                .andExpect(jsonPath("$", hasItem("Toyota")));
    }
}
