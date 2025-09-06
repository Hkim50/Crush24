package com.crushai.crushai.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.NoSuchElementException;

public enum Gender {
    MALE("MALE"),
    FEMALE("FEMALE"),
    NON_BINARY("NON_BINARY");

    private final String value;

    Gender(String value) {
        this.value = value;
    }

    // This method is optional but can be useful for serialization.
    @JsonValue
    public String getValue() {
        return this.value;
    }

    // This single method handles both JSON deserialization and general string-to-enum conversion.
    @JsonCreator
    public static Gender fromString(String text) {
        return Arrays.stream(Gender.values())
                .filter(gender -> gender.value.equalsIgnoreCase(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No constant with text: " + text + " found"));
    }
}