package com.stephanofer.prismapractice.data.redis;

import com.stephanofer.prismapractice.config.ConfigDescriptor;
import com.stephanofer.prismapractice.config.ConfigException;
import com.stephanofer.prismapractice.config.YamlConfigHelper;

import java.util.Map;

public final class RedisConfigDescriptorFactory {

    private RedisConfigDescriptorFactory() {
    }

    public static ConfigDescriptor<RedisStorageConfig> redisDescriptor() {
        return ConfigDescriptor.builder("redis-storage", RedisStorageConfig.class)
                .filePath("storage.yml")
                .bundledResourcePath("defaults/storage.yml")
                .schemaVersion(2)
                .migration(1, root -> {
                })
                .mapper(root -> {
                    Map<String, Object> redis = YamlConfigHelper.section(root, "redis");
                    Map<String, Object> ssl = YamlConfigHelper.section(redis, "ssl");
                    Map<String, Object> resources = YamlConfigHelper.section(redis, "resources");
                    Map<String, Object> timeouts = YamlConfigHelper.section(redis, "timeouts");
                    Map<String, Object> reconnect = YamlConfigHelper.section(redis, "reconnect");
                    Map<String, Object> pubSub = YamlConfigHelper.section(redis, "pubsub");
                    Map<String, Object> keyspace = YamlConfigHelper.section(redis, "keyspace");
                    Map<String, Object> ttl = YamlConfigHelper.section(redis, "ttl");

                    return new RedisStorageConfig(
                            YamlConfigHelper.bool(redis, "enabled"),
                            YamlConfigHelper.string(redis, "host"),
                            YamlConfigHelper.integer(redis, "port"),
                            YamlConfigHelper.integer(redis, "database"),
                            YamlConfigHelper.string(redis, "username"),
                            YamlConfigHelper.string(redis, "password"),
                            YamlConfigHelper.string(redis, "client-name"),
                            new RedisStorageConfig.RedisSslConfig(
                                    YamlConfigHelper.bool(ssl, "enabled"),
                                    YamlConfigHelper.bool(ssl, "verify-peer")
                            ),
                            new RedisStorageConfig.RedisResourcesConfig(
                                    YamlConfigHelper.integer(resources, "io-thread-pool-size"),
                                    YamlConfigHelper.integer(resources, "computation-thread-pool-size")
                            ),
                            new RedisStorageConfig.RedisTimeoutConfig(
                                    longValue(timeouts, "connect-timeout-ms"),
                                    longValue(timeouts, "command-timeout-ms")
                            ),
                            new RedisStorageConfig.RedisReconnectConfig(
                                    longValue(reconnect, "initial-delay-ms"),
                                    longValue(reconnect, "max-delay-ms")
                            ),
                            new RedisStorageConfig.RedisPubSubConfig(YamlConfigHelper.bool(pubSub, "enabled")),
                            new RedisStorageConfig.RedisKeyspaceConfig(
                                    YamlConfigHelper.string(keyspace, "prefix"),
                                    YamlConfigHelper.string(keyspace, "namespace")
                            ),
                            new RedisStorageConfig.RedisTtlConfig(
                                    longValue(ttl, "player-presence-ms"),
                                    longValue(ttl, "social-cooldown-ms"),
                                    longValue(ttl, "friend-request-ms"),
                                    longValue(ttl, "duel-request-ms"),
                                    longValue(ttl, "party-invite-ms"),
                                    longValue(ttl, "transition-lock-ms"),
                                    longValue(ttl, "matchmaking-lock-ms"),
                                    longValue(ttl, "arena-lock-ms")
                            )
                    );
                })
                .validator(RedisConfigDescriptorFactory::validate)
                .build();
    }

    private static void validate(RedisStorageConfig config) {
        requireNotBlank(config.host(), "redis.host");
        requireRange(config.port(), 1, 65_535, "redis.port");
        requireRange(config.database(), 0, 32, "redis.database");
        requireRange(config.resources().ioThreadPoolSize(), 1, 8, "redis.resources.io-thread-pool-size");
        requireRange(config.resources().computationThreadPoolSize(), 1, 8, "redis.resources.computation-thread-pool-size");
        requireMinimum(config.timeouts().connectTimeoutMs(), 100L, "redis.timeouts.connect-timeout-ms");
        requireMinimum(config.timeouts().commandTimeoutMs(), 100L, "redis.timeouts.command-timeout-ms");
        requireMinimum(config.reconnect().initialDelayMs(), 50L, "redis.reconnect.initial-delay-ms");
        requireMinimum(config.reconnect().maxDelayMs(), config.reconnect().initialDelayMs(), "redis.reconnect.max-delay-ms");
        requireNotBlank(config.keyspace().prefix(), "redis.keyspace.prefix");
        requireNoColon(config.keyspace().prefix(), "redis.keyspace.prefix");
        if (!config.keyspace().namespace().isBlank()) {
            requireNoColon(config.keyspace().namespace(), "redis.keyspace.namespace");
        }
        requireMinimum(config.ttl().playerPresenceMs(), 1_000L, "redis.ttl.player-presence-ms");
        requireMinimum(config.ttl().socialCooldownMs(), 250L, "redis.ttl.social-cooldown-ms");
        requireMinimum(config.ttl().friendRequestMs(), 1_000L, "redis.ttl.friend-request-ms");
        requireMinimum(config.ttl().duelRequestMs(), 1_000L, "redis.ttl.duel-request-ms");
        requireMinimum(config.ttl().partyInviteMs(), 1_000L, "redis.ttl.party-invite-ms");
        requireMinimum(config.ttl().transitionLockMs(), 250L, "redis.ttl.transition-lock-ms");
        requireMinimum(config.ttl().matchmakingLockMs(), 250L, "redis.ttl.matchmaking-lock-ms");
        requireMinimum(config.ttl().arenaLockMs(), 250L, "redis.ttl.arena-lock-ms");
    }

    private static long longValue(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new ConfigException("Expected integer at key '" + key + "'");
    }

    private static void requireNotBlank(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new ConfigException(key + " must not be blank");
        }
    }

    private static void requireNoColon(String value, String key) {
        if (value.contains(":")) {
            throw new ConfigException(key + " must not contain ':'");
        }
    }

    private static void requireRange(long value, long minimum, long maximum, String key) {
        if (value < minimum || value > maximum) {
            throw new ConfigException(key + " must be between " + minimum + " and " + maximum);
        }
    }

    private static void requireMinimum(long value, long minimum, String key) {
        if (value < minimum) {
            throw new ConfigException(key + " must be >= " + minimum);
        }
    }
}
