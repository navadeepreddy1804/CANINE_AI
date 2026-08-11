package com.canineai.backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Gender {
    MALE,
    FEMALE,
    OTHER;

    @JsonCreator
    public static Gender fromString(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        String trimmed = value.trim().toUpperCase();
        for (Gender gender : values()) {
            if (gender.name().equals(trimmed)) {
                return gender;
            }
        }
        return OTHER;
    }
}
