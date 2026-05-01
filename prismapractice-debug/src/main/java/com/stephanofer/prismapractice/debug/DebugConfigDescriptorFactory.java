package com.stephanofer.prismapractice.debug;

import com.stephanofer.prismapractice.config.ConfigDescriptor;
import com.stephanofer.prismapractice.config.ConfigException;
import com.stephanofer.prismapractice.config.YamlConfigHelper;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DebugConfigDescriptorFactory {

    private DebugConfigDescriptorFactory() {
    }

    public static ConfigDescriptor<DebugConfig> descriptor() {
        return ConfigDescriptor.builder("runtime-debug", DebugConfig.class)
                .filePath("debug.yml")
                .bundledResourcePath("defaults/debug.yml")
                .schemaVersion(1)
                .mapper(DebugConfigDescriptorFactory::map)
                .validator(DebugConfigDescriptorFactory::validate)
                .build();
    }

    private static DebugConfig map(Map<String, Object> root) {
        Map<String, Object> debug = YamlConfigHelper.section(root, "debug");
        Map<String, Object> categories = YamlConfigHelper.section(debug, "categories");
        Map<String, Object> slow = YamlConfigHelper.section(debug, "slow-thresholds");
        Map<String, Object> watch = YamlConfigHelper.section(debug, "watch");

        return new DebugConfig(
                YamlConfigHelper.bool(debug, "enabled"),
                parseSeverity(YamlConfigHelper.string(debug, "console-severity"), "debug.console-severity"),
                YamlConfigHelper.integer(debug, "ring-buffer-size"),
                parseLevel(YamlConfigHelper.string(debug, "default-category-level"), "debug.default-category-level"),
                categoryLevels(categories),
                new DebugConfig.SlowThresholds(
                        longValue(slow, "service-ms"),
                        longValue(slow, "mysql-ms"),
                        longValue(slow, "redis-ms"),
                        longValue(slow, "scoreboard-ms")
                ),
                new DebugConfig.WatchSettings(
                        YamlConfigHelper.integer(watch, "default-minutes"),
                        YamlConfigHelper.integer(watch, "max-minutes")
                ),
                YamlConfigHelper.bool(debug, "include-exception-stack-traces")
        );
    }

    private static void validate(DebugConfig config) {
        if (config.ringBufferSize() < 50 || config.ringBufferSize() > 10_000) {
            throw new ConfigException("debug.ring-buffer-size must be between 50 and 10000");
        }
        requireMinimum(config.slowThresholds().serviceMs(), 1L, "debug.slow-thresholds.service-ms");
        requireMinimum(config.slowThresholds().mysqlMs(), 1L, "debug.slow-thresholds.mysql-ms");
        requireMinimum(config.slowThresholds().redisMs(), 1L, "debug.slow-thresholds.redis-ms");
        requireMinimum(config.slowThresholds().scoreboardMs(), 1L, "debug.slow-thresholds.scoreboard-ms");
        if (config.watch().defaultMinutes() < 1 || config.watch().defaultMinutes() > config.watch().maxMinutes()) {
            throw new ConfigException("debug.watch.default-minutes must be >= 1 and <= debug.watch.max-minutes");
        }
        if (config.watch().maxMinutes() < 1 || config.watch().maxMinutes() > 720) {
            throw new ConfigException("debug.watch.max-minutes must be between 1 and 720");
        }
    }

    private static Map<String, DebugDetailLevel> categoryLevels(Map<String, Object> section) {
        Map<String, DebugDetailLevel> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            if (!(entry.getValue() instanceof String stringValue)) {
                throw new ConfigException("Expected string level for debug.categories." + entry.getKey());
            }
            values.put(entry.getKey(), parseLevel(stringValue, "debug.categories." + entry.getKey()));
        }
        return values;
    }

    private static DebugDetailLevel parseLevel(String value, String key) {
        try {
            return DebugDetailLevel.parse(value);
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("Invalid debug level at '" + key + "': " + value, exception);
        }
    }

    private static DebugSeverity parseSeverity(String value, String key) {
        try {
            return DebugSeverity.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("Invalid debug severity at '" + key + "': " + value, exception);
        }
    }

    private static long longValue(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new ConfigException("Expected integer at key '" + key + "'");
    }

    private static void requireMinimum(long value, long minimum, String key) {
        if (value < minimum) {
            throw new ConfigException(key + " must be >= " + minimum);
        }
    }
}
