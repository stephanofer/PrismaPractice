package com.stephanofer.prismapractice.paper.ui.demo;

import fr.maxlego08.menu.api.MenuItemStack;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

final class DemoButtonSupport {

    private DemoButtonSupport() {
    }

    static Placeholders placeholders(Player player, DemoMenuStateStore stateStore) {
        Placeholders placeholders = new Placeholders();
        placeholders.register("player_name", player.getName());
        for (Map.Entry<String, String> entry : stateStore.snapshot(player).entrySet()) {
            placeholders.register(entry.getKey(), entry.getValue());
        }
        placeholders.register("demo_counter", stateStore.get(player, "demo_counter").orElse("0"));
        placeholders.register("demo_mode", stateStore.get(player, "demo_mode").orElse("bronze"));
        return placeholders;
    }

    static ItemStack build(MenuItemStack itemStack, Player player, DemoMenuStateStore stateStore) {
        return itemStack.build(player, false, placeholders(player, stateStore));
    }
}
