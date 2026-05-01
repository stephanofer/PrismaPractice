package com.stephanofer.prismapractice.data.redis;

import com.stephanofer.prismapractice.config.ConfigBootstrapResult;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ConfigPlatforms;
import com.stephanofer.prismapractice.debug.DebugCategories;
import com.stephanofer.prismapractice.debug.DebugController;
import com.stephanofer.prismapractice.debug.DebugDetailLevel;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

public final class RedisStorageBootstrap {

    private final RedisResourcesFactory resourcesFactory;
    private final RedisClientFactory clientFactory;
    private final RedisConnectionVerifier connectionVerifier;

    public RedisStorageBootstrap() {
        this(new RedisResourcesFactory(), new RedisClientFactory(), new RedisConnectionVerifier());
    }

    RedisStorageBootstrap(RedisResourcesFactory resourcesFactory, RedisClientFactory clientFactory, RedisConnectionVerifier connectionVerifier) {
        this.resourcesFactory = Objects.requireNonNull(resourcesFactory, "resourcesFactory");
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory");
        this.connectionVerifier = Objects.requireNonNull(connectionVerifier, "connectionVerifier");
    }

    public RedisStorage bootstrapRuntime(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger, String runtimeName) {
        return bootstrapRuntime(dataDirectory, classLoader, logger, runtimeName, DebugController.noop());
    }

    public RedisStorage bootstrapRuntime(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger, String runtimeName, DebugController debug) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(classLoader, "classLoader");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(runtimeName, "runtimeName");
        Objects.requireNonNull(debug, "debug");

        ConfigManager configManager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(dataDirectory, classLoader, logger::accept),
                java.util.List.of(RedisConfigDescriptorFactory.redisDescriptor())
        );
        ConfigBootstrapResult bootstrapResult = configManager.loadAll();
        debug.info(
                DebugCategories.STORAGE_REDIS,
                DebugDetailLevel.BASIC,
                "redis.config.loaded",
                "Redis configuration loaded",
                debug.context()
                        .field("created", bootstrapResult.createdFiles())
                        .field("updated", bootstrapResult.updatedFiles())
                        .field("migrated", bootstrapResult.migratedFiles())
                        .field("recovered", bootstrapResult.recoveredFiles())
                        .field("warnings", bootstrapResult.warnings())
                        .build()
        );

        RedisStorageConfig config = configManager.get("redis-storage", RedisStorageConfig.class);
        if (!config.enabled()) {
            debug.info(DebugCategories.STORAGE_REDIS, DebugDetailLevel.BASIC, "redis.disabled", "Redis disabled by configuration", debug.context().build());
            return RedisStorage.disabled(runtimeName, config, debug);
        }

        ClientResources resources = null;
        RedisClient client = null;
        StatefulRedisConnection<String, String> connection = null;
        StatefulRedisPubSubConnection<String, String> pubSubConnection = null;
        try {
            resources = resourcesFactory.create(config);
            client = clientFactory.create(resources, config, runtimeName);
            connection = client.connect(StringCodec.UTF8);
            connection.setTimeout(Duration.ofMillis(config.timeouts().commandTimeoutMs()));
            connection.addListener(loggingListener(debug, runtimeName, "command"));
            connectionVerifier.verify(connection, Duration.ofMillis(config.timeouts().commandTimeoutMs()));

            RedisPubSubDispatcher dispatcher = null;
            if (config.pubSub().enabled()) {
                pubSubConnection = client.connectPubSub(StringCodec.UTF8);
                pubSubConnection.setTimeout(Duration.ofMillis(config.timeouts().commandTimeoutMs()));
                pubSubConnection.addListener(loggingListener(debug, runtimeName, "pubsub"));
                dispatcher = new RedisPubSubDispatcher(logger, pubSubConnection.async());
                pubSubConnection.addListener(dispatcher);
            }

            debug.info(
                    DebugCategories.STORAGE_REDIS,
                    DebugDetailLevel.BASIC,
                    "redis.bootstrap.completed",
                    "Redis storage bootstrapped",
                    debug.context()
                            .field("uri", config.safeUri())
                            .field("clientName", config.effectiveClientName(runtimeName))
                            .field("pubSubEnabled", config.pubSub().enabled())
                            .field("ioThreads", config.resources().ioThreadPoolSize())
                            .field("computationThreads", config.resources().computationThreadPoolSize())
                            .build()
            );

            return RedisStorage.enabled(runtimeName, config, debug, resources, client, connection, pubSubConnection, dispatcher);
        } catch (RuntimeException exception) {
            closeQuietly(pubSubConnection);
            closeQuietly(connection);
            shutdownQuietly(client);
            shutdownQuietly(resources);
            debug.error(DebugCategories.STORAGE_REDIS, "redis.bootstrap.failed", "Failed to bootstrap Redis storage", debug.context().build(), exception);
            throw new RedisAccessException("Failed to bootstrap Redis storage for runtime '" + runtimeName + "'", exception);
        }
    }

    private RedisConnectionStateListener loggingListener(DebugController debug, String runtimeName, String channelType) {
        return new RedisConnectionStateListener() {
            @Override
            public void onRedisConnected(io.lettuce.core.RedisChannelHandler<?, ?> connection, java.net.SocketAddress socketAddress) {
                debug.info(DebugCategories.REDIS_CONNECTION, DebugDetailLevel.BASIC, "redis.connection.connected", "Redis connection established", debug.context().field("type", channelType).field("remote", socketAddress).field("runtime", runtimeName).build());
            }

            @Override
            public void onRedisDisconnected(io.lettuce.core.RedisChannelHandler<?, ?> connection) {
                debug.warn(DebugCategories.REDIS_CONNECTION, "redis.connection.disconnected", "Redis connection disconnected", debug.context().field("type", channelType).field("runtime", runtimeName).build());
            }

            @Override
            public void onRedisExceptionCaught(io.lettuce.core.RedisChannelHandler<?, ?> connection, Throwable cause) {
                debug.error(DebugCategories.REDIS_CONNECTION, "redis.connection.exception", "Redis connection reported an exception", debug.context().field("type", channelType).field("runtime", runtimeName).build(), cause);
            }
        };
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

    private void shutdownQuietly(RedisClient client) {
        if (client == null) {
            return;
        }
        try {
            client.shutdown();
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
