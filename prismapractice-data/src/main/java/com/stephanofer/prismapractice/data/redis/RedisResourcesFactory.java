package com.stephanofer.prismapractice.data.redis;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.Delay;
import io.lettuce.core.resource.DefaultClientResources;

import java.time.Duration;

public final class RedisResourcesFactory {

    public ClientResources create(RedisStorageConfig config) {
        return DefaultClientResources.builder()
                .ioThreadPoolSize(config.resources().ioThreadPoolSize())
                .computationThreadPoolSize(config.resources().computationThreadPoolSize())
                .reconnectDelay(() -> Delay.exponential(
                        Duration.ofMillis(config.reconnect().initialDelayMs()),
                        Duration.ofMillis(config.reconnect().maxDelayMs()),
                        2,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                ))
                .build();
    }
}
