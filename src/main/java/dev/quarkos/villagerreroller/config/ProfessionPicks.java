package dev.quarkos.villagerreroller.config;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Emerald ceilings the player chose, kept per profession.
 * A trade id that is not on the open catalog stays in the file, so a different
 * level of the same profession does not erase the other level's picks.
 */
public final class ProfessionPicks {
    static final int VERSION = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");

    private final Map<String, Map<String, Integer>> professions = new LinkedHashMap<>();
    private boolean readable = true;

    public static ProfessionPicks load(Path path) {
        ProfessionPicks picks = new ProfessionPicks();
        if (path == null || !Files.isRegularFile(path)) {
            return picks;
        }
        String text;
        try {
            text = Files.readString(path);
        } catch (IOException e) {
            LOGGER.error("Could not read {}", path, e);
            return unreadable();
        }
        if (text.isBlank()) {
            return picks;
        }
        try {
            JsonElement root = JsonParser.parseString(text);
            if (!root.isJsonObject()) {
                LOGGER.error("Picks file {} is not a JSON object", path);
                return unreadable();
            }
            JsonElement body = root.getAsJsonObject().get("professions");
            if (body == null || body.isJsonNull()) {
                return picks;
            }
            if (!body.isJsonObject()) {
                LOGGER.error("Picks file {} has no professions object", path);
                return unreadable();
            }
            for (Map.Entry<String, JsonElement> profession : body.getAsJsonObject().entrySet()) {
                if (!profession.getValue().isJsonObject()) {
                    LOGGER.error("Picks file {} has a bad entry for {}", path, profession.getKey());
                    return unreadable();
                }
                Map<String, Integer> trades = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> trade : profession.getValue().getAsJsonObject().entrySet()) {
                    JsonElement price = trade.getValue();
                    if (!price.isJsonPrimitive() || !price.getAsJsonPrimitive().isNumber()) {
                        LOGGER.error("Picks file {} has a bad price for {}", path, trade.getKey());
                        return unreadable();
                    }
                    int value = price.getAsInt();
                    if (value > 0) {
                        trades.put(trade.getKey(), value);
                    }
                }
                if (!trades.isEmpty()) {
                    picks.professions.put(profession.getKey(), trades);
                }
            }
            return picks;
        } catch (RuntimeException e) {
            LOGGER.error("Could not read {}", path, e);
            return unreadable();
        }
    }

    private static ProfessionPicks unreadable() {
        ProfessionPicks picks = new ProfessionPicks();
        picks.readable = false;
        return picks;
    }

    public boolean readable() {
        return readable;
    }

    public Map<String, Map<String, Integer>> snapshot() {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : professions.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return copy;
    }

    public Map<String, Integer> picks(String profession) {
        Map<String, Integer> found = professions.get(profession);
        if (found == null || found.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(found);
    }

    /**
     * Replaces saved prices for {@code catalogIds} with {@code selected}.
     * Prices for other trades of this profession, and every other profession, stay.
     */
    public void replaceCatalog(String profession, Set<String> catalogIds, Map<String, Integer> selected) {
        if (!readable || profession == null || profession.isEmpty() || "unknown".equals(profession)) {
            return;
        }
        Map<String, Integer> stored = professions.computeIfAbsent(profession, key -> new LinkedHashMap<>());
        if (catalogIds != null) {
            stored.keySet().removeIf(catalogIds::contains);
        }
        if (selected != null && catalogIds != null) {
            for (Map.Entry<String, Integer> entry : selected.entrySet()) {
                Integer price = entry.getValue();
                if (!catalogIds.contains(entry.getKey()) || price == null || price <= 0) {
                    continue;
                }
                stored.put(entry.getKey(), price);
            }
        }
        if (stored.isEmpty()) {
            professions.remove(profession);
        }
    }

    public boolean save(Path path) {
        if (path == null) {
            return false;
        }
        if (!readable) {
            LOGGER.error("Refusing to overwrite unreadable picks file {}", path);
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
                // The temp file is the only leftover. The picks file is unchanged.
            }
            return false;
        }
    }

    /**
     * @return the ceiling to show for a saved price, or {@code 0} when the row should stay unselected
     */
    public static int fitPrice(int saved, int emeraldMin, int emeraldMax, boolean paysEmeralds) {
        if (saved <= 0) {
            return 0;
        }
        if (!paysEmeralds || saved == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (saved < emeraldMin) {
            return emeraldMin;
        }
        if (saved > emeraldMax) {
            return emeraldMax;
        }
        return saved;
    }

    private String toJson() {
        StringBuilder out = new StringBuilder();
        out.append("{\n  \"version\": ").append(VERSION).append(",\n  \"professions\": {");
        boolean anyProfession = false;
        for (Map.Entry<String, Map<String, Integer>> profession : professions.entrySet()) {
            if (profession.getValue().isEmpty()) {
                continue;
            }
            if (anyProfession) {
                out.append(',');
            }
            anyProfession = true;
            out.append("\n    \"").append(escape(profession.getKey())).append("\": {");
            boolean anyTrade = false;
            for (Map.Entry<String, Integer> trade : profession.getValue().entrySet()) {
                if (anyTrade) {
                    out.append(',');
                }
                anyTrade = true;
                out.append("\n      \"").append(escape(trade.getKey())).append("\": ").append(trade.getValue().intValue());
            }
            if (anyTrade) {
                out.append('\n');
            }
            out.append("    }");
        }
        if (anyProfession) {
            out.append('\n');
        }
        out.append("  }\n}\n");
        return out.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
