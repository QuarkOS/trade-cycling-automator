package dev.quarkos.villagerreroller.gametest;

import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.RerollMethod;
import dev.quarkos.villagerreroller.picker.TradeSession;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One librarian whose current offers are not selected, frozen in place the way
 * the trading hall freezes them. The job-site path has to break the lectern on
 * the server, and the villager has to lose the profession, before the block is
 * put back.
 */
@SuppressWarnings("UnstableApiUsage")
public class LecternBreakClientGameTest implements FabricClientGameTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.pauseOnLostFocus = false);
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(client -> {
            client.options.guiScale().set(2);
            client.options.renderDistance().set(4);
            client.options.simulationDistance().set(6);
            client.options.framerateLimit().set(20);
            if (JobSiteReroller.isRunning()) {
                JobSiteReroller.stop(JobSiteReroller.StopReason.STOPPED);
            }
            RerollMethod.reset();
            LibrarianTracker.unpin();
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
            int x = feet.getX() + 4;
            int y = feet.getY();
            int z = feet.getZ() + 4;
            double villagerX = x + 0.5;
            double villagerZ = z + 1.5;
            // Same offset as the hall's first novice: the player stands in front of a
            // neighboring lectern, two blocks to the side of this one.
            double playerX = x + 2.5;
            double playerZ = z - 1.0;

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
            world.getServer().runCommand("fill " + (x - 2) + " " + (y - 1) + " " + (z - 3) + " " + (x + 2) + " " + (y - 1) + " " + (z + 3) + " minecraft:stone");
            world.getServer().runCommand("fill " + (x - 2) + " " + y + " " + (z - 3) + " " + (x + 2) + " " + (y + 3) + " " + (z + 3) + " minecraft:air");
            world.getServer().runCommand("setblock " + x + " " + y + " " + z + " minecraft:lectern");
            world.getServer().runCommand("summon minecraft:villager " + villagerX + " " + y + " " + villagerZ + " {PersistenceRequired:1b,CustomName:{text:\"break_librarian\"}}");
            world.getServer().runCommand("tp @p " + playerX + " " + y + " " + playerZ + " 0 0");
            context.waitTicks(5);
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("give @p minecraft:diamond_axe");
            world.getServer().runCommand("give @p minecraft:lectern 16");
            waitForChunks(world);

            context.waitFor(client -> isLibrarian(named(client)), 20 * 40);
            world.getServer().runCommand("attribute @e[type=minecraft:villager,limit=1,sort=nearest] minecraft:movement_speed base set 0");
            world.getServer().runCommand("tp @e[type=minecraft:villager,limit=1,sort=nearest] " + villagerX + " " + y + " " + villagerZ + " 180 0");
            context.waitTicks(10);

            openTrades(context, villagerX, y, villagerZ);
            context.waitTicks(8);
            boolean panelOpen = context.computeOnClient(client -> TradeSession.panelOpen());
            if (!panelOpen) {
                context.clickScreenButton("text.villager_reroller.button.offers");
                context.waitTicks(5);
            }
            context.runOnClient(client -> {
                if (!(McBridge.currentScreen(client) instanceof MerchantScreen screen)) {
                    throw new AssertionError("offers button did not leave the merchant screen open");
                }
                int selected = TradeSession.selectAllExcept(screen.getMenu().getOffers());
                if (selected <= 0) {
                    throw new AssertionError("every catalog row was already on the counter");
                }
                if (TradeSession.offerMatchesSelection(screen.getMenu().getOffers())) {
                    throw new AssertionError("librarian already matched the selection, so a lectern break would be skipped");
                }
                if (RerollMethod.usesTradeCycling()) {
                    throw new AssertionError("job-site break test was left on Trade Cycling");
                }
            });

            BlockPos lectern = new BlockPos(x, y, z);
            if (!lecternOn(context, world, lectern, true) || !lecternOn(context, world, lectern, false)) {
                throw new AssertionError("lectern missing before the break: " + describe(context, world, lectern));
            }
            context.clickScreenButton("text.villager_reroller.button.start");

            boolean sawClientAir = false;
            boolean sawServerAir = false;
            boolean sawServerUnemployed = false;
            int predictedBreakTicks = 0;
            String reason = "running";
            StringBuilder trace = new StringBuilder();
            String previous = "";
            for (int tick = 0; tick < 20 * 40; tick++) {
                context.waitTick();
                String clientSide = context.computeOnClient(client -> clientSide(client, lectern));
                String serverSide = world.getServer().computeOnServer(server -> serverSide(server, lectern));
                if (clientSide.startsWith("air")) {
                    sawClientAir = true;
                }
                if (serverSide.startsWith("air")) {
                    sawServerAir = true;
                }
                if (serverSide.contains("unemployed")) {
                    sawServerUnemployed = true;
                }
                if (clientSide.startsWith("air") && serverSide.startsWith("lectern") && serverSide.contains("librarian")) {
                    predictedBreakTicks++;
                    if (predictedBreakTicks > 40) {
                        throw new AssertionError("client treated the lectern as broken while the villager kept the profession\n" + trace);
                    }
                } else {
                    predictedBreakTicks = 0;
                }
                String line = tick + " client=" + clientSide + " server=" + serverSide;
                if (!line.substring(line.indexOf(' ')).equals(previous)) {
                    trace.append(line).append('\n');
                    LOGGER.info("lectern break {}", line);
                    previous = line.substring(line.indexOf(' '));
                }
                JobSiteReroller.StopReason stop = context.computeOnClient(client -> JobSiteReroller.stopReason());
                boolean running = context.computeOnClient(client -> JobSiteReroller.isRunning());
                if (!running && stop != null) {
                    reason = stop.name();
                    break;
                }
            }
            if (!"FOUND".equals(reason)) {
                throw new AssertionError("reroll stopped with " + reason
                        + " clientAir=" + sawClientAir
                        + " serverAir=" + sawServerAir
                        + " serverUnemployed=" + sawServerUnemployed
                        + "\n" + trace);
            }
            if (!sawServerAir || !sawServerUnemployed) {
                throw new AssertionError("lectern break was not exercised on the server: clientAir="
                        + sawClientAir + " serverAir=" + sawServerAir + " serverUnemployed=" + sawServerUnemployed
                        + "\n" + trace);
            }
            context.waitForScreen(MerchantScreen.class);
            context.waitTicks(10);
            if (!lecternOn(context, world, lectern, true) || !lecternOn(context, world, lectern, false)) {
                throw new AssertionError("lectern was not replaced: " + describe(context, world, lectern));
            }
            boolean librarian = context.computeOnClient(client -> isLibrarian(named(client)));
            boolean serverLibrarian = world.getServer().computeOnServer(server -> serverSide(server, lectern).contains("librarian"));
            if (!librarian || !serverLibrarian) {
                throw new AssertionError("villager did not take the lectern back: " + describe(context, world, lectern));
            }
        }
    }

    private static boolean lecternOn(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos, boolean server) {
        if (server) {
            return world.getServer().computeOnServer(game -> game.overworld().getBlockState(pos).is(Blocks.LECTERN));
        }
        return context.computeOnClient(client -> client.level.getBlockState(pos).is(Blocks.LECTERN));
    }

    private static String describe(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos) {
        return "client=" + context.computeOnClient(client -> clientSide(client, pos))
                + " server=" + world.getServer().computeOnServer(server -> serverSide(server, pos));
    }

    private static String clientSide(net.minecraft.client.Minecraft client, BlockPos pos) {
        if (client.level == null) {
            return "nolevel";
        }
        String block = client.level.getBlockState(pos).isAir() ? "air" : "lectern";
        Villager villager = named(client);
        String profession = villager == null ? "missing" : McBridge.unemployed(villager) ? "unemployed" : McBridge.professionId(villager);
        boolean running = JobSiteReroller.isRunning();
        JobSiteReroller.StopReason stop = JobSiteReroller.stopReason();
        double reach = client.player == null ? -1 : client.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos));
        return block + " " + profession + " running=" + running + " stop=" + stop + " reachSq=" + String.format(java.util.Locale.ROOT, "%.2f", reach);
    }

    private static String serverSide(MinecraftServer server, BlockPos pos) {
        ServerLevel level = server.overworld();
        String block = level.getBlockState(pos).isAir() ? "air" : level.getBlockState(pos).is(Blocks.LECTERN) ? "lectern" : "other";
        String profession = "missing";
        for (Villager villager : level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(8.0), entity -> true)) {
            profession = McBridge.unemployed(villager) ? "unemployed" : McBridge.professionId(villager);
            break;
        }
        double reach = -1;
        if (!server.getPlayerList().getPlayers().isEmpty()) {
            reach = server.getPlayerList().getPlayers().get(0).getEyePosition().distanceToSqr(Vec3.atCenterOf(pos));
        }
        return block + " " + profession + " reachSq=" + String.format(java.util.Locale.ROOT, "%.2f", reach);
    }

    private static void openTrades(ClientGameTestContext context, double x, double y, double z) {
        for (int attempt = 0; attempt < 8; attempt++) {
            context.runOnClient(client -> {
                Villager villager = named(client);
                if (villager == null || client.player == null || client.gameMode == null) {
                    return;
                }
                Vec3 eyes = villager.getEyePosition();
                Vec3 eye = client.player.getEyePosition();
                double dx = eyes.x - eye.x;
                double dy = eyes.y - eye.y;
                double dz = eyes.z - eye.z;
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
                client.player.setYRot(yaw);
                client.player.setXRot(pitch);
                client.player.setYHeadRot(yaw);
                McBridge.sendLook(client.player, yaw, pitch);
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
        throw new AssertionError("trade screen did not open at " + x + "," + z);
    }

    private static Villager named(net.minecraft.client.Minecraft client) {
        if (client.level == null || client.player == null) {
            return null;
        }
        AABB box = client.player.getBoundingBox().inflate(16.0);
        for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
            if (villager.hasCustomName() && villager.getCustomName() != null
                    && "break_librarian".equals(villager.getCustomName().getString())) {
                return villager;
            }
        }
        return null;
    }

    private static boolean isLibrarian(Villager villager) {
        return villager != null && villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN);
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
