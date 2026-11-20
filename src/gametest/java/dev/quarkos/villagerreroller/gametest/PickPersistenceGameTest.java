package dev.quarkos.villagerreroller.gametest;

import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.harness.BrowserHarness;
import dev.quarkos.villagerreroller.config.ForgeConfigScreen;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.picker.TradeEntry;
import dev.quarkos.villagerreroller.picker.TradeSession;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Picks a librarian price, switches the villager to a farmer, then drops the
 * in-memory session the way a new client process would. Reopening the librarian
 * browser has to restore the price from config/villager_reroller-picks.json.
 */
@SuppressWarnings("UnstableApiUsage")
public class PickPersistenceGameTest implements FabricClientGameTest {

    private String fixedId = "";
    private int fixedPrice;
    private String rangedId = "";
    private int rangedPrice;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.pauseOnLostFocus = false);
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(client -> {
            client.options.guiScale().set(2);
            client.options.renderDistance().set(2);
            client.options.simulationDistance().set(5);
            client.options.framerateLimit().set(20);
            try {
                Files.deleteIfExists(PickFiles.configFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            TradeSession.simulateClientRestart();
        });

        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(settings -> {
                    settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
                    settings.getNormalPresetList().stream()
                            .filter(entry -> entry.preset().is(WorldPresets.FLAT))
                            .findFirst()
                            .ifPresent(settings::setWorldType);
                })
                .create()) {
            GameTestSupport.waitForChunks(world);
            BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
            int x = feet.getX() + 6;
            int y = feet.getY();
            int z = feet.getZ();
            //? if >=26.2 {
            /*world.getServer().runCommand("gamerule send_command_feedback false");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("gamerule spawn_mobs false");
            world.getServer().runCommand("gamerule immediate_respawn true");
            *///?} else {
            world.getServer().runCommand("gamerule sendCommandFeedback false");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            world.getServer().runCommand("gamerule doMobSpawning false");
            world.getServer().runCommand("gamerule doImmediateRespawn true");
            //?}
            world.getServer().runCommand("time set day");
            world.getServer().runCommand("fill " + (x - 2) + " " + (y - 1) + " " + (z - 2) + " " + (x + 3) + " " + (y - 1) + " " + (z + 2) + " minecraft:stone");
            world.getServer().runCommand("fill " + (x - 2) + " " + y + " " + (z - 2) + " " + (x + 3) + " " + (y + 3) + " " + (z + 2) + " minecraft:air");
            world.getServer().runCommand("setblock " + x + " " + y + " " + z + " minecraft:lectern");
            world.getServer().runCommand("setblock " + (x - 1) + " " + y + " " + z + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + (x + 2) + " " + y + " " + z + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + x + " " + y + " " + (z + 1) + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + (x + 1) + " " + y + " " + (z + 1) + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + (x - 1) + " " + y + " " + (z - 1) + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + (x + 2) + " " + y + " " + (z - 1) + " minecraft:oak_fence");
            double villagerX = x + 1.15;
            double villagerZ = z + 0.5;
            world.getServer().runCommand("summon minecraft:villager " + villagerX + " " + y + " " + villagerZ + " {PersistenceRequired:1b}");
            world.getServer().runCommand("tp @p " + (x + 0.5) + " " + y + " " + (z - 1.6) + " facing " + (x + 1.0) + " " + (y + 1) + " " + z);
            context.waitTicks(5);
            world.getServer().runCommand("gamemode survival @p");
            GameTestSupport.waitForChunks(world);

            waitForProfession(context, "minecraft:librarian");
            openTrades(context);
            context.clickScreenButton("text.villager_reroller.browser.title");
            context.runOnClient(this::chooseTargets);
            GameTestSupport.saveShot(context, "picks-before-restart");
            context.runOnClient(client -> assertRemembered(client, "minecraft:librarian"));

            closeTrades(context);
            world.getServer().runCommand("setblock " + x + " " + y + " " + z + " minecraft:air");
            context.waitTicks(40);
            world.getServer().runCommand("setblock " + x + " " + y + " " + z + " minecraft:composter");
            waitForProfession(context, "minecraft:farmer");
            context.waitTicks(20);
            openTrades(context);
            context.waitTicks(5);
            context.clickScreenButton("text.villager_reroller.browser.title");
            GameTestSupport.saveShot(context, "picks-other-profession");
            context.runOnClient(client -> {
                if (!"minecraft:farmer".equals(TradeSession.profession())) {
                    throw new AssertionError("browser did not switch to the farmer, profession=" + TradeSession.profession());
                }
                if (TradeSession.selected(rangedId) || TradeSession.selected(fixedId)) {
                    throw new AssertionError("librarian picks stayed selected after the profession switch: " + TradeSession.selections());
                }
                Map<String, Integer> saved = ProfessionPicks.load(PickFiles.configFile()).picks("minecraft:librarian");
                if (!saved.getOrDefault(rangedId, 0).equals(rangedPrice) || !saved.getOrDefault(fixedId, 0).equals(fixedPrice)) {
                    throw new AssertionError("profession switch dropped the librarian file: " + saved);
                }
            });

            closeTrades(context);
            context.runOnClient(client -> {
                TradeSession.simulateClientRestart();
                if (TradeSession.hasSelection() || !TradeSession.profession().isEmpty()) {
                    throw new AssertionError("client restart left picks in memory");
                }
                Map<String, Integer> saved = ProfessionPicks.load(PickFiles.configFile()).picks("minecraft:librarian");
                if (!saved.getOrDefault(rangedId, 0).equals(rangedPrice) || !saved.getOrDefault(fixedId, 0).equals(fixedPrice)) {
                    throw new AssertionError("restart lost the picks file: " + saved);
                }
            });
            world.getServer().runCommand("setblock " + x + " " + y + " " + z + " minecraft:air");
            context.waitTicks(40);
            world.getServer().runCommand("setblock " + x + " " + y + " " + z + " minecraft:lectern");
            waitForProfession(context, "minecraft:librarian");
            context.waitTicks(20);
            openTrades(context);
            context.clickScreenButton("text.villager_reroller.browser.title");
            context.waitTicks(5);
            GameTestSupport.saveShot(context, "picks-after-restart");
            context.runOnClient(client -> assertRemembered(client, "minecraft:librarian"));
            context.runOnClient(client -> McBridge.playMatchFound(client, 1.0F));

            context.runOnClient(client -> McBridge.setScreen(client, new ForgeConfigScreen(McBridge.currentScreen(client))));
            context.waitTicks(20);
            GameTestSupport.saveShot(context, "match-sound-settings");
            String title = context.computeOnClient(client -> {
                var screen = McBridge.currentScreen(client);
                return screen == null ? "" : screen.getTitle().getString();
            });
            if (!title.toLowerCase().contains("villager")) {
                throw new AssertionError("match sound settings were not open, title=" + title);
            }
            context.runOnClient(client -> TradeSession.simulateClientRestart());
        }
    }

    private void chooseTargets(net.minecraft.client.Minecraft client) {
        List<TradeEntry> entries = TradeSession.entries();
        TradeEntry ranged = null;
        TradeEntry fixed = null;
        for (TradeEntry entry : entries) {
            if (!entry.paysEmeralds) {
                continue;
            }
            if (ranged == null && entry.emeraldMin != entry.emeraldMax) {
                ranged = entry;
            }
            if (fixed == null && entry.emeraldMin == entry.emeraldMax && entry.emeraldMin > 0 && entry != ranged) {
                fixed = entry;
            }
        }
        if (ranged == null || fixed == null) {
            throw new AssertionError("librarian catalog had no fixed price and no range: " + entries.size());
        }
        fixedId = fixed.id;
        fixedPrice = fixed.emeraldMax;
        rangedId = ranged.id;
        rangedPrice = ranged.emeraldMin + 1;
        BrowserHarness.pick(client, fixed, fixedPrice);
        BrowserHarness.pick(client, ranged, rangedPrice);
    }

    private void assertRemembered(net.minecraft.client.Minecraft client, String profession) {
        if (!profession.equals(TradeSession.profession())) {
            throw new AssertionError("profession " + TradeSession.profession() + " wanted " + profession);
        }
        if (!TradeSession.selected(fixedId) || TradeSession.chosenMax(fixedId) != fixedPrice) {
            throw new AssertionError("fixed pick " + fixedId + " at " + fixedPrice + " restored as " + TradeSession.selections());
        }
        if (!TradeSession.selected(rangedId) || TradeSession.chosenMax(rangedId) != rangedPrice) {
            throw new AssertionError("ranged pick " + rangedId + " at " + rangedPrice + " restored as " + TradeSession.selections());
        }
        Map<String, Integer> saved = ProfessionPicks.load(PickFiles.configFile()).picks(profession);
        if (!saved.getOrDefault(fixedId, 0).equals(fixedPrice) || !saved.getOrDefault(rangedId, 0).equals(rangedPrice)) {
            throw new AssertionError("file " + PickFiles.configFile() + " has " + saved);
        }
    }

    private void waitForProfession(ClientGameTestContext context, String profession) {
        context.waitFor(client -> {
            Villager villager = nearestVillager(client);
            return villager != null && profession.equals(McBridge.professionId(villager));
        }, 20 * 40);
    }

    private void openTrades(ClientGameTestContext context) {
        context.runOnClient(client -> {
            Villager villager = nearestVillager(client);
            if (villager == null || client.player == null || client.gameMode == null) {
                throw new AssertionError("villager disappeared before the trade screen opened");
            }
            LibrarianTracker.remember(villager);
            McBridge.interact(client.gameMode, client.player, villager, InteractionHand.MAIN_HAND);
        });
        context.waitForScreen(MerchantScreen.class);
        context.waitTicks(10);
    }

    private void closeTrades(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (client.player != null) {
                client.player.closeContainer();
            }
        });
        context.waitFor(client -> McBridge.currentScreen(client) == null, GameTestSupport.scaled(20 * 10));
    }

    private static Villager nearestVillager(net.minecraft.client.Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }
        AABB box = client.player.getBoundingBox().inflate(24.0);
        Villager nearest = null;
        double best = Double.MAX_VALUE;
        for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
            double distance = villager.distanceToSqr(client.player);
            if (distance < best) {
                best = distance;
                nearest = villager;
            }
        }
        return nearest;
    }
}
