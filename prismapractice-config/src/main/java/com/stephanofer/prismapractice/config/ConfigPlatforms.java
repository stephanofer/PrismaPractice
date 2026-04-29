package com.stephanofer.prismapractice.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class ConfigPlatforms {

    private ConfigPlatforms() {
    }

    public static ConfigPlatform fromClassLoader(Path dataDirectory, ClassLoader classLoader, ConfigLogger logger) {
        Objects.requireNonNull(classLoader, "classLoader");
        return custom(dataDirectory, path -> {
            try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
                if (inputStream == null) {
                    return Optional.empty();
                }

                return Optional.of(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }, logger);
    }

    public static ConfigPlatform custom(Path dataDirectory, ResourceReader resourceReader, ConfigLogger logger) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(resourceReader, "resourceReader");
        Objects.requireNonNull(logger, "logger");

        return new ConfigPlatform() {
            @Override
            public Path dataDirectory() {
                return dataDirectory;
            }

            @Override
            public Optional<String> readBundledResource(String path) throws IOException {
                return resourceReader.read(path);
            }

            @Override
            public ConfigLogger logger() {
                return logger;
            }
        };
    }

    @FunctionalInterface
    public interface ResourceReader {

        Optional<String> read(String path) throws IOException;
    }
}
