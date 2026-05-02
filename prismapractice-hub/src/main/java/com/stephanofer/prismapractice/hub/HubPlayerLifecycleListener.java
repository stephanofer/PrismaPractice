package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.core.application.profile.ProfileService;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
import com.stephanofer.prismapractice.debug.DebugCategories;
import com.stephanofer.prismapractice.debug.DebugController;
import com.stephanofer.prismapractice.debug.DebugDetailLevel;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarService;
import com.stephanofer.prismapractice.hub.hotbar.HubStaffModeService;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
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
    private final HubStaffModeService staffModeService;
    private final HubScoreboardModule scoreboardModule;
    private final PaperFeedbackService feedbackService;
    private final DebugController debug;

    HubPlayerLifecycleListener(JavaPlugin plugin, ProfileService profileService, PlayerStateService playerStateService, HubHotbarService hotbarService, HubStaffModeService staffModeService, HubScoreboardModule scoreboardModule, PaperFeedbackService feedbackService, DebugController debug) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.playerStateService = Objects.requireNonNull(playerStateService, "playerStateService");
        this.hotbarService = Objects.requireNonNull(hotbarService, "hotbarService");
        this.staffModeService = Objects.requireNonNull(staffModeService, "staffModeService");
        this.scoreboardModule = Objects.requireNonNull(scoreboardModule, "scoreboardModule");
        this.feedbackService = Objects.requireNonNull(feedbackService, "feedbackService");
        this.debug = Objects.requireNonNull(debug, "debug");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            PlayerId playerId = new PlayerId(event.getPlayer().getUniqueId());
            debug.debug(DebugCategories.PLAYER_LIFECYCLE, DebugDetailLevel.BASIC, "player.join.started", "Bootstrapping player join state", playerContext(event));
            profileService.ensureProfile(playerId, event.getPlayer().getName());
            scoreboardModule.warm(playerId);
            playerStateService.ensureOnlineHubState(playerId, plugin.getServer().getName(), RuntimeType.HUB);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                hotbarService.refresh(event.getPlayer(), true);
                scoreboardModule.scoreboardService().refresh(event.getPlayer(), true);
            });
            debug.debug(DebugCategories.PLAYER_LIFECYCLE, DebugDetailLevel.BASIC, "player.join.completed", "Player join state bootstrapped", playerContext(event));
        } catch (RuntimeException exception) {
            debug.error(DebugCategories.PLAYER_LIFECYCLE, "player.join.failed", "Failed to bootstrap practice state for player", playerContext(event), exception);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            PlayerId playerId = new PlayerId(event.getPlayer().getUniqueId());
            debug.debug(DebugCategories.PLAYER_LIFECYCLE, DebugDetailLevel.BASIC, "player.quit.started", "Persisting player quit state", playerContext(event));
            staffModeService.disable(event.getPlayer());
            hotbarService.clear(event.getPlayer());
            feedbackService.clear(event.getPlayer());
            scoreboardModule.clearUiFocus(playerId);
            scoreboardModule.scoreboardService().clear(event.getPlayer());
            playerStateService.markOffline(playerId);
            debug.debug(DebugCategories.PLAYER_LIFECYCLE, DebugDetailLevel.BASIC, "player.quit.completed", "Player quit state persisted", playerContext(event));
        } catch (RuntimeException exception) {
            debug.error(DebugCategories.PLAYER_LIFECYCLE, "player.quit.failed", "Failed to persist offline state for player", playerContext(event), exception);
        }
    }

    private com.stephanofer.prismapractice.debug.DebugContext playerContext(org.bukkit.event.player.PlayerEvent event) {
        return debug.context().player(event.getPlayer().getUniqueId().toString(), event.getPlayer().getName()).build();
    }
}
