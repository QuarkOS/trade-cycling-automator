package dev.quarkos.villagerreroller.harness;

import com.mojang.blaze3d.platform.NativeImage;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.net.ServerRerollClient;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.TradeSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Joins {@code 127.0.0.1} and rerolls the proof librarian.
 * {@code -Dvillager_reroller.serverHarness=with} expects the server addon.
 * {@code without} expects the job-site fallback.
 */
public final class ServerJoinHarness {
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");
    private static final int SCALE = Integer.getInteger("villager_reroller.timeoutScale", 1);

    private static final String MODE = System.getProperty("villager_reroller.serverHarness", "");
    private static final String ADDRESS = System.getProperty("villager_reroller.serverAddress", "127.0.0.1:25565");

    private static boolean armed;
    private static State state = State.WAIT_TITLE;
    private static int ticks;
    private static BlockPos lectern;
    private static boolean sawAir;
    private static boolean shotPanel;
    private static boolean shotResult;

    private ServerJoinHarness() {
    }

    public static void tick(Minecraft client) {
        if (!"with".equals(MODE) && !"without".equals(MODE)) {
            return;
        }
        if (!armed) {
            armed = true;
            client.options.pauseOnLostFocus = false;
            //? if >=1.19.4 {
            client.options.onboardAccessibility = false;
            //?}
            skipMultiplayerWarning(client);
            client.options.guiScale().set(2);
            LOGGER.info("villager_reroller server harness armed mode={}", MODE);
        }
        ticks++;
        try {
            switch (state) {
                case WAIT_TITLE -> waitTitle(client);
                case OPEN_MULTIPLAYER -> openMultiplayer(client);
                case OPEN_DIRECT -> openDirect(client);
                case JOIN -> join(client);
                case WAIT_WORLD -> waitWorld(client);
                case WAIT_VILLAGER -> waitVillager(client);
                case WAIT_MERCHANT -> waitMerchant(client);
                case PANEL -> panel(client);
                case REROLL -> reroll(client);
                case DONE -> finish(client);
            }
        } catch (RuntimeException ex) {
            fail(ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private static void waitTitle(Minecraft client) {
        Screen screen = McBridge.currentScreen(client);
        if (screen != null && !(screen instanceof TitleScreen)) {
            String type = screen.getClass().getSimpleName().toLowerCase();
            if (type.contains("notice") || type.contains("safety") || type.contains("accessibility") || type.contains("backup")) {
                pressContains(screen, "continue");
                pressContains(screen, "proceed");
                return;
            }
        }
        if (screen instanceof TitleScreen && !loading(client)) {
            state = State.OPEN_MULTIPLAYER;
            ticks = 0;
            return;
        }
        if (ticks > timeout(20 * 90)) {
            fail("title screen did not open");
        }
    }

    private static void openMultiplayer(Minecraft client) {
        if (press(McBridge.currentScreen(client), "menu.multiplayer")) {
            state = State.OPEN_DIRECT;
            ticks = 0;
            return;
        }
        if (ticks > timeout(20 * 20)) {
            fail("multiplayer button missing");
        }
    }

    private static void openDirect(Minecraft client) {
        Screen screen = McBridge.currentScreen(client);
        if (screen != null && screen.getClass().getSimpleName().toLowerCase().contains("safety")) {
            if (press(screen, "multiplayerWarning.check") || pressContains(screen, "proceed") || pressContains(screen, "check")) {
                return;
            }
            if (openJoinList(client)) {
                return;
            }
        }
        if (press(screen, "selectServer.direct") || pressContains(screen, "direct")) {
            state = State.JOIN;
            ticks = 0;
            return;
        }
        if (ticks > timeout(20 * 20)) {
            fail("direct connect button missing, screen=" + screen + " buttons=" + labels(screen));
        }
    }

    private static void skipMultiplayerWarning(Minecraft client) {
        try {
            java.lang.reflect.Field field = client.options.getClass().getField("skipMultiplayerWarning");
            field.setBoolean(client.options, true);
        } catch (ReflectiveOperationException ignored) {
            // Older clients have no multiplayer safety screen.
        }
    }

    private static boolean openJoinList(Minecraft client) {
        try {
            Class<?> join = Class.forName("net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen");
            Screen next = (Screen) join.getConstructor(Screen.class).newInstance(new TitleScreen());
            client.setScreen(next);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private static String labels(Screen screen) {
        if (screen == null) {
            return "[]";
        }
        StringBuilder labels = new StringBuilder();
        for (var widget : McBridge.buttons(screen)) {
            if (widget instanceof Button button) {
                if (labels.length() > 0) {
                    labels.append(" | ");
                }
                labels.append(button.getMessage().getString());
            }
        }
        return labels.toString();
    }

    private static void join(Minecraft client) {
        Screen screen = McBridge.currentScreen(client);
        if (screen == null) {
            return;
        }
        for (GuiEventListener child : screen.children()) {
            if (child instanceof EditBox box) {
                box.setValue(ADDRESS);
            }
        }
        if (ticks > 5 && (press(screen, "selectServer.select") || pressContains(screen, "join"))) {
            state = State.WAIT_WORLD;
            ticks = 0;
        }
        if (ticks > timeout(20 * 20)) {
            fail("could not join " + ADDRESS + " from " + screen.getClass().getName());
        }
    }

    private static void waitWorld(Minecraft client) {
        if (client.player != null && client.level != null && McBridge.currentScreen(client) == null) {
            if ("with".equals(MODE) && !ServerRerollClient.available()) {
                if (ticks > timeout(20 * 30)) {
                    fail("dedicated server did not advertise " + dev.quarkos.villagerreroller.net.RerollCodec.CHANNEL);
                }
                return;
            }
            if ("without".equals(MODE) && ServerRerollClient.available()) {
                fail("server advertised the reroll channel without the addon");
            }
            note("villager_reroller server harness joined mode=" + MODE + " capable=" + ServerRerollClient.available());
            state = State.WAIT_VILLAGER;
            ticks = 0;
            return;
        }
        if (ticks > timeout(20 * 90)) {
            fail("did not enter the dedicated world, screen=" + McBridge.currentScreen(client));
        }
    }

    private static void waitVillager(Minecraft client) {
        Villager villager = nearestProofVillager(client);
        lectern = findLectern(client);
        if (client.player != null && villager != null && client.player.distanceToSqr(villager) > 4.0) {
            stepToward(client, villager.getX(), villager.getY(), villager.getZ());
            return;
        }
        if (client.player != null && villager == null && lectern != null) {
            stepToward(client, lectern.getX() + 0.5, client.player.getY(), lectern.getZ() + 0.5);
            return;
        }
        if ("without".equals(MODE) && villager != null && (McBridge.unemployed(villager) || McBridge.nitwit(villager))) {
            return;
        }
        if (villager != null && lectern != null && client.gameMode != null && client.player != null && stocked(client)) {
            McBridge.interact(client.gameMode, client.player, villager, InteractionHand.MAIN_HAND);
            state = State.WAIT_MERCHANT;
            ticks = 0;
            return;
        }
        if (ticks > timeout(20 * 40)) {
            fail("librarian or lectern missing near spawn");
        }
    }

    private static boolean stocked(Minecraft client) {
        if (!"without".equals(MODE) || client.player == null) {
            return true;
        }
        var inventory = client.player.getInventory();
        boolean lecterns = false;
        boolean axe = false;
        for (int slot = 0; slot < McBridge.storageSize(inventory); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.LECTERN)) {
                lecterns = true;
            }
            if (stack.is(Items.IRON_AXE) || stack.is(Items.DIAMOND_AXE)) {
                axe = true;
            }
        }
        return lecterns && axe;
    }

    private static Villager nearestProofVillager(Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }
        AABB box = client.player.getBoundingBox().inflate(48.0);
        Villager nearest = null;
        double best = Double.MAX_VALUE;
        for (Villager villager : McBridge.nearby(client.level, Villager.class, box)) {
            if (!villager.isAlive()) {
                continue;
            }
            double distance = villager.distanceToSqr(client.player);
            if (distance < best) {
                best = distance;
                nearest = villager;
            }
        }
        if (nearest != null) {
            LibrarianTracker.remember(nearest);
        }
        return nearest;
    }

    private static void waitMerchant(Minecraft client) {
        if (McBridge.currentScreen(client) instanceof MerchantScreen) {
            state = State.PANEL;
            ticks = 0;
            return;
        }
        if (ticks % 20 == 0) {
            LibrarianTracker.bindNearest(client);
            Villager villager = LibrarianTracker.current(client);
            if (villager != null && client.player != null && client.player.distanceToSqr(villager) > 4.0) {
                stepToward(client, villager.getX(), villager.getY(), villager.getZ());
            } else if (villager != null && client.gameMode != null && client.player != null) {
                McBridge.interact(client.gameMode, client.player, villager, InteractionHand.MAIN_HAND);
            }
        }
        if (ticks > timeout(20 * 30)) {
            fail("trade screen did not open");
        }
    }

    private static void panel(Minecraft client) {
        if (!(McBridge.currentScreen(client) instanceof MerchantScreen screen)) {
            if (ticks > timeout(20 * 10)) {
                fail("merchant screen closed before the panel");
            }
            return;
        }
        if (!TradeSession.panelOpen()) {
            if (!press(screen, "text.villager_reroller.button.offers")) {
                if (ticks > timeout(20 * 10)) {
                    fail("offers button missing");
                }
            }
            return;
        }
        if (TradeSession.entries().isEmpty()) {
            if (ticks > timeout(20 * 10)) {
                fail("trade catalog was empty");
            }
            return;
        }
        int selected = TradeSession.selectAllExcept(screen.getMenu().getOffers());
        if (selected <= 0) {
            fail("every catalog row was already on the counter");
            return;
        }
        if (!shotPanel) {
            shotPanel = true;
            shot(client, "server-" + MODE + "-panel");
        }
        if (!press(screen, "text.villager_reroller.button.start")) {
            fail("start button missing");
            return;
        }
        state = State.REROLL;
        ticks = 0;
    }

    private static void reroll(Minecraft client) {
        if (lectern != null && client.level != null && client.level.getBlockState(lectern).isAir()) {
            sawAir = true;
        }
        if (!shotResult && ticks > timeout(40) && McBridge.currentScreen(client) instanceof MerchantScreen) {
            shotResult = true;
            shot(client, "server-" + MODE + "-reroll");
        }
        boolean running = JobSiteReroller.isRunning();
        JobSiteReroller.StopReason reason = JobSiteReroller.stopReason();
        if (!running && reason != null) {
            if (reason != JobSiteReroller.StopReason.FOUND) {
                fail("reroll stopped with " + reason + " after " + JobSiteReroller.attempts() + " attempts");
                return;
            }
            state = State.DONE;
            ticks = 0;
        }
        if (ticks > timeout(20 * 180)) {
            fail("reroll did not finish, running=" + running + " air=" + sawAir);
        }
    }

    private static void finish(Minecraft client) {
        if (ticks < 30) {
            if (ticks == 1) {
                if (lectern != null && client.level != null && client.level.getBlockState(lectern).isAir()) {
                    sawAir = true;
                }
                boolean still = lectern != null && client.level != null && client.level.getBlockState(lectern).is(Blocks.LECTERN);
                shot(client, "server-" + MODE + "-found");
                if ("with".equals(MODE)) {
                    if (sawAir || !still) {
                        fail("server reroll broke the lectern");
                        return;
                    }
                    if (JobSiteReroller.attempts() < 1) {
                        fail("server reroll matched without a request");
                        return;
                    }
                } else if (!sawAir) {
                    fail("fallback path never broke the lectern");
                    return;
                }
                note("villager_reroller server harness PASS mode=" + MODE + " attempts=" + JobSiteReroller.attempts() + " lectern=" + still);
                writeResult("PASS\nmode=" + MODE + "\nattempts=" + JobSiteReroller.attempts() + "\nlectern=" + still + "\n");
            }
            return;
        }
        System.exit(0);
    }

    private static BlockPos findLectern(Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }
        BlockPos origin = client.player.blockPosition();
        for (int dx = -24; dx <= 24; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -24; dz <= 24; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (client.level.getBlockState(pos).is(Blocks.LECTERN)) {
                        return pos.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static boolean press(Screen screen, String key) {
        if (screen == null) {
            return false;
        }
        String wanted = Component.translatable(key).getString();
        for (var widget : McBridge.buttons(screen)) {
            if (widget instanceof Button button && button.getMessage().getString().equals(wanted)) {
                pressButton(button);
                return true;
            }
        }
        return false;
    }

    private static boolean pressContains(Screen screen, String needle) {
        if (screen == null) {
            return false;
        }
        for (var widget : McBridge.buttons(screen)) {
            if (widget instanceof Button button && button.getMessage().getString().toLowerCase().contains(needle)) {
                pressButton(button);
                return true;
            }
        }
        return false;
    }

    private static void pressButton(Button button) {
        //? if <1.21.9 {
        /*button.onPress();
        *///?} else {
        button.onPress(new net.minecraft.client.input.InputWithModifiers() {
            @Override
            public int input() {
                return 0;
            }

            @Override
            public int modifiers() {
                return 0;
            }
        });
        //?}
    }

    private static void stepToward(Minecraft client, double x, double y, double z) {
        var player = client.player;
        if (player == null) {
            return;
        }
        double dx = x - player.getX();
        double dy = y - player.getY();
        double dz = z - player.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.05) {
            return;
        }
        double step = Math.min(0.15, dist);
        player.setPos(player.getX() + dx / dist * step, player.getY() + dy / dist * step, player.getZ() + dz / dist * step);
        player.setDeltaMovement(0.0, 0.0, 0.0);
    }

    private static boolean loading(Minecraft client) {
        //? if >=26.2 {
        /*return client.gui.overlay() != null;
        *///?} else {
        return client.getOverlay() != null;
        //?}
    }

    private static com.mojang.blaze3d.pipeline.RenderTarget mainTarget(Minecraft client) {
        //? if >=26.2 {
        /*return client.gameRenderer.mainRenderTarget();
        *///?} else {
        return client.getMainRenderTarget();
        //?}
    }

    private static void shot(Minecraft client, String name) {
        try {
            Path dir = artifacts();
            Files.createDirectories(dir);
            //? if <1.21.5 {
            /*NativeImage image = Screenshot.takeScreenshot(mainTarget(client));
            try {
                image.writeToFile(dir.resolve(name + ".png"));
                Files.copy(dir.resolve(name + ".png"), Path.of("/opt/cursor/artifacts/server-reroll").resolve(name + ".png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } finally {
                image.close();
            }
            *///?} else {
            Screenshot.takeScreenshot(mainTarget(client), image -> {
                try {
                    Path file = dir.resolve(name + ".png");
                    image.writeToFile(file);
                    Path copyDir = Path.of("/opt/cursor/artifacts/server-reroll");
                    Files.createDirectories(copyDir);
                    Files.copy(file, copyDir.resolve(name + ".png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    fail("screenshot " + name + " failed: " + ex.getMessage());
                } finally {
                    image.close();
                }
            });
            //?}
        } catch (IOException ex) {
            fail("screenshot " + name + " failed: " + ex.getMessage());
        }
    }

    private static Path artifacts() {
        String configured = System.getProperty("villager_reroller.artifacts");
        if (configured == null || configured.isBlank()) {
            return Path.of("/opt/cursor/artifacts/server-reroll");
        }
        return Path.of(configured);
    }

    private static int timeout(int ticks) {
        return Math.max(1, ticks) * Math.max(1, SCALE);
    }

    private static void note(String message) {
        LOGGER.info(message);
        System.out.println(message);
    }

    private static void writeResult(String body) {
        try {
            Path file = Path.of("/opt/cursor/artifacts/server-reroll/harness-result.txt");
            Files.createDirectories(file.getParent());
            Files.writeString(file, body);
        } catch (IOException ignored) {
            // The log line is the fallback record.
        }
    }

    private static void fail(String message) {
        LOGGER.error("villager_reroller server harness FAIL {}", message);
        System.err.println("villager_reroller server harness FAIL " + message);
        writeResult("FAIL\n" + message + "\n");
        System.exit(1);
    }

    private enum State {
        WAIT_TITLE,
        OPEN_MULTIPLAYER,
        OPEN_DIRECT,
        JOIN,
        WAIT_WORLD,
        WAIT_VILLAGER,
        WAIT_MERCHANT,
        PANEL,
        REROLL,
        DONE
    }
}
