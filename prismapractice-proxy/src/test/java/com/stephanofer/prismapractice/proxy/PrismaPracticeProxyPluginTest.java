package com.stephanofer.prismapractice.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PrismaPracticeProxyPluginTest {

    @Test
    void shouldKeepProxyPluginClassLoadable() {
        assertTrue(PrismaPracticeProxyPlugin.class.getName().contains("PrismaPracticeProxyPlugin"));
    }
}
