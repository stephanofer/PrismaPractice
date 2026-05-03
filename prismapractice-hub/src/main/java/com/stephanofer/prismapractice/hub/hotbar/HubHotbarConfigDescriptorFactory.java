package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.config.ConfigDescriptor;
import com.stephanofer.prismapractice.config.ConfigException;
import com.stephanofer.prismapractice.config.YamlConfigHelper;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.Comparator;
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
                .schemaVersion(2)
                .migration(1, HubHotbarConfigDescriptorFactory::migrateSlotKeyedItemsToStableKeys)
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
            boolean profileEnabled = bool(profile, "enabled", true);
            if (!profileEnabled) {
                continue;
            }
            Map<String, Object> constraintsSection = YamlConfigHelper.section(profile, "constraints");
            HubHotbarConstraints constraints = new HubHotbarConstraints(
                    bool(constraintsSection, "deny-move", true),
                    bool(constraintsSection, "deny-drop", true),
                    bool(constraintsSection, "deny-place", true),
                    bool(constraintsSection, "deny-pickup", true),
                    bool(constraintsSection, "deny-swap-offhand", true)
            );

            Map<String, Object> itemsSection = YamlConfigHelper.section(profile, "items");
            Map<String, HubHotbarItemConfig> items = new LinkedHashMap<>();
            for (Map.Entry<String, Object> itemEntry : itemsSection.entrySet()) {
                if (!(itemEntry.getValue() instanceof Map<?, ?> rawItem)) {
                    throw new ConfigException("Item definition at profiles." + entry.getKey() + ".items." + itemEntry.getKey() + " must be a map");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> itemSection = (Map<String, Object>) rawItem;
                boolean itemEnabled = bool(itemSection, "enabled", true);
                if (!itemEnabled) {
                    continue;
                }
                String itemKey = itemEntry.getKey();
                int slot = integer(itemSection, "slot", -1);
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

                items.put(itemKey, new HubHotbarItemConfig(
                        itemKey,
                        true,
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
                    true,
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
            Map<Integer, String> slots = new LinkedHashMap<>();
            for (Map.Entry<String, HubHotbarItemConfig> entry : profile.items().entrySet()) {
                String itemKey = entry.getKey();
                HubHotbarItemConfig item = entry.getValue();
                int slot = item.slot();
                if (slot < 0 || slot > 8) {
                    throw new ConfigException("profiles." + profile.key() + ".items." + itemKey + ".slot must be between 0 and 8");
                }
                if (item.amount() < 1 || item.amount() > 64) {
                    throw new ConfigException("profiles." + profile.key() + ".items." + itemKey + ".amount must be between 1 and 64");
                }
                String previousItem = slots.putIfAbsent(slot, itemKey);
                if (previousItem != null) {
                    throw new ConfigException("profiles." + profile.key() + ".items defines duplicate active slot " + slot + " for '" + previousItem + "' and '" + itemKey + "'");
                }
                validateAction(profile.key(), slot, item.action());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void migrateSlotKeyedItemsToStableKeys(Map<String, Object> root) {
        Map<String, Object> profilesSection = YamlConfigHelper.section(root, "profiles");
        for (Object rawProfile : profilesSection.values()) {
            if (!(rawProfile instanceof Map<?, ?> rawProfileMap)) {
                continue;
            }
            Map<String, Object> profile = (Map<String, Object>) rawProfileMap;
            Map<String, Object> itemsSection = YamlConfigHelper.section(profile, "items");
            if (itemsSection.isEmpty()) {
                continue;
            }

            List<Map.Entry<String, Object>> orderedEntries = new ArrayList<>(itemsSection.entrySet());
            orderedEntries.sort(Comparator.comparingInt(entry -> parseLegacySlot(entry.getKey())));

            Map<String, Object> migratedItems = new LinkedHashMap<>();
            for (Map.Entry<String, Object> itemEntry : orderedEntries) {
                if (!(itemEntry.getValue() instanceof Map<?, ?> rawItemMap)) {
                    migratedItems.put(itemEntry.getKey(), itemEntry.getValue());
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>((Map<String, Object>) rawItemMap);
                int slot = parseLegacySlot(itemEntry.getKey());
                String preferredKey = string(item, "key", "slot-" + slot).trim();
                String stableKey = preferredKey.isBlank() ? "slot-" + slot : preferredKey;
                if (migratedItems.containsKey(stableKey)) {
                    stableKey = stableKey + "-slot-" + slot;
                }
                item.put("slot", slot);
                item.remove("key");
                migratedItems.put(stableKey, item);
            }
            profile.put("items", migratedItems);
        }
    }

    private static int parseLegacySlot(String rawValue) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
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
