package dev.quarkos.villagerreroller.paper;

import dev.quarkos.villagerreroller.net.RerollCodec;
import dev.quarkos.villagerreroller.server.RerollRules;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional Paper addon. It speaks {@code villager_reroller:v1} and never messages
 * a player who has not registered that channel.
 */
public final class PaperRerollerPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, Long> lastTick = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, RerollCodec.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, RerollCodec.CHANNEL, (channel, player, message) -> onMessage(player, message));
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("villager_reroller paper listening on " + RerollCodec.CHANNEL);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> advertise(player), 20L);
        Bukkit.getScheduler().runTaskLater(this, () -> advertise(player), 60L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastTick.remove(event.getPlayer().getUniqueId());
    }

    private void advertise(Player player) {
        if (!player.isOnline() || !getConfig().getBoolean("enabled", true)) {
            return;
        }
        if (!player.getListeningPluginChannels().contains(RerollCodec.CHANNEL)) {
            return;
        }
        player.sendPluginMessage(this, RerollCodec.CHANNEL, RerollCodec.hello(hello()));
        getLogger().info("villager_reroller advertised " + RerollCodec.CHANNEL + " to " + player.getName());
    }

    private void onMessage(Player player, byte[] message) {
        Object decoded = RerollCodec.decode(message);
        if (!(decoded instanceof RerollCodec.Request request)) {
            return;
        }
        if (!getConfig().getBoolean("enabled", true)) {
            reply(player, request.nonce(), RerollCodec.Deny.DISABLED);
            return;
        }
        Villager villager = villager(player, request.entityId());
        RerollCodec.Deny deny = validate(player, villager);
        if (deny != RerollCodec.Deny.OK) {
            reply(player, request.nonce(), deny);
            return;
        }
        if (!charge(player)) {
            reply(player, request.nonce(), RerollCodec.Deny.COST);
            return;
        }
        lastTick.put(player.getUniqueId(), worldTick(player));
        if (!reroll(villager)) {
            reply(player, request.nonce(), RerollCodec.Deny.NO_PROFESSION);
            return;
        }
        reply(player, request.nonce(), RerollCodec.Deny.OK);
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline() && villager.isValid()) {
                player.closeInventory();
                player.openMerchant(villager, true);
            }
        });
        getLogger().info("villager_reroller rerolled villager " + villager.getEntityId() + " for " + player.getName());
    }

    private RerollCodec.Deny validate(Player player, Villager villager) {
        if (villager == null) {
            return RerollCodec.Deny.NOT_VILLAGER;
        }
        if (requirePermission() && !player.hasPermission(permission())) {
            return RerollCodec.Deny.PERMISSION;
        }
        Villager.Profession profession = villager.getProfession();
        if (profession == Villager.Profession.NONE || profession == Villager.Profession.NITWIT) {
            return RerollCodec.Deny.NO_PROFESSION;
        }
        if (!(player.getOpenInventory().getTopInventory() instanceof org.bukkit.inventory.MerchantInventory merchant)
                || merchant.getMerchant() != villager) {
            return RerollCodec.Deny.NOT_TRADING;
        }
        boolean same = player.getWorld().equals(villager.getWorld());
        double distance = player.getLocation().distanceSquared(villager.getLocation());
        if (RerollRules.tooFar(same, distance, maxDistance())) {
            return RerollCodec.Deny.TOO_FAR;
        }
        boolean used = false;
        for (MerchantRecipe recipe : villager.getRecipes()) {
            if (recipe.getUses() > 0) {
                used = true;
                break;
            }
        }
        if (RerollRules.locked(freshOnly(), villager.getVillagerLevel(), villager.getVillagerExperience(), used)) {
            return RerollCodec.Deny.LOCKED;
        }
        long last = lastTick.getOrDefault(player.getUniqueId(), -1L);
        if (RerollRules.coolingDown(worldTick(player), last, cooldownTicks())) {
            return RerollCodec.Deny.COOLDOWN;
        }
        if (!RerollRules.canAfford(costKind(), costAmount(), countEmeralds(player), player.getTotalExperience())) {
            return RerollCodec.Deny.COST;
        }
        return RerollCodec.Deny.OK;
    }

    private boolean reroll(Villager villager) {
        Villager.Profession profession = villager.getProfession();
        Villager.Type type = villager.getVillagerType();
        int level = villager.getVillagerLevel();
        int experience = villager.getVillagerExperience();
        List<MerchantRecipe> before = new ArrayList<>(villager.getRecipes());
        villager.setRecipes(List.of());
        villager.setProfession(Villager.Profession.NONE);
        villager.setProfession(profession);
        villager.setVillagerType(type);
        villager.setVillagerLevel(level);
        villager.setVillagerExperience(experience);
        List<MerchantRecipe> after = villager.getRecipes();
        if (after == null || after.isEmpty()) {
            villager.setRecipes(before);
            return false;
        }
        return true;
    }

    private Villager villager(Player player, int entityId) {
        for (org.bukkit.entity.Entity entity : player.getWorld().getEntities()) {
            if (entity.getEntityId() == entityId && entity instanceof Villager villager && villager.isValid()) {
                return villager;
            }
        }
        return null;
    }

    private boolean charge(Player player) {
        int kind = costKind();
        int amount = costAmount();
        if (amount <= 0 || kind == RerollCodec.COST_FREE) {
            return true;
        }
        if (kind == RerollCodec.COST_XP) {
            player.giveExp(-amount);
            return true;
        }
        int left = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && left > 0; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType() != org.bukkit.Material.EMERALD) {
                continue;
            }
            int take = Math.min(left, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                player.getInventory().setItem(slot, null);
            }
            left -= take;
        }
        return left == 0;
    }

    private int countEmeralds(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == org.bukkit.Material.EMERALD) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private void reply(Player player, int nonce, RerollCodec.Deny deny) {
        if (!player.getListeningPluginChannels().contains(RerollCodec.CHANNEL)) {
            return;
        }
        player.sendPluginMessage(this, RerollCodec.CHANNEL, RerollCodec.result(new RerollCodec.Result(nonce, deny)));
    }

    private RerollCodec.Hello hello() {
        int milli = (int) Math.round(maxDistance() * 1000.0);
        return new RerollCodec.Hello(costKind(), costAmount(), Math.max(1, cooldownTicks()), freshOnly(), milli);
    }

    private boolean requirePermission() {
        return getConfig().getBoolean("requirePermission", false);
    }

    private String permission() {
        return getConfig().getString("permission", "villager_reroller.reroll");
    }

    private boolean freshOnly() {
        return getConfig().getBoolean("onlyFreshVillagers", true);
    }

    private int cooldownTicks() {
        return getConfig().getInt("cooldownTicks", 1);
    }

    private double maxDistance() {
        return getConfig().getDouble("maxDistance", 8.0);
    }

    private int costAmount() {
        return Math.max(0, getConfig().getInt("costAmount", 1));
    }

    private int costKind() {
        return RerollCodec.costKind(getConfig().getString("cost", "free"));
    }

    private long worldTick(Player player) {
        return player.getWorld().getFullTime();
    }
}
