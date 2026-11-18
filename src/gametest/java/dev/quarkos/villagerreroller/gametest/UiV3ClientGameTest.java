package dev.quarkos.villagerreroller.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.config.ForgeConfigScreen;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.config.VillagerRoster;
import dev.quarkos.villagerreroller.config.VillagerRosterFiles;
import dev.quarkos.villagerreroller.picker.GuiDraw;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.MyVillagersScreen;
import dev.quarkos.villagerreroller.picker.SearchBox;
import dev.quarkos.villagerreroller.picker.TradeBrowserScreen;
import dev.quarkos.villagerreroller.picker.TradeHall;
import dev.quarkos.villagerreroller.picker.TradeHallScreen;
import dev.quarkos.villagerreroller.picker.TradePickerUi;
import dev.quarkos.villagerreroller.picker.TradeSession;
import dev.quarkos.villagerreroller.picker.UiTheme;
import dev.quarkos.villagerreroller.picker.VillagerRosterClient;
import dev.quarkos.villagerreroller.price.VanillaBookPrices;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
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

@SuppressWarnings("UnstableApiUsage")
public final class UiV3ClientGameTest implements FabricClientGameTest {
    private static final String LIBRARIAN = "minecraft:librarian";
    private static final String MENDING = "enchant:minecraft:mending:1";
    private static final String UNBREAKING = "enchant:minecraft:unbreaking:3";
    private static final String EFFICIENCY = "enchant:minecraft:efficiency:5";
    private static final String SOURCE = "Match";
    private static final String CANDIDATE = "Nearby novice";
    private static final String[] LOCKED = {"Alder", "Birch", "Cedar"};

    @Override
    public void runTest(ClientGameTestContext context) {
        Path picksFile = PickFiles.configFile();
        Path rosterFile = VillagerRosterFiles.configFile();
        byte[] previousPicks = read(picksFile);
        byte[] previousRoster = read(rosterFile);
        ConfigState previous = context.computeOnClient(client -> {
            AutomatorConfig config = AutomatorConfig.get();
            ConfigState saved = new ConfigState(config.enabled(), config.autoHallDiscovery(), config.hallDiscoveryRadius());
            config.setEnabled(true);
            config.setAutoHallDiscovery(true);
            config.setHallDiscoveryRadius(16);
            TradeHall.clear();
            TradeSession.simulateClientRestart();
            client.options.pauseOnLostFocus = false;
            client.options.renderDistance().set(4);
            client.options.simulationDistance().set(6);
            client.options.framerateLimit().set(20);
            return saved;
        });
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(settings -> {
                    settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
                    settings.getNormalPresetList().stream()
                            .filter(entry -> entry.preset().is(WorldPresets.FLAT))
                            .findFirst()
                            .ifPresent(settings::setWorldType);
                }).create()) {
            context.getInput().resizeWindow(1920, 1080);
            scale(context, 2);
            GameTestSupport.waitForChunks(world);
            BlockPos origin = context.computeOnClient(client -> client.player.blockPosition().offset(4, 0, 6));
            Fixture source = new Fixture(SOURCE, origin.getX() + 0.5, origin.getZ() + 2.5, 14, false);
            List<Fixture> librarians = List.of(
                    new Fixture(LOCKED[0], origin.getX() - 3.7, origin.getZ() + 4.5, 5, true),
                    new Fixture(LOCKED[1], origin.getX() - 2.6, origin.getZ() + 4.7, 12, true),
                    new Fixture(LOCKED[2], origin.getX() - 1.5, origin.getZ() + 6.3, 23, true));
            setup(world, origin, source, librarians);
            setupPicks(context);
            for (Fixture librarian : librarians) {
                openMerchant(context, world, origin.getY(), librarian);
                openPanel(context);
                closeMerchant(context);
            }
            context.waitFor(client -> VillagerRosterClient.count(client) == librarians.size(), GameTestSupport.scaled(80));

            openMerchant(context, world, origin.getY(), source);
            openPanel(context);
            context.runOnClient(client -> {
                Villager candidate = named(client, CANDIDATE);
                if (candidate == null || TradeHall.blocker(client, candidate.getId()) != null) {
                    throw new AssertionError("Nearby novice was not ready for Hall discovery");
                }
            });
            // The first menu already has the target. The normal Start action reaches
            // the success callback without relying on a random replacement roll.
            click(context, "button.start");
            context.runOnClient(client -> {
                Villager match = named(client, SOURCE);
                Villager candidate = named(client, CANDIDATE);
                if (JobSiteReroller.stopReason() != JobSiteReroller.StopReason.FOUND || TradeHall.size() != 2
                        || !queued(match) || !queued(candidate)) {
                    throw new AssertionError("Expected the match and the nearby novice to join the Hall");
                }
            });
            click(context, "button.start");
            if (context.computeOnClient(client -> TradeHall.size()) != 2) {
                throw new AssertionError("Repeating the match duplicated queue slots");
            }
            closeMerchant(context);

            for (int scale : new int[]{2, 4}) {
                scale(context, scale);
                openMerchant(context, world, origin.getY(), source);
                openPanel(context);
                captureBrowser(context, scale);
                captureHall(context, scale);
                context.setScreen(() -> new MyVillagersScreen(null));
                shot(context, "my-villagers", scale);
                context.setScreen(() -> new ForgeConfigScreen(null));
                shot(context, "settings", scale);
                context.runOnClient(client -> McBridge.setScreen(client, null));
                context.waitForScreen(null);
            }
        } finally {
            context.runOnClient(client -> {
                TradeHall.clear();
                TradeSession.simulateClientRestart();
                restore(picksFile, previousPicks);
                restore(rosterFile, previousRoster);
                VillagerRosterClient.reload();
                AutomatorConfig config = AutomatorConfig.get();
                config.setEnabled(previous.enabled());
                config.setAutoHallDiscovery(previous.discovery());
                config.setHallDiscoveryRadius(previous.radius());
            });
        }
    }

    private static boolean queued(Villager villager) {
        return villager != null && TradeHall.slots().stream().anyMatch(slot -> slot.villagerId() == villager.getId());
    }

    private static void setupPicks(ClientGameTestContext context) {
        context.runOnClient(client -> {
            VillagerRosterClient.replace(new VillagerRoster());
            Map<String, Integer> wanted = Map.of(MENDING, 18, EFFICIENCY, 35);
            ProfessionPicks picks = new ProfessionPicks();
            picks.replaceCatalog(LIBRARIAN, wanted.keySet(), wanted);
            if (!picks.save(PickFiles.configFile())) {
                throw new AssertionError("Could not write the v3 picks");
            }
        });
    }

    private static void setup(TestSingleplayerContext world, BlockPos origin, Fixture source, List<Fixture> librarians) {
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();
        world.getServer().runCommand("gamerule advance_time false");
        world.getServer().runCommand("gamerule spawn_mobs false");
        world.getServer().runCommand("time set day");
        world.getServer().runCommand("fill " + (x - 4) + " " + (y - 1) + " " + (z - 4)
                + " " + (x + 10) + " " + (y - 1) + " " + (z + 10) + " minecraft:stone");
        world.getServer().runCommand("setblock " + x + " " + y + " " + (z + 3) + " minecraft:lectern");
        world.getServer().runCommand("setblock " + (x + 2) + " " + y + " " + (z + 2) + " minecraft:lectern");
        summon(world, y, source);
        summon(world, y, new Fixture(CANDIDATE, x + 2.4, z + 1.5, 0, false));
        for (Fixture fixture : librarians) {
            summon(world, y, fixture);
        }
        world.getServer().runOnServer(server -> {
            var level = server.overworld();
            var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
            for (Villager villager : level.getEntitiesOfClass(Villager.class, new AABB(origin).inflate(16),
                    entity -> entity.getCustomName() != null)) {
                String name = villager.getCustomName().getString();
                Fixture fixture = SOURCE.equals(name) ? source : librarians.stream()
                        .filter(known -> known.name().equals(name)).findFirst().orElse(null);
                if (fixture == null) {
                    continue;
                }
                ItemStack book = EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, 1));
                MerchantOffers offers = new MerchantOffers();
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, fixture.price()), book, 12, 1, 0F));
                if (fixture.locked()) {
                    villager.setVillagerXp(25);
                    if (LOCKED[0].equals(name)) {
                        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 9), new ItemStack(Items.BOOKSHELF), 12, 1, 0F));
                    } else if (LOCKED[1].equals(name)) {
                        offers.add(new MerchantOffer(new ItemCost(Items.PAPER, 24), new ItemStack(Items.EMERALD), 12, 1, 0F));
                    } else {
                        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(Items.GLASS, 4), 12, 1, 0F));
                    }
                }
                villager.setOffers(offers);
            }
        });
    }

    private static void summon(TestSingleplayerContext world, int y, Fixture fixture) {
        int level = fixture.locked() ? 2 : 1;
        world.getServer().runCommand("summon minecraft:villager " + fixture.x() + " " + y + " " + fixture.z()
                + " {NoAI:1b,PersistenceRequired:1b,CustomName:{text:\"" + fixture.name()
                + "\"},VillagerData:{profession:\"minecraft:librarian\",level:" + level + ",type:\"minecraft:plains\"}}");
    }

    private static void openMerchant(ClientGameTestContext context, TestSingleplayerContext world, int y, Fixture fixture) {
        world.getServer().runCommand("tp @p " + fixture.x() + " " + y + " " + (fixture.z() - 2) + " 0 0");
        context.waitFor(client -> {
            Villager villager = named(client, fixture.name());
            return villager != null && McBridge.withinEntity(client.player, villager);
        }, GameTestSupport.scaled(80));
        for (int attempt = 0; attempt < GameTestSupport.scaled(12); attempt++) {
            boolean open = context.computeOnClient(client -> {
                if (McBridge.currentScreen(client) instanceof MerchantScreen merchant) {
                    return !merchant.getMenu().getOffers().isEmpty();
                }
                Villager villager = named(client, fixture.name());
                if (villager != null && McBridge.withinEntity(client.player, villager)) {
                    LibrarianTracker.remember(villager);
                    McBridge.interact(client.gameMode, client.player, villager, InteractionHand.MAIN_HAND);
                }
                return false;
            });
            if (open) {
                return;
            }
            context.waitTicks(4);
        }
        throw new AssertionError("Could not open offers for " + fixture.name());
    }

    private static void openPanel(ClientGameTestContext context) {
        if (!context.computeOnClient(client -> TradePickerUi.panelShown())) {
            click(context, "button.offers");
        }
        if (!context.computeOnClient(client -> TradePickerUi.panelShown())) {
            throw new AssertionError("Trade panel did not open");
        }
    }

    private static void closeMerchant(ClientGameTestContext context) {
        context.runOnClient(client -> client.player.closeContainer());
        context.waitForScreen(null);
    }

    private static void captureBrowser(ClientGameTestContext context, int scale) {
        shot(context, "trade-panel", scale);
        click(context, "button.browse");
        context.waitForScreen(TradeBrowserScreen.class);
        assertButton(context, "Armorer", true);
        assertButton(context, "Weaponsmith", true);
        shot(context, "browser-job", scale);
        clickLabel(context, "Librarian");
        shot(context, "browser-item", scale);
        clickLabel(context, "Enchanted Book");
        context.runOnClient(client -> McBridge.currentScreen(client).children().stream()
                .filter(SearchBox.class::isInstance).map(SearchBox.class::cast).findFirst().orElseThrow().setValue("Unbreaking"));
        shot(context, "browser-level", scale);
        clickLabel(context, "Unbreaking III");
        shot(context, "browser-price", scale);
        if (scale == 2) {
            context.getInput().holdKey(InputConstants.KEY_LSHIFT);
            try {
                clickLabel(context, "−");
            } finally {
                context.getInput().releaseKey(InputConstants.KEY_LSHIFT);
            }
            click(context, "browser.add");
            assertPick(UNBREAKING, VanillaBookPrices.max(3, false) - 5);
        } else {
            click(context, "any");
            click(context, "browser.save_price");
            assertPick(UNBREAKING, TradeSession.ANY_PRICE);
        }
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        assertButton(context, "Unbreaking III", true);
        for (int i = 0; i < 3; i++) {
            click(context, "button.back");
        }
        context.waitForScreen(MerchantScreen.class);
        if (!context.computeOnClient(client -> TradeSession.selected(UNBREAKING))) {
            throw new AssertionError("The quick panel did not load the browser pick");
        }
    }

    private static void captureHall(ClientGameTestContext context, int scale) {
        clickLabel(context, context.computeOnClient(client -> GuiDraw.message("button.hall", TradeHall.size()).getString()));
        context.waitForScreen(TradeHallScreen.class);
        shot(context, "hall-all", scale);
        click(context, "hall.filter.available");
        assertButton(context, "Mending", true);
        assertButton(context, "Efficiency V", false);
        shot(context, "hall-available", scale);
        click(context, "hall.filter.missing");
        assertButton(context, "Efficiency V", true);
        assertButton(context, "Mending", false);
        shot(context, "hall-missing", scale);

        click(context, "hall.filter.available");
        clickLabel(context, GuiDraw.message("hall.want_trade", "Bookshelf").getString());
        if (ProfessionPicks.load(PickFiles.configFile()).picks(LIBRARIAN).size() != 4) {
            throw new AssertionError("Hall tile checkbox did not save its want");
        }
        clickLabel(context, GuiDraw.message("hall.unwant_trade", "Bookshelf").getString());
        if (ProfessionPicks.load(PickFiles.configFile()).picks(LIBRARIAN).size() != 3) {
            throw new AssertionError("Hall tile checkbox did not clear its want");
        }

        click(context, "hall.queue");
        shot(context, "hall-queue", scale);
        click(context, "hall.trades");
        click(context, "hall.filter.available");
        click(context, "button.browse");
        context.waitForScreen(TradeBrowserScreen.class);
        click(context, "button.back");
        context.waitForScreen(TradeHallScreen.class);
        clickLabel(context, "Mending");
        context.runOnClient(client -> {
            Villager cheapest = named(client, LOCKED[0]);
            Villager mostExpensive = named(client, LOCKED[2]);
            for (String name : List.of(SOURCE, LOCKED[0], LOCKED[1], LOCKED[2])) {
                Villager villager = named(client, name);
                if (villager == null || !client.shouldEntityAppearGlowing(villager)) {
                    throw new AssertionError("Missing Hall outline for " + name);
                }
            }
            if (cheapest.getTeamColor() != (UiTheme.GREEN & 0xFFFFFF)
                    || mostExpensive.getTeamColor() != (UiTheme.RED & 0xFFFFFF)) {
                throw new AssertionError("Hall outlines were not ranked by price");
            }
        });
        shot(context, "hall-selected", scale);
        clickLabel(context, "Mending");
        context.runOnClient(client -> {
            if (client.shouldEntityAppearGlowing(named(client, LOCKED[0]))) {
                throw new AssertionError("Clicking the selected tile did not clear its outlines");
            }
        });
        clickLabel(context, "Mending");
        click(context, "hall.view_world");
        context.waitForScreen(null);
        shot(context, "hall-highlight-world", scale);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(null);
        context.runOnClient(client -> {
            Villager villager = named(client, LOCKED[0]);
            if (villager == null || client.shouldEntityAppearGlowing(villager)) {
                throw new AssertionError("Esc did not clear the Hall outlines");
            }
        });
    }

    private static void assertPick(String trade, int expected) {
        int actual = ProfessionPicks.load(PickFiles.configFile()).picks(LIBRARIAN).getOrDefault(trade, 0);
        if (actual != expected) {
            throw new AssertionError(trade + " had ceiling " + actual + ", expected " + expected);
        }
    }

    private static void assertButton(ClientGameTestContext context, String label, boolean visible) {
        boolean found = context.computeOnClient(client -> findButton(McBridge.currentScreen(client), label) != null);
        if (found != visible) {
            throw new AssertionError("Unexpected visibility for " + label + ": " + found);
        }
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
            var bounds = GuiDraw.bounds(button);
            var click = new MouseButtonEvent(bounds.x() + bounds.width() / 2.0, bounds.y() + bounds.height() / 2.0,
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            if (!screen.mouseClicked(click, false)) {
                throw new AssertionError("Screen did not handle " + label);
            }
            screen.mouseReleased(click);
        });
    }

    private static Button findButton(Screen screen, String label) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.visible && button.getMessage().getString().equals(label)).findFirst().orElse(null);
    }

    private static Villager named(Minecraft client, String name) {
        for (var entity : client.level.entitiesForRendering()) {
            if (entity instanceof Villager villager && villager.getCustomName() != null
                    && name.equals(villager.getCustomName().getString())) {
                return villager;
            }
        }
        return null;
    }

    private static void scale(ClientGameTestContext context, int scale) {
        context.runOnClient(client -> {
            client.options.guiScale().set(scale);
            //? if >=26.1 {
            /*client.resizeGui();
            *///?} else {
            client.resizeDisplay();
            //?}
        });
        context.waitFor(client -> client.getWindow().getWidth() == 1920 && client.getWindow().getHeight() == 1080
                && client.getWindow().getGuiScale() == scale, GameTestSupport.scaled(60));
    }

    private static void shot(ClientGameTestContext context, String name, int scale) {
        context.waitTicks(3);
        context.runOnClient(client -> {
            var window = client.getWindow();
            if (window.getWidth() != 1920 || window.getHeight() != 1080 || window.getGuiScale() != scale) {
                throw new AssertionError("Wrong window or GUI scale for " + name);
            }
        });
        GameTestSupport.saveShot(context, "v3-" + name + "-s" + scale);
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

    private record Fixture(String name, double x, double z, int price, boolean locked) {
    }

    private record ConfigState(boolean enabled, boolean discovery, int radius) {
    }
}
