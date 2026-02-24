package com.carsai.back.car;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Transmission {
    MANUAL, AUTOMATIC, ROBOT, CVT;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static Transmission fromJson(String value) {
        return valueOf(value.toUpperCase());
    }
}
