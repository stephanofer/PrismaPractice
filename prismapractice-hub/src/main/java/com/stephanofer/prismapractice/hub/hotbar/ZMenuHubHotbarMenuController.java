package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

final class ZMenuHubHotbarMenuController implements HubHotbarMenuController {

    private final ZMenuUiService menuUiService;

    ZMenuHubHotbarMenuController(ZMenuUiService menuUiService) {
        this.menuUiService = Objects.requireNonNull(menuUiService, "menuUiService");
    }

    @Override
    public boolean openMenu(Player player, String pluginName, String menuName, int page, List<String> arguments) {
        return menuUiService.openMenu(player, pluginName, menuName, page, true);
    }
}
