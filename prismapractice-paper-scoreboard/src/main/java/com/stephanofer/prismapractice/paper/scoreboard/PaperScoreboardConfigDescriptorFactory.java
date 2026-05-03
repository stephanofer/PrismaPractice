package com.stephanofer.prismapractice.paper.scoreboard;

import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.queue.QueueType;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import com.stephanofer.prismapractice.config.ConfigDescriptor;
import com.stephanofer.prismapractice.config.ConfigException;
import com.stephanofer.prismapractice.config.YamlConfigHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PaperScoreboardConfigDescriptorFactory {

    private PaperScoreboardConfigDescriptorFactory() {
    }

    public static ConfigDescriptor<PaperScoreboardConfig> descriptor(String id, String filePath, String bundledResourcePath) {
        return ConfigDescriptor.builder(id, PaperScoreboardConfig.class)
                .filePath(filePath)
                .bundledResourcePath(bundledResourcePath)
                .schemaVersion(2)
                .migration(1, root -> {
                })
                .mapper(PaperScoreboardConfigDescriptorFactory::map)
                .validator(PaperScoreboardConfigDescriptorFactory::validate)
                .build();
    }

    private static PaperScoreboardConfig map(Map<String, Object> root) {
        Map<String, Object> settingsSection = YamlConfigHelper.section(root, "settings");
        PaperScoreboardSettings settings = new PaperScoreboardSettings(
                bool(settingsSection, "enabled", true),
                integer(settingsSection, "tick-interval", 20),
                integer(settingsSection, "default-refresh-ticks", 20),
                bool(settingsSection, "hide-when-disabled-in-settings", true),
                bool(settingsSection, "allow-placeholderapi", false)
        );

        Map<String, Object> scenesSection = YamlConfigHelper.section(root, "scenes");
        Map<String, ScoreboardSceneConfig> scenesByKey = new LinkedHashMap<>();
        List<ScoreboardSceneConfig> scenes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : scenesSection.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> rawScene)) {
                throw new ConfigException("Scene '" + entry.getKey() + "' must be a map");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> sceneSection = (Map<String, Object>) rawScene;
            Map<String, Object> refreshSection = YamlConfigHelper.section(sceneSection, "refresh");
            Map<String, Object> conditionsSection = YamlConfigHelper.section(sceneSection, "conditions");
            ScoreboardSceneConfig scene = new ScoreboardSceneConfig(
                    entry.getKey(),
                    bool(sceneSection, "enabled", true),
                    integer(sceneSection, "priority", 0),
                    integer(refreshSection, "interval-ticks", settings.defaultRefreshTicks()),
                    new ScoreboardSceneConditions(
                            enumSet(RuntimeType.class, stringList(conditionsSection, "runtime-types")),
                            enumSet(PlayerStatus.class, stringList(conditionsSection, "statuses")),
                            enumSet(PlayerSubStatus.class, stringList(conditionsSection, "sub-statuses")),
                            booleanSet(booleanList(conditionsSection, "party-membership")),
                            enumSet(ScoreboardPartyRole.class, stringList(conditionsSection, "party-roles")),
                            enumSet(QueueType.class, stringList(conditionsSection, "queue-types")),
                            enumSet(PlayerType.class, stringList(conditionsSection, "queue-player-types")),
                            enumSet(ScoreboardUiFocus.class, stringList(conditionsSection, "ui-focuses"))
                    ),
                    string(sceneSection, "title", "<gray>Practice"),
                    stringList(sceneSection, "lines")
            );
            if (!scene.enabled()) {
                continue;
            }
            scenes.add(scene);
            scenesByKey.put(scene.key(), scene);
        }
        scenes.sort(Comparator.comparingInt(ScoreboardSceneConfig::priority).reversed().thenComparing(ScoreboardSceneConfig::key));
        return new PaperScoreboardConfig(settings, scenes, scenesByKey);
    }

    private static void validate(PaperScoreboardConfig config) {
        if (config.scenes().isEmpty()) {
            throw new ConfigException("scenes must not be empty");
        }
        for (ScoreboardSceneConfig scene : config.scenes()) {
            if (scene.lines().size() > 15) {
                throw new ConfigException("scenes." + scene.key() + ".lines must contain at most 15 entries");
            }
        }
    }

    private static <E extends Enum<E>> Set<E> enumSet(Class<E> type, List<String> values) {
        Set<E> result = new LinkedHashSet<>();
        for (String value : values) {
            try {
                result.add(Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new ConfigException("Unknown value '" + value + "' for enum " + type.getSimpleName());
            }
        }
        return result;
    }

    private static Set<Boolean> booleanSet(List<Boolean> values) {
        Set<Boolean> result = new LinkedHashSet<>();
        for (Boolean value : values) {
            result.add(value);
        }
        return result;
    }

    private static String string(Map<String, Object> root, String key, String defaultValue) {
        Object value = root.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static int integer(Map<String, Object> root, String key, int defaultValue) {
        Object value = root.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    private static boolean bool(Map<String, Object> root, String key, boolean defaultValue) {
        Object value = root.get(key);
        return value instanceof Boolean booleanValue ? booleanValue : defaultValue;
    }

    private static List<String> stringList(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new ConfigException("Expected list at key '" + key + "'");
        }
        List<String> values = new ArrayList<>();
        for (Object element : list) {
            if (!(element instanceof String stringValue)) {
                throw new ConfigException("Expected string list at key '" + key + "'");
            }
            values.add(stringValue);
        }
        return values;
    }

    private static List<Boolean> booleanList(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new ConfigException("Expected list at key '" + key + "'");
        }
        List<Boolean> values = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof Boolean booleanValue) {
                values.add(booleanValue);
                continue;
            }
            if (element instanceof String stringValue) {
                String normalized = stringValue.trim().toLowerCase(Locale.ROOT);
                if (normalized.equals("true")) {
                    values.add(Boolean.TRUE);
                    continue;
                }
                if (normalized.equals("false")) {
                    values.add(Boolean.FALSE);
                    continue;
                }
            }
            throw new ConfigException("Expected boolean list at key '" + key + "'");
        }
        return values;
    }
}
