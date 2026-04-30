package com.stephanofer.prismapractice.data.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.resource.ClientResources;

import java.time.Duration;
import java.util.Objects;

public final class RedisClientFactory {

    public RedisClient create(ClientResources resources, RedisStorageConfig config, String runtimeName) {
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(runtimeName, "runtimeName");

        RedisClient client = RedisClient.create(resources, config.redisUri(runtimeName));
        client.setOptions(ClientOptions.builder()
                .autoReconnect(true)
                .pingBeforeActivateConnection(true)
                .protocolVersion(ProtocolVersion.RESP2)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofMillis(config.timeouts().connectTimeoutMs()))
                        .keepAlive(true)
                        .tcpNoDelay(true)
                        .build())
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(config.timeouts().commandTimeoutMs())))
                .build());
        return client;
    }
}
