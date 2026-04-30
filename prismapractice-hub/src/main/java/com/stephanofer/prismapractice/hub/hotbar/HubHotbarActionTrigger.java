package com.stephanofer.prismapractice.hub.hotbar;

import org.bukkit.event.block.Action;

enum HubHotbarActionTrigger {
    RIGHT_CLICK,
    LEFT_CLICK,
    ANY;

    boolean matches(Action action) {
        return switch (this) {
            case ANY -> action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
            case RIGHT_CLICK -> action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
            case LEFT_CLICK -> action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        };
    }
}
