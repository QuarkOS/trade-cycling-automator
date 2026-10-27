package dev.quarkos.villagerreroller.compat;

//? if forge {
/*import de.maxhenkel.tradecycling.ForgeTradeCyclingMod;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
*///?} else {
import de.maxhenkel.tradecycling.FabricTradeCyclingClientMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
//?}
import de.maxhenkel.tradecycling.gui.CycleTradesButton;
import net.minecraft.world.inventory.MerchantMenu;

/**
 * Calls Trade Cycling's own cycle action. Do not load this class unless
 * {@link TradeCyclingHook#installed()} is true.
 */
final class TradeCyclingActions {
    private TradeCyclingActions() {
    }

    static boolean serverHasChannel() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return false;
        }
        //? if neoforge {
        /*return NeoForgeTradeCycling.serverHasChannel(minecraft.getConnection());
        *///?}
        //? if forge && !neoforge {
        /*var channel = ForgeTradeCyclingMod.CYCLE_TRADES_CHANNEL;
        if (channel == null) {
            return false;
        }
        return channel.isRemotePresent(minecraft.getConnection().getConnection());
        *///?}
        //? if fabric {
        return ClientPlayNetworking.canSend(channel());
        //?}
    }

    //? if fabric {
    private static Identifier channel() {
        //? if <1.21 {
        /*return new Identifier("trade_cycling", "cycle_trades");
        *///?} else {
        return Identifier.fromNamespaceAndPath("trade_cycling", "cycle_trades");
        //?}
    }
    //?}

    static boolean canCycle(MerchantMenu menu) {
        return CycleTradesButton.canCycle(menu);
    }

    static boolean cycle(MerchantMenu menu) {
        if (!serverHasChannel() || !CycleTradesButton.canCycle(menu)) {
            return false;
        }
        //? if forge {
        /*        var handler = Minecraft.getInstance().getConnection();
        if (handler == null) {
            return false;
        }
        //? if <1.20.2 {
        de.maxhenkel.tradecycling.TradeCyclingClientMod.sendCycleTradesPacket();
        //?}
        //? if >=1.20.2 {
        // FML constructs ForgeTradeCyclingMod. sendCycleTradesPacket() lives on a
        // client object stored in a private field of that class, and the method
        // itself only writes an empty buffer to this public channel.
        ForgeTradeCyclingMod.CYCLE_TRADES_CHANNEL.send(new FriendlyByteBuf(Unpooled.buffer()), handler.getConnection());
        //?}
        *///?} else {
        FabricTradeCyclingClientMod mod = FabricTradeCyclingClientMod.instance();
        if (mod == null) {
            return false;
        }
        mod.sendCycleTradesPacket();
        //?}
        return true;
    }
}
