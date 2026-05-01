package com.stephanofer.prismapractice.paper.ui.demo;

import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class DemoPaginatedListButtonLoader extends ButtonLoader {

    private final DemoMenuStateStore stateStore;

    public DemoPaginatedListButtonLoader(Plugin plugin, DemoMenuStateStore stateStore) {
        super(plugin, "PP_DEMO_LIST");
        this.stateStore = stateStore;
    }

    @Override
    public Button load(YamlConfiguration configuration, String path, DefaultButtonValue defaultButtonValue) {
        int totalItems = configuration.getInt(path + "total-items", 48);
        int emptySlot = configuration.getInt(path + "empty-slot", -1);
        return new DemoPaginatedListButton(stateStore, totalItems, emptySlot);
    }
}
