package com.stephanofer.prismapractice.data.redis;

import java.time.Duration;
import java.util.Objects;

public final class RedisTtlPolicies {

    private final RedisStorageConfig.RedisTtlConfig config;

    public RedisTtlPolicies(RedisStorageConfig.RedisTtlConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public Duration playerPresence() {
        return Duration.ofMillis(config.playerPresenceMs());
    }

    public Duration socialCooldown() {
        return Duration.ofMillis(config.socialCooldownMs());
    }

    public Duration friendRequest() {
        return Duration.ofMillis(config.friendRequestMs());
    }

    public Duration duelRequest() {
        return Duration.ofMillis(config.duelRequestMs());
    }

    public Duration partyInvite() {
        return Duration.ofMillis(config.partyInviteMs());
    }

    public Duration transitionLock() {
        return Duration.ofMillis(config.transitionLockMs());
    }

    public Duration matchmakingLock() {
        return Duration.ofMillis(config.matchmakingLockMs());
    }

    public Duration arenaLock() {
        return Duration.ofMillis(config.arenaLockMs());
    }
}
