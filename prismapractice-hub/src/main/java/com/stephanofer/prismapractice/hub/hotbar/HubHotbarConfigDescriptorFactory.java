package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.config.ConfigDescriptor;
import com.stephanofer.prismapractice.config.ConfigException;
import com.stephanofer.prismapractice.config.YamlConfigHelper;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HubHotbarConfigDescriptorFactory {

    private HubHotbarConfigDescriptorFactory() {
    }

    public static ConfigDescriptor<HubHotbarConfig> descriptor() {
        return ConfigDescriptor.builder("hub-items", HubHotbarConfig.class)
                .filePath("hub-items.yml")
                .bundledResourcePath("defaults/hub-items.yml")
                .schemaVersion(1)
                .mapper(HubHotbarConfigDescriptorFactory::map)
                .validator(HubHotbarConfigDescriptorFactory::validate)
                .build();
    }

    private static HubHotbarConfig map(Map<String, Object> root) {
        Map<String, Object> profilesSection = YamlConfigHelper.section(root, "profiles");
        Map<String, HubHotbarProfileConfig> profiles = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : profilesSection.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> rawProfile)) {
                throw new ConfigException("Profile '" + entry.getKey() + "' must be a map");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> profile = (Map<String, Object>) rawProfile;
            Map<String, Object> constraintsSection = YamlConfigHelper.section(profile, "constraints");
            HubHotbarConstraints constraints = new HubHotbarConstraints(
                    bool(constraintsSection, "deny-move", true),
                    bool(constraintsSection, "deny-drop", true),
                    bool(constraintsSection, "deny-place", true),
                    bool(constraintsSection, "deny-pickup", true),
                    bool(constraintsSection, "deny-swap-offhand", true)
            );

            Map<String, Object> itemsSection = YamlConfigHelper.section(profile, "items");
            Map<Integer, HubHotbarItemConfig> items = new LinkedHashMap<>();
            for (Map.Entry<String, Object> itemEntry : itemsSection.entrySet()) {
                if (!(itemEntry.getValue() instanceof Map<?, ?> rawItem)) {
                    throw new ConfigException("Item definition at profiles." + entry.getKey() + ".items." + itemEntry.getKey() + " must be a map");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> itemSection = (Map<String, Object>) rawItem;
                int slot = Integer.parseInt(itemEntry.getKey());
                String itemKey = string(itemSection, "key", entry.getKey() + "-slot-" + slot);
                Material material = parseMaterial(YamlConfigHelper.string(itemSection, "material"));
                Map<String, Object> actionSection = YamlConfigHelper.section(itemSection, "action");
                HubHotbarActionConfig action = new HubHotbarActionConfig(
                        parseActionType(string(actionSection, "type", "RUN_PLAYER_COMMAND")),
                        parseTrigger(string(actionSection, "trigger", "RIGHT_CLICK")),
                        string(actionSection, "target", ""),
                        string(actionSection, "command", ""),
                        string(actionSection, "plugin", ""),
                        integer(actionSection, "page", 1),
                        stringList(actionSection, "arguments"),
                        string(actionSection, "custom-key", "")
                );

                items.put(slot, new HubHotbarItemConfig(
                        itemKey,
                        slot,
                        material,
                        integer(itemSection, "amount", 1),
                        string(itemSection, "name", ""),
                        stringList(itemSection, "lore"),
                        optionalInteger(itemSection, "custom-model-data"),
                        bool(itemSection, "glow", false),
                        bool(itemSection, "hide-attributes", true),
                        parseItemFlags(stringList(itemSection, "item-flags")),
                        action
                ));
            }

            profiles.put(entry.getKey(), new HubHotbarProfileConfig(
                    entry.getKey(),
                    integer(profile, "selected-slot", 0),
                    bool(profile, "reset-inventory", true),
                    constraints,
                    items
            ));
        }

        return new HubHotbarConfig(profiles);
    }

    private static void validate(HubHotbarConfig config) {
        if (config.profiles().isEmpty()) {
            throw new ConfigException("profiles must not be empty");
        }

        for (HubHotbarProfileConfig profile : config.profiles().values()) {
            if (profile.selectedSlot() < 0 || profile.selectedSlot() > 8) {
                throw new ConfigException("profiles." + profile.key() + ".selected-slot must be between 0 and 8");
            }
            for (Map.Entry<Integer, HubHotbarItemConfig> entry : profile.items().entrySet()) {
                int slot = entry.getKey();
                HubHotbarItemConfig item = entry.getValue();
                if (slot < 0 || slot > 8) {
                    throw new ConfigException("profiles." + profile.key() + ".items slot must be between 0 and 8");
                }
                if (item.amount() < 1 || item.amount() > 64) {
                    throw new ConfigException("profiles." + profile.key() + ".items." + slot + ".amount must be between 1 and 64");
                }
                validateAction(profile.key(), slot, item.action());
            }
        }
    }

    private static void validateAction(String profileKey, int slot, HubHotbarActionConfig action) {
        String path = "profiles." + profileKey + ".items." + slot + ".action";
        switch (action.type()) {
            case OPEN_MENU -> {
                if (action.target().isBlank()) {
                    throw new ConfigException(path + ".target must not be blank for OPEN_MENU");
                }
                if (action.page() < 1) {
                    throw new ConfigException(path + ".page must be >= 1");
                }
            }
            case RUN_PLAYER_COMMAND, RUN_CONSOLE_COMMAND -> {
                if (action.command().isBlank()) {
                    throw new ConfigException(path + ".command must not be blank for command actions");
                }
            }
            case CUSTOM -> {
                if (action.customKey().isBlank()) {
                    throw new ConfigException(path + ".custom-key must not be blank for CUSTOM");
                }
            }
            case LEAVE_QUEUE -> {
            }
        }
    }

    private static HubHotbarActionType parseActionType(String value) {
        try {
            return HubHotbarActionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("Unknown hub item action type '" + value + "'");
        }
    }

    private static HubHotbarActionTrigger parseTrigger(String value) {
        try {
            return HubHotbarActionTrigger.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("Unknown hub item action trigger '" + value + "'");
        }
    }

    private static Material parseMaterial(String value) {
        Material material = Material.matchMaterial(value);
        if (material == null) {
            throw new ConfigException("Unknown material '" + value + "'");
        }
        return material;
    }

    private static Set<ItemFlag> parseItemFlags(List<String> values) {
        Set<ItemFlag> flags = new LinkedHashSet<>();
        for (String value : values) {
            try {
                flags.add(ItemFlag.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new ConfigException("Unknown item flag '" + value + "'");
            }
        }
        return flags;
    }

    private static String string(Map<String, Object> root, String key, String defaultValue) {
        Object value = root.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static int integer(Map<String, Object> root, String key, int defaultValue) {
        Object value = root.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    private static Integer optionalInteger(Map<String, Object> root, String key) {
        Object value = root.get(key);
        return value instanceof Number number ? number.intValue() : null;
    }

    private static boolean bool(Map<String, Object> root, String key, boolean defaultValue) {
        Object value = root.get(key);
        return value instanceof Boolean booleanValue ? booleanValue : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new ConfigException("Expected list at key '" + key + "'");
        }
        List<String> result = new ArrayList<>();
        for (Object element : list) {
            if (!(element instanceof String stringValue)) {
                throw new ConfigException("Expected string list at key '" + key + "'");
            }
            result.add(stringValue);
        }
        return result;
    }
}
