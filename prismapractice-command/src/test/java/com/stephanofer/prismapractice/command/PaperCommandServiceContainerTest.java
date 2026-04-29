package com.stephanofer.prismapractice.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaperCommandServiceContainerTest {

    @Test
    void shouldResolveRegisteredService() {
        final PaperCommandServiceContainer container = PaperCommandServiceContainer.builder()
            .add(String.class, "practice")
            .build();

        assertEquals("practice", container.require(String.class));
    }

    @Test
    void shouldFailForMissingService() {
        final PaperCommandServiceContainer container = PaperCommandServiceContainer.builder().build();

        assertThrows(IllegalStateException.class, () -> container.require(Integer.class));
    }
}
