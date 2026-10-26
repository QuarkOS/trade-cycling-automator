package dev.quarkos.villagerreroller.harness;

import com.mojang.blaze3d.platform.NativeImage;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.compat.TradeCyclingHook;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.RerollMethod;
import dev.quarkos.villagerreroller.picker.TradeEntry;
import dev.quarkos.villagerreroller.picker.TradePickerUi;
import dev.quarkos.villagerreroller.picker.TradeSession;
import dev.quarkos.villagerreroller.text.ScrollingNames;
import dev.quarkos.villagerreroller.text.TradeNameLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
//? if >=1.21.11 {
import net.minecraft.client.gui.ActiveTextCollector;
//?}
//? if >=1.21.9 {
import net.minecraft.client.input.InputWithModifiers;
//?}
//? if >=1.19.4 {
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
//?}
//? if >=1.20.2 {
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
//?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
//? if <1.21.11 {
/*import net.minecraft.Util;
*///?} else {
import net.minecraft.util.Util;
//?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// In-game checks for a production jar. Fabric 1.21.5+ dev runs still use the
// client gametest. This tick harness runs on every version, including those,
// when -Dvillager_reroller.harness=true is set. Normal launches ignore it.
public final class ClientHarness121 {
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");
    private static final int SCALE = Integer.getInteger("villager_reroller.timeoutScale", 1);

    private static boolean armed;
    private static State state = State.WAIT_TITLE;
    private static int ticks;
    private static int x;
    private static int y;
    private static int z;
    private static double villagerX;
    private static double villagerZ;
    private static boolean cycling;
    private static boolean sawAir;
    private static boolean midShot;
    private static boolean maxShot;
    private static boolean jobSiteShot;
    private static boolean cyclingPanelShot;
    private static boolean rerollShot;
    private static boolean oddsShot;
    private static boolean oddsCleared;
    private static int lecternsBefore;
    private static String stopReason = "running";
    private static final java.util.ArrayDeque<String> COMMANDS = new java.util.ArrayDeque<>();
    private static boolean platformQueued;
    private static int commandGap;

    private ClientHarness121() {
    }

    public static void start(Minecraft client) {
        if (!Boolean.getBoolean("villager_reroller.harness")) {
            return;
        }
        if (!armed) {
            armed = true;
            client.options.pauseOnLostFocus = false;
            //? if >=1.19.4 {
            client.options.onboardAccessibility = false;
            //?}
            //? if >=1.20.5 && <1.21 {
            /*client.options.menuBackgroundBlurriness().set(0.0);
            *///?}
            //? if >=1.21 {
            client.options.menuBackgroundBlurriness().set(0);
            //?}
            client.options.guiScale().set(2);
            client.options.renderDistance().set(2);
            client.options.simulationDistance().set(5);
            client.options.framerateLimit().set(20);
            LOGGER.info("1.21.1 client harness armed");
            note("villager_reroller harness check mod-loaded");
        }
        ticks++;
        try {
            switch (state) {
                case WAIT_TITLE -> waitTitle(client);
                case OPEN_CONFIG -> openConfig(client);
                case WAIT_CONFIG -> waitConfig(client);
                case WAIT_PRESETS -> waitPresets(client);
                case WAIT_PLAYER -> waitPlayer(client);
                case WAIT_LIBRARIAN -> waitLibrarian(client);
                case WAIT_MERCHANT -> waitMerchant(client);
                case PANEL -> openPanel(client);
                case WAIT_SCROLL -> waitScroll(client);
                case START_REROLL -> startReroll(client);
                case WAIT_FOUND -> waitFound(client);
                case DONE -> finish(client);
            }
        } catch (RuntimeException e) {
            fail(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private static void waitTitle(Minecraft client) {
        //? if >=1.19.4 {
        if (McBridge.currentScreen(client) instanceof AccessibilityOnboardingScreen) {
            //? if >=1.21 {
            client.options.onboardingAccessibilityFinished();
            //?}
            McBridge.setScreen(client, new TitleScreen());
            return;
        }
        //?}
        //? if >=1.20.2 {
        if (McBridge.currentScreen(client) instanceof NoticeWithLinkScreen notice) {
            notice.onClose();
            return;
        }
        //?}
        //? if forge {
        /*if (McBridge.currentScreen(client) instanceof net.minecraftforge.client.gui.LoadingErrorScreen) {
            for (AbstractWidget widget : McBridge.buttons(McBridge.currentScreen(client))) {
                if (widget instanceof Button button
                        && button.getMessage().getString().toLowerCase(java.util.Locale.ROOT).contains("continue")) {
                    pressButton(button);
                    McBridge.setScreen(client, new TitleScreen());
                    return;
                }
            }
        }
        *///?}
        Screen early = McBridge.currentScreen(client);
        if (early != null && !(early instanceof TitleScreen) && ticks > 20 && ticks % 20 == 0) {
            String typeName = early.getClass().getName().toLowerCase(java.util.Locale.ROOT);
            if (typeName.contains("notice") || typeName.contains("safety") || typeName.contains("warning") || typeName.contains("backup")) {
                for (AbstractWidget widget : McBridge.buttons(early)) {
                    if (widget instanceof Button button) {
                        String label = button.getMessage().getString().toLowerCase(java.util.Locale.ROOT);
                        if (label.contains("continue") || label.contains("proceed") || label.contains("offline")) {
                            pressButton(button);
                            return;
                        }
                    }
                }
            }
        }
        if (!(McBridge.currentScreen(client) instanceof TitleScreen)) {
            if (ticks > timeout(20 * 45)) {
                fail("title screen did not open, screen=" + McBridge.currentScreen(client));
            }
            return;
        }
        // The Mojang splash is an overlay drawn on top of the title screen. A screenshot
        // taken while it is up is the red logo, not the config screen underneath.
        if (loadingOverlay(client)) {
            if (ticks > timeout(20 * 120)) {
                fail("loading overlay did not finish, screen=" + McBridge.currentScreen(client));
            }
            return;
        }
        state = State.OPEN_CONFIG;
        ticks = 0;
    }

    private static void openConfig(Minecraft client) {
        if (!(McBridge.currentScreen(client) instanceof TitleScreen)) {
            if (ticks > timeout(20 * 20)) {
                fail("title screen left before the config screen, screen=" + McBridge.currentScreen(client));
            }
            return;
        }
        Screen config;
        //? if forge {
        /*config = new dev.quarkos.villagerreroller.config.ForgeConfigScreen(McBridge.currentScreen(client));
        *///?} else {
        config = dev.quarkos.villagerreroller.config.AutomatorConfigScreen.create(McBridge.currentScreen(client));
        //?}
        if (config == null) {
            fail("config screen factory returned null");
            return;
        }
        McBridge.setScreen(client, config);
        state = State.WAIT_CONFIG;
        ticks = 0;
    }

    private static void waitConfig(Minecraft client) {
        Screen screen = McBridge.currentScreen(client);
        if (screen == null || screen instanceof TitleScreen || loadingOverlay(client)) {
            if (loadingOverlay(client)) {
                ticks = 0;
            }
            if (ticks > timeout(20 * 30)) {
                fail("config screen did not open, screen=" + screen);
            }
            return;
        }
        // The tick runs before the frame is presented, so wait for a rendered frame.
        if (ticks < 20) {
            return;
        }
        String title = screen.getTitle().getString();
        String typeName = screen.getClass().getName();
        boolean config = title.contains("Villager")
                || title.contains("villager_reroller")
                || typeName.contains("ForgeConfigScreen")
                || typeName.toLowerCase(java.util.Locale.ROOT).contains("cloth");
        if (!config) {
            fail("config screen opened the wrong screen: " + typeName + " title=" + title);
            return;
        }
        shot(client, "config-screen");
        note("villager_reroller harness check config-screen");
        openCreateWorld(client, screen);
        state = State.WAIT_PRESETS;
        ticks = 0;
    }

    private static boolean loadingOverlay(Minecraft client) {
        //? if >=26.2 {
        /*return client.gui.overlay() != null;
        *///?} else {
        return client.getOverlay() != null;
        //?}
    }

    private static void openCreateWorld(Minecraft client, Screen parent) {
        //? if <1.21.9 {
        /*CreateWorldScreen.openFresh(client, parent);
        *///?} else {
        CreateWorldScreen.openFresh(client, () -> McBridge.setScreen(client, new TitleScreen()));
        //?}
    }

    private static void waitPresets(Minecraft client) {
        if (!(McBridge.currentScreen(client) instanceof CreateWorldScreen create)) {
            if (ticks > timeout(20 * 60)) {
                fail("create-world screen did not open");
            }
            return;
        }
        //? if <1.19.4 {
        /*        if (ticks < 100) {
            // Forge's create-world screen reloads datapacks after it opens and then rebuilds the
            // widgets. Applying the flat preset or cheats before that reload is wiped, and creating
            // during the reload never starts the server.
            return;
        }
        if (ticks == 100) {
            applyLegacyFlat(create);
            return;
        }
        if (ticks < 105) {
            return;
        }
        applyLegacyFlat(create);
        LOGGER.info("creating legacy world, commands={}, screen={}", create.commands, McBridge.currentScreen(client));
        create.onCreate();
        *///?}
        //? if >=1.19.4 {
        var presets = create.getUiState().getNormalPresetList();
        if (presets.isEmpty()) {
            if (ticks > timeout(20 * 60)) {
                fail("world presets never loaded");
            }
            return;
        }
        WorldCreationUiState ui = create.getUiState();
        ui.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
        //? if <1.20.5 {
        /*ui.setAllowCheats(true);
        *///?} else {
        ui.setAllowCommands(true);
        //?}
        ui.setName("reroll");
        presets.stream()
                .filter(entry -> entry.preset().is(WorldPresets.FLAT))
                .findFirst()
                .ifPresent(ui::setWorldType);
        create.onCreate();
        //?}
        state = State.WAIT_PLAYER;
        ticks = 0;
    }

    private static void waitPlayer(Minecraft client) {
        Screen current = McBridge.currentScreen(client);
        if (current instanceof net.minecraft.client.gui.screens.ConfirmScreen) {
            for (AbstractWidget widget : McBridge.buttons(current)) {
                if (widget instanceof Button button) {
                    pressButton(button);
                    return;
                }
            }
        }
        if (current instanceof CreateWorldScreen && ticks > 20 && ticks % 20 == 0 && !press(current, "selectWorld.create")) {
            for (AbstractWidget widget : McBridge.buttons(current)) {
                if (widget instanceof Button button
                        && button.getMessage().getString().toLowerCase(java.util.Locale.ROOT).contains("create")) {
                    pressButton(button);
                    break;
                }
            }
        }
        if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
            if (ticks > timeout(20 * 180)) {
                fail("world did not load, screen=" + McBridge.currentScreen(client));
            }
            return;
        }
        if (ticks < 40) {
            return;
        }
        if (client.player == null) {
            return;
        }
        if (!platformQueued) {
            BlockPos feet = client.player.blockPosition();
            x = feet.getX() + 6;
            y = feet.getY();
            z = feet.getZ();
            villagerX = x + 1.15;
            villagerZ = z + 0.5;
            //? if >=26.2 {
            /*enqueue("gamerule send_command_feedback false");
            enqueue("gamerule advance_time false");
            enqueue("gamerule spawn_mobs false");
            enqueue("gamerule immediate_respawn true");
            *///?} else {
            enqueue("gamerule sendCommandFeedback false");
            enqueue("gamerule doDaylightCycle false");
            enqueue("gamerule doMobSpawning false");
            enqueue("gamerule doImmediateRespawn true");
            //?}
            enqueue("time set day");
            enqueue("fill " + (x - 2) + " " + (y - 1) + " " + (z - 2) + " " + (x + 3) + " " + (y - 1) + " " + (z + 2) + " minecraft:stone");
            enqueue("fill " + (x - 2) + " " + y + " " + (z - 2) + " " + (x + 3) + " " + (y + 3) + " " + (z + 2) + " minecraft:air");
            enqueue("setblock " + x + " " + y + " " + z + " minecraft:lectern");
            enqueue("setblock " + (x - 1) + " " + y + " " + z + " minecraft:oak_fence");
            enqueue("setblock " + (x + 2) + " " + y + " " + z + " minecraft:oak_fence");
            enqueue("setblock " + x + " " + y + " " + (z + 1) + " minecraft:oak_fence");
            enqueue("setblock " + (x + 1) + " " + y + " " + (z + 1) + " minecraft:oak_fence");
            enqueue("setblock " + (x - 1) + " " + y + " " + (z - 1) + " minecraft:oak_fence");
            enqueue("setblock " + (x + 2) + " " + y + " " + (z - 1) + " minecraft:oak_fence");
            enqueue("summon minecraft:villager " + villagerX + " " + y + " " + villagerZ + " {PersistenceRequired:1b}");
            enqueue("tp @p " + (x + 0.5) + " " + y + " " + (z - 1.6) + " facing " + (x + 1.0) + " " + (y + 1) + " " + z);
            enqueue("gamemode survival @p");
            enqueue("clear @p");
            enqueue("give @p minecraft:diamond_axe");
            enqueue("give @p minecraft:lectern 16");
            enqueue("tp @e[type=minecraft:villager,limit=1,sort=nearest] " + villagerX + " " + y + " " + villagerZ + " 90 0");
            platformQueued = true;
        }
        // 1.19.3 adds 20 to the spam counter per command and removes 1 per tick, and kicks at 200
        // when the player is not op. One command every 15 ticks stays under that limit.
        if (!COMMANDS.isEmpty()) {
            if (client.player == null) {
                fail("player left before the platform was built");
                return;
            }
            //? if <1.20 {
            /*if (commandGap > 0) {
                commandGap--;
                return;
            }
            *///?}
            command(client, COMMANDS.removeFirst());
            //? if <1.20 {
            /*commandGap = 15;
            *///?}
            return;
        }
        state = State.WAIT_LIBRARIAN;
        ticks = 0;
    }

    //? if <1.19.4 {
    /*private static void applyLegacyFlat(CreateWorldScreen create) {
        create.setGameMode(CreateWorldScreen.SelectedGameMode.SURVIVAL);
        create.commands = true;
        var access = create.worldGenSettingsComponent.registryHolder();
        //? if <1.19.3 {
        create.worldGenSettingsComponent.updateSettings(net.minecraft.client.gui.screens.worldselection.PresetEditor.flatWorldConfigurator(
                net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings.getDefault(
                        access.registryOrThrow(net.minecraft.core.Registry.BIOME_REGISTRY),
                        access.registryOrThrow(net.minecraft.core.Registry.STRUCTURE_SET_REGISTRY))));
        //?}
        //? if >=1.19.3 {
        create.worldGenSettingsComponent.updateSettings(net.minecraft.client.gui.screens.worldselection.PresetEditor.flatWorldConfigurator(
                net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings.getDefault(
                        access.registryOrThrow(net.minecraft.core.registries.Registries.BIOME).asLookup(),
                        access.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET).asLookup(),
                        access.registryOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE).asLookup())));
        //?}
    }
    *///?}

    private static void enqueue(String command) {
        COMMANDS.add(command);
    }

    private static void waitLibrarian(Minecraft client) {
        if (ticks % 20 == 0) {
            command(client, "tp @e[type=minecraft:villager,limit=1,sort=nearest] " + villagerX + " " + y + " " + villagerZ + " 90 0");
        }
        Villager villager = nearestLibrarian(client);
        if (villager == null) {
            if (ticks > timeout(20 * 40)) {
                fail("librarian did not claim the lectern");
            }
            return;
        }
        LibrarianTracker.remember(villager);
        McBridge.interact(client.gameMode, client.player, villager, InteractionHand.MAIN_HAND);
        state = State.WAIT_MERCHANT;
        ticks = 0;
    }

    private static void waitMerchant(Minecraft client) {
        if (!(McBridge.currentScreen(client) instanceof MerchantScreen)) {
            // 26.3 drops the first interact if the villager is still walking onto the lectern.
            // Keep them in range and try again until the trade screen opens.
            Villager villager = nearestLibrarian(client);
            if (villager != null && ticks % 20 == 10 && villager.distanceToSqr(villagerX, y, villagerZ) > 4.0) {
                command(client, "tp @e[type=minecraft:villager,limit=1,sort=nearest] " + villagerX + " " + y + " " + villagerZ + " 90 0");
            }
            if (villager != null && ticks > 0 && ticks % 20 == 0) {
                McBridge.interact(client.gameMode, client.player, villager, InteractionHand.MAIN_HAND);
            }
            if (ticks > timeout(20 * 20)) {
                fail("merchant screen did not open");
            }
            return;
        }
        if (ticks < 10) {
            return;
        }
        RerollMethod.reset();
        cycling = TradeCyclingHook.installed();
        if (!cycling) {
            shot(client, "merchant-button");
        }
        if (!press(McBridge.currentScreen(client), "text.villager_reroller.button.offers")) {
            fail("offers button missing");
            return;
        }
        state = State.PANEL;
        ticks = 0;
    }

    private static void openPanel(Minecraft client) {
        if (ticks < 5) {
            return;
        }
        //? if <1.19.4 {
        /*if (false) {
        *///?}
        //? if >=1.19.4 && <1.21.11 {
        /*if (ScrollingNames.PERIOD_PER_PIXEL != AbstractWidget.PERIOD_PER_SCROLLED_PIXEL
                || ScrollingNames.MIN_PERIOD_SECONDS != AbstractWidget.MIN_SCROLL_PERIOD) {
        *///?}
        //? if >=1.21.11 {
        if (ScrollingNames.PERIOD_PER_PIXEL != ActiveTextCollector.PERIOD_PER_SCROLLED_PIXEL
                || ScrollingNames.MIN_PERIOD_SECONDS != ActiveTextCollector.MIN_SCROLL_PERIOD) {
        //?}
            fail("name scroll period does not match vanilla");
            return;
        }
        boolean ranged = TradeSession.entries().stream().anyMatch(entry -> entry.paysEmeralds && entry.emeraldMin != entry.emeraldMax);
        boolean fixed = TradeSession.entries().stream().anyMatch(entry -> entry.emeraldMin == entry.emeraldMax && entry.emeraldMin > 0);
        if (!ranged || !fixed) {
            fail("panel catalog missing a fixed price or a min/max range: " + TradeSession.entries().size());
            return;
        }
        note("villager_reroller harness check trade-panel");
        state = State.WAIT_SCROLL;
        ticks = 0;
    }

    private static void waitScroll(Minecraft client) {
        if (!oddsShot) {
            if (ticks == 1) {
                if (!selectOddsSample()) {
                    fail("no enchanted book to estimate");
                }
                return;
            }
            if (ticks < 3) {
                return;
            }
            String[] lines = TradePickerUi.oddsLines();
            if (lines == null || lines[0] == null || !lines[0].contains("1 in")) {
                fail("odds line missing: " + java.util.Arrays.toString(lines));
                return;
            }
            try {
                Path dir = artifacts();
                Files.createDirectories(dir);
                String time = lines.length > 1 && lines[1] != null ? lines[1] : "";
                Files.writeString(dir.resolve("panel-odds.txt"), lines[0] + "\n" + time + "\n");
            } catch (IOException e) {
                fail("odds text was not written: " + e.getMessage());
                return;
            }
            shot(client, "panel-odds");
            oddsShot = true;
            return;
        }
        if (!oddsCleared) {
            TradeSession.clearSelection();
            oddsCleared = true;
        }
        ScrollSpot spot = scrollSpot(client);
        if (!cycling && spot == ScrollSpot.MID && !midShot) {
            midShot = true;
            shot(client, "name-scroll-mid", "name-scroll", "trade-panel");
        }
        if (spot != ScrollSpot.MAX) {
            if (ticks > timeout(20 * 40)) {
                fail("name scroll did not reach the far end");
            }
            return;
        }
        if (!cycling) {
            if (!maxShot) {
                maxShot = true;
                shot(client, "name-scroll-max", "panel-job-site");
            }
            assertNameClearsThePrice(client);
            selectAndRemember(client);
            state = State.START_REROLL;
            ticks = 0;
            return;
        }
        if (!jobSiteShot) {
            jobSiteShot = true;
            shot(client, "panel-job-site");
            if (!press(McBridge.currentScreen(client), "text.villager_reroller.method.job_site")) {
                fail("method button missing");
                return;
            }
            return;
        }
        if (!RerollMethod.usesTradeCycling()) {
            if (ticks > timeout(20 * 10)) {
                fail("method toggle did not select Trade Cycling");
            }
            return;
        }
        if (!cyclingPanelShot) {
            cyclingPanelShot = true;
            shot(client, "panel-trade-cycling");
        }
        assertNameClearsThePrice(client);
        selectAndRemember(client);
        state = State.START_REROLL;
        ticks = 0;
    }

    private static boolean selectOddsSample() {
        TradeSession.clearSelection();
        for (TradeEntry entry : TradeSession.entries()) {
            if ("enchant:minecraft:mending:1".equals(entry.id)) {
                TradeSession.toggle(entry);
                TradePickerUi.reveal(entry.id);
                return true;
            }
        }
        for (TradeEntry entry : TradeSession.entries()) {
            if (entry.id.startsWith("enchant:")) {
                TradeSession.toggle(entry);
                TradePickerUi.reveal(entry.id);
                return true;
            }
        }
        return false;
    }

    private static void selectAndRemember(Minecraft client) {
        if (!(McBridge.currentScreen(client) instanceof MerchantScreen screen)) {
            fail("offers button did not leave the merchant screen open");
            return;
        }
        int selected = TradeSession.selectAllExcept(screen.getMenu().getOffers());
        if (selected <= 0) {
            fail("every catalog row was already on the counter");
            return;
        }
        if (!client.level.getBlockState(new BlockPos(x, y, z)).is(Blocks.LECTERN)) {
            fail("lectern missing before the reroll");
            return;
        }
        lecternsBefore = lecternCount(client);
    }

    private static void startReroll(Minecraft client) {
        if (!press(McBridge.currentScreen(client), "text.villager_reroller.button.start")) {
            fail("start button missing");
            return;
        }
        sawAir = false;
        stopReason = "running";
        state = State.WAIT_FOUND;
        ticks = 0;
    }

    private static void waitFound(Minecraft client) {
        // Teleporting the villager the player is trading with closes MerchantScreen.
        // 1.21.4 then sits in this state until the deadline, which looked like a reroll timeout.
        boolean running = JobSiteReroller.isRunning() || JobSiteReroller.stopReason() == null;
        if (running && !(McBridge.currentScreen(client) instanceof MerchantScreen) && ticks % 10 == 0) {
            Villager villager = nearestLibrarian(client);
            boolean outOfReach = villager != null && client.player != null && !McBridge.withinEntity(client.player, villager);
            boolean wandered = villager != null && villager.distanceToSqr(villagerX, y, villagerZ) > 4.0;
            if (outOfReach || wandered) {
                command(client, "tp @e[type=minecraft:villager,limit=1,sort=nearest] " + villagerX + " " + y + " " + villagerZ + " 90 0");
            }
        }
        if (client.level.getBlockState(new BlockPos(x, y, z)).isAir()) {
            sawAir = true;
        }
        if (running) {
            if (ticks > timeout(20 * 300)) {
                fail("reroll still running after the deadline");
            }
            return;
        }
        stopReason = JobSiteReroller.stopReason().name();
        if (!"FOUND".equals(stopReason)) {
            // The villager can finish claiming the lectern after the trade screen opens. Pull them
            // back onto it and press Start again instead of failing the first lookup.
            if ("NO_JOB_SITE".equals(stopReason) && ticks < 20 * 20) {
                if (ticks % 20 == 0) {
                    command(client, "tp @e[type=minecraft:villager,limit=1,sort=nearest] " + villagerX + " " + y + " " + villagerZ + " 90 0");
                }
                if (ticks % 20 == 10) {
                    press(McBridge.currentScreen(client), "text.villager_reroller.button.start");
                }
                return;
            }
            fail("reroll stopped with " + stopReason + " after " + JobSiteReroller.attempts() + " attempts");
            return;
        }
        state = State.DONE;
        ticks = 0;
    }

    private static void finish(Minecraft client) {
        if (!(McBridge.currentScreen(client) instanceof MerchantScreen)) {
            if (ticks > timeout(20 * 20)) {
                fail("merchant screen did not reopen after a found trade");
            }
            return;
        }
        if (ticks < 5) {
            return;
        }
        String shotName = cycling ? "reroll-cycling" : "reroll-found";
        if (!rerollShot) {
            rerollShot = true;
            shot(client, shotName);
        }
        boolean stillLectern = client.level.getBlockState(new BlockPos(x, y, z)).is(Blocks.LECTERN);
        int lecternsAfter = lecternCount(client);
        int attempts = JobSiteReroller.attempts();
        if (cycling) {
            if (sawAir || !stillLectern || lecternsAfter != lecternsBefore) {
                fail("Trade Cycling path broke the lectern: air=" + sawAir + " still=" + stillLectern + " items " + lecternsBefore + "->" + lecternsAfter);
                return;
            }
            if (attempts < 1) {
                fail("Trade Cycling path matched without sending a cycle");
                return;
            }
        } else if (!sawAir) {
            fail("job-block path never broke the lectern");
            return;
        }
        if (!Files.exists(artifacts().resolve(shotName + ".png"))) {
            if (ticks > timeout(20 * 20)) {
                fail("reroll screenshot was not written");
            }
            return;
        }
        note("villager_reroller harness check reroll-matched");
        LOGGER.info("1.21.1 client harness passed (cycling={})", cycling);
        System.out.println("1.21.1 client harness passed (cycling=" + cycling + ")");
        System.out.flush();
        writeResult("PASS\ncycling=" + cycling + "\nmod-loaded\nconfig-screen\ntrade-panel\nreroll-matched\n");
        client.stop();
    }

    private static void command(Minecraft client, String command) {
        if (client.player == null) {
            fail("player left before command: " + command);
            return;
        }
        //? if <1.19.1 {
        /*client.player.command(command);
        *///?}
        //? if >=1.19.1 && <1.19.3 {
        /*client.player.commandSigned(command, null);
        *///?}
        //? if >=1.19.3 {
        client.player.connection.sendCommand(command);
        //?}
    }

    private static Villager nearestLibrarian(Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }
        AABB box = client.player.getBoundingBox().inflate(24.0);
        Villager nearest = null;
        double best = Double.MAX_VALUE;
        for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
            if (!McBridge.isLibrarian(villager)) {
                continue;
            }
            double distance = villager.distanceToSqr(client.player);
            if (distance < best) {
                best = distance;
                nearest = villager;
            }
        }
        return nearest;
    }

    private static boolean press(Screen screen, String key) {
        if (screen == null) {
            return false;
        }
        String wanted = Component.translatable(key).getString();
        for (AbstractWidget widget : McBridge.buttons(screen)) {
            if (widget.getMessage().getString().equals(wanted) && widget instanceof Button button) {
                pressButton(button);
                return true;
            }
        }
        return false;
    }

    private enum ScrollSpot {
        NONE,
        MID,
        MAX
    }

    private static ScrollSpot scrollSpot(Minecraft client) {
        TradeEntry bane = bane(client);
        if (bane == null || client.font == null) {
            return ScrollSpot.NONE;
        }
        int available = TradePickerUi.nameBudget(client.font.width(bane.priceLabel()));
        int textWidth = client.font.width(bane.name);
        int overflow = textWidth - available;
        if (overflow < 8) {
            return ScrollSpot.NONE;
        }
        int offset = ScrollingNames.offset(textWidth, available, Util.getMillis());
        if (offset >= overflow - 1) {
            return ScrollSpot.MAX;
        }
        if (offset >= overflow * 2 / 5 && offset <= overflow * 3 / 5) {
            return ScrollSpot.MID;
        }
        return ScrollSpot.NONE;
    }

    private static void assertNameClearsThePrice(Minecraft client) {
        TradeEntry bane = bane(client);
        if (bane == null) {
            fail("Bane of Arthropods was not in the catalog");
            return;
        }
        int priceWidth = client.font.width(bane.priceLabel());
        int textWidth = client.font.width(bane.name);
        int available = TradePickerUi.nameBudget(priceWidth);
        int nameLeft = TradeNameLayout.NAME_LEFT;
        int nameRight = nameLeft + available;
        int priceX = nameRight + TradeNameLayout.PRICE_GAP;
        int overflow = textWidth - available;
        if (overflow < 8) {
            fail("Bane of Arthropods does not overflow the name box");
            return;
        }
        for (int offset = 0; offset <= overflow; offset++) {
            int visibleRight = TradeNameLayout.clippedRight(nameLeft, nameRight, textWidth, offset);
            if (visibleRight > priceX - TradeNameLayout.PRICE_GAP) {
                fail("name reaches the price at offset " + offset);
                return;
            }
        }
    }

    private static TradeEntry bane(Minecraft client) {
        for (TradeEntry entry : TradeSession.entries()) {
            if (entry.name.startsWith("Bane of Arthropods")) {
                return entry;
            }
        }
        return null;
    }

    private static int lecternCount(Minecraft client) {
        int count = 0;
        var inventory = client.player.getInventory();
        for (int slot = 0; slot < McBridge.storageSize(inventory); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.LECTERN)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void shot(Minecraft client, String name, String... aliases) {
        try {
            Path dir = artifacts();
            Files.createDirectories(dir);
            //? if <1.21.5 {
            /*NativeImage image = Screenshot.takeScreenshot(mainTarget(client));
            try {
                writeShot(dir, name, image, aliases);
            } finally {
                image.close();
            }
            *///?} else {
            // The GPU read finishes after this returns, so aliases are copied in the callback.
            Screenshot.takeScreenshot(mainTarget(client), image -> {
                try {
                    writeShot(dir, name, image, aliases);
                } catch (IOException e) {
                    fail("screenshot " + name + " failed: " + e.getMessage());
                } finally {
                    image.close();
                }
            });
            //?}
        } catch (IOException e) {
            fail("screenshot " + name + " failed: " + e.getMessage());
        }
    }

    private static void writeShot(Path dir, String name, NativeImage image, String[] aliases) throws IOException {
        image.writeToFile(dir.resolve(name + ".png"));
        for (String alias : aliases) {
            Files.copy(dir.resolve(name + ".png"), dir.resolve(alias + ".png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path artifacts() {
        String configured = System.getProperty("villager_reroller.artifacts");
        if (configured == null || configured.isBlank()) {
            return Path.of("/opt/cursor/artifacts/1.21.1/plain");
        }
        return Path.of(configured);
    }

    private static com.mojang.blaze3d.pipeline.RenderTarget mainTarget(Minecraft client) {
        //? if >=26.2 {
        /*return client.gameRenderer.mainRenderTarget();
        *///?} else {
        return client.getMainRenderTarget();
        //?}
    }

    private static void pressButton(Button button) {
        //? if <1.21.9 {
        /*button.onPress();
        *///?} else {
        button.onPress(new InputWithModifiers() {
            @Override
            public int input() {
                return 0;
            }

            @Override
            public int modifiers() {
                return 0;
            }
        });
        //?}
    }

    private static int timeout(int ticks) {
        return Math.max(1, ticks) * Math.max(1, SCALE);
    }

    private static void note(String message) {
        LOGGER.info(message);
        System.out.println(message);
        System.out.flush();
    }

    private static void writeResult(String body) {
        try {
            Path file = artifacts().resolve("harness-result.txt");
            Files.createDirectories(file.getParent());
            Files.writeString(file, body);
        } catch (IOException e) {
            LOGGER.error("could not write harness result: {}", e.getMessage());
        }
    }

    private static void fail(String message) {
        LOGGER.error("1.21.1 client harness failed: {}", message);
        System.err.println("1.21.1 client harness failed: " + message);
        System.err.flush();
        writeResult("FAIL\n" + message + "\n");
        System.exit(1);
    }

    private enum State {
        WAIT_TITLE,
        OPEN_CONFIG,
        WAIT_CONFIG,
        WAIT_PRESETS,
        WAIT_PLAYER,
        WAIT_LIBRARIAN,
        WAIT_MERCHANT,
        PANEL,
        WAIT_SCROLL,
        START_REROLL,
        WAIT_FOUND,
        DONE
    }
}
