package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.hub.HubPracticeServices;
import com.stephanofer.prismapractice.hub.HubScoreboardModule;
import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class HubHotbarModule {

    private final ConfigManager configManager;
    private final HubHotbarItemRegistry registry;
    private final HubHotbarService hotbarService;
    private final HubHotbarActionDispatcher actionDispatcher;
    private final Listener protectionListener;
    private final Listener interactionListener;

    private HubHotbarModule(
            ConfigManager configManager,
            HubHotbarItemRegistry registry,
            HubHotbarService hotbarService,
            HubHotbarActionDispatcher actionDispatcher,
            Listener protectionListener,
            Listener interactionListener
    ) {
        this.configManager = configManager;
        this.registry = registry;
        this.hotbarService = hotbarService;
        this.actionDispatcher = actionDispatcher;
        this.protectionListener = protectionListener;
        this.interactionListener = interactionListener;
    }

    public static HubHotbarModule create(JavaPlugin plugin, ConfigManager configManager, HubPracticeServices practiceServices, HubScoreboardModule scoreboardModule, ZMenuUiService menuUiService) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(configManager, "configManager");
        Objects.requireNonNull(practiceServices, "practiceServices");
        Objects.requireNonNull(scoreboardModule, "scoreboardModule");
        Objects.requireNonNull(menuUiService, "menuUiService");

        HubHotbarConfig config = configManager.get("hub-items", HubHotbarConfig.class);
        HubHotbarItemRegistry registry = new HubHotbarItemRegistry(plugin, config);
        HubPlayerHotbarContextService contextService = new HubPlayerHotbarContextService(practiceServices.playerStateService(), practiceServices.playerPartyIndexRepository());
        HubHotbarService hotbarService = new HubHotbarService(contextService, new HubHotbarProfileResolver(), registry);
        HubHotbarActionDispatcher dispatcher = new HubHotbarActionDispatcher(plugin, practiceServices.queueService(), new ZMenuHubHotbarMenuController(menuUiService), hotbarService, scoreboardModule);
        dispatcher.registerCustomHandler("layout-exit", (player, action) -> {
            player.sendMessage(Component.text("La edición de layout todavía no está conectada, pero la base del item ya quedó lista.", NamedTextColor.YELLOW));
            return false;
        });
        return new HubHotbarModule(
                configManager,
                registry,
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

    public void reload() {
        registry.reload(configManager.get("hub-items", HubHotbarConfig.class));
        Bukkit.getOnlinePlayers().forEach(player -> hotbarService.refresh(player, true));
    }
}
