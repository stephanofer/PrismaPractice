package com.stephanofer.prismapractice.paper.ui.demo;

import com.stephanofer.prismapractice.paper.ui.dialog.PaperDialogService;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class DemoOpenDialogButtonLoader extends ButtonLoader {

    private final DemoMenuStateStore stateStore;
    private final PaperDialogService dialogService;

    public DemoOpenDialogButtonLoader(Plugin plugin, DemoMenuStateStore stateStore, PaperDialogService dialogService) {
        super(plugin, "PP_DEMO_OPEN_DIALOG");
        this.stateStore = stateStore;
        this.dialogService = dialogService;
    }

    @Override
    public Button load(YamlConfiguration configuration, String path, DefaultButtonValue defaultButtonValue) {
        String dialogId = configuration.getString(path + "dialog", "demo-notice");
        return new DemoOpenDialogButton(stateStore, dialogService, dialogId);
    }
}
