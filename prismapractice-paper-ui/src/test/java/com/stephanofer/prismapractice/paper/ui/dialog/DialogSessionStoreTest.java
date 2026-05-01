package com.stephanofer.prismapractice.paper.ui.dialog;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogSessionStoreTest {

    @Test
    void shouldKeepSessionValuesAndHistory() {
        DialogSessionStore store = new DialogSessionStore(Duration.ofMinutes(5));

        DialogSession session = store.session("player-1");
        session.markCurrent("demo-configurator");
        session.pushHistory("demo-main");
        session.put("theme", "gold");

        assertEquals("demo-configurator", session.currentDialogId());
        assertEquals("demo-main", session.popHistory());
        assertEquals("gold", session.value("theme"));
    }

    @Test
    void shouldRemoveSessionWhenRequested() {
        DialogSessionStore store = new DialogSessionStore(Duration.ofMinutes(5));
        store.session("player-2").put("rounds", "3");

        assertTrue(store.find("player-2").isPresent());
        store.remove("player-2");
        assertFalse(store.find("player-2").isPresent());
    }
}
