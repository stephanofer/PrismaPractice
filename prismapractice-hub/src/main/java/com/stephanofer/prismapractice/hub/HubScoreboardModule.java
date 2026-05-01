package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.debug.DebugController;
import com.stephanofer.prismapractice.paper.scoreboard.DefaultScoreboardPlaceholderResolver;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardBootstrap;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardConfig;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardService;
import com.stephanofer.prismapractice.paper.scoreboard.PlayerScoreboardDataCache;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardUiFocus;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardUiStateService;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class HubScoreboardModule {

    private final ConfigManager configManager;
    private final PaperScoreboardService scoreboardService;
    private final ScoreboardUiStateService uiStateService;
    private final PlayerScoreboardDataCache dataCache;
    private final Listener listener;

    private HubScoreboardModule(ConfigManager configManager, PaperScoreboardService scoreboardService, ScoreboardUiStateService uiStateService, PlayerScoreboardDataCache dataCache, Listener listener) {
        this.configManager = configManager;
        this.scoreboardService = scoreboardService;
        this.uiStateService = uiStateService;
        this.dataCache = dataCache;
        this.listener = listener;
    }

    public static HubScoreboardModule create(JavaPlugin plugin, ConfigManager configManager, DebugController debugController, HubPracticeServices practiceServices) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(configManager, "configManager");
        Objects.requireNonNull(debugController, "debugController");
        Objects.requireNonNull(practiceServices, "practiceServices");

        PlayerScoreboardDataCache dataCache = new PlayerScoreboardDataCache(practiceServices.profileRepository());
        ScoreboardUiStateService uiStateService = new ScoreboardUiStateService();
        PaperScoreboardService scoreboardService = PaperScoreboardBootstrap.create(
                plugin,
                configManager,
                "hub-scoreboards",
                debugController,
                new HubScoreboardContextProvider(practiceServices, dataCache, uiStateService),
                new DefaultScoreboardPlaceholderResolver()
        );

        return new HubScoreboardModule(configManager, scoreboardService, uiStateService, dataCache, new HubScoreboardListener(uiStateService, scoreboardService));
    }

    public void warm(PlayerId playerId) {
        dataCache.warm(playerId);
    }

    public void setUiFocus(PlayerId playerId, ScoreboardUiFocus focus) {
        uiStateService.setFocus(playerId, focus);
    }

    public void clearUiFocus(PlayerId playerId) {
        uiStateService.clear(playerId);
    }

    public PaperScoreboardService scoreboardService() {
        return scoreboardService;
    }

    public Listener listener() {
        return listener;
    }

    public void reload() {
        scoreboardService.reload(configManager.get("hub-scoreboards", PaperScoreboardConfig.class));
    }

    public void close() {
        scoreboardService.close();
    }
}
