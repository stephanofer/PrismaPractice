package com.stephanofer.prismapractice.paper.ui.demo;

import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public final class DemoToggleButton extends Button {

    private final DemoMenuStateStore stateStore;
    private final Supplier<ZMenuUiService> menuUiServiceSupplier;
    private final String stateKey;
    private final String defaultValue;
    private final List<String> values;

    public DemoToggleButton(DemoMenuStateStore stateStore, Supplier<ZMenuUiService> menuUiServiceSupplier, String stateKey, String defaultValue, List<String> values) {
        this.stateStore = stateStore;
        this.menuUiServiceSupplier = menuUiServiceSupplier;
        this.stateKey = stateKey;
        this.defaultValue = defaultValue;
        this.values = List.copyOf(values);
    }

    @Override
    public ItemStack getCustomItemStack(Player player, boolean useCache, Placeholders placeholders) {
        stateStore.put(player, stateKey, stateStore.get(player, stateKey).orElse(defaultValue));
        return DemoButtonSupport.build(getItemStack(), player, stateStore);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        stateStore.cycle(player, stateKey, values, defaultValue);
        menuUiServiceSupplier.get().updateInventory(player);
    }
}
