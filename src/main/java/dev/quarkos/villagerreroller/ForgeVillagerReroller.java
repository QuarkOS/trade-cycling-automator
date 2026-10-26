//? if forge {
/*
package dev.quarkos.villagerreroller;

import dev.quarkos.villagerreroller.client.AutomatorKeys;
import dev.quarkos.villagerreroller.client.CycleAutomator;
import dev.quarkos.villagerreroller.config.ForgeConfigScreen;
import dev.quarkos.villagerreroller.picker.TradeHallHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
//? if <26.1 {
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?}
//? if <1.21.6 {
import net.minecraftforge.eventbus.api.IEventBus;
//?}

@Mod(VillagerReroller.MOD_ID)
public final class ForgeVillagerReroller {
    public ForgeVillagerReroller() {
        VillagerReroller.bootstrap();
        //? if <1.20.4 {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new ForgeConfigScreen(parent)));
        //?}
        //? if >=1.20.4 {
        MinecraftForge.registerConfigScreen(ForgeConfigScreen::new);
        //?}
        //? if <1.21.6 {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::registerKeys);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(this::onScreenInit);
        MinecraftForge.EVENT_BUS.addListener(this::onKey);
        MinecraftForge.EVENT_BUS.addListener(this::onEntity);
        //? if <1.20 {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.RenderGuiEvent.Post event) -> TradeHallHud.draw(event.getPoseStack()));
        //?}
        //? if >=1.20 && <1.20.6 {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.RenderGuiEvent.Post event) -> TradeHallHud.draw(event.getGuiGraphics()));
        //?}
        //? if >=1.20.6 {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.CustomizeGuiOverlayEvent.Chat event) -> TradeHallHud.draw(event.getGuiGraphics()));
        //?}
        //?}
        //? if >=1.21.6 && <26.1 {
        RegisterKeyMappingsEvent.getBus(FMLJavaModLoadingContext.get().getModBusGroup()).addListener(this::registerKeys);
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> VillagerReroller.onClientTick(Minecraft.getInstance()));
        ScreenEvent.Init.Post.BUS.addListener(event -> VillagerReroller.onScreen(Minecraft.getInstance(), event.getScreen()));
        ScreenEvent.KeyPressed.Pre.BUS.addListener(this::onKeyBus);
        net.minecraftforge.client.event.CustomizeGuiOverlayEvent.Chat.BUS.addListener((java.util.function.Consumer<net.minecraftforge.client.event.CustomizeGuiOverlayEvent.Chat>) event -> TradeHallHud.draw(event.getGuiGraphics()));
        PlayerInteractEvent.EntityInteract.BUS.addListener(event -> {
            if (event.getLevel().isClientSide()) {
                VillagerReroller.remember(event.getTarget());
            }
        });
        //?}
        //? if >=26.1 {
        RegisterKeyMappingsEvent.BUS.addListener(this::registerKeys);
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> VillagerReroller.onClientTick(Minecraft.getInstance()));
        ScreenEvent.Init.Post.BUS.addListener(event -> VillagerReroller.onScreen(Minecraft.getInstance(), event.getScreen()));
        ScreenEvent.KeyPressed.Pre.BUS.addListener(this::onKeyBus);
        net.minecraftforge.client.event.AddGuiOverlayLayersEvent.BUS.addListener(event -> event.getLayeredDraw().add(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(VillagerReroller.MOD_ID, "hall"),
                (graphics, delta) -> TradeHallHud.draw(graphics)));
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(event -> {
            if (event.getLevel().isClientSide()) {
                VillagerReroller.remember(event.getTarget());
            }
        });
        //?}
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        if (AutomatorKeys.TOGGLE != null) {
            event.register(AutomatorKeys.TOGGLE);
        }
    }

    //? if <1.21.6 {
    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        VillagerReroller.onClientTick(Minecraft.getInstance());
    }

    private void onScreenInit(ScreenEvent.Init.Post event) {
        VillagerReroller.onScreen(Minecraft.getInstance(), event.getScreen());
    }

    private void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof MerchantScreen) || AutomatorKeys.TOGGLE == null) {
            return;
        }
        if (AutomatorKeys.TOGGLE.matches(event.getKeyCode(), event.getScanCode())) {
            CycleAutomator.toggle();
            event.setCanceled(true);
        }
    }

    private void onEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            VillagerReroller.remember(event.getTarget());
        }
    }
    //?}

    //? if >=1.21.6 {
    private boolean onKeyBus(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof MerchantScreen) || AutomatorKeys.TOGGLE == null) {
            return false;
        }
        boolean matches;
        //? if >=1.21.9 {
        matches = AutomatorKeys.TOGGLE.matches(event.getInfo());
        //?} else {
        matches = AutomatorKeys.TOGGLE.matches(event.getKeyCode(), event.getScanCode());
        //?}
        if (!matches) {
            return false;
        }
        CycleAutomator.toggle();
        return true;
    }
    //?}
}
*///?}
