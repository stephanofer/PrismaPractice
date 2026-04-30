package com.stephanofer.prismapractice.hub.hotbar;

import fr.maxlego08.menu.api.MenuPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class ZMenuHubHotbarMenuController implements HubHotbarMenuController {

    private final JavaPlugin plugin;

    ZMenuHubHotbarMenuController(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public boolean openMenu(Player player, String pluginName, String menuName, int page, List<String> arguments) {
        Plugin zMenuPlugin = Bukkit.getPluginManager().getPlugin("zMenu");
        if (!(zMenuPlugin instanceof MenuPlugin menuPlugin) || !zMenuPlugin.isEnabled()) {
            plugin.getLogger().warning("Cannot open hub menu '" + menuName + "' because zMenu is not available.");
            return false;
        }

        var inventoryManager = menuPlugin.getInventoryManager();
        Optional<fr.maxlego08.menu.api.Inventory> inventory = pluginName == null || pluginName.isBlank()
                ? inventoryManager.getInventory(menuName)
                : inventoryManager.getInventory(menuName, pluginName);
        if (inventory.isEmpty()) {
            plugin.getLogger().warning("Cannot open hub menu '" + menuName + "' because it is not loaded in zMenu.");
            return false;
        }

        if (arguments != null && !arguments.isEmpty()) {
            plugin.getLogger().warning("Hub menu arguments are not supported by the current zMenu API binding yet. Opening menu without arguments: " + menuName);
        }
        if (page <= 1) {
            inventoryManager.openInventory(player, inventory.get());
        } else {
            inventoryManager.openInventory(player, inventory.get(), page);
        }
        return true;
    }
}
