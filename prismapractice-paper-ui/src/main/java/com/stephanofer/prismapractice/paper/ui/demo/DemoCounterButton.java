package com.stephanofer.prismapractice.paper.ui.demo;

import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public final class DemoCounterButton extends Button {

    private final DemoMenuStateStore stateStore;
    private final Supplier<ZMenuUiService> menuUiServiceSupplier;
    private final String counterKey;

    public DemoCounterButton(DemoMenuStateStore stateStore, Supplier<ZMenuUiService> menuUiServiceSupplier, String counterKey) {
        this.stateStore = stateStore;
        this.menuUiServiceSupplier = menuUiServiceSupplier;
        this.counterKey = counterKey;
    }

    @Override
    public ItemStack getCustomItemStack(Player player, boolean useCache, Placeholders placeholders) {
        return DemoButtonSupport.build(getItemStack(), player, stateStore);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        stateStore.increment(player, counterKey);
        menuUiServiceSupplier.get().updateInventory(player);
    }
}
