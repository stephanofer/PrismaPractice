package com.stephanofer.prismapractice.match;

import com.stephanofer.prismapractice.command.PaperCommandServiceContainer;
import com.stephanofer.prismapractice.command.PaperCommands;
import com.stephanofer.prismapractice.command.ReloadCoordinator;
import com.stephanofer.prismapractice.command.ReloadResult;
import com.stephanofer.prismapractice.api.state.PlayerPresenceRepository;
import com.stephanofer.prismapractice.api.state.PlayerStateRepository;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;
import com.stephanofer.prismapractice.data.mysql.StorageRuntime;
import com.stephanofer.prismapractice.data.mysql.repository.MySqlProfileRepository;
import com.stephanofer.prismapractice.data.redis.RedisStorage;
import com.stephanofer.prismapractice.data.redis.repository.RedisPlayerPresenceRepository;
import com.stephanofer.prismapractice.data.redis.repository.RedisPlayerStateRepository;
import com.stephanofer.prismapractice.debug.DebugConfig;
import com.stephanofer.prismapractice.debug.DebugCategories;
import com.stephanofer.prismapractice.debug.DebugConsoleSink;
import com.stephanofer.prismapractice.debug.DebugController;
import com.stephanofer.prismapractice.debug.DebugDetailLevel;
import com.stephanofer.prismapractice.match.command.MatchCommandDefinitions;
import com.stephanofer.prismapractice.paper.scoreboard.DefaultScoreboardPlaceholderResolver;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardBootstrap;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardService;
import com.stephanofer.prismapractice.paper.scoreboard.PlayerScoreboardDataCache;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismaPracticeMatchPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MySqlStorage storage;
    private RedisStorage redisStorage;
    private PaperScoreboardService scoreboardService;
    private ReloadCoordinator reloadCoordinator;
    private DebugController debugController;

    @Override
    public void onEnable() {
        try {
            StorageRuntime runtime = MatchStorageBootstrap.bootstrap(getDataFolder().toPath(), getClassLoader(), message -> DebugConsoleSink.jul(getLogger()).info(message));
            this.configManager = runtime.configManager();
            this.storage = runtime.storage();
            this.redisStorage = runtime.redisStorage();
            this.debugController = runtime.debugController();

            PlayerStateRepository stateRepository = new RedisPlayerStateRepository(this.redisStorage);
            PlayerPresenceRepository presenceRepository = new RedisPlayerPresenceRepository(this.redisStorage);
            PlayerScoreboardDataCache dataCache = new PlayerScoreboardDataCache(new MySqlProfileRepository(this.storage));
            this.scoreboardService = PaperScoreboardBootstrap.create(
                    this,
                    this.configManager,
                    "match-scoreboards",
                    this.debugController,
                    new MatchScoreboardContextProvider(stateRepository, presenceRepository, dataCache),
                    new DefaultScoreboardPlaceholderResolver()
            );
            Bukkit.getPluginManager().registerEvents(new MatchScoreboardListener(this, dataCache, this.scoreboardService), this);
            this.reloadCoordinator = createReloadCoordinator();
            this.debugController.info(DebugCategories.BOOTSTRAP, DebugDetailLevel.BASIC, "plugin.enable.completed", "Match plugin initialized", this.debugController.context().build());
        } catch (RuntimeException exception) {
            if (this.debugController != null) {
                this.debugController.error(DebugCategories.BOOTSTRAP, "plugin.enable.failed", "Failed to initialize PrismaPractice Match storage. Disabling plugin.", this.debugController.context().build(), exception);
            } else {
                getLogger().log(java.util.logging.Level.SEVERE, "Failed to initialize PrismaPractice Match storage. Disabling plugin.", exception);
            }
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        PaperCommands.register(
            this,
            PaperCommandServiceContainer.builder()
                .add(JavaPlugin.class, this)
                .add(ConfigManager.class, this.configManager)
                .add(MySqlStorage.class, this.storage)
                .add(RedisStorage.class, this.redisStorage)
                .add(DebugController.class, this.debugController)
                .add(PaperScoreboardService.class, this.scoreboardService)
                .add(ReloadCoordinator.class, this.reloadCoordinator)
                .build(),
            MatchCommandDefinitions.create()
        );
    }

    private ReloadCoordinator createReloadCoordinator() {
        return new ReloadCoordinator()
                .register("config", "base runtime config", () -> {
                    this.configManager.reloadAll();
                    this.debugController.reload(this.configManager.get("runtime-debug", DebugConfig.class));
                    this.debugController.info(DebugCategories.RELOAD, DebugDetailLevel.BASIC, "reload.config.completed", "Base runtime config reloaded", this.debugController.context().build());
                    return ReloadResult.of("Configuraciones base recargadas.");
                })
                .register("scoreboard", "match scoreboard", java.util.List.of("config"), () -> {
                    this.scoreboardService.reload(this.configManager.get("match-scoreboards", com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardConfig.class));
                    this.debugController.info(DebugCategories.RELOAD, DebugDetailLevel.BASIC, "reload.scoreboard.completed", "Match scoreboard reloaded", this.debugController.context().build());
                    return ReloadResult.of("Scoreboard de match recargado para jugadores online.");
                });
    }

    @Override
    public void onDisable() {
        if (this.debugController != null) {
            this.debugController.info(DebugCategories.BOOTSTRAP, DebugDetailLevel.BASIC, "plugin.disable.started", "Match plugin shutting down", this.debugController.context().build());
        }
        if (this.scoreboardService != null) {
            this.scoreboardService.close();
        }
        if (this.storage != null) {
            this.storage.close();
        }
        if (this.redisStorage != null) {
            this.redisStorage.close();
        }
    }
}
