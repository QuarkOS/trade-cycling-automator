package dev.quarkos.villagerreroller.odds;

/**
 * Ticks between attempts for the two reroll methods, used before a run has a
 * measured pace.
 *
 * <p>Trade Cycling waits {@link #TRADE_CYCLING_TICKS} ticks when the offers
 * have not changed, which is the spacing the cycle driver already uses. A
 * fast server can answer sooner; the estimate uses this constant anyway.
 *
 * <p>A job-site attempt is not a fixed timer. {@link #JOB_SITE_TICKS} is a
 * nominal lectern replacement with a wooden axe: 2 ticks to look, about 38
 * ticks to break (hardness 2.5, axe speed 2), about 20 ticks for the villager
 * to drop the profession, 2 ticks to look at the placement, about 5 ticks to
 * place, about 50 ticks to reclaim the block, and about 15 ticks to reopen
 * the trade screen. 140 ticks is 7 seconds.
 */
public final class AttemptPace {
    public static final int TRADE_CYCLING_TICKS = 8;
    public static final int JOB_SITE_TICKS = 140;

    private AttemptPace() {
    }
}
