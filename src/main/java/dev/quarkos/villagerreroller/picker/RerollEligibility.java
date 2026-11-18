package dev.quarkos.villagerreroller.picker;

/** The first profession tier that offers a trade in the current catalog. */
public enum RerollEligibility {
    NOVICE(1),
    APPRENTICE(2),
    JOURNEYMAN(3),
    EXPERT(4),
    MASTER(5),
    UNAVAILABLE(0);

    private final int level;

    RerollEligibility(int level) {
        this.level = level;
    }

    public static RerollEligibility of(int minLevel) {
        return switch (minLevel) {
            case 1 -> NOVICE;
            case 2 -> APPRENTICE;
            case 3 -> JOURNEYMAN;
            case 4 -> EXPERT;
            case 5 -> MASTER;
            default -> UNAVAILABLE;
        };
    }

    public boolean canReroll() {
        return this == NOVICE;
    }

    public String levelKey() {
        return "merchant.level." + level;
    }
}
