package dev.quarkos.villagerreroller.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.phys.AABB;
//? if <1.21.11 {
/*import net.minecraft.world.entity.npc.Villager;
*///?} else {
import net.minecraft.world.entity.npc.villager.Villager;
//?}

/**
 * The client trade menu holds a {@code ClientSideMerchant}, not the villager.
 * The last villager the player used is the one the trade screen belongs to.
 */
public final class LibrarianTracker {
    private static int entityId = Integer.MIN_VALUE;
    /** While set, the merchant screen keeps this villager instead of the nearest one. */
    private static boolean pinned;

    private LibrarianTracker() {
    }

    public static void remember(Entity entity) {
        if (pinned) {
            return;
        }
        if (entity instanceof Villager) {
            entityId = entity.getId();
        } else if (entity instanceof Merchant) {
            entityId = Integer.MIN_VALUE;
        }
    }

    /**
     * Hall mode interacts with a chosen villager. The trade screen must stay bound
     * to that villager even when another one is standing closer.
     */
    public static void pin(Villager villager) {
        pinned = true;
        entityId = villager.getId();
    }

    public static void unpin() {
        pinned = false;
    }

    public static void bindNearest(Minecraft client) {
        if (pinned && current(client) != null) {
            return;
        }
        if (client.player == null || client.level == null) {
            return;
        }
        Villager remembered = current(client);
        if (remembered != null && McBridge.withinEntity(client.player, remembered)) {
            return;
        }
        AABB box = client.player.getBoundingBox().inflate(8.0);
        Villager nearest = null;
        double best = Double.MAX_VALUE;
        for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
            if (!villager.isAlive()) {
                continue;
            }
            double distance = villager.distanceToSqr(client.player);
            if (distance < best) {
                best = distance;
                nearest = villager;
            }
        }
        if (nearest != null) {
            remember(nearest);
        }
    }

    public static Villager current(Minecraft client) {
        if (client.level == null || entityId == Integer.MIN_VALUE) {
            return null;
        }
        Entity entity = client.level.getEntity(entityId);
        if (entity instanceof Villager villager && villager.isAlive()) {
            return villager;
        }
        return null;
    }

    public static boolean currentIsLibrarian(Minecraft client) {
        Entity entity = current(client);
        return entity != null && McBridge.isLibrarian(entity);
    }
}
