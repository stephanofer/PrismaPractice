package com.stephanofer.prismapractice.data.mysql;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.data.redis.RedisStorage;
import com.stephanofer.prismapractice.debug.DebugController;

import java.util.Objects;

public record StorageRuntime(ConfigManager configManager, MySqlStorage storage, RedisStorage redisStorage, DebugController debugController) {

    public StorageRuntime {
        Objects.requireNonNull(configManager, "configManager");
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(redisStorage, "redisStorage");
        Objects.requireNonNull(debugController, "debugController");
    }
}
