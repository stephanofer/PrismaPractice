package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.queue.QueueEntry;
import com.stephanofer.prismapractice.api.queue.QueueEntryRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.LinkedHashMap;
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
    private final QueueEntryRepository queueEntryRepository;
    private final HubStaffModeService staffModeService;
    private final Map<UUID, AppliedHotbarProfile> appliedProfiles;

    public HubHotbarService(
            HubPlayerHotbarContextService contextService,
            HubHotbarProfileResolver profileResolver,
            HubHotbarItemRegistry registry,
            QueueEntryRepository queueEntryRepository,
            HubStaffModeService staffModeService
    ) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.profileResolver = Objects.requireNonNull(profileResolver, "profileResolver");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.queueEntryRepository = Objects.requireNonNull(queueEntryRepository, "queueEntryRepository");
        this.staffModeService = Objects.requireNonNull(staffModeService, "staffModeService");
        this.appliedProfiles = new ConcurrentHashMap<>();
    }

    public void refresh(Player player) {
        refresh(player, false);
    }

    public void refresh(Player player, boolean force) {
        Objects.requireNonNull(player, "player");
        if (staffModeService.isEnabled(player)) {
            appliedProfiles.remove(player.getUniqueId());
            return;
        }
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

    public void clearInventory(Player player) {
        Objects.requireNonNull(player, "player");
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new org.bukkit.inventory.ItemStack[4]);
        inventory.setItemInOffHand(null);
        player.setItemOnCursor(null);
    }

    public boolean isStaffBypassEnabled(Player player) {
        return staffModeService.isEnabled(player);
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

        PlayerId playerId = new PlayerId(player.getUniqueId());
        for (Map.Entry<Integer, HubCompiledItem> entry : profile.items().entrySet()) {
            inventory.setItem(entry.getKey(), renderItem(playerId, entry.getValue()));
        }
        inventory.setHeldItemSlot(profile.selectedSlot());
    }

    private ItemStack renderItem(PlayerId playerId, HubCompiledItem item) {
        if (item.action().type() != HubHotbarActionType.CUSTOM || !"queue-context-source".equalsIgnoreCase(item.action().customKey())) {
            return item.cloneStack();
        }

        Optional<QueueEntry> activeEntry = queueEntryRepository.findByPlayerId(playerId);
        if (activeEntry.isEmpty()) {
            return item.cloneStack();
        }

        String sourceItemKey = actionArguments(item.action()).get(activeEntry.get().queueType().name().toLowerCase(java.util.Locale.ROOT));
        if (sourceItemKey == null || sourceItemKey.isBlank()) {
            return item.cloneStack();
        }

        return registry.findItemByKey(sourceItemKey)
                .map(HubCompiledItem::cloneStack)
                .orElseGet(item::cloneStack);
    }

    private Map<String, String> actionArguments(HubHotbarActionConfig action) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String argument : action.arguments()) {
            int separator = argument.indexOf('=');
            if (separator <= 0 || separator >= argument.length() - 1) {
                continue;
            }
            values.put(argument.substring(0, separator).trim().toLowerCase(java.util.Locale.ROOT), argument.substring(separator + 1).trim());
        }
        return values;
    }
}
