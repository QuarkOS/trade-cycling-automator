//? if >=26.1 {
/*package dev.quarkos.villagerreroller.picker;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ClientTradeRegistries {
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");
    private static final Map<Boolean, Optional<RegistryAccess>> VANILLA = new HashMap<>();

    private ClientTradeRegistries() {
    }

    static RegistryAccess get(Level level) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            return server.registryAccess();
        }
        RegistryAccess synced = level.registryAccess();
        if (synced.lookup(Registries.TRADE_SET).isPresent()) {
            return synced;
        }

        // Vanilla servers keep trade sets server-side. Read this client's built-in data packs.
        boolean rebalance = level.enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE);
        return VANILLA.computeIfAbsent(rebalance, ClientTradeRegistries::loadVanilla).orElse(synced);
    }

    private static Optional<RegistryAccess> loadVanilla(boolean rebalance) {
        var repository = ServerPacksSource.createVanillaTrustedRepository();
        repository.reload();
        repository.setSelected(rebalance ? List.of("vanilla", "trade_rebalance") : List.of("vanilla"));
        var layers = RegistryLayer.createRegistryAccess();
        //? if >=26.3 {
        var layer = RegistryLayer.WORLD;
        var definitions = RegistryDataLoader.WORLD_REGISTRIES;
        //?} else {
        var layer = RegistryLayer.WORLDGEN;
        var definitions = RegistryDataLoader.WORLDGEN_REGISTRIES;
        //?}
        try (var resources = new MultiPackResourceManager(PackType.SERVER_DATA, repository.openAllSelected())) {
            var statics = layers.getAccessForLoading(layer);
            var tags = TagLoader.loadTagsForExistingRegistries(resources, statics);
            var lookups = TagLoader.buildUpdatedLookups(statics, tags);
            var world = RegistryDataLoader.load(resources, lookups, definitions, Runnable::run).join();
            return Optional.of(layers.replaceFrom(layer, world).compositeAccess());
        } catch (RuntimeException error) {
            LOGGER.warn("Could not load the built-in villager trade sets", error);
            return Optional.empty();
        }
    }
}
*///?}
