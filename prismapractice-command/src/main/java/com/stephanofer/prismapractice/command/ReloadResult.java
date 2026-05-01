package com.stephanofer.prismapractice.command;

import java.util.Objects;

public record ReloadResult(String message) {

    public ReloadResult {
        Objects.requireNonNull(message, "message");
    }

    public static ReloadResult of(String message) {
        return new ReloadResult(message);
    }
}
