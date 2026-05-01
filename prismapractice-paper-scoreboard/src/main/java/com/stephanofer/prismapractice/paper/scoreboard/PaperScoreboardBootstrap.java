package com.stephanofer.prismapractice.paper.scoreboard;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.debug.DebugCategories;
import com.stephanofer.prismapractice.debug.DebugController;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class PaperScoreboardBootstrap {

    private PaperScoreboardBootstrap() {
    }

    public static PaperScoreboardService create(
            Plugin plugin,
            ConfigManager configManager,
            String configId,
            DebugController debug,
            ScoreboardContextProvider contextProvider,
            ScoreboardPlaceholderResolver placeholderResolver
    ) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(configManager, "configManager");
        Objects.requireNonNull(debug, "debug");

        PaperScoreboardConfig config = configManager.get(configId, PaperScoreboardConfig.class);
        try {
            return new PaperScoreboardService(plugin, ScoreboardLibrary.loadScoreboardLibrary(plugin), config, debug, contextProvider, placeholderResolver);
        } catch (NoPacketAdapterAvailableException exception) {
            debug.warn(DebugCategories.SCOREBOARD, "scoreboard.bootstrap.no-adapter", "Scoreboard library packet adapter unavailable. Falling back to no-op scoreboards.", debug.context().build());
            return new PaperScoreboardService(plugin, new NoopScoreboardLibrary(), config, debug, contextProvider, placeholderResolver);
        }
    }
}
