package dev.quarkos.villagerreroller.gametest;

import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.ForgeConfigScreen;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.config.VillagerRoster;
import dev.quarkos.villagerreroller.config.VillagerRosterFiles;
import dev.quarkos.villagerreroller.picker.MyVillagersScreen;
import dev.quarkos.villagerreroller.picker.TradeEntry;
import dev.quarkos.villagerreroller.picker.TradeHall;
import dev.quarkos.villagerreroller.picker.TradeHallScreen;
import dev.quarkos.villagerreroller.picker.TradeSession;
import dev.quarkos.villagerreroller.picker.VillagerRosterClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Opens the checklist, the hall, and the trade picker, including the empty
 * checklist and the small GUI scales where text has to ellipsize.
 */
@SuppressWarnings("UnstableApiUsage")
public class ChecklistClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        Path picks = PickFiles.configFile();
        Path villagers = VillagerRosterFiles.configFile();
        byte[] picksBackup = readQuiet(picks);
        byte[] villagerBackup = readQuiet(villagers);
        context.runOnClient(client -> client.options.pauseOnLostFocus = false);
        context.getInput().resizeWindow(1280, 720);
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
            //? if >=1.21.11 {
            world.getServer().runCommand("gamerule send_command_feedback false");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("gamerule spawn_mobs false");
            //?} else {
            /*world.getServer().runCommand("gamerule sendCommandFeedback false");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            world.getServer().runCommand("gamerule doMobSpawning false");
            *///?}
            context.runOnClient(client -> {
                TradeHall.clear();
                VillagerRoster empty = new VillagerRoster();
                VillagerRosterClient.replace(empty);
                saveWanted(Map.of(
                        "enchant:minecraft:mending:1", 12,
                        "enchant:minecraft:unbreaking:3", 14,
                        "enchant:minecraft:efficiency:5", 20));
                client.options.guiScale().set(3);
                resize(client);
            });
            context.waitTicks(5);
            context.setScreen(() -> new MyVillagersScreen(null));
            context.waitTicks(10);
            GameTestSupport.saveShot(context, "checklist-empty");
            scale(context, 4, 1280, 960);
            context.setScreen(() -> new MyVillagersScreen(null));
            context.waitTicks(5);
            GameTestSupport.saveShot(context, "checklist-empty-scale4");
            scale(context, 3, 1280, 720);

            context.runOnClient(client -> {
                String worldKey = VillagerRosterClient.currentWorldKey(client);
                VillagerRoster roster = new VillagerRoster();
                roster.upsert(worldKey, villager("1", "Novice", 1, 1, offer("buy:minecraft:bookshelf:1:9", "Bookshelf", "minecraft:bookshelf", 9)));
                roster.upsert(worldKey, villager("2", "Mending", 5, 2, offer("enchant:minecraft:mending:1", "Mending I", "minecraft:enchanted_book", 10)));
                roster.upsert(worldKey, villager("3", "Unbreaking", 4, 3, offer("enchant:minecraft:unbreaking:3", "Unbreaking III", "minecraft:enchanted_book", 12)));
                roster.upsert(worldKey, villager("4", "Silk", 3, 4, offer("enchant:minecraft:silk_touch:1", "Silk Touch I", "minecraft:enchanted_book", 16)));
                roster.upsert(worldKey, villager("5", "Fortune", 3, 5, offer("enchant:minecraft:fortune:3", "Fortune III", "minecraft:enchanted_book", 14)));
                roster.upsert(worldKey, villager("6", "Looting", 3, 6, offer("enchant:minecraft:looting:3", "Looting III", "minecraft:enchanted_book", 15)));
                roster.upsert(worldKey, villager("7", "Glass", 2, 7, offer("buy:minecraft:glass:1:1", "Glass", "minecraft:glass", 1)));
                roster.upsert(worldKey, villager("8", "Name Tag", 2, 8, offer("buy:minecraft:name_tag:1:20", "Name Tag", "minecraft:name_tag", 20)));
                VillagerRosterClient.replace(roster);
                saveWanted(Map.of(
                        "enchant:minecraft:mending:1", 12,
                        "enchant:minecraft:unbreaking:3", 14,
                        "enchant:minecraft:fortune:3", 16,
                        "enchant:minecraft:looting:3", 18,
                        "enchant:minecraft:efficiency:5", 20,
                        "enchant:minecraft:protection:4", 16,
                        "enchant:minecraft:silk_touch:1", 10));
                client.options.guiScale().set(3);
                resize(client);
            });
            context.setScreen(() -> new MyVillagersScreen(null));
            context.waitTicks(5);
            context.runOnClient(client -> {
                if (McBridge.currentScreen(client) instanceof MyVillagersScreen screen) {
                    screen.scrollTo(1);
                }
            });
            context.waitTicks(5);
            GameTestSupport.saveShot(context, "checklist-populated");
            context.runOnClient(client -> {
                client.options.guiScale().set(2);
                resize(client);
            });
            context.waitTicks(5);
            GameTestSupport.saveShot(context, "checklist-populated-scale2");
            scale(context, 4, 1280, 960);
            context.setScreen(() -> new MyVillagersScreen(null));
            context.waitTicks(5);
            context.runOnClient(client -> {
                if (McBridge.currentScreen(client) instanceof MyVillagersScreen screen) {
                    screen.scrollTo(1);
                }
            });
            context.waitTicks(5);
            GameTestSupport.saveShot(context, "checklist-populated-scale4");
            scale(context, 3, 1280, 720);

            context.runOnClient(client -> {
                TradeHall.clear();
                client.options.guiScale().set(3);
                resize(client);
            });
            context.setScreen(() -> new TradeHallScreen());
            context.waitTicks(8);
            GameTestSupport.saveShot(context, "hall-empty");
            scale(context, 4, 1280, 960);
            context.setScreen(() -> new TradeHallScreen());
            context.waitTicks(5);
            GameTestSupport.saveShot(context, "hall-empty-scale4");
            scale(context, 2, 1280, 720);
            context.setScreen(() -> new ForgeConfigScreen(null));
            context.waitTicks(8);
            GameTestSupport.saveShot(context, "settings-scale2");
            scale(context, 4, 1280, 960);
            context.setScreen(() -> new ForgeConfigScreen(null));
            context.waitTicks(8);
            GameTestSupport.saveShot(context, "settings-scale4");
            // The settings screen pauses the integrated server. Close it before the villager has to claim a lectern.
            context.runOnClient(client -> McBridge.setScreen(client, null));
            context.waitForScreen(null);
            scale(context, 2, 1280, 720);

            BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
            int x = feet.getX() + 4;
            int y = feet.getY();
            int z = feet.getZ();
            world.getServer().runCommand("setblock " + x + " " + y + " " + z + " minecraft:lectern");
            world.getServer().runCommand("summon minecraft:villager " + (x + 1.2) + " " + y + " " + (z + 0.5) + " {PersistenceRequired:1b}");
            world.getServer().runCommand("tp @p " + (x + 0.5) + " " + y + " " + (z - 1.6) + " facing " + x + " " + (y + 1) + " " + z);
            BlockPos job = new BlockPos(x, y, z);
            GameTestSupport.waitForServer(context, world, GameTestSupport.scaled(100), server ->
                    !server.overworld().getEntitiesOfClass(Villager.class, new AABB(job).inflate(4), villager ->
                            villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)).isEmpty(),
                    "Checklist librarian did not claim the lectern");
            context.waitFor(client -> {
                if (client.level == null || client.player == null) {
                    return false;
                }
                AABB box = client.player.getBoundingBox().inflate(16.0);
                for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
                    if (villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
                        return true;
                    }
                }
                return false;
            }, GameTestSupport.scaled(20 * 10));
            context.runOnClient(client -> {
                client.options.guiScale().set(2);
                resize(client);
                Villager nearest = null;
                AABB box = client.player.getBoundingBox().inflate(16.0);
                for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
                    if (villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
                        nearest = villager;
                    }
                }
                if (nearest == null) {
                    throw new AssertionError("librarian missing");
                }
                dev.quarkos.villagerreroller.client.LibrarianTracker.remember(nearest);
                McBridge.interact(client.gameMode, client.player, nearest, net.minecraft.world.InteractionHand.MAIN_HAND);
            });
            context.waitForScreen(MerchantScreen.class);
            context.waitTicks(10);
            openPicker(context);
            context.runOnClient(client -> {
                int toggled = 0;
                for (TradeEntry entry : TradeSession.entries()) {
                    if (entry.paysEmeralds && toggled < 2) {
                        TradeSession.toggle(entry);
                        toggled++;
                    }
                }
                if (toggled == 0) {
                    throw new AssertionError("picker had no emerald trades to select");
                }
            });
            context.waitTicks(5);
            GameTestSupport.saveShot(context, "picker-polished");
            scale(context, 3, 1280, 720);
            context.waitTicks(20);
            reopenMerchant(context, world, x, y, z);
            openPicker(context);
            GameTestSupport.saveShot(context, "picker-scale3");
            scale(context, 4, 1280, 960);
            context.waitTicks(20);
            reopenMerchant(context, world, x, y, z);
            openPicker(context);
            GameTestSupport.saveShot(context, "picker-scale4");
            // 1280x960 at scale 4 is only 320 GUI pixels wide, so the panel stays shut.
            // A 1920-wide window is wide enough for the panel at the same scale.
            scale(context, 4, 1920, 1080);
            context.waitTicks(10);
            reopenMerchant(context, world, x, y, z);
            openPicker(context);
            GameTestSupport.saveShot(context, "picker-scale4-wide");
            context.runOnClient(client -> McBridge.setScreen(client, null));
        } finally {
            restore(picks, picksBackup);
            restore(villagers, villagerBackup);
            VillagerRosterClient.reload();
        }
    }

    private static void saveWanted(Map<String, Integer> prices) {
        ProfessionPicks picks = new ProfessionPicks();
        picks.replaceCatalog("minecraft:librarian", prices.keySet(), prices);
        picks.save(PickFiles.configFile());
    }

    private static VillagerRoster.Entry villager(String uuid, String name, int level, int x, VillagerRoster.LockedOffer offer) {
        return new VillagerRoster.Entry(
                uuid,
                "minecraft:overworld",
                x,
                64,
                8,
                name,
                "minecraft:librarian",
                level,
                x,
                List.of(offer));
    }

    private static VillagerRoster.LockedOffer offer(String id, String name, String item, int cost) {
        return new VillagerRoster.LockedOffer(id, name, "", 0, item, 1, cost, cost, true);
    }

    private static byte[] readQuiet(Path path) {
        try {
            return Files.isRegularFile(path) ? Files.readAllBytes(path) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static void restore(Path path, byte[] backup) {
        try {
            if (backup == null) {
                Files.deleteIfExists(path);
            } else {
                Files.createDirectories(path.getParent());
                Files.write(path, backup);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void resize(net.minecraft.client.Minecraft client) {
        //? if >=26.1 {
        /*client.resizeGui();
        *///?} else {
        client.resizeDisplay();
        //?}
    }

    private static void scale(ClientGameTestContext context, int gui, int width, int height) {
        context.getInput().resizeWindow(width, height);
        context.runOnClient(client -> {
            client.options.guiScale().set(gui);
            resize(client);
        });
        context.waitTicks(5);
    }

    /**
     * A merchant opened while the panel was already open gets it for free, and
     * the offers button would close it. Click only when it is shut.
     */
    private static void openPicker(ClientGameTestContext context) {
        if (Boolean.TRUE.equals(context.computeOnClient(client -> TradeSession.panelOpen()))) {
            return;
        }
        context.clickScreenButton("text.villager_reroller.button.offers");
        context.waitTicks(5);
    }

    /**
     * Closing the merchant and resizing the window lands in the middle of a
     * server stall often enough that one interact is processed before the
     * teleport. Keep offering the trade until the screen is actually open.
     */
    private static void reopenMerchant(ClientGameTestContext context, TestSingleplayerContext world, int x, int y, int z) {
        context.runOnClient(client -> McBridge.setScreen(client, null));
        context.waitTicks(10);
        world.getServer().runCommand("tp @e[type=minecraft:villager,limit=1,sort=nearest] " + (x + 1.2) + " " + y + " " + (z + 0.5) + " 180 0");
        world.getServer().runCommand("tp @p " + (x + 0.5) + " " + y + " " + (z - 1.6) + " facing " + x + " " + (y + 1) + " " + z);
        boolean sawLibrarian = false;
        for (int attempt = 0; attempt < 45; attempt++) {
            context.waitTicks(8);
            if (Boolean.TRUE.equals(context.computeOnClient(client -> McBridge.currentScreen(client) instanceof MerchantScreen))) {
                context.waitTicks(10);
                return;
            }
            boolean found = Boolean.TRUE.equals(context.computeOnClient(client -> {
                if (client.player == null || client.level == null || client.gameMode == null) {
                    return false;
                }
                Villager nearest = null;
                AABB box = client.player.getBoundingBox().inflate(16.0);
                for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
                    if (villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
                        nearest = villager;
                    }
                }
                if (nearest == null) {
                    return false;
                }
                double dx = nearest.getX() - client.player.getX();
                double dy = (nearest.getY() + nearest.getEyeHeight()) - client.player.getEyeY();
                double dz = nearest.getZ() - client.player.getZ();
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.max(horizontal, 1.0E-4))));
                client.player.setYRot(yaw);
                client.player.setXRot(pitch);
                McBridge.sendLook(client.player, yaw, pitch);
                dev.quarkos.villagerreroller.client.LibrarianTracker.remember(nearest);
                McBridge.interact(client.gameMode, client.player, nearest, net.minecraft.world.InteractionHand.MAIN_HAND);
                return true;
            }));
            sawLibrarian |= found;
        }
        if (!sawLibrarian) {
            throw new AssertionError("librarian missing");
        }
        throw new AssertionError("merchant screen did not reopen");
    }
}
