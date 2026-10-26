package dev.quarkos.villagerreroller.gametest;

import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.RerollMethod;
import dev.quarkos.villagerreroller.picker.TradeEntry;
import dev.quarkos.villagerreroller.picker.TradeHall;
import dev.quarkos.villagerreroller.picker.TradeHallScreen;
import dev.quarkos.villagerreroller.picker.TradeSession;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Queues three novice librarians plus one locked villager and one whose job
 * site is out of reach, then rerolls the hall without moving the player.
 */
@SuppressWarnings("UnstableApiUsage")
public class TradeHallClientGameTest implements FabricClientGameTest {
    private static final Path ARTIFACTS = Path.of("/opt/cursor/artifacts");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.pauseOnLostFocus = false);
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(client -> {
            client.options.guiScale().set(2);
            client.options.renderDistance().set(8);
            client.options.simulationDistance().set(8);
            client.options.framerateLimit().set(20);
            if (JobSiteReroller.isRunning()) {
                JobSiteReroller.stop(JobSiteReroller.StopReason.STOPPED);
            }
            TradeHall.clear();
            RerollMethod.reset();
            LibrarianTracker.unpin();
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
            waitForChunks(world);
            BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
            int x = feet.getX() + 2;
            int y = feet.getY();
            int z = feet.getZ();
            int[] cellX = {x, x + 2, x + 4};
            double playerX = x + 2.5;
            double playerZ = z + 0.5;
            double lockedX = x + 2.5;
            double lockedZ = z - 1.5;
            int farX = x + 28;

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
            world.getServer().runCommand("fill " + (x - 2) + " " + (y - 1) + " " + (z - 5) + " " + (x + 8) + " " + (y - 1) + " " + (z + 5) + " minecraft:stone");
            world.getServer().runCommand("fill " + (x - 2) + " " + y + " " + (z - 5) + " " + (x + 8) + " " + (y + 3) + " " + (z + 5) + " minecraft:air");
            world.getServer().runCommand("fill " + (farX - 2) + " " + (y - 1) + " " + (z - 2) + " " + (farX + 2) + " " + (y - 1) + " " + (z + 2) + " minecraft:stone");
            world.getServer().runCommand("fill " + (farX - 2) + " " + y + " " + (z - 2) + " " + (farX + 2) + " " + (y + 3) + " " + (z + 2) + " minecraft:air");

            for (int cx : cellX) {
                pen(world, cx, y, z + 3, z + 2);
            }
            pen(world, x + 2, y, z - 3, z - 2);
            world.getServer().runCommand("setblock " + farX + " " + y + " " + z + " minecraft:lectern");
            world.getServer().runCommand("setblock " + (farX - 1) + " " + y + " " + z + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + (farX + 1) + " " + y + " " + z + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + farX + " " + y + " " + (z + 1) + " minecraft:oak_fence");

            String[] tags = {"hall_a", "hall_b", "hall_c"};
            for (int i = 0; i < cellX.length; i++) {
                summon(world, tags[i], cellX[i] + 0.5, y, z + 2.5, false);
            }
            summon(world, "hall_locked", x + 2.5, y, z - 1.5, true);
            summon(world, "hall_far", farX + 0.5, y, z + 0.5, false);

            world.getServer().runCommand("tp @p " + playerX + " " + y + " " + playerZ + " 180 0");
            context.waitTicks(5);
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("give @p minecraft:diamond_axe");
            world.getServer().runCommand("give @p minecraft:lectern 32");
            waitForChunks(world);

            context.waitFor(client -> countLibrarians(client) >= 4, 20 * 40);
            world.getServer().runCommand("data merge entity @e[type=minecraft:villager,tag=hall_locked,limit=1] {Xp:25}");
            context.waitFor(client -> {
                Villager locked = nearestTo(client, lockedX, y, lockedZ);
                return locked != null && (McBridge.villagerXp(locked) > 0 || McBridge.villagerLevel(locked) > 1);
            }, 20 * 20);

            queueVillager(context, cellX[0] + 0.5, y, z + 2.5, 0);
            context.waitTicks(10);
            saveShot(context, "hall-panel");
            queueVillager(context, lockedX, y, lockedZ, 1);
            world.getServer().runCommand("tp @p " + (farX + 0.5) + " " + y + " " + (z - 1.2) + " facing " + (farX + 0.5) + " " + y + " " + z);
            context.waitTicks(20);
            waitForChunks(world);
            context.waitFor(client -> nearestTo(client, farX + 0.5, y, z + 0.5) != null
                    && isLibrarian(nearestTo(client, farX + 0.5, y, z + 0.5)), 20 * 40);
            queueVillager(context, farX + 0.5, y, z + 0.5, 3);
            world.getServer().runCommand("tp @p " + playerX + " " + y + " " + playerZ + " 180 0");
            context.waitTicks(10);
            queueVillager(context, cellX[1] + 0.5, y, z + 2.5, 2);
            queueVillager(context, cellX[2] + 0.5, y, z + 2.5, 4);

            int queued = context.computeOnClient(client -> TradeHall.size());
            if (queued != 5) {
                throw new AssertionError("expected 5 queued villagers, got " + queued);
            }
            context.runOnClient(client -> {
                Villager current = nearestTo(client, cellX[2] + 0.5, y, z + 2.5);
                if (current == null) {
                    throw new AssertionError("last novice disappeared before the queue screen");
                }
                LibrarianTracker.remember(current);
                McBridge.interact(client.gameMode, client.player, current, InteractionHand.MAIN_HAND);
            });
            context.waitForScreen(MerchantScreen.class);
            context.waitTicks(8);
            context.clickScreenButton("text.villager_reroller.button.offers");
            context.waitTicks(5);
            context.clickScreenButton("text.villager_reroller.button.hall");
            context.waitForScreen(TradeHallScreen.class);
            world.getServer().runCommand("tp @p " + playerX + " " + y + " " + playerZ + " 180 0");
            context.waitTicks(15);
            saveShot(context, "hall-queue");

            context.runOnClient(client -> {
                if (TradeHall.slots().size() != 5) {
                    throw new AssertionError("queue screen lost villagers");
                }
                assertBlocker(client, cellX[0] + 0.5, y, z + 2.5, null);
                assertBlocker(client, cellX[1] + 0.5, y, z + 2.5, null);
                assertBlocker(client, cellX[2] + 0.5, y, z + 2.5, null);
                assertBlocker(client, lockedX, y, lockedZ, "locked");
                assertBlocker(client, farX + 0.5, y, z + 0.5, "out_of_reach");
                int ranged = 0;
                for (TradeHall.Slot slot : TradeHall.slots()) {
                    if (slot.selections().isEmpty()) {
                        throw new AssertionError("queued villager has no targets");
                    }
                    if (slot.description() == null || slot.description().isEmpty()) {
                        throw new AssertionError("queued villager has no target label");
                    }
                    for (int ceiling : slot.selections().values()) {
                        if (ceiling != TradeSession.ANY_PRICE) {
                            ranged++;
                        }
                    }
                }
                if (ranged < 3) {
                    throw new AssertionError("expected per-villager emerald ceilings, found " + ranged);
                }
            });

            Vec3 anchor = context.computeOnClient(client -> client.player.position());
            BlockPos lockedLectern = new BlockPos(x + 2, y, z - 3);
            BlockPos farLectern = new BlockPos(farX, y, z);
            BlockPos[] noviceLecterns = {
                    new BlockPos(cellX[0], y, z + 3),
                    new BlockPos(cellX[1], y, z + 3),
                    new BlockPos(cellX[2], y, z + 3)
            };
            boolean[] sawAir = {false, false, false};
            boolean progressShot = false;
            context.clickScreenButton("text.villager_reroller.button.hall_start");
            String phase = "running";
            for (int tick = 0; tick < 20 * 300 * 3; tick++) {
                context.waitTick();
                boolean trading = context.computeOnClient(client -> McBridge.currentScreen(client) instanceof MerchantScreen);
                if (tick % 10 == 0 && !trading) {
                    if (context.computeOnClient(client -> wandered(client, cellX[0] + 0.5, y, z + 2.5))) {
                        world.getServer().runCommand("tp @e[type=minecraft:villager,tag=hall_a,limit=1] " + (cellX[0] + 0.5) + " " + y + " " + (z + 2.5) + " 180 0");
                    }
                    if (context.computeOnClient(client -> wandered(client, cellX[1] + 0.5, y, z + 2.5))) {
                        world.getServer().runCommand("tp @e[type=minecraft:villager,tag=hall_b,limit=1] " + (cellX[1] + 0.5) + " " + y + " " + (z + 2.5) + " 180 0");
                    }
                    if (context.computeOnClient(client -> wandered(client, cellX[2] + 0.5, y, z + 2.5))) {
                        world.getServer().runCommand("tp @e[type=minecraft:villager,tag=hall_c,limit=1] " + (cellX[2] + 0.5) + " " + y + " " + (z + 2.5) + " 180 0");
                    }
                    if (context.computeOnClient(client -> wandered(client, lockedX, y, lockedZ))) {
                        world.getServer().runCommand("tp @e[type=minecraft:villager,tag=hall_locked,limit=1] " + lockedX + " " + y + " " + lockedZ + " 0 0");
                    }
                }
                for (int i = 0; i < noviceLecterns.length; i++) {
                    int index = i;
                    boolean air = context.computeOnClient(client -> client.level.getBlockState(noviceLecterns[index]).isAir());
                    if (air) {
                        sawAir[i] = true;
                    }
                }
                boolean running = context.computeOnClient(client -> JobSiteReroller.isRunning());
                boolean summary = context.computeOnClient(client -> TradeHall.showingSummary());
                if (!progressShot && running) {
                    context.waitTicks(8);
                    saveShot(context, "hall-progress");
                    progressShot = true;
                }
                if (summary) {
                    phase = "summary";
                    break;
                }
            }
            if (!"summary".equals(phase)) {
                String reason = context.computeOnClient(client -> {
                    StringBuilder builder = new StringBuilder();
                    for (TradeHall.Slot slot : TradeHall.slots()) {
                        builder.append(slot.outcome()).append(':').append(slot.detail()).append(' ');
                    }
                    return builder.toString();
                });
                throw new AssertionError("hall did not finish: " + reason);
            }
            context.waitForScreen(TradeHallScreen.class);
            context.waitTicks(10);
            saveShot(context, "hall-summary");

            context.runOnClient(client -> {
                if (client.player.position().distanceToSqr(anchor) > 2.25) {
                    throw new AssertionError("player moved during the hall from " + anchor + " to " + client.player.position());
                }
                if (!client.level.getBlockState(lockedLectern).is(Blocks.LECTERN)) {
                    throw new AssertionError("locked villager's lectern was broken");
                }
                if (!client.level.getBlockState(farLectern).is(Blocks.LECTERN)) {
                    throw new AssertionError("out-of-reach lectern was broken");
                }
                var slots = TradeHall.slots();
                if (slots.size() != 5) {
                    throw new AssertionError("summary lost queue entries");
                }
                expect(slots.get(0), TradeHall.Outcome.FOUND, "found");
                expect(slots.get(1), TradeHall.Outcome.SKIPPED, "locked");
                expect(slots.get(2), TradeHall.Outcome.SKIPPED, "out_of_reach");
                expect(slots.get(3), TradeHall.Outcome.FOUND, "found");
                expect(slots.get(4), TradeHall.Outcome.FOUND, "found");
            });
            if (!sawAir[0] || !sawAir[1] || !sawAir[2]) {
                throw new AssertionError("a novice lectern was never broken: " + sawAir[0] + " " + sawAir[1] + " " + sawAir[2]);
            }
        }
    }

    private static void pen(TestSingleplayerContext world, int x, int y, int lecternZ, int villagerZ) {
        int minZ = Math.min(lecternZ, villagerZ);
        int maxZ = Math.max(lecternZ, villagerZ);
        for (int dz = minZ - 1; dz <= maxZ + 1; dz++) {
            world.getServer().runCommand("setblock " + (x - 1) + " " + y + " " + dz + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + (x + 1) + " " + y + " " + dz + " minecraft:oak_fence");
        }
        world.getServer().runCommand("setblock " + x + " " + y + " " + (minZ - 1) + " minecraft:oak_fence");
        world.getServer().runCommand("setblock " + x + " " + y + " " + (maxZ + 1) + " minecraft:oak_fence");
        world.getServer().runCommand("setblock " + x + " " + y + " " + lecternZ + " minecraft:lectern");
    }

    private static void expect(TradeHall.Slot slot, TradeHall.Outcome outcome, String detail) {
        if (slot.outcome() != outcome || !detail.equals(slot.detail())) {
            throw new AssertionError("expected " + outcome + " " + detail + " but was " + slot.outcome() + " " + slot.detail()
                    + " after " + slot.attempts() + " attempts");
        }
    }

    private static void queueVillager(ClientGameTestContext context, double x, double y, double z, int offset) {
        int before = context.computeOnClient(client -> TradeHall.size());
        context.runOnClient(client -> {
            Villager villager = nearestTo(client, x, y, z);
            if (villager == null) {
                throw new AssertionError("no villager to queue at " + x + " " + z);
            }
            LibrarianTracker.remember(villager);
            McBridge.interact(client.gameMode, client.player, villager, InteractionHand.MAIN_HAND);
        });
        context.waitForScreen(MerchantScreen.class);
        context.waitTicks(10);
        context.clickScreenButton("text.villager_reroller.button.offers");
        context.waitTicks(5);
        context.runOnClient(client -> {
            if (!(McBridge.currentScreen(client) instanceof MerchantScreen screen)) {
                throw new AssertionError("trade screen closed before the targets were chosen");
            }
            int selected = TradeSession.selectAllExcept(screen.getMenu().getOffers());
            if (selected <= 0) {
                throw new AssertionError("no unoffered trades at " + x + "," + z + " catalog=" + TradeSession.entries().size());
            }
            for (TradeEntry entry : TradeSession.entries()) {
                if (!TradeSession.selected(entry.id) || !entry.paysEmeralds || entry.emeraldMin == entry.emeraldMax) {
                    continue;
                }
                int ceiling = Math.min(entry.emeraldMax, entry.emeraldMin + offset);
                TradeSession.setChosenMax(entry.id, ceiling);
                break;
            }
        });
        context.clickScreenButton("text.villager_reroller.button.queue");
        context.waitTicks(5);
        int after = context.computeOnClient(client -> TradeHall.size());
        if (after != before + 1) {
            throw new AssertionError("queue did not grow at " + x + "," + z + " (" + before + " -> " + after + ")");
        }
        context.runOnClient(client -> {
            if (client.player != null) {
                client.player.closeContainer();
            }
        });
        context.waitFor(client -> McBridge.currentScreen(client) == null, 20 * 10);
    }

    private static void summon(TestSingleplayerContext world, String tag, double x, double y, double z, boolean locked) {
        String data = locked
                ? "{PersistenceRequired:1b,Tags:[\"" + tag + "\"],Xp:25,VillagerData:{profession:\"minecraft:librarian\",level:2,type:\"minecraft:plains\"}}"
                : "{PersistenceRequired:1b,Tags:[\"" + tag + "\"]}";
        world.getServer().runCommand("summon minecraft:villager " + x + " " + y + " " + z + " " + data);
    }

    private static int countLibrarians(net.minecraft.client.Minecraft client) {
        if (client.level == null || client.player == null) {
            return 0;
        }
        int count = 0;
        AABB box = client.player.getBoundingBox().inflate(48.0);
        for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
            if (isLibrarian(villager)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isLibrarian(Villager villager) {
        return villager != null && villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN);
    }

    private static boolean wandered(net.minecraft.client.Minecraft client, double x, double y, double z) {
        Villager villager = nearestTo(client, x, y, z);
        return villager == null || villager.distanceToSqr(x, y, z) > 4.0;
    }

    private static Villager nearestTo(net.minecraft.client.Minecraft client, double x, double y, double z) {
        if (client.level == null) {
            return null;
        }
        Villager best = null;
        double bestDistance = Double.MAX_VALUE;
        AABB box = new AABB(x, y, z, x, y, z).inflate(6.0);
        for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
            double distance = villager.distanceToSqr(x, y, z);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = villager;
            }
        }
        return best;
    }

    private static int idOf(net.minecraft.client.Minecraft client, double x, double y, double z) {
        Villager villager = nearestTo(client, x, y, z);
        if (villager == null) {
            throw new AssertionError("missing villager at " + x + " " + z);
        }
        return villager.getId();
    }

    private static void assertBlocker(net.minecraft.client.Minecraft client, double x, double y, double z, String expected) {
        String blocker = TradeHall.blocker(client, idOf(client, x, y, z));
        if (expected == null) {
            if (blocker != null) {
                throw new AssertionError("villager at " + x + "," + z + " was blocked by " + blocker);
            }
            return;
        }
        if (!expected.equals(blocker)) {
            throw new AssertionError("villager at " + x + "," + z + " blocker was " + blocker);
        }
    }

    private static Path artifactsDir() {
        String configured = System.getProperty("villager_reroller.artifacts");
        if (configured == null || configured.isBlank()) {
            return ARTIFACTS;
        }
        return Path.of(configured);
    }

    private static void saveShot(ClientGameTestContext context, String name) {
        Path shot = context.takeScreenshot(name);
        try {
            Path dir = artifactsDir();
            Files.createDirectories(dir);
            Files.createDirectories(ARTIFACTS);
            Files.copy(shot, dir.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(shot, ARTIFACTS.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void waitForChunks(TestSingleplayerContext world) {
        //? if >=26.2 {
        /*world.getConnection().waitForChunksRender();
        *///?} else {
        //? if >=26.1 {
        /*world.getClientLevel().waitForChunksRender();
        *///?} else {
        world.getClientWorld().waitForChunksRender();
        //?}
        //?}
    }
}
