//? if forge {
/*package dev.quarkos.villagerreroller;

import dev.quarkos.villagerreroller.client.AutomatorKeys;
import dev.quarkos.villagerreroller.client.CycleAutomator;
import dev.quarkos.villagerreroller.config.AutomatorConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VillagerReroller.MOD_ID)
public final class ForgeVillagerReroller {
    public ForgeVillagerReroller() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::registerKeys);
        VillagerReroller.bootstrap();
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(this::configScreen)
        );
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(this::onScreenInit);
        MinecraftForge.EVENT_BUS.addListener(this::onKey);
        MinecraftForge.EVENT_BUS.addListener(this::onEntity);
    }

    private Screen configScreen(Minecraft client, Screen parent) {
        return AutomatorConfigScreen.create(parent);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        if (AutomatorKeys.TOGGLE != null) {
            event.register(AutomatorKeys.TOGGLE);
        }
    }

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
}
*///?}
