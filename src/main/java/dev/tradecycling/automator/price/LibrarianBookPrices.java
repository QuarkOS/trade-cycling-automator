package dev.tradecycling.automator.price;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Vanilla librarian enchanted-book trade rules as published on the Minecraft Wiki,
 * loaded from {@value #RESOURCE}.
 */
public final class LibrarianBookPrices {

    public static final String RESOURCE = "/assets/trade-cycling-automator/librarian_book_prices.json";

    private static LibrarianBookPrices instance;

    public static LibrarianBookPrices get() {
        if (instance == null) {
            var version = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion();
            instance = load(predicate -> {
                try {
                    return VersionPredicate.parse(predicate).test(version);
                } catch (VersionParsingException e) {
                    throw new IllegalArgumentException("Bad enchantment version predicate " + predicate, e);
                }
            });
        }
        return instance;
    }

    public record PriceRange(int min, int max) {
        public boolean contains(int price) {
            return price >= min && price <= max;
        }
    }

    /**
     * @param minecraft Fabric version predicate limiting which Minecraft versions have this enchantment, or null for all
     */
    public record BookEnchantment(String id, int maxLevel, boolean treasure, String minecraft) {
    }

    public enum ProblemKind {
        UNKNOWN_ENCHANTMENT,
        LEVEL_TOO_LOW,
        LEVEL_TOO_HIGH,
        PRICE_BELOW_WIKI_MIN,
        PRICE_ABOVE_WIKI_MAX
    }

    /**
     * @param limit the bound that was violated (max level, wiki min or wiki max price); 0 for unknown enchantments
     */
    public record Problem(ProblemKind kind, int limit) {
    }

    private final Map<Integer, PriceRange> normalPrices;
    private final Map<Integer, PriceRange> treasurePrices;
    private final Map<String, BookEnchantment> enchantments;

    private LibrarianBookPrices(Map<Integer, PriceRange> normalPrices, Map<Integer, PriceRange> treasurePrices, Map<String, BookEnchantment> enchantments) {
        this.normalPrices = Collections.unmodifiableMap(normalPrices);
        this.treasurePrices = Collections.unmodifiableMap(treasurePrices);
        this.enchantments = Collections.unmodifiableMap(enchantments);
    }

    /**
     * @param minecraftVersionMatches tests an enchantment's {@code minecraft} version predicate against the running game
     */
    public static LibrarianBookPrices load(Predicate<String> minecraftVersionMatches) {
        try (InputStream in = LibrarianBookPrices.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing price table " + RESOURCE);
            }
            return parse(new InputStreamReader(in, StandardCharsets.UTF_8), minecraftVersionMatches);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read price table " + RESOURCE, e);
        }
    }

    static LibrarianBookPrices parse(Reader reader, Predicate<String> minecraftVersionMatches) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

        Map<Integer, PriceRange> normal = new TreeMap<>();
        Map<Integer, PriceRange> treasure = new TreeMap<>();
        for (JsonElement element : root.getAsJsonArray("levels")) {
            JsonObject row = element.getAsJsonObject();
            int level = row.get("level").getAsInt();
            normal.put(level, range(row.getAsJsonArray("normal")));
            treasure.put(level, range(row.getAsJsonArray("treasure")));
        }

        Map<String, BookEnchantment> enchantments = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("enchantments")) {
            JsonObject entry = element.getAsJsonObject();
            String minecraft = entry.has("minecraft") ? entry.get("minecraft").getAsString() : null;
            if (minecraft != null && !minecraftVersionMatches.test(minecraft)) {
                continue;
            }
            BookEnchantment enchantment = new BookEnchantment(
                    entry.get("id").getAsString(),
                    entry.get("maxLevel").getAsInt(),
                    entry.has("treasure") && entry.get("treasure").getAsBoolean(),
                    minecraft
            );
            if (!normal.containsKey(enchantment.maxLevel())) {
                throw new IllegalStateException("No price row for level " + enchantment.maxLevel() + " of " + enchantment.id());
            }
            enchantments.put(enchantment.id(), enchantment);
        }

        return new LibrarianBookPrices(normal, treasure, enchantments);
    }

    private static PriceRange range(JsonArray pair) {
        return new PriceRange(pair.get(0).getAsInt(), pair.get(1).getAsInt());
    }

    public Collection<BookEnchantment> enchantments() {
        return enchantments.values();
    }

    public Optional<BookEnchantment> enchantment(String id) {
        return Optional.ofNullable(enchantments.get(id));
    }

    public Map<Integer, PriceRange> normalPrices() {
        return normalPrices;
    }

    public Map<Integer, PriceRange> treasurePrices() {
        return treasurePrices;
    }

    /**
     * Wiki price range for a book, or empty if the enchantment is not sold by librarians at that level.
     */
    public Optional<PriceRange> priceRange(String id, int level) {
        return enchantment(id)
                .filter(e -> level >= 1 && level <= e.maxLevel())
                .map(e -> (e.treasure() ? treasurePrices : normalPrices).get(level));
    }

    public Optional<Problem> validateLevel(String id, int level) {
        BookEnchantment enchantment = enchantments.get(id);
        if (enchantment == null) {
            return Optional.of(new Problem(ProblemKind.UNKNOWN_ENCHANTMENT, 0));
        }
        if (level < 1) {
            return Optional.of(new Problem(ProblemKind.LEVEL_TOO_LOW, 1));
        }
        if (level > enchantment.maxLevel()) {
            return Optional.of(new Problem(ProblemKind.LEVEL_TOO_HIGH, enchantment.maxLevel()));
        }
        return Optional.empty();
    }

    public Optional<Problem> validate(String id, int level, int maxPrice) {
        Optional<Problem> levelProblem = validateLevel(id, level);
        if (levelProblem.isPresent()) {
            return levelProblem;
        }
        PriceRange range = priceRange(id, level).orElseThrow();
        if (maxPrice < range.min()) {
            return Optional.of(new Problem(ProblemKind.PRICE_BELOW_WIKI_MIN, range.min()));
        }
        if (maxPrice > range.max()) {
            return Optional.of(new Problem(ProblemKind.PRICE_ABOVE_WIKI_MAX, range.max()));
        }
        return Optional.empty();
    }
}
