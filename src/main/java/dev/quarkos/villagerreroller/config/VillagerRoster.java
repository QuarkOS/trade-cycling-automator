package dev.quarkos.villagerreroller.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Locked villagers, grouped by world. The JSON shape matches
 * {@code config/villager_reroller-villagers.json}.
 */
public final class VillagerRoster {
    static final int VERSION = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private final Map<String, List<Entry>> worlds = new LinkedHashMap<>();
    private boolean readable = true;

    public enum WantedState {
        LOCKED_IN,
        OVER_CAP,
        MISSING
    }

    public record LockedOffer(
            String tradeId,
            String displayName,
            String enchantmentId,
            int enchantmentLevel,
            String itemId,
            int itemCount,
            int emeraldCost,
            int effectiveCost,
            boolean paysEmeralds
    ) {
    }

    public record Entry(
            String uuid,
            String dimension,
            int x,
            int y,
            int z,
            String customName,
            String profession,
            int level,
            long updatedAt,
            List<LockedOffer> trades
    ) {
    }

    public record WantedCheck(
            String profession,
            String tradeId,
            String displayName,
            int targetMax,
            WantedState state,
            int bestCost,
            int bestVillagerIndex
    ) {
    }

    public static VillagerRoster load(Path path) {
        VillagerRoster roster = new VillagerRoster();
        if (path == null || !Files.isRegularFile(path)) {
            return roster;
        }
        String text;
        try {
            text = Files.readString(path);
        } catch (IOException e) {
            LOGGER.error("Could not read {}", path, e);
            return unreadable();
        }
        if (text.isBlank()) {
            return roster;
        }
        try {
            JsonElement root = JsonParser.parseString(text);
            if (!root.isJsonObject()) {
                LOGGER.error("Villager file {} is not a JSON object", path);
                return unreadable();
            }
            JsonElement worlds = root.getAsJsonObject().get("worlds");
            if (worlds == null || worlds.isJsonNull()) {
                return roster;
            }
            if (!worlds.isJsonObject()) {
                LOGGER.error("Villager file {} has no worlds object", path);
                return unreadable();
            }
            for (Map.Entry<String, JsonElement> world : worlds.getAsJsonObject().entrySet()) {
                if (!world.getValue().isJsonArray()) {
                    LOGGER.error("Villager file {} has a bad world {}", path, world.getKey());
                    return unreadable();
                }
                List<Entry> entries = new ArrayList<>();
                for (JsonElement element : world.getValue().getAsJsonArray()) {
                    if (!element.isJsonObject()) {
                        LOGGER.error("Villager file {} has a bad villager in {}", path, world.getKey());
                        return unreadable();
                    }
                    entries.add(entry(element.getAsJsonObject()));
                }
                entries.sort((left, right) -> Long.compare(left.updatedAt, right.updatedAt));
                roster.worlds.put(world.getKey(), entries);
            }
            return roster;
        } catch (RuntimeException e) {
            LOGGER.error("Could not read {}", path, e);
            return unreadable();
        }
    }

    private static VillagerRoster unreadable() {
        VillagerRoster roster = new VillagerRoster();
        roster.readable = false;
        return roster;
    }

    public boolean readable() {
        return readable;
    }

    public List<Entry> entries(String worldKey) {
        List<Entry> found = worlds.get(worldKey);
        if (found == null) {
            return List.of();
        }
        return List.copyOf(found);
    }

    public int count(String worldKey) {
        return entries(worldKey).size();
    }

    /**
     * Inserts or replaces a villager. The first lock time is kept so the list
     * stays in the order the player recorded them.
     */
    public void upsert(String worldKey, Entry entry) {
        if (!readable || worldKey == null || worldKey.isEmpty() || entry == null || entry.uuid == null || entry.uuid.isEmpty()) {
            return;
        }
        List<Entry> list = worlds.computeIfAbsent(worldKey, key -> new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            if (entry.uuid.equals(list.get(i).uuid)) {
                Entry previous = list.get(i);
                list.set(i, new Entry(
                        entry.uuid,
                        entry.dimension,
                        entry.x,
                        entry.y,
                        entry.z,
                        entry.customName,
                        entry.profession,
                        entry.level,
                        previous.updatedAt,
                        entry.trades
                ));
                return;
            }
        }
        list.add(entry);
    }

    public boolean remove(String worldKey, String uuid) {
        List<Entry> list = worlds.get(worldKey);
        if (list == null || uuid == null) {
            return false;
        }
        boolean removed = list.removeIf(entry -> uuid.equals(entry.uuid));
        if (list.isEmpty()) {
            worlds.remove(worldKey);
        }
        return removed;
    }

    public int removeAll(String worldKey, Set<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (String uuid : uuids) {
            if (remove(worldKey, uuid)) {
                removed++;
            }
        }
        return removed;
    }

    public void clearWorld(String worldKey) {
        worlds.remove(worldKey);
    }

    public boolean save(Path path) {
        if (path == null) {
            return false;
        }
        if (!readable) {
            LOGGER.error("Refusing to overwrite unreadable villager file {}", path);
            return false;
        }
        Path temp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(temp, toJson());
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("Could not save {}", path, e);
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // The temp file is the only leftover. The villager file is unchanged.
            }
            return false;
        }
    }

    public static List<WantedCheck> evaluateWanted(ProfessionPicks picks, List<Entry> entries, Function<String, String> tradeNameLookup) {
        List<WantedCheck> checks = new ArrayList<>();
        if (picks == null) {
            return checks;
        }
        List<Entry> roster = entries == null ? List.of() : entries;
        for (Map.Entry<String, Map<String, Integer>> profession : picks.snapshot().entrySet()) {
            for (Map.Entry<String, Integer> trade : profession.getValue().entrySet()) {
                int target = trade.getValue();
                if (target <= 0) {
                    continue;
                }
                String name = tradeNameLookup == null ? null : tradeNameLookup.apply(trade.getKey());
                if (name == null || name.isBlank()) {
                    name = prettyTrade(trade.getKey());
                }
                int bestCost = Integer.MAX_VALUE;
                int bestIndex = 0;
                boolean pays = true;
                int index = 1;
                for (Entry entry : roster) {
                    if (!profession.getKey().equals(entry.profession)) {
                        index++;
                        continue;
                    }
                    for (LockedOffer offer : entry.trades) {
                        if (!trade.getKey().equals(offer.tradeId)) {
                            continue;
                        }
                        int cost = offer.effectiveCost > 0 ? offer.effectiveCost : offer.emeraldCost;
                        if (!offer.paysEmeralds) {
                            pays = false;
                            bestCost = cost;
                            bestIndex = index;
                        } else if (cost < bestCost) {
                            bestCost = cost;
                            bestIndex = index;
                        }
                    }
                    index++;
                }
                WantedState state;
                if (bestIndex == 0) {
                    state = WantedState.MISSING;
                    bestCost = 0;
                } else if (pays && target != Integer.MAX_VALUE && bestCost > target) {
                    state = WantedState.OVER_CAP;
                } else {
                    state = WantedState.LOCKED_IN;
                }
                checks.add(new WantedCheck(profession.getKey(), trade.getKey(), name, target, state, bestCost, bestIndex));
            }
        }
        return checks;
    }

    public static String prettyTrade(String tradeId) {
        if (tradeId == null || tradeId.isEmpty()) {
            return "";
        }
        String[] parts = tradeId.split(":");
        if (parts.length >= 4 && "enchant".equals(parts[0])) {
            int level = number(parts[parts.length - 1]);
            String path = parts[parts.length - 2];
            return words(path) + (level > 0 ? " " + roman(level) : "");
        }
        if (parts.length >= 2) {
            String path = parts[1].contains("/") ? parts[1] : parts.length >= 3 && parts[1].equals("minecraft") ? parts[2] : parts[1];
            if ("minecraft".equals(parts[1]) && parts.length >= 3) {
                path = parts[2];
            }
            return words(path);
        }
        return words(tradeId);
    }

    public static String roman(int level) {
        if (level > 0 && level < ROMAN.length) {
            return ROMAN[level];
        }
        return Integer.toString(level);
    }

    public static String tierName(int level) {
        return switch (level) {
            case 2 -> "Apprentice";
            case 3 -> "Journeyman";
            case 4 -> "Expert";
            case 5 -> "Master";
            default -> "Novice";
        };
    }

    private static String words(String path) {
        String[] bits = path.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String bit : bits) {
            if (bit.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            if ("of".equals(bit) || "the".equals(bit)) {
                builder.append(bit);
            } else {
                builder.append(Character.toUpperCase(bit.charAt(0)));
                if (bit.length() > 1) {
                    builder.append(bit.substring(1).toLowerCase(Locale.ROOT));
                }
            }
        }
        return builder.toString();
    }

    private static int number(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Entry entry(JsonObject object) {
        List<LockedOffer> trades = new ArrayList<>();
        JsonElement tradesElement = object.get("trades");
        if (tradesElement != null && tradesElement.isJsonArray()) {
            for (JsonElement element : tradesElement.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    trades.add(offer(element.getAsJsonObject()));
                }
            }
        }
        return new Entry(
                text(object, "uuid"),
                text(object, "dimension"),
                number(object, "x"),
                number(object, "y"),
                number(object, "z"),
                text(object, "customName"),
                text(object, "profession"),
                number(object, "level"),
                object.has("updatedAt") ? object.get("updatedAt").getAsLong() : 0L,
                List.copyOf(trades)
        );
    }

    private static LockedOffer offer(JsonObject object) {
        return new LockedOffer(
                text(object, "tradeId"),
                text(object, "displayName"),
                text(object, "enchantmentId"),
                number(object, "enchantmentLevel"),
                text(object, "itemId"),
                number(object, "itemCount"),
                number(object, "emeraldCost"),
                number(object, "effectiveCost"),
                object.has("paysEmeralds") && object.get("paysEmeralds").getAsBoolean()
        );
    }

    private static String text(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        return element.getAsString();
    }

    private static int number(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return 0;
        }
        return element.getAsInt();
    }

    private String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        JsonObject worldsObject = new JsonObject();
        for (Map.Entry<String, List<Entry>> world : worlds.entrySet()) {
            JsonArray array = new JsonArray();
            for (Entry entry : world.getValue()) {
                array.add(entryJson(entry));
            }
            worldsObject.add(world.getKey(), array);
        }
        root.add("worlds", worldsObject);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n";
    }

    private static JsonObject entryJson(Entry entry) {
        JsonObject object = new JsonObject();
        object.addProperty("uuid", entry.uuid);
        object.addProperty("dimension", entry.dimension);
        object.addProperty("x", entry.x);
        object.addProperty("y", entry.y);
        object.addProperty("z", entry.z);
        object.addProperty("customName", entry.customName);
        object.addProperty("profession", entry.profession);
        object.addProperty("level", entry.level);
        object.addProperty("updatedAt", entry.updatedAt);
        JsonArray trades = new JsonArray();
        for (LockedOffer offer : entry.trades) {
            JsonObject trade = new JsonObject();
            trade.addProperty("tradeId", offer.tradeId);
            trade.addProperty("displayName", offer.displayName);
            trade.addProperty("enchantmentId", offer.enchantmentId);
            trade.addProperty("enchantmentLevel", offer.enchantmentLevel);
            trade.addProperty("itemId", offer.itemId);
            trade.addProperty("itemCount", offer.itemCount);
            trade.addProperty("emeraldCost", offer.emeraldCost);
            trade.addProperty("effectiveCost", offer.effectiveCost);
            trade.addProperty("paysEmeralds", offer.paysEmeralds);
            trades.add(trade);
        }
        object.add("trades", trades);
        return object;
    }
}
