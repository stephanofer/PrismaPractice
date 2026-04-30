package com.stephanofer.prismapractice.hub.hotbar;

import org.bukkit.entity.Player;

@FunctionalInterface
interface HubHotbarCustomActionHandler {

    boolean execute(Player player, HubHotbarActionConfig actionConfig);
}
