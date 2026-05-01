package com.stephanofer.prismapractice.paper.ui.demo;

import fr.maxlego08.menu.api.MenuItemStack;
import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public final class DemoPaginatedListButton extends PaginateButton {

    private final DemoMenuStateStore stateStore;
    private final int totalItems;
    private final int emptySlot;

    public DemoPaginatedListButton(DemoMenuStateStore stateStore, int totalItems, int emptySlot) {
        this.stateStore = stateStore;
        this.totalItems = totalItems;
        this.emptySlot = emptySlot;
    }

    @Override
    public void onRender(Player player, InventoryEngine inventoryEngine) {
        List<Integer> entries = stateStore.integerRange(totalItems);
        if (entries.isEmpty()) {
            if (emptySlot >= 0) {
                inventoryEngine.addItem(emptySlot, new ItemStack(Material.BARRIER));
            }
            return;
        }
        MenuItemStack template = getItemStack();
        paginate(entries, inventoryEngine, (slot, entry) -> {
            Placeholders placeholders = DemoButtonSupport.placeholders(player, stateStore);
            placeholders.register("entry", String.valueOf(entry));
            var button = inventoryEngine.addItem(slot, template.build(player, false, placeholders));
            if (button != null) {
                button.setClick(createClick(player, entry));
            }
        });
    }

    @Override
    public int getPaginationSize(Player player) {
        return totalItems;
    }

    private Consumer<InventoryClickEvent> createClick(Player player, int entry) {
        return event -> player.sendRichMessage("<green>Demo entry seleccionado:</green> <white>" + entry + "</white>");
    }
}
