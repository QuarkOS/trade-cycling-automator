package dev.quarkos.villagerreroller.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Runs only when a dedicated server is started with
 * {@code -Dvillager_reroller.serverSelfTest=true} or {@code -Dvillager_reroller.proofWorld=true}.
 */
public final class DedicatedProof {
    private static final Logger LOGGER = LoggerFactory.getLogger("villager_reroller");
    private static boolean hooked;

    private DedicatedProof() {
    }

    public static void hook() {
        if (hooked) {
            return;
        }
        hooked = true;
        boolean selfTest = Boolean.getBoolean("villager_reroller.serverSelfTest") || Boolean.getBoolean("villager_reroller.proofWorld");
        boolean proofWorld = Boolean.getBoolean("villager_reroller.proofWorld");
        if (!selfTest && !proofWorld) {
            return;
        }
        Thread thread = new Thread(() -> waitAndRun(selfTest, proofWorld), "villager-reroller-proof");
        thread.setDaemon(true);
        thread.start();
    }

    private static void waitAndRun(boolean selfTest, boolean proofWorld) {
        MinecraftServer server = null;
        for (int i = 0; i < 2400 && server == null; i++) {
            server = current();
            sleep();
        }
        if (server == null) {
            LOGGER.error("villager_reroller SERVER_SELFTEST FAIL no server");
            return;
        }
        MinecraftServer ready = server;
        // The server thread drains its queue immediately, so a requeued task
        // would spin before the first tick. Wait out here, then run once.
        for (int i = 0; i < 20; i++) {
            sleep();
        }
        ready.execute(() -> {
            if (selfTest) {
                selfTest(ready);
            }
            if (proofWorld) {
                prepare(ready);
            }
        });
        if (proofWorld) {
            // give @a runs before the harness joins. Repeat it until the player is online.
            for (int i = 0; i < 180; i++) {
                sleep();
                sleep();
                sleep();
                sleep();
                ready.execute(() -> supply(ready));
            }
        }
    }

    private static void supply(MinecraftServer server) {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        command(server, "gamemode survival @a");
        command(server, "give @a minecraft:lectern 16");
        command(server, "give @a minecraft:iron_axe");
    }

    private static void selfTest(MinecraftServer server) {
        try {
            Level level = server.overworld();
            if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
                LOGGER.error("villager_reroller SERVER_SELFTEST FAIL no overworld");
                return;
            }
            //? if >=26.2 {
            /*Villager villager = new Villager(net.minecraft.world.entity.EntityTypes.VILLAGER, serverLevel);
            *///?} else {
            Villager villager = new Villager(EntityType.VILLAGER, serverLevel);
            //?}
            BlockPos spawn = spawnPos(serverLevel);
            villager.setPos(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            applyLibrarian(serverLevel, villager);
            serverLevel.addFreshEntity(villager);
            MerchantOffers before = villager.getOffers();
            boolean hadOffers = before != null && !before.isEmpty();
            boolean regenerated = false;
            if (hadOffers) {
                villager.setOffers(null);
                MerchantOffers after = villager.getOffers();
                regenerated = after != null && after != before && !after.isEmpty();
            }
            boolean freshLocked = RerollRules.locked(true, 1, 0, false);
            boolean usedLocked = RerollRules.locked(true, 1, 0, true);
            boolean levelLocked = RerollRules.locked(true, 2, 0, false);
            boolean distance = RerollRules.tooFar(true, 81.0, 8.0) && !RerollRules.tooFar(true, 1.0, 8.0) && RerollRules.tooFar(false, 1.0, 8.0);
            villager.discard();
            boolean pass = hadOffers && regenerated && !freshLocked && usedLocked && levelLocked && distance;
            LOGGER.info(
                    "villager_reroller SERVER_SELFTEST offers={} regenerated={} rules={} {}",
                    hadOffers,
                    regenerated,
                    distance && usedLocked && levelLocked && !freshLocked,
                    pass ? "PASS" : "FAIL");
            if (!pass) {
                LOGGER.error("villager_reroller SERVER_SELFTEST FAIL");
            }
        } catch (RuntimeException ex) {
            LOGGER.error("villager_reroller SERVER_SELFTEST FAIL {}", ex.toString());
        }
    }

    private static void applyLibrarian(net.minecraft.server.level.ServerLevel level, Villager villager) {
        //? if <1.21.5 {
        /*villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.LIBRARIAN));
        *///?} else {
        villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.LIBRARIAN));
        //?}
    }

    private static void prepare(MinecraftServer server) {
        command(server, "gamerule doMobSpawning false");
        command(server, "gamerule doDaylightCycle false");
        command(server, "gamerule sendCommandFeedback false");
        supply(server);
        net.minecraft.server.level.ServerLevel level = server.overworld();
        BlockPos spawn = spawnPos(level);
        int x = spawn.getX() + 2;
        int z = spawn.getZ();
        level.getChunk(x >> 4, z >> 4);
        int y = level.getMinY();
        for (int scan = level.getMaxY() - 1; scan > level.getMinY(); scan--) {
            if (!level.getBlockState(new BlockPos(x, scan, z)).isAir()) {
                y = scan + 1;
                break;
            }
        }
        command(server, "setblock " + x + " " + y + " " + z + " minecraft:lectern");
        command(server, "summon minecraft:villager " + (x) + " " + y + " " + (z + 1)
                + " {PersistenceRequired:1b}");
        LOGGER.info("villager_reroller PROOF_WORLD_READY {} {} {}", x, y, z + 1);
    }

    private static BlockPos spawnPos(net.minecraft.server.level.ServerLevel level) {
        //? if >=1.21.9 {
        return level.getRespawnData().pos();
        //?}
        //? if >=1.20.5 && <1.21.9 {
        /*return level.getLevelData().getSpawnPos();
        *///?}
        //? if <1.20.5 {
        /*net.minecraft.world.level.storage.LevelData data = level.getLevelData();
        return new BlockPos(data.getXSpawn(), data.getYSpawn(), data.getZSpawn());
        *///?}
    }

    private static void command(MinecraftServer server, String command) {
        try {
            Object commands = server.getCommands();
            Object source = server.createCommandSourceStack();
            for (Method method : commands.getClass().getMethods()) {
                if (method.getName().equals("performPrefixedCommand") && method.getParameterCount() == 2) {
                    method.invoke(commands, source, command);
                    return;
                }
            }
            LOGGER.warn("villager_reroller proof command missing performer: {}", command);
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("villager_reroller proof command failed: {} {}", command, ex.getCause() == null ? ex.toString() : ex.getCause().toString());
        }
    }

    private static MinecraftServer current() {
        try {
            Class<?> holder = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = holder.getMethod("getInstance").invoke(null);
            Object game = loader.getClass().getMethod("getGameInstance").invoke(loader);
            if (game instanceof MinecraftServer server) {
                return server;
            }
        } catch (ReflectiveOperationException ignored) {
            // Forge exposes the server through a different hook.
        }
        try {
            Class<?> serverType = Class.forName("net.minecraftforge.server.ServerLifecycleHooks");
            Object server = serverType.getMethod("getCurrentServer").invoke(null);
            if (server instanceof MinecraftServer minecraft) {
                return minecraft;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    private static void sleep() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
