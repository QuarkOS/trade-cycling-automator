package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.config.VillagerRoster;
import net.minecraft.core.Registry;
//? if >=1.19.3 {
import net.minecraft.core.registries.BuiltInRegistries;
//?}
//? if >=26.1 {
/*import net.minecraft.core.registries.Registries;
*///?}
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class TradeIcons {
    private TradeIcons() {
    }

    record Job(Identifier id, Component name, ItemStack icon) {
    }

    static List<Job> jobs(Level level) {
        //? if <1.19.3 {
        /*Registry<VillagerProfession> professions = Registry.VILLAGER_PROFESSION;
        Registry<PoiType> sites = Registry.POINT_OF_INTEREST_TYPE;
        *///?} else {
        Registry<VillagerProfession> professions = BuiltInRegistries.VILLAGER_PROFESSION;
        Registry<PoiType> sites = BuiltInRegistries.POINT_OF_INTEREST_TYPE;
        //?}
        //? if >=26.1 {
        /*if (level != null) {
            professions = level.registryAccess().lookup(Registries.VILLAGER_PROFESSION).orElse(professions);
        }
        *///?}
        List<Job> jobs = new ArrayList<>();
        for (VillagerProfession profession : professions) {
            Identifier id = professions.getKey(profession);
            if (id == null || id.toString().equals("minecraft:none") || id.toString().equals("minecraft:nitwit")) {
                continue;
            }
            sites.stream().flatMap(site -> site.matchingStates().stream())
                    .filter(state -> PoiTypes.forState(state).filter(profession.heldJobSite()).isPresent())
                    .map(state -> state.getBlock().asItem())
                    .filter(item -> item != Items.AIR)
                    .min(Comparator.comparing(TradeEntry::itemId))
                    .ifPresent(item -> jobs.add(new Job(id, professionName(id.toString()), new ItemStack(item))));
        }
        jobs.sort(Comparator.comparing(job -> job.name().getString()));
        return List.copyOf(jobs);
    }

    static Component professionName(String value) {
        Identifier id = value == null ? null : Identifier.tryParse(value);
        if (id == null) {
            return Component.empty();
        }
        String key = "entity." + id.getNamespace() + ".villager." + id.getPath();
        return Language.getInstance().has(key) ? Component.translatable(key)
                : Component.literal(VillagerRoster.prettyTrade("item:minecraft:" + id.getPath()));
    }

    static ItemStack item(String value) {
        Identifier id = value == null || value.isEmpty() ? null : Identifier.tryParse(value);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        //? if <1.19.3 {
        /*return new ItemStack(Registry.ITEM.getOptional(id).orElse(Items.AIR));
        *///?} else {
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR));
        //?}
    }

    static ItemStack forTrade(String tradeId) {
        String[] parts = tradeId.split(":");
        if (parts.length >= 3) {
            switch (parts[0]) {
                case "buy", "sell", "gear", "dye", "convert":
                    return item(parts[1] + ":" + parts[2]);
                case "typed":
                    if (parts.length >= 5) {
                        return item(parts[3] + ":" + parts[4]);
                    }
                    break;
                case "enchant":
                    return new ItemStack(Items.ENCHANTED_BOOK);
                case "map":
                    return new ItemStack(Items.FILLED_MAP);
                case "arrows":
                    return new ItemStack(Items.TIPPED_ARROW);
                case "stew":
                    return new ItemStack(Items.SUSPICIOUS_STEW);
                default:
                    break;
            }
        }
        return ItemStack.EMPTY;
    }

    static int bookLevel(String tradeId) {
        if (!tradeId.startsWith("enchant:")) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(tradeId.substring(tradeId.lastIndexOf(':') + 1)));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    static String levelBadge(int level) {
        return level > 0 ? VillagerRoster.roman(level) : null;
    }
}
