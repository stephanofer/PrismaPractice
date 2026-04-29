package com.stephanofer.prismapractice.config;

@FunctionalInterface
public interface ConfigLogger {

    void info(String message);

    default void warn(String message) {
        info("[WARN] " + message);
    }

    default void error(String message) {
        info("[ERROR] " + message);
    }

    default void error(String message, Throwable throwable) {
        error(message + " (" + throwable.getClass().getSimpleName() + ": " + throwable.getMessage() + ")");
    }
}
