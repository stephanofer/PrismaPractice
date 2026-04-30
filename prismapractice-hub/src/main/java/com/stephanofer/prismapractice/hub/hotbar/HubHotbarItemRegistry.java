package com.stephanofer.prismapractice.hub.hotbar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class HubHotbarItemRegistry {

    private final NamespacedKey itemKey;
    private final MiniMessage miniMessage;
    private final Map<String, HubCompiledHotbarProfile> profiles;
    private final Map<String, HubCompiledItem> itemsByKey;

    HubHotbarItemRegistry(Plugin plugin, HubHotbarConfig config) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(config, "config");
        this.itemKey = new NamespacedKey(plugin, "hub-item-key");
        this.miniMessage = MiniMessage.miniMessage();
        this.profiles = new LinkedHashMap<>();
        this.itemsByKey = new LinkedHashMap<>();
        compile(config);
    }

    Optional<HubCompiledHotbarProfile> findProfile(String key) {
        return Optional.ofNullable(profiles.get(key));
    }

    Optional<HubCompiledItem> findItemByKey(String key) {
        return Optional.ofNullable(itemsByKey.get(key));
    }

    Optional<HubCompiledItem> findItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return Optional.empty();
        }
        String value = itemStack.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        return value == null ? Optional.empty() : findItemByKey(value);
    }

    private void compile(HubHotbarConfig config) {
        for (HubHotbarProfileConfig profileConfig : config.profiles().values()) {
            Map<Integer, HubCompiledItem> compiledItems = new LinkedHashMap<>();
            for (HubHotbarItemConfig itemConfig : profileConfig.items().values()) {
                HubCompiledItem compiledItem = new HubCompiledItem(
                        itemConfig.key(),
                        itemConfig.slot(),
                        createItemStack(itemConfig),
                        itemConfig.action()
                );
                compiledItems.put(itemConfig.slot(), compiledItem);
                itemsByKey.put(itemConfig.key(), compiledItem);
            }
            profiles.put(profileConfig.key(), new HubCompiledHotbarProfile(
                    profileConfig.key(),
                    profileConfig.selectedSlot(),
                    profileConfig.resetInventory(),
                    profileConfig.constraints(),
                    compiledItems
            ));
        }
    }

    private ItemStack createItemStack(HubHotbarItemConfig config) {
        ItemStack itemStack = new ItemStack(config.material(), config.amount());
        itemStack.editMeta(meta -> {
            if (!config.name().isBlank()) {
                meta.displayName(component(config.name()));
            }
            if (!config.lore().isEmpty()) {
                meta.lore(config.lore().stream().map(this::component).toList());
            }
            if (config.customModelData() != null) {
                var customModelData = meta.getCustomModelDataComponent();
                customModelData.setFloats(List.of(config.customModelData().floatValue()));
                meta.setCustomModelDataComponent(customModelData);
            }
            Set<ItemFlag> flags = config.itemFlags();
            if (!flags.isEmpty()) {
                meta.addItemFlags(flags.toArray(ItemFlag[]::new));
            }
            if (config.hideAttributes()) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            }
            if (config.glow()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        });
        itemStack.editPersistentDataContainer(container -> container.set(itemKey, PersistentDataType.STRING, config.key()));
        return itemStack;
    }

    private Component component(String input) {
        return miniMessage.deserialize(input);
    }
}
