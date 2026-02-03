package com.example.budongbatch.common.enums;

public enum PropertyType {
    APARTMENT("아파트"),
    VILLA("빌라"),
    OFFICETEL("오피스텔");

    private final String description;

    PropertyType(String description) {
        this.description = description;
    }
}
