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
import com.stephanofer.prismapractice.hub.command.HubCommandDefinitions;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismaPracticeHubPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MySqlStorage storage;
    private RedisStorage redisStorage;
    private HubPracticeServices practiceServices;

    @Override
    public void onEnable() {
        try {
            StorageRuntime runtime = HubStorageBootstrap.bootstrap(getDataFolder().toPath(), getClassLoader(), message -> getLogger().info(message));
            this.configManager = runtime.configManager();
            this.storage = runtime.storage();
            this.redisStorage = runtime.redisStorage();
            this.practiceServices = HubPracticeServicesFactory.create(this.storage, this.redisStorage);
        } catch (RuntimeException exception) {
            getLogger().severe("Failed to initialize PrismaPractice Hub storage. Disabling plugin.");
            exception.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(
                new HubPlayerLifecycleListener(this, this.practiceServices.profileService(), this.practiceServices.playerStateService()),
                this
        );

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
        if (this.storage != null) {
            this.storage.close();
        }
        if (this.redisStorage != null) {
            this.redisStorage.close();
        }
    }
}
