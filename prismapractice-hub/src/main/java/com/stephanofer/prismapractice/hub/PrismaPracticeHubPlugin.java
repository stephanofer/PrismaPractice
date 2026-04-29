package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.command.PaperCommandServiceContainer;
import com.stephanofer.prismapractice.command.PaperCommands;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.hub.command.HubCommandDefinitions;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismaPracticeHubPlugin extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = HubDemoConfigBootstrap.bootstrap(getDataFolder().toPath(), getClassLoader(), message -> getLogger().info(message));
        PaperCommands.register(
            this,
            PaperCommandServiceContainer.builder()
                .add(JavaPlugin.class, this)
                .add(ConfigManager.class, this.configManager)
                .build(),
            HubCommandDefinitions.create()
        );
    }
}
