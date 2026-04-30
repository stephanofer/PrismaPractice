package com.stephanofer.prismapractice.hub.hotbar;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;

record HubCompiledItem(String key, int slot, ItemStack itemStack, HubHotbarActionConfig action) {

    HubCompiledItem {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(itemStack, "itemStack");
        Objects.requireNonNull(action, "action");
    }

    ItemStack cloneStack() {
        return itemStack.clone();
    }
}
