package dev.quarkos.villagerreroller.net;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.picker.JobSiteReroller;
import dev.quarkos.villagerreroller.picker.TradeEntry;
import dev.quarkos.villagerreroller.picker.TradeSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asks a server that already sent {@code villager_reroller:v1} hello to reroll the open villager.
 * No packet leaves the client until that hello has been decoded for this connection.
 */
public final class ServerRerollClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");
    private static final int RESULT_TIMEOUT = 80;

    private static Sender sender = new Sender() {
        @Override
        public boolean canSend() {
            return false;
        }

        @Override
        public void send(byte[] data) {
        }
    };

    private static int generation;
    private static int helloGeneration = -1;
    private static boolean running;
    private static int attempts;
    private static int nonce;
    private static int phaseTicks;
    private static int cooldownTicks = 1;
    private static int wait;
    private static String pending = "";
    private static boolean fallbackNoted;
    private static Component status = Component.empty();

    private ServerRerollClient() {
    }

    public interface Sender {
        boolean canSend();

        void send(byte[] data);
    }

    public static void bind(Sender bound) {
        if (bound != null) {
            sender = bound;
        }
    }

    public static void onJoin() {
        generation++;
        helloGeneration = -1;
    }

    public static void onDisconnect() {
        generation++;
        helloGeneration = -1;
        if (running) {
            halt(JobSiteReroller.StopReason.VILLAGER_GONE);
        }
    }

    public static void accept(byte[] data) {
        Object decoded = RerollCodec.decode(data);
        if (decoded instanceof RerollCodec.Hello hello) {
            helloGeneration = generation;
            cooldownTicks = Math.max(1, hello.cooldownTicks());
            LOGGER.info(
                    "villager_reroller server capability {} costKind={} amount={} cooldown={} freshOnly={} maxDistanceMilli={}",
                    RerollCodec.CHANNEL,
                    hello.costKind(),
                    hello.costAmount(),
                    hello.cooldownTicks(),
                    hello.freshOnly(),
                    hello.maxDistanceMilli());
            return;
        }
        if (!available() || !(decoded instanceof RerollCodec.Result result) || result.nonce() != nonce) {
            return;
        }
        if (!running) {
            return;
        }
        if (result.deny() == RerollCodec.Deny.COOLDOWN) {
            wait = cooldownTicks;
            pending = "";
            return;
        }
        if (result.deny() != RerollCodec.Deny.OK) {
            halt(map(result.deny()));
            return;
        }
        pending = "";
        phaseTicks = 0;
    }

    public static boolean available() {
        return helloGeneration == generation;
    }

    public static boolean isRunning() {
        return running;
    }

    public static int attempts() {
        return attempts;
    }

    public static Component status() {
        return status;
    }

    public static void noteFallbackOnce() {
        if (fallbackNoted) {
            return;
        }
        fallbackNoted = true;
        LOGGER.info("villager_reroller server capability absent, using job site");
    }

    public static boolean tryStart(Minecraft client, Villager villager) {
        if (!available() || running || villager == null) {
            return false;
        }
        attempts = 0;
        nonce = 0;
        wait = 0;
        pending = "";
        phaseTicks = 0;
        running = true;
        status = rerolling();
        LOGGER.info("villager_reroller server reroll started for {}", villager.getId());
        return true;
    }

    public static void halt(JobSiteReroller.StopReason reason) {
        if (!running && reason != JobSiteReroller.StopReason.PANIC) {
            return;
        }
        int done = attempts;
        running = false;
        status = Component.translatable("text." + VillagerReroller.MOD_ID + "." + reason.id());
        JobSiteReroller.recordStop(reason, done);
        if (reason != JobSiteReroller.StopReason.FOUND) {
            announce(Minecraft.getInstance(), status);
        }
        LOGGER.info("villager_reroller server reroll stopped: {} after {} attempts", reason, done);
    }

    public static void tick(Minecraft client) {
        if (!running) {
            return;
        }
        if (!available() || !AutomatorConfig.get().enabled()) {
            halt(AutomatorConfig.get().enabled() ? JobSiteReroller.StopReason.VILLAGER_GONE : JobSiteReroller.StopReason.DISABLED);
            return;
        }
        if (!sender.canSend()) {
            halt(JobSiteReroller.StopReason.VILLAGER_GONE);
            return;
        }
        LocalPlayer player = client.player;
        if (player == null || !(McBridge.currentScreen(client) instanceof MerchantScreen screen)) {
            if (phaseTicks++ > timeout(40)) {
                halt(JobSiteReroller.StopReason.GUI_CLOSED);
            }
            return;
        }
        MerchantMenu menu = screen.getMenu();
        MerchantOffers offers = menu.getOffers();
        if (offers.isEmpty()) {
            if (phaseTicks++ > timeout(RESULT_TIMEOUT)) {
                halt(JobSiteReroller.StopReason.NO_OFFERS);
            }
            return;
        }
        if (TradeSession.offerMatchesSelection(offers)) {
            int done = attempts;
            running = false;
            status = Component.translatable("text." + VillagerReroller.MOD_ID + ".found");
            JobSiteReroller.recordStop(JobSiteReroller.StopReason.FOUND, done);
            announce(client, status);
            LOGGER.info("villager_reroller server reroll matched after {} requests", done);
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        String signature = signature(offers);
        if (!pending.isEmpty()) {
            if (signature.equals(pending)) {
                if (phaseTicks++ > timeout(RESULT_TIMEOUT)) {
                    halt(JobSiteReroller.StopReason.TIMEOUT);
                }
                return;
            }
            pending = "";
            phaseTicks = 0;
        }
        if (attempts >= AutomatorConfig.get().maxAttempts()) {
            halt(JobSiteReroller.StopReason.ATTEMPT_CAP);
            return;
        }
        Villager villager = LibrarianTracker.current(client);
        if (villager == null) {
            halt(JobSiteReroller.StopReason.NOT_VILLAGER);
            return;
        }
        nonce++;
        attempts++;
        pending = signature;
        phaseTicks = 0;
        status = rerolling();
        sender.send(RerollCodec.request(new RerollCodec.Request(villager.getId(), nonce)));
    }

    private static JobSiteReroller.StopReason map(RerollCodec.Deny deny) {
        return switch (deny) {
            case LOCKED -> JobSiteReroller.StopReason.LOCKED;
            case PERMISSION -> JobSiteReroller.StopReason.PERMISSION;
            case COST -> JobSiteReroller.StopReason.COST;
            case COOLDOWN -> JobSiteReroller.StopReason.COOLDOWN;
            case TOO_FAR -> JobSiteReroller.StopReason.TOO_FAR;
            case NOT_TRADING -> JobSiteReroller.StopReason.NOT_TRADING;
            case NO_PROFESSION -> JobSiteReroller.StopReason.NO_PROFESSION;
            case NOT_VILLAGER -> JobSiteReroller.StopReason.NOT_VILLAGER;
            case DISABLED -> JobSiteReroller.StopReason.DISABLED;
            default -> JobSiteReroller.StopReason.TIMEOUT;
        };
    }

    private static String signature(MerchantOffers offers) {
        StringBuilder builder = new StringBuilder();
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            builder.append(offer.getCostA().getCount())
                    .append(':')
                    .append(result.getCount())
                    .append(':')
                    .append(TradeEntry.stackKey(result))
                    .append(';');
        }
        return builder.toString();
    }

    private static Component rerolling() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".rerolling", attempts + 1);
    }

    private static void announce(Minecraft client, Component component) {
        if (client.player != null) {
            McBridge.chat(client.player, component);
        }
        McBridge.toast(client, Component.translatable("text." + VillagerReroller.MOD_ID + ".toast"), component);
    }

    private static int timeout(int ticks) {
        String scale = System.getProperty("villager_reroller.timeoutScale");
        if (scale == null || scale.isBlank()) {
            return ticks;
        }
        try {
            return Math.max(ticks, ticks * Integer.parseInt(scale));
        } catch (NumberFormatException ignored) {
            return ticks;
        }
    }
}
