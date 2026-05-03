package com.stephanofer.prismapractice.hub.ui;

import com.stephanofer.prismapractice.hub.HubPracticeServices;
import com.stephanofer.prismapractice.hub.HubQueueFeedbackCoordinator;
import com.stephanofer.prismapractice.hub.HubScoreboardModule;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarService;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import fr.maxlego08.menu.api.ButtonManager;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.Supplier;

final class HubMenuButtonRegistrar {

    private final Plugin plugin;
    private final HubQueueMenuService queueMenuService;
    private final HubScoreboardModule scoreboardModule;
    private final PaperFeedbackService feedbackService;
    private final HubQueueFeedbackCoordinator queueFeedbackCoordinator;
    private final Supplier<HubHotbarService> hotbarServiceSupplier;
    private final Supplier<ZMenuUiService> menuUiServiceSupplier;

    HubMenuButtonRegistrar(
            Plugin plugin,
            HubPracticeServices practiceServices,
            HubScoreboardModule scoreboardModule,
            PaperFeedbackService feedbackService,
            HubQueueFeedbackCoordinator queueFeedbackCoordinator,
            Supplier<HubHotbarService> hotbarServiceSupplier,
            Supplier<ZMenuUiService> menuUiServiceSupplier
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(practiceServices, "practiceServices");
        this.queueMenuService = new HubQueueMenuService(
                practiceServices.queueRepository(),
                practiceServices.queueEntryRepository(),
                practiceServices.playerPresenceRepository(),
                practiceServices.queueService()
        );
        this.scoreboardModule = Objects.requireNonNull(scoreboardModule, "scoreboardModule");
        this.feedbackService = Objects.requireNonNull(feedbackService, "feedbackService");
        this.queueFeedbackCoordinator = Objects.requireNonNull(queueFeedbackCoordinator, "queueFeedbackCoordinator");
        this.hotbarServiceSupplier = Objects.requireNonNull(hotbarServiceSupplier, "hotbarServiceSupplier");
        this.menuUiServiceSupplier = Objects.requireNonNull(menuUiServiceSupplier, "menuUiServiceSupplier");
    }

    void register(ButtonManager buttonManager) {
        buttonManager.register(new QueueEntryButtonLoader(
                plugin,
                queueMenuService,
                scoreboardModule,
                feedbackService,
                queueFeedbackCoordinator,
                hotbarServiceSupplier,
                menuUiServiceSupplier
        ));
    }
}
