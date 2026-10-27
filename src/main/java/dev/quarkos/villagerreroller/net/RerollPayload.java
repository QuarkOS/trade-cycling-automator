//? if <1.20.5 {
/*package dev.quarkos.villagerreroller.net;

public final class RerollPayload {
    private RerollPayload() {
    }
}
*///?} else {
package dev.quarkos.villagerreroller.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * One custom-payload id, {@code villager_reroller:v1}, for every 1.20.5+ loader.
 * Older loaders send the same bytes through the pre-payload networking API.
 */
public record RerollPayload(byte[] data) implements CustomPacketPayload {
    public static final Type<RerollPayload> TYPE = new Type<>(RerollIds.channel());

    public static final StreamCodec<RegistryFriendlyByteBuf, RerollPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBytes(payload.data),
            buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new RerollPayload(bytes);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//?}
