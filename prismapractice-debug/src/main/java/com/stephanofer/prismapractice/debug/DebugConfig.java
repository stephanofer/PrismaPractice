package com.stephanofer.prismapractice.debug;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record DebugConfig(
        boolean enabled,
        DebugSeverity consoleSeverity,
        int ringBufferSize,
        DebugDetailLevel defaultCategoryLevel,
        Map<String, DebugDetailLevel> categories,
        SlowThresholds slowThresholds,
        WatchSettings watch,
        boolean includeExceptionStackTraces
) {

    public DebugConfig {
        Objects.requireNonNull(consoleSeverity, "consoleSeverity");
        Objects.requireNonNull(defaultCategoryLevel, "defaultCategoryLevel");
        Objects.requireNonNull(categories, "categories");
        Objects.requireNonNull(slowThresholds, "slowThresholds");
        Objects.requireNonNull(watch, "watch");
        categories = normalizeCategories(categories);
    }

    public static DebugConfig defaults() {
        return new DebugConfig(
                false,
                DebugSeverity.WARN,
                500,
                DebugDetailLevel.OFF,
                Map.of(),
                new SlowThresholds(25L, 50L, 20L, 15L),
                new WatchSettings(10, 60),
                true
        );
    }

    public DebugDetailLevel categoryLevel(String category) {
        return categories.getOrDefault(DebugCategories.normalize(category), defaultCategoryLevel);
    }

    private static Map<String, DebugDetailLevel> normalizeCategories(Map<String, DebugDetailLevel> categories) {
        Map<String, DebugDetailLevel> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, DebugDetailLevel> entry : categories.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            normalized.put(DebugCategories.normalize(entry.getKey()), entry.getValue());
        }
        return Map.copyOf(normalized);
    }

    public record SlowThresholds(long serviceMs, long mysqlMs, long redisMs, long scoreboardMs) {
    }

    public record WatchSettings(int defaultMinutes, int maxMinutes) {
    }
}
