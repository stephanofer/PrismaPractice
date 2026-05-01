package com.stephanofer.prismapractice.paper.ui.demo;

import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogService;
import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;

public final class DemoResetButtonLoader extends ButtonLoader {

    private final DemoMenuStateStore stateStore;
    private final PaperDialogService dialogService;
    private final Supplier<ZMenuUiService> menuUiServiceSupplier;

    public DemoResetButtonLoader(Plugin plugin, DemoMenuStateStore stateStore, PaperDialogService dialogService, Supplier<ZMenuUiService> menuUiServiceSupplier) {
        super(plugin, "PP_DEMO_RESET");
        this.stateStore = stateStore;
        this.dialogService = dialogService;
        this.menuUiServiceSupplier = menuUiServiceSupplier;
    }

    @Override
    public Button load(YamlConfiguration configuration, String path, DefaultButtonValue defaultButtonValue) {
        return new DemoResetButton(stateStore, dialogService, menuUiServiceSupplier);
    }
}
