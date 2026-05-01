package com.stephanofer.prismapractice.paper.ui.demo;

import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;

public final class DemoCounterButtonLoader extends ButtonLoader {

    private final DemoMenuStateStore stateStore;
    private final Supplier<ZMenuUiService> menuUiServiceSupplier;

    public DemoCounterButtonLoader(Plugin plugin, DemoMenuStateStore stateStore, Supplier<ZMenuUiService> menuUiServiceSupplier) {
        super(plugin, "PP_DEMO_COUNTER");
        this.stateStore = stateStore;
        this.menuUiServiceSupplier = menuUiServiceSupplier;
    }

    @Override
    public Button load(YamlConfiguration configuration, String path, DefaultButtonValue defaultButtonValue) {
        String counterKey = configuration.getString(path + "counter-key", "demo_counter");
        return new DemoCounterButton(stateStore, menuUiServiceSupplier, counterKey);
    }
}
