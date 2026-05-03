package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Objects;

final class HubSafetyListener implements Listener {

    private final JavaPlugin plugin;
    private final HubLobbyService lobbyService;
    private final PlayerStateService playerStateService;
    private final HubQueueExitService queueExitService;
    private final HubHotbarService hotbarService;
    private final HubScoreboardModule scoreboardModule;

    HubSafetyListener(
            JavaPlugin plugin,
            HubLobbyService lobbyService,
            PlayerStateService playerStateService,
            HubQueueExitService queueExitService,
            HubHotbarService hotbarService,
            HubScoreboardModule scoreboardModule
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
        this.playerStateService = Objects.requireNonNull(playerStateService, "playerStateService");
        this.queueExitService = Objects.requireNonNull(queueExitService, "queueExitService");
        this.hotbarService = Objects.requireNonNull(hotbarService, "hotbarService");
        this.scoreboardModule = Objects.requireNonNull(scoreboardModule, "scoreboardModule");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        PlayerStatus status = playerStateService.findCurrentState(new PlayerId(player.getUniqueId()))
                .map(com.stephanofer.prismapractice.api.state.PlayerState::status)
                .orElse(null);
        if (status == PlayerStatus.HUB || status == PlayerStatus.IN_QUEUE) {
            event.setCancelled(true);
            if (player.getFoodLevel() < 20) {
                player.setFoodLevel(20);
            }
            player.setSaturation(Math.max(player.getSaturation(), 20.0F));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        queueExitService.leave(player, HubQueueExitCause.RESPAWN_SAFETY);
        lobbyService.lobbyLocation().ifPresent(event::setRespawnLocation);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setFireTicks(0);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            hotbarService.refresh(player, true);
            scoreboardModule.clearUiFocus(new PlayerId(player.getUniqueId()));
            scoreboardModule.scoreboardService().refresh(player, true);
        });
    }
}
