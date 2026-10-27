package dev.quarkos.villagerreroller.picker;

import dev.quarkos.villagerreroller.odds.AttemptPace;
import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.LibrarianTracker;
import dev.quarkos.villagerreroller.client.McBridge;
import dev.quarkos.villagerreroller.compat.TradeCyclingHook;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.config.MatchSound;
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
    /** Predicted breaks and placements the server put back before this attempt gives up. */
    private static final int CORRECTION_LIMIT = 40;

    /** Headless clients stretch these when the integrated server falls behind. Unset for normal play. */
    private static int timeout(int ticks) {
        int scaled = ticks * AutomatorConfig.get().timeoutMultiplier();
        String scale = System.getProperty("villager_reroller.timeoutScale");
        if (scale == null || scale.isBlank()) {
            return scaled;
        }
        try {
            int extra = Integer.parseInt(scale);
            return Math.max(scaled, scaled * extra);
        } catch (NumberFormatException ignored) {
            return scaled;
        }
    }
    /**
     * Ticks the odds line uses for one attempt of the method selected on the panel.
     * Trade Cycling uses the configured pace, including the server-friendly preset.
     */
    public static int ticksPerAttempt() {
        return RerollMethod.usesTradeCycling() ? ticksBetweenCycles() : AttemptPace.JOB_SITE_TICKS;
    }

    private static int ticksBetweenCycles() {
        return AutomatorConfig.get().ticksBetweenCycles();
    }

    private static int lookBudget() {
        float step = Math.max(1.0F, AutomatorConfig.get().lookDegreesPerTick());
        return (int) Math.ceil(180.0F / step) + 4;
    }

    private static Phase phase = Phase.IDLE;
    private static StopReason stopReason;
    private static int phaseTicks;
    private static int attempts;
    private static boolean breakStarted;
    private static int breakCorrections;
    private static int placeCorrections;
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
        TOOL_WORN("tool_worn"),
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
        MISSING_TRADE_CYCLING("missing_trade_cycling"),
        PERMISSION("permission"),
        COST("cost"),
        COOLDOWN("cooldown"),
        TOO_FAR("too_far"),
        NOT_TRADING("not_trading"),
        SERVER_UNSUPPORTED("server_unsupported");

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
        return phase != Phase.IDLE || dev.quarkos.villagerreroller.net.ServerRerollClient.isRunning();
    }

    public static StopReason stopReason() {
        return stopReason;
    }

    public static int attempts() {
        if (dev.quarkos.villagerreroller.net.ServerRerollClient.isRunning()) {
            return dev.quarkos.villagerreroller.net.ServerRerollClient.attempts();
        }
        return attempts;
    }

    public static Component status() {
        if (dev.quarkos.villagerreroller.net.ServerRerollClient.isRunning()) {
            return dev.quarkos.villagerreroller.net.ServerRerollClient.status();
        }
        return status;
    }

    public static void onToggleKey() {
        if (TradeHall.isActive()) {
            TradeHall.requestStop();
            return;
        }
        if (isRunning()) {
            stop(StopReason.STOPPED);
        } else {
            start(Minecraft.getInstance());
        }
    }

    /**
     * The server path never enters a job-site phase, so {@link #stop} would return
     * before recording why it finished. The harness and the toast read this field.
     */
    public static void recordStop(StopReason reason, int serverAttempts) {
        stopReason = reason;
        if (serverAttempts > 0) {
            attempts = serverAttempts;
        }
        status = message(reason);
    }

    public static void stop(StopReason reason) {
        boolean server = dev.quarkos.villagerreroller.net.ServerRerollClient.isRunning();
        if (server) {
            dev.quarkos.villagerreroller.net.ServerRerollClient.halt(reason);
        }
        if (phase == Phase.IDLE && !server && reason != StopReason.PANIC && reason != StopReason.DISABLED) {
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
        breakCorrections = 0;
        placeCorrections = 0;
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
        breakCorrections = 0;
        placeCorrections = 0;
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
        if (dev.quarkos.villagerreroller.net.ServerRerollClient.tryStart(client, villager)) {
            return;
        }
        dev.quarkos.villagerreroller.net.ServerRerollClient.noteFallbackOnce();
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
        if (!prepareTool(client, client.player, state, false)) {
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
        if (dev.quarkos.villagerreroller.net.ServerRerollClient.isRunning()) {
            dev.quarkos.villagerreroller.net.ServerRerollClient.tick(client);
            return;
        }
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
        if (!TradeCyclingHook.serverSupports()) {
            fail(client, StopReason.SERVER_UNSUPPORTED);
            return;
        }
        if (!TradeCyclingHook.canCycle(menu)) {
            fail(client, StopReason.CANNOT_CYCLE);
            return;
        }
        villagerId = villager.getId();
        attempts = 0;
        ticksSinceSend = ticksBetweenCycles();
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
        if (!prepareTool(client, player, state, true)) {
            return;
        }
        Vec3 aim = Vec3.atCenterOf(jobPos);
        lookAt(player, aim);
        if (phaseTicks >= 1 && (facing(player, aim) || phaseTicks >= lookBudget())) {
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
        if (!heldIsSafe(player, state) && !prepareTool(client, player, state, true)) {
            client.gameMode.stopDestroyBlock();
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
            breakCorrections = 0;
            enter(Phase.PICKUP);
            return;
        }
        // The client predicts the break and then stops mining. Until the server agrees,
        // the lectern is still there and the villager will not change job.
        if (phaseTicks >= 20 && phaseTicks % 20 == 0) {
            dropUnconfirmedPredictions(Minecraft.getInstance());
        }
        if (jobBlockPresent()) {
            if (breakCorrections >= CORRECTION_LIMIT) {
                stop(StopReason.TIMEOUT);
                return;
            }
            breakCorrections++;
            breakStarted = false;
            LOGGER.info("Job block returned before the villager was unemployed, breaking again ({})", breakCorrections);
            enter(Phase.BREAK);
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
        Vec3 aim = Vec3.atCenterOf(jobPos.below()).add(0.0, 0.5, 0.0);
        lookAt(player, aim);
        if (phaseTicks >= 1 && (facing(player, aim) || phaseTicks >= lookBudget())) {
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
        BlockPos below = jobPos.below();
        Vec3 hit = Vec3.atCenterOf(below).add(0.0, 0.5, 0.0);
        lookAt(player, hit);
        if (!facing(player, hit) && phaseTicks < lookBudget()) {
            return;
        }
        if (phaseTicks % 4 != 1) {
            return;
        }
        BlockHitResult result = new BlockHitResult(hit, Direction.UP, below, false);
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, result);
        McBridge.swing(player, InteractionHand.MAIN_HAND);
    }

    private static void waitEmployed(Villager villager) {
        if (McBridge.professionId(villager).equals(professionId)) {
            placeCorrections = 0;
            enter(Phase.OPEN);
            return;
        }
        if (phaseTicks >= 20 && phaseTicks % 20 == 0) {
            dropUnconfirmedPredictions(Minecraft.getInstance());
        }
        if (!jobBlockPresent()) {
            if (placeCorrections >= CORRECTION_LIMIT) {
                stop(StopReason.TIMEOUT);
                return;
            }
            placeCorrections++;
            LOGGER.info("Job block missing before the villager was employed, placing again ({})", placeCorrections);
            enter(Phase.PLACE);
            return;
        }
        if (phaseTicks > timeout(EMPLOYED_TIMEOUT)) {
            stop(StopReason.TIMEOUT);
        }
    }

    /**
     * Puts the client block back to the state the server has acknowledged.
     * The getter is package-private, so this stays on reflection and compiles on every loader.
     */
    private static void dropUnconfirmedPredictions(Minecraft client) {
        if (client.level == null) {
            return;
        }
        try {
            java.lang.reflect.Method getter = predictionGetter(client.level.getClass());
            if (getter == null) {
                return;
            }
            getter.setAccessible(true);
            Object handler = getter.invoke(client.level);
            if (handler == null) {
                return;
            }
            for (java.lang.reflect.Method method : handler.getClass().getMethods()) {
                if (method.getParameterCount() == 2 && (method.getName().equals("endPredictionsUpTo") || method.getName().equals("method_41938"))) {
                    method.invoke(handler, Integer.MAX_VALUE, client.level);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // A build without the prediction handler just keeps the normal wait.
        }
    }

    private static java.lang.reflect.Method predictionGetter(Class<?> type) {
        while (type != null) {
            for (String name : new String[] {"getBlockStatePredictionHandler", "method_41925"}) {
                try {
                    return type.getDeclaredMethod(name);
                } catch (NoSuchMethodException ignored) {
                    // Named in dev and on Forge, intermediary in a Fabric production jar.
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    /** The job site this attempt is supposed to break or replace, as the client currently sees it. */
    private static boolean jobBlockPresent() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || jobPos == null || jobBlock == null) {
            return false;
        }
        BlockState state = client.level.getBlockState(jobPos);
        return !state.isAir() && state.getBlock() == jobBlock;
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
        Vec3 eyes = villager.getEyePosition();
        lookAt(player, eyes);
        if (!facing(player, eyes) && phaseTicks < lookBudget()) {
            return;
        }
        if (phaseTicks % 10 == 1) {
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
            if (ticksSinceSend < ticksBetweenCycles()) {
                return;
            }
        }
        if (attempts >= AutomatorConfig.get().maxAttempts()) {
            stop(StopReason.ATTEMPT_CAP);
            return;
        }
        if (!TradeCyclingHook.serverSupports()) {
            stop(StopReason.SERVER_UNSUPPORTED);
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
                    .append(TradeEntry.stackKey(result))
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
        AutomatorConfig config = AutomatorConfig.get();
        if (MatchSound.audible(config.matchSound(), config.matchVolume())) {
            McBridge.playMatchFound(client, MatchSound.scale(config.matchVolume()));
        }
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

    public static ToolGuard.Choice chooseTool(LocalPlayer player, BlockState state) {
        AutomatorConfig config = AutomatorConfig.get();
        Inventory inventory = player.getInventory();
        java.util.ArrayList<ToolGuard.Candidate> tools = new java.util.ArrayList<>();
        for (int i = 0; i < McBridge.storageSize(inventory); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !ToolWear.correct(stack, state)) {
                continue;
            }
            tools.add(new ToolGuard.Candidate(
                    i,
                    ToolWear.speed(stack, state),
                    ToolWear.remaining(stack),
                    ToolWear.enchanted(stack),
                    ToolWear.mending(stack)));
        }
        return ToolGuard.choose(tools, config.durabilityFloor(), config.allowBareHand(), ToolWear.handCanMine(state));
    }

    private static boolean prepareTool(Minecraft client, LocalPlayer player, BlockState state, boolean running) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        ToolGuard.Choice choice = chooseTool(player, state);
        if (choice.decision() == ToolGuard.Decision.TOOL) {
            selectSlot(client, choice.slot());
            return true;
        }
        if (choice.decision() == ToolGuard.Decision.HAND) {
            int empty = emptySlot(player);
            if (empty >= 0) {
                selectSlot(client, empty);
                return true;
            }
        }
        StopReason reason = choice.decision() == ToolGuard.Decision.NONE ? StopReason.NEED_TOOL : StopReason.TOOL_WORN;
        if (running) {
            stop(reason);
        } else {
            fail(client, reason);
        }
        return false;
    }

    private static boolean heldIsSafe(LocalPlayer player, BlockState state) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        ItemStack held = player.getMainHandItem();
        AutomatorConfig config = AutomatorConfig.get();
        if (held.isEmpty()) {
            return config.allowBareHand() && ToolWear.handCanMine(state);
        }
        if (!ToolWear.correct(held, state) || ToolWear.speed(held, state) <= 1.0F) {
            return false;
        }
        return ToolWear.remaining(held) > config.durabilityFloor();
    }

    private static int emptySlot(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        int size = McBridge.storageSize(inventory);
        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
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
        float[] aim = angles(player, target);
        float step = AutomatorConfig.get().lookDegreesPerTick();
        float yaw = LookStep.approach(player.getYRot(), aim[0], step);
        float pitch = LookStep.approach(player.getXRot(), aim[1], step);
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        McBridge.sendLook(player, yaw, pitch);
    }

    private static boolean facing(LocalPlayer player, Vec3 target) {
        float[] aim = angles(player, target);
        return LookStep.within(player.getYRot(), player.getXRot(), aim[0], aim[1], 3.0F);
    }

    private static float[] angles(LocalPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
        return new float[] {yaw, pitch};
    }
}
