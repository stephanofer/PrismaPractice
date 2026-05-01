package com.stephanofer.prismapractice.paper.ui.demo;

import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogService;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class DemoOpenDialogButton extends Button {

    private final DemoMenuStateStore stateStore;
    private final PaperDialogService dialogService;
    private final String dialogId;

    public DemoOpenDialogButton(DemoMenuStateStore stateStore, PaperDialogService dialogService, String dialogId) {
        this.stateStore = stateStore;
        this.dialogService = dialogService;
        this.dialogId = dialogId;
    }

    @Override
    public ItemStack getCustomItemStack(Player player, boolean useCache, Placeholders placeholders) {
        return DemoButtonSupport.build(getItemStack(), player, stateStore);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        dialogService.sessionStore().session(player.getUniqueId().toString()).putAll(stateStore.snapshot(player));
        dialogService.open(player, dialogId);
    }
}
