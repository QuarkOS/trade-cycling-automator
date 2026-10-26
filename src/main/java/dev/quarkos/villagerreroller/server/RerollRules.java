package dev.quarkos.villagerreroller.server;

/**
 * Pure checks the dedicated server and the Paper plugin both apply.
 * Callers still have to enforce them; nothing here touches the game.
 */
public final class RerollRules {
    private RerollRules() {
    }

    /** At least one tick, even when the admin sets the cooldown to zero. */
    public static int enforcedCooldown(int configuredTicks) {
        return Math.max(1, configuredTicks);
    }

    public static boolean coolingDown(long now, long lastAccepted, int configuredTicks) {
        if (lastAccepted < 0) {
            return false;
        }
        return now - lastAccepted < enforcedCooldown(configuredTicks);
    }

    public static boolean tooFar(boolean sameLevel, double distanceSquared, double maxDistance) {
        if (!sameLevel) {
            return true;
        }
        double max = Math.max(1.0, Math.min(64.0, maxDistance));
        return distanceSquared > max * max;
    }

    /**
     * Vanilla refuses to forget a profession once a trade has been used,
     * the villager has experience, or the level is past novice.
     */
    public static boolean locked(boolean onlyFresh, int level, int xp, boolean anyUses) {
        if (!onlyFresh) {
            return false;
        }
        return level > 1 || xp > 0 || anyUses;
    }

    public static boolean canAfford(int costKind, int amount, int emeralds, int xpPoints) {
        if (amount <= 0 || costKind == 0) {
            return true;
        }
        if (costKind == 1) {
            return emeralds >= amount;
        }
        if (costKind == 2) {
            return xpPoints >= amount;
        }
        return false;
    }

    /** Total experience points vanilla would report for this level and bar. */
    public static int totalXp(int level, float progress, int xpNeededForNext) {
        int safeLevel = Math.max(0, level);
        int base;
        if (safeLevel <= 16) {
            base = safeLevel * safeLevel + 6 * safeLevel;
        } else if (safeLevel <= 31) {
            base = (int) (2.5 * safeLevel * safeLevel - 40.5 * safeLevel + 360);
        } else {
            base = (int) (4.5 * safeLevel * safeLevel - 162.5 * safeLevel + 2220);
        }
        if (xpNeededForNext <= 0 || progress <= 0.0F) {
            return base;
        }
        return base + Math.round(progress * xpNeededForNext);
    }
}
