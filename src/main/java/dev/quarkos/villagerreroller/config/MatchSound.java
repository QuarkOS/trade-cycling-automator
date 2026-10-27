package dev.quarkos.villagerreroller.config;

/**
 * On/off and volume for the chime that plays when a chosen offer appears.
 * The numbers are plain settings so tests can check them without Minecraft.
 */
public final class MatchSound {
    public static final int DEFAULT_VOLUME = 100;

    private MatchSound() {
    }

    public static int clampPercent(int volume) {
        return Math.max(0, Math.min(100, volume));
    }

    public static float scale(int volumePercent) {
        return clampPercent(volumePercent) / 100.0F;
    }

    public static boolean audible(boolean enabled, int volumePercent) {
        return enabled && clampPercent(volumePercent) > 0;
    }
}
