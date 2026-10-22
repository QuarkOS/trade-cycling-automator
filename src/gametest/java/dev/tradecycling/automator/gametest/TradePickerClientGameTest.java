package dev.tradecycling.automator.gametest;

import dev.tradecycling.automator.picker.JobSiteReroller;
import dev.tradecycling.automator.picker.TradeSession;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Spawns a novice librarian, opens the trade screen, and rerolls the lectern
 * until a trade that was not on the counter appears.
 */
@SuppressWarnings("UnstableApiUsage")
public class TradePickerClientGameTest implements FabricClientGameTest {
    private static final Path ARTIFACTS = Path.of("/opt/cursor/artifacts");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.pauseOnLostFocus = false);
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(client -> client.options.guiScale().set(2));

        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL))
                .create()) {
            world.getClientWorld().waitForChunksRender();
            BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
            int x = feet.getX() + 6;
            int y = feet.getY();
            int z = feet.getZ();

            world.getServer().runCommand("gamerule sendCommandFeedback false");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            world.getServer().runCommand("gamerule doMobSpawning false");
            world.getServer().runCommand("gamerule doImmediateRespawn true");
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
            world.getServer().runCommand("tp @p " + (x + 0.5) + " " + y + " " + (z - 2.2) + " facing " + (x + 0.5) + " " + (y + 1) + " " + z);
            context.waitTicks(5);
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("give @p minecraft:diamond_axe");
            world.getServer().runCommand("give @p minecraft:lectern 16");
            world.getClientWorld().waitForChunksRender();

            context.waitFor(client -> {
                if (client.level == null || client.player == null) {
                    return false;
                }
                AABB box = client.player.getBoundingBox().inflate(24.0);
                for (Villager villager : client.level.getEntitiesOfClass(Villager.class, box)) {
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
                dev.tradecycling.automator.client.LibrarianTracker.remember(villager);
                client.gameMode.interact(client.player, villager, net.minecraft.world.InteractionHand.MAIN_HAND);
            });
            context.waitForScreen(MerchantScreen.class);
            context.waitTicks(10);
            saveShot(context, "merchant-button");

            context.clickScreenButton("text.trade-cycling-automator.button.offers");
            context.waitTicks(5);
            context.runOnClient(client -> {
                boolean ranged = TradeSession.entries().stream().anyMatch(entry -> entry.paysEmeralds && entry.emeraldMin != entry.emeraldMax);
                boolean fixed = TradeSession.entries().stream().anyMatch(entry -> entry.emeraldMin == entry.emeraldMax && entry.emeraldMin > 0);
                if (!ranged || !fixed) {
                    throw new AssertionError("panel catalog missing a fixed price or a min/max range: " + TradeSession.entries().size());
                }
                if (!(client.screen instanceof MerchantScreen)) {
                    throw new AssertionError("offers button did not leave the merchant screen open");
                }
            });
            // Capture the ranges before selection replaces a chosen ceiling with "Any".
            saveShot(context, "trade-panel");
            context.runOnClient(client -> {
                if (!(client.screen instanceof MerchantScreen screen)) {
                    throw new AssertionError("offers button did not leave the merchant screen open");
                }
                int selected = TradeSession.selectAllExcept(screen.getMenu().getOffers());
                if (selected <= 0) {
                    throw new AssertionError("every catalog row was already on the counter");
                }
            });

            context.clickScreenButton("text.trade-cycling-automator.button.start");
            String reason = "running";
            for (int tick = 0; tick < 20 * 180; tick++) {
                context.waitTick();
                if (tick % 20 == 0) {
                    world.getServer().runCommand("tp @e[type=minecraft:villager,limit=1] " + villagerX + " " + y + " " + villagerZ + " 90 0");
                }
                boolean running = context.computeOnClient(client -> JobSiteReroller.isRunning());
                JobSiteReroller.StopReason stop = context.computeOnClient(client -> JobSiteReroller.stopReason());
                if (!running && stop != null) {
                    reason = stop.name();
                    break;
                }
            }
            context.waitForScreen(MerchantScreen.class);
            context.waitTicks(5);
            saveShot(context, "reroll-found");
            if (!"FOUND".equals(reason)) {
                int attempts = context.computeOnClient(client -> JobSiteReroller.attempts());
                throw new AssertionError("reroll stopped with " + reason + " after " + attempts + " attempts");
            }
        }
    }

    private static Villager nearestLibrarian(net.minecraft.client.Minecraft client) {
        AABB box = client.player.getBoundingBox().inflate(24.0);
        Villager nearest = null;
        double best = Double.MAX_VALUE;
        for (Villager villager : client.level.getEntitiesOfClass(Villager.class, box)) {
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

    private static void saveShot(ClientGameTestContext context, String name) {
        Path shot = context.takeScreenshot(name);
        try {
            Files.createDirectories(ARTIFACTS);
            Files.copy(shot, ARTIFACTS.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
