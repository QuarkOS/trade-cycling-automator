package dev.quarkos.villagerreroller.server;

import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;

public final class OfferSync {
    private OfferSync() {
    }

    /**
     * Clears the villager's offers and lets vanilla roll them again.
     * {@code getOffers} fills a null list through the same path as a new profession.
     */
    public static boolean reroll(Villager villager) {
        // overrideOffers is a no-op on AbstractVillager. setOffers writes the field,
        // and a null list makes the next getOffers roll a new set.
        villager.setOffers(null);
        MerchantOffers fresh = villager.getOffers();
        return fresh != null && !fresh.isEmpty();
    }

    public static void push(ServerPlayer player, MerchantMenu menu, Villager villager) {
        MerchantOffers fresh = villager.getOffers();
        menu.setOffers(fresh);
        //? if >=26.2 {
        /*player.connection.send(new ClientboundMerchantOffersPacket(
                menu.containerId,
                fresh,
                menu.getTraderLevel(),
                menu.getTraderXp(),
                menu.showProgressBar(),
                menu.canRestock()));
        *///?} else {
        player.connection.send(new ClientboundMerchantOffersPacket(
                menu.containerId,
                fresh,
                menu.getTraderLevel(),
                menu.getTraderXp(),
                menu.showProgressBar(),
                menu.canRestock()));
        //?}
    }
}
