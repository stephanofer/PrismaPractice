package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.core.application.profile.ProfileService;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

final class HubPlayerLifecycleListener implements Listener {

    private final JavaPlugin plugin;
    private final ProfileService profileService;
    private final PlayerStateService playerStateService;
    private final HubHotbarService hotbarService;
    private final HubScoreboardModule scoreboardModule;

    HubPlayerLifecycleListener(JavaPlugin plugin, ProfileService profileService, PlayerStateService playerStateService, HubHotbarService hotbarService, HubScoreboardModule scoreboardModule) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.playerStateService = Objects.requireNonNull(playerStateService, "playerStateService");
        this.hotbarService = Objects.requireNonNull(hotbarService, "hotbarService");
        this.scoreboardModule = Objects.requireNonNull(scoreboardModule, "scoreboardModule");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            PlayerId playerId = new PlayerId(event.getPlayer().getUniqueId());
            profileService.ensureProfile(playerId, event.getPlayer().getName());
            scoreboardModule.warm(playerId);
            playerStateService.ensureOnlineHubState(playerId, plugin.getServer().getName(), RuntimeType.HUB);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                hotbarService.refresh(event.getPlayer(), true);
                scoreboardModule.scoreboardService().refresh(event.getPlayer(), true);
            });
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Failed to bootstrap practice state for player " + event.getPlayer().getName());
            exception.printStackTrace();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            PlayerId playerId = new PlayerId(event.getPlayer().getUniqueId());
            hotbarService.clear(event.getPlayer());
            scoreboardModule.clearUiFocus(playerId);
            scoreboardModule.scoreboardService().clear(event.getPlayer());
            playerStateService.markOffline(playerId);
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Failed to persist offline state for player " + event.getPlayer().getName());
            exception.printStackTrace();
        }
    }
}
