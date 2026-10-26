//? if neoforge {
/*
package dev.quarkos.villagerreroller.compat;

import net.minecraft.client.multiplayer.ClientPacketListener;

// Sends Trade Cycling's cycle packet. 1.20.2 builds a play packet, 1.20.4 wraps
// a payload, and 1.20.6 and newer hand CycleTradesPacket to the distributor.
final class NeoForgeTradeCycling {
    private NeoForgeTradeCycling() {
    }

    static void send(ClientPacketListener handler) {
        //? if <1.20.4 {
        var packet = net.neoforged.neoforge.network.PlayNetworkDirection.PLAY_TO_SERVER.buildPacket(
                new net.neoforged.neoforge.network.INetworkDirection.PacketData(
                        new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer()),
                        0),
                de.maxhenkel.tradecycling.TradeCyclingMod.CYCLE_TRADES_PACKET);
        handler.send(packet);
        //?}
        //? if >=1.20.4 && <1.20.6 {
        net.neoforged.neoforge.network.PacketDistributor.SERVER.noArg().send(
                new de.maxhenkel.tradecycling.NeoForgeTradeCyclingMod.WrappedCycleTradesPacket());
        //?}
        //? if >=1.20.6 && <1.21.9 {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new de.maxhenkel.tradecycling.net.CycleTradesPacket());
        //?}
        //? if >=1.21.9 {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new de.maxhenkel.tradecycling.net.CycleTradesPacket());
        //?}
    }
}
*///?}
