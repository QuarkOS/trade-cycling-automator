package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.compat.TradeCyclingHook;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Breaks and replaces a villager's job site with ordinary client mining and
 * placement. One break packet is started, then continued once per tick.
 */
public final class JobSiteReroller {
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");
    private static final int BREAK_TIMEOUT = 200;
    private static final int UNEMPLOYED_TIMEOUT = 200;
    private static final int PICKUP_TIMEOUT = 80;
    private static final int PLACE_TIMEOUT = 100;
    private static final int EMPLOYED_TIMEOUT = 300;
    private static final int OPEN_TIMEOUT = 120;
    private static final int OFFER_TIMEOUT = 40;

    /** Headless clients stretch these when the integrated server falls behind. Unset for normal play. */
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
    private static final int TICKS_BETWEEN_CYCLES = 8;

    private static Phase phase = Phase.IDLE;
    private static StopReason stopReason;
    private static int phaseTicks;
    private static int attempts;
    private static boolean breakStarted;
    private static boolean closingForReroll;
    private static BlockPos jobPos;
    private static Block jobBlock;
    private static String professionId = "";
    private static int villagerId = Integer.MIN_VALUE;
    private static Component status = Component.empty();
    private static String pendingSignature = "";
    private static int ticksSinceSend;
    private static boolean packetRun;

    private JobSiteReroller() {
    }

    public enum StopReason {
        FOUND("found"),
        STOPPED("stopped"),
        LOCKED("locked"),
        NO_JOB_SITE("no_job_site"),
        OUT_OF_REACH("out_of_reach"),
        NEED_BLOCK("need_block"),
        NEED_TOOL("need_tool"),
        TIMEOUT("timeout"),
        NO_OFFERS("no_offers"),
        ATTEMPT_CAP("attempt_cap"),
        NOT_VILLAGER("not_villager"),
        NO_PROFESSION("no_profession"),
        DISABLED("disabled"),
        PANIC("panic"),
        NEED_TARGET("need_target"),
        GUI_CLOSED("gui_closed"),
        VILLAGER_GONE("villager_gone"),
        NEED_SCREEN("need_screen"),
        CANNOT_CYCLE("cannot_cycle"),
        MISSING_TRADE_CYCLING("missing_trade_cycling");

        private final String id;

        StopReason(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private enum Phase {
        IDLE,
        PREPARE_BREAK,
        BREAK,
        WAIT_UNEMPLOYED,
        PICKUP,
        PREPARE_PLACE,
        PLACE,
        WAIT_EMPLOYED,
        OPEN,
        CHECK,
        CYCLE
    }

    public static boolean isRunning() {
        return phase != Phase.IDLE;
    }

    public static StopReason stopReason() {
        return stopReason;
    }

    public static int attempts() {
        return attempts;
    }

    public static Component status() {
        return status;
    }

    public static void onToggleKey() {
        if (isRunning()) {
            stop(StopReason.STOPPED);
        } else {
            start(Minecraft.getInstance());
        }
    }

    public static void stop(StopReason reason) {
        if (phase == Phase.IDLE && reason != StopReason.PANIC && reason != StopReason.DISABLED) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (phase == Phase.BREAK && client.gameMode != null) {
            client.gameMode.stopDestroyBlock();
        }
        Phase stoppedDuring = phase;
        phase = Phase.IDLE;
        closingForReroll = false;
        breakStarted = false;
        packetRun = false;
        pendingSignature = "";
        stopReason = reason;
        status = message(reason);
        if (reason != StopReason.FOUND) {
            announce(client, status);
        }
        LOGGER.info("Trade reroll stopped: {} during {} after {} attempts", reason, stoppedDuring, attempts);
    }

    public static void start(Minecraft client) {
        if (isRunning()) {
            return;
        }
        packetRun = false;
        AutomatorConfig config = AutomatorConfig.get();
        if (!config.enabled()) {
            fail(client, StopReason.DISABLED);
            return;
        }
        if (!(McBridge.currentScreen(client) instanceof MerchantScreen screen)) {
            fail(client, StopReason.NEED_SCREEN);
            return;
        }
        Villager villager = LibrarianTracker.current(client);
        if (villager == null) {
            fail(client, StopReason.NOT_VILLAGER);
            return;
        }
        MerchantMenu menu = screen.getMenu();
        if (locked(menu)) {
            fail(client, StopReason.LOCKED);
            return;
        }
        if (!TradeSession.hasSelection()) {
            fail(client, StopReason.NEED_TARGET);
            return;
        }
        if (McBridge.unemployed(villager) || McBridge.nitwit(villager)) {
            fail(client, StopReason.NO_PROFESSION);
            return;
        }
        if (TradeSession.offerMatchesSelection(menu.getOffers())) {
            succeed(client);
            return;
        }
        if (RerollMethod.usesTradeCycling()) {
            startCycling(client, villager, menu);
            return;
        }
        BlockPos pos = JobSiteFinder.find(client, villager);
        if (pos == null) {
            fail(client, StopReason.NO_JOB_SITE);
            return;
        }
        if (client.player == null || !McBridge.withinBlock(client.player, pos)) {
            fail(client, StopReason.OUT_OF_REACH);
            return;
        }
        BlockState state = client.level.getBlockState(pos);
        if (!hasBlock(client.player, state.getBlock())) {
            fail(client, StopReason.NEED_BLOCK);
            return;
        }
        if (!client.player.getAbilities().instabuild && toolSlot(client.player, state) < 0) {
            fail(client, StopReason.NEED_TOOL);
            return;
        }
        professionId = McBridge.professionId(villager);
        jobPos = pos.immutable();
        jobBlock = state.getBlock();
        villagerId = villager.getId();
        attempts = 0;
        stopReason = null;
        TradeSession.setPanelOpen(true);
        closingForReroll = true;
        client.player.closeContainer();
        enter(Phase.PREPARE_BREAK);
        status = rerolling();
        LOGGER.info("Trade reroll started at {} for {}", jobPos, professionId);
    }

    public static void tick(Minecraft client) {
        if (phase == Phase.IDLE) {
            return;
        }
        if (!AutomatorConfig.get().enabled()) {
            stop(StopReason.DISABLED);
            return;
        }
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            stop(StopReason.VILLAGER_GONE);
            return;
        }
        Villager villager = villager(client);
        if (villager == null) {
            stop(StopReason.VILLAGER_GONE);
            return;
        }
        phaseTicks++;
        switch (phase) {
            case PREPARE_BREAK -> prepareBreak(client, player);
            case BREAK -> breaking(client, player);
            case WAIT_UNEMPLOYED -> waitUnemployed(villager);
            case PICKUP -> pickup(player);
            case PREPARE_PLACE -> preparePlace(client, player);
            case PLACE -> place(client, player);
            case WAIT_EMPLOYED -> waitEmployed(villager);
            case OPEN -> open(client, player, villager);
            case CHECK -> check(client, player);
            case CYCLE -> cycle(client);
            case IDLE -> {
            }
        }
    }

    private static void startCycling(Minecraft client, Villager villager, MerchantMenu menu) {
        if (!TradeCyclingHook.installed()) {
            fail(client, StopReason.MISSING_TRADE_CYCLING);
            return;
        }
        if (!TradeCyclingHook.canCycle(menu)) {
            fail(client, StopReason.CANNOT_CYCLE);
            return;
        }
        villagerId = villager.getId();
        attempts = 0;
        ticksSinceSend = TICKS_BETWEEN_CYCLES;
        pendingSignature = "";
        packetRun = true;
        stopReason = null;
        TradeSession.setPanelOpen(true);
        enter(Phase.CYCLE);
        status = rerolling();
        LOGGER.info("Trade cycling reroll started for {}", McBridge.professionId(villager));
    }

    private static void prepareBreak(Minecraft client, LocalPlayer player) {
        BlockState state = client.level.getBlockState(jobPos);
        if (state.isAir() || state.getBlock() != jobBlock) {
            enter(Phase.WAIT_UNEMPLOYED);
            return;
        }
        if (!hasBlock(player, jobBlock)) {
            enter(Phase.PICKUP);
            return;
        }
        if (!player.getAbilities().instabuild) {
            int slot = toolSlot(player, state);
            if (slot < 0) {
                stop(StopReason.NEED_TOOL);
                return;
            }
            selectSlot(client, slot);
        }
        lookAt(player, Vec3.atCenterOf(jobPos));
        if (phaseTicks >= 2) {
            breakStarted = false;
            enter(Phase.BREAK);
        }
    }

    private static void breaking(Minecraft client, LocalPlayer player) {
        BlockState state = client.level.getBlockState(jobPos);
        if (state.isAir() || state.getBlock() != jobBlock) {
            enter(Phase.WAIT_UNEMPLOYED);
            return;
        }
        if (phaseTicks > timeout(BREAK_TIMEOUT)) {
            client.gameMode.stopDestroyBlock();
            stop(StopReason.TIMEOUT);
            return;
        }
        lookAt(player, Vec3.atCenterOf(jobPos));
        if (!breakStarted) {
            breakStarted = true;
            client.gameMode.startDestroyBlock(jobPos, Direction.UP);
            McBridge.swing(player, InteractionHand.MAIN_HAND);
            return;
        }
        client.gameMode.continueDestroyBlock(jobPos, Direction.UP);
        McBridge.swing(player, InteractionHand.MAIN_HAND);
    }

    private static void waitUnemployed(Villager villager) {
        if (McBridge.unemployed(villager)) {
            enter(Phase.PICKUP);
            return;
        }
        if (phaseTicks > timeout(UNEMPLOYED_TIMEOUT)) {
            stop(StopReason.TIMEOUT);
        }
    }

    private static void pickup(LocalPlayer player) {
        if (hasBlock(player, jobBlock)) {
            enter(Phase.PREPARE_PLACE);
            return;
        }
        if (phaseTicks > timeout(PICKUP_TIMEOUT)) {
            stop(StopReason.NEED_BLOCK);
        }
    }

    private static void preparePlace(Minecraft client, LocalPlayer player) {
        int slot = blockSlot(player, jobBlock);
        if (slot < 0) {
            enter(Phase.PICKUP);
            return;
        }
        selectSlot(client, slot);
        lookAt(player, Vec3.atCenterOf(jobPos.below()).add(0.0, 0.5, 0.0));
        if (phaseTicks >= 2) {
            enter(Phase.PLACE);
        }
    }

    private static void place(Minecraft client, LocalPlayer player) {
        BlockState state = client.level.getBlockState(jobPos);
        if (state.getBlock() == jobBlock) {
            enter(Phase.WAIT_EMPLOYED);
            return;
        }
        if (phaseTicks > timeout(PLACE_TIMEOUT)) {
            stop(StopReason.TIMEOUT);
            return;
        }
        if (phaseTicks % 4 != 1) {
            return;
        }
        BlockPos below = jobPos.below();
        Vec3 hit = Vec3.atCenterOf(below).add(0.0, 0.5, 0.0);
        lookAt(player, hit);
        BlockHitResult result = new BlockHitResult(hit, Direction.UP, below, false);
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, result);
        McBridge.swing(player, InteractionHand.MAIN_HAND);
    }

    private static void waitEmployed(Villager villager) {
        if (McBridge.professionId(villager).equals(professionId)) {
            enter(Phase.OPEN);
            return;
        }
        if (phaseTicks > timeout(EMPLOYED_TIMEOUT)) {
            stop(StopReason.TIMEOUT);
        }
    }

    private static void open(Minecraft client, LocalPlayer player, Villager villager) {
        if (McBridge.currentScreen(client) instanceof MerchantScreen) {
            closingForReroll = false;
            enter(Phase.CHECK);
            return;
        }
        if (phaseTicks > timeout(OPEN_TIMEOUT)) {
            stop(StopReason.TIMEOUT);
            return;
        }
        // The villager often takes a step while the job block is replaced. Stopping on
        // that first tick looked like a reach failure even when they were walking back.
        if (!McBridge.withinEntity(player, villager)) {
            if (phaseTicks > timeout(40)) {
                stop(StopReason.OUT_OF_REACH);
            }
            return;
        }
        if (phaseTicks % 10 == 1) {
            lookAt(player, villager.getEyePosition());
            McBridge.interact(client.gameMode, player, villager, InteractionHand.MAIN_HAND);
        }
    }

    private static void check(Minecraft client, LocalPlayer player) {
        if (!(McBridge.currentScreen(client) instanceof MerchantScreen screen)) {
            if (!closingForReroll) {
                stop(StopReason.GUI_CLOSED);
            }
            return;
        }
        MerchantMenu menu = screen.getMenu();
        if (menu.getOffers().isEmpty()) {
            if (phaseTicks > timeout(OFFER_TIMEOUT)) {
                stop(StopReason.NO_OFFERS);
            }
            return;
        }
        if (locked(menu)) {
            stop(StopReason.LOCKED);
            return;
        }
        if (TradeSession.offerMatchesSelection(menu.getOffers())) {
            succeed(client);
            return;
        }
        attempts++;
        if (attempts >= AutomatorConfig.get().maxAttempts()) {
            stop(StopReason.ATTEMPT_CAP);
            return;
        }
        status = rerolling();
        closingForReroll = true;
        player.closeContainer();
        enter(Phase.PREPARE_BREAK);
    }

    private static void cycle(Minecraft client) {
        if (!(McBridge.currentScreen(client) instanceof MerchantScreen screen)) {
            stop(StopReason.GUI_CLOSED);
            return;
        }
        MerchantMenu menu = screen.getMenu();
        MerchantOffers offers = menu.getOffers();
        if (offers.isEmpty()) {
            if (phaseTicks > timeout(OFFER_TIMEOUT)) {
                stop(StopReason.NO_OFFERS);
            }
            return;
        }
        if (locked(menu)) {
            stop(StopReason.LOCKED);
            return;
        }
        if (TradeSession.offerMatchesSelection(offers)) {
            succeed(client);
            return;
        }
        String signature = signature(offers);
        if (signature.equals(pendingSignature)) {
            ticksSinceSend++;
            if (ticksSinceSend < TICKS_BETWEEN_CYCLES) {
                return;
            }
        }
        if (attempts >= AutomatorConfig.get().maxAttempts()) {
            stop(StopReason.ATTEMPT_CAP);
            return;
        }
        if (!TradeCyclingHook.cycle(menu)) {
            stop(TradeCyclingHook.installed() ? StopReason.CANNOT_CYCLE : StopReason.MISSING_TRADE_CYCLING);
            return;
        }
        attempts++;
        pendingSignature = signature;
        ticksSinceSend = 0;
        status = rerolling();
    }

    private static String signature(MerchantOffers offers) {
        StringBuilder builder = new StringBuilder();
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            builder.append(offer.getCostA().getCount())
                    .append(':')
                    .append(result.getCount())
                    .append(':')
                    .append(result.getComponents())
                    .append(';');
        }
        return builder.toString();
    }

    private static void succeed(Minecraft client) {
        phase = Phase.IDLE;
        closingForReroll = false;
        breakStarted = false;
        stopReason = StopReason.FOUND;
        status = message(StopReason.FOUND);
        TradeSession.setPanelOpen(true);
        announce(client, status);
        LOGGER.info("Trade reroll found a target after {} {}", attempts, packetRun ? "cycles" : "replacements");
        packetRun = false;
    }

    private static void fail(Minecraft client, StopReason reason) {
        stopReason = reason;
        status = message(reason);
        announce(client, status);
        LOGGER.info("Trade reroll refused: {}", reason);
    }

    private static void enter(Phase next) {
        phase = next;
        phaseTicks = 0;
        if (next != Phase.CHECK) {
            status = rerolling();
        }
    }

    private static Component rerolling() {
        return Component.translatable("text." + VillagerReroller.MOD_ID + ".rerolling", attempts + 1);
    }

    private static Component message(StopReason reason) {
        return Component.translatable("text." + VillagerReroller.MOD_ID + "." + reason.id());
    }

    private static void announce(Minecraft client, Component component) {
        if (client.player != null) {
            McBridge.chat(client.player, component);
        }
        McBridge.toast(client, Component.translatable("text." + VillagerReroller.MOD_ID + ".toast"), component);
    }

    private static boolean locked(MerchantMenu menu) {
        if (menu.getTraderXp() > 0 || menu.getTraderLevel() > 1) {
            return true;
        }
        for (var offer : menu.getOffers()) {
            if (offer.getUses() > 0) {
                return true;
            }
        }
        return false;
    }

    private static Villager villager(Minecraft client) {
        if (client.level == null || villagerId == Integer.MIN_VALUE) {
            return null;
        }
        Entity entity = client.level.getEntity(villagerId);
        if (entity instanceof Villager villager && villager.isAlive()) {
            return villager;
        }
        return null;
    }

    private static boolean hasBlock(LocalPlayer player, Block block) {
        return blockSlot(player, block) >= 0;
    }

    private static int blockSlot(LocalPlayer player, Block block) {
        Item item = block.asItem();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < McBridge.storageSize(inventory); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item) && stack.getCount() > 0) {
                return i;
            }
        }
        return -1;
    }

    private static int toolSlot(LocalPlayer player, BlockState state) {
        Inventory inventory = player.getInventory();
        int best = -1;
        float bestSpeed = 1.0F;
        for (int i = 0; i < McBridge.storageSize(inventory); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            float speed = stack.getItem().getDestroySpeed(stack, state);
            if (speed > bestSpeed && stack.getItem().isCorrectToolForDrops(stack, state)) {
                bestSpeed = speed;
                best = i;
            }
        }
        return best;
    }

    private static void selectSlot(Minecraft client, int index) {
        LocalPlayer player = client.player;
        Inventory inventory = player.getInventory();
        if (index < 9) {
            McBridge.selectHotbar(inventory, index);
            return;
        }
        int hotbar = McBridge.selectedHotbar(inventory);
        McBridge.swapWithHotbar(client, player, index, hotbar);
    }

    private static void lookAt(LocalPlayer player, Vec3 target) {
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
        McBridge.sendLook(player, yaw, pitch);
    }
}
