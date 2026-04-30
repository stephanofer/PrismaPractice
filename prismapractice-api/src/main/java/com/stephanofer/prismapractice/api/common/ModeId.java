package com.stephanofer.prismapractice.api.common;

import java.util.Locale;
import java.util.Objects;

public record ModeId(String value) {

    public ModeId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Mode id must not be blank");
        }
        value = normalize(value);
    }

    public static String normalize(String value) {
        return Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
