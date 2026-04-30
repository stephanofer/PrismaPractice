package com.stephanofer.prismapractice.hub.hotbar;

import org.bukkit.entity.Player;

import java.util.List;

interface HubHotbarMenuController {

    boolean openMenu(Player player, String pluginName, String menuName, int page, List<String> arguments);
}
