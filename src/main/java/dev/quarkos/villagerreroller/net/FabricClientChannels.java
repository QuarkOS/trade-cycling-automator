//? if forge {
/*package dev.quarkos.villagerreroller.net;

public final class FabricClientChannels {
    private FabricClientChannels() {
    }

    public static void register() {
    }

    public static boolean canSend() {
        return false;
    }

    public static void send(byte[] data) {
    }
}
*///?} else {
package dev.quarkos.villagerreroller.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Client half of the Fabric channel. Packets are sent only after a hello, and
 * {@link ClientPlayNetworking#canSend} stays false until the server registered the same id.
 */
public final class FabricClientChannels {
    private FabricClientChannels() {
    }

    public static void register() {
        //? if <1.20.5 {
        /*ClientPlayNetworking.registerGlobalReceiver(RerollIds.channel(), (client, handler, buf, responseSender) -> {
            byte[] data = RerollIds.read(buf);
            client.execute(() -> ServerRerollClient.accept(data));
        });
        *///?} else {
        ClientPlayNetworking.registerGlobalReceiver(RerollPayload.TYPE, (payload, context) -> {
            byte[] data = payload.data();
            context.client().execute(() -> ServerRerollClient.accept(data));
        });
        //?}
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ServerRerollClient.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ServerRerollClient.onDisconnect());
    }

    public static boolean canSend() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            return false;
        }
        //? if <1.20.5 {
        /*return ClientPlayNetworking.canSend(RerollIds.channel());
        *///?} else {
        return ClientPlayNetworking.canSend(RerollPayload.TYPE);
        //?}
    }

    public static void send(byte[] data) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            return;
        }
        //? if <1.20.5 {
        /*if (!ClientPlayNetworking.canSend(RerollIds.channel())) {
            return;
        }
        ClientPlayNetworking.send(RerollIds.channel(), RerollIds.write(data));
        *///?} else {
        if (ClientPlayNetworking.canSend(RerollPayload.TYPE)) {
            ClientPlayNetworking.send(new RerollPayload(data));
            return;
        }
        // A Paper server advertises with a plugin message. Fabric's negotiation
        // list stays empty, but the payload codec is registered locally.
        //? if >=26.1 {
        /*client.getConnection().send(ClientPlayNetworking.createServerboundPacket(new RerollPayload(data)));
        *///?} else {
        client.getConnection().send(ClientPlayNetworking.createC2SPacket(new RerollPayload(data)));
        //?}
        //?}
    }
}
//?}
