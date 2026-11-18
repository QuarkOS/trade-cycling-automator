package dev.quarkos.villagerreroller.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.config.AutomatorConfig.HallView;
import dev.quarkos.villagerreroller.config.ForgeConfigScreen;
import dev.quarkos.villagerreroller.config.PickFiles;
import dev.quarkos.villagerreroller.config.ProfessionPicks;
import dev.quarkos.villagerreroller.config.VillagerRoster;
import dev.quarkos.villagerreroller.config.VillagerRosterFiles;
import dev.quarkos.villagerreroller.picker.GuiDraw;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.MerchantLayout;
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
import net.minecraft.world.inventory.MerchantMenu;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static dev.quarkos.villagerreroller.picker.UiLayout.GAP;

@SuppressWarnings("UnstableApiUsage")
public final class UiV3ClientGameTest implements FabricClientGameTest {
    private static final String LIBRARIAN = "minecraft:librarian";
    private static final String MENDING = "enchant:minecraft:mending:1";
    private static final String MENDING_NAME = "Mending I";
    private static final String UNBREAKING = "enchant:minecraft:unbreaking:3";
    private static final String EFFICIENCY = "enchant:minecraft:efficiency:5";
    private static final String SOURCE = "Match";
    private static final String CANDIDATE = "Nearby novice";
    private static final String[] LOCKED = {"Alder", "Birch", "Cedar"};
    private static final int SYNC_TICKS = 20 * 20;

    private final Map<String, MenuState> observedMenus = new LinkedHashMap<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        Path picksFile = PickFiles.configFile();
        Path rosterFile = VillagerRosterFiles.configFile();
        Path configFile = automatorConfigFile();
        byte[] previousPicks = read(picksFile);
        byte[] previousRoster = read(rosterFile);
        byte[] previousConfig = read(configFile);
        ConfigState previous = context.computeOnClient(client -> {
            observedMenus.clear();
            AutomatorConfig config = AutomatorConfig.get();
            ConfigState saved = new ConfigState(config.enabled(), config.autoHallDiscovery(), config.hallDiscoveryRadius(),
                    config.hallView());
            config.setEnabled(true);
            config.setAutoHallDiscovery(true);
            config.setHallDiscoveryRadius(16);
            config.setHallView(HallView.GRID);
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
            resize(context, new Viewport(1920, 1080, 2));
            try {
                GameTestSupport.waitForChunks(world);
            } catch (AssertionError timeout) {
                throw waitFailure(context, "chunks to render", timeout);
            }
            BlockPos origin = context.computeOnClient(client -> client.player.blockPosition().offset(4, 0, 6));
            Fixture source = new Fixture(SOURCE, origin.getX() + 0.5, origin.getZ() + 2.5, 14, false);
            List<Fixture> librarians = List.of(
                    new Fixture(LOCKED[0], origin.getX() - 3.7, origin.getZ() + 4.5, 5, true),
                    new Fixture(LOCKED[1], origin.getX() - 2.6, origin.getZ() + 4.7, 12, true),
                    new Fixture(LOCKED[2], origin.getX() - 1.5, origin.getZ() + 6.3, 23, true));
            setup(world, origin, source, librarians);
            setupPicks(context);
            for (int i = 0; i < librarians.size(); i++) {
                Fixture librarian = librarians.get(i);
                openMerchant(context, world, origin.getY(), librarian);
                if (i == 0) {
                    openPanel(context);
                } else {
                    waitFor(context, "the panel to reopen automatically for " + librarian.name(),
                            client -> TradePickerUi.panelShown());
                }
                waitFor(context, librarian.name() + " to be saved with all offers", client -> recorded(client, librarian));
                closeMerchant(context);
            }
            waitFor(context, librarians.size() + " locked villagers in the roster",
                    client -> VillagerRosterClient.count(client) == librarians.size());

            openMerchant(context, world, origin.getY(), source);
            openPanel(context);
            waitFor(context, "nearby novice and its job site in reach", client -> {
                Villager candidate = named(client, CANDIDATE);
                return candidate != null && TradeHall.blocker(client, candidate.getId()) == null;
            });
            waitFor(context, "the match's saved target and enabled Start button", client -> {
                Button start = findButton(McBridge.currentScreen(client), GuiDraw.message("button.start").getString());
                return TradeSession.selected(MENDING) && start != null && start.active;
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
                Viewport viewport = new Viewport(1920, 1080, scale);
                resize(context, viewport);
                openMerchant(context, world, origin.getY(), source);
                openPanel(context);
                assertMerchantLayout(context, false);
                captureBrowser(context, viewport);
                captureHall(context, viewport);
                context.setScreen(() -> new MyVillagersScreen(null));
                shot(context, "my-villagers", viewport);
                context.setScreen(() -> new ForgeConfigScreen(null));
                shot(context, "settings", viewport);
                context.runOnClient(client -> McBridge.setScreen(client, null));
                waitForScreen(context, null);
            }
            // Vanilla caps the GUI scale at window height / 240, so 1280x720 cannot reach scale 4.
            // 1280x960 at scale 4 (320x240 GUI) is the narrowest layout that exists.
            captureCompact(context, world, origin.getY(), source, new Viewport(1280, 960, 4));
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
                config.setHallView(previous.view());
                restore(configFile, previousConfig);
            });
        }
    }

    private static boolean queued(Villager villager) {
        return villager != null && TradeHall.slots().stream().anyMatch(slot -> slot.villagerId() == villager.getId());
    }

    private static boolean recorded(Minecraft client, Fixture fixture) {
        Villager villager = named(client, fixture.name());
        String worldKey = VillagerRosterClient.currentWorldKey(client);
        return villager != null && VillagerRosterClient.roster().entries(worldKey).stream().anyMatch(entry ->
                entry.uuid().equals(villager.getUUID().toString()) && entry.level() == fixture.level()
                        && entry.trades().size() == fixture.offerCount());
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
        List<Fixture> fixtures = new ArrayList<>(librarians);
        fixtures.add(source);
        world.getServer().runOnServer(server -> {
            var level = server.overworld();
            var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
            var villagers = level.getEntitiesOfClass(Villager.class, new AABB(origin).inflate(16),
                    entity -> entity.getCustomName() != null);
            for (Fixture fixture : fixtures) {
                Villager villager = villagers.stream()
                        .filter(entity -> fixture.name().equals(entity.getCustomName().getString()))
                        .findFirst().orElseThrow(() -> new AssertionError("Missing server fixture " + fixture.name()));
                villager.setVillagerData(villager.getVillagerData().withLevel(fixture.level()));
                villager.setVillagerXp(fixture.xp());
                ItemStack book = EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, 1));
                MerchantOffers offers = new MerchantOffers();
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, fixture.price()), book, 12, 1, 0F));
                if (fixture.locked()) {
                    if (LOCKED[0].equals(fixture.name())) {
                        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 9), new ItemStack(Items.BOOKSHELF), 12, 1, 0F));
                    } else if (LOCKED[1].equals(fixture.name())) {
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
        world.getServer().runCommand("summon minecraft:villager " + fixture.x() + " " + y + " " + fixture.z()
                + " {NoAI:1b,PersistenceRequired:1b,CustomName:{text:\"" + fixture.name()
                + "\"},VillagerData:{profession:\"minecraft:librarian\",level:" + fixture.level() + ",type:\"minecraft:plains\"}}");
    }

    private void openMerchant(ClientGameTestContext context, TestSingleplayerContext world, int y, Fixture fixture) {
        try {
            GameTestSupport.waitForServer(context, world, GameTestSupport.scaled(SYNC_TICKS),
                    server -> server.getPlayerList().getPlayers().stream()
                            .anyMatch(player -> player.containerMenu == player.inventoryMenu),
                    "Waiting for the player's inventory menu");
        } catch (AssertionError timeout) {
            throw waitFailure(context, "server to close the prior merchant before opening " + fixture.name(), timeout);
        }
        double z = fixture.z() - 2;
        world.getServer().runCommand("tp @p " + fixture.x() + " " + y + " " + z + " 0 0");
        waitFor(context, "teleport near " + fixture.name(), client -> {
            Villager villager = named(client, fixture.name());
            return villager != null && client.player != null && client.gameMode != null
                    && client.player.distanceToSqr(fixture.x(), y, z) < 0.25
                    && McBridge.withinEntity(client.player, villager);
        });
        context.runOnClient(client -> {
            Villager villager = named(client, fixture.name());
            LibrarianTracker.remember(villager);
            McBridge.interact(client.gameMode, client.player, villager, InteractionHand.MAIN_HAND);
        });
        waitFor(context, "synced offers for " + fixture.name() + " (xp=" + fixture.xp()
                + ", level=" + fixture.level() + ", offers=" + fixture.offerCount() + ")", client -> {
            if (!(McBridge.currentScreen(client) instanceof MerchantScreen merchant)
                    || !fixture.name().equals(merchant.getTitle().getString())) {
                return false;
            }
            MenuState state = MenuState.read(merchant.getMenu());
            observedMenus.put(fixture.name(), state);
            Villager villager = named(client, fixture.name());
            return villager != null && LibrarianTracker.current(client) == villager
                    && client.player != null && client.player.containerMenu == merchant.getMenu()
                    && state.xp() == fixture.xp() && state.level() == fixture.level()
                    && state.offers() == fixture.offerCount() && state.locked() == fixture.locked();
        });
    }

    private static void openPanel(ClientGameTestContext context) {
        if (!context.computeOnClient(client -> TradePickerUi.panelShown())) {
            click(context, "button.offers");
        }
        if (!context.computeOnClient(client -> TradePickerUi.panelShown())) {
            throw new AssertionError("Trade panel did not open");
        }
    }

    private void closeMerchant(ClientGameTestContext context) {
        context.runOnClient(client -> client.player.closeContainer());
        waitForScreen(context, null);
    }

    private void captureBrowser(ClientGameTestContext context, Viewport viewport) {
        shot(context, "trade-panel", viewport);
        click(context, "button.browse");
        waitForScreen(context, TradeBrowserScreen.class);
        assertButton(context, "Armorer", true);
        assertButton(context, "Weaponsmith", true);
        shot(context, "browser-job", viewport);
        clickLabel(context, "Librarian");
        assertBrowserItemLayout(context);
        shot(context, "browser-item", viewport);
        clickLabel(context, "Enchanted Book");
        context.runOnClient(client -> McBridge.currentScreen(client).children().stream()
                .filter(SearchBox.class::isInstance).map(SearchBox.class::cast).findFirst().orElseThrow().setValue("Unbreaking"));
        shot(context, "browser-level", viewport);
        clickLabel(context, "Unbreaking III");
        shot(context, "browser-price", viewport);
        if (viewport.guiScale() == 2) {
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
        waitFor(context, "the browser's level grid after Esc",
                client -> findButton(McBridge.currentScreen(client), "Unbreaking III") != null);
        for (int i = 0; i < 3; i++) {
            click(context, "button.back");
        }
        waitForScreen(context, MerchantScreen.class);
        if (!context.computeOnClient(client -> TradeSession.selected(UNBREAKING))) {
            throw new AssertionError("The quick panel did not load the browser pick");
        }
    }

    private void captureHall(ClientGameTestContext context, Viewport viewport) {
        openHall(context);
        assertHallView(context, HallView.GRID);
        shot(context, "hall-all", viewport);
        click(context, "hall.filter.available");
        assertButton(context, MENDING_NAME, true);
        assertButton(context, "Efficiency V", false);
        shot(context, "hall-available", viewport);
        click(context, "hall.filter.missing");
        assertButton(context, "Efficiency V", true);
        assertButton(context, MENDING_NAME, false);
        shot(context, "hall-missing", viewport);

        click(context, "hall.filter.available");
        assertBookshelfWantedRoundTrip(context);

        click(context, "hall.queue");
        shot(context, "hall-queue", viewport);
        click(context, "hall.trades");
        click(context, "hall.filter.available");
        click(context, "button.browse");
        waitForScreen(context, TradeBrowserScreen.class);
        click(context, "button.back");
        waitForScreen(context, TradeHallScreen.class);
        Map<String, Integer> gridHighlights = captureGridSelection(context, viewport);
        captureHallSearchAndList(context, viewport, gridHighlights);
        clickLabel(context, MENDING_NAME);
        click(context, "hall.view_world");
        waitForScreen(context, null);
        shot(context, "hall-highlight-world", viewport);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        waitFor(context, "Esc to clear the Hall outlines", client -> {
            Villager villager = named(client, LOCKED[0]);
            return McBridge.currentScreen(client) == null && villager != null && !client.shouldEntityAppearGlowing(villager);
        });
    }

    private Map<String, Integer> captureGridSelection(ClientGameTestContext context, Viewport viewport) {
        Map<String, Integer> expected = Map.of(LOCKED[0], UiTheme.GREEN & 0xFFFFFF, LOCKED[1], UiTheme.YELLOW & 0xFFFFFF,
                SOURCE, UiTheme.AMBER & 0xFFFFFF, LOCKED[2], UiTheme.RED & 0xFFFFFF);
        waitFor(context, "all Hall outline fixtures to be loaded",
                client -> expected.keySet().stream().allMatch(name -> named(client, name) != null));
        clickLabel(context, MENDING_NAME);
        Map<String, Integer> highlights = hallHighlights(context);
        if (!highlights.equals(expected)) {
            throw new AssertionError("Expected price-ranked Hall outlines " + expected + ", saw " + highlights);
        }
        shot(context, "hall-selected", viewport);
        clickLabel(context, MENDING_NAME);
        assertNoHallHighlights(context);
        return highlights;
    }

    private void captureHallSearchAndList(ClientGameTestContext context, Viewport viewport, Map<String, Integer> gridHighlights) {
        click(context, "hall.filter.all");
        TradeHallScreen hall = focusHallSearch(context);
        context.getInput().pressKey(InputConstants.KEY_E);
        context.getInput().typeChar('e');
        assertHallSearch(context, hall, "e", true);
        context.getInput().pressKey(InputConstants.KEY_T);
        context.getInput().typeChar('t');
        assertHallSearch(context, hall, "et", true);

        replaceHallQuery(context, "et", "mend");
        context.getInput().pressKey(InputConstants.KEY_RETURN);
        assertHallSearch(context, hall, "mend", true);
        assertHallTrades(context, List.of(MENDING_NAME));
        shot(context, "hall-search", viewport);
        click(context, "hall.filter.missing");
        assertHallTrades(context, List.of());
        click(context, "hall.filter.all");
        assertHallTrades(context, List.of(MENDING_NAME));

        focusHallSearch(context);
        String noMatch = "no such enchantment";
        replaceHallQuery(context, "mend", noMatch);
        assertHallSearch(context, hall, noMatch, true);
        assertHallTrades(context, List.of());
        shot(context, "hall-search-empty", viewport);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        assertHallSearch(context, hall, noMatch, false);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        waitForScreen(context, MerchantScreen.class);
        openHall(context);

        click(context, "hall.list_view");
        assertHallView(context, HallView.LIST);
        assertSavedHallView(HallView.LIST);
        shot(context, "hall-list", viewport);
        assertListScroll(context);
        assertBookshelfWantedRoundTrip(context);
        clickLabel(context, MENDING_NAME);
        Map<String, Integer> listHighlights = hallHighlights(context);
        if (!listHighlights.equals(gridHighlights)) {
            throw new AssertionError("List selection showed " + listHighlights + ", grid selection showed " + gridHighlights);
        }
        shot(context, "hall-list-selected", viewport);
        clickLabel(context, MENDING_NAME);
        assertNoHallHighlights(context);

        click(context, "button.back");
        waitForScreen(context, MerchantScreen.class);
        openHall(context);
        assertHallView(context, HallView.LIST);
        click(context, "hall.grid_view");
        assertHallView(context, HallView.GRID);
    }

    private void captureCompact(ClientGameTestContext context, TestSingleplayerContext world, int y,
                                Fixture source, Viewport viewport) {
        resize(context, viewport);
        openMerchant(context, world, y, source);
        openPanel(context);
        assertMerchantLayout(context, true);
        shot(context, "trade-panel", viewport);
        click(context, "button.back");
        context.runOnClient(client -> {
            MerchantScreen screen = (MerchantScreen) McBridge.currentScreen(client);
            int left = McBridge.merchantLeft(screen);
            if (TradePickerUi.panelShown() || left < 0 || left + MerchantLayout.MERCHANT_WIDTH > screen.width) {
                throw new AssertionError("Back did not restore the merchant at " + viewport);
            }
        });
        openPanel(context);
        click(context, "button.browse");
        waitForScreen(context, TradeBrowserScreen.class);
        clickLabel(context, "Librarian");
        assertBrowserItemLayout(context);
        click(context, "button.back");
        click(context, "button.back");
        waitForScreen(context, MerchantScreen.class);
        openHall(context);
        shot(context, "hall-all", viewport);
        Map<String, Integer> gridHighlights = captureGridSelection(context, viewport);
        captureHallSearchAndList(context, viewport, gridHighlights);
        click(context, "hall.queue");
        shot(context, "hall-queue", viewport);
        click(context, "button.back");
        waitForScreen(context, MerchantScreen.class);
        closeMerchant(context);
    }

    private void openHall(ClientGameTestContext context) {
        clickLabel(context, context.computeOnClient(client -> GuiDraw.message("button.hall", TradeHall.size()).getString()));
        waitForScreen(context, TradeHallScreen.class);
    }

    private static TradeHallScreen focusHallSearch(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            TradeHallScreen hall = (TradeHallScreen) McBridge.currentScreen(client);
            clickWidget(hall, hallSearch(hall));
            return hall;
        });
    }

    private static SearchBox hallSearch(TradeHallScreen hall) {
        return hall.children().stream().filter(SearchBox.class::isInstance).map(SearchBox.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("Hall Trades had no search box"));
    }

    private static void replaceHallQuery(ClientGameTestContext context, String previous, String query) {
        var input = context.getInput();
        input.pressKey(InputConstants.KEY_END);
        for (int i = 0; i < previous.length(); i++) {
            input.pressKey(InputConstants.KEY_BACKSPACE);
        }
        input.typeChars(query);
    }

    private static void assertHallSearch(ClientGameTestContext context, TradeHallScreen expected, String query, boolean focused) {
        context.runOnClient(client -> {
            Screen screen = McBridge.currentScreen(client);
            if (screen != expected) {
                throw new AssertionError("Typing in Hall search opened " + screen + " instead of staying in the Hall");
            }
            SearchBox search = hallSearch(expected);
            if (!search.getValue().equals(query) || search.isFocused() != focused) {
                throw new AssertionError("Expected Hall query " + query + " with focused=" + focused
                        + ", saw " + search.getValue() + " with focused=" + search.isFocused());
            }
        });
    }

    private static void assertHallTrades(ClientGameTestContext context, List<String> expected) {
        context.runOnClient(client -> {
            Screen hall = McBridge.currentScreen(client);
            List<String> visible = hall.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(button -> button.visible).map(button -> button.getMessage().getString()).toList();
            // Both layouts pair each visible trade button with its wanted marker.
            List<String> trades = visible.stream().filter(label ->
                    visible.contains(GuiDraw.message("hall.want_trade", label).getString())
                            || visible.contains(GuiDraw.message("hall.unwant_trade", label).getString())).toList();
            if (!trades.equals(expected)) {
                throw new AssertionError("Expected Hall trade results " + expected + ", saw " + trades);
            }
        });
    }

    private static void assertHallView(ClientGameTestContext context, HallView expected) {
        context.runOnClient(client -> {
            Button mending = findButton(McBridge.currentScreen(client), MENDING_NAME);
            if (mending == null || (mending.getWidth() > mending.getHeight()) != (expected == HallView.LIST)) {
                throw new AssertionError("Mending did not have a " + expected + " button in the Hall");
            }
        });
    }

    private static Path automatorConfigFile() {
        return PickFiles.configFile().resolveSibling("villager_reroller.json");
    }

    private static void assertSavedHallView(HallView expected) {
        try (var reader = Files.newBufferedReader(automatorConfigFile())) {
            HallView saved = AutomatorConfig.parse(reader).hallView();
            if (saved != expected) {
                throw new AssertionError("Expected saved Hall view " + expected + ", saw " + saved);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void assertListScroll(ClientGameTestContext context) {
        context.runOnClient(client -> {
            Screen hall = McBridge.currentScreen(client);
            Button first = findButton(hall, "Efficiency V");
            if (first == null) {
                throw new AssertionError("Expected Efficiency V at the top of the Hall list");
            }
            var bounds = GuiDraw.bounds(first);
            if (!hall.mouseScrolled(bounds.x() + GAP, bounds.y() + GAP, 0, -1)
                    || findButton(hall, "Efficiency V") != null) {
                throw new AssertionError("Scrolling down did not move the first Hall row out of view");
            }
            if (!hall.mouseScrolled(bounds.x() + GAP, bounds.y() + GAP, 0, 1)
                    || findButton(hall, "Efficiency V") == null) {
                throw new AssertionError("Scrolling up did not restore the first Hall row");
            }

            Button mark = findButton(hall, GuiDraw.message("hall.unwant_trade", "Efficiency V").getString());
            if (mark == null) {
                throw new AssertionError("The first Hall row had no wanted marker");
            }
            double thumbX = GuiDraw.bounds(mark).right() + 3;
            double thumbY = bounds.y() + GAP;
            MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
            var grab = new MouseButtonEvent(thumbX, thumbY, left);
            var bottom = new MouseButtonEvent(thumbX, hall.height - 1, left);
            if (!hall.mouseClicked(grab, false) || !hall.mouseDragged(bottom, 0, bottom.y() - grab.y())
                    || findButton(hall, "Efficiency V") != null) {
                throw new AssertionError("Dragging the Hall scrollbar down did not move the list");
            }
            hall.mouseDragged(grab, 0, grab.y() - bottom.y());
            hall.mouseReleased(grab);
            if (findButton(hall, "Efficiency V") == null) {
                throw new AssertionError("Dragging the Hall scrollbar back did not restore the first row");
            }
        });
    }

    private static void assertBookshelfWantedRoundTrip(ClientGameTestContext context) {
        clickLabel(context, GuiDraw.message("hall.want_trade", "Bookshelf").getString());
        if (ProfessionPicks.load(PickFiles.configFile()).picks(LIBRARIAN).size() != 4) {
            throw new AssertionError("Hall marker did not save the Bookshelf target");
        }
        clickLabel(context, GuiDraw.message("hall.unwant_trade", "Bookshelf").getString());
        if (ProfessionPicks.load(PickFiles.configFile()).picks(LIBRARIAN).size() != 3) {
            throw new AssertionError("Hall marker did not clear the Bookshelf target");
        }
    }

    private static Map<String, Integer> hallHighlights(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            Map<String, Integer> colors = new LinkedHashMap<>();
            for (String name : List.of(LOCKED[0], LOCKED[1], SOURCE, LOCKED[2])) {
                Villager villager = named(client, name);
                if (villager != null && client.shouldEntityAppearGlowing(villager)) {
                    colors.put(name, villager.getTeamColor());
                }
            }
            return colors;
        });
    }

    private static void assertNoHallHighlights(ClientGameTestContext context) {
        Map<String, Integer> highlights = hallHighlights(context);
        if (!highlights.isEmpty()) {
            throw new AssertionError("Clicking the already-selected trade left outlines " + highlights);
        }
    }

    private static void assertBrowserItemLayout(ClientGameTestContext context) {
        context.runOnClient(client -> {
            Screen browser = McBridge.currentScreen(client);
            Button book = findButton(browser, "Enchanted Book");
            if (book == null) {
                throw new AssertionError("The book slot was missing from the browser item grid");
            }
            var bounds = GuiDraw.bounds(book);
            if (bounds.width() < 24 || bounds.width() > 28 || bounds.width() != bounds.height()
                    || bounds.x() < 0 || bounds.right() > browser.width || bounds.y() < 0 || bounds.bottom() > browser.height) {
                throw new AssertionError("Unexpected browser item slot " + bounds + " at " + currentViewport(client));
            }
        });
    }

    private static void assertMerchantLayout(ClientGameTestContext context, boolean compact) {
        context.runOnClient(client -> {
            MerchantScreen screen = (MerchantScreen) McBridge.currentScreen(client);
            int merchantLeft = McBridge.merchantLeft(screen);
            if (TradePickerUi.compactShown() != compact || (compact && merchantLeft < screen.width)) {
                throw new AssertionError("Unexpected merchant layout at " + screen.width + "x" + screen.height);
            }
            for (String label : List.of(GuiDraw.message("button.queue").getString(),
                    GuiDraw.message("button.hall", TradeHall.size()).getString(),
                    GuiDraw.message("button.list").getString())) {
                Button button = findButton(screen, label);
                if (button == null) {
                    throw new AssertionError("Missing merchant navigation button " + label);
                }
                var bounds = GuiDraw.bounds(button);
                if (client.font.width(label) > bounds.width() - 2 * GAP || bounds.x() < 0 || bounds.right() > screen.width
                        || bounds.y() < 0 || bounds.bottom() > screen.height
                        || (!compact && bounds.x() < merchantLeft + MerchantLayout.MERCHANT_WIDTH)) {
                    throw new AssertionError("Merchant navigation button clipped or overlapped: " + label + " " + bounds);
                }
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

    private void resize(ClientGameTestContext context, Viewport viewport) {
        context.getInput().resizeWindow(viewport.width(), viewport.height());
        context.runOnClient(client -> {
            client.options.guiScale().set(viewport.guiScale());
            //? if >=26.1 {
            /*client.resizeGui();
            *///?} else {
            client.resizeDisplay();
            //?}
        });
        waitFor(context, "window " + viewport, viewport::matches);
    }

    private static void shot(ClientGameTestContext context, String name, Viewport viewport) {
        context.waitTicks(GameTestSupport.scaled(5));
        context.runOnClient(client -> {
            if (!viewport.matches(client)) {
                throw new AssertionError("Expected " + viewport + " for " + name + ", saw " + currentViewport(client));
            }
        });
        String suffix = viewport.width() == 1920 ? "" : "-" + viewport.width() + "x" + viewport.height();
        GameTestSupport.saveShot(context, "v3-" + name + "-s" + viewport.guiScale() + suffix);
    }

    private void waitForScreen(ClientGameTestContext context, Class<? extends Screen> expected) {
        waitFor(context, expected == null ? "the world view" : expected.getSimpleName(), client -> {
            Screen screen = McBridge.currentScreen(client);
            return expected == null ? screen == null : expected.isInstance(screen);
        });
    }

    private void waitFor(ClientGameTestContext context, String description, Predicate<Minecraft> condition) {
        try {
            context.waitFor(condition, GameTestSupport.scaled(SYNC_TICKS));
        } catch (AssertionError timeout) {
            throw waitFailure(context, description, timeout);
        }
    }

    private AssertionError waitFailure(ClientGameTestContext context, String description, AssertionError cause) {
        String observed = context.computeOnClient(client -> {
            String worldKey = VillagerRosterClient.currentWorldKey(client);
            Screen screen = McBridge.currentScreen(client);
            Villager tracked = LibrarianTracker.current(client);
            Villager candidate = named(client, CANDIDATE);
            String menu = screen instanceof MerchantScreen merchant
                    ? merchant.getTitle().getString() + " " + MenuState.read(merchant.getMenu()) : "none";
            return "worldKey=" + worldKey + ", rosterCount=" + VillagerRosterClient.count(client)
                    + ", roster=" + VillagerRosterClient.roster().entries(worldKey).stream()
                    .map(entry -> entry.customName() + " (level=" + entry.level() + ", offers=" + entry.trades().size() + ")").toList()
                    + ", observedMenus=" + observedMenus + ", currentMenu=" + menu
                    + ", tracked=" + (tracked == null ? "none" : tracked.getName().getString())
                    + ", panel=" + TradePickerUi.panelShown() + ", hallActive=" + TradeHall.isActive()
                    + ", rerollActive=" + JobSiteReroller.isRunning() + ", queueSize=" + TradeHall.size()
                    + ", nearbyNoviceBlocker=" + (candidate == null ? "unloaded" : TradeHall.blocker(client, candidate.getId()))
                    + ", player=" + (client.player == null ? "none" : client.player.blockPosition())
                    + ", screen=" + (screen == null ? "none" : screen.getClass().getSimpleName())
                    + ", window=" + currentViewport(client);
        });
        return new AssertionError("Timed out waiting for " + description + ". Observed " + observed, cause);
    }

    private static String currentViewport(Minecraft client) {
        var window = client.getWindow();
        return window.getWidth() + "x" + window.getHeight() + " at GUI scale " + window.getGuiScale();
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
        int xp() {
            return locked ? 25 : 0;
        }

        int level() {
            return locked ? 2 : 1;
        }

        int offerCount() {
            return locked ? 2 : 1;
        }
    }

    private record MenuState(int xp, int level, int offers, boolean locked) {
        static MenuState read(MerchantMenu menu) {
            return new MenuState(menu.getTraderXp(), menu.getTraderLevel(), menu.getOffers().size(), TradeHall.menuLocked(menu));
        }
    }

    private record Viewport(int width, int height, int guiScale) {
        boolean matches(Minecraft client) {
            var window = client.getWindow();
            return width == window.getWidth() && height == window.getHeight() && guiScale == window.getGuiScale();
        }
    }

    private record ConfigState(boolean enabled, boolean discovery, int radius, HallView view) {
    }
}
