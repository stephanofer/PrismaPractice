package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.api.common.PlayerId;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HubHotbarService {

    private final HubPlayerHotbarContextService contextService;
    private final HubHotbarProfileResolver profileResolver;
    private final HubHotbarItemRegistry registry;
    private final Map<UUID, AppliedHotbarProfile> appliedProfiles;

    public HubHotbarService(
            HubPlayerHotbarContextService contextService,
            HubHotbarProfileResolver profileResolver,
            HubHotbarItemRegistry registry
    ) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.profileResolver = Objects.requireNonNull(profileResolver, "profileResolver");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.appliedProfiles = new ConcurrentHashMap<>();
    }

    public void refresh(Player player) {
        refresh(player, false);
    }

    public void refresh(Player player, boolean force) {
        Objects.requireNonNull(player, "player");
        PlayerId playerId = new PlayerId(player.getUniqueId());
        HubPlayerHotbarContext context = contextService.snapshot(playerId);
        Optional<HubCompiledHotbarProfile> profile = resolveProfile(context);
        if (profile.isEmpty()) {
            appliedProfiles.remove(player.getUniqueId());
            return;
        }

        AppliedHotbarProfile current = appliedProfiles.get(player.getUniqueId());
        if (!force && current != null && current.key().equals(profile.get().key())) {
            return;
        }

        apply(player, profile.get());
        appliedProfiles.put(player.getUniqueId(), new AppliedHotbarProfile(profile.get().key(), profile.get().constraints()));
    }

    public void refresh(PlayerId playerId, boolean force) {
        Player player = Bukkit.getPlayer(playerId.value());
        if (player != null) {
            refresh(player, force);
        }
    }

    public Optional<AppliedHotbarProfile> findAppliedProfile(Player player) {
        return Optional.ofNullable(appliedProfiles.get(player.getUniqueId()));
    }

    public Optional<HubCompiledItem> resolveManagedItem(org.bukkit.inventory.ItemStack itemStack) {
        return registry.findItem(itemStack);
    }

    public void clear(Player player) {
        appliedProfiles.remove(player.getUniqueId());
    }

    private Optional<HubCompiledHotbarProfile> resolveProfile(HubPlayerHotbarContext context) {
        List<String> candidates = profileResolver.resolveCandidates(context);
        for (String candidate : candidates) {
            Optional<HubCompiledHotbarProfile> profile = registry.findProfile(candidate);
            if (profile.isPresent()) {
                return profile;
            }
        }
        return Optional.empty();
    }

    private void apply(Player player, HubCompiledHotbarProfile profile) {
        PlayerInventory inventory = player.getInventory();
        if (profile.resetInventory()) {
            inventory.clear();
            inventory.setArmorContents(new org.bukkit.inventory.ItemStack[4]);
            inventory.setItemInOffHand(null);
            player.setItemOnCursor(null);
        } else {
            for (int slot = 0; slot < 9; slot++) {
                inventory.setItem(slot, null);
            }
        }

        for (Map.Entry<Integer, HubCompiledItem> entry : profile.items().entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().cloneStack());
        }
        inventory.setHeldItemSlot(profile.selectedSlot());
    }
}
