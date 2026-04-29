package com.stephanofer.prismapractice.match;

import com.stephanofer.prismapractice.command.PaperCommandServiceContainer;
import com.stephanofer.prismapractice.command.PaperCommands;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.match.command.MatchCommandDefinitions;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismaPracticeMatchPlugin extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = MatchDemoConfigBootstrap.bootstrap(getDataFolder().toPath(), getClassLoader(), message -> getLogger().info(message));
        PaperCommands.register(
            this,
            PaperCommandServiceContainer.builder()
                .add(JavaPlugin.class, this)
                .add(ConfigManager.class, this.configManager)
                .build(),
            MatchCommandDefinitions.create()
        );
    }
}
