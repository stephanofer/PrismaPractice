package com.stephanofer.prismapractice.data.redis;

public record RedisHealthSnapshot(
        boolean enabled,
        boolean closed,
        boolean commandConnectionOpen,
        boolean pubSubEnabled,
        boolean pubSubConnectionOpen,
        int registeredChannelHandlers
) {
}
