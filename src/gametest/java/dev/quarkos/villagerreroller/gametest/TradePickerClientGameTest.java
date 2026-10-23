package dev.quarkos.villagerreroller.gametest;

import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.compat.TradeCyclingHook;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.RerollMethod;
import dev.quarkos.villagerreroller.picker.TradeEntry;
import dev.quarkos.villagerreroller.picker.TradePickerUi;
import dev.quarkos.villagerreroller.picker.TradeSession;
import dev.quarkos.villagerreroller.text.ScrollingNames;
import dev.quarkos.villagerreroller.text.TradeNameLayout;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
//? if <1.21.11 {
/*import net.minecraft.client.gui.components.AbstractWidget;
*///?} else {
import net.minecraft.client.gui.ActiveTextCollector;
//?}
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
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
            waitForChunks(world);
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
            world.getServer().runCommand("tp @p " + (x + 0.5) + " " + y + " " + (z - 1.6) + " facing " + (x + 1.0) + " " + (y + 1) + " " + z);
            context.waitTicks(5);
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runCommand("clear @p");
            world.getServer().runCommand("give @p minecraft:diamond_axe");
            world.getServer().runCommand("give @p minecraft:lectern 16");
            waitForChunks(world);

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
            boolean cycling = context.computeOnClient(client -> TradeCyclingHook.installed());
            if (!cycling) {
                saveShot(context, "merchant-button");
            }
            context.clickScreenButton("text.villager_reroller.button.offers");
            context.waitTicks(5);
            context.runOnClient(client -> {
                //? if <1.21.11 {
                /*if (ScrollingNames.PERIOD_PER_PIXEL != AbstractWidget.PERIOD_PER_SCROLLED_PIXEL
                        || ScrollingNames.MIN_PERIOD_SECONDS != AbstractWidget.MIN_SCROLL_PERIOD) {
                *///?} else {
                if (ScrollingNames.PERIOD_PER_PIXEL != ActiveTextCollector.PERIOD_PER_SCROLLED_PIXEL
                        || ScrollingNames.MIN_PERIOD_SECONDS != ActiveTextCollector.MIN_SCROLL_PERIOD) {
                //?}
                    throw new AssertionError("name scroll period does not match vanilla");
                }
                boolean ranged = TradeSession.entries().stream().anyMatch(entry -> entry.paysEmeralds && entry.emeraldMin != entry.emeraldMax);
                boolean fixed = TradeSession.entries().stream().anyMatch(entry -> entry.emeraldMin == entry.emeraldMax && entry.emeraldMin > 0);
                if (!ranged || !fixed) {
                    throw new AssertionError("panel catalog missing a fixed price or a min/max range: " + TradeSession.entries().size());
                }
                if (!(McBridge.currentScreen(client) instanceof MerchantScreen)) {
                    throw new AssertionError("offers button did not leave the merchant screen open");
                }
            });
            context.waitFor(client -> scrollSpot(client) == ScrollSpot.MID, 20 * 40);
            if (!cycling) {
                saveShot(context, "name-scroll-mid");
                copyArtifact("name-scroll-mid", "name-scroll");
                copyArtifact("name-scroll-mid", "trade-panel");
            }
            context.waitFor(client -> scrollSpot(client) == ScrollSpot.MAX, 20 * 40);
            if (!cycling) {
                saveShot(context, "name-scroll-max");
                copyArtifact("name-scroll-max", "panel-job-site");
            } else {
                saveShot(context, "panel-job-site");
                context.clickScreenButton("text.villager_reroller.method.job_site");
                context.waitTicks(5);
                boolean switched = context.computeOnClient(client -> RerollMethod.usesTradeCycling());
                if (!switched) {
                    throw new AssertionError("method toggle did not select Trade Cycling");
                }
                context.waitFor(client -> scrollSpot(client) == ScrollSpot.MAX, 20 * 40);
                saveShot(context, "panel-trade-cycling");
            }
            context.runOnClient(client -> assertNameClearsThePrice(client));
            context.runOnClient(client -> {
                if (!(McBridge.currentScreen(client) instanceof MerchantScreen screen)) {
                    throw new AssertionError("offers button did not leave the merchant screen open");
                }
                int selected = TradeSession.selectAllExcept(screen.getMenu().getOffers());
                if (selected <= 0) {
                    throw new AssertionError("every catalog row was already on the counter");
                }
            });

            BlockPos lecternPos = new BlockPos(x, y, z);
            boolean lecternThere = context.computeOnClient(client -> client.level.getBlockState(lecternPos).is(Blocks.LECTERN));
            if (!lecternThere) {
                throw new AssertionError("lectern missing before the reroll");
            }
            int lecternsBefore = context.computeOnClient(TradePickerClientGameTest::lecternCount);
            context.clickScreenButton("text.villager_reroller.button.start");
            String reason = "running";
            boolean sawAir = false;
            for (int tick = 0; tick < 20 * 300; tick++) {
                context.waitTick();
                if (tick % 40 == 0) {
                    world.getServer().runCommand("tp @e[type=minecraft:villager,limit=1,sort=nearest] " + villagerX + " " + y + " " + villagerZ + " 90 0");
                }
                boolean air = context.computeOnClient(client -> client.level.getBlockState(lecternPos).isAir());
                if (air) {
                    sawAir = true;
                }
                boolean running = context.computeOnClient(client -> JobSiteReroller.isRunning());
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
            saveShot(context, cycling ? "reroll-cycling" : "reroll-found");
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

    private enum ScrollSpot {
        NONE,
        MID,
        MAX
    }

    private static ScrollSpot scrollSpot(net.minecraft.client.Minecraft client) {
        TradeEntry bane = bane(client);
        if (bane == null) {
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

    private static void assertNameClearsThePrice(net.minecraft.client.Minecraft client) {
        TradeEntry bane = bane(client);
        if (bane == null) {
            throw new AssertionError("Bane of Arthropods was not in the catalog");
        }
        int priceWidth = client.font.width(bane.priceLabel());
        int textWidth = client.font.width(bane.name);
        int available = TradePickerUi.nameBudget(priceWidth);
        int nameLeft = TradeNameLayout.NAME_LEFT;
        int nameRight = nameLeft + available;
        int priceX = nameRight + TradeNameLayout.PRICE_GAP;
        int overflow = textWidth - available;
        if (overflow < 8) {
            throw new AssertionError("Bane of Arthropods does not overflow the name box");
        }
        for (int offset = 0; offset <= overflow; offset++) {
            int visibleRight = TradeNameLayout.clippedRight(nameLeft, nameRight, textWidth, offset);
            if (visibleRight > priceX - TradeNameLayout.PRICE_GAP) {
                throw new AssertionError("name reaches the price at offset " + offset);
            }
        }
    }

    private static TradeEntry bane(net.minecraft.client.Minecraft client) {
        if (client.font == null) {
            return null;
        }
        for (TradeEntry entry : TradeSession.entries()) {
            if (entry.name.startsWith("Bane of Arthropods")) {
                return entry;
            }
        }
        return null;
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
            Files.copy(shot, dir.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
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

    private static void copyArtifact(String from, String to) {
        try {
            Path dir = artifactsDir();
            Files.copy(dir.resolve(from + ".png"), dir.resolve(to + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
