package com.stephanofer.prismapractice.hub.hotbar;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;

final class HubHotbarInteractionListener implements Listener {

    private final HubHotbarService hotbarService;
    private final HubHotbarActionDispatcher actionDispatcher;

    HubHotbarInteractionListener(HubHotbarService hotbarService, HubHotbarActionDispatcher actionDispatcher) {
        this.hotbarService = Objects.requireNonNull(hotbarService, "hotbarService");
        this.actionDispatcher = Objects.requireNonNull(actionDispatcher, "actionDispatcher");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() == org.bukkit.event.block.Action.PHYSICAL) {
            return;
        }
        HubCompiledItem item = hotbarService.resolveManagedItem(event.getItem()).orElse(null);
        if (item == null || !item.action().trigger().matches(event.getAction())) {
            return;
        }

        event.setCancelled(true);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        actionDispatcher.dispatch(event.getPlayer(), item.action());
    }
}
