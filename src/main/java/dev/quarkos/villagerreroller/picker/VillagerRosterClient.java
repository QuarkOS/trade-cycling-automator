package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.config.VillagerRoster;
import dev.quarkos.villagerreroller.config.VillagerRosterFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads the open trade screen into {@link VillagerRoster} and writes the config file.
 */
public final class VillagerRosterClient {
    private static VillagerRoster roster;

    private VillagerRosterClient() {
    }

    public static VillagerRoster roster() {
        if (roster == null) {
            roster = VillagerRoster.load(VillagerRosterFiles.configFile());
        }
        return roster;
    }

    public static void reload() {
        roster = VillagerRoster.load(VillagerRosterFiles.configFile());
    }

    /** Test hook. Replaces the in-memory roster and writes it. */
    public static void replace(VillagerRoster loaded) {
        roster = loaded == null ? new VillagerRoster() : loaded;
        roster.save(VillagerRosterFiles.configFile());
    }

    public static String currentWorldKey(Minecraft client) {
        if (client != null && client.getSingleplayerServer() != null) {
            return "local:" + client.getSingleplayerServer().getWorldData().getLevelName();
        }
        if (client != null && client.getCurrentServer() != null && client.getCurrentServer().ip != null) {
            return "remote:" + client.getCurrentServer().ip;
        }
        return "local:unknown";
    }

    public static int count(Minecraft client) {
        return roster().count(currentWorldKey(client));
    }

    public static boolean owns(Minecraft client, String tradeId) {
        if (tradeId == null || tradeId.isEmpty()) {
            return false;
        }
        for (VillagerRoster.Entry entry : roster().entries(currentWorldKey(client))) {
            for (VillagerRoster.LockedOffer offer : entry.trades()) {
                if (tradeId.equals(offer.tradeId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void inspectOpenMerchant(Minecraft client, Villager villager, MerchantMenu menu) {
        if (client == null || client.level == null || villager == null || menu == null) {
            return;
        }
        if (!TradeHall.menuLocked(menu)) {
            return;
        }
        List<TradeEntry> catalog = new ArrayList<>();
        int traderLevel = Math.max(1, menu.getTraderLevel());
        for (int level = 1; level <= traderLevel; level++) {
            catalog.addAll(TradeCatalog.list(client.level, villager, level));
        }
        List<VillagerRoster.LockedOffer> trades = new ArrayList<>();
        for (MerchantOffer offer : menu.getOffers()) {
            TradeEntry match = null;
            for (TradeEntry entry : catalog) {
                if (entry.identifies(offer)) {
                    match = entry;
                    break;
                }
            }
            trades.add(locked(offer, match));
        }
        BlockPos job = JobSiteFinder.find(client, villager);
        if (job == null) {
            job = villager.blockPosition();
        }
        String name = villager.hasCustomName() && villager.getCustomName() != null
                ? villager.getCustomName().getString()
                : "";
        VillagerRoster.Entry entry = new VillagerRoster.Entry(
                villager.getUUID().toString(),
                dimensionId(client.level),
                job.getX(),
                job.getY(),
                job.getZ(),
                name,
                McBridge.professionId(villager),
                traderLevel,
                System.currentTimeMillis(),
                List.copyOf(trades)
        );
        VillagerRoster stored = roster();
        stored.upsert(currentWorldKey(client), entry);
        stored.save(VillagerRosterFiles.configFile());
    }

    /**
     * Drops recorded villagers whose chunk is loaded near the player and whose
     * entity is gone. Villagers farther than 64 blocks stay in the file.
     */
    public static int pruneDeadLoadedVillagers(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return 0;
        }
        String world = currentWorldKey(client);
        String dimension = dimensionId(client.level);
        AABB box = client.player.getBoundingBox().inflate(64.0);
        Set<String> alive = new HashSet<>();
        for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
            if (villager.isAlive()) {
                alive.add(villager.getUUID().toString());
            }
        }
        Set<String> drop = new HashSet<>();
        for (VillagerRoster.Entry entry : roster().entries(world)) {
            if (!dimension.equals(entry.dimension())) {
                continue;
            }
            double dx = entry.x() + 0.5 - client.player.getX();
            double dy = entry.y() - client.player.getY();
            double dz = entry.z() + 0.5 - client.player.getZ();
            if (dx * dx + dy * dy + dz * dz > 64.0 * 64.0) {
                continue;
            }
            if (!alive.contains(entry.uuid())) {
                drop.add(entry.uuid());
            }
        }
        int removed = roster().removeAll(world, drop);
        if (removed > 0) {
            roster().save(VillagerRosterFiles.configFile());
        }
        return removed;
    }

    public static String dimensionId(net.minecraft.world.level.Level level) {
        if (level == null) {
            return "";
        }
        //? if >=1.21.5 {
        return level.dimension().identifier().toString();
        //?} else {
        /*return level.dimension().location().toString();
        *///?}
    }

    public static void clearWorld(Minecraft client) {
        roster().clearWorld(currentWorldKey(client));
        roster().save(VillagerRosterFiles.configFile());
    }

    private static VillagerRoster.LockedOffer locked(MerchantOffer offer, TradeEntry match) {
        boolean pays = offer.getCostA().is(Items.EMERALD);
        ItemStack traded = pays ? offer.getResult() : offer.getCostA();
        if (traded.isEmpty() && !offer.getResult().isEmpty()) {
            traded = offer.getResult();
            pays = offer.getCostA().is(Items.EMERALD);
        }
        int count = Math.max(1, traded.getCount());
        int effective = pays ? offer.getCostA().getCount() : offer.getResult().getCount();
        String tradeId = match == null ? "" : match.id;
        String display = match == null ? traded.getHoverName().getString() : match.name;
        String enchantmentId = "";
        int enchantmentLevel = 0;
        if (tradeId.startsWith("enchant:")) {
            String[] parts = tradeId.split(":");
            if (parts.length >= 4) {
                enchantmentId = parts[1] + ":" + parts[2];
                try {
                    enchantmentLevel = Integer.parseInt(parts[3]);
                } catch (NumberFormatException ignored) {
                    enchantmentLevel = 0;
                }
                String roman = VillagerRoster.roman(enchantmentLevel);
                if (enchantmentLevel > 0 && !display.endsWith(" " + roman) && !display.endsWith(roman)) {
                    display = display + " " + roman;
                }
            }
        }
        if (tradeId.isEmpty()) {
            tradeId = (pays ? "buy:" : "sell:") + TradeEntry.itemId(traded.getItem()) + ":" + count + ":" + effective;
            display = traded.getHoverName().getString();
        }
        return new VillagerRoster.LockedOffer(
                tradeId,
                display,
                enchantmentId,
                enchantmentLevel,
                TradeEntry.itemId(traded.getItem()),
                count,
                effective,
                effective,
                pays
        );
    }
}
