package dev.tradecycling.automator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;

/**
 * The client trade menu holds a {@code ClientSideMerchant}, not the villager.
 * The last entity the player used is what we check for the librarian profession.
 */
public final class LibrarianTracker {
    private static int entityId = Integer.MIN_VALUE;

    private LibrarianTracker() {
    }

    public static void remember(Entity entity) {
        if (McBridge.isLibrarian(entity)) {
            entityId = entity.getId();
        } else if (entity instanceof Merchant) {
            entityId = Integer.MIN_VALUE;
        }
    }

    public static boolean currentIsLibrarian(Minecraft client) {
        if (client.level == null || entityId == Integer.MIN_VALUE) {
            return false;
        }
        Entity entity = client.level.getEntity(entityId);
        return entity != null && entity.isAlive() && McBridge.isLibrarian(entity);
    }
}
