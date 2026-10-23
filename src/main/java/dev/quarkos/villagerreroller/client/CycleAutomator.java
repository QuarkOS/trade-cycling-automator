package dev.quarkos.villagerreroller.client;

import de.maxhenkel.tradecycling.gui.CycleTradesButton;
import dev.quarkos.villagerreroller.compat.TradeCyclingHook;
import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.config.AutomatorConfig;
import dev.quarkos.villagerreroller.price.LibrarianBookPrices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Drives Trade Cycling through {@link TradeCyclingHook#cycle}.
 * Never clicks a trade slot and never moves emeralds.
 */
public final class CycleAutomator {
    private static final int TICKS_BETWEEN_CYCLES = 8;
    private static final int EMPTY_OFFER_TIMEOUT = 40;

    private static boolean running;
    private static int attempts;
    private static int ticksSinceSend;
    private static int emptyTicks;
    private static String pendingSignature = "";

    private CycleAutomator() {
    }

    public static boolean isRunning() {
        return running;
    }

    public static void toggle() {
        dev.quarkos.villagerreroller.picker.JobSiteReroller.onToggleKey();
    }

    public static void start() {
        Minecraft client = Minecraft.getInstance();
        if (!(McBridge.currentScreen(client) instanceof MerchantScreen)) {
            tell(StopReason.NEED_SCREEN);
            return;
        }
        AutomatorConfig config = AutomatorConfig.get();
        if (!config.enabled()) {
            tell(StopReason.DISABLED);
            return;
        }
        LibrarianBookPrices.Problem problem = config.problem().orElse(null);
        if (problem != null) {
            tell(Component.translatable("text." + VillagerReroller.MOD_ID + ".invalid." + problem.kind().name().toLowerCase(), problem.limit()));
            return;
        }
        running = true;
        attempts = 0;
        ticksSinceSend = TICKS_BETWEEN_CYCLES;
        emptyTicks = 0;
        pendingSignature = "";
        tell(StopReason.STARTED);
    }

    public static void stop(StopReason reason) {
        dev.quarkos.villagerreroller.picker.JobSiteReroller.stop(switch (reason) {
            case DISABLED -> dev.quarkos.villagerreroller.picker.JobSiteReroller.StopReason.DISABLED;
            case PANIC -> dev.quarkos.villagerreroller.picker.JobSiteReroller.StopReason.PANIC;
            case FOUND -> dev.quarkos.villagerreroller.picker.JobSiteReroller.StopReason.FOUND;
            case KEYBIND -> dev.quarkos.villagerreroller.picker.JobSiteReroller.StopReason.STOPPED;
            default -> dev.quarkos.villagerreroller.picker.JobSiteReroller.StopReason.STOPPED;
        });
    }

    private static void stopLegacy(StopReason reason) {
        boolean wasRunning = running;
        running = false;
        pendingSignature = "";
        if (wasRunning || reason == StopReason.PANIC || reason == StopReason.KEYBIND) {
            if (wasRunning || reason == StopReason.PANIC) {
                tell(reason);
            }
        }
    }

    public static void tick(Minecraft client) {
        if (!running) {
            return;
        }
        AutomatorConfig config = AutomatorConfig.get();
        if (!config.enabled()) {
            stop(StopReason.DISABLED);
            return;
        }
        if (!(McBridge.currentScreen(client) instanceof MerchantScreen screen)) {
            stop(StopReason.GUI_CLOSED);
            return;
        }
        if (client.level == null) {
            stop(StopReason.GUI_CLOSED);
            return;
        }
        if (!LibrarianTracker.currentIsLibrarian(client)) {
            stop(StopReason.NOT_LIBRARIAN);
            return;
        }

        MerchantMenu menu = screen.getMenu();
        MerchantOffers offers = menu.getOffers();
        if (offers.isEmpty()) {
            emptyTicks++;
            if (emptyTicks > EMPTY_OFFER_TIMEOUT) {
                stop(StopReason.NO_OFFERS);
            }
            return;
        }
        emptyTicks = 0;

        Holder<Enchantment> target = McBridge.enchantmentHolder(client.level.registryAccess(), config.enchantment()).orElse(null);
        if (target == null) {
            stop(StopReason.INVALID);
            return;
        }
        if (containsTarget(offers, target, config.level(), config.maxPrice())) {
            stop(StopReason.FOUND);
            return;
        }
        if (!CycleTradesButton.canCycle(menu)) {
            stop(StopReason.CANNOT_CYCLE);
            return;
        }

        String signature = signature(offers);
        if (signature.equals(pendingSignature)) {
            ticksSinceSend++;
            if (ticksSinceSend < TICKS_BETWEEN_CYCLES) {
                return;
            }
        }
        if (attempts >= config.maxAttempts()) {
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
    }

    static boolean containsTarget(MerchantOffers offers, Holder<Enchantment> target, int level, int maxPrice) {
        for (MerchantOffer offer : offers) {
            if (offer.isOutOfStock()) {
                continue;
            }
            ItemStack cost = offer.getCostA();
            if (!cost.is(Items.EMERALD)) {
                continue;
            }
            int price = cost.getCount();
            if (price <= 0 || price > maxPrice) {
                continue;
            }
            ItemStack result = offer.getResult();
            if (!result.is(Items.ENCHANTED_BOOK)) {
                continue;
            }
            ItemEnchantments stored = result.get(DataComponents.STORED_ENCHANTMENTS);
            boolean levelMatches;
            //? if <1.21 {
            /*levelMatches = stored != null && stored.getLevel(target.value()) == level;
            *///?} else {
            levelMatches = stored != null && stored.getLevel(target) == level;
            //?}
            if (levelMatches) {
                return true;
            }
        }
        return false;
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

    private static void tell(StopReason reason) {
        tell(Component.translatable("text." + VillagerReroller.MOD_ID + "." + reason.id()));
    }

    private static void tell(Component message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            McBridge.actionBar(client.player, message);
        }
    }

    public enum StopReason {
        STARTED("started"),
        FOUND("found"),
        KEYBIND("stopped"),
        GUI_CLOSED("gui_closed"),
        NOT_LIBRARIAN("not_librarian"),
        ATTEMPT_CAP("attempt_cap"),
        DISABLED("disabled"),
        PANIC("panic"),
        CANNOT_CYCLE("cannot_cycle"),
        INVALID("invalid"),
        NO_OFFERS("no_offers"),
        MISSING_TRADE_CYCLING("missing_trade_cycling"),
        NEED_SCREEN("need_screen");

        private final String id;

        StopReason(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }
}
