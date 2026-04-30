package com.stephanofer.prismapractice.data.redis;

import io.lettuce.core.RedisURI;

import java.time.Duration;
import java.util.Objects;

public record RedisStorageConfig(
        boolean enabled,
        String host,
        int port,
        int database,
        String username,
        String password,
        String clientName,
        RedisSslConfig ssl,
        RedisResourcesConfig resources,
        RedisTimeoutConfig timeouts,
        RedisReconnectConfig reconnect,
        RedisPubSubConfig pubSub,
        RedisKeyspaceConfig keyspace,
        RedisTtlConfig ttl
) {

    public RedisStorageConfig {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(clientName, "clientName");
        Objects.requireNonNull(ssl, "ssl");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(timeouts, "timeouts");
        Objects.requireNonNull(reconnect, "reconnect");
        Objects.requireNonNull(pubSub, "pubSub");
        Objects.requireNonNull(keyspace, "keyspace");
        Objects.requireNonNull(ttl, "ttl");
    }

    public RedisURI redisUri(String runtimeName) {
        RedisURI.Builder builder = RedisURI.Builder.redis(host, port)
                .withDatabase(database)
                .withClientName(effectiveClientName(runtimeName))
                .withLibraryName("prismapractice")
                .withLibraryVersion("1.0.0")
                .withSsl(ssl.enabled())
                .withVerifyPeer(ssl.verifyPeer())
                .withTimeout(Duration.ofMillis(timeouts.commandTimeoutMs()));

        if (!username.isBlank()) {
            builder.withAuthentication(username, password);
        } else if (!password.isBlank()) {
            builder.withPassword(password);
        }

        return builder.build();
    }

    public String effectiveClientName(String runtimeName) {
        Objects.requireNonNull(runtimeName, "runtimeName");
        if (clientName.isBlank()) {
            return "prismapractice-" + runtimeName;
        }
        return clientName + '-' + runtimeName;
    }

    public String safeUri() {
        String scheme = ssl.enabled() ? "rediss" : "redis";
        return scheme + "://" + host + ':' + port + '/' + database;
    }

    public record RedisSslConfig(boolean enabled, boolean verifyPeer) {
    }

    public record RedisResourcesConfig(int ioThreadPoolSize, int computationThreadPoolSize) {
    }

    public record RedisTimeoutConfig(long connectTimeoutMs, long commandTimeoutMs) {
    }

    public record RedisReconnectConfig(long initialDelayMs, long maxDelayMs) {
    }

    public record RedisPubSubConfig(boolean enabled) {
    }

    public record RedisKeyspaceConfig(String prefix, String namespace) {

        public RedisKeyspaceConfig {
            Objects.requireNonNull(prefix, "prefix");
            Objects.requireNonNull(namespace, "namespace");
        }
    }

    public record RedisTtlConfig(
            long playerPresenceMs,
            long socialCooldownMs,
            long friendRequestMs,
            long duelRequestMs,
            long partyInviteMs,
            long transitionLockMs,
            long matchmakingLockMs,
            long arenaLockMs
    ) {
    }
}
