package dev.quarkos.villagerreroller.client;

import dev.quarkos.villagerreroller.VillagerReroller;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;

/**
 * Version-specific Minecraft calls. Stonecutter keeps the 1.21.11 branch live
 * and swaps the others when it builds 1.21.1 or 26.x.
 */
public final class McBridge {
    private McBridge() {
    }

    public static Screen currentScreen(Minecraft client) {
        //? if >=26.2 {
        /*return client.gui.screen();
        *///?} else {
        return client.screen;
        //?}
    }

    public static KeyMapping registerToggle() {
        KeyMapping mapping = new KeyMapping(
                "key." + VillagerReroller.MOD_ID + ".toggle",
                75, // GLFW_KEY_K. 26.3 does not expose org.lwjgl.glfw on the compile classpath.
                //? if >=1.21.9 {
                KeyMapping.Category.MISC
                //?} else {
                /*"key.categories." + VillagerReroller.MOD_ID
                *///?}
        );
        return KeyBindingHelper.registerKeyBinding(mapping);
    }

    public static void hookScreenKeys(Screen screen) {
        //? if >=1.21.9 {
        ScreenKeyboardEvents.afterKeyPress(screen).register((current, event) -> {
            if (current instanceof MerchantScreen && AutomatorKeys.TOGGLE.matches(event)) {
                CycleAutomator.toggle();
            }
        });
        //?} else {
        /*ScreenKeyboardEvents.afterKeyPress(screen).register((current, key, scancode, modifiers) -> {
            if (current instanceof MerchantScreen && AutomatorKeys.TOGGLE.matches(key, scancode)) {
                CycleAutomator.toggle();
            }
        });
        *///?}
    }

    public static void actionBar(Player player, net.minecraft.network.chat.Component message) {
        //? if >=26.1 {
        /*player.sendOverlayMessage(message);
        *///?} else {
        player.displayClientMessage(message, true);
        //?}
    }

    public static boolean isLibrarian(Entity entity) {
        //? if >=1.21.11 {
        if (!(entity instanceof net.minecraft.world.entity.npc.villager.Villager villager)) {
            return false;
        }
        return villager.getVillagerData().profession().is(net.minecraft.world.entity.npc.villager.VillagerProfession.LIBRARIAN);
        //?} elif >=1.21.5 {
        /*if (!(entity instanceof net.minecraft.world.entity.npc.Villager villager)) {
            return false;
        }
        return villager.getVillagerData().profession().is(net.minecraft.world.entity.npc.VillagerProfession.LIBRARIAN);
        *///?} else {
        /*if (!(entity instanceof net.minecraft.world.entity.npc.Villager villager)) {
            return false;
        }
        return villager.getVillagerData().getProfession() == net.minecraft.world.entity.npc.VillagerProfession.LIBRARIAN;
        *///?}
    }

    public static String keyId(ResourceKey<?> key) {
        //? if >=1.21.11 {
        return key.identifier().toString();
        //?} else {
        /*return key.location().toString();
        *///?}
    }

    public static boolean withinBlock(Player player, BlockPos pos) {
        //? if >=1.21.11 {
        return player.isWithinBlockInteractionRange(pos, 0.0);
        //?} elif >=1.21.5 {
        /*return player.canInteractWithBlock(pos, 0.0);
        *///?} else {
        /*return player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= 36.0;
        *///?}
    }

    public static boolean withinEntity(Player player, Entity entity) {
        //? if >=1.21.11 {
        return player.isWithinEntityInteractionRange(entity, 0.0);
        //?} elif >=1.21.5 {
        /*return player.canInteractWithEntity(entity, 0.0);
        *///?} else {
        /*return player.distanceToSqr(entity) <= 36.0;
        *///?}
    }

    public static Optional<Holder<Enchantment>> enchantmentHolder(RegistryAccess access, String id) {
        HolderGetter<Enchantment> lookup = access.lookupOrThrow(Registries.ENCHANTMENT);
        Identifier parsed = Identifier.parse(id);
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, parsed);
        return lookup.get(key).map(holder -> (Holder<Enchantment>) holder);
    }
}
