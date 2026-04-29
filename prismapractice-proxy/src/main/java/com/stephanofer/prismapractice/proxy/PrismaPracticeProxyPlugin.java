package com.stephanofer.prismapractice.proxy;

import com.google.inject.Inject;
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

    @Inject
    public PrismaPracticeProxyPlugin(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        logger.info("[proxy-runtime] data-directory={}", dataDirectory.toAbsolutePath());
        logger.info("[demo-runtime] proxy-version={}", proxyServer.getVersion().getVersion());
    }
}
