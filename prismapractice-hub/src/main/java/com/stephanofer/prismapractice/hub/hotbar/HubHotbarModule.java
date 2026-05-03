package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.hub.HubPracticeServices;
import com.stephanofer.prismapractice.hub.HubQueueFeedbackCoordinator;
import com.stephanofer.prismapractice.hub.HubQueueExitService;
import com.stephanofer.prismapractice.hub.HubScoreboardModule;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class HubHotbarModule {

    private final ConfigManager configManager;
    private final HubHotbarItemRegistry registry;
    private final HubHotbarService hotbarService;
    private final HubStaffModeService staffModeService;
    private final HubHotbarActionDispatcher actionDispatcher;
    private final Listener protectionListener;
    private final Listener interactionListener;
    private final AtomicReference<HubQueueExitService> queueExitServiceReference;

    private HubHotbarModule(
            ConfigManager configManager,
            HubHotbarItemRegistry registry,
            HubHotbarService hotbarService,
            HubStaffModeService staffModeService,
            HubHotbarActionDispatcher actionDispatcher,
            Listener protectionListener,
            Listener interactionListener,
            AtomicReference<HubQueueExitService> queueExitServiceReference
    ) {
        this.configManager = configManager;
        this.registry = registry;
        this.hotbarService = hotbarService;
        this.staffModeService = staffModeService;
        this.actionDispatcher = actionDispatcher;
        this.protectionListener = protectionListener;
        this.interactionListener = interactionListener;
        this.queueExitServiceReference = queueExitServiceReference;
    }

    public static HubHotbarModule create(JavaPlugin plugin, ConfigManager configManager, HubPracticeServices practiceServices, HubScoreboardModule scoreboardModule, ZMenuUiService menuUiService, PaperFeedbackService feedbackService, HubQueueFeedbackCoordinator queueFeedbackCoordinator) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(configManager, "configManager");
        Objects.requireNonNull(practiceServices, "practiceServices");
        Objects.requireNonNull(scoreboardModule, "scoreboardModule");
        Objects.requireNonNull(menuUiService, "menuUiService");
        Objects.requireNonNull(feedbackService, "feedbackService");
        Objects.requireNonNull(queueFeedbackCoordinator, "queueFeedbackCoordinator");

        HubHotbarConfig config = configManager.get("hub-items", HubHotbarConfig.class);
        HubHotbarItemRegistry registry = new HubHotbarItemRegistry(plugin, config);
        HubPlayerHotbarContextService contextService = new HubPlayerHotbarContextService(practiceServices.playerStateService(), practiceServices.playerPartyIndexRepository());
        HubStaffModeService staffModeService = new HubStaffModeService();
        HubHotbarService hotbarService = new HubHotbarService(contextService, new HubHotbarProfileResolver(), registry, practiceServices.queueEntryRepository(), staffModeService);
        AtomicReference<HubQueueExitService> queueExitServiceReference = new AtomicReference<>();
        HubHotbarActionDispatcher dispatcher = new HubHotbarActionDispatcher(plugin, practiceServices.queueService(), new ZMenuHubHotbarMenuController(menuUiService), hotbarService, scoreboardModule, feedbackService, queueFeedbackCoordinator, queueExitServiceReference::get);
        dispatcher.registerCustomHandler("layout-exit", (player, action) -> {
            player.sendMessage(Component.text("La edición de layout todavía no está conectada, pero la base del item ya quedó lista.", NamedTextColor.YELLOW));
            return false;
        });
        dispatcher.registerCustomHandler("queue-context-source", (player, action) -> {
            com.stephanofer.prismapractice.api.queue.QueueEntry activeEntry = practiceServices.queueEntryRepository().findByPlayerId(new com.stephanofer.prismapractice.api.common.PlayerId(player.getUniqueId())).orElse(null);
            if (activeEntry == null) {
                return false;
            }
            String sourceItemKey = null;
            for (String argument : action.arguments()) {
                int separator = argument.indexOf('=');
                if (separator <= 0 || separator >= argument.length() - 1) {
                    continue;
                }
                String key = argument.substring(0, separator).trim();
                if (key.equalsIgnoreCase(activeEntry.queueType().name())) {
                    sourceItemKey = argument.substring(separator + 1).trim();
                    break;
                }
            }
            if (sourceItemKey == null || sourceItemKey.isBlank()) {
                return false;
            }
            HubCompiledItem sourceItem = registry.findItemByKey(sourceItemKey).orElse(null);
            if (sourceItem == null || sourceItem.action().type() != HubHotbarActionType.OPEN_MENU) {
                return false;
            }
            return dispatcher.dispatch(player, sourceItem.action());
        });
        return new HubHotbarModule(
                configManager,
                registry,
                hotbarService,
                staffModeService,
                dispatcher,
                new HubHotbarProtectionListener(hotbarService),
                new HubHotbarInteractionListener(hotbarService, dispatcher),
                queueExitServiceReference
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

    public HubStaffModeService staffModeService() {
        return staffModeService;
    }

    public void bindQueueExitService(HubQueueExitService queueExitService) {
        this.queueExitServiceReference.set(Objects.requireNonNull(queueExitService, "queueExitService"));
    }

    public void reload() {
        registry.reload(configManager.get("hub-items", HubHotbarConfig.class));
        Bukkit.getOnlinePlayers().forEach(player -> hotbarService.refresh(player, true));
    }
}
