package com.stephanofer.prismapractice.paper.ui.demo;

import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogService;
import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import fr.maxlego08.menu.api.ButtonManager;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.Supplier;

public final class DemoMenuButtonRegistrar {

    private final Plugin plugin;
    private final DemoMenuStateStore stateStore;
    private final Supplier<ZMenuUiService> menuUiServiceSupplier;
    private final PaperDialogService dialogService;

    public DemoMenuButtonRegistrar(Plugin plugin, DemoMenuStateStore stateStore, Supplier<ZMenuUiService> menuUiServiceSupplier, PaperDialogService dialogService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.menuUiServiceSupplier = Objects.requireNonNull(menuUiServiceSupplier, "menuUiServiceSupplier");
        this.dialogService = Objects.requireNonNull(dialogService, "dialogService");
    }

    public void register(ButtonManager buttonManager) {
        buttonManager.register(new DemoCounterButtonLoader(plugin, stateStore, menuUiServiceSupplier));
        buttonManager.register(new DemoToggleButtonLoader(plugin, stateStore, menuUiServiceSupplier));
        buttonManager.register(new DemoPaginatedListButtonLoader(plugin, stateStore));
        buttonManager.register(new DemoOpenDialogButtonLoader(plugin, stateStore, dialogService));
        buttonManager.register(new DemoResetButtonLoader(plugin, stateStore, dialogService, menuUiServiceSupplier));
    }
}
