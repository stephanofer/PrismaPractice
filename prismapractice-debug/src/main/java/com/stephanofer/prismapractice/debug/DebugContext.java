package com.stephanofer.prismapractice.debug;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DebugContext {

    private static final DebugContext EMPTY = new DebugContext(Map.of());

    private final Map<String, Object> fields;

    private DebugContext(Map<String, Object> fields) {
        this.fields = Map.copyOf(fields);
    }

    public static DebugContext empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> fields() {
        return fields;
    }

    public Object get(String key) {
        return fields.get(key);
    }

    public DebugContext merge(DebugContext other) {
        Objects.requireNonNull(other, "other");
        if (other.fields.isEmpty()) {
            return this;
        }
        if (this.fields.isEmpty()) {
            return other;
        }
        Map<String, Object> merged = new LinkedHashMap<>(this.fields);
        merged.putAll(other.fields);
        return new DebugContext(merged);
    }

    public static final class Builder {

        private final Map<String, Object> fields = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder runtime(String runtime) {
            return field("runtime", runtime);
        }

        public Builder server(String serverId) {
            return field("server", serverId);
        }

        public Builder player(String playerId, String playerName) {
            field("playerId", playerId);
            return field("playerName", playerName);
        }

        public Builder matchId(String matchId) {
            return field("matchId", matchId);
        }

        public Builder queueId(String queueId) {
            return field("queueId", queueId);
        }

        public Builder arenaId(String arenaId) {
            return field("arenaId", arenaId);
        }

        public Builder traceId(String traceId) {
            return field("traceId", traceId);
        }

        public Builder command(String command) {
            return field("command", command);
        }

        public Builder permission(String permission) {
            return field("permission", permission);
        }

        public Builder state(String state) {
            return field("state", state);
        }

        public Builder field(String key, Object value) {
            if (key == null || key.isBlank() || value == null) {
                return this;
            }
            fields.put(key, value);
            return this;
        }

        public DebugContext build() {
            return fields.isEmpty() ? EMPTY : new DebugContext(fields);
        }
    }
}
