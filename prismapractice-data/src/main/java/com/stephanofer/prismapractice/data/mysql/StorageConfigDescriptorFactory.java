package com.stephanofer.prismapractice.data.mysql;

import com.stephanofer.prismapractice.config.ConfigDescriptor;
import com.stephanofer.prismapractice.config.ConfigException;
import com.stephanofer.prismapractice.config.YamlConfigHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StorageConfigDescriptorFactory {

    private StorageConfigDescriptorFactory() {
    }

    public static ConfigDescriptor<MySqlStorageConfig> storageDescriptor() {
        return ConfigDescriptor.builder("storage", MySqlStorageConfig.class)
                .filePath("storage.yml")
                .bundledResourcePath("defaults/storage.yml")
                .schemaVersion(2)
                .migration(1, root -> {
                })
                .mapper(root -> {
                    Map<String, Object> mysql = YamlConfigHelper.section(root, "mysql");
                    Map<String, Object> pool = YamlConfigHelper.section(root, "pool");
                    Map<String, Object> migrations = YamlConfigHelper.section(root, "migrations");
                    Map<String, Object> startup = YamlConfigHelper.section(root, "startup");

                    return new MySqlStorageConfig(
                            YamlConfigHelper.string(mysql, "host"),
                            YamlConfigHelper.integer(mysql, "port"),
                            YamlConfigHelper.string(mysql, "database"),
                            YamlConfigHelper.string(mysql, "username"),
                            YamlConfigHelper.string(mysql, "password"),
                            stringMap(YamlConfigHelper.section(mysql, "parameters")),
                            new MySqlStorageConfig.MySqlPoolConfig(
                                    YamlConfigHelper.integer(pool, "maximum-pool-size"),
                                    YamlConfigHelper.integer(pool, "minimum-idle"),
                                    longValue(pool, "connection-timeout-ms"),
                                    longValue(pool, "validation-timeout-ms"),
                                    longValue(pool, "idle-timeout-ms"),
                                    longValue(pool, "max-lifetime-ms"),
                                    longValue(pool, "keepalive-time-ms"),
                                    longValue(pool, "leak-detection-threshold-ms")
                            ),
                            new MySqlStorageConfig.MySqlMigrationConfig(
                                    stringList(migrations, "locations"),
                                    YamlConfigHelper.string(migrations, "table"),
                                    YamlConfigHelper.bool(migrations, "baseline-on-migrate"),
                                    YamlConfigHelper.bool(migrations, "validate-on-migrate"),
                                    YamlConfigHelper.bool(migrations, "clean-disabled"),
                                    YamlConfigHelper.bool(migrations, "fail-on-missing-locations")
                            ),
                            new MySqlStorageConfig.MySqlStartupConfig(YamlConfigHelper.string(startup, "test-query"))
                    );
                })
                .validator(StorageConfigDescriptorFactory::validate)
                .build();
    }

    private static void validate(MySqlStorageConfig config) {
        requireNotBlank(config.host(), "mysql.host");
        requireRange(config.port(), 1, 65_535, "mysql.port");
        requireNotBlank(config.database(), "mysql.database");
        requireNotBlank(config.username(), "mysql.username");
        if (config.password() == null) {
            throw new ConfigException("mysql.password must not be null");
        }
        requireRange(config.pool().maximumPoolSize(), 1, 64, "pool.maximum-pool-size");
        requireRange(config.pool().minimumIdle(), 0, config.pool().maximumPoolSize(), "pool.minimum-idle");
        requireMinimum(config.pool().connectionTimeoutMs(), 250L, "pool.connection-timeout-ms");
        requireMinimum(config.pool().validationTimeoutMs(), 250L, "pool.validation-timeout-ms");
        requireMinimum(config.pool().idleTimeoutMs(), 10_000L, "pool.idle-timeout-ms");
        requireMinimum(config.pool().maxLifetimeMs(), 30_000L, "pool.max-lifetime-ms");
        requireMinimum(config.pool().keepaliveTimeMs(), 30_000L, "pool.keepalive-time-ms");
        if (config.pool().maxLifetimeMs() <= config.pool().keepaliveTimeMs()) {
            throw new ConfigException("pool.max-lifetime-ms must be greater than pool.keepalive-time-ms");
        }
        if (config.pool().leakDetectionThresholdMs() != 0L && config.pool().leakDetectionThresholdMs() < 2_000L) {
            throw new ConfigException("pool.leak-detection-threshold-ms must be 0 or >= 2000");
        }
        if (config.migrations().locations().isEmpty()) {
            throw new ConfigException("migrations.locations must not be empty");
        }
        requireNotBlank(config.migrations().table(), "migrations.table");
        if (!config.migrations().cleanDisabled()) {
            throw new ConfigException("migrations.clean-disabled must remain true for safety");
        }
        requireNotBlank(config.startup().testQuery(), "startup.test-query");
    }

    private static Map<String, String> stringMap(Map<String, Object> section) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            values.put(entry.getKey(), Objects.toString(entry.getValue(), ""));
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (!(value instanceof List<?> list)) {
            throw new ConfigException("Expected list at key '" + key + "'");
        }
        for (Object element : list) {
            if (!(element instanceof String)) {
                throw new ConfigException("Expected string list at key '" + key + "'");
            }
        }
        return (List<String>) list;
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
