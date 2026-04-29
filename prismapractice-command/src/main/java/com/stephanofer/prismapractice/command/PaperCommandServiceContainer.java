package com.stephanofer.prismapractice.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PaperCommandServiceContainer {

    private final Map<Class<?>, Object> services;

    private PaperCommandServiceContainer(final Map<Class<?>, Object> services) {
        this.services = Map.copyOf(services);
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> Optional<T> find(final Class<T> type) {
        Objects.requireNonNull(type, "type");
        return Optional.ofNullable(this.services.get(type)).map(type::cast);
    }

    public <T> T require(final Class<T> type) {
        return this.find(type)
            .orElseThrow(() -> new IllegalStateException("Missing command service: " + type.getName()));
    }

    public static final class Builder {

        private final Map<Class<?>, Object> services = new HashMap<>();

        private Builder() {
        }

        public <T> Builder add(final Class<T> type, final T instance) {
            this.services.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(instance, "instance"));
            return this;
        }

        public PaperCommandServiceContainer build() {
            return new PaperCommandServiceContainer(this.services);
        }
    }
}
