package dev.quarkos.villagerreroller.picker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Client render state for villagers offering the active Hall tile. */
public final class HallHighlights {
    static final int PREVIEW_COUNT = 3;
    private static ClientLevel level;
    private static HallTrades.Tile tile;
    private static List<PriceRanking.Ranked> ranks = List.of();
    private static volatile Map<Entity, Integer> colors = Map.of();
    private static int refreshTicks;

    private HallHighlights() {
    }

    static HallTrades.Tile selected() {
        return tile;
    }

    static void select(Minecraft client, HallTrades.Tile selected) {
        tile = selected;
        level = client.level;
        refresh(client);
    }

    public static boolean clear() {
        boolean selected = tile != null;
        tile = null;
        level = null;
        ranks = List.of();
        colors = Map.of();
        return selected;
    }

    public static void tick(Minecraft client) {
        if (tile == null) {
            return;
        }
        if (client.level != level || client.player == null) {
            clear();
        } else if (--refreshTicks <= 0) {
            refresh(client);
        }
    }

    private static void refresh(Minecraft client) {
        refreshTicks = 10;
        if (level == null || client.player == null || tile.localPrices().isEmpty()) {
            colors = Map.of();
            ranks = List.of();
            return;
        }
        List<PriceRanking.Offer> offers = new ArrayList<>();
        Map<UUID, Entity> entities = new HashMap<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Villager villager) || !villager.isAlive()) {
                continue;
            }
            UUID id = villager.getUUID();
            Integer price = tile.localPrices().get(id.toString());
            if (price != null) {
                offers.add(new PriceRanking.Offer(id, price, villager.distanceToSqr(client.player)));
                entities.put(id, villager);
            }
        }
        ranks = PriceRanking.rank(offers);
        Map<Entity, Integer> outlineColors = new IdentityHashMap<>();
        for (PriceRanking.Ranked rank : ranks) {
            outlineColors.put(entities.get(rank.offer().villagerId()), rank.color());
        }
        colors = Collections.unmodifiableMap(outlineColors);
    }

    /** Null leaves the entity's vanilla glow and team color in effect. */
    public static Integer color(Entity entity) {
        return colors.get(entity);
    }

    static List<PriceRanking.Ranked> ranks() {
        return ranks;
    }

    static Component label(PriceRanking.Ranked rank) {
        return GuiDraw.message("hall.rank", rank.rank(), rank.offer().price(),
                Math.round(Math.sqrt(rank.offer().distanceSquared())));
    }
}
