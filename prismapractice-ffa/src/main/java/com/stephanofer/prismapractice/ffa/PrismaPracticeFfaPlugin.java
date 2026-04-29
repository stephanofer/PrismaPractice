package com.stephanofer.prismapractice.ffa;

import com.stephanofer.prismapractice.command.PaperCommandServiceContainer;
import com.stephanofer.prismapractice.command.PaperCommands;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.ffa.command.FfaCommandDefinitions;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismaPracticeFfaPlugin extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = FfaDemoConfigBootstrap.bootstrap(getDataFolder().toPath(), getClassLoader(), message -> getLogger().info(message));
        PaperCommands.register(
            this,
            PaperCommandServiceContainer.builder()
                .add(JavaPlugin.class, this)
                .add(ConfigManager.class, this.configManager)
                .build(),
            FfaCommandDefinitions.create()
        );
    }
}
