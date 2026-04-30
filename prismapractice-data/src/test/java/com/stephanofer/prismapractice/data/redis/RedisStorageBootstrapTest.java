package com.stephanofer.prismapractice.data.redis;

import com.stephanofer.prismapractice.config.ConfigBootstrapResult;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ConfigPlatforms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisStorageBootstrapTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadRedisDefaultsFromSharedStorageConfig() {
        ConfigManager configManager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(tempDir, getClass().getClassLoader(), message -> {
                }),
                List.of(RedisConfigDescriptorFactory.redisDescriptor())
        );

        ConfigBootstrapResult result = configManager.loadAll();
        RedisStorageConfig config = configManager.get("redis-storage", RedisStorageConfig.class);

        assertTrue(result.createdFiles().contains("storage.yml"));
        assertTrue(config.enabled());
        assertEquals("prismapractice", config.clientName());
        assertEquals(2, config.resources().ioThreadPoolSize());
        assertEquals(45_000L, config.ttl().playerPresenceMs());
    }

    @Test
    void shouldReturnDisabledStorageWithoutConnectingWhenRedisIsDisabled() throws Exception {
        List<String> logs = new ArrayList<>();
        ConfigManager configManager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(tempDir, getClass().getClassLoader(), logs::add),
                List.of(RedisConfigDescriptorFactory.redisDescriptor())
        );
        configManager.loadAll();

        Path storageFile = tempDir.resolve("storage.yml");
        String configText = Files.readString(storageFile).replace("enabled: true", "enabled: false");
        Files.writeString(storageFile, configText);
        configManager.reloadAll();

        RedisStorage storage = new RedisStorageBootstrap().bootstrapRuntime(tempDir, getClass().getClassLoader(), logs::add, "hub");

        assertFalse(storage.enabled());
        assertFalse(storage.healthSnapshot().commandConnectionOpen());
        assertTrue(logs.stream().anyMatch(log -> log.contains("status=disabled-by-config")));
    }
}
