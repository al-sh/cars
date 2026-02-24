package com.carsai.back.car.dto;

import com.carsai.back.car.BodyType;
import com.carsai.back.car.DriveType;
import com.carsai.back.car.EngineType;
import com.carsai.back.car.Transmission;

import java.util.UUID;

/**
 * Сокращённый DTO для передачи в LLM Format.
 * Содержит только поля, необходимые для формирования ответа пользователю.
 */
public record CarShortDto(
        UUID id,
        String brand,
        String model,
        int year,
        int price,
        BodyType bodyType,
        EngineType engineType,
        int powerHp,
        Transmission transmission,
        DriveType drive
) {}
