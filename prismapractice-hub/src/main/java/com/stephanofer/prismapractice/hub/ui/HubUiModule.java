package com.stephanofer.prismapractice.hub.ui;

import com.stephanofer.prismapractice.paper.ui.demo.DemoMenuButtonRegistrar;
import com.stephanofer.prismapractice.paper.ui.demo.DemoMenuStateStore;
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
            "defaults/ui/inventories/demo-main.yml",
            "defaults/ui/inventories/demo-dynamic.yml",
            "defaults/ui/dialogs/demo-notice.yml",
            "defaults/ui/dialogs/demo-confirmation.yml",
            "defaults/ui/dialogs/demo-multi-action.yml",
            "defaults/ui/dialogs/demo-configurator.yml",
            "defaults/ui/dialogs/demo-review.yml"
    );

    private final ZMenuUiService menuService;
    private final PaperDialogService dialogService;
    private final DemoMenuStateStore demoStateStore;

    private HubUiModule(ZMenuUiService menuService, PaperDialogService dialogService, DemoMenuStateStore demoStateStore) {
        this.menuService = menuService;
        this.dialogService = dialogService;
        this.demoStateStore = demoStateStore;
    }

    public static HubUiModule create(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        Path uiDirectory = plugin.getDataFolder().toPath().resolve("ui");
        DemoMenuStateStore demoStateStore = new DemoMenuStateStore();
        PaperDialogService dialogService = new PaperDialogService(plugin, uiDirectory.resolve("dialogs"), BUNDLED_UI_RESOURCES);
        AtomicReference<ZMenuUiService> menuReference = new AtomicReference<>();
        DemoMenuButtonRegistrar registrar = new DemoMenuButtonRegistrar(plugin, demoStateStore, menuReference::get, dialogService);
        ZMenuUiService menuService = new ZMenuUiService(
                plugin,
                uiDirectory.resolve("patterns"),
                uiDirectory.resolve("inventories"),
                BUNDLED_UI_RESOURCES,
                registrar::register
        );
        menuReference.set(menuService);

        dialogService.initialize();
        if (menuService.isAvailable()) {
            menuService.initialize();
        }
        return new HubUiModule(menuService, dialogService, demoStateStore);
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

    public void close() {
        dialogService.close();
    }
}
