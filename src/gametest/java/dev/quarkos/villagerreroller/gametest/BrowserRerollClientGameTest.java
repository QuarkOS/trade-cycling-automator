package dev.quarkos.villagerreroller.gametest;

import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.harness.BrowserHarness;
import dev.quarkos.villagerreroller.picker.GuiDraw;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.RerollMethod;
import dev.quarkos.villagerreroller.picker.TradeCatalog;
import dev.quarkos.villagerreroller.picker.TradeEntry;
import dev.quarkos.villagerreroller.picker.TradeHall;
import dev.quarkos.villagerreroller.picker.TradeSession;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public final class BrowserRerollClientGameTest implements FabricClientGameTest {
    private static final String LIBRARIAN = "minecraft:librarian";
    private static final String NAME = "Browser librarian";
    private static final List<Viewport> VIEWPORTS = List.of(
            new Viewport(1920, 1080, 2), new Viewport(1920, 1080, 4), new Viewport(1280, 960, 4));

    @Override
    public void runTest(ClientGameTestContext context) {
        try {
            rerollFromBrowser(context);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void rerollFromBrowser(ClientGameTestContext context) throws IOException {
        var picksFile = PickFiles.configFile();
        byte[] backup = Files.exists(picksFile) ? Files.readAllBytes(picksFile) : null;
        AutomatorConfig config = AutomatorConfig.get();
        boolean enabled = config.enabled();
        int maxAttempts = config.maxAttempts();
        context.runOnClient(client -> {
            config.setEnabled(true);
            config.setMaxAttempts(120);
            TradeHall.clear();
            TradeSession.simulateClientRestart();
            RerollMethod.reset();
            LibrarianTracker.unpin();
            client.options.pauseOnLostFocus = false;
            client.options.renderDistance().set(3);
            client.options.simulationDistance().set(5);
            client.options.framerateLimit().set(30);
        });
        Files.deleteIfExists(picksFile);
        try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
            settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
            settings.getNormalPresetList().stream().filter(entry -> entry.preset().is(WorldPresets.FLAT))
                    .findFirst().ifPresent(settings::setWorldType);
        }).create()) {
            resize(context, VIEWPORTS.get(0));
            GameTestSupport.waitForChunks(world);
            BlockPos lectern = context.computeOnClient(client -> client.player.blockPosition().offset(4, 0, 4));
            prepare(world, lectern);
            GameTestSupport.waitForServer(context, world, GameTestSupport.scaled(20 * 60), server -> {
                Villager villager = atLectern(server, lectern);
                return villager != null && McBridge.isLibrarian(villager)
                        && villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                        .filter(site -> site.pos().equals(lectern)).isPresent();
            }, "The librarian did not claim its lectern");
            world.getServer().runCommand("attribute @e[type=minecraft:villager,tag=browser_roll,limit=1] minecraft:movement_speed base set 0");
            world.getServer().runCommand("tp @e[type=minecraft:villager,tag=browser_roll,limit=1] "
                    + (lectern.getX() + 0.5) + " " + lectern.getY() + " " + (lectern.getZ() + 1.5) + " 180 0");
            openMerchant(context);
            UUID villagerId = context.computeOnClient(client -> clientVillager(client).getUUID());
            MerchantOffers initial = context.computeOnClient(client ->
                    ((MerchantScreen) McBridge.currentScreen(client)).getMenu().getOffers());
            TradeEntry legacy = context.computeOnClient(client -> TradeCatalog.list(client.level, clientVillager(client), 2)
                    .stream().filter(entry -> entry.icon.is(Items.LANTERN)).findFirst().orElseThrow());
            if (!PickFiles.writePick(LIBRARIAN, legacy.id, legacy.emeraldMax)) {
                throw new AssertionError("Could not write the legacy Apprentice pick");
            }

            click(context, "browser.title");
            assertButton(context, "browser.start", false);
            List<TradeEntry> candidates = context.computeOnClient(client -> TradeSession.entries().stream()
                    .filter(entry -> entry.id.startsWith("enchant:")
                            && initial.stream().noneMatch(offer -> entry.matches(offer, TradeSession.ANY_PRICE))).toList());
            TradeEntry lastPick = candidates.stream().filter(entry -> entry.id.equals("enchant:minecraft:unbreaking:3"))
                    .findFirst().orElseGet(() -> candidates.get(0));
            int cap = lastPick.emeraldMax - 1;
            captureBrowser(context, lastPick, cap);
            context.runOnClient(BrowserHarness::clearPicks);
            if (!ProfessionPicks.load(picksFile).picks(LIBRARIAN).isEmpty()) {
                throw new AssertionError("Removing browser rows did not clear the saved picks");
            }

            Map<String, Integer> expected = new LinkedHashMap<>();
            //? if >=26.3 {
            /*world.getServer().runOnServer(server -> seedNextBook(server, lectern, lastPick, cap));
            *///?} else {
            // Older listing tables use ordinary random draws. Give them several eligible book targets
            // so the test can still require a book match without waiting on one uncommon level.
            context.runOnClient(client -> {
                for (TradeEntry book : candidates.stream().filter(entry -> !entry.id.equals(lastPick.id)).limit(32).toList()) {
                    BrowserHarness.pick(client, book, book.emeraldMax);
                    expected.put(book.id, book.emeraldMax);
                }
            });
            //?}
            expected.put(lastPick.id, cap);
            resize(context, VIEWPORTS.get(0));
            context.runOnClient(client -> BrowserHarness.choose(client, lastPick));
            context.clickScreenButton("−");
            shot(context, "add-and-roll", VIEWPORTS.get(0));
            click(context, "browser.add_roll");
            context.runOnClient(client -> {
                if (!JobSiteReroller.isRunning() || !expected.equals(TradeSession.selections())) {
                    throw new AssertionError("Add & roll did not start with the browser's prices: " + TradeSession.selections()
                            + ", run state " + JobSiteReroller.stopReason());
                }
            });
            if (!ProfessionPicks.load(picksFile).picks(LIBRARIAN).equals(expected)) {
                throw new AssertionError("Add & roll did not persist the active prices");
            }
            waitForMatch(context, world, lectern, villagerId, Map.copyOf(expected));
            GameTestSupport.saveShot(context, "v33-found-merchant-s2");
            context.waitTicks(20 * 6);
            click(context, "browser.title");
            for (Viewport viewport : VIEWPORTS) {
                resize(context, viewport);
                shot(context, "found", viewport);
            }
        } finally {
            context.runOnClient(client -> {
                if (JobSiteReroller.isRunning()) {
                    JobSiteReroller.stop(JobSiteReroller.StopReason.STOPPED);
                }
                config.setEnabled(enabled);
                config.setMaxAttempts(maxAttempts);
                TradeHall.clear();
                TradeSession.simulateClientRestart();
            });
            if (backup == null) {
                Files.deleteIfExists(picksFile);
            } else {
                Files.write(picksFile, backup);
            }
        }
    }

    private static void prepare(TestSingleplayerContext world, BlockPos lectern) {
        int x = lectern.getX();
        int y = lectern.getY();
        int z = lectern.getZ();
        //? if >=26.2 {
        /*world.getServer().runCommand("gamerule send_command_feedback false");
        world.getServer().runCommand("gamerule advance_time false");
        world.getServer().runCommand("gamerule spawn_mobs false");
        *///?} else {
        world.getServer().runCommand("gamerule sendCommandFeedback false");
        world.getServer().runCommand("gamerule doDaylightCycle false");
        world.getServer().runCommand("gamerule doMobSpawning false");
        //?}
        world.getServer().runCommand("time set day");
        world.getServer().runCommand("fill " + (x - 3) + " " + (y - 1) + " " + (z - 3) + " "
                + (x + 3) + " " + (y - 1) + " " + (z + 3) + " minecraft:stone");
        world.getServer().runCommand("setblock " + x + " " + y + " " + z + " minecraft:lectern");
        world.getServer().runCommand("summon minecraft:villager " + (x + 0.5) + " " + y + " " + (z + 1.5)
                + " {PersistenceRequired:1b,Tags:[\"browser_roll\"],CustomName:{text:\"" + NAME + "\"}}");
        world.getServer().runCommand("tp @p " + (x + 1.7) + " " + y + " " + (z - 0.7)
                + " facing " + (x + 0.5) + " " + (y + 1.5) + " " + (z + 1.5));
        world.getServer().runCommand("gamemode survival @p");
        world.getServer().runCommand("clear @p");
        world.getServer().runCommand("give @p minecraft:diamond_axe");
        world.getServer().runCommand("give @p minecraft:lectern 16");
    }

    private static void openMerchant(ClientGameTestContext context) {
        context.waitFor(client -> clientVillager(client) != null && McBridge.isLibrarian(clientVillager(client))
                && McBridge.withinEntity(client.player, clientVillager(client)), GameTestSupport.scaled(20 * 20));
        for (int attempt = 0; attempt < 8; attempt++) {
            context.runOnClient(client -> {
                Villager villager = clientVillager(client);
                client.player.setYRot(28.6F);
                client.player.setXRot(6.0F);
                McBridge.sendLook(client.player, 28.6F, 6.0F);
                LibrarianTracker.remember(villager);
                McBridge.interact(client.gameMode, client.player, villager, InteractionHand.MAIN_HAND);
            });
            context.waitTicks(10);
            if (context.computeOnClient(client -> McBridge.currentScreen(client) instanceof MerchantScreen screen
                    && screen.getMenu().getTraderLevel() == 1 && !screen.getMenu().getOffers().isEmpty())) {
                return;
            }
        }
        throw new AssertionError("The novice librarian's merchant did not open");
    }

    private static void captureBrowser(ClientGameTestContext context, TradeEntry book, int cap) {
        for (int index = 0; index < VIEWPORTS.size(); index++) {
            Viewport viewport = VIEWPORTS.get(index);
            resize(context, viewport);
            context.runOnClient(client -> {
                if (!LIBRARIAN.equals(TradeSession.profession())
                        || BrowserHarness.find(BrowserHarness.browser(client), "Armorer") != null) {
                    throw new AssertionError("The browser did not skip the job step");
                }
            });
            shot(context, "from-villager", viewport);

            var lantern = context.computeOnClient(client -> GuiDraw.bounds(BrowserHarness.find(BrowserHarness.browser(client), "Lantern")));
            context.getInput().setCursorPos((lantern.x() + lantern.width() / 2.0) * viewport.scale(),
                    (lantern.y() + lantern.height() / 2.0) * viewport.scale());
            shot(context, "locked-items", viewport);
            context.runOnClient(client -> BrowserHarness.findItem(client, "Lantern"));
            assertButton(context, "browser.add", false);
            assertButton(context, "browser.add_roll", false);
            shot(context, "locked-reason", viewport);
            click(context, "button.back");
            context.getInput().setCursorPos(2, 2);

            if (index == 0) {
                context.runOnClient(client -> BrowserHarness.pick(client, book, cap));
            }
            assertButton(context, "browser.start", true);
            shot(context, "picks-start", viewport);
        }
    }

    private static void waitForMatch(ClientGameTestContext context, TestSingleplayerContext world, BlockPos lectern,
                                     UUID villagerId, Map<String, Integer> picks) {
        int first = world.getServer().computeOnServer(MinecraftServer::getTickCount);
        int last = first;
        int stalled = 0;
        boolean sawServerAir = false;
        while (last - first < GameTestSupport.scaled(20 * 150) && stalled < GameTestSupport.scaled(20 * 15)) {
            context.waitTick();
            ServerSample sample = world.getServer().computeOnServer(server ->
                    new ServerSample(server.getTickCount(), server.overworld().getBlockState(lectern).isAir()));
            sawServerAir |= sample.air();
            JobSiteReroller.StopReason stop = context.computeOnClient(client ->
                    JobSiteReroller.isRunning() ? null : JobSiteReroller.stopReason());
            if (stop != null) {
                if (stop != JobSiteReroller.StopReason.FOUND || !sawServerAir) {
                    throw new AssertionError("Expected FOUND after a real lectern break, saw " + stop + ", server air=" + sawServerAir);
                }
                boolean matched = world.getServer().computeOnServer(server -> {
                    if (!(server.overworld().getEntity(villagerId) instanceof Villager villager)
                            || !McBridge.isLibrarian(villager) || McBridge.villagerLevel(villager) != 1
                            || !server.overworld().getBlockState(lectern).is(Blocks.LECTERN)) {
                        return false;
                    }
                    return containsPickedBook(villager.getOffers(), picks);
                });
                if (!matched) {
                    throw new AssertionError("FOUND did not correspond to a picked novice book within its cap on the server: " + picks);
                }
                return;
            }
            stalled = last == sample.tick() ? stalled + 1 : 0;
            last = sample.tick();
        }
        throw new AssertionError("Browser reroll did not finish in " + (last - first) + " server ticks");
    }

    private static boolean containsPickedBook(MerchantOffers offers, Map<String, Integer> picks) {
        for (var offer : offers) {
            if (!offer.getCostA().is(Items.EMERALD) || !offer.getResult().is(Items.ENCHANTED_BOOK)) {
                continue;
            }
            var enchantments = offer.getResult().get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && enchantments.entrySet().stream().anyMatch(entry -> {
                String id = "enchant:" + entry.getKey().unwrapKey().orElseThrow().identifier() + ":" + entry.getIntValue();
                return offer.getCostA().getCount() <= picks.getOrDefault(id, 0);
            })) {
                return true;
            }
        }
        return false;
    }

    //? if >=26.3 {
    /*private static void seedNextBook(MinecraftServer server, BlockPos lectern, TradeEntry target, int cap) {
        // Use vanilla's trade generator on an unspawned villager to find a repeatable draw.
        // The test villager's current offers and job memory are untouched.
        Villager trader = atLectern(server, lectern);
        var level = server.overworld();
        var key = trader.getVillagerData().profession().value().getTrades(1);
        var tradeSet = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TRADE_SET)
                .getOrThrow(key).value();
        var sequence = tradeSet.randomSequence().orElseThrow();
        Villager probe = new Villager(net.minecraft.world.entity.EntityTypes.VILLAGER, level);
        probe.setPos(trader.getX(), trader.getY(), trader.getZ());
        probe.setVillagerData(trader.getVillagerData());
        for (long seed = 1; seed <= 512; seed++) {
            server.getRandomSequences().reset(sequence, seed);
            probe.setOffers(null);
            if (containsPickedBook(probe.getOffers(), Map.of(target.id, cap))) {
                server.getRandomSequences().reset(sequence, seed);
                return;
            }
        }
        throw new AssertionError("No vanilla trade seed produced " + target.id + " within " + cap);
    }
    *///?}

    private static Villager atLectern(MinecraftServer server, BlockPos lectern) {
        return server.overworld().getEntitiesOfClass(Villager.class, new AABB(lectern).inflate(8),
                villager -> villager.getName().getString().equals(NAME)).stream().findFirst().orElse(null);
    }

    private static Villager clientVillager(Minecraft client) {
        return McBridge.nearby(client.level, Villager.class, client.player.getBoundingBox().inflate(8)).stream()
                .filter(villager -> villager.getName().getString().equals(NAME)).findFirst().orElse(null);
    }

    private static void assertButton(ClientGameTestContext context, String key, boolean active) {
        context.runOnClient(client -> {
            Button button = BrowserHarness.find(BrowserHarness.browser(client), GuiDraw.message(key).getString());
            if (button == null || button.active != active) {
                throw new AssertionError("Expected " + key + " active=" + active + ", saw " + button);
            }
        });
    }

    private static void click(ClientGameTestContext context, String key) {
        context.clickScreenButton("text.villager_reroller." + key);
    }

    private static void resize(ClientGameTestContext context, Viewport viewport) {
        context.getInput().resizeWindow(viewport.width(), viewport.height());
        context.runOnClient(client -> {
            client.options.guiScale().set(viewport.scale());
            //? if >=26.1 {
            /*client.resizeGui();
            *///?} else {
            client.resizeDisplay();
            //?}
        });
        context.waitFor(client -> client.getWindow().getGuiScale() == viewport.scale()
                && client.getWindow().getWidth() == viewport.width() && client.getWindow().getHeight() == viewport.height(),
                GameTestSupport.scaled(20 * 10));
    }

    private static void shot(ClientGameTestContext context, String name, Viewport viewport) {
        context.waitTicks(5);
        String suffix = viewport.width() == 1920 ? "" : "-" + viewport.width() + "x" + viewport.height();
        GameTestSupport.saveShot(context, "v33-" + name + "-s" + viewport.scale() + suffix);
    }

    private record Viewport(int width, int height, int scale) {
    }

    private record ServerSample(int tick, boolean air) {
    }
}
