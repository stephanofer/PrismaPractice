package com.stephanofer.prismapractice.ffa;

import com.stephanofer.prismapractice.command.PaperCommandServiceContainer;
import com.stephanofer.prismapractice.command.PaperCommands;
import com.stephanofer.prismapractice.api.state.PlayerPresenceRepository;
import com.stephanofer.prismapractice.api.state.PlayerStateRepository;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;
import com.stephanofer.prismapractice.data.mysql.StorageRuntime;
import com.stephanofer.prismapractice.data.mysql.repository.MySqlProfileRepository;
import com.stephanofer.prismapractice.data.redis.RedisStorage;
import com.stephanofer.prismapractice.data.redis.repository.RedisPlayerPresenceRepository;
import com.stephanofer.prismapractice.data.redis.repository.RedisPlayerStateRepository;
import com.stephanofer.prismapractice.ffa.command.FfaCommandDefinitions;
import com.stephanofer.prismapractice.paper.scoreboard.DefaultScoreboardPlaceholderResolver;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardBootstrap;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardService;
import com.stephanofer.prismapractice.paper.scoreboard.PlayerScoreboardDataCache;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismaPracticeFfaPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MySqlStorage storage;
    private RedisStorage redisStorage;
    private PaperScoreboardService scoreboardService;

    @Override
    public void onEnable() {
        try {
            StorageRuntime runtime = FfaStorageBootstrap.bootstrap(getDataFolder().toPath(), getClassLoader(), message -> getLogger().info(message));
            this.configManager = runtime.configManager();
            this.storage = runtime.storage();
            this.redisStorage = runtime.redisStorage();

            PlayerStateRepository stateRepository = new RedisPlayerStateRepository(this.redisStorage);
            PlayerPresenceRepository presenceRepository = new RedisPlayerPresenceRepository(this.redisStorage);
            PlayerScoreboardDataCache dataCache = new PlayerScoreboardDataCache(new MySqlProfileRepository(this.storage));
            this.scoreboardService = PaperScoreboardBootstrap.create(
                    this,
                    this.configManager,
                    "ffa-scoreboards",
                    new FfaScoreboardContextProvider(stateRepository, presenceRepository, dataCache),
                    new DefaultScoreboardPlaceholderResolver()
            );
            Bukkit.getPluginManager().registerEvents(new FfaScoreboardListener(this, dataCache, this.scoreboardService), this);
        } catch (RuntimeException exception) {
            getLogger().severe("Failed to initialize PrismaPractice FFA storage. Disabling plugin.");
            exception.printStackTrace();
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
                .add(PaperScoreboardService.class, this.scoreboardService)
                .build(),
            FfaCommandDefinitions.create()
        );
    }

    @Override
    public void onDisable() {
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
