//? if forge {
/*package dev.quarkos.villagerreroller.net;

public final class FabricChannels {
    private FabricChannels() {
    }
}
*///?} else {
package dev.quarkos.villagerreroller.net;

import dev.quarkos.villagerreroller.server.RerollHost;
import dev.quarkos.villagerreroller.server.ServerSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server half of the Fabric channel. Client registration lives in {@link FabricClientChannels}
 * so a dedicated server never loads client networking classes.
 */
public final class FabricChannels {
    private FabricChannels() {
    }

    public static void registerServer() {
        ServerSettings settings = ServerSettings.load(FabricLoader.getInstance().getConfigDir().resolve("villager_reroller-server.json"));
        //? if <1.20.5 {
        /*ServerPlayNetworking.registerGlobalReceiver(RerollIds.channel(), (server, player, handler, buf, responseSender) -> {
            byte[] data = RerollIds.read(buf);
            server.execute(() -> RerollHost.handle(player, data));
        });
        RerollHost.bind(new RerollHost.Transport() {
            @Override
            public void send(ServerPlayer player, byte[] packet) {
                ServerPlayNetworking.send(player, RerollIds.channel(), RerollIds.write(packet));
            }

            @Override
            public boolean canSend(ServerPlayer player) {
                return ServerPlayNetworking.canSend(player, RerollIds.channel());
            }
        }, settings);
        *///?} else {
        //? if >=26.1 {
        /*net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(RerollPayload.TYPE, RerollPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(RerollPayload.TYPE, RerollPayload.CODEC);
        *///?} else {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(RerollPayload.TYPE, RerollPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(RerollPayload.TYPE, RerollPayload.CODEC);
        //?}
        ServerPlayNetworking.registerGlobalReceiver(RerollPayload.TYPE, (payload, context) -> {
            byte[] data = payload.data();
            context.server().execute(() -> RerollHost.handle(context.player(), data));
        });
        RerollHost.bind(new RerollHost.Transport() {
            @Override
            public void send(ServerPlayer player, byte[] packet) {
                ServerPlayNetworking.send(player, new RerollPayload(packet));
            }

            @Override
            public boolean canSend(ServerPlayer player) {
                return ServerPlayNetworking.canSend(player, RerollPayload.TYPE);
            }
        }, settings);
        //?}
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> RerollHost.onJoin(handler.getPlayer()));
    }
}
//?}
