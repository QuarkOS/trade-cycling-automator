//? if neoforge {
/*
package dev.quarkos.villagerreroller.net;

import dev.quarkos.villagerreroller.server.RerollHost;
import dev.quarkos.villagerreroller.server.ServerSettings;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

//? if <1.20.4 {
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.simple.SimpleChannel;
//?}
//? if >=1.20.4 && <1.20.6 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
//?}
//? if >=1.20.6 {
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
//?}

// Same villager_reroller:v1 bytes as Fabric, Forge, and the Paper plugin. Optional, so a player without this mod stays connected.
public final class NeoForgeChannels {
    private NeoForgeChannels() {
    }

    //? if <1.20.4 {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel LEGACY = NetworkRegistry.newSimpleChannel(
            RerollIds.channel(),
            () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL),
            NetworkRegistry.acceptMissingOr(PROTOCOL));
    //?}

    public static void register(IEventBus modBus) {
        //? if <1.20.4 {
        registerLegacy();
        //?}
        //? if >=1.20.4 && <1.20.6 {
        modBus.addListener(NeoForgeChannels::registerPayload1204);
        //?}
        //? if >=1.20.6 {
        modBus.addListener(NeoForgeChannels::registerPayload);
        //?}
        ServerSettings settings = ServerSettings.load(FMLPaths.CONFIGDIR.get().resolve("villager_reroller-server.json"));
        RerollHost.bind(new RerollHost.Transport() {
            @Override
            public void send(ServerPlayer player, byte[] packet) {
                //? if <1.20.4 {
                LEGACY.send(PacketDistributor.PLAYER.with(() -> player), packet);
                //?}
                //? if >=1.20.4 && <1.20.6 {
                PacketDistributor.PLAYER.with(player).send(new PlayBytes(packet));
                //?}
                //? if >=1.20.6 {
                PacketDistributor.sendToPlayer(player, new RerollPayload(packet));
                //?}
            }

            @Override
            public boolean canSend(ServerPlayer player) {
                if (player.connection == null) {
                    return false;
                }
                //? if <1.20.4 {
                Connection connection = connection(player);
                return connection != null && LEGACY.isRemotePresent(connection);
                //?}
                //? if >=1.20.4 && <1.20.6 {
                return net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance()
                        .isConnected(player.connection, RerollIds.channel());
                //?}
                //? if >=1.20.6 {
                return ((ICommonPacketListener) player.connection).hasChannel(RerollPayload.TYPE);
                //?}
            }
        }, settings);
        NeoForge.EVENT_BUS.addListener(NeoForgeChannels::onLogin);
    }

    public static void registerClient() {
        ServerRerollClient.bind(new ServerRerollClient.Sender() {
            @Override
            public boolean canSend() {
                var handler = net.minecraft.client.Minecraft.getInstance().getConnection();
                if (handler == null) {
                    return false;
                }
                //? if <1.20.4 {
                return LEGACY.isRemotePresent(handler.getConnection());
                //?}
                //? if >=1.20.4 && <1.20.6 {
                return net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance()
                        .isConnected(handler, RerollIds.channel());
                //?}
                //? if >=1.20.6 {
                return ((ICommonPacketListener) handler).hasChannel(RerollPayload.TYPE);
                //?}
            }

            @Override
            public void send(byte[] data) {
                if (!canSend()) {
                    return;
                }
                //? if <1.20.4 {
                LEGACY.sendToServer(data);
                //?}
                //? if >=1.20.4 && <1.20.6 {
                PacketDistributor.SERVER.noArg().send(new PlayBytes(data));
                //?}
                //? if >=1.20.6 && <1.21.9 {
                PacketDistributor.sendToServer(new RerollPayload(data));
                //?}
                //? if >=1.21.9 {
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new RerollPayload(data));
                //?}
            }
        });
    }

    //? if <1.20.4 {
    private static void registerLegacy() {
        LEGACY.registerMessage(0, byte[].class,
                (msg, buf) -> buf.writeBytes(msg),
                NeoForgeChannels::readAll,
                (msg, ctx) -> {
                    ctx.enqueueWork(() -> dispatch(msg, ctx.getDirection().getReceptionSide().isServer(), ctx.getSender()));
                    ctx.setPacketHandled(true);
                });
    }
    //?}

    //? if >=1.20.4 && <1.20.6 {
    private static void registerPayload1204(RegisterPayloadHandlerEvent event) {
        event.registrar(RerollCodec.NAMESPACE).optional().play(
                RerollIds.channel(),
                buf -> new PlayBytes(readAll(buf)),
                NeoForgeChannels::handle1204);
    }

    private static void handle1204(PlayBytes payload, PlayPayloadContext context) {
        boolean serverSide = context.flow() == net.minecraft.network.protocol.PacketFlow.SERVERBOUND;
        byte[] data = payload.data();
        ServerPlayer sender = context.player().orElse(null) instanceof ServerPlayer player ? player : null;
        context.workHandler().execute(() -> dispatch(data, serverSide, sender));
    }

    public static final class PlayBytes implements CustomPacketPayload {
        private final byte[] data;

        public PlayBytes(byte[] data) {
            this.data = data;
        }

        public byte[] data() {
            return data;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeBytes(data);
        }

        @Override
        public net.minecraft.resources.ResourceLocation id() {
            return RerollIds.channel();
        }
    }
    //?}

    //? if >=1.20.6 {
    private static void registerPayload(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        //? if <1.21.9 {
        registrar.playBidirectional(RerollPayload.TYPE, RerollPayload.CODEC, NeoForgeChannels::handle);
        //?} else {
        registrar.playBidirectional(RerollPayload.TYPE, RerollPayload.CODEC, NeoForgeChannels::handle, NeoForgeChannels::handle);
        //?}
    }

    private static void handle(RerollPayload payload, IPayloadContext context) {
        boolean serverSide = context.flow() == net.minecraft.network.protocol.PacketFlow.SERVERBOUND;
        byte[] data = payload.data();
        ServerPlayer sender = context.player() instanceof ServerPlayer player ? player : null;
        context.enqueueWork(() -> dispatch(data, serverSide, sender));
    }
    //?}

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RerollHost.onJoin(player);
        }
    }

    //? if <1.20.4 {
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
                // Declared on a superclass.
            }
            type = type.getSuperclass();
        }
        return null;
    }
    //?}

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

public final class NeoForgeChannels {
    private NeoForgeChannels() {
    }
}
//?}
