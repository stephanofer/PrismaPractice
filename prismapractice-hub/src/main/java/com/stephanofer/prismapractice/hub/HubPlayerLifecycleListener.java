package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.core.application.profile.ProfileService;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
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

    HubPlayerLifecycleListener(JavaPlugin plugin, ProfileService profileService, PlayerStateService playerStateService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.playerStateService = Objects.requireNonNull(playerStateService, "playerStateService");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            PlayerId playerId = new PlayerId(event.getPlayer().getUniqueId());
            profileService.ensureProfile(playerId, event.getPlayer().getName());
            playerStateService.ensureOnlineHubState(playerId, plugin.getServer().getName(), RuntimeType.HUB);
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Failed to bootstrap practice state for player " + event.getPlayer().getName());
            exception.printStackTrace();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            playerStateService.markOffline(new PlayerId(event.getPlayer().getUniqueId()));
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Failed to persist offline state for player " + event.getPlayer().getName());
            exception.printStackTrace();
        }
    }
}
