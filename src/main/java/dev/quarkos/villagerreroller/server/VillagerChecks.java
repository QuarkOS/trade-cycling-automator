package dev.quarkos.villagerreroller.server;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;

public final class VillagerChecks {
    private VillagerChecks() {
    }

    public static boolean noProfession(Villager villager) {
        //? if <1.21.5 {
        /*VillagerProfession profession = villager.getVillagerData().getProfession();
        return profession == null || profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT;
        *///?} else {
        var profession = villager.getVillagerData().profession().unwrapKey().orElse(null);
        return profession == null || profession.equals(VillagerProfession.NONE) || profession.equals(VillagerProfession.NITWIT);
        //?}
    }

    public static int level(Villager villager) {
        //? if <1.21.5 {
        /*return villager.getVillagerData().getLevel();
        *///?} else {
        return villager.getVillagerData().level();
        //?}
    }

    public static boolean anyUses(Villager villager) {
        for (MerchantOffer offer : villager.getOffers()) {
            if (offer.getUses() > 0) {
                return true;
            }
        }
        return false;
    }
}
