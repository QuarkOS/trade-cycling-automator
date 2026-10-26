package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Runs the single-villager reroller once per queued villager.
 * The player is never moved. Each job site is used only when it is already in reach.
 */
public final class TradeHall {
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");
    private static final int FOCUS_TICKS = 2;
    private static final int BETWEEN_TICKS = 5;

    private static final List<Slot> SLOTS = new ArrayList<>();
    private static Phase phase = Phase.IDLE;
    private static int index;
    private static int waitTicks;
    private static boolean stopRequested;
    private static BlockPos focusPos;

    private TradeHall() {
    }

    public enum Outcome {
        PENDING,
        FOUND,
        SKIPPED,
        FAILED,
        CANCELLED
    }

    public static final class Slot {
        private final int villagerId;
        private final String labelKey;
        private final String description;
        private final Map<String, Integer> selections;
        private Outcome outcome = Outcome.PENDING;
        private String detail = "";
        private int attempts;

        private Slot(int villagerId, String labelKey, String description, Map<String, Integer> selections) {
            this.villagerId = villagerId;
            this.labelKey = labelKey;
            this.description = description == null ? "" : description;
            this.selections = Map.copyOf(selections);
        }

        public int villagerId() {
            return villagerId;
        }

        public Component label() {
            return Component.translatable(labelKey);
        }

        public String description() {
            return description;
        }

        public Map<String, Integer> selections() {
            return selections;
        }

        public Outcome outcome() {
            return outcome;
        }

        public String detail() {
            return detail;
        }

        public int attempts() {
            return attempts;
        }
    }

    private enum Phase {
        IDLE,
        FOCUS,
        WAIT_SCREEN,
        DRIVING,
        PAUSE,
        COOLDOWN,
        SUMMARY
    }

    public static List<Slot> slots() {
        return Collections.unmodifiableList(SLOTS);
    }

    public static int size() {
        return SLOTS.size();
    }

    public static boolean isActive() {
        return phase == Phase.FOCUS
                || phase == Phase.WAIT_SCREEN
                || phase == Phase.DRIVING
                || phase == Phase.PAUSE
                || phase == Phase.COOLDOWN;
    }

    public static boolean showingSummary() {
        return phase == Phase.SUMMARY;
    }

    public static boolean queueCurrent(Minecraft client) {
        if (isActive()) {
            return false;
        }
        if (!(McBridge.currentScreen(client) instanceof MerchantScreen)) {
            notice(client, "need_screen");
            return false;
        }
        Villager villager = LibrarianTracker.current(client);
        if (villager == null) {
            notice(client, "not_villager");
            return false;
        }
        if (McBridge.unemployed(villager) || McBridge.nitwit(villager)) {
            notice(client, "no_profession");
            return false;
        }
        if (!TradeSession.hasSelection()) {
            notice(client, "need_target");
            return false;
        }
        Slot slot = new Slot(
                villager.getId(),
                labelKey(villager),
                TradeSession.describeSelection(),
                TradeSession.copySelections());
        for (int i = 0; i < SLOTS.size(); i++) {
            if (SLOTS.get(i).villagerId == villager.getId()) {
                SLOTS.set(i, slot);
                LOGGER.info("Updated hall queue slot {} for villager {}", i, villager.getId());
                noticeQueued(client, slot);
                return true;
            }
        }
        SLOTS.add(slot);
        LOGGER.info("Queued villager {} ({}) with {} targets", villager.getId(), slot.labelKey, slot.selections.size());
        noticeQueued(client, slot);
        return true;
    }

    public static void remove(int slotIndex) {
        if (isActive() || slotIndex < 0 || slotIndex >= SLOTS.size()) {
            return;
        }
        SLOTS.remove(slotIndex);
    }

    public static void clear() {
        if (isActive()) {
            return;
        }
        SLOTS.clear();
        index = 0;
        phase = Phase.IDLE;
        stopRequested = false;
        focusPos = null;
        LibrarianTracker.unpin();
    }

    public static void dismiss() {
        if (phase == Phase.SUMMARY) {
            phase = Phase.IDLE;
        }
    }

    public static boolean start(Minecraft client) {
        if (isActive() || SLOTS.isEmpty() || JobSiteReroller.isRunning()) {
            return false;
        }
        if (!AutomatorConfig.get().enabled()) {
            notice(client, "disabled");
            return false;
        }
        for (Slot slot : SLOTS) {
            slot.outcome = Outcome.PENDING;
            slot.detail = "";
            slot.attempts = 0;
        }
        index = 0;
        waitTicks = 0;
        stopRequested = false;
        focusPos = null;
        phase = Phase.FOCUS;
        LOGGER.info("Trading hall started with {} villagers", SLOTS.size());
        if (client.player != null && McBridge.currentScreen(client) instanceof MerchantScreen) {
            client.player.closeContainer();
        } else if (McBridge.currentScreen(client) != null) {
            McBridge.setScreen(client, null);
        }
        return true;
    }

    public static void requestStop() {
        if (!isActive()) {
            return;
        }
        stopRequested = true;
        if (JobSiteReroller.isRunning()) {
            JobSiteReroller.stop(JobSiteReroller.StopReason.STOPPED);
        }
    }

    public static void tick(Minecraft client) {
        if (phase == Phase.IDLE || phase == Phase.SUMMARY) {
            return;
        }
        if (!AutomatorConfig.get().enabled() && phase != Phase.COOLDOWN) {
            stopRequested = true;
            if (JobSiteReroller.isRunning()) {
                JobSiteReroller.stop(JobSiteReroller.StopReason.DISABLED);
            }
        }
        switch (phase) {
            case FOCUS -> tickFocus(client);
            case WAIT_SCREEN -> tickWait(client);
            case DRIVING -> tickDrive(client);
            case PAUSE -> tickPause(client);
            case COOLDOWN -> tickCooldown(client);
            case IDLE, SUMMARY -> {
            }
        }
        if (isActive() && client.player != null) {
            McBridge.actionBar(client.player, hudLine());
        }
    }

    /**
     * Why this villager would be skipped from the player's current position.
     * Null means the single-villager driver is allowed to try.
     */
    public static String blocker(Minecraft client, int villagerId) {
        if (client.player == null || client.level == null) {
            return "villager_gone";
        }
        Villager villager = find(client, villagerId);
        if (villager == null) {
            return "villager_gone";
        }
        if (McBridge.unemployed(villager) || McBridge.nitwit(villager)) {
            return "no_profession";
        }
        if (locked(villager)) {
            return "locked";
        }
        BlockPos pos = JobSiteFinder.find(client, villager);
        if (pos == null) {
            return "no_job_site";
        }
        if (!McBridge.withinBlock(client.player, pos) || !McBridge.withinEntity(client.player, villager)) {
            return "out_of_reach";
        }
        return null;
    }

    public static Component hudLine() {
        return hudTitle().copy().append(" · ").append(hudDetail());
    }

    public static Component hudTitle() {
        int current = Math.min(index + 1, Math.max(SLOTS.size(), 1));
        Component name = index >= 0 && index < SLOTS.size() ? SLOTS.get(index).label() : Component.literal("-");
        if (phase == Phase.COOLDOWN || phase == Phase.SUMMARY) {
            return countsLine();
        }
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.hud", current, SLOTS.size(), name);
    }

    public static Component hudDetail() {
        return switch (phase) {
            case FOCUS -> Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.focus");
            case WAIT_SCREEN -> Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.open");
            case DRIVING -> {
                Component status = JobSiteReroller.status();
                yield status == null || status.getString().isEmpty()
                        ? Component.translatable("text." + VillagerReroller.MOD_ID + ".rerolling", JobSiteReroller.attempts() + 1)
                        : status;
            }
            case PAUSE -> Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.next");
            case COOLDOWN, SUMMARY -> countsLine();
            case IDLE -> Component.empty();
        };
    }

    public static Component countsLine() {
        int found = 0;
        int skipped = 0;
        int failed = 0;
        for (Slot slot : SLOTS) {
            switch (slot.outcome) {
                case FOUND -> found++;
                case SKIPPED -> skipped++;
                case FAILED -> failed++;
                case PENDING, CANCELLED -> {
                }
            }
        }
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.counts", found, skipped, failed);
    }

    public static Component outcomeLine(Slot slot) {
        Component why = reasonText(slot.detail);
        return switch (slot.outcome) {
            case FOUND -> Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.found", slot.attempts);
            case SKIPPED -> Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.skipped", why);
            case FAILED -> Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.failed", why);
            case CANCELLED -> Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.cancelled");
            case PENDING -> Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.pending");
        };
    }

    private static void tickFocus(Minecraft client) {
        if (stopRequested) {
            finish(client);
            return;
        }
        if (index >= SLOTS.size()) {
            finish(client);
            return;
        }
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            mark(SLOTS.get(index), Outcome.FAILED, "villager_gone");
            beginPause();
            return;
        }
        Slot slot = SLOTS.get(index);
        String blocked = blocker(client, slot.villagerId);
        if (blocked != null) {
            mark(slot, skipOutcome(blocked), blocked);
            LOGGER.info("Hall skipped villager {} ({})", slot.villagerId, blocked);
            beginPause();
            return;
        }
        Villager villager = find(client, slot.villagerId);
        BlockPos job = JobSiteFinder.find(client, villager);
        if (job == null) {
            mark(slot, Outcome.SKIPPED, "no_job_site");
            beginPause();
            return;
        }
        focusPos = job;
        lookAt(player, Vec3.atCenterOf(job));
        waitTicks++;
        if (waitTicks < FOCUS_TICKS) {
            return;
        }
        lookAt(player, villager.getEyePosition());
        LibrarianTracker.pin(villager);
        TradeSession.setPanelOpen(true);
        McBridge.interact(client.gameMode, player, villager, InteractionHand.MAIN_HAND);
        phase = Phase.WAIT_SCREEN;
        waitTicks = 0;
        LOGGER.info("Hall focusing job site {} for villager {}", job, slot.villagerId);
    }

    private static void tickWait(Minecraft client) {
        waitTicks++;
        LocalPlayer player = client.player;
        Slot slot = SLOTS.get(index);
        if (player == null || client.level == null || client.gameMode == null) {
            mark(slot, Outcome.FAILED, "villager_gone");
            beginPause();
            return;
        }
        Villager villager = find(client, slot.villagerId);
        if (villager == null) {
            mark(slot, Outcome.SKIPPED, "villager_gone");
            beginPause();
            return;
        }
        if (McBridge.currentScreen(client) instanceof MerchantScreen screen) {
            LibrarianTracker.pin(villager);
            TradeSession.rebuild(client.level, villager, screen.getMenu());
            if (TradeSession.restoreSelections(slot.selections) <= 0) {
                mark(slot, Outcome.FAILED, "need_target");
                beginPause();
                return;
            }
            if (menuLocked(screen.getMenu())) {
                mark(slot, Outcome.SKIPPED, "locked");
                LOGGER.info("Hall skipped villager {} (locked offers)", slot.villagerId);
                beginPause();
                return;
            }
            JobSiteReroller.start(client);
            if (JobSiteReroller.isRunning()) {
                phase = Phase.DRIVING;
                waitTicks = 0;
                return;
            }
            noteDriver(slot);
            if (aborts(JobSiteReroller.stopReason()) || stopRequested) {
                stopRequested = true;
            }
            beginPause();
            return;
        }
        if (!McBridge.withinEntity(player, villager)) {
            if (waitTicks > scaled(40)) {
                mark(slot, Outcome.SKIPPED, "out_of_reach");
                beginPause();
            }
            return;
        }
        if (focusPos != null) {
            lookAt(player, Vec3.atCenterOf(focusPos));
        }
        if (waitTicks % 10 == 1) {
            lookAt(player, villager.getEyePosition());
            LibrarianTracker.pin(villager);
            McBridge.interact(client.gameMode, player, villager, InteractionHand.MAIN_HAND);
        }
        if (waitTicks > scaled(120)) {
            mark(slot, Outcome.FAILED, "timeout");
            beginPause();
        }
    }

    private static void tickDrive(Minecraft client) {
        if (JobSiteReroller.isRunning()) {
            return;
        }
        Slot slot = SLOTS.get(index);
        noteDriver(slot);
        if (aborts(JobSiteReroller.stopReason()) || stopRequested) {
            stopRequested = true;
        }
        beginPause();
    }

    private static void beginPause() {
        phase = Phase.PAUSE;
        waitTicks = 0;
    }

    private static void tickPause(Minecraft client) {
        waitTicks++;
        closeMerchant(client);
        if (waitTicks < BETWEEN_TICKS) {
            return;
        }
        advance(client);
    }

    private static void tickCooldown(Minecraft client) {
        waitTicks++;
        closeMerchant(client);
        boolean merchant = McBridge.currentScreen(client) instanceof MerchantScreen;
        if (merchant) {
            if (waitTicks > scaled(40)) {
                openSummary(client);
            }
            return;
        }
        if (waitTicks >= BETWEEN_TICKS) {
            openSummary(client);
        }
    }

    private static void advance(Minecraft client) {
        LibrarianTracker.unpin();
        index++;
        waitTicks = 0;
        focusPos = null;
        if (stopRequested || index >= SLOTS.size()) {
            finish(client);
            return;
        }
        phase = Phase.FOCUS;
    }

    private static void finish(Minecraft client) {
        LibrarianTracker.unpin();
        if (JobSiteReroller.isRunning()) {
            JobSiteReroller.stop(stopRequested ? JobSiteReroller.StopReason.STOPPED : JobSiteReroller.StopReason.DISABLED);
        }
        for (Slot slot : SLOTS) {
            if (slot.outcome == Outcome.PENDING) {
                slot.outcome = Outcome.CANCELLED;
                slot.detail = "stopped";
            }
        }
        phase = Phase.COOLDOWN;
        waitTicks = 0;
        focusPos = null;
        closeMerchant(client);
    }

    private static void openSummary(Minecraft client) {
        phase = Phase.SUMMARY;
        stopRequested = false;
        Component summary = countsLine();
        LOGGER.info("Trading hall finished: {}", summary.getString());
        if (client.player != null) {
            McBridge.chat(client.player, Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.done").append(": ").append(summary));
        }
        McBridge.toast(client, Component.translatable("text." + VillagerReroller.MOD_ID + ".hall.summary"), summary);
        McBridge.setScreen(client, new TradeHallScreen());
    }

    private static void noteDriver(Slot slot) {
        JobSiteReroller.StopReason reason = JobSiteReroller.stopReason();
        slot.attempts = JobSiteReroller.attempts();
        if (reason == null) {
            mark(slot, Outcome.FAILED, "timeout");
            return;
        }
        if (reason == JobSiteReroller.StopReason.FOUND) {
            mark(slot, Outcome.FOUND, "found");
        } else if (skipOutcome(reason.id()) == Outcome.SKIPPED) {
            mark(slot, Outcome.SKIPPED, reason.id());
        } else {
            mark(slot, Outcome.FAILED, reason.id());
        }
        LOGGER.info("Hall villager {} finished {} ({}) after {} attempts", slot.villagerId, slot.outcome, slot.detail, slot.attempts);
    }

    private static Outcome skipOutcome(String reason) {
        return switch (reason) {
            case "locked", "no_job_site", "out_of_reach", "not_villager", "no_profession", "villager_gone" -> Outcome.SKIPPED;
            default -> Outcome.FAILED;
        };
    }

    private static boolean aborts(JobSiteReroller.StopReason reason) {
        return reason == JobSiteReroller.StopReason.STOPPED
                || reason == JobSiteReroller.StopReason.DISABLED
                || reason == JobSiteReroller.StopReason.PANIC;
    }

    private static void mark(Slot slot, Outcome outcome, String detail) {
        slot.outcome = outcome;
        slot.detail = detail == null ? "" : detail;
    }

    private static void closeMerchant(Minecraft client) {
        if (client.player != null && McBridge.currentScreen(client) instanceof MerchantScreen) {
            client.player.closeContainer();
        }
    }

    private static boolean menuLocked(MerchantMenu menu) {
        if (menu.getTraderXp() > 0 || menu.getTraderLevel() > 1) {
            return true;
        }
        for (MerchantOffer offer : menu.getOffers()) {
            if (offer.getUses() > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean locked(Villager villager) {
        if (McBridge.villagerXp(villager) > 0 || McBridge.villagerLevel(villager) > 1) {
            return true;
        }
        var offers = villager.getOffers();
        if (offers == null) {
            return false;
        }
        for (MerchantOffer offer : offers) {
            if (offer.getUses() > 0) {
                return true;
            }
        }
        return false;
    }

    private static Villager find(Minecraft client, int villagerId) {
        if (client.level == null) {
            return null;
        }
        Entity entity = client.level.getEntity(villagerId);
        if (entity instanceof Villager villager && villager.isAlive()) {
            return villager;
        }
        return null;
    }

    private static String labelKey(Villager villager) {
        String id = McBridge.professionId(villager);
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        return "entity.minecraft.villager." + path;
    }

    private static Component reasonText(String detail) {
        if (detail == null || detail.isEmpty()) {
            return Component.empty();
        }
        String specific = "text." + VillagerReroller.MOD_ID + ".hall.why." + detail;
        return Component.translatable(specific);
    }

    private static void notice(Minecraft client, String reason) {
        Component message = Component.translatable("text." + VillagerReroller.MOD_ID + "." + reason);
        if (client.player != null) {
            McBridge.actionBar(client.player, message);
        }
    }

    private static void noticeQueued(Minecraft client, Slot slot) {
        if (client.player != null) {
            McBridge.actionBar(client.player, Component.translatable(
                    "text." + VillagerReroller.MOD_ID + ".hall.queued",
                    slot.label(),
                    slot.selections.size()));
        }
    }

    private static int scaled(int ticks) {
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

    private static void lookAt(Player player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        if (player instanceof LocalPlayer local) {
            McBridge.sendLook(local, yaw, pitch);
        }
    }
}
