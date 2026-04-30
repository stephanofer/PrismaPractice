package com.stephanofer.prismapractice.ffa;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardService;
import com.stephanofer.prismapractice.paper.scoreboard.PlayerScoreboardDataCache;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

final class FfaScoreboardListener implements Listener {

    private final Plugin plugin;
    private final PlayerScoreboardDataCache dataCache;
    private final PaperScoreboardService scoreboardService;

    FfaScoreboardListener(Plugin plugin, PlayerScoreboardDataCache dataCache, PaperScoreboardService scoreboardService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.dataCache = Objects.requireNonNull(dataCache, "dataCache");
        this.scoreboardService = Objects.requireNonNull(scoreboardService, "scoreboardService");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerId playerId = new PlayerId(event.getPlayer().getUniqueId());
        dataCache.warm(playerId);
        event.getPlayer().getServer().getScheduler().runTask(plugin, () -> scoreboardService.refresh(event.getPlayer(), true));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dataCache.evict(new PlayerId(event.getPlayer().getUniqueId()));
        scoreboardService.clear(event.getPlayer());
    }
}
