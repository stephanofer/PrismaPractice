package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.command.PaperCommandServiceContainer;
import com.stephanofer.prismapractice.command.PaperCommands;
import com.stephanofer.prismapractice.command.ReloadCoordinator;
import com.stephanofer.prismapractice.command.ReloadResult;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.core.application.arena.ArenaAllocationService;
import com.stephanofer.prismapractice.core.application.history.HistoryService;
import com.stephanofer.prismapractice.core.application.leaderboard.LeaderboardProjectionService;
import com.stephanofer.prismapractice.core.application.match.MatchService;
import com.stephanofer.prismapractice.core.application.matchmaking.MatchmakingService;
import com.stephanofer.prismapractice.core.application.rating.RatingService;
import com.stephanofer.prismapractice.core.application.profile.ProfileService;
import com.stephanofer.prismapractice.core.application.queue.QueueService;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;
import com.stephanofer.prismapractice.data.mysql.StorageRuntime;
import com.stephanofer.prismapractice.data.redis.RedisStorage;
import com.stephanofer.prismapractice.debug.DebugConfig;
import com.stephanofer.prismapractice.debug.DebugCategories;
import com.stephanofer.prismapractice.debug.DebugConsoleSink;
import com.stephanofer.prismapractice.debug.DebugController;
import com.stephanofer.prismapractice.debug.DebugDetailLevel;
import com.stephanofer.prismapractice.feedback.FeedbackConfig;
import com.stephanofer.prismapractice.hub.command.HubCommandDefinitions;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarModule;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarService;
import com.stephanofer.prismapractice.hub.hotbar.HubStaffModeService;
import com.stephanofer.prismapractice.hub.ui.HubUiModule;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardService;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogService;
import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismaPracticeHubPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MySqlStorage storage;
    private RedisStorage redisStorage;
    private HubPracticeServices practiceServices;
    private HubHotbarModule hotbarModule;
    private HubScoreboardModule scoreboardModule;
    private PaperFeedbackService feedbackService;
    private HubUiModule uiModule;
    private ReloadCoordinator reloadCoordinator;
    private DebugController debugController;

    @Override
    public void onEnable() {
        try {
            StorageRuntime runtime = HubStorageBootstrap.bootstrap(getDataFolder().toPath(), getClassLoader(), message -> DebugConsoleSink.jul(getLogger()).info(message));
            this.configManager = runtime.configManager();
            this.storage = runtime.storage();
            this.redisStorage = runtime.redisStorage();
            this.debugController = runtime.debugController();
            this.practiceServices = HubPracticeServicesFactory.create(this.storage, this.redisStorage);
            this.feedbackService = new PaperFeedbackService(this, this.configManager.get("hub-feedback", FeedbackConfig.class));
            this.scoreboardModule = HubScoreboardModule.create(this, this.configManager, this.debugController, this.practiceServices);
            this.uiModule = HubUiModule.create(this);
            this.hotbarModule = HubHotbarModule.create(this, this.configManager, this.practiceServices, this.scoreboardModule, this.uiModule.menuService());
            this.reloadCoordinator = createReloadCoordinator();
            this.debugController.info(DebugCategories.BOOTSTRAP, DebugDetailLevel.BASIC, "plugin.enable.completed", "Hub plugin initialized", this.debugController.context().build());
        } catch (RuntimeException exception) {
            if (this.debugController != null) {
                this.debugController.error(DebugCategories.BOOTSTRAP, "plugin.enable.failed", "Failed to initialize PrismaPractice Hub storage. Disabling plugin.", this.debugController.context().build(), exception);
            } else {
                getLogger().log(java.util.logging.Level.SEVERE, "Failed to initialize PrismaPractice Hub storage. Disabling plugin.", exception);
            }
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(
                new HubPlayerLifecycleListener(this, this.practiceServices.profileService(), this.practiceServices.playerStateService(), this.hotbarModule.hotbarService(), this.hotbarModule.staffModeService(), this.scoreboardModule, this.feedbackService, this.debugController),
                this
        );
        Bukkit.getPluginManager().registerEvents(
                this.hotbarModule.protectionListener(),
                this
        );
        Bukkit.getPluginManager().registerEvents(
                this.hotbarModule.interactionListener(),
                this
        );
        Bukkit.getPluginManager().registerEvents(this.scoreboardModule.listener(), this);
        Bukkit.getPluginManager().registerEvents(this.uiModule.dialogListener(), this);

        PaperCommands.register(
            this,
            PaperCommandServiceContainer.builder()
                .add(JavaPlugin.class, this)
                .add(ConfigManager.class, this.configManager)
                .add(MySqlStorage.class, this.storage)
                .add(RedisStorage.class, this.redisStorage)
                .add(DebugController.class, this.debugController)
                .add(ProfileService.class, this.practiceServices.profileService())
                .add(PlayerStateService.class, this.practiceServices.playerStateService())
                .add(QueueService.class, this.practiceServices.queueService())
                .add(HubHotbarService.class, this.hotbarModule.hotbarService())
                .add(HubStaffModeService.class, this.hotbarModule.staffModeService())
                .add(HubUiModule.class, this.uiModule)
                .add(ZMenuUiService.class, this.uiModule.menuService())
                .add(PaperDialogService.class, this.uiModule.dialogService())
                .add(PaperFeedbackService.class, this.feedbackService)
                .add(PaperScoreboardService.class, this.scoreboardModule.scoreboardService())
                .add(ReloadCoordinator.class, this.reloadCoordinator)
                .add(MatchmakingService.class, this.practiceServices.matchmakingService())
                .add(ArenaAllocationService.class, this.practiceServices.arenaAllocationService())
                .add(MatchService.class, this.practiceServices.matchService())
                .add(RatingService.class, this.practiceServices.ratingService())
                .add(HistoryService.class, this.practiceServices.historyService())
                .add(LeaderboardProjectionService.class, this.practiceServices.leaderboardProjectionService())
                .build(),
            HubCommandDefinitions.create()
        );
    }

    private ReloadCoordinator createReloadCoordinator() {
        // Hot reload stays intentionally scoped to operational content/config. Anything that changes
        // command trees, listeners, connection pools or bootstrap wiring still requires full restart.
        return new ReloadCoordinator()
                .register("config", "base runtime config", () -> {
                    this.configManager.reloadAll();
                    this.debugController.reload(this.configManager.get("runtime-debug", DebugConfig.class));
                    this.debugController.info(DebugCategories.RELOAD, DebugDetailLevel.BASIC, "reload.config.completed", "Base runtime config reloaded", this.debugController.context().build());
                    return ReloadResult.of("Configuraciones base recargadas.");
                })
                .register("feedback", "feedback templates", java.util.List.of("config"), () -> {
                    this.feedbackService.reload(this.configManager.get("hub-feedback", FeedbackConfig.class));
                    this.debugController.info(DebugCategories.RELOAD, DebugDetailLevel.BASIC, "reload.feedback.completed", "Feedback templates reloaded", this.debugController.context().build());
                    return ReloadResult.of("Feedback recargado y estados persistentes limpiados.");
                })
                .register("scoreboard", "hub scoreboard", java.util.List.of("config"), () -> {
                    this.scoreboardModule.reload();
                    this.debugController.info(DebugCategories.RELOAD, DebugDetailLevel.BASIC, "reload.scoreboard.completed", "Hub scoreboard reloaded", this.debugController.context().build());
                    return ReloadResult.of("Scoreboard recargado para jugadores online.");
                })
                .register("hotbar", "hub hotbar", java.util.List.of("config"), () -> {
                    this.hotbarModule.reload();
                    this.debugController.info(DebugCategories.RELOAD, DebugDetailLevel.BASIC, "reload.hotbar.completed", "Hub hotbar reloaded", this.debugController.context().build());
                    return ReloadResult.of("Hotbar recargada y reaplicada a jugadores online.");
                })
                .register("ui", "hub menus and dialogs", () -> {
                    this.uiModule.dialogService().reload();
                    if (this.uiModule.menuService().isAvailable()) {
                        this.uiModule.menuService().reload();
                    }
                    this.debugController.info(DebugCategories.RELOAD, DebugDetailLevel.BASIC, "reload.ui.completed", "Hub UI reloaded", this.debugController.context().build());
                    return ReloadResult.of("UI recargada.");
                });
    }

    @Override
    public void onDisable() {
        if (this.debugController != null) {
            this.debugController.info(DebugCategories.BOOTSTRAP, DebugDetailLevel.BASIC, "plugin.disable.started", "Hub plugin shutting down", this.debugController.context().build());
        }
        if (this.uiModule != null) {
            this.uiModule.close();
        }
        if (this.feedbackService != null) {
            this.feedbackService.close();
        }
        if (this.scoreboardModule != null) {
            this.scoreboardModule.close();
        }
        if (this.storage != null) {
            this.storage.close();
        }
        if (this.redisStorage != null) {
            this.redisStorage.close();
        }
    }
}
