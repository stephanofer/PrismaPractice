package com.stephanofer.prismapractice.paper.ui.menu;

import com.stephanofer.prismapractice.paper.ui.UiResourceInstaller;
import fr.maxlego08.menu.api.ButtonManager;
import fr.maxlego08.menu.api.Inventory;
import fr.maxlego08.menu.api.InventoryManager;
import fr.maxlego08.menu.api.MenuPlugin;
import fr.maxlego08.menu.api.exceptions.InventoryException;
import fr.maxlego08.menu.api.pattern.PatternManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class ZMenuUiService {

    private final JavaPlugin plugin;
    private final Path patternsDirectory;
    private final Path inventoriesDirectory;
    private final Collection<String> bundledResources;
    private final Consumer<ButtonManager> buttonRegistrar;

    public ZMenuUiService(
            JavaPlugin plugin,
            Path patternsDirectory,
            Path inventoriesDirectory,
            Collection<String> bundledResources,
            Consumer<ButtonManager> buttonRegistrar
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.patternsDirectory = Objects.requireNonNull(patternsDirectory, "patternsDirectory");
        this.inventoriesDirectory = Objects.requireNonNull(inventoriesDirectory, "inventoriesDirectory");
        this.bundledResources = List.copyOf(Objects.requireNonNull(bundledResources, "bundledResources"));
        this.buttonRegistrar = Objects.requireNonNull(buttonRegistrar, "buttonRegistrar");
    }

    public void initialize() {
        UiResourceInstaller.install(plugin, bundledResources);
        MenuPlugin menuPlugin = requireMenuPlugin();
        ButtonManager buttonManager = menuPlugin.getButtonManager();
        buttonManager.unregisters(plugin);
        buttonRegistrar.accept(buttonManager);
        loadPatterns(menuPlugin.getPatternManager());
        loadInventories(menuPlugin.getInventoryManager(), true);
    }

    public boolean isAvailable() {
        return lookupMenuPlugin().isPresent();
    }

    public void reload() {
        MenuPlugin menuPlugin = requireMenuPlugin();
        ButtonManager buttonManager = menuPlugin.getButtonManager();
        buttonManager.unregisters(plugin);
        buttonRegistrar.accept(buttonManager);
        loadPatterns(menuPlugin.getPatternManager());
        loadInventories(menuPlugin.getInventoryManager(), false);
    }

    public boolean openMenu(Player player, String pluginName, String menuName, int page, boolean preserveHistory) {
        Objects.requireNonNull(player, "player");
        if (menuName == null || menuName.isBlank()) {
            return false;
        }

        Optional<MenuPlugin> optional = lookupMenuPlugin();
        if (optional.isEmpty()) {
            plugin.getLogger().warning("Cannot open menu '" + menuName + "' because zMenu is not available.");
            return false;
        }

        InventoryManager inventoryManager = optional.get().getInventoryManager();
        Optional<Inventory> inventory = pluginName == null || pluginName.isBlank()
                ? inventoryManager.getInventory(menuName)
                : inventoryManager.getInventory(pluginName, menuName);
        if (inventory.isEmpty()) {
            plugin.getLogger().warning("Cannot open menu '" + menuName + "' because it is not loaded in zMenu.");
            return false;
        }

        int safePage = Math.max(1, page);
        if (preserveHistory) {
            inventoryManager.openInventoryWithOldInventories(player, inventory.get(), safePage);
        } else if (safePage <= 1) {
            inventoryManager.openInventory(player, inventory.get());
        } else {
            inventoryManager.openInventory(player, inventory.get(), safePage);
        }
        return true;
    }

    public void updateInventory(Player player) {
        Optional<MenuPlugin> optional = lookupMenuPlugin();
        if (optional.isEmpty()) {
            return;
        }
        optional.get().getInventoryManager().updateInventory(player, plugin);
    }

    private void loadPatterns(PatternManager patternManager) {
        try (Stream<Path> stream = Files.walk(patternsDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".yml"))
                    .forEach(path -> loadPattern(patternManager, path));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot scan zMenu pattern directory '" + patternsDirectory + "'", exception);
        }
    }

    private void loadPattern(PatternManager patternManager, Path path) {
        try {
            patternManager.loadPattern(path.toFile());
        } catch (InventoryException exception) {
            throw new IllegalStateException("Cannot load zMenu pattern '" + path + "'", exception);
        }
    }

    private void loadInventories(InventoryManager inventoryManager, boolean firstLoad) {
        if (!firstLoad) {
            inventoryManager.deleteInventories(plugin);
        }
        try (Stream<Path> stream = Files.walk(inventoriesDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".yml"))
                    .forEach(path -> loadInventory(inventoryManager, path));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot scan zMenu inventory directory '" + inventoriesDirectory + "'", exception);
        }
    }

    private void loadInventory(InventoryManager inventoryManager, Path path) {
        try {
            inventoryManager.loadInventory(plugin, path.toFile());
        } catch (InventoryException exception) {
            throw new IllegalStateException("Cannot load zMenu inventory '" + path + "'", exception);
        }
    }

    private MenuPlugin requireMenuPlugin() {
        return lookupMenuPlugin().orElseThrow(() -> new IllegalStateException("zMenu is required for inventory UI initialization"));
    }

    private Optional<MenuPlugin> lookupMenuPlugin() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("zMenu");
        if (!(plugin instanceof MenuPlugin menuPlugin) || !plugin.isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(menuPlugin);
    }
}
