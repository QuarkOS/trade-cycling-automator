package dev.quarkos.villagerreroller.picker;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
//? if <1.20.5 {
/*import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
*///?}
//? if >=1.20.5 && <1.21 {
/*import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
*///?}
//? if >=1.21 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
//?}

/**
 * Durability and enchantment facts for one stack. Unbreakable stacks report
 * {@link Integer#MAX_VALUE} remaining so the floor never retires them.
 */
public final class ToolWear {
    private ToolWear() {
    }

    public static int remaining(ItemStack stack) {
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, max - stack.getDamageValue());
    }

    public static float speed(ItemStack stack, BlockState state) {
        return stack.getItem().getDestroySpeed(stack, state);
    }

    public static boolean correct(ItemStack stack, BlockState state) {
        //? if <1.20.5 {
        /*return stack.getItem().isCorrectToolForDrops(state);
        *///?} else {
        return stack.getItem().isCorrectToolForDrops(stack, state);
        //?}
    }

    public static boolean handCanMine(BlockState state) {
        return !state.requiresCorrectToolForDrops();
    }

    public static boolean enchanted(ItemStack stack) {
        //? if <1.20.5 {
        /*return !EnchantmentHelper.getEnchantments(stack).isEmpty();
        *///?}
        //? if >=1.20.5 && <1.21 {
        /*ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        return !enchants.isEmpty();
        *///?}
        //? if >=1.21 {
        return !enchants(stack).isEmpty();
        //?}
    }

    public static boolean mending(ItemStack stack) {
        //? if <1.20.5 {
        /*return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, stack) > 0;
        *///?}
        //? if >=1.20.5 && <1.21 {
        /*return EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(Enchantments.MENDING) > 0;
        *///?}
        //? if >=1.21 {
        ItemEnchantments enchants = enchants(stack);
        if (enchants.isEmpty()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        return minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.MENDING)
                .map(holder -> enchants.getLevel(holder) > 0)
                .orElse(false);
        //?}
    }

    //? if >=1.21 {
    private static ItemEnchantments enchants(ItemStack stack) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }
    //?}
}
