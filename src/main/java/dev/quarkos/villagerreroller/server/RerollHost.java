package dev.quarkos.villagerreroller.server;

import dev.quarkos.villagerreroller.net.RerollCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.npc.villager.Villager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative reroll. The packet is ignored unless this process is a dedicated
 * server, the player is the villager's current customer, and every admin rule passes.
 */
public final class RerollHost {
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");

    private static ServerSettings settings = ServerSettings.defaults();
    private static Transport transport;
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

    private RerollHost() {
    }

    public interface Transport {
        void send(ServerPlayer player, byte[] packet);

        boolean canSend(ServerPlayer player);
    }

    public static void bind(Transport bound, ServerSettings loaded) {
        transport = bound;
        settings = loaded == null ? ServerSettings.defaults() : loaded.sanitize();
        LOGGER.info(
                "villager_reroller server config enabled={} cost={} amount={} cooldownTicks={} permission={} requirePermission={} opLevel={} freshOnly={} maxDistance={}",
                settings.enabled(),
                settings.cost(),
                settings.costAmount(),
                settings.cooldownTicks(),
                settings.permission(),
                settings.requirePermission(),
                settings.opLevel(),
                settings.onlyFreshVillagers(),
                settings.maxDistance());
    }

    public static ServerSettings settings() {
        return settings;
    }

    public static void onJoin(ServerPlayer player) {
        if (!dedicated(player) || transport == null || !settings.enabled()) {
            return;
        }
        if (!transport.canSend(player)) {
            return;
        }
        transport.send(player, RerollCodec.hello(settings.hello()));
        LOGGER.info("villager_reroller advertised {} to {}", RerollCodec.CHANNEL, name(player));
    }

    public static void handle(ServerPlayer player, byte[] packet) {
        if (player == null || transport == null) {
            return;
        }
        Object decoded = RerollCodec.decode(packet);
        if (!(decoded instanceof RerollCodec.Request request)) {
            return;
        }
        if (!dedicated(player) || !settings.enabled()) {
            reply(player, request.nonce(), RerollCodec.Deny.DISABLED);
            return;
        }
        RerollCodec.Deny deny = validate(player, request.entityId());
        if (deny != RerollCodec.Deny.OK) {
            reply(player, request.nonce(), deny);
            return;
        }
        Villager villager = villager(player, request.entityId());
        if (!charge(player)) {
            reply(player, request.nonce(), RerollCodec.Deny.COST);
            return;
        }
        LAST.put(player.getUUID(), gameTime(player));
        if (!OfferSync.reroll(villager)) {
            reply(player, request.nonce(), RerollCodec.Deny.NO_PROFESSION);
            return;
        }
        if (player.containerMenu instanceof MerchantMenu menu) {
            OfferSync.push(player, menu, villager);
        }
        reply(player, request.nonce(), RerollCodec.Deny.OK);
        LOGGER.info("villager_reroller rerolled villager {} for {}", villager.getId(), name(player));
    }

    private static RerollCodec.Deny validate(ServerPlayer player, int entityId) {
        if (!permitted(player)) {
            return RerollCodec.Deny.PERMISSION;
        }
        Villager villager = villager(player, entityId);
        if (villager == null) {
            return RerollCodec.Deny.NOT_VILLAGER;
        }
        if (VillagerChecks.noProfession(villager)) {
            return RerollCodec.Deny.NO_PROFESSION;
        }
        if (villager.getTradingPlayer() != player || !(player.containerMenu instanceof MerchantMenu)) {
            return RerollCodec.Deny.NOT_TRADING;
        }
        if (RerollRules.tooFar(sameLevel(player, villager), player.distanceToSqr(villager), settings.maxDistance())) {
            return RerollCodec.Deny.TOO_FAR;
        }
        if (RerollRules.locked(settings.onlyFreshVillagers(), VillagerChecks.level(villager), villager.getVillagerXp(), VillagerChecks.anyUses(villager))) {
            return RerollCodec.Deny.LOCKED;
        }
        long now = gameTime(player);
        long last = LAST.getOrDefault(player.getUUID(), -1L);
        if (RerollRules.coolingDown(now, last, settings.cooldownTicks())) {
            return RerollCodec.Deny.COOLDOWN;
        }
        int emeralds = countEmeralds(player);
        int xp = RerollRules.totalXp(player.experienceLevel, player.experienceProgress, player.getXpNeededForNextLevel());
        if (!RerollRules.canAfford(settings.costKind(), settings.costAmount(), emeralds, xp)) {
            return RerollCodec.Deny.COST;
        }
        return RerollCodec.Deny.OK;
    }

    private static void reply(ServerPlayer player, int nonce, RerollCodec.Deny deny) {
        if (transport == null || !transport.canSend(player)) {
            return;
        }
        transport.send(player, RerollCodec.result(new RerollCodec.Result(nonce, deny)));
        if (deny != RerollCodec.Deny.OK && deny != RerollCodec.Deny.COOLDOWN) {
            LOGGER.info("villager_reroller denied {} for {}: {}", deny, name(player), RerollCodec.CHANNEL);
        }
    }

    private static Villager villager(ServerPlayer player, int entityId) {
        //? if <1.20 {
        /*Entity entity = player.level.getEntity(entityId);
        *///?} else {
        Entity entity = player.level().getEntity(entityId);
        //?}
        if (entity instanceof Villager villager && villager.isAlive()) {
            return villager;
        }
        return null;
    }

    private static boolean sameLevel(ServerPlayer player, Villager villager) {
        //? if <1.20 {
        /*return player.level == villager.level;
        *///?} else {
        return player.level() == villager.level();
        //?}
    }

    private static long gameTime(ServerPlayer player) {
        //? if <1.20 {
        /*return player.level.getGameTime();
        *///?} else {
        return player.level().getGameTime();
        //?}
    }

    private static boolean dedicated(ServerPlayer player) {
        //? if <1.20 {
        /*MinecraftServer server = player.level.getServer();
        *///?} else {
        MinecraftServer server = player.level().getServer();
        //?}
        return server != null && server.isDedicatedServer();
    }

    private static String name(ServerPlayer player) {
        //? if >=1.21.9 {
        return player.getGameProfile().name();
        //?} else {
        /*return player.getGameProfile().getName();
        *///?}
    }

    private static boolean permitted(ServerPlayer player) {
        if (!settings.requirePermission()) {
            return true;
        }
        try {
            Class<?> permissions = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
            Method check = permissions.getMethod("check", Entity.class, String.class, int.class);
            Object result = check.invoke(null, player, settings.permission(), settings.opLevel());
            if (result instanceof Boolean allowed) {
                return allowed;
            }
        } catch (ReflectiveOperationException ignored) {
            // No permissions mod on the server. Op level is the fallback.
        }
        //? if >=1.21.11 {
        return player.permissions().hasPermission(new net.minecraft.server.permissions.Permission.HasCommandLevel(
                net.minecraft.server.permissions.PermissionLevel.byId(settings.opLevel())));
        //?} else {
        /*return player.hasPermissions(settings.opLevel());
        *///?}
    }

    private static boolean charge(ServerPlayer player) {
        int kind = settings.costKind();
        int amount = settings.costAmount();
        if (amount <= 0 || kind == RerollCodec.COST_FREE) {
            return true;
        }
        if (kind == RerollCodec.COST_EMERALDS) {
            return takeEmeralds(player, amount);
        }
        if (kind == RerollCodec.COST_XP) {
            player.giveExperiencePoints(-amount);
            return true;
        }
        return false;
    }

    private static boolean takeEmeralds(ServerPlayer player, int amount) {
        int left = amount;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !stack.is(Items.EMERALD)) {
                continue;
            }
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
            left -= take;
        }
        player.containerMenu.broadcastChanges();
        return left == 0;
    }

    private static int countEmeralds(ServerPlayer player) {
        int count = 0;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.EMERALD)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
