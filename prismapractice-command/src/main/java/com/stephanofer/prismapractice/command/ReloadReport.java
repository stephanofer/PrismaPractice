package com.stephanofer.prismapractice.command;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record ReloadReport(
        String requestedScope,
        List<String> resolvedScopes,
        List<Entry> entries,
        long durationMillis,
        boolean successful,
        @Nullable String failureScope,
        @Nullable String failureMessage
) {

    public ReloadReport {
        Objects.requireNonNull(requestedScope, "requestedScope");
        resolvedScopes = List.copyOf(Objects.requireNonNull(resolvedScopes, "resolvedScopes"));
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public record Entry(String scope, String description, long durationMillis, boolean successful, String message) {

        public Entry {
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(message, "message");
        }
    }
}
