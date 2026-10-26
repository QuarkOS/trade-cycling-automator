//? if forge {
/*
package dev.quarkos.villagerreroller.net;

import dev.quarkos.villagerreroller.server.RerollHost;
import dev.quarkos.villagerreroller.server.ServerSettings;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.loading.FMLPaths;

//? if <1.20.2 {
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
//?}
//? if >=1.20.2 {
import net.minecraftforge.network.ChannelBuilder;
//?}

public final class ForgeChannels {
    private ForgeChannels() {
    }

    //? if <1.20.2 {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel LEGACY = NetworkRegistry.newSimpleChannel(
            RerollIds.channel(),
            () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL),
            NetworkRegistry.acceptMissingOr(PROTOCOL));

    private static void registerLegacy() {
        LEGACY.registerMessage(0, byte[].class,
                (msg, buf) -> buf.writeBytes(msg),
                buf -> readAll(buf),
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> dispatch(msg, ctx.get().getDirection().getReceptionSide().isServer(), ctx.get().getSender()));
                    ctx.get().setPacketHandled(true);
                });
    }
    //?}

    //? if >=1.20.2 && <1.21 {
    private static net.minecraftforge.network.SimpleChannel MID;

    private static void registerMid() {
        MID = ChannelBuilder.named(RerollIds.channel())
                .networkProtocolVersion(1)
                .optional()
                .simpleChannel();
        MID.messageBuilder(byte[].class, 0)
                .encoder((msg, buf) -> buf.writeBytes(msg))
                .decoder(ForgeChannels::readAll)
                .consumerNetworkThread((msg, ctx) -> {
                    ctx.enqueueWork(() -> dispatch(msg, ctx.isServerSide(), ctx.getSender()));
                    ctx.setPacketHandled(true);
                })
                .add();
    }
    //?}

    //? if >=1.21 {
    private static net.minecraftforge.network.Channel<net.minecraft.network.protocol.common.custom.CustomPacketPayload> MODERN;

    private static void registerModern() {
        MODERN = ChannelBuilder.named(RerollIds.channel())
                .networkProtocolVersion(1)
                .optional()
                .payloadChannel()
                .play()
                .bidirectional()
                .add(RerollPayload.TYPE, RerollPayload.CODEC, (payload, ctx) -> {
                    ctx.enqueueWork(() -> dispatch(payload.data(), ctx.isServerSide(), ctx.getSender()));
                    ctx.setPacketHandled(true);
                })
                .build();
    }
    //?}

    public static void registerServer() {
        //? if <1.20.2 {
        registerLegacy();
        //?}
        //? if >=1.20.2 && <1.21 {
        registerMid();
        //?}
        //? if >=1.21 {
        registerModern();
        //?}
        ServerSettings settings = ServerSettings.load(FMLPaths.CONFIGDIR.get().resolve("villager_reroller-server.json"));
        RerollHost.bind(new RerollHost.Transport() {
            @Override
            public void send(ServerPlayer player, byte[] packet) {
                Connection connection = connection(player);
                if (connection == null) {
                    return;
                }
                //? if <1.20.2 {
                LEGACY.send(PacketDistributor.PLAYER.with(() -> player), packet);
                //?}
                //? if >=1.20.2 && <1.21 {
                MID.send(packet, connection);
                //?}
                //? if >=1.21 {
                MODERN.send(new RerollPayload(packet), connection);
                //?}
            }

            @Override
            public boolean canSend(ServerPlayer player) {
                Connection connection = connection(player);
                if (connection == null) {
                    return false;
                }
                //? if <1.20.2 {
                return LEGACY.isRemotePresent(connection);
                //?}
                //? if >=1.20.2 && <1.21 {
                return MID.isRemotePresent(connection);
                //?}
                //? if >=1.21 {
                return MODERN.isRemotePresent(connection);
                //?}
            }
        }, settings);
        //? if <1.21.6 {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ForgeChannels::onLogin);
        //?}
        //? if >=1.21.6 {
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(ForgeChannels::onLogin);
        //?}
    }

    public static void registerClient() {
        ServerRerollClient.bind(new ServerRerollClient.Sender() {
            @Override
            public boolean canSend() {
                var handler = net.minecraft.client.Minecraft.getInstance().getConnection();
                if (handler == null) {
                    return false;
                }
                Connection connection = handler.getConnection();
                //? if <1.20.2 {
                return LEGACY.isRemotePresent(connection);
                //?}
                //? if >=1.20.2 && <1.21 {
                return MID != null && MID.isRemotePresent(connection);
                //?}
                //? if >=1.21 {
                return MODERN != null && MODERN.isRemotePresent(connection);
                //?}
            }

            @Override
            public void send(byte[] data) {
                if (!canSend()) {
                    return;
                }
                var handler = net.minecraft.client.Minecraft.getInstance().getConnection();
                //? if <1.20.2 {
                LEGACY.sendToServer(data);
                //?}
                //? if >=1.20.2 && <1.21 {
                MID.send(data, handler.getConnection());
                //?}
                //? if >=1.21 {
                MODERN.send(new RerollPayload(data), handler.getConnection());
                //?}
            }
        });
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RerollHost.onJoin(player);
        }
    }

    // getConnection() is public on 1.19.x. 1.20.1 hides the field, and 1.20.2+ moves it to the parent.
    private static Connection connection(ServerPlayer player) {
        Object listener = player.connection;
        if (listener == null) {
            return null;
        }
        Class<?> type = listener.getClass();
        while (type != null && type != Object.class) {
            try {
                var method = type.getDeclaredMethod("getConnection");
                if (Connection.class.isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    return (Connection) method.invoke(listener);
                }
            } catch (ReflectiveOperationException ignored) {
                // This class has no getConnection. The field is the next candidate.
            }
            try {
                var field = type.getDeclaredField("connection");
                if (Connection.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return (Connection) field.get(listener);
                }
            } catch (ReflectiveOperationException ignored) {
                // Declared on a superclass from 1.20.2 onward.
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static byte[] readAll(FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    private static void dispatch(byte[] data, boolean serverSide, ServerPlayer sender) {
        if (serverSide) {
            if (sender != null) {
                RerollHost.handle(sender, data);
            }
            return;
        }
        ServerRerollClient.accept(data);
    }
}
*///?} else {
package dev.quarkos.villagerreroller.net;

public final class ForgeChannels {
    private ForgeChannels() {
    }
}
//?}
