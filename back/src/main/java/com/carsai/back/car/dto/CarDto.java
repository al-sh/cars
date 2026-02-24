package com.carsai.back.car.dto;

import com.carsai.back.car.BodyType;
import com.carsai.back.car.Car;
import com.carsai.back.car.DriveType;
import com.carsai.back.car.EngineType;
import com.carsai.back.car.Transmission;

import java.math.BigDecimal;
import java.util.UUID;

public record CarDto(
        UUID id,
        String brand,
        String model,
        int year,
        int price,
        BodyType bodyType,
        EngineType engineType,
        BigDecimal engineVolume,
        int powerHp,
        Transmission transmission,
        DriveType drive,
        int seats,
        BigDecimal fuelConsumption,
        String description,
        String imageUrl
) {
    public static CarDto from(Car car) {
        return new CarDto(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getYear(),
                car.getPrice(),
                car.getBodyType(),
                car.getEngineType(),
                car.getEngineVolume(),
                car.getPowerHp(),
                car.getTransmission(),
                car.getDrive(),
                car.getSeats(),
                car.getFuelConsumption(),
                car.getDescription(),
                car.getImageUrl()
        );
    }
}
