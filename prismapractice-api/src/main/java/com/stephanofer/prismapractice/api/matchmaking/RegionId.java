package com.stephanofer.prismapractice.api.matchmaking;

import java.util.Locale;
import java.util.Objects;

public record RegionId(String value) {

    public RegionId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Region id must not be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
