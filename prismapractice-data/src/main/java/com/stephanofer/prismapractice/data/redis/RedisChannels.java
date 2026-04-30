package com.stephanofer.prismapractice.data.redis;

import java.util.Objects;

public final class RedisChannels {

    private final RedisStorageConfig.RedisKeyspaceConfig keyspaceConfig;

    public RedisChannels(RedisStorageConfig.RedisKeyspaceConfig keyspaceConfig) {
        this.keyspaceConfig = Objects.requireNonNull(keyspaceConfig, "keyspaceConfig");
    }

    public String stateInvalidation() {
        return channel("state-invalidation");
    }

    public String presenceUpdates() {
        return channel("presence-updates");
    }

    public String transitionSignals() {
        return channel("transition-signals");
    }

    public String socialSignals() {
        return channel("social-signals");
    }

    public String cacheInvalidations() {
        return channel("cache-invalidations");
    }

    private String channel(String suffix) {
        StringBuilder builder = new StringBuilder(keyspaceConfig.prefix());
        if (!keyspaceConfig.namespace().isBlank()) {
            builder.append(':').append(keyspaceConfig.namespace());
        }
        return builder.append(':').append("channel").append(':').append(suffix).toString();
    }
}
