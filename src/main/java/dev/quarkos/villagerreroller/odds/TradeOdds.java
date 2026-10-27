package dev.quarkos.villagerreroller.odds;

import dev.quarkos.villagerreroller.price.VanillaBookPrices;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Closed-form chance that one vanilla trade roll hits any selected target,
 * plus a procedural simulator of the same generation steps.
 *
 * <p>One attempt draws {@code offerCounts} distinct listings without
 * replacement (or with replacement when {@code duplicates} is set). Each
 * listing then draws one branch by weight and, unless the branch is marked
 * together, one result uniformly. A book price is
 * {@code 2 + nextInt(5 + level * 10) + 3 * level}, doubled for treasure, then
 * capped at 64. Enchanted gear adds {@code 5 + nextInt(15)} to its base and
 * caps at 64. The attempt hits when any drawn result is a selected id whose
 * rolled price is at or under that id's ceiling.
 */
public final class TradeOdds {
    private static final int EMERALD_CAP = VanillaBookPrices.EMERALD_CAP;
    private static final int COMBINATION_LIMIT = 100_000;

    private TradeOdds() {
    }

    public enum Kind {
        FIXED,
        BOOK,
        GEAR,
        SELL
    }

    /**
     * How one catalog row is produced inside a vanilla listing.
     *
     * @param group    listings that share a group are one ItemListing or one trade-set entry
     * @param branch   weighted alternative inside that listing (one enchantment, or the only item)
     * @param weight   branch weight; vanilla books use {@link VanillaRoll#UNIFORM_ENCHANTMENT_WEIGHT}
     * @param kind     price formula
     * @param primary  book level, or the gear base cost
     * @param treasure double the book price before the emerald cap
     * @param together results that share a together-branch are all on the one offer
     */
    public record Roll(String group, String branch, int weight, Kind kind, int primary, boolean treasure, boolean together) {
        public Roll {
            if (weight < 1) {
                weight = 1;
            }
        }

        public static Roll fixed(String group) {
            return new Roll(group, "item", 1, Kind.FIXED, 0, false, false);
        }

        public static Roll sell(String group) {
            return new Roll(group, "sell", 1, Kind.SELL, 0, false, false);
        }

        public static Roll gear(String group, int base) {
            return new Roll(group, "gear", 1, Kind.GEAR, base, false, false);
        }

        public static Roll book(String group, String enchantmentId, int weight, int level, boolean treasure) {
            return new Roll(group, enchantmentId, weight, Kind.BOOK, level, treasure, false);
        }

        public static Roll together(String group) {
            return new Roll(group, "all", 1, Kind.FIXED, 0, false, true);
        }
    }

    public record Result(String tradeId, Price price) {
    }

    public sealed interface Price {
        record Fixed(int min, int max) implements Price {
        }

        record Book(int level, boolean treasure) implements Price {
        }

        record Gear(int base) implements Price {
        }

        record Sell() implements Price {
        }
    }

    public record Branch(int weight, boolean together, List<Result> results) {
    }

    public record Listing(List<Branch> branches) {
    }

    /**
     * @param offerCounts each entry is an equally likely number of offers to draw
     * @param duplicates  trade-set {@code allow_duplicates}; classic villagers are false
     */
    public record Pool(int[] offerCounts, boolean duplicates, List<Listing> listings) {
        public static final Pool EMPTY = new Pool(new int[]{0}, false, List.of());

        public Pool {
            offerCounts = offerCounts == null || offerCounts.length == 0 ? new int[]{0} : offerCounts.clone();
            listings = List.copyOf(listings);
        }
    }

    public record Estimate(double probability, long oneIn, String oneInText, String timeText, boolean impossible) {
    }

    public static Estimate estimate(Pool pool, Map<String, Integer> targets, int ticksPerAttempt) {
        if (pool == null || targets == null || targets.isEmpty()) {
            return null;
        }
        double probability = hitProbability(pool, targets);
        if (!(probability > 0)) {
            return new Estimate(0, 0, "", "", true);
        }
        long oneIn = oneIn(probability);
        int ticks = Math.max(1, ticksPerAttempt);
        double seconds = oneIn * (ticks / 20.0);
        return new Estimate(probability, oneIn, formatCount(oneIn), formatDuration(seconds), false);
    }

    public static long oneIn(double probability) {
        if (!(probability > 0)) {
            return Long.MAX_VALUE;
        }
        double inverse = 1.0 / probability;
        if (inverse >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        // 1 / (2/3) is 1.4999999999999998 in binary. Snap values that sit on a
        // half so "about 1 in 2" stays 2 instead of falling to 1.
        double half = Math.floor(inverse) + 0.5;
        if (Math.abs(inverse - half) < 1e-8) {
            inverse = half;
        }
        return Math.max(1L, Math.round(inverse));
    }

    public static String formatCount(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    public static String formatDuration(double seconds) {
        if (seconds < 1) {
            return "<1 sec";
        }
        if (seconds < 60) {
            return Math.round(seconds) + " sec";
        }
        double minutes = seconds / 60.0;
        if (minutes < 60) {
            if (minutes < 10) {
                return String.format(Locale.US, "%.1f min", minutes);
            }
            return Math.round(minutes) + " min";
        }
        double hours = minutes / 60.0;
        if (hours < 48) {
            if (hours < 10) {
                return String.format(Locale.US, "%.1f hr", hours);
            }
            return Math.round(hours) + " hr";
        }
        return Math.round(hours / 24.0) + " days";
    }

    public static double hitProbability(Pool pool, Map<String, Integer> targets) {
        if (pool == null || targets == null || targets.isEmpty() || pool.listings.isEmpty()) {
            return 0;
        }
        double[] listingHit = new double[pool.listings.size()];
        for (int i = 0; i < listingHit.length; i++) {
            listingHit[i] = listingHit(pool.listings.get(i), targets);
        }
        double total = 0;
        for (int count : pool.offerCounts) {
            total += attemptHit(listingHit, Math.max(0, count), pool.duplicates);
        }
        return total / pool.offerCounts.length;
    }

    /**
     * One vanilla attempt: sample the offer count, draw listings the way
     * {@code addOffersFromItemListings} does, then roll the enchantment, level,
     * and price with the same {@code nextInt} bounds.
     */
    public static boolean rollOnce(Pool pool, Map<String, Integer> targets, Random random) {
        if (pool == null || targets == null || targets.isEmpty() || pool.listings.isEmpty()) {
            return false;
        }
        int count = pool.offerCounts[random.nextInt(pool.offerCounts.length)];
        if (count <= 0) {
            return false;
        }
        List<Listing> bag = new ArrayList<>(pool.listings);
        int draws = 0;
        int limit = pool.duplicates ? count : bag.size();
        while (draws < count && !bag.isEmpty() && draws < limit) {
            int index = random.nextInt(bag.size());
            Listing listing = pool.duplicates ? bag.get(index) : bag.remove(index);
            draws++;
            if (listingHits(listing, targets, random)) {
                return true;
            }
        }
        return false;
    }

    public static double simulate(Pool pool, Map<String, Integer> targets, long seed, int trials) {
        Random random = new Random(seed);
        int hits = 0;
        for (int i = 0; i < trials; i++) {
            if (rollOnce(pool, targets, random)) {
                hits++;
            }
        }
        return hits / (double) trials;
    }

    public static double bookAtMost(int level, boolean treasure, int maxPrice) {
        if (level < 1 || maxPrice < 1) {
            return 0;
        }
        int bound = bookRollCount(level);
        int hits = 0;
        for (int roll = 0; roll < bound; roll++) {
            if (bookCost(level, treasure, roll) <= maxPrice) {
                hits++;
            }
        }
        return hits / (double) bound;
    }

    public static int bookRollCount(int level) {
        return 5 + level * 10;
    }

    public static int bookCost(int level, boolean treasure, int roll) {
        int cost = 2 + roll + 3 * level;
        if (treasure) {
            cost *= 2;
        }
        return Math.min(EMERALD_CAP, cost);
    }

    public static double gearAtMost(int base, int maxPrice) {
        if (maxPrice < 1) {
            return 0;
        }
        int hits = 0;
        for (int roll = 0; roll < 15; roll++) {
            if (gearCost(base, roll) <= maxPrice) {
                hits++;
            }
        }
        return hits / 15.0;
    }

    public static int gearCost(int base, int roll) {
        return Math.min(EMERALD_CAP, base + 5 + roll);
    }

    public static Pool pool(List<RolledRow> rows, int[] offerCounts, boolean duplicates) {
        Map<String, Map<String, List<RolledRow>>> groups = new LinkedHashMap<>();
        for (RolledRow row : rows) {
            groups.computeIfAbsent(row.roll.group(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(row.roll.branch(), key -> new ArrayList<>())
                    .add(row);
        }
        List<Listing> listings = new ArrayList<>();
        for (Map<String, List<RolledRow>> group : groups.values()) {
            List<Branch> branches = new ArrayList<>();
            for (List<RolledRow> members : group.values()) {
                Roll first = members.get(0).roll();
                List<Result> results = new ArrayList<>();
                for (RolledRow row : members) {
                    results.add(new Result(row.tradeId(), priceOf(row)));
                }
                branches.add(new Branch(first.weight(), first.together(), results));
            }
            listings.add(new Listing(branches));
        }
        return new Pool(offerCounts, duplicates, listings);
    }

    private static Price priceOf(RolledRow row) {
        Roll roll = row.roll();
        return switch (roll.kind()) {
            case BOOK -> new Price.Book(roll.primary(), roll.treasure());
            case GEAR -> new Price.Gear(roll.primary());
            case SELL -> new Price.Sell();
            case FIXED -> new Price.Fixed(row.priceMin(), Math.max(row.priceMin(), row.priceMax()));
        };
    }

    /**
     * A catalog row plus the roll metadata the live reader attaches.
     */
    public record RolledRow(String tradeId, int priceMin, int priceMax, Roll roll) {
    }

    private static double attemptHit(double[] listingHit, int count, boolean duplicates) {
        int n = listingHit.length;
        if (n == 0 || count <= 0) {
            return 0;
        }
        if (duplicates) {
            double missOne = 0;
            for (double hit : listingHit) {
                missOne += 1 - hit;
            }
            missOne /= n;
            return 1 - Math.pow(missOne, count);
        }
        int take = Math.min(count, n);
        double ways = combinations(n, take);
        if (ways <= 0 || ways > COMBINATION_LIMIT) {
            return sampledAttempt(listingHit, take);
        }
        return 1 - missSum(listingHit, take, 0, 1) / ways;
    }

    private static double sampledAttempt(double[] listingHit, int take) {
        // Only used if a datapack pool is too large to enumerate. The sample is seeded.
        Random random = new Random(0x5EEDL);
        int trials = 20_000;
        int hits = 0;
        List<Integer> bag = new ArrayList<>();
        for (int trial = 0; trial < trials; trial++) {
            bag.clear();
            for (int i = 0; i < listingHit.length; i++) {
                bag.add(i);
            }
            boolean hit = false;
            for (int drawn = 0; drawn < take && !bag.isEmpty(); drawn++) {
                int index = bag.remove(random.nextInt(bag.size()));
                if (random.nextDouble() < listingHit[index]) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                hits++;
            }
        }
        return hits / (double) trials;
    }

    private static double missSum(double[] listingHit, int left, int start, double product) {
        if (left == 0) {
            return product;
        }
        double sum = 0;
        for (int i = start; i <= listingHit.length - left; i++) {
            sum += missSum(listingHit, left - 1, i + 1, product * (1 - listingHit[i]));
        }
        return sum;
    }

    private static double combinations(int n, int k) {
        if (k < 0 || k > n) {
            return 0;
        }
        if (k == 0 || k == n) {
            return 1;
        }
        k = Math.min(k, n - k);
        double value = 1;
        for (int i = 1; i <= k; i++) {
            value = value * (n - k + i) / i;
        }
        return value;
    }

    private static double listingHit(Listing listing, Map<String, Integer> targets) {
        int total = 0;
        for (Branch branch : listing.branches) {
            total += branch.weight;
        }
        if (total <= 0) {
            return 0;
        }
        double hit = 0;
        for (Branch branch : listing.branches) {
            hit += (branch.weight / (double) total) * branchHit(branch, targets);
        }
        return hit;
    }

    private static double branchHit(Branch branch, Map<String, Integer> targets) {
        if (branch.results.isEmpty()) {
            return 0;
        }
        if (branch.together) {
            double miss = 1;
            for (Result result : branch.results) {
                miss *= 1 - resultHit(result, targets);
            }
            return 1 - miss;
        }
        double sum = 0;
        for (Result result : branch.results) {
            sum += resultHit(result, targets);
        }
        return sum / branch.results.size();
    }

    private static double resultHit(Result result, Map<String, Integer> targets) {
        Integer maxPrice = targets.get(result.tradeId);
        if (maxPrice == null) {
            return 0;
        }
        return priceAtMost(result.price, maxPrice);
    }

    private static double priceAtMost(Price price, int maxPrice) {
        if (price instanceof Price.Sell) {
            return 1;
        }
        if (price instanceof Price.Fixed fixed) {
            int min = fixed.min();
            int max = Math.max(min, fixed.max());
            if (maxPrice >= max) {
                return 1;
            }
            if (maxPrice < min) {
                return 0;
            }
            return (maxPrice - min + 1) / (double) (max - min + 1);
        }
        if (price instanceof Price.Book book) {
            return bookAtMost(book.level(), book.treasure(), maxPrice);
        }
        if (price instanceof Price.Gear gear) {
            return gearAtMost(gear.base(), maxPrice);
        }
        return 0;
    }

    private static boolean listingHits(Listing listing, Map<String, Integer> targets, Random random) {
        int total = 0;
        for (Branch branch : listing.branches) {
            total += branch.weight;
        }
        if (total <= 0 || listing.branches.isEmpty()) {
            return false;
        }
        int pick = random.nextInt(total);
        Branch branch = listing.branches.get(0);
        int cursor = 0;
        for (Branch candidate : listing.branches) {
            cursor += candidate.weight;
            if (pick < cursor) {
                branch = candidate;
                break;
            }
        }
        if (branch.results.isEmpty()) {
            return false;
        }
        if (branch.together) {
            for (Result result : branch.results) {
                if (priceHits(result, targets, random)) {
                    return true;
                }
            }
            return false;
        }
        Result result = branch.results.get(random.nextInt(branch.results.size()));
        return priceHits(result, targets, random);
    }

    private static boolean priceHits(Result result, Map<String, Integer> targets, Random random) {
        Integer maxPrice = targets.get(result.tradeId);
        if (maxPrice == null) {
            return false;
        }
        return rolledCost(result.price, random) <= maxPrice;
    }

    private static int rolledCost(Price price, Random random) {
        if (price instanceof Price.Sell) {
            return 0;
        }
        if (price instanceof Price.Fixed fixed) {
            int min = fixed.min();
            int max = Math.max(min, fixed.max());
            if (min >= max) {
                return min;
            }
            return min + random.nextInt(max - min + 1);
        }
        if (price instanceof Price.Book book) {
            int level = Math.max(1, book.level());
            int roll = random.nextInt(bookRollCount(level));
            return bookCost(level, book.treasure(), roll);
        }
        if (price instanceof Price.Gear gear) {
            return gearCost(gear.base(), random.nextInt(15));
        }
        return Integer.MAX_VALUE;
    }
}
