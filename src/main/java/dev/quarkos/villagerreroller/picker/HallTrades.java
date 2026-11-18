package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.config.VillagerRoster;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Joins saved targets with the offers actually seen in the roster and queue. */
final class HallTrades {
    private final ClientLevel level;
    private final Map<String, Map<String, TradeEntry>> catalogs = new HashMap<>();

    HallTrades(ClientLevel level) {
        this.level = level;
    }

    List<Tile> tiles(Minecraft client) {
        Map<Key, TileData> tiles = new LinkedHashMap<>();
        ProfessionPicks.load(PickFiles.configFile()).snapshot().forEach((profession, picks) ->
                picks.forEach((tradeId, ceiling) -> tiles.computeIfAbsent(new Key(profession, tradeId), TileData::new)
                        .wantedMax = ceiling));

        String world = VillagerRosterClient.currentWorldKey(client);
        List<VillagerRoster.Entry> roster = VillagerRosterClient.roster().entries(world);
        String dimension = VillagerRosterClient.dimensionId(level);
        Set<String> recordedSlots = new HashSet<>();
        for (TradeHall.Slot slot : TradeHall.slots()) {
            if (slot.isIn(level) && !slot.offers().isEmpty()) {
                addOffers(tiles, slot.profession(), slot.uuid().toString(), dimension, slot.offers());
                recordedSlots.add(slot.uuid().toString());
            }
        }
        for (VillagerRoster.Entry villager : roster) {
            if (!recordedSlots.contains(villager.uuid())) {
                addOffers(tiles, villager.profession(), villager.uuid(), villager.dimension(), villager.trades());
            }
        }
        List<Tile> result = new ArrayList<>(tiles.size());
        for (TileData tile : tiles.values()) {
            Map<String, TradeEntry> catalog = catalogs.computeIfAbsent(tile.key.profession(), this::catalog);
            result.add(tile.build(catalog.get(tile.key.tradeId()), dimension));
        }
        result.sort(Comparator.comparing((Tile tile) -> !tile.wanted())
                .thenComparing(tile -> tile.name().getString())
                .thenComparing(tile -> tile.key().profession())
                .thenComparing(tile -> tile.key().tradeId()));
        return List.copyOf(result);
    }

    private Map<String, TradeEntry> catalog(String profession) {
        Map<String, TradeEntry> catalog = new HashMap<>();
        TradeCatalog.browse(level, Identifier.tryParse(profession), null).offers()
                .forEach((id, offer) -> catalog.put(id, offer.entry()));
        return catalog;
    }

    private static void addOffers(Map<Key, TileData> tiles, String profession, String uuid, String dimension,
                                  List<VillagerRoster.LockedOffer> offers) {
        for (VillagerRoster.LockedOffer offer : offers) {
            if (offer.tradeId() == null || offer.tradeId().isEmpty()) {
                continue;
            }
            Key key = new Key(profession, offer.tradeId());
            TileData tile = tiles.computeIfAbsent(key, TileData::new);
            if (tile.sample == null) {
                tile.sample = offer;
            }
            int price = offer.effectiveCost() > 0 ? offer.effectiveCost() : offer.emeraldCost();
            tile.offers.merge(uuid, new KnownOffer(dimension, price),
                    (first, second) -> second.price() > 0 && (first.price() <= 0 || second.price() < first.price()) ? second : first);
        }
    }

    record Key(String profession, String tradeId) {
    }

    record Tile(Key key, Component name, Component profession, ItemStack icon, String badge, int wantedMax,
                int defaultMax, int knownCount, int bestPrice, Map<String, Integer> localPrices) {
        boolean wanted() {
            return wantedMax > 0;
        }

        boolean available() {
            return knownCount > 0;
        }
    }

    private record KnownOffer(String dimension, int price) {
    }

    private static final class TileData {
        private final Key key;
        private final Map<String, KnownOffer> offers = new HashMap<>();
        private int wantedMax;
        private VillagerRoster.LockedOffer sample;

        private TileData(Key key) {
            this.key = key;
        }

        private Tile build(TradeEntry entry, String dimension) {
            String name = entry != null ? entry.name
                    : sample != null && sample.displayName() != null && !sample.displayName().isBlank()
                    ? sample.displayName() : VillagerRoster.prettyTrade(key.tradeId());
            ItemStack icon = entry == null ? ItemStack.EMPTY : entry.icon;
            if (icon.isEmpty() && sample != null) {
                icon = TradeIcons.item(sample.itemId());
            }
            if (icon.isEmpty()) {
                icon = TradeIcons.forTrade(key.tradeId());
            }
            if (icon.isEmpty()) {
                icon = new ItemStack(Items.EMERALD);
            }
            int level = sample != null && sample.enchantmentLevel() > 0
                    ? sample.enchantmentLevel() : TradeIcons.bookLevel(key.tradeId());
            String badge = TradeIcons.levelBadge(level);
            if (badge != null && !name.endsWith(" " + badge)
                    && !name.endsWith(" " + Component.translatable("enchantment.level." + level).getString())) {
                name += " " + badge;
            }
            int defaultMax = entry != null && entry.paysEmeralds ? entry.emeraldMax : TradeSession.ANY_PRICE;
            int bestPrice = offers.values().stream().mapToInt(KnownOffer::price).filter(price -> price > 0).min().orElse(0);
            Map<String, Integer> prices = new HashMap<>();
            offers.forEach((uuid, offer) -> {
                if (dimension.equals(offer.dimension()) && offer.price() > 0) {
                    prices.put(uuid, offer.price());
                }
            });
            return new Tile(key, Component.literal(name), TradeIcons.professionName(key.profession()), icon, badge,
                    wantedMax, defaultMax, offers.size(), bestPrice, Map.copyOf(prices));
        }
    }
}
