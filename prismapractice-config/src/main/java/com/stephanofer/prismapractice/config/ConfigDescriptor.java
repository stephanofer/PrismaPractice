package com.stephanofer.prismapractice.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ConfigDescriptor<T> {

    private final String id;
    private final String filePath;
    private final String bundledResourcePath;
    private final int schemaVersion;
    private final boolean required;
    private final Function<Map<String, Object>, T> mapper;
    private final Consumer<T> validator;
    private final List<MigrationStep> migrations;

    private ConfigDescriptor(Builder<T> builder) {
        this.id = builder.id;
        this.filePath = builder.filePath;
        this.bundledResourcePath = builder.bundledResourcePath;
        this.schemaVersion = builder.schemaVersion;
        this.required = builder.required;
        this.mapper = builder.mapper;
        this.validator = builder.validator;
        this.migrations = List.copyOf(builder.migrations);
    }

    public String id() {
        return id;
    }

    public String filePath() {
        return filePath;
    }

    public String bundledResourcePath() {
        return bundledResourcePath;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public boolean required() {
        return required;
    }

    public Function<Map<String, Object>, T> mapper() {
        return mapper;
    }

    public Consumer<T> validator() {
        return validator;
    }

    public List<MigrationStep> migrations() {
        return migrations;
    }

    public static <T> Builder<T> builder(String id, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return new Builder<>(id);
    }

    public static final class Builder<T> {

        private final String id;
        private String filePath;
        private String bundledResourcePath;
        private int schemaVersion = 1;
        private boolean required = true;
        private Function<Map<String, Object>, T> mapper = root -> null;
        private Consumer<T> validator = value -> {
        };
        private final List<MigrationStep> migrations = new ArrayList<>();

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder<T> filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder<T> bundledResourcePath(String bundledResourcePath) {
            this.bundledResourcePath = bundledResourcePath;
            return this;
        }

        public Builder<T> schemaVersion(int schemaVersion) {
            if (schemaVersion < 1) {
                throw new IllegalArgumentException("schemaVersion must be >= 1");
            }
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder<T> required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder<T> mapper(Function<Map<String, Object>, T> mapper) {
            this.mapper = Objects.requireNonNull(mapper, "mapper");
            return this;
        }

        public Builder<T> validator(Consumer<T> validator) {
            this.validator = Objects.requireNonNull(validator, "validator");
            return this;
        }

        public Builder<T> migration(int fromVersion, ConfigMigration migration) {
            this.migrations.add(new MigrationStep(fromVersion, migration));
            return this;
        }

        public ConfigDescriptor<T> build() {
            if (filePath == null || filePath.isBlank()) {
                throw new IllegalStateException("filePath is required");
            }

            if (bundledResourcePath == null || bundledResourcePath.isBlank()) {
                throw new IllegalStateException("bundledResourcePath is required");
            }

            migrations.sort((left, right) -> Integer.compare(left.fromVersion(), right.fromVersion()));
            return new ConfigDescriptor<>(this);
        }
    }

    public record MigrationStep(int fromVersion, ConfigMigration migration) {

        public MigrationStep {
            if (fromVersion < 1) {
                throw new IllegalArgumentException("fromVersion must be >= 1");
            }
            Objects.requireNonNull(migration, "migration");
        }
    }
}
