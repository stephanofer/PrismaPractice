package com.stephanofer.prismapractice.paper.ui;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Objects;

public final class UiResourceInstaller {

    private UiResourceInstaller() {
    }

    public static void install(JavaPlugin plugin, Collection<String> bundledResources) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(bundledResources, "bundledResources");

        for (String bundledResource : bundledResources) {
            if (bundledResource == null || bundledResource.isBlank()) {
                continue;
            }
            String relativePath = bundledResource.startsWith("defaults/") ? bundledResource.substring("defaults/".length()) : bundledResource;
            Path target = plugin.getDataFolder().toPath().resolve(relativePath).normalize();
            try {
                Files.createDirectories(target.getParent());
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot create directories for resource '" + bundledResource + "'", exception);
            }
            if (Files.notExists(target)) {
                try (InputStream inputStream = plugin.getResource(bundledResource)) {
                    if (inputStream == null) {
                        throw new IllegalStateException("Bundled UI resource not found: " + bundledResource);
                    }
                    Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot copy resource '" + bundledResource + "'", exception);
                }
            }
        }
    }
}
