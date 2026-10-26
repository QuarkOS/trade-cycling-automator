package dev.quarkos.villagerreroller.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RerollCodecTest {
    @Test
    void helloRoundTripKeepsTheAdminRules() {
        byte[] bytes = RerollCodec.hello(new RerollCodec.Hello(RerollCodec.COST_EMERALDS, 3, 20, true, 8000));
        RerollCodec.Hello hello = assertInstanceOf(RerollCodec.Hello.class, RerollCodec.decode(bytes));
        assertEquals(RerollCodec.COST_EMERALDS, hello.costKind());
        assertEquals(3, hello.costAmount());
        assertEquals(20, hello.cooldownTicks());
        assertTrue(hello.freshOnly());
        assertEquals(8000, hello.maxDistanceMilli());
    }

    @Test
    void requestAndResultRoundTrip() {
        byte[] requestBytes = RerollCodec.request(new RerollCodec.Request(42, 7));
        RerollCodec.Request request = assertInstanceOf(RerollCodec.Request.class, RerollCodec.decode(requestBytes));
        assertEquals(42, request.entityId());
        assertEquals(7, request.nonce());

        byte[] resultBytes = RerollCodec.result(new RerollCodec.Result(7, RerollCodec.Deny.LOCKED));
        RerollCodec.Result result = assertInstanceOf(RerollCodec.Result.class, RerollCodec.decode(resultBytes));
        assertEquals(7, result.nonce());
        assertEquals(RerollCodec.Deny.LOCKED, result.deny());
    }

    @Test
    void rejectsAShortOrForeignPacket() {
        assertNull(RerollCodec.decode(new byte[] {1}));
        assertNull(RerollCodec.decode(new byte[] {RerollCodec.HELLO, 99}));
        assertNull(RerollCodec.decode(null));
        byte[] huge = new byte[RerollCodec.MAX_BYTES + 1];
        huge[0] = RerollCodec.HELLO;
        huge[1] = RerollCodec.PROTOCOL;
        assertNull(RerollCodec.decode(huge));
    }

    @Test
    void registerListMustNameThisChannel() {
        assertTrue(RerollCodec.registerListsChannel(RerollCodec.registerPayload()));
        assertTrue(RerollCodec.registerListsChannel("minecraft:brand\u0000villager_reroller:v1".getBytes()));
        assertFalse(RerollCodec.registerListsChannel("villager_reroller:v2\u0000minecraft:brand".getBytes()));
    }
}
