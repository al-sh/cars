package com.carsai.back.car;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EngineType {
    PETROL, DIESEL, HYBRID, ELECTRIC;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static EngineType fromJson(String value) {
        return valueOf(value.toUpperCase());
    }
}
