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
            // Wide enough for the out-of-reach villager (~28 blocks) and small
            // enough that waitForChunksRender finishes in the client gametest.
            client.options.renderDistance().set(4);
            client.options.simulationDistance().set(6);
            client.options.framerateLimit().set(20);
            if (JobSiteReroller.isRunning()) {
                JobSiteReroller.stop(JobSiteReroller.StopReason.STOPPED);
            }
            TradeHall.clear();
            RerollMethod.reset();
            LibrarianTracker.unpin();
            // The single-villager test leaves the panel open in this same client.
            TradeSession.setPanelOpen(false);
            TradeSession.setFilter("");
            TradeSession.restoreSelections(java.util.Map.of());
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
            // Three lecterns in a row, each villager one block behind their own lectern
            // (south), two blocks apart so the closest job site is unambiguous. The
            // player stands just north of the middle lectern: the break ray hits that
            // lectern's north face, and the side rays pass through the air gaps.
            // Villagers are frozen with movement speed 0 after they take the job, so
            // the brain still drops and reclaims the profession but they cannot walk
            // out of reach. The locked and far lecterns stay loaded and outside the
            // novice search cube.
            int x = feet.getX() + 8;
            int y = feet.getY();
            int z = feet.getZ() + 8;
            int[] cellX = {x, x + 2, x + 4};
            double playerX = x + 2.5;
            double playerZ = z - 1.0;
            double lockedX = x + 2.5;
            double lockedZ = z - 7.5;
            int farX = x + 2;
            int farZ = z + 9;

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
            world.getServer().runCommand("fill " + (x - 2) + " " + (y - 1) + " " + (z - 12) + " " + (x + 6) + " " + (y - 1) + " " + (z + 12) + " minecraft:stone");
            world.getServer().runCommand("fill " + (x - 2) + " " + y + " " + (z - 12) + " " + (x + 6) + " " + (y + 3) + " " + (z + 12) + " minecraft:air");

            for (int cx : cellX) {
                world.getServer().runCommand("setblock " + cx + " " + y + " " + z + " minecraft:lectern");
            }
            // Gap fences keep a novice from stepping onto a neighbor's lectern. They sit
            // beside the villager, behind the block face the player breaks.
            world.getServer().runCommand("setblock " + (x + 1) + " " + y + " " + (z + 1) + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + (x + 3) + " " + y + " " + (z + 1) + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + (x - 1) + " " + y + " " + (z + 1) + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + (x + 5) + " " + y + " " + (z + 1) + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + x + " " + y + " " + (z + 2) + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + (x + 2) + " " + y + " " + (z + 2) + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + (x + 4) + " " + y + " " + (z + 2) + " minecraft:oak_fence");
            world.getServer().runCommand("setblock " + (x + 2) + " " + y + " " + (z - 9) + " minecraft:lectern");
            world.getServer().runCommand("setblock " + farX + " " + y + " " + farZ + " minecraft:lectern");

            String[] tags = {"hall_a", "hall_b", "hall_c"};
            for (int i = 0; i < cellX.length; i++) {
                summon(world, tags[i], cellX[i] + 0.5, y, z + 1.5, false);
            }
            summon(world, "hall_locked", lockedX, y, lockedZ, true);
            summon(world, "hall_far", farX + 0.5, y, farZ + 1.5, false);

            world.getServer().runCommand("tp @p " + playerX + " " + y + " " + playerZ + " 0 0");
            context.waitTicks(5);
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("give @p minecraft:diamond_axe");
            world.getServer().runCommand("give @p minecraft:lectern 32");
            waitForChunks(world);

            context.waitFor(client -> isLibrarian(named(client, "hall_a"))
                    && isLibrarian(named(client, "hall_b"))
                    && isLibrarian(named(client, "hall_c"))
                    && isLibrarian(named(client, "hall_locked")), 20 * 40);
            world.getServer().runCommand("data merge entity @e[type=minecraft:villager,tag=hall_locked,limit=1] {Xp:25}");
            context.waitFor(client -> {
                Villager locked = named(client, "hall_locked");
                return locked != null && (McBridge.villagerXp(locked) > 0 || McBridge.villagerLevel(locked) > 1);
            }, 20 * 20);
            // Speed 0 keeps them on the lectern. NoAI would also freeze the brain, so
            // breaking the lectern would never clear the profession.
            for (String tag : new String[] {"hall_a", "hall_b", "hall_c", "hall_locked"}) {
                freeze(world, tag);
            }

            place(world, context, "hall_a", cellX[0] + 0.5, y, z + 1.5);
            queueVillager(world, context, "hall_a", cellX[0] + 0.5, y, z + 1.5, 0, true);
            place(world, context, "hall_locked", lockedX, y, lockedZ);
            queueVillager(world, context, "hall_locked", lockedX, y, lockedZ, 1, false);
            place(world, context, "hall_far", farX + 0.5, y, farZ + 1.5);
            context.waitFor(client -> {
                Villager far = named(client, "hall_far");
                return far != null && isLibrarian(far);
            }, 20 * 40);
            freeze(world, "hall_far");
            place(world, context, "hall_far", farX + 0.5, y, farZ + 1.5);
            queueVillager(world, context, "hall_far", farX + 0.5, y, farZ + 1.5, 3, false);
            world.getServer().runCommand("tp @p " + playerX + " " + y + " " + playerZ + " 0 0");
            context.waitTicks(10);
            place(world, context, "hall_b", cellX[1] + 0.5, y, z + 1.5);
            queueVillager(world, context, "hall_b", cellX[1] + 0.5, y, z + 1.5, 2, false);
            place(world, context, "hall_c", cellX[2] + 0.5, y, z + 1.5);
            queueVillager(world, context, "hall_c", cellX[2] + 0.5, y, z + 1.5, 4, false);

            int queued = context.computeOnClient(client -> TradeHall.size());
            if (queued != 5) {
                throw new AssertionError("expected 5 queued villagers, got " + queued);
            }
            // Teleporting the player closes the hall screen, so stand at the anchor first.
            world.getServer().runCommand("tp @p " + playerX + " " + y + " " + playerZ + " 0 0");
            place(world, context, "hall_a", cellX[0] + 0.5, y, z + 1.5);
            place(world, context, "hall_b", cellX[1] + 0.5, y, z + 1.5);
            place(world, context, "hall_c", cellX[2] + 0.5, y, z + 1.5);
            place(world, context, "hall_locked", lockedX, y, lockedZ);
            context.waitTicks(40);
            openTrades(world, context, "hall_c", cellX[2] + 0.5, y, z + 1.5, false);
            context.waitTicks(8);
            openOffers(context);
            // The hall button's label includes the queue size, so the plain translation key does not match it.
            String hallLabel = context.computeOnClient(client -> net.minecraft.network.chat.Component
                    .translatable("text.villager_reroller.button.hall", TradeHall.size())
                    .getString());
            context.clickScreenButton(hallLabel);
            context.waitForScreen(TradeHallScreen.class);
            context.waitTicks(10);
            saveShot(context, "hall-queue");

            context.runOnClient(client -> {
                if (TradeHall.slots().size() != 5) {
                    throw new AssertionError("queue screen lost villagers");
                }
                assertBlocker(client, "hall_a", null);
                assertBlocker(client, "hall_b", null);
                assertBlocker(client, "hall_c", null);
                assertBlocker(client, "hall_locked", "locked");
                assertBlocker(client, "hall_far", "out_of_reach");
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
            BlockPos lockedLectern = new BlockPos(x + 2, y, z - 9);
            BlockPos farLectern = new BlockPos(farX, y, farZ);
            BlockPos[] noviceLecterns = {
                    new BlockPos(cellX[0], y, z),
                    new BlockPos(cellX[1], y, z),
                    new BlockPos(cellX[2], y, z)
            };
            boolean[] sawAir = {false, false, false};
            boolean progressShot = false;
            context.waitTicks(20);
            context.clickScreenButton("text.villager_reroller.button.hall_start");
            String phase = "running";
            for (int tick = 0; tick < 20 * 300 * 3; tick++) {
                context.waitTick();
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

    private static void expect(TradeHall.Slot slot, TradeHall.Outcome outcome, String detail) {
        if (slot.outcome() != outcome || !detail.equals(slot.detail())) {
            throw new AssertionError("expected " + outcome + " " + detail + " but was " + slot.outcome() + " " + slot.detail()
                    + " after " + slot.attempts() + " attempts");
        }
    }

    /** Stops pathing without stopping the villager brain. */
    private static void freeze(TestSingleplayerContext world, String tag) {
        world.getServer().runCommand("attribute @e[type=minecraft:villager,tag=" + tag + ",limit=1] minecraft:movement_speed base set 0");
    }

    private static void place(TestSingleplayerContext world, ClientGameTestContext context, String tag, double x, double y, double z) {
        world.getServer().runCommand("tp @e[type=minecraft:villager,tag=" + tag + ",limit=1] " + x + " " + y + " " + z + " 180 0");
        int stable = 0;
        for (int tick = 0; tick < 20 * 10; tick++) {
            context.waitTick();
            boolean arrived = context.computeOnClient(client -> {
                Villager villager = named(client, tag);
                return villager != null && villager.distanceToSqr(x, y, z) <= 2.25;
            });
            if (arrived) {
                stable++;
                if (stable >= 5) {
                    return;
                }
            } else {
                stable = 0;
            }
        }
        throw new AssertionError(tag + " did not arrive at " + x + "," + z + " " + context.computeOnClient(client -> describe(client, tag, x, y, z)));
    }

    /** The book button toggles. A second click closes the panel and hides Queue. */
    private static void openOffers(ClientGameTestContext context) {
        boolean open = context.computeOnClient(client -> TradeSession.panelOpen());
        if (!open) {
            context.clickScreenButton("text.villager_reroller.button.offers");
            context.waitTicks(5);
        }
    }

    /**
     * Stands just north of the villager and retries the direct interact until the
     * merchant screen opens. One interact after a teleport misses while the client
     * entity is still at the old position.
     */
    private static void openTrades(TestSingleplayerContext world, ClientGameTestContext context, String tag, double x, double y, double z, boolean approach) {
        if (approach) {
            world.getServer().runCommand("tp @e[type=minecraft:villager,tag=" + tag + ",limit=1] " + x + " " + y + " " + z + " 180 0");
            world.getServer().runCommand("tp @p " + x + " " + y + " " + (z - 2.0) + " facing " + x + " " + (y + 1.0) + " " + z);
        }
        for (int tick = 0; tick < 20 * 15; tick++) {
            context.waitTick();
            boolean ready = context.computeOnClient(client -> {
                Villager villager = named(client, tag);
                return villager != null
                        && isLibrarian(villager)
                        && client.player != null
                        && McBridge.withinEntity(client.player, villager);
            });
            if (ready) {
                break;
            }
            if (tick == 20 * 15 - 1) {
                throw new AssertionError(tag + " was not in reach " + context.computeOnClient(client -> describe(client, tag, x, y, z)));
            }
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            context.runOnClient(client -> {
                Villager villager = named(client, tag);
                if (villager == null || client.player == null) {
                    return;
                }
                lookAt(client.player, villager.getEyePosition());
                LibrarianTracker.remember(villager);
                McBridge.interact(client.gameMode, client.player, villager, InteractionHand.MAIN_HAND);
            });
            for (int tick = 0; tick < 20; tick++) {
                context.waitTick();
                if (context.computeOnClient(client -> McBridge.currentScreen(client) instanceof MerchantScreen)) {
                    return;
                }
            }
        }
        throw new AssertionError("trade screen did not open for " + tag + " " + context.computeOnClient(client -> describe(client, tag, x, y, z)));
    }

    private static void lookAt(net.minecraft.client.player.LocalPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        McBridge.sendLook(player, yaw, pitch);
    }

    private static void queueVillager(TestSingleplayerContext world, ClientGameTestContext context, String tag, double x, double y, double z, int offset, boolean capturePanel) {
        int before = context.computeOnClient(client -> TradeHall.size());
        openTrades(world, context, tag, x, y, z, true);
        context.waitTicks(10);
        openOffers(context);
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
        if (capturePanel) {
            saveShot(context, "hall-panel");
        }
        context.runOnClient(client -> {
            if (client.player != null) {
                client.player.closeContainer();
            }
        });
        context.waitFor(client -> McBridge.currentScreen(client) == null, 20 * 10);
    }

    private static void summon(TestSingleplayerContext world, String tag, double x, double y, double z, boolean locked) {
        String name = "CustomName:{text:\"" + tag + "\"}";
        String data = locked
                ? "{PersistenceRequired:1b," + name + ",Tags:[\"" + tag + "\"],Xp:25,VillagerData:{profession:\"minecraft:librarian\",level:2,type:\"minecraft:plains\"}}"
                : "{PersistenceRequired:1b," + name + ",Tags:[\"" + tag + "\"]}";
        world.getServer().runCommand("summon minecraft:villager " + x + " " + y + " " + z + " " + data);
    }

    private static Villager named(net.minecraft.client.Minecraft client, String tag) {
        if (client.level == null || client.player == null) {
            return null;
        }
        AABB box = client.player.getBoundingBox().inflate(80.0);
        for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
            if (villager.hasCustomName() && villager.getCustomName() != null && tag.equals(villager.getCustomName().getString())) {
                return villager;
            }
        }
        return null;
    }

    private static String describe(net.minecraft.client.Minecraft client, String tag, double x, double y, double z) {
        Villager villager = named(client, tag);
        if (villager == null || client.player == null) {
            return "missing name=" + tag + " librarians=" + countLibrarians(client);
        }
        boolean reach = McBridge.withinEntity(client.player, villager);
        return "pos=" + villager.position()
                + " distSq=" + villager.distanceToSqr(x, y, z)
                + " reach=" + reach
                + " librarian=" + isLibrarian(villager)
                + " screen=" + (McBridge.currentScreen(client) == null ? "none" : McBridge.currentScreen(client).getClass().getSimpleName());
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

    private static void assertBlocker(net.minecraft.client.Minecraft client, String tag, String expected) {
        Villager villager = named(client, tag);
        if (villager == null) {
            throw new AssertionError(tag + " missing during blocker check");
        }
        String blocker = TradeHall.blocker(client, villager.getId());
        if (expected == null) {
            if (blocker != null) {
                throw new AssertionError(tag + " was blocked by " + blocker);
            }
            return;
        }
        if (!expected.equals(blocker)) {
            throw new AssertionError(tag + " blocker was " + blocker);
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
