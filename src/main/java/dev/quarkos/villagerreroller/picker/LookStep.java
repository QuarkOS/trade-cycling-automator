package dev.quarkos.villagerreroller.picker;

/**
 * Shortest-path camera step. A tick never turns farther than the rate limit,
 * so the look packet is a small delta instead of a snap.
 */
public final class LookStep {
    private LookStep() {
    }

    public static float wrap(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    public static float approach(float current, float target, float maxStep) {
        float step = Math.max(0.0F, maxStep);
        float delta = wrap(target - current);
        if (Math.abs(delta) <= step) {
            return target;
        }
        return current + Math.copySign(step, delta);
    }

    public static boolean within(float yaw, float pitch, float targetYaw, float targetPitch, float tolerance) {
        return Math.abs(wrap(targetYaw - yaw)) <= tolerance && Math.abs(wrap(targetPitch - pitch)) <= tolerance;
    }
}
