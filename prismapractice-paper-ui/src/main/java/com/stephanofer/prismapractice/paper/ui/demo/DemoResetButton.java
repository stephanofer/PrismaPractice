package com.stephanofer.prismapractice.paper.ui.demo;

import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogService;
import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public final class DemoResetButton extends Button {

    private final DemoMenuStateStore stateStore;
    private final PaperDialogService dialogService;
    private final Supplier<ZMenuUiService> menuUiServiceSupplier;

    public DemoResetButton(DemoMenuStateStore stateStore, PaperDialogService dialogService, Supplier<ZMenuUiService> menuUiServiceSupplier) {
        this.stateStore = stateStore;
        this.dialogService = dialogService;
        this.menuUiServiceSupplier = menuUiServiceSupplier;
    }

    @Override
    public ItemStack getCustomItemStack(Player player, boolean useCache, Placeholders placeholders) {
        return DemoButtonSupport.build(getItemStack(), player, stateStore);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        stateStore.reset(player);
        dialogService.sessionStore().remove(player.getUniqueId().toString());
        menuUiServiceSupplier.get().updateInventory(player);
        player.sendRichMessage("<yellow>Demo UI state reseteado.</yellow>");
    }
}
