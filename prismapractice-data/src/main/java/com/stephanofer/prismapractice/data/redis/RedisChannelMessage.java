package com.stephanofer.prismapractice.data.redis;

import java.util.Objects;

public record RedisChannelMessage(String channel, String message) {

    public RedisChannelMessage {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(message, "message");
    }
}
