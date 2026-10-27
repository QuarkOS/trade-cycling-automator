package dev.quarkos.villagerreroller.net;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

/** Channel id shared by the pre-1.20.5 packet APIs. */
public final class RerollIds {
    private RerollIds() {
    }

    public static Identifier channel() {
        //? if <1.21 {
        /*return new Identifier(RerollCodec.NAMESPACE, RerollCodec.PATH);
        *///?} else {
        return Identifier.fromNamespaceAndPath(RerollCodec.NAMESPACE, RerollCodec.PATH);
        //?}
    }

    public static FriendlyByteBuf write(byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(data.length));
        buf.writeBytes(data);
        return buf;
    }

    public static byte[] read(FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }
}
