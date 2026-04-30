package com.stephanofer.prismapractice.data.redis;

import io.lettuce.core.api.StatefulRedisConnection;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class RedisConnectionVerifier {

    public void verify(StatefulRedisConnection<String, String> connection, Duration timeout) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(timeout, "timeout");
        try {
            String response = connection.async().ping().toCompletableFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!"PONG".equalsIgnoreCase(response)) {
                throw new RedisAccessException("Redis connectivity test returned unexpected response: " + response);
            }
        } catch (RedisAccessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RedisAccessException("Failed to verify Redis connectivity", exception);
        }
    }
}
