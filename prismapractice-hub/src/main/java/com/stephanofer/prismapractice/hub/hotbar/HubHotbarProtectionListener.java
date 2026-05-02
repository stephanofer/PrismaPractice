package com.stephanofer.prismapractice.hub.hotbar;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.Objects;

final class HubHotbarProtectionListener implements Listener {

    private final HubHotbarService hotbarService;

    HubHotbarProtectionListener(HubHotbarService hotbarService) {
        this.hotbarService = Objects.requireNonNull(hotbarService, "hotbarService");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (hotbarService.isStaffBypassEnabled(player)) {
            return;
        }
        AppliedHotbarProfile profile = hotbarService.findAppliedProfile(player).orElse(null);
        if (profile == null || !profile.constraints().denyMove()) {
            return;
        }

        boolean clickedBottom = event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory());
        boolean movingIntoBottom = event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getView().getTopInventory());
        boolean hotbarSwap = event.getClick() == ClickType.NUMBER_KEY
                || event.getClick() == ClickType.SWAP_OFFHAND
                || event.getAction() == InventoryAction.HOTBAR_SWAP;
        if (clickedBottom || movingIntoBottom || hotbarSwap) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (hotbarService.isStaffBypassEnabled(player)) {
            return;
        }
        AppliedHotbarProfile profile = hotbarService.findAppliedProfile(player).orElse(null);
        if (profile == null || !profile.constraints().denyMove()) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (hotbarService.isStaffBypassEnabled(event.getPlayer())) {
            return;
        }
        AppliedHotbarProfile profile = hotbarService.findAppliedProfile(event.getPlayer()).orElse(null);
        if (profile != null && profile.constraints().denyDrop()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapOffhand(PlayerSwapHandItemsEvent event) {
        if (hotbarService.isStaffBypassEnabled(event.getPlayer())) {
            return;
        }
        AppliedHotbarProfile profile = hotbarService.findAppliedProfile(event.getPlayer()).orElse(null);
        if (profile != null && profile.constraints().denySwapOffhand()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (hotbarService.isStaffBypassEnabled(event.getPlayer())) {
            return;
        }
        AppliedHotbarProfile profile = hotbarService.findAppliedProfile(event.getPlayer()).orElse(null);
        if (profile != null && profile.constraints().denyPlace()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (hotbarService.isStaffBypassEnabled(player)) {
            return;
        }
        AppliedHotbarProfile profile = hotbarService.findAppliedProfile(player).orElse(null);
        if (profile != null && profile.constraints().denyPickup()) {
            event.setCancelled(true);
        }
    }
}
