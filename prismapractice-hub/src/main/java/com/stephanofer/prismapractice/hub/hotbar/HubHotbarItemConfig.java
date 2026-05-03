package com.stephanofer.prismapractice.hub.hotbar;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.List;
import java.util.Objects;
import java.util.Set;

record HubHotbarItemConfig(
        String key,
        boolean enabled,
        int slot,
        Material material,
        int amount,
        String name,
        List<String> lore,
        Integer customModelData,
        boolean glow,
        boolean hideAttributes,
        Set<ItemFlag> itemFlags,
        HubHotbarActionConfig action
) {

    HubHotbarItemConfig {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lore, "lore");
        Objects.requireNonNull(itemFlags, "itemFlags");
        Objects.requireNonNull(action, "action");
        lore = List.copyOf(lore);
        itemFlags = Set.copyOf(itemFlags);
    }
}
