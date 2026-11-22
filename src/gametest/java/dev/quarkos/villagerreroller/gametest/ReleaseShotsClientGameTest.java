package dev.quarkos.villagerreroller.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.villagerreroller.client.AutomatorKeys;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.compat.TradeCyclingHook;
import dev.quarkos.villagerreroller.config.ForgeConfigScreen;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.config.VillagerRoster;
import dev.quarkos.villagerreroller.config.VillagerRosterFiles;
import dev.quarkos.villagerreroller.harness.BrowserHarness;
import dev.quarkos.villagerreroller.picker.GuiDraw;
import dev.quarkos.villagerreroller.picker.MyVillagersScreen;
import dev.quarkos.villagerreroller.picker.RerollMethod;
import dev.quarkos.villagerreroller.picker.TradeBrowserScreen;
import dev.quarkos.villagerreroller.picker.TradeHall;
import dev.quarkos.villagerreroller.picker.TradeSession;
import dev.quarkos.villagerreroller.picker.VillagerRosterClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Release listing shots at 1920x1080, GUI scale 2: the empty My villagers list,
 * the settings screen opened with the J key, a list long enough for a scrollbar,
 * and the browser's reroll Method toggle (Trade Cycling).
 */
@SuppressWarnings("UnstableApiUsage")
public final class ReleaseShotsClientGameTest implements FabricClientGameTest {
    private static final String LIBRARIAN = "minecraft:librarian";
    private static final String NOVICE = "Novice";
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int SCALE = 2;
    private static final int SYNC_TICKS = 20 * 20;

    @Override
    public void runTest(ClientGameTestContext context) {
        Path picksFile = PickFiles.configFile();
        Path rosterFile = VillagerRosterFiles.configFile();
        byte[] previousPicks = read(picksFile);
        byte[] previousRoster = read(rosterFile);
        context.runOnClient(client -> {
            TradeHall.clear();
            TradeSession.simulateClientRestart();
            RerollMethod.reset();
            client.options.pauseOnLostFocus = false;
            client.options.renderDistance().set(4);
            client.options.simulationDistance().set(6);
        });
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(settings -> {
                    settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
                    settings.getNormalPresetList().stream()
                            .filter(entry -> entry.preset().is(WorldPresets.FLAT))
                            .findFirst()
                            .ifPresent(settings::setWorldType);
                }).create()) {
            resize(context);
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
            world.getServer().runCommand("time set day");

            // 1. My villagers with nothing saved.
            context.runOnClient(client -> VillagerRosterClient.replace(new VillagerRoster()));
            context.setScreen(() -> new MyVillagersScreen(null));
            waitForScreen(context, MyVillagersScreen.class);
            if (context.computeOnClient(VillagerRosterClient::count) != 0) {
                throw new AssertionError("My villagers was not empty");
            }
            shot(context, "release-my-villagers-empty");
            closeScreen(context);

            // 2. Settings opened by the real J key binding.
            String key = context.computeOnClient(client -> AutomatorKeys.CONFIG.getTranslatedKeyMessage().getString());
            context.getInput().pressKey(AutomatorKeys.CONFIG);
            waitForScreen(context, ForgeConfigScreen.class);
            String title = context.computeOnClient(client -> McBridge.currentScreen(client).getTitle().getString());
            if (!title.contains("Reroll")) {
                throw new AssertionError("Settings title '" + title + "' does not mention Reroll (key " + key + ")");
            }
            System.out.println("[release-shots] settings opened with key " + key + ", title '" + title + "'");
            shot(context, "release-settings-j");
            closeScreen(context);

            // 3. Twelve saved villagers: more rows than fit, so the list shows its scrollbar.
            context.runOnClient(client -> {
                String worldKey = VillagerRosterClient.currentWorldKey(client);
                VillagerRoster roster = new VillagerRoster();
                String[][] rows = {
                        {"Mending", "enchant:minecraft:mending:1", "Mending I", "minecraft:enchanted_book", "10"},
                        {"Unbreaking", "enchant:minecraft:unbreaking:3", "Unbreaking III", "minecraft:enchanted_book", "12"},
                        {"Silk", "enchant:minecraft:silk_touch:1", "Silk Touch I", "minecraft:enchanted_book", "16"},
                        {"Fortune", "enchant:minecraft:fortune:3", "Fortune III", "minecraft:enchanted_book", "14"},
                        {"Looting", "enchant:minecraft:looting:3", "Looting III", "minecraft:enchanted_book", "15"},
                        {"Efficiency", "enchant:minecraft:efficiency:5", "Efficiency V", "minecraft:enchanted_book", "20"},
                        {"Protection", "enchant:minecraft:protection:4", "Protection IV", "minecraft:enchanted_book", "17"},
                        {"Sharpness", "enchant:minecraft:sharpness:5", "Sharpness V", "minecraft:enchanted_book", "21"},
                        {"Feather", "enchant:minecraft:feather_falling:4", "Feather Falling IV", "minecraft:enchanted_book", "13"},
                        {"Power", "enchant:minecraft:power:5", "Power V", "minecraft:enchanted_book", "19"},
                        {"Glass", "buy:minecraft:glass:1:1", "Glass", "minecraft:glass", "1"},
                        {"Name Tag", "buy:minecraft:name_tag:1:20", "Name Tag", "minecraft:name_tag", "20"}};
                for (int i = 0; i < rows.length; i++) {
                    String[] row = rows[i];
                    int cost = Integer.parseInt(row[4]);
                    roster.upsert(worldKey, new VillagerRoster.Entry(String.valueOf(i + 1), "minecraft:overworld",
                            i + 1, 64, 8, row[0], LIBRARIAN, 1 + i % 5, i + 1,
                            List.of(new VillagerRoster.LockedOffer(row[1], row[2], "", 0, row[3], 1, cost, cost, true))));
                }
                VillagerRosterClient.replace(roster);
                Map<String, Integer> wanted = Map.of(
                        "enchant:minecraft:mending:1", 12, "enchant:minecraft:unbreaking:3", 14,
                        "enchant:minecraft:fortune:3", 16, "enchant:minecraft:efficiency:5", 20);
                ProfessionPicks picks = new ProfessionPicks();
                picks.replaceCatalog(LIBRARIAN, wanted.keySet(), wanted);
                picks.save(PickFiles.configFile());
            });
            context.setScreen(() -> new MyVillagersScreen(null));
            waitForScreen(context, MyVillagersScreen.class);
            if (context.computeOnClient(VillagerRosterClient::count) != 12) {
                throw new AssertionError("Expected 12 saved villagers");
            }
            shot(context, "release-scroll-bar");
            closeScreen(context);

            // 4. The browser Method row. Trade Cycling is on the dev runtime (-PwithTradeCycling=true),
            // so the integrated server advertises its channel and the toggle can be switched.
            context.runOnClient(client -> {
                VillagerRosterClient.replace(new VillagerRoster());
                ProfessionPicks picks = new ProfessionPicks();
                Map<String, Integer> wanted = Map.of("enchant:minecraft:mending:1", 18);
                picks.replaceCatalog(LIBRARIAN, wanted.keySet(), wanted);
                picks.save(PickFiles.configFile());
            });
            BlockPos origin = context.computeOnClient(client -> client.player.blockPosition().offset(3, 0, 4));
            setupNovice(world, origin);
            openMerchant(context, world, origin);
            click(context, "browser.title");
            context.runOnClient(client -> BrowserHarness.browser(client));
            waitForScreen(context, TradeBrowserScreen.class);
            boolean installed = context.computeOnClient(client -> TradeCyclingHook.installed());
            if (installed) {
                context.waitFor(client -> TradeCyclingHook.serverSupports(), GameTestSupport.scaled(20 * 10));
            }
            boolean choosable = context.computeOnClient(client -> RerollMethod.canChoose());
            System.out.println("[release-shots] trade cycling installed=" + installed + " choosable=" + choosable);
            if (!installed) {
                throw new AssertionError("Trade Cycling is not on the runtime; run with -PwithTradeCycling=true");
            }
            if (choosable && !context.computeOnClient(client -> RerollMethod.usesTradeCycling())) {
                clickLabel(context, context.computeOnClient(client -> RerollMethod.label().getString()));
            }
            String label = context.computeOnClient(client -> RerollMethod.label().getString());
            System.out.println("[release-shots] method label '" + label + "' usesTradeCycling="
                    + context.computeOnClient(client -> RerollMethod.usesTradeCycling()));
            hover(context, label);
            shot(context, "release-trade-cycling-toggle");
            context.runOnClient(client -> RerollMethod.reset());
            context.runOnClient(client -> client.player.closeContainer());
            waitForScreen(context, null);
        } finally {
            context.runOnClient(client -> {
                TradeHall.clear();
                TradeSession.simulateClientRestart();
                RerollMethod.reset();
                restore(picksFile, previousPicks);
                restore(rosterFile, previousRoster);
                VillagerRosterClient.reload();
            });
        }
    }

    private static void setupNovice(TestSingleplayerContext world, BlockPos origin) {
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();
        world.getServer().runCommand("fill " + (x - 3) + " " + (y - 1) + " " + (z - 4)
                + " " + (x + 3) + " " + (y - 1) + " " + (z + 4) + " minecraft:stone");
        world.getServer().runCommand("setblock " + (x + 1) + " " + y + " " + (z + 1) + " minecraft:lectern");
        world.getServer().runCommand("summon minecraft:villager " + (x + 0.5) + " " + y + " " + (z + 0.5)
                + " {NoAI:1b,PersistenceRequired:1b,CustomName:{text:\"" + NOVICE
                + "\"},VillagerData:{profession:\"minecraft:librarian\",level:1,type:\"minecraft:plains\"}}");
        world.getServer().runOnServer(server -> {
            var level = server.overworld();
            var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
            Villager villager = level.getEntitiesOfClass(Villager.class, new AABB(origin).inflate(8),
                    entity -> entity.getCustomName() != null && NOVICE.equals(entity.getCustomName().getString()))
                    .stream().findFirst().orElseThrow(() -> new AssertionError("Missing the novice librarian"));
            villager.setVillagerXp(0);
            MerchantOffers offers = new MerchantOffers();
            offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 24),
                    EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, 1)), 12, 1, 0F));
            villager.setOffers(offers);
        });
    }

    private static void openMerchant(ClientGameTestContext context, TestSingleplayerContext world, BlockPos origin) {
        double x = origin.getX() + 0.5;
        double z = origin.getZ() - 1.5;
        world.getServer().runCommand("tp @p " + x + " " + origin.getY() + " " + z + " 0 0");
        waitFor(context, "teleport near the novice", client -> {
            Villager villager = named(client, NOVICE);
            return villager != null && client.player != null && client.gameMode != null
                    && client.player.distanceToSqr(x, origin.getY(), z) < 0.25
                    && McBridge.withinEntity(client.player, villager);
        });
        context.runOnClient(client -> {
            Villager villager = named(client, NOVICE);
            LibrarianTracker.remember(villager);
            McBridge.interact(client.gameMode, client.player, villager, InteractionHand.MAIN_HAND);
        });
        waitFor(context, "the novice's merchant screen", client ->
                McBridge.currentScreen(client) instanceof MerchantScreen merchant
                        && merchant.getMenu().getOffers().size() == 1);
        context.waitTicks(GameTestSupport.scaled(5));
    }

    private static void hover(ClientGameTestContext context, String label) {
        double[] point = context.computeOnClient(client -> {
            Button button = findButton(McBridge.currentScreen(client), label);
            if (button == null) {
                throw new AssertionError("No Method button labelled " + label);
            }
            var bounds = GuiDraw.bounds(button);
            double scale = client.getWindow().getGuiScale();
            return new double[]{(bounds.x() + bounds.width() / 2.0) * scale, (bounds.y() + bounds.height() / 2.0) * scale};
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.waitTicks(GameTestSupport.scaled(10));
    }

    private static void closeScreen(ClientGameTestContext context) {
        context.runOnClient(client -> McBridge.setScreen(client, null));
        waitForScreen(context, null);
    }

    private static void click(ClientGameTestContext context, String key) {
        clickLabel(context, context.computeOnClient(client -> GuiDraw.message(key).getString()));
    }

    private static void clickLabel(ClientGameTestContext context, String label) {
        context.runOnClient(client -> {
            Screen screen = McBridge.currentScreen(client);
            Button button = findButton(screen, label);
            if (button == null || !button.active) {
                throw new AssertionError("Could not click " + label + " in " + screen);
            }
            clickWidget(screen, button);
        });
    }

    private static void clickWidget(Screen screen, AbstractWidget widget) {
        var bounds = GuiDraw.bounds(widget);
        var click = new MouseButtonEvent(bounds.x() + bounds.width() / 2.0, bounds.y() + bounds.height() / 2.0,
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        if (!screen.mouseClicked(click, false)) {
            throw new AssertionError("Screen did not handle " + widget.getMessage().getString());
        }
        screen.mouseReleased(click);
    }

    private static Button findButton(Screen screen, String label) {
        if (screen == null) {
            return null;
        }
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.visible && button.getMessage().getString().equals(label)).findFirst().orElse(null);
    }

    private static Villager named(Minecraft client, String name) {
        if (client.level == null) {
            return null;
        }
        for (var entity : client.level.entitiesForRendering()) {
            if (entity instanceof Villager villager && villager.getCustomName() != null
                    && name.equals(villager.getCustomName().getString())) {
                return villager;
            }
        }
        return null;
    }

    private static void resize(ClientGameTestContext context) {
        context.getInput().resizeWindow(WIDTH, HEIGHT);
        context.runOnClient(client -> {
            client.options.guiScale().set(SCALE);
            //? if >=26.1 {
            /*client.resizeGui();
            *///?} else {
            client.resizeDisplay();
            //?}
        });
        waitFor(context, "window " + WIDTH + "x" + HEIGHT + " at scale " + SCALE, ReleaseShotsClientGameTest::viewportOk);
    }

    private static boolean viewportOk(Minecraft client) {
        var window = client.getWindow();
        return window.getWidth() == WIDTH && window.getHeight() == HEIGHT && window.getGuiScale() == SCALE;
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.waitTicks(GameTestSupport.scaled(5));
        if (!context.computeOnClient(ReleaseShotsClientGameTest::viewportOk)) {
            throw new AssertionError("Viewport drifted before " + name);
        }
        GameTestSupport.saveShot(context, name + "-s" + SCALE);
    }

    private static void waitForScreen(ClientGameTestContext context, Class<? extends Screen> expected) {
        waitFor(context, expected == null ? "the world view" : expected.getSimpleName(), client -> {
            Screen screen = McBridge.currentScreen(client);
            return expected == null ? screen == null : expected.isInstance(screen);
        });
    }

    private static void waitFor(ClientGameTestContext context, String description, Predicate<Minecraft> condition) {
        try {
            context.waitFor(condition, GameTestSupport.scaled(SYNC_TICKS));
        } catch (AssertionError timeout) {
            throw new AssertionError("Timed out waiting for " + description, timeout);
        }
    }

    private static byte[] read(Path file) {
        try {
            return Files.exists(file) ? Files.readAllBytes(file) : null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void restore(Path file, byte[] backup) {
        try {
            if (backup == null) {
                Files.deleteIfExists(file);
            } else {
                Files.write(file, backup);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
