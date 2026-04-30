package com.stephanofer.prismapractice.paper.scoreboard;

import com.stephanofer.prismapractice.config.ConfigManager;
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
            ScoreboardContextProvider contextProvider,
            ScoreboardPlaceholderResolver placeholderResolver
    ) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(configManager, "configManager");

        PaperScoreboardConfig config = configManager.get(configId, PaperScoreboardConfig.class);
        try {
            return new PaperScoreboardService(plugin, ScoreboardLibrary.loadScoreboardLibrary(plugin), config, contextProvider, placeholderResolver);
        } catch (NoPacketAdapterAvailableException exception) {
            plugin.getLogger().warning("Scoreboard library packet adapter unavailable for this runtime. Falling back to no-op scoreboards.");
            return new PaperScoreboardService(plugin, new NoopScoreboardLibrary(), config, contextProvider, placeholderResolver);
        }
    }
}
