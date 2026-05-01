package com.stephanofer.prismapractice.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ConfigManager {

    private static final String SCHEMA_VERSION_KEY = "config-schema-version";

    private final ConfigPlatform platform;
    private final Map<String, ConfigDescriptor<?>> descriptors;
    private final Map<String, ManagedConfig<?>> cachedConfigs;
    private final Yaml yamlReader;
    private final Yaml yamlWriter;

    public ConfigManager(ConfigPlatform platform, Collection<ConfigDescriptor<?>> descriptors) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.descriptors = indexDescriptors(descriptors);
        this.cachedConfigs = new ConcurrentHashMap<>();
        this.yamlReader = new Yaml(new SafeConstructor(new LoaderOptions()));

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(2);
        dumperOptions.setIndicatorIndent(1);
        dumperOptions.setProcessComments(false);
        dumperOptions.setWidth(160);
        this.yamlWriter = new Yaml(dumperOptions);
    }

    public ConfigBootstrapResult loadAll() {
        ConfigBootstrapResult result = new ConfigBootstrapResult();
        cachedConfigs.putAll(loadManagedConfigs(result));
        return result;
    }

    public ConfigBootstrapResult reloadAll() {
        ConfigBootstrapResult result = new ConfigBootstrapResult();
        Map<String, ManagedConfig<?>> reloadedConfigs = loadManagedConfigs(result);
        cachedConfigs.clear();
        cachedConfigs.putAll(reloadedConfigs);
        return result;
    }

    public <T> T get(String id, Class<T> type) {
        ManagedConfig<?> config = cachedConfigs.get(id);
        if (config == null) {
            throw new ConfigException("Configuration '" + id + "' is not loaded");
        }

        if (!type.isInstance(config.value())) {
            throw new ConfigException("Configuration '" + id + "' is not of type " + type.getName());
        }

        return type.cast(config.value());
    }

    public Optional<ManagedConfig<?>> findManaged(String id) {
        return Optional.ofNullable(cachedConfigs.get(id));
    }

    private <T> void loadDescriptor(ConfigDescriptor<T> descriptor, ConfigBootstrapResult result) {
        try {
            ManagedConfig<T> managedConfig = loadManaged(descriptor, result);
            cachedConfigs.put(descriptor.id(), managedConfig);
        } catch (IOException exception) {
            throw new ConfigException("Failed to load configuration '" + descriptor.id() + "'", exception);
        }
    }

    private Map<String, ManagedConfig<?>> loadManagedConfigs(ConfigBootstrapResult result) {
        Map<String, ManagedConfig<?>> loaded = new LinkedHashMap<>();
        for (ConfigDescriptor<?> descriptor : descriptors.values()) {
            loaded.put(descriptor.id(), loadManagedUnchecked(descriptor, result));
        }
        return loaded;
    }

    private ManagedConfig<?> loadManagedUnchecked(ConfigDescriptor<?> descriptor, ConfigBootstrapResult result) {
        try {
            return loadManaged(descriptor, result);
        } catch (IOException exception) {
            throw new ConfigException("Failed to load configuration '" + descriptor.id() + "'", exception);
        }
    }

    private <T> ManagedConfig<T> loadManaged(ConfigDescriptor<T> descriptor, ConfigBootstrapResult result) throws IOException {
        Path file = platform.dataDirectory().resolve(descriptor.filePath()).normalize();
        Path rootDirectory = platform.dataDirectory().normalize();
        if (!file.startsWith(rootDirectory)) {
            throw new ConfigException("Config path escapes data directory: " + descriptor.filePath());
        }

        Files.createDirectories(rootDirectory);
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }

        Map<String, Object> defaults = loadDefaults(descriptor);
        boolean changed = false;
        boolean migrated = false;

        Map<String, Object> currentRoot;
        if (Files.notExists(file)) {
            currentRoot = deepCopyMap(defaults);
            changed = true;
            result.created(descriptor.filePath());
            platform.logger().info("Created missing config '" + descriptor.filePath() + "'.");
        } else {
            try {
                currentRoot = readYamlFile(file);
            } catch (RuntimeException exception) {
                Path backup = backupBrokenFile(file);
                result.recovered(descriptor.filePath());
                result.warning("Recovered broken config '" + descriptor.filePath() + "' into '" + backup.getFileName() + "'.");
                platform.logger().warn("Recovered broken config '" + descriptor.filePath() + "' into '" + backup.getFileName() + "'.");
                currentRoot = deepCopyMap(defaults);
                changed = true;
            }
        }

        int beforeVersion = readSchemaVersion(currentRoot);
        if (beforeVersion > descriptor.schemaVersion()) {
            result.warning("Config '" + descriptor.filePath() + "' uses newer schema v" + beforeVersion + " than this binary supports (v" + descriptor.schemaVersion() + ").");
            platform.logger().warn("Config '" + descriptor.filePath() + "' uses newer schema v" + beforeVersion + " than this binary supports (v" + descriptor.schemaVersion() + ").");
        }

        int resolvedVersion = beforeVersion;
        if (beforeVersion < descriptor.schemaVersion()) {
            resolvedVersion = applyMigrations(descriptor, currentRoot, beforeVersion, descriptor.schemaVersion());
            migrated = resolvedVersion != beforeVersion;
        }

        MergeStats mergeStats = new MergeStats();
        if (mergeMissing(defaults, currentRoot, mergeStats)) {
            changed = true;
        }

        if (resolvedVersion < descriptor.schemaVersion()) {
            throw new ConfigException("Missing migration path for config '" + descriptor.id() + "' from schema v" + beforeVersion + " to v" + descriptor.schemaVersion());
        }

        if (beforeVersion < descriptor.schemaVersion()) {
            currentRoot.put(SCHEMA_VERSION_KEY, descriptor.schemaVersion());
            changed = true;
        }

        if (migrated) {
            result.migrated(descriptor.filePath());
            platform.logger().info("Migrated config '" + descriptor.filePath() + "' to schema v" + descriptor.schemaVersion() + ".");
        }

        if (mergeStats.addedPaths > 0) {
            result.updated(descriptor.filePath());
            platform.logger().info("Updated config '" + descriptor.filePath() + "' with " + mergeStats.addedPaths + " missing key(s).");
        }

        if (changed) {
            writeYamlFile(file, currentRoot);
        }

        T value = descriptor.mapper().apply(deepCopyMap(currentRoot));
        descriptor.validator().accept(value);
        return new ManagedConfig<>(descriptor, file, deepCopyMap(currentRoot), value);
    }

    private Map<String, Object> loadDefaults(ConfigDescriptor<?> descriptor) throws IOException {
        Optional<String> bundledText = platform.readBundledResource(descriptor.bundledResourcePath());
        if (bundledText.isEmpty()) {
            if (descriptor.required()) {
                throw new ConfigException("Missing bundled resource '" + descriptor.bundledResourcePath() + "' for config '" + descriptor.id() + "'");
            }
            return new LinkedHashMap<>();
        }

        return readYamlString(bundledText.get(), descriptor.bundledResourcePath());
    }

    private int applyMigrations(ConfigDescriptor<?> descriptor, Map<String, Object> root, int currentVersion, int targetVersion) {
        int version = currentVersion;
        while (version < targetVersion) {
            boolean applied = false;
            for (ConfigDescriptor.MigrationStep migration : descriptor.migrations()) {
                if (migration.fromVersion() == version) {
                    migration.migration().migrate(root);
                    version++;
                    applied = true;
                    break;
                }
            }

            if (!applied) {
                break;
            }
        }

        return version;
    }

    private Map<String, Object> readYamlFile(Path file) throws IOException {
        return readYamlString(Files.readString(file, StandardCharsets.UTF_8), file.toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readYamlString(String yamlText, String source) {
        Object loaded;
        try {
            loaded = yamlReader.load(yamlText);
        } catch (Exception exception) {
            throw new ConfigException("Invalid YAML in '" + source + "'", exception);
        }

        if (loaded == null) {
            return new LinkedHashMap<>();
        }

        if (!(loaded instanceof Map<?, ?> loadedMap)) {
            throw new ConfigException("Root node of '" + source + "' must be a map/object");
        }

        return deepCopyMap((Map<String, Object>) loadedMap);
    }

    private void writeYamlFile(Path file, Map<String, Object> root) throws IOException {
        String dumped = yamlWriter.dump(root);
        Files.writeString(file, dumped, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private Path backupBrokenFile(Path file) throws IOException {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(Instant.now().atZone(java.time.ZoneOffset.UTC));
        Path backup = file.resolveSibling(file.getFileName() + ".broken-" + timestamp);
        Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
        return backup;
    }

    @SuppressWarnings("unchecked")
    private boolean mergeMissing(Map<String, Object> defaults, Map<String, Object> target, MergeStats stats) {
        boolean changed = false;
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();

            if (!target.containsKey(key)) {
                target.put(key, deepCopy(defaultValue));
                stats.addedPaths++;
                changed = true;
                continue;
            }

            Object currentValue = target.get(key);
            if (defaultValue instanceof Map<?, ?> defaultMap && currentValue instanceof Map<?, ?> currentMap) {
                changed |= mergeMissing((Map<String, Object>) defaultMap, (Map<String, Object>) currentMap, stats);
            }
        }
        return changed;
    }

    private int readSchemaVersion(Map<String, Object> root) {
        Object rawValue = root.get(SCHEMA_VERSION_KEY);
        if (rawValue instanceof Number number) {
            return number.intValue();
        }
        if (rawValue instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepCopyMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), deepCopy(entry.getValue()));
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Object deepCopy(Object value) {
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), deepCopy(entry.getValue()));
            }
            return copy;
        }

        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object element : list) {
                copy.add(deepCopy(element));
            }
            return copy;
        }

        return value;
    }

    private static Map<String, ConfigDescriptor<?>> indexDescriptors(Collection<ConfigDescriptor<?>> descriptors) {
        Objects.requireNonNull(descriptors, "descriptors");
        Map<String, ConfigDescriptor<?>> indexed = new LinkedHashMap<>();
        Deque<String> filePaths = new ArrayDeque<>();
        for (ConfigDescriptor<?> descriptor : descriptors) {
            Objects.requireNonNull(descriptor, "descriptor");
            if (indexed.putIfAbsent(descriptor.id(), descriptor) != null) {
                throw new IllegalArgumentException("Duplicate config id: " + descriptor.id());
            }
            if (filePaths.contains(descriptor.filePath())) {
                throw new IllegalArgumentException("Duplicate config path: " + descriptor.filePath());
            }
            filePaths.add(descriptor.filePath());
        }
        return Map.copyOf(indexed);
    }

    private static final class MergeStats {
        private int addedPaths;
    }
}
