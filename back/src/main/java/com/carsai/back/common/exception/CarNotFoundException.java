package com.carsai.back.common.exception;

import java.util.UUID;

public class CarNotFoundException extends RuntimeException {

    public CarNotFoundException(UUID carId) {
        super("Автомобиль не найден: " + carId);
    }
}
