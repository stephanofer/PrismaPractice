package com.stephanofer.prismapractice.data.redis;

import com.stephanofer.prismapractice.config.ConfigBootstrapResult;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ConfigPlatforms;
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
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(classLoader, "classLoader");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(runtimeName, "runtimeName");

        ConfigManager configManager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(dataDirectory, classLoader, logger::accept),
                java.util.List.of(RedisConfigDescriptorFactory.redisDescriptor())
        );
        ConfigBootstrapResult bootstrapResult = configManager.loadAll();
        logger.accept("[redis-config] runtime=" + runtimeName + ", created=" + bootstrapResult.createdFiles()
                + ", updated=" + bootstrapResult.updatedFiles() + ", migrated=" + bootstrapResult.migratedFiles()
                + ", recovered=" + bootstrapResult.recoveredFiles() + ", warnings=" + bootstrapResult.warnings());

        RedisStorageConfig config = configManager.get("redis-storage", RedisStorageConfig.class);
        if (!config.enabled()) {
            logger.accept("[redis] runtime=" + runtimeName + ", enabled=false, status=disabled-by-config");
            return RedisStorage.disabled(runtimeName, config);
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
            connection.addListener(loggingListener(logger, runtimeName, "command"));
            connectionVerifier.verify(connection, Duration.ofMillis(config.timeouts().commandTimeoutMs()));

            RedisPubSubDispatcher dispatcher = null;
            if (config.pubSub().enabled()) {
                pubSubConnection = client.connectPubSub(StringCodec.UTF8);
                pubSubConnection.setTimeout(Duration.ofMillis(config.timeouts().commandTimeoutMs()));
                pubSubConnection.addListener(loggingListener(logger, runtimeName, "pubsub"));
                dispatcher = new RedisPubSubDispatcher(logger, pubSubConnection.async());
                pubSubConnection.addListener(dispatcher);
            }

            logger.accept("[redis] runtime=" + runtimeName
                    + ", uri=" + config.safeUri()
                    + ", client-name=" + config.effectiveClientName(runtimeName)
                    + ", pubsub-enabled=" + config.pubSub().enabled()
                    + ", io-threads=" + config.resources().ioThreadPoolSize()
                    + ", computation-threads=" + config.resources().computationThreadPoolSize());

            return RedisStorage.enabled(runtimeName, config, resources, client, connection, pubSubConnection, dispatcher);
        } catch (RuntimeException exception) {
            closeQuietly(pubSubConnection);
            closeQuietly(connection);
            shutdownQuietly(client);
            shutdownQuietly(resources);
            throw new RedisAccessException("Failed to bootstrap Redis storage for runtime '" + runtimeName + "'", exception);
        }
    }

    private RedisConnectionStateListener loggingListener(Consumer<String> logger, String runtimeName, String channelType) {
        return new RedisConnectionStateListener() {
            @Override
            public void onRedisConnected(io.lettuce.core.RedisChannelHandler<?, ?> connection, java.net.SocketAddress socketAddress) {
                logger.accept("[redis-connection] runtime=" + runtimeName + ", type=" + channelType + ", state=connected, remote=" + socketAddress);
            }

            @Override
            public void onRedisDisconnected(io.lettuce.core.RedisChannelHandler<?, ?> connection) {
                logger.accept("[redis-connection] runtime=" + runtimeName + ", type=" + channelType + ", state=disconnected");
            }

            @Override
            public void onRedisExceptionCaught(io.lettuce.core.RedisChannelHandler<?, ?> connection, Throwable cause) {
                logger.accept("[redis-connection] runtime=" + runtimeName + ", type=" + channelType + ", state=exception, message=" + cause.getMessage());
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
