package com.stephanofer.prismapractice.feedback;

import com.stephanofer.prismapractice.config.ConfigDescriptor;
import com.stephanofer.prismapractice.config.ConfigException;
import com.stephanofer.prismapractice.config.YamlConfigHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class FeedbackConfigDescriptorFactory {

    private FeedbackConfigDescriptorFactory() {
    }

    public static ConfigDescriptor<FeedbackConfig> descriptor(String id, String filePath, String bundledResourcePath) {
        return ConfigDescriptor.builder(id, FeedbackConfig.class)
                .filePath(filePath)
                .bundledResourcePath(bundledResourcePath)
                .schemaVersion(1)
                .mapper(FeedbackConfigDescriptorFactory::map)
                .validator(FeedbackConfigDescriptorFactory::validate)
                .build();
    }

    private static FeedbackConfig map(Map<String, Object> root) {
        Map<String, Object> templatesSection = YamlConfigHelper.section(root, "templates");
        Map<String, FeedbackTemplate> templates = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : templatesSection.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> rawTemplate)) {
                throw new ConfigException("Template '" + entry.getKey() + "' must be a map");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> templateSection = (Map<String, Object>) rawTemplate;
            List<Map<String, Object>> rawDeliveries = sectionList(templateSection, "deliveries");
            List<FeedbackDelivery> deliveries = new ArrayList<>(rawDeliveries.size());
            for (Map<String, Object> rawDelivery : rawDeliveries) {
                deliveries.add(mapDelivery(entry.getKey(), rawDelivery));
            }
            templates.put(entry.getKey(), new FeedbackTemplate(entry.getKey(), List.copyOf(deliveries)));
        }
        return new FeedbackConfig(Map.copyOf(templates));
    }

    private static FeedbackDelivery mapDelivery(String templateKey, Map<String, Object> rawDelivery) {
        FeedbackChannel channel = parseEnum(FeedbackChannel.class, string(rawDelivery, "channel", true), "channel");
        return switch (channel) {
            case CHAT -> new ChatFeedbackDelivery(readChatLines(rawDelivery));
            case ACTION_BAR -> new ActionBarFeedbackDelivery(
                    parseDeliveryMode(string(rawDelivery, "mode", false)),
                    string(rawDelivery, "message", true),
                    mapPersistence(rawDelivery)
            );
            case TITLE -> new TitleFeedbackDelivery(
                    string(rawDelivery, "title", true),
                    string(rawDelivery, "subtitle", false, ""),
                    mapTitleTimes(rawDelivery)
            );
            case SOUND -> new SoundFeedbackDelivery(
                    string(rawDelivery, "sound", true),
                    parseEnum(FeedbackSoundSource.class, string(rawDelivery, "source", false, "MASTER"), "source"),
                    floatNumber(rawDelivery, "volume", 1.0f),
                    floatNumber(rawDelivery, "pitch", 1.0f)
            );
            case BOSSBAR -> new BossBarFeedbackDelivery(
                    parseDeliveryMode(string(rawDelivery, "mode", false)),
                    string(rawDelivery, "message", true),
                    floatNumber(rawDelivery, "progress", 1.0f),
                    parseEnum(FeedbackBossBarColor.class, string(rawDelivery, "color", false, "BLUE"), "color"),
                    parseEnum(FeedbackBossBarOverlay.class, string(rawDelivery, "overlay", false, "PROGRESS"), "overlay"),
                    enumSet(FeedbackBossBarFlag.class, stringList(rawDelivery, "flags"), "flags"),
                    mapPersistence(rawDelivery),
                    integer(rawDelivery, "duration-ticks", 60)
            );
        };
    }

    private static void validate(FeedbackConfig config) {
        if (config.templates().isEmpty()) {
            throw new ConfigException("templates must not be empty");
        }
        for (FeedbackTemplate template : config.templates().values()) {
            if (template.deliveries().isEmpty()) {
                throw new ConfigException("templates." + template.key() + ".deliveries must not be empty");
            }
            for (FeedbackDelivery delivery : template.deliveries()) {
                switch (delivery) {
                    case ChatFeedbackDelivery chat -> validateChat(template.key(), chat);
                    case ActionBarFeedbackDelivery actionBar -> validateActionBar(template.key(), actionBar);
                    case TitleFeedbackDelivery title -> validateTitle(template.key(), title);
                    case SoundFeedbackDelivery sound -> validateSound(template.key(), sound);
                    case BossBarFeedbackDelivery bossBar -> validateBossBar(template.key(), bossBar);
                }
            }
        }
    }

    private static void validateChat(String templateKey, ChatFeedbackDelivery delivery) {
        if (delivery.lines().isEmpty()) {
            throw new ConfigException(path(templateKey, delivery.channel()) + " must define at least one line");
        }
    }

    private static void validateActionBar(String templateKey, ActionBarFeedbackDelivery delivery) {
        if (delivery.message().isBlank()) {
            throw new ConfigException(path(templateKey, delivery.channel()) + ".message must not be blank");
        }
        validatePersistence(templateKey, delivery.channel(), delivery.mode(), delivery.persistence());
    }

    private static void validateTitle(String templateKey, TitleFeedbackDelivery delivery) {
        if (delivery.title().isBlank()) {
            throw new ConfigException(path(templateKey, delivery.channel()) + ".title must not be blank");
        }
        TitleTimes times = delivery.times();
        if (times.fadeInTicks() < 0 || times.stayTicks() < 0 || times.fadeOutTicks() < 0) {
            throw new ConfigException(path(templateKey, delivery.channel()) + ".times must be >= 0");
        }
    }

    private static void validateSound(String templateKey, SoundFeedbackDelivery delivery) {
        if (delivery.sound().isBlank()) {
            throw new ConfigException(path(templateKey, delivery.channel()) + ".sound must not be blank");
        }
        if (delivery.volume() < 0.0f) {
            throw new ConfigException(path(templateKey, delivery.channel()) + ".volume must be >= 0");
        }
        if (delivery.pitch() < 0.0f || delivery.pitch() > 2.0f) {
            throw new ConfigException(path(templateKey, delivery.channel()) + ".pitch must be between 0 and 2");
        }
    }

    private static void validateBossBar(String templateKey, BossBarFeedbackDelivery delivery) {
        if (delivery.message().isBlank()) {
            throw new ConfigException(path(templateKey, delivery.channel()) + ".message must not be blank");
        }
        if (delivery.progress() < 0.0f || delivery.progress() > 1.0f) {
            throw new ConfigException(path(templateKey, delivery.channel()) + ".progress must be between 0 and 1");
        }
        validatePersistence(templateKey, delivery.channel(), delivery.mode(), delivery.persistence());
        if (delivery.mode() == FeedbackDeliveryMode.ONE_SHOT && delivery.durationTicks() <= 0) {
            throw new ConfigException(path(templateKey, delivery.channel()) + ".duration-ticks must be > 0 for ONE_SHOT bossbars");
        }
    }

    private static void validatePersistence(String templateKey, FeedbackChannel channel, FeedbackDeliveryMode mode, FeedbackPersistence persistence) {
        String path = path(templateKey, channel);
        if (mode == FeedbackDeliveryMode.ONE_SHOT) {
            return;
        }
        if (persistence == null) {
            throw new ConfigException(path + " requires slot configuration in PERSISTENT mode");
        }
        if (persistence.slot().isBlank()) {
            throw new ConfigException(path + ".slot must not be blank");
        }
        if (persistence.intervalTicks() < 1) {
            throw new ConfigException(path + ".interval-ticks must be >= 1");
        }
    }

    private static String path(String templateKey, FeedbackChannel channel) {
        return "templates." + templateKey + ".deliveries(" + channel + ")";
    }

    private static TitleTimes mapTitleTimes(Map<String, Object> rawDelivery) {
        Map<String, Object> timesSection = YamlConfigHelper.section(rawDelivery, "times");
        return new TitleTimes(
                integer(timesSection, "fade-in-ticks", 10),
                integer(timesSection, "stay-ticks", 40),
                integer(timesSection, "fade-out-ticks", 10)
        );
    }

    private static FeedbackPersistence mapPersistence(Map<String, Object> rawDelivery) {
        String slot = string(rawDelivery, "slot", false, "");
        if (slot.isBlank()) {
            return null;
        }
        return new FeedbackPersistence(slot, integer(rawDelivery, "interval-ticks", 20), integer(rawDelivery, "priority", 0));
    }

    private static List<String> readChatLines(Map<String, Object> rawDelivery) {
        List<String> lines = stringList(rawDelivery, "lines");
        if (!lines.isEmpty()) {
            return lines;
        }
        String message = string(rawDelivery, "message", false, "");
        return message.isBlank() ? List.of() : List.of(message);
    }

    private static FeedbackDeliveryMode parseDeliveryMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return FeedbackDeliveryMode.ONE_SHOT;
        }
        return parseEnum(FeedbackDeliveryMode.class, raw, "mode");
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String key) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("Unknown " + key + " '" + value + "'");
        }
    }

    private static <E extends Enum<E>> Set<E> enumSet(Class<E> type, List<String> values, String key) {
        Set<E> result = new LinkedHashSet<>();
        for (String value : values) {
            result.add(parseEnum(type, value, key));
        }
        return Set.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> sectionList(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (!(value instanceof List<?> list)) {
            throw new ConfigException("Expected list at key '" + key + "'");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object element : list) {
            if (!(element instanceof Map<?, ?> map)) {
                throw new ConfigException("Expected map entries at key '" + key + "'");
            }
            result.add((Map<String, Object>) map);
        }
        return result;
    }

    private static List<String> stringList(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new ConfigException("Expected list at key '" + key + "'");
        }
        List<String> result = new ArrayList<>(list.size());
        for (Object element : list) {
            if (!(element instanceof String stringValue)) {
                throw new ConfigException("Expected string list at key '" + key + "'");
            }
            result.add(stringValue);
        }
        return List.copyOf(result);
    }

    private static String string(Map<String, Object> root, String key, boolean required) {
        return string(root, key, required, null);
    }

    private static String string(Map<String, Object> root, String key, boolean required, String defaultValue) {
        Object value = root.get(key);
        if (value == null) {
            if (required) {
                throw new ConfigException("Expected string at key '" + key + "'");
            }
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private static int integer(Map<String, Object> root, String key, int defaultValue) {
        Object value = root.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    private static float floatNumber(Map<String, Object> root, String key, float defaultValue) {
        Object value = root.get(key);
        return value instanceof Number number ? number.floatValue() : defaultValue;
    }
}
