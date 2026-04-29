package com.stephanofer.prismapractice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateMissingConfigFromBundledDefaults() throws IOException {
        List<String> logs = new ArrayList<>();
        ConfigManager manager = manager(logs);

        ConfigBootstrapResult result = manager.loadAll();

        PracticeSettings settings = manager.get("main", PracticeSettings.class);
        assertEquals("PrismaPractice", settings.serverName());
        assertEquals(150, settings.queueLimit());
        assertTrue(Files.exists(tempDir.resolve("config.yml")));
        assertEquals(List.of("config.yml"), result.createdFiles());
        assertFalse(logs.isEmpty());
    }

    @Test
    void shouldMergeMissingKeysWithoutOverwritingExistingValues() throws IOException {
        Files.writeString(tempDir.resolve("config.yml"), "config-schema-version: 2\nserver-name: CustomName\nqueue:\n  max-entries: 30\nlegacy-section:\n  enabled: true\n");

        ConfigManager manager = manager(new ArrayList<>());
        manager.loadAll();

        String updated = Files.readString(tempDir.resolve("config.yml"));
        PracticeSettings settings = manager.get("main", PracticeSettings.class);
        assertEquals("CustomName", settings.serverName());
        assertEquals(30, settings.queueLimit());
        assertTrue(updated.contains("legacy-section"));
        assertTrue(updated.contains("motd: Fight hard"));
        assertTrue(updated.contains("announce-joins: true"));
    }

    @Test
    void shouldApplyMigrationsBeforeValidation() throws IOException {
        Files.writeString(tempDir.resolve("config.yml"), "config-schema-version: 1\nserver-name: OldName\nqueue:\n  max-players: 42\nmessages:\n  motd: Legacy\n");

        ConfigManager manager = manager(new ArrayList<>());
        ConfigBootstrapResult result = manager.loadAll();

        PracticeSettings settings = manager.get("main", PracticeSettings.class);
        String updated = Files.readString(tempDir.resolve("config.yml"));
        assertEquals(42, settings.queueLimit());
        assertEquals(List.of("config.yml"), result.migratedFiles());
        assertTrue(updated.contains("max-entries: 42"));
        assertFalse(updated.contains("max-players"));
        assertTrue(updated.contains("config-schema-version: 2"));
    }

    @Test
    void shouldRecoverBrokenYamlWithoutDestroyingOriginalFile() throws IOException {
        Files.writeString(tempDir.resolve("config.yml"), "queue: [broken\n");

        ConfigManager manager = manager(new ArrayList<>());
        ConfigBootstrapResult result = manager.loadAll();

        assertEquals(List.of("config.yml"), result.recoveredFiles());
        assertTrue(Files.exists(tempDir.resolve("config.yml")));
        try (Stream<Path> files = Files.list(tempDir)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().contains("config.yml.broken-")));
        }
        PracticeSettings settings = manager.get("main", PracticeSettings.class);
        assertEquals(150, settings.queueLimit());
    }

    private ConfigManager manager(List<String> logs) {
        ConfigPlatform platform = ConfigPlatforms.fromClassLoader(tempDir, getClass().getClassLoader(), logs::add);
        ConfigDescriptor<PracticeSettings> descriptor = ConfigDescriptor.builder("main", PracticeSettings.class)
                .filePath("config.yml")
                .bundledResourcePath("defaults/config.yml")
                .schemaVersion(2)
                .migration(1, root -> YamlConfigHelper.move(root, "queue.max-players", "queue.max-entries"))
                .mapper(root -> new PracticeSettings(
                        YamlConfigHelper.string(root, "server-name"),
                        YamlConfigHelper.integer(YamlConfigHelper.section(root, "queue"), "max-entries"),
                        YamlConfigHelper.string(YamlConfigHelper.section(root, "messages"), "motd")
                ))
                .validator(settings -> {
                    if (settings.queueLimit() <= 0) {
                        throw new ConfigException("queue.max-entries must be positive");
                    }
                })
                .build();

        return new ConfigManager(platform, List.of(descriptor));
    }

    private record PracticeSettings(String serverName, int queueLimit, String motd) {
    }
}
