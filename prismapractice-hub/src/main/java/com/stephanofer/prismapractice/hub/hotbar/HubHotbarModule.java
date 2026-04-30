package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.hub.HubPracticeServices;
import com.stephanofer.prismapractice.hub.HubScoreboardModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class HubHotbarModule {

    private final HubHotbarService hotbarService;
    private final HubHotbarActionDispatcher actionDispatcher;
    private final Listener protectionListener;
    private final Listener interactionListener;

    private HubHotbarModule(
            HubHotbarService hotbarService,
            HubHotbarActionDispatcher actionDispatcher,
            Listener protectionListener,
            Listener interactionListener
    ) {
        this.hotbarService = hotbarService;
        this.actionDispatcher = actionDispatcher;
        this.protectionListener = protectionListener;
        this.interactionListener = interactionListener;
    }

    public static HubHotbarModule create(JavaPlugin plugin, ConfigManager configManager, HubPracticeServices practiceServices, HubScoreboardModule scoreboardModule) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(configManager, "configManager");
        Objects.requireNonNull(practiceServices, "practiceServices");
        Objects.requireNonNull(scoreboardModule, "scoreboardModule");

        HubHotbarConfig config = configManager.get("hub-items", HubHotbarConfig.class);
        HubHotbarItemRegistry registry = new HubHotbarItemRegistry(plugin, config);
        HubPlayerHotbarContextService contextService = new HubPlayerHotbarContextService(practiceServices.playerStateService(), practiceServices.playerPartyIndexRepository());
        HubHotbarService hotbarService = new HubHotbarService(contextService, new HubHotbarProfileResolver(), registry);
        HubHotbarActionDispatcher dispatcher = new HubHotbarActionDispatcher(plugin, practiceServices.queueService(), new ZMenuHubHotbarMenuController(plugin), hotbarService, scoreboardModule);
        dispatcher.registerCustomHandler("layout-exit", (player, action) -> {
            player.sendMessage(Component.text("La edición de layout todavía no está conectada, pero la base del item ya quedó lista.", NamedTextColor.YELLOW));
            return false;
        });
        return new HubHotbarModule(
                hotbarService,
                dispatcher,
                new HubHotbarProtectionListener(hotbarService),
                new HubHotbarInteractionListener(hotbarService, dispatcher)
        );
    }

    public HubHotbarService hotbarService() {
        return hotbarService;
    }

    public Listener protectionListener() {
        return protectionListener;
    }

    public Listener interactionListener() {
        return interactionListener;
    }
}
