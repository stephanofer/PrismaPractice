package com.stephanofer.prismapractice.data.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.resource.ClientResources;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class RedisStorage implements AutoCloseable {

    private final String runtimeName;
    private final RedisStorageConfig config;
    private final RedisKeyspace keyspace;
    private final RedisTtlPolicies ttlPolicies;
    private final RedisChannels channels;
    private final ClientResources clientResources;
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private final RedisPubSubDispatcher pubSubDispatcher;
    private volatile boolean closed;

    private RedisStorage(
            String runtimeName,
            RedisStorageConfig config,
            ClientResources clientResources,
            RedisClient client,
            StatefulRedisConnection<String, String> connection,
            StatefulRedisPubSubConnection<String, String> pubSubConnection,
            RedisPubSubDispatcher pubSubDispatcher
    ) {
        this.runtimeName = Objects.requireNonNull(runtimeName, "runtimeName");
        this.config = Objects.requireNonNull(config, "config");
        this.keyspace = new RedisKeyspace(config.keyspace());
        this.ttlPolicies = new RedisTtlPolicies(config.ttl());
        this.channels = new RedisChannels(config.keyspace());
        this.clientResources = clientResources;
        this.client = client;
        this.connection = connection;
        this.pubSubConnection = pubSubConnection;
        this.pubSubDispatcher = pubSubDispatcher;
    }

    public static RedisStorage disabled(String runtimeName, RedisStorageConfig config) {
        return new RedisStorage(runtimeName, config, null, null, null, null, null);
    }

    static RedisStorage enabled(
            String runtimeName,
            RedisStorageConfig config,
            ClientResources clientResources,
            RedisClient client,
            StatefulRedisConnection<String, String> connection,
            StatefulRedisPubSubConnection<String, String> pubSubConnection,
            RedisPubSubDispatcher pubSubDispatcher
    ) {
        return new RedisStorage(runtimeName, config, clientResources, client, connection, pubSubConnection, pubSubDispatcher);
    }

    public boolean enabled() {
        return config.enabled();
    }

    public String runtimeName() {
        return runtimeName;
    }

    public RedisStorageConfig config() {
        return config;
    }

    public RedisKeyspace keyspace() {
        return keyspace;
    }

    public RedisTtlPolicies ttlPolicies() {
        return ttlPolicies;
    }

    public RedisChannels channels() {
        return channels;
    }

    public RedisAsyncCommands<String, String> asyncCommands() {
        ensureUsable();
        return connection.async();
    }

    public RedisCommands<String, String> syncCommands() {
        ensureUsable();
        return connection.sync();
    }

    public RedisPubSubAsyncCommands<String, String> pubSubAsyncCommands() {
        ensurePubSubUsable();
        return pubSubConnection.async();
    }

    public CompletableFuture<Long> publish(String channel, String message) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(message, "message");
        ensureUsable();
        return asyncCommands().publish(channel, message).toCompletableFuture();
    }

    public CompletableFuture<Void> subscribe(String channel, Consumer<RedisChannelMessage> handler) {
        ensurePubSubUsable();
        return pubSubDispatcher.subscribe(channel, handler);
    }

    public CompletableFuture<Void> unsubscribe(String channel, Consumer<RedisChannelMessage> handler) {
        ensurePubSubUsable();
        return pubSubDispatcher.unsubscribe(channel, handler);
    }

    public RedisHealthSnapshot healthSnapshot() {
        return new RedisHealthSnapshot(
                enabled(),
                closed,
                connection != null && connection.isOpen(),
                config.pubSub().enabled(),
                pubSubConnection != null && pubSubConnection.isOpen(),
                pubSubDispatcher == null ? 0 : pubSubDispatcher.registeredChannelHandlers()
        );
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeQuietly(pubSubConnection);
        closeQuietly(connection);
        shutdownQuietly(client);
        shutdownQuietly(clientResources);
    }

    private void ensureUsable() {
        if (!enabled()) {
            throw new IllegalStateException("Redis is disabled in configuration");
        }
        if (closed || connection == null) {
            throw new IllegalStateException("Redis storage is closed");
        }
    }

    private void ensurePubSubUsable() {
        ensureUsable();
        if (!config.pubSub().enabled()) {
            throw new IllegalStateException("Redis pub/sub is disabled in configuration");
        }
        if (pubSubConnection == null || pubSubDispatcher == null) {
            throw new IllegalStateException("Redis pub/sub connection is unavailable");
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    private void shutdownQuietly(RedisClient redisClient) {
        if (redisClient == null) {
            return;
        }
        try {
            redisClient.shutdown();
        } catch (RuntimeException ignored) {
        }
    }

    private void shutdownQuietly(ClientResources resources) {
        if (resources == null) {
            return;
        }
        try {
            resources.shutdown();
        } catch (RuntimeException ignored) {
        }
    }
}
