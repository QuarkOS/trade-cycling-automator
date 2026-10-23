package dev.quarkos.villagerreroller.client;

import dev.quarkos.villagerreroller.VillagerReroller;
//? if forge {
/*import net.minecraft.client.gui.components.events.GuiEventListener;
*///?} else {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
//?}
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
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

    public static void setScreen(Minecraft client, Screen screen) {
        //? if >=26.2 {
        /*client.gui.setScreen(screen);
        *///?} else {
        client.setScreen(screen);
        //?}
    }

    public static KeyMapping registerToggle() {
        KeyMapping mapping = new KeyMapping(
                "key." + VillagerReroller.MOD_ID + ".toggle",
                75, // GLFW_KEY_K. 26.3 does not expose org.lwjgl.glfw on the compile classpath.
                //? if <1.21.11 {
                /*"key.categories." + VillagerReroller.MOD_ID
                *///?} else {
                KeyMapping.Category.MISC
                //?}
        );
        //? if forge {
        /*return mapping;
        *///?} else {
        return KeyBindingHelper.registerKeyBinding(mapping);
        //?}
    }

    public static void hookScreenKeys(Screen screen) {
        //? if forge {
        /*return;
        *///?} else {
        //? if <1.21.11 {
        /*ScreenKeyboardEvents.afterKeyPress(screen).register((current, key, scancode, modifiers) -> {
            if (current instanceof MerchantScreen && AutomatorKeys.TOGGLE.matches(key, scancode)) {
                CycleAutomator.toggle();
            }
        });
        *///?} else {
        ScreenKeyboardEvents.afterKeyPress(screen).register((current, event) -> {
            if (current instanceof MerchantScreen && AutomatorKeys.TOGGLE.matches(event)) {
                CycleAutomator.toggle();
            }
        });
        //?}
        //?}
    }

    public static void actionBar(Player player, Component message) {
        //? if >=26.1 {
        /*player.sendOverlayMessage(message);
        *///?} else {
        player.displayClientMessage(message, true);
        //?}
    }

    public static void chat(Player player, Component message) {
        //? if >=26.1 {
        /*player.sendSystemMessage(message);
        *///?} else {
        player.displayClientMessage(message, false);
        //?}
    }

    public static List<AbstractWidget> buttons(Screen screen) {
        //? if forge {
        /*return new ForgeScreenWidgets(screen);
        *///?} else {
        //? if >=26.1 {
        /*return net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen);
        *///?} else {
        return net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen);
        //?}
        //?}
    }

    public static void toast(Minecraft client, Component title, Component message) {
        //? if >=26.2 {
        /*var manager = client.gui.toastManager();
        *///?} else {
        //? if <1.21.11 {
        /*var manager = client.toast;
        *///?} else {
        var manager = client.getToastManager();
        //?}
        //?}
        if (manager != null) {
            SystemToast.add(manager, SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, message);
        }
    }

    public static void swing(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.InteractionHand hand) {
        //? if >=26.3 {
        /*entity.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        *///?} else {
        entity.swing(hand);
        //?}
    }

    public static boolean isLibrarian(Entity entity) {
        if (!(entity instanceof Villager villager)) {
            return false;
        }
        return professionId(villager).equals("minecraft:librarian");
    }

    public static boolean unemployed(Villager villager) {
        //? if <1.21.11 {
        /*return villager.getVillagerData().getProfession() == VillagerProfession.NONE;
        *///?} else {
        return villager.getVillagerData().profession().is(VillagerProfession.NONE);
        //?}
    }

    public static boolean nitwit(Villager villager) {
        //? if <1.21.11 {
        /*return villager.getVillagerData().getProfession() == VillagerProfession.NITWIT;
        *///?} else {
        return villager.getVillagerData().profession().is(VillagerProfession.NITWIT);
        //?}
    }

    public static String professionId(Villager villager) {
        //? if <1.21.11 {
        /*Identifier id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession());
        return id == null ? "unknown" : id.toString();
        *///?} else {
        return villager.getVillagerData().profession().unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
        //?}
    }

    public static boolean isJobSite(Villager villager, Holder<PoiType> poi) {
        //? if <1.21.11 {
        /*return villager.getVillagerData().getProfession().heldJobSite().test(poi);
        *///?} else {
        return villager.getVillagerData().profession().value().heldJobSite().test(poi);
        //?}
    }

    public static boolean withinBlock(Player player, BlockPos pos) {
        //? if <1.21.11 {
        /*return player.canInteractWithBlock(pos, 0.0);
        *///?} else {
        return player.isWithinBlockInteractionRange(pos, 0.0);
        //?}
    }

    public static boolean withinEntity(Player player, Entity entity) {
        //? if <1.21.11 {
        /*return player.canInteractWithEntity(entity, 0.0);
        *///?} else {
        return player.isWithinEntityInteractionRange(entity, 0.0);
        //?}
    }

    public static <T extends Entity> List<T> nearby(Level level, Class<T> type, AABB box) {
        //? if >=26.1 {
        /*return level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(type), box, entity -> true);
        *///?} else {
        return level.getEntitiesOfClass(type, box);
        //?}
    }

    public static int storageSize(Inventory inventory) {
        //? if <1.21.11 {
        /*return inventory.items.size();
        *///?} else {
        return inventory.getNonEquipmentItems().size();
        //?}
    }

    public static void selectHotbar(Inventory inventory, int slot) {
        //? if <1.21.11 {
        /*inventory.selected = slot;
        *///?} else {
        inventory.setSelectedSlot(slot);
        //?}
    }

    public static int selectedHotbar(Inventory inventory) {
        //? if <1.21.11 {
        /*return inventory.selected;
        *///?} else {
        return inventory.getSelectedSlot();
        //?}
    }

    public static void swapWithHotbar(Minecraft client, LocalPlayer player, int index, int hotbar) {
        //? if >=26.1 {
        /*client.gameMode.handleContainerInput(player.inventoryMenu.containerId, index, hotbar, net.minecraft.world.inventory.ContainerInput.SWAP, player);
        *///?} else {
        client.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, index, hotbar, net.minecraft.world.inventory.ClickType.SWAP, player);
        //?}
    }

    public static void sendLook(LocalPlayer player, float yaw, float pitch) {
        //? if <1.21.11 {
        /*player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround()));
        *///?} else {
        player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(), player.horizontalCollision));
        //?}
    }

    public static void interact(MultiPlayerGameMode gameMode, LocalPlayer player, Entity entity, InteractionHand hand) {
        //? if >=26.1 {
        /*gameMode.interact(player, entity, new net.minecraft.world.phys.EntityHitResult(entity), hand);
        *///?} else {
        gameMode.interact(player, entity, hand);
        //?}
    }

    public static Optional<Holder<Enchantment>> enchantmentHolder(RegistryAccess access, String id) {
        HolderGetter<Enchantment> lookup = access.lookupOrThrow(Registries.ENCHANTMENT);
        Identifier parsed = Identifier.parse(id);
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, parsed);
        return lookup.get(key).map(holder -> (Holder<Enchantment>) holder);
    }

    //? if forge {
    /*private static final class ForgeScreenWidgets extends java.util.AbstractList<AbstractWidget> {
        private final Screen screen;

        private ForgeScreenWidgets(Screen screen) {
            this.screen = screen;
        }

        @Override
        public AbstractWidget get(int index) {
            return widgets().get(index);
        }

        @Override
        public int size() {
            return widgets().size();
        }

        @Override
        public boolean add(AbstractWidget widget) {
            screen.addRenderableWidget(widget);
            return true;
        }

        @Override
        public boolean remove(Object widget) {
            if (widget instanceof GuiEventListener listener) {
                screen.removeWidget(listener);
                return true;
            }
            return false;
        }

        private java.util.List<AbstractWidget> widgets() {
            java.util.List<AbstractWidget> found = new java.util.ArrayList<>();
            for (GuiEventListener child : screen.children()) {
                if (child instanceof AbstractWidget widget) {
                    found.add(widget);
                }
            }
            return found;
        }
    }
    *///?}
}
