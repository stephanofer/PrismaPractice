package com.stephanofer.prismapractice.hub.hotbar;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class HubStaffModeServiceTest {

    @Test
    void shouldToggleStaffModeForPlayer() {
        HubStaffModeService service = new HubStaffModeService();
        Player player = Mockito.mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        assertTrue(service.toggle(player));
        assertTrue(service.isEnabled(player));

        assertFalse(service.toggle(player));
        assertFalse(service.isEnabled(player));
    }
}
