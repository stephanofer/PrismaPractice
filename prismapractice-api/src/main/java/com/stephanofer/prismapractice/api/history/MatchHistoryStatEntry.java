package com.stephanofer.prismapractice.api.history;

import java.util.Objects;

public record MatchHistoryStatEntry(String scope, String key, String value) {

    public MatchHistoryStatEntry {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (scope.isBlank() || key.isBlank()) {
            throw new IllegalArgumentException("scope/key must not be blank");
        }
    }
}
