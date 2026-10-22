package dev.tradecycling.automator.client;

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

    private LibrarianTracker() {
    }

    public static void remember(Entity entity) {
        if (entity instanceof Villager) {
            entityId = entity.getId();
        } else if (entity instanceof Merchant) {
            entityId = Integer.MIN_VALUE;
        }
    }

    public static void bindNearest(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        AABB box = client.player.getBoundingBox().inflate(8.0);
        Villager nearest = null;
        double best = Double.MAX_VALUE;
        for (Villager villager : client.level.getEntitiesOfClass(Villager.class, box)) {
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
