package dev.quarkos.villagerreroller.gametest;

import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.harness.BrowserHarness;
import dev.quarkos.villagerreroller.compat.TradeCyclingHook;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.RerollMethod;
import dev.quarkos.villagerreroller.picker.ToolGuard;
import dev.quarkos.villagerreroller.picker.TradeEntry;
import dev.quarkos.villagerreroller.picker.TradeSession;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Spawns a novice librarian, opens the trade screen, and rerolls the lectern
 * until a trade that was not on the counter appears.
 */
@SuppressWarnings("UnstableApiUsage")
public class TradePickerClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.pauseOnLostFocus = false);
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(client -> {
            client.options.guiScale().set(2);
            client.options.renderDistance().set(2);
            client.options.simulationDistance().set(5);
            client.options.framerateLimit().set(20);
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
            assertServerChannel(context);
            BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
            int x = feet.getX() + 6;
            int y = feet.getY();
            int z = feet.getZ();

            //? if >=1.21.11 {
            world.getServer().runCommand("gamerule send_command_feedback false");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("gamerule spawn_mobs false");
            world.getServer().runCommand("gamerule immediate_respawn true");
            //?} else {
            /*world.getServer().runCommand("gamerule sendCommandFeedback false");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            world.getServer().runCommand("gamerule doMobSpawning false");
            world.getServer().runCommand("gamerule doImmediateRespawn true");
            *///?}
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
            world.getServer().runCommand("clear @p");
            assertDurabilityGuard(context, world);
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("give @p minecraft:diamond_axe");
            world.getServer().runCommand("give @p minecraft:lectern 16");
            GameTestSupport.waitForChunks(world);

            context.waitFor(client -> {
                if (client.level == null || client.player == null) {
                    return false;
                }
                AABB box = client.player.getBoundingBox().inflate(24.0);
                for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
                    if (villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
                        return true;
                    }
                }
                return false;
            }, 20 * 40);

            context.runOnClient(client -> {
                Villager villager = nearestLibrarian(client);
                if (villager == null) {
                    throw new AssertionError("librarian disappeared before the trade screen opened");
                }
                dev.quarkos.villagerreroller.client.LibrarianTracker.remember(villager);
                McBridge.interact(client.gameMode, client.player, villager, net.minecraft.world.InteractionHand.MAIN_HAND);
            });
            context.waitForScreen(MerchantScreen.class);
            context.waitTicks(10);
            context.runOnClient(client -> RerollMethod.reset());
            boolean cycling = context.computeOnClient(client -> TradeCyclingHook.serverSupports());
            if (!cycling) {
                GameTestSupport.saveShot(context, "merchant-button");
            }
            MerchantOffers initial = context.computeOnClient(client ->
                    ((MerchantScreen) McBridge.currentScreen(client)).getMenu().getOffers());
            context.clickScreenButton("text.villager_reroller.browser.title");
            context.runOnClient(client -> {
                BrowserHarness.browser(client);
                boolean ranged = TradeSession.entries().stream().anyMatch(entry -> entry.paysEmeralds && entry.emeraldMin != entry.emeraldMax);
                boolean fixed = TradeSession.entries().stream().anyMatch(entry -> entry.emeraldMin == entry.emeraldMax && entry.emeraldMin > 0);
                if (!ranged || !fixed) {
                    throw new AssertionError("browser catalog missing a fixed price or a min/max range: " + TradeSession.entries().size());
                }
                BrowserHarness.clearPicks(client);
                TradeEntry sample = TradeSession.entries().stream().filter(entry -> entry.id.startsWith("enchant:"))
                        .findFirst().orElseThrow();
                BrowserHarness.pick(client, sample, sample.emeraldMax);
                var lines = BrowserHarness.browser(client).oddsLines();
                if (lines.size() != 2 || !lines.get(0).getString().contains("1 in")) {
                    throw new AssertionError("browser odds or ETA missing: " + lines);
                }
                try {
                    Path dir = GameTestSupport.artifactsDir();
                    Files.createDirectories(dir);
                    Files.writeString(dir.resolve("panel-odds.txt"), lines.get(0).getString() + "\n" + lines.get(1).getString() + "\n");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            GameTestSupport.saveShot(context, "panel-odds");
            GameTestSupport.saveShot(context, "panel-job-site");
            if (cycling) {
                context.clickScreenButton("text.villager_reroller.method.job_site");
                if (!context.computeOnClient(client -> RerollMethod.usesTradeCycling())) {
                    throw new AssertionError("method toggle did not select Trade Cycling");
                }
                GameTestSupport.saveShot(context, "panel-trade-cycling");
            }
            context.runOnClient(client -> BrowserHarness.pickMissing(client, initial));

            BlockPos lecternPos = new BlockPos(x, y, z);
            boolean lecternThere = context.computeOnClient(client -> client.level.getBlockState(lecternPos).is(Blocks.LECTERN));
            if (!lecternThere) {
                throw new AssertionError("lectern missing before the reroll");
            }
            int lecternsBefore = context.computeOnClient(TradePickerClientGameTest::lecternCount);
            context.clickScreenButton("text.villager_reroller.browser.start");
            String reason = "running";
            boolean sawAir = false;
            for (int tick = 0; tick < 20 * 300; tick++) {
                context.waitTick();
                // A villager teleport closes an open merchant screen. Only pull a villager
                // who has walked off the lectern, and never while that screen is open.
                boolean running = context.computeOnClient(client -> JobSiteReroller.isRunning());
                boolean trading = context.computeOnClient(client -> McBridge.currentScreen(client) instanceof MerchantScreen);
                if (running && !trading && tick % 10 == 0) {
                    boolean pullBack = context.computeOnClient(client -> {
                        Villager villager = nearestLibrarian(client);
                        if (villager == null || client.player == null) {
                            return false;
                        }
                        return !McBridge.withinEntity(client.player, villager)
                                || villager.distanceToSqr(villagerX, y, villagerZ) > 4.0;
                    });
                    if (pullBack) {
                        world.getServer().runCommand("tp @e[type=minecraft:villager,limit=1,sort=nearest] " + villagerX + " " + y + " " + villagerZ + " 90 0");
                    }
                }
                boolean air = context.computeOnClient(client -> client.level.getBlockState(lecternPos).isAir());
                if (air) {
                    sawAir = true;
                }
                JobSiteReroller.StopReason stop = context.computeOnClient(client -> JobSiteReroller.stopReason());
                if (!running && stop != null) {
                    reason = stop.name();
                    break;
                }
            }
            if (!"FOUND".equals(reason)) {
                int attempts = context.computeOnClient(client -> JobSiteReroller.attempts());
                throw new AssertionError("reroll stopped with " + reason + " after " + attempts + " attempts");
            }
            context.waitForScreen(MerchantScreen.class);
            context.waitTicks(5);
            GameTestSupport.saveShot(context, cycling ? "reroll-cycling" : "reroll-found");
            int attempts = context.computeOnClient(client -> JobSiteReroller.attempts());
            boolean stillLectern = context.computeOnClient(client -> client.level.getBlockState(lecternPos).is(Blocks.LECTERN));
            int lecternsAfter = context.computeOnClient(TradePickerClientGameTest::lecternCount);
            if (cycling) {
                if (sawAir || !stillLectern || lecternsAfter != lecternsBefore) {
                    throw new AssertionError("Trade Cycling path broke the lectern: air=" + sawAir + " still=" + stillLectern + " items " + lecternsBefore + "->" + lecternsAfter);
                }
                if (attempts < 1) {
                    throw new AssertionError("Trade Cycling path matched without sending a cycle");
                }
            } else if (!sawAir) {
                throw new AssertionError("job-block path never broke the lectern");
            }
        }
    }

    private static int lecternCount(net.minecraft.client.Minecraft client) {
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

    private static Villager nearestLibrarian(net.minecraft.client.Minecraft client) {
        AABB box = client.player.getBoundingBox().inflate(24.0);
        Villager nearest = null;
        double best = Double.MAX_VALUE;
        for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
            if (!villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
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

    /**
     * The integrated server advertises the cycle channel only when Trade Cycling
     * is actually loaded there. A client-only install must not look choosable.
     */
    private static void assertServerChannel(ClientGameTestContext context) {
        boolean installed = context.computeOnClient(client -> TradeCyclingHook.installed());
        if (installed) {
            context.waitFor(client -> TradeCyclingHook.serverSupports(), 20 * 10);
        }
        context.runOnClient(client -> {
            boolean loaded = TradeCyclingHook.installed();
            boolean server = TradeCyclingHook.serverSupports();
            if (loaded != server) {
                throw new AssertionError("integrated server channel " + server + " but client mod loaded=" + loaded);
            }
            RerollMethod.reset();
            if (!server && RerollMethod.canChoose()) {
                throw new AssertionError("Trade Cycling was choosable without the server channel");
            }
            if (!server) {
                RerollMethod.toggle();
                if (RerollMethod.usesTradeCycling()) {
                    throw new AssertionError("toggle selected Trade Cycling without the server channel");
                }
            }
        });
    }

    /**
     * A diamond axe with 6 uses left is under the floor of 10. The slower wooden
     * axe wins, and the worn axe alone falls back to the bare hand.
     */
    private static void assertDurabilityGuard(ClientGameTestContext context, TestSingleplayerContext world) {
        context.runOnClient(client -> {
            AutomatorConfig config = AutomatorConfig.get();
            config.setDurabilityFloor(10);
            config.setAllowBareHand(true);
        });
        world.getServer().runCommand("clear @p");
        world.getServer().runCommand("give @p minecraft:diamond_axe[damage=1555]");
        world.getServer().runCommand("give @p minecraft:wooden_axe");
        context.waitTicks(20);
        context.runOnClient(client -> {
            BlockState lectern = Blocks.LECTERN.defaultBlockState();
            ToolGuard.Choice choice = JobSiteReroller.chooseTool(client.player, lectern);
            ItemStack chosen = choice.decision() == ToolGuard.Decision.TOOL
                    ? client.player.getInventory().getItem(choice.slot())
                    : ItemStack.EMPTY;
            if (choice.decision() != ToolGuard.Decision.TOOL || !chosen.is(Items.WOODEN_AXE)) {
                throw new AssertionError("worn diamond axe should lose to the wooden axe, got " + choice.decision() + " " + chosen);
            }
        });
        world.getServer().runCommand("clear @p");
        world.getServer().runCommand("give @p minecraft:diamond_axe[damage=1555]");
        context.waitTicks(20);
        context.runOnClient(client -> {
            ToolGuard.Choice choice = JobSiteReroller.chooseTool(client.player, Blocks.LECTERN.defaultBlockState());
            if (choice.decision() != ToolGuard.Decision.HAND) {
                throw new AssertionError("bare hand should be the fallback for a worn axe, got " + choice.decision());
            }
        });
    }


}
