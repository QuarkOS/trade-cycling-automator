package dev.quarkos.villagerreroller.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.quarkos.villagerreroller.net.RerollCodec;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dedicated-server admin config. The client mod does not read this file.
 * {@code cost} is {@code free}, {@code emeralds}, or {@code xp}.
 */
public final class ServerSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean enabled = true;
    private String cost = "free";
    private int costAmount = 1;
    private int cooldownTicks = 1;
    private String permission = "villager_reroller.reroll";
    private boolean requirePermission = false;
    private int opLevel = 2;
    private boolean onlyFreshVillagers = true;
    private double maxDistance = 8.0;

    public static ServerSettings defaults() {
        return new ServerSettings().sanitize();
    }

    public static ServerSettings load(Path path) {
        if (path != null && Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                ServerSettings settings = GSON.fromJson(reader, ServerSettings.class);
                if (settings != null) {
                    return settings.sanitize();
                }
            } catch (IOException ignored) {
                // Fall through and rewrite a valid file.
            }
        }
        ServerSettings settings = defaults();
        settings.save(path);
        return settings;
    }

    public void save(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
            // The in-memory defaults still apply for this boot.
        }
    }

    public ServerSettings sanitize() {
        if (cost == null) {
            cost = "free";
        }
        cost = switch (cost.trim().toLowerCase()) {
            case "emeralds", "emerald" -> "emeralds";
            case "xp", "experience" -> "xp";
            default -> "free";
        };
        if (costAmount < 0) {
            costAmount = 0;
        }
        if (costAmount > 100000) {
            costAmount = 100000;
        }
        if (cooldownTicks < 0) {
            cooldownTicks = 0;
        }
        if (cooldownTicks > 20 * 60 * 30) {
            cooldownTicks = 20 * 60 * 30;
        }
        if (permission == null || permission.isBlank()) {
            permission = "villager_reroller.reroll";
        }
        permission = permission.trim();
        if (opLevel < 0) {
            opLevel = 0;
        }
        if (opLevel > 4) {
            opLevel = 4;
        }
        if (maxDistance < 1.0) {
            maxDistance = 1.0;
        }
        if (maxDistance > 64.0) {
            maxDistance = 64.0;
        }
        return this;
    }

    public boolean enabled() {
        return enabled;
    }

    public String cost() {
        return cost;
    }

    public int costKind() {
        return RerollCodec.costKind(cost);
    }

    public int costAmount() {
        return costAmount;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public String permission() {
        return permission;
    }

    public boolean requirePermission() {
        return requirePermission;
    }

    public int opLevel() {
        return opLevel;
    }

    public boolean onlyFreshVillagers() {
        return onlyFreshVillagers;
    }

    public double maxDistance() {
        return maxDistance;
    }

    public RerollCodec.Hello hello() {
        int milli = (int) Math.round(maxDistance * 1000.0);
        return new RerollCodec.Hello(costKind(), costAmount, Math.max(1, cooldownTicks), onlyFreshVillagers, milli);
    }
}
