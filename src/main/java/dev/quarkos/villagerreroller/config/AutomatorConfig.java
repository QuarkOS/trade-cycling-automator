package dev.quarkos.villagerreroller.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.quarkos.villagerreroller.price.LibrarianBookPrices;
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
import java.util.Optional;

public final class AutomatorConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AutomatorConfig instance;

    private boolean enabled = true;
    private String enchantment = "minecraft:mending";
    private int level = 1;
    private int maxPrice = 10;
    private int maxAttempts = 500;

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
                GSON.toJson(get(), writer);
            }
        } catch (IOException e) {
            LOGGER.error("Could not save {}", path, e);
        }
    }

    private static AutomatorConfig load() {
        Path path = path();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                AutomatorConfig config = GSON.fromJson(reader, AutomatorConfig.class);
                if (config != null) {
                    config.maxAttempts = Math.max(1, config.maxAttempts);
                    return config;
                }
            } catch (IOException e) {
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

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String enchantment() {
        return enchantment;
    }

    public void setEnchantment(String enchantment) {
        this.enchantment = enchantment;
    }

    public int level() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int maxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(int maxPrice) {
        this.maxPrice = maxPrice;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public Optional<LibrarianBookPrices.Problem> problem() {
        return LibrarianBookPrices.get().validate(enchantment, level, maxPrice);
    }

    /**
     * Picks a listed enchantment so the config screen can open after a version change.
     */
    public void ensureListedEnchantment() {
        LibrarianBookPrices prices = LibrarianBookPrices.get();
        if (prices.enchantment(enchantment).isPresent()) {
            return;
        }
        LibrarianBookPrices.BookEnchantment fallback = prices.enchantments().iterator().next();
        enchantment = fallback.id();
        level = 1;
        maxPrice = prices.priceRange(enchantment, 1).orElseThrow().max();
    }
}
