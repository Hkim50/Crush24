package com.crushai.crushai.enums;

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

    // 열거형이 가진 값을 반환하는 getter 메서드
    public String getValue() {
        return this.value;
    }

    // 문자열을 Gender 열거형으로 변환하는 정적 팩토리 메서드
    public static Gender fromString(String text) {
        return Arrays.stream(Gender.values())
                .filter(gender -> gender.value.equalsIgnoreCase(text))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No constant with text: " + text + " found"));
    }
}