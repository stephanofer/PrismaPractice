package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.command.PaperCommandServiceContainer;
import com.stephanofer.prismapractice.command.PaperCommands;
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
import com.stephanofer.prismapractice.feedback.FeedbackConfig;
import com.stephanofer.prismapractice.hub.command.HubCommandDefinitions;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarModule;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarService;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardService;
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

    @Override
    public void onEnable() {
        try {
            StorageRuntime runtime = HubStorageBootstrap.bootstrap(getDataFolder().toPath(), getClassLoader(), message -> getLogger().info(message));
            this.configManager = runtime.configManager();
            this.storage = runtime.storage();
            this.redisStorage = runtime.redisStorage();
            this.practiceServices = HubPracticeServicesFactory.create(this.storage, this.redisStorage);
            this.feedbackService = new PaperFeedbackService(this, this.configManager.get("hub-feedback", FeedbackConfig.class));
            this.scoreboardModule = HubScoreboardModule.create(this, this.configManager, this.practiceServices);
            this.hotbarModule = HubHotbarModule.create(this, this.configManager, this.practiceServices, this.scoreboardModule);
        } catch (RuntimeException exception) {
            getLogger().severe("Failed to initialize PrismaPractice Hub storage. Disabling plugin.");
            exception.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(
                new HubPlayerLifecycleListener(this, this.practiceServices.profileService(), this.practiceServices.playerStateService(), this.hotbarModule.hotbarService(), this.scoreboardModule, this.feedbackService),
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

        PaperCommands.register(
            this,
            PaperCommandServiceContainer.builder()
                .add(JavaPlugin.class, this)
                .add(ConfigManager.class, this.configManager)
                .add(MySqlStorage.class, this.storage)
                .add(RedisStorage.class, this.redisStorage)
                .add(ProfileService.class, this.practiceServices.profileService())
                .add(PlayerStateService.class, this.practiceServices.playerStateService())
                .add(QueueService.class, this.practiceServices.queueService())
                .add(HubHotbarService.class, this.hotbarModule.hotbarService())
                .add(PaperFeedbackService.class, this.feedbackService)
                .add(PaperScoreboardService.class, this.scoreboardModule.scoreboardService())
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

    @Override
    public void onDisable() {
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
