package com.carsai.back.car.dto;

import com.carsai.back.car.BodyType;
import com.carsai.back.car.DriveType;
import com.carsai.back.car.EngineType;
import com.carsai.back.car.Transmission;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CarSearchCriteria {
    // LLM-извлекаемые критерии
    private Integer priceMin;
    private Integer priceMax;
    private BodyType bodyType;
    private EngineType engineType;
    private String brand;
    private Integer seats;
    private Transmission transmission;
    private DriveType drive;
    private Integer yearMin;
    private Integer yearMax;

    // Только для REST API, LLM не извлекает
    private Integer minPower;
    private Integer maxPower;
    private BigDecimal maxFuelConsumption;
}
