package dev.quarkos.villagerreroller.net;

import java.nio.charset.StandardCharsets;

/**
 * Versioned play-phase payload for {@code villager_reroller:v1}.
 * Bytes only, so Fabric, Forge, and the Paper plugin share one layout.
 * The client sends {@link #REQUEST} only after it has decoded a {@link #HELLO}.
 */
public final class RerollCodec {
    public static final int PROTOCOL = 1;
    public static final String NAMESPACE = "villager_reroller";
    public static final String PATH = "v1";
    public static final String CHANNEL = NAMESPACE + ":" + PATH;
    public static final int MAX_BYTES = 64;

    public static final int HELLO = 1;
    public static final int REQUEST = 2;
    public static final int RESULT = 3;

    public static final int COST_FREE = 0;
    public static final int COST_EMERALDS = 1;
    public static final int COST_XP = 2;

    public enum Deny {
        OK(0),
        DISABLED(1),
        BAD_PACKET(2),
        NOT_VILLAGER(3),
        NO_PROFESSION(4),
        LOCKED(5),
        TOO_FAR(6),
        PERMISSION(7),
        COOLDOWN(8),
        COST(9),
        NOT_TRADING(10);

        private final int code;

        Deny(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }

        public static Deny from(int code) {
            for (Deny deny : values()) {
                if (deny.code == code) {
                    return deny;
                }
            }
            return BAD_PACKET;
        }
    }

    public record Hello(int costKind, int costAmount, int cooldownTicks, boolean freshOnly, int maxDistanceMilli) {
    }

    public record Request(int entityId, int nonce) {
    }

    public record Result(int nonce, Deny deny) {
    }

    private RerollCodec() {
    }

    public static int costKind(String cost) {
        if (cost == null) {
            return COST_FREE;
        }
        return switch (cost.trim().toLowerCase()) {
            case "emeralds", "emerald" -> COST_EMERALDS;
            case "xp", "experience" -> COST_XP;
            default -> COST_FREE;
        };
    }

    public static byte[] hello(Hello hello) {
        Writer writer = new Writer();
        writer.u8(HELLO);
        writer.u8(PROTOCOL);
        writer.u8(hello.costKind);
        writer.varint(hello.costAmount);
        writer.varint(hello.cooldownTicks);
        writer.u8(hello.freshOnly ? 1 : 0);
        writer.varint(hello.maxDistanceMilli);
        return writer.bytes();
    }

    public static byte[] request(Request request) {
        Writer writer = new Writer();
        writer.u8(REQUEST);
        writer.u8(PROTOCOL);
        writer.varint(request.entityId);
        writer.varint(request.nonce);
        return writer.bytes();
    }

    public static byte[] result(Result result) {
        Writer writer = new Writer();
        writer.u8(RESULT);
        writer.u8(PROTOCOL);
        writer.varint(result.nonce);
        writer.u8(result.deny.code);
        return writer.bytes();
    }

    public static Object decode(byte[] data) {
        if (data == null || data.length < 2 || data.length > MAX_BYTES) {
            return null;
        }
        Reader reader = new Reader(data);
        int type = reader.u8();
        int protocol = reader.u8();
        if (protocol != PROTOCOL || reader.overflow) {
            return null;
        }
        try {
            return switch (type) {
                case HELLO -> new Hello(reader.u8(), reader.varint(), reader.varint(), reader.u8() != 0, reader.varint());
                case REQUEST -> new Request(reader.varint(), reader.varint());
                case RESULT -> new Result(reader.varint(), Deny.from(reader.u8()));
                default -> null;
            };
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Null-separated channel list carried by the vanilla {@code minecraft:register} payload. */
    public static boolean registerListsChannel(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return false;
        }
        String text = new String(payload, StandardCharsets.UTF_8);
        for (String channel : text.split("\u0000")) {
            if (CHANNEL.equals(channel)) {
                return true;
            }
        }
        return false;
    }

    public static byte[] registerPayload() {
        return CHANNEL.getBytes(StandardCharsets.UTF_8);
    }

    private static final class Writer {
        private final byte[] buf = new byte[MAX_BYTES];
        private int at;

        void u8(int value) {
            if (at >= buf.length) {
                throw new IllegalArgumentException("payload too large");
            }
            buf[at++] = (byte) value;
        }

        void varint(int value) {
            int bits = value;
            do {
                int next = bits & 0x7F;
                bits >>>= 7;
                if (bits != 0) {
                    next |= 0x80;
                }
                u8(next);
            } while (bits != 0);
        }

        byte[] bytes() {
            byte[] copy = new byte[at];
            System.arraycopy(buf, 0, copy, 0, at);
            return copy;
        }
    }

    private static final class Reader {
        private final byte[] data;
        private int at;
        private boolean overflow;

        Reader(byte[] data) {
            this.data = data;
        }

        int u8() {
            if (at >= data.length) {
                overflow = true;
                throw new IllegalArgumentException("truncated");
            }
            return data[at++] & 0xFF;
        }

        int varint() {
            int value = 0;
            int shift = 0;
            for (int i = 0; i < 5; i++) {
                int piece = u8();
                value |= (piece & 0x7F) << shift;
                if ((piece & 0x80) == 0) {
                    return value;
                }
                shift += 7;
            }
            overflow = true;
            throw new IllegalArgumentException("varint");
        }
    }
}
