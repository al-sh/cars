package com.carsai.back.car;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BodyType {
    SEDAN, SUV, HATCHBACK, WAGON, MINIVAN, COUPE, PICKUP;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static BodyType fromJson(String value) {
        return valueOf(value.toUpperCase());
    }
}
