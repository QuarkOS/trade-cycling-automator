//? if neoforge {
/*
package dev.quarkos.villagerreroller;

import dev.quarkos.villagerreroller.client.AutomatorKeys;
import dev.quarkos.villagerreroller.config.ForgeConfigScreen;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
//? if <1.20.4 {
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
//?}
//? if <1.20.6 {
import net.neoforged.neoforge.event.TickEvent;
//?}
//? if >=1.20.6 {
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
//?}

@Mod(VillagerReroller.MOD_ID)
public final class NeoForgeVillagerReroller {
    //? if <1.20.4 {
    public NeoForgeVillagerReroller() {
        register(FMLJavaModLoadingContext.get().getModEventBus());
    }
    //?} else {
    public NeoForgeVillagerReroller(IEventBus modBus) {
        register(modBus);
    }
    //?}

    private void register(IEventBus modBus) {
        VillagerReroller.bootstrap();
        //? if <1.20.6 {
        ModLoadingContext.get().registerExtensionPoint(
                net.neoforged.neoforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.neoforged.neoforge.client.ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new ForgeConfigScreen(parent)));
        //?} else {
        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (container, parent) -> new ForgeConfigScreen(parent));
        //?}
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            if (AutomatorKeys.TOGGLE != null) {
                event.register(AutomatorKeys.TOGGLE);
            }
            if (AutomatorKeys.CONFIG != null) {
                event.register(AutomatorKeys.CONFIG);
            }
        });
        //? if <1.20.6 {
        NeoForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            VillagerReroller.onClientTick(Minecraft.getInstance());
        });
        //?} else {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> VillagerReroller.onClientTick(Minecraft.getInstance()));
        //?}
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) ->
                VillagerReroller.onScreen(Minecraft.getInstance(), event.getScreen()));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.KeyPressed.Pre event) -> {
            if (!(event.getScreen() instanceof MerchantScreen) || AutomatorKeys.TOGGLE == null) {
                return;
            }
            boolean matches;
            //? if >=1.21.9 {
            matches = AutomatorKeys.TOGGLE.matches(event.getKeyEvent());
            //?} else {
            matches = AutomatorKeys.TOGGLE.matches(event.getKeyCode(), event.getScanCode());
            //?}
            if (!matches) {
                return;
            }
            JobSiteReroller.onToggleKey();
            event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract event) -> {
            if (event.getLevel().isClientSide()) {
                VillagerReroller.remember(event.getTarget());
            }
        });
    }
}
*///?}
