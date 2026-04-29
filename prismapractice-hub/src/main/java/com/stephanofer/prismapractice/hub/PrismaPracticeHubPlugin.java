package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.command.PaperCommandServiceContainer;
import com.stephanofer.prismapractice.command.PaperCommands;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;
import com.stephanofer.prismapractice.data.mysql.StorageRuntime;
import com.stephanofer.prismapractice.hub.command.HubCommandDefinitions;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismaPracticeHubPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MySqlStorage storage;

    @Override
    public void onEnable() {
        try {
            StorageRuntime runtime = HubStorageBootstrap.bootstrap(getDataFolder().toPath(), getClassLoader(), message -> getLogger().info(message));
            this.configManager = runtime.configManager();
            this.storage = runtime.storage();
        } catch (RuntimeException exception) {
            getLogger().severe("Failed to initialize PrismaPractice Hub storage. Disabling plugin.");
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
                .build(),
            HubCommandDefinitions.create()
        );
    }

    @Override
    public void onDisable() {
        if (this.storage != null) {
            this.storage.close();
        }
    }
}
