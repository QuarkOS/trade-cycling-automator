package dev.quarkos.villagerreroller.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
//? if forge {
/*import net.minecraftforge.fml.loading.FMLPaths;
*///?} else {
import net.fabricmc.loader.api.FabricLoader;
//?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AutomatorConfig {
    public static final int NORMAL_TICKS = 8;
    public static final int FRIENDLY_TICKS = 20;
    public static final int NORMAL_TIMEOUT = 1;
    public static final int FRIENDLY_TIMEOUT = 2;
    public static final float NORMAL_LOOK = 30.0F;
    public static final float FRIENDLY_LOOK = 8.0F;
    public static final int DEFAULT_DURABILITY_FLOOR = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AutomatorConfig instance;

    private boolean enabled = true;
    private int maxAttempts = 500;
    private int durabilityFloor = DEFAULT_DURABILITY_FLOOR;
    private boolean allowBareHand = true;
    private boolean serverFriendly = false;
    private int ticksBetweenCycles = NORMAL_TICKS;
    private int timeoutMultiplier = NORMAL_TIMEOUT;
    private float lookDegreesPerTick = NORMAL_LOOK;

    public static AutomatorConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(get().toJson(), writer);
            }
        } catch (IOException e) {
            LOGGER.error("Could not save {}", path, e);
        }
    }

    /**
     * Reads the current keys and drops the legacy enchantment, level, and maxPrice
     * fields. Missing keys keep the defaults, so an older file still loads.
     */
    public static AutomatorConfig parse(Reader reader) {
        JsonElement parsed = JsonParser.parseReader(reader);
        if (parsed == null || !parsed.isJsonObject()) {
            return new AutomatorConfig();
        }
        JsonObject object = parsed.getAsJsonObject();
        AutomatorConfig config = new AutomatorConfig();
        if (bool(object, "enabled") != null) {
            config.enabled = bool(object, "enabled");
        }
        if (integer(object, "maxAttempts") != null) {
            config.maxAttempts = Math.max(1, integer(object, "maxAttempts"));
        }
        if (integer(object, "durabilityFloor") != null) {
            config.durabilityFloor = Math.max(0, integer(object, "durabilityFloor"));
        }
        if (bool(object, "allowBareHand") != null) {
            config.allowBareHand = bool(object, "allowBareHand");
        }
        if (bool(object, "serverFriendly") != null) {
            config.serverFriendly = bool(object, "serverFriendly");
        }
        if (integer(object, "ticksBetweenCycles") != null) {
            config.ticksBetweenCycles = Math.max(1, integer(object, "ticksBetweenCycles"));
        }
        if (integer(object, "timeoutMultiplier") != null) {
            config.timeoutMultiplier = Math.max(1, integer(object, "timeoutMultiplier"));
        }
        if (decimal(object, "lookDegreesPerTick") != null) {
            config.lookDegreesPerTick = Math.max(1.0F, decimal(object, "lookDegreesPerTick"));
        }
        return config;
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("enabled", enabled);
        object.addProperty("maxAttempts", maxAttempts);
        object.addProperty("durabilityFloor", durabilityFloor);
        object.addProperty("allowBareHand", allowBareHand);
        object.addProperty("serverFriendly", serverFriendly);
        object.addProperty("ticksBetweenCycles", ticksBetweenCycles);
        object.addProperty("timeoutMultiplier", timeoutMultiplier);
        object.addProperty("lookDegreesPerTick", lookDegreesPerTick);
        return object;
    }

    public void applyServerFriendly(boolean enabled) {
        this.serverFriendly = enabled;
        if (enabled) {
            ticksBetweenCycles = FRIENDLY_TICKS;
            timeoutMultiplier = FRIENDLY_TIMEOUT;
            lookDegreesPerTick = FRIENDLY_LOOK;
        } else {
            ticksBetweenCycles = NORMAL_TICKS;
            timeoutMultiplier = NORMAL_TIMEOUT;
            lookDegreesPerTick = NORMAL_LOOK;
        }
    }

    private static AutomatorConfig load() {
        Path path = path();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                return parse(reader);
            } catch (RuntimeException | IOException e) {
                LOGGER.error("Could not read {}", path, e);
            }
        }
        AutomatorConfig config = new AutomatorConfig();
        instance = config;
        save();
        return config;
    }

    private static Path path() {
        //? if forge {
        /*return FMLPaths.CONFIGDIR.get().resolve("villager_reroller.json");
        *///?} else {
        return FabricLoader.getInstance().getConfigDir().resolve("villager_reroller.json");
        //?}
    }

    private static Boolean bool(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        return element.getAsBoolean();
    }

    private static Integer integer(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        return element.getAsInt();
    }

    private static Float decimal(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        return element.getAsFloat();
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public int durabilityFloor() {
        return durabilityFloor;
    }

    public void setDurabilityFloor(int durabilityFloor) {
        this.durabilityFloor = Math.max(0, durabilityFloor);
    }

    public boolean allowBareHand() {
        return allowBareHand;
    }

    public void setAllowBareHand(boolean allowBareHand) {
        this.allowBareHand = allowBareHand;
    }

    public boolean serverFriendly() {
        return serverFriendly;
    }

    public void setServerFriendly(boolean serverFriendly) {
        this.serverFriendly = serverFriendly;
    }

    public int ticksBetweenCycles() {
        return Math.max(1, ticksBetweenCycles);
    }

    public void setTicksBetweenCycles(int ticksBetweenCycles) {
        this.ticksBetweenCycles = Math.max(1, ticksBetweenCycles);
    }

    public int timeoutMultiplier() {
        return Math.max(1, timeoutMultiplier);
    }

    public void setTimeoutMultiplier(int timeoutMultiplier) {
        this.timeoutMultiplier = Math.max(1, timeoutMultiplier);
    }

    public float lookDegreesPerTick() {
        return Math.max(1.0F, lookDegreesPerTick);
    }

    public void setLookDegreesPerTick(float lookDegreesPerTick) {
        this.lookDegreesPerTick = Math.max(1.0F, lookDegreesPerTick);
    }
}
