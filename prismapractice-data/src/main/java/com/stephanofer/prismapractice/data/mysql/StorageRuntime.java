package com.stephanofer.prismapractice.data.mysql;

import com.stephanofer.prismapractice.config.ConfigManager;

import java.util.Objects;

public record StorageRuntime(ConfigManager configManager, MySqlStorage storage) {

    public StorageRuntime {
        Objects.requireNonNull(configManager, "configManager");
        Objects.requireNonNull(storage, "storage");
    }
}
