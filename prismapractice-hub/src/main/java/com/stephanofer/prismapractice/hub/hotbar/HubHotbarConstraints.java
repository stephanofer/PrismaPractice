package com.stephanofer.prismapractice.hub.hotbar;

record HubHotbarConstraints(
        boolean denyMove,
        boolean denyDrop,
        boolean denyPlace,
        boolean denyPickup,
        boolean denySwapOffhand
) {

    static final HubHotbarConstraints DEFAULT = new HubHotbarConstraints(true, true, true, true, true);
}
