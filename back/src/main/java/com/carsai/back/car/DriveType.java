package com.carsai.back.car;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DriveType {
    FWD, RWD, AWD;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static DriveType fromJson(String value) {
        return valueOf(value.toUpperCase());
    }
}
