package com.stephanofer.prismapractice.api.arena;

import java.util.Locale;
import java.util.Objects;

public record ArenaId(String value) {

    public ArenaId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Arena id must not be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
