package com.stephanofer.prismapractice.debug;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

public interface DebugConsoleSink {

    void debug(String message);

    void info(String message);

    void warn(String message);

    void error(String message);

    static DebugConsoleSink consumer(Consumer<String> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        return new DebugConsoleSink() {
            @Override
            public void debug(String message) {
                consumer.accept(message);
            }

            @Override
            public void info(String message) {
                consumer.accept(message);
            }

            @Override
            public void warn(String message) {
                consumer.accept(message);
            }

            @Override
            public void error(String message) {
                consumer.accept(message);
            }
        };
    }

    static DebugConsoleSink jul(Logger logger) {
        Objects.requireNonNull(logger, "logger");
        return new DebugConsoleSink() {
            @Override
            public void debug(String message) {
                logger.info("[DEBUG] " + message);
            }

            @Override
            public void info(String message) {
                logger.info(message);
            }

            @Override
            public void warn(String message) {
                logger.warning(message);
            }

            @Override
            public void error(String message) {
                logger.severe(message);
            }
        };
    }

    static DebugConsoleSink noop() {
        return consumer(message -> {
        });
    }
}
