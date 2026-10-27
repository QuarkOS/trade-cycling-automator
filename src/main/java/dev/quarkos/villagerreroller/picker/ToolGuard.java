package dev.quarkos.villagerreroller.picker;

import java.util.List;

/**
 * Picks a tool for a job-site break. Faster tools win. Equal speeds prefer an
 * unenchanted tool, then one without Mending. Tools at or below the durability
 * floor are skipped. The bare hand is the fallback when that is allowed.
 */
public final class ToolGuard {
    public static final int NONE = -1;
    public static final int HAND = -2;

    public enum Decision {
        TOOL,
        HAND,
        WORN,
        NONE
    }

    public record Candidate(int slot, float speed, int remaining, boolean enchanted, boolean mending) {
    }

    public record Choice(Decision decision, int slot) {
    }

    private ToolGuard() {
    }

    public static Choice choose(List<Candidate> tools, int floor, boolean allowHand, boolean handCorrect) {
        int bestSlot = NONE;
        float bestSpeed = 1.0F;
        int bestRank = Integer.MAX_VALUE;
        int bestRemaining = -1;
        boolean sawFastWorn = false;
        for (Candidate tool : tools) {
            if (tool.speed() <= 1.0F) {
                continue;
            }
            if (tool.remaining() <= floor) {
                sawFastWorn = true;
                continue;
            }
            int rank = rank(tool);
            boolean better = bestSlot == NONE
                    || tool.speed() > bestSpeed + 0.0001F
                    || (Math.abs(tool.speed() - bestSpeed) <= 0.0001F && rank < bestRank)
                    || (Math.abs(tool.speed() - bestSpeed) <= 0.0001F && rank == bestRank && tool.remaining() > bestRemaining);
            if (!better) {
                continue;
            }
            bestSlot = tool.slot();
            bestSpeed = tool.speed();
            bestRank = rank;
            bestRemaining = tool.remaining();
        }
        if (bestSlot != NONE) {
            return new Choice(Decision.TOOL, bestSlot);
        }
        if (allowHand && handCorrect) {
            return new Choice(Decision.HAND, HAND);
        }
        if (sawFastWorn) {
            return new Choice(Decision.WORN, NONE);
        }
        return new Choice(Decision.NONE, NONE);
    }

    /**
     * Unenchanted tools rank ahead of enchanted ones, and Mending ranks last.
     */
    static int rank(Candidate tool) {
        if (!tool.enchanted()) {
            return 0;
        }
        return tool.mending() ? 2 : 1;
    }
}
