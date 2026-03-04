package com.carsai.back.car.dto;

import com.carsai.back.car.BodyType;
import com.carsai.back.car.DriveType;
import com.carsai.back.car.EngineType;
import com.carsai.back.car.Transmission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarSearchCriteria {
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
    private Integer powerMin;
    private Integer powerMax;
    private BigDecimal fuelConsumptionMax;
    private BigDecimal engineVolumeMin;
    private BigDecimal engineVolumeMax;
}
