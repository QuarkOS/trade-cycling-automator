//? if forge {
/*
package dev.quarkos.villagerreroller;

import dev.quarkos.villagerreroller.client.AutomatorKeys;
import dev.quarkos.villagerreroller.client.CycleAutomator;
import dev.quarkos.villagerreroller.config.ForgeConfigScreen;
import dev.quarkos.villagerreroller.net.ForgeChannels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
//? if <26.1 {
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?}
//? if <1.21.6 {
import net.minecraftforge.eventbus.api.IEventBus;
//?}

public final class ForgeClientHooks {
    public static void init() {
        VillagerReroller.bootstrap();
        ForgeChannels.registerClient();
        new ForgeClientHooks().register();
    }

    private void register() {
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
        //?}
        //? if >=1.21.6 && <26.1 {
        RegisterKeyMappingsEvent.getBus(FMLJavaModLoadingContext.get().getModBusGroup()).addListener(this::registerKeys);
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> VillagerReroller.onClientTick(Minecraft.getInstance()));
        ScreenEvent.Init.Post.BUS.addListener(event -> VillagerReroller.onScreen(Minecraft.getInstance(), event.getScreen()));
        ScreenEvent.KeyPressed.Pre.BUS.addListener(this::onKeyBus);
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
*///?} else {
package dev.quarkos.villagerreroller;

public final class ForgeClientHooks {
    private ForgeClientHooks() {
    }
}
//?}
