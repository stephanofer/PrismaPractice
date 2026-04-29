package com.stephanofer.prismapractice.proxy;

import com.google.inject.Inject;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "prismapracticeproxy",
        name = "PrismaPracticeProxy",
        version = "1.0.0",
        description = "PrismaPractice proxy plugin"
)
public final class PrismaPracticeProxyPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final ConfigManager configManager;

    @Inject
    public PrismaPracticeProxyPlugin(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.configManager = ProxyDemoConfigBootstrap.bootstrap(dataDirectory, getClass().getClassLoader(), logger::info);
        logger.info("[demo-runtime] proxy-version={}", proxyServer.getVersion().getVersion());
    }
}
