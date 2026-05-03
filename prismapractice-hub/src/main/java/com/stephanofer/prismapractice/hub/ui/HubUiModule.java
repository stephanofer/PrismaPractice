package com.stephanofer.prismapractice.hub.ui;

import com.stephanofer.prismapractice.hub.HubPracticeServices;
import com.stephanofer.prismapractice.hub.HubQueueFeedbackCoordinator;
import com.stephanofer.prismapractice.hub.HubScoreboardModule;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarService;
import com.stephanofer.prismapractice.paper.ui.demo.DemoMenuButtonRegistrar;
import com.stephanofer.prismapractice.paper.ui.demo.DemoMenuStateStore;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogService;
import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class HubUiModule {

    private static final List<String> BUNDLED_UI_RESOURCES = List.of(
            "defaults/ui/patterns/demo-back.yml",
            "defaults/ui/patterns/demo-pagination.yml",
            "defaults/patterns/queue-entry.yml",
            "defaults/ui/patterns/queue-back.yml",
            "defaults/ui/patterns/queue-frame.yml",
            "defaults/ui/inventories/demo-main.yml",
            "defaults/ui/inventories/demo-dynamic.yml",
            "defaults/ui/inventories/ranked.yml",
            "defaults/ui/inventories/unranked.yml",
            "defaults/ui/dialogs/demo-notice.yml",
            "defaults/ui/dialogs/demo-confirmation.yml",
            "defaults/ui/dialogs/demo-multi-action.yml",
            "defaults/ui/dialogs/demo-configurator.yml",
            "defaults/ui/dialogs/demo-review.yml"
    );

    private final ZMenuUiService menuService;
    private final PaperDialogService dialogService;
    private final DemoMenuStateStore demoStateStore;
    private final AtomicReference<HubHotbarService> hotbarServiceReference;

    private HubUiModule(ZMenuUiService menuService, PaperDialogService dialogService, DemoMenuStateStore demoStateStore, AtomicReference<HubHotbarService> hotbarServiceReference) {
        this.menuService = menuService;
        this.dialogService = dialogService;
        this.demoStateStore = demoStateStore;
        this.hotbarServiceReference = hotbarServiceReference;
    }

    public static HubUiModule create(JavaPlugin plugin, HubPracticeServices practiceServices, HubScoreboardModule scoreboardModule, PaperFeedbackService feedbackService, HubQueueFeedbackCoordinator queueFeedbackCoordinator) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(practiceServices, "practiceServices");
        Objects.requireNonNull(scoreboardModule, "scoreboardModule");
        Objects.requireNonNull(feedbackService, "feedbackService");
        Objects.requireNonNull(queueFeedbackCoordinator, "queueFeedbackCoordinator");
        Path uiDirectory = plugin.getDataFolder().toPath().resolve("ui");
        DemoMenuStateStore demoStateStore = new DemoMenuStateStore();
        PaperDialogService dialogService = new PaperDialogService(plugin, uiDirectory.resolve("dialogs"), BUNDLED_UI_RESOURCES);
        AtomicReference<ZMenuUiService> menuReference = new AtomicReference<>();
        AtomicReference<HubHotbarService> hotbarServiceReference = new AtomicReference<>();
        DemoMenuButtonRegistrar registrar = new DemoMenuButtonRegistrar(plugin, demoStateStore, menuReference::get, dialogService);
        HubMenuButtonRegistrar hubRegistrar = new HubMenuButtonRegistrar(plugin, practiceServices, scoreboardModule, feedbackService, queueFeedbackCoordinator, hotbarServiceReference::get, menuReference::get);
        ZMenuUiService menuService = new ZMenuUiService(
                plugin,
                uiDirectory.resolve("patterns"),
                uiDirectory.resolve("inventories"),
                BUNDLED_UI_RESOURCES,
                buttonManager -> {
                    registrar.register(buttonManager);
                    hubRegistrar.register(buttonManager);
                }
        );
        menuReference.set(menuService);

        dialogService.initialize();
        if (menuService.isAvailable()) {
            menuService.initialize();
        }
        return new HubUiModule(menuService, dialogService, demoStateStore, hotbarServiceReference);
    }

    public ZMenuUiService menuService() {
        return menuService;
    }

    public PaperDialogService dialogService() {
        return dialogService;
    }

    public DemoMenuStateStore demoStateStore() {
        return demoStateStore;
    }

    public Listener dialogListener() {
        return dialogService;
    }

    public void bindHotbarService(HubHotbarService hotbarService) {
        this.hotbarServiceReference.set(Objects.requireNonNull(hotbarService, "hotbarService"));
    }

    public void close() {
        dialogService.close();
    }
}
