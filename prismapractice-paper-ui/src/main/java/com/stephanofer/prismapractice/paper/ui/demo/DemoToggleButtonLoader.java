package com.stephanofer.prismapractice.paper.ui.demo;

import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.function.Supplier;

public final class DemoToggleButtonLoader extends ButtonLoader {

    private final DemoMenuStateStore stateStore;
    private final Supplier<ZMenuUiService> menuUiServiceSupplier;

    public DemoToggleButtonLoader(Plugin plugin, DemoMenuStateStore stateStore, Supplier<ZMenuUiService> menuUiServiceSupplier) {
        super(plugin, "PP_DEMO_TOGGLE");
        this.stateStore = stateStore;
        this.menuUiServiceSupplier = menuUiServiceSupplier;
    }

    @Override
    public Button load(YamlConfiguration configuration, String path, DefaultButtonValue defaultButtonValue) {
        String stateKey = configuration.getString(path + "state-key", "demo_mode");
        String defaultValue = configuration.getString(path + "default-value", "bronze");
        List<String> values = configuration.getStringList(path + "values");
        if (values.isEmpty()) {
            values = List.of(defaultValue);
        }
        return new DemoToggleButton(stateStore, menuUiServiceSupplier, stateKey, defaultValue, values);
    }
}
