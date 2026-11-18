package dev.quarkos.villagerreroller.gametest;

import dev.quarkos.villagerreroller.harness.HarnessArtifacts;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
final class GameTestSupport {
    private GameTestSupport() {
    }

    static Path artifactsDir() {
        return HarnessArtifacts.directory();
    }

    static void saveShot(ClientGameTestContext context, String name) {
        Path shot = context.takeScreenshot(name);
        try {
            Path dir = artifactsDir();
            Files.createDirectories(dir);
            Files.copy(shot, dir.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static int scaled(int ticks) {
        int scale = Math.max(1, Integer.getInteger("villager_reroller.timeoutScale", 1));
        return (int) Math.min(Integer.MAX_VALUE, (long) ticks * scale);
    }

    static void waitForServer(ClientGameTestContext context, TestSingleplayerContext world, int ticks,
                              Predicate<MinecraftServer> condition, String description) {
        int firstTick = world.getServer().computeOnServer(MinecraftServer::getTickCount);
        int lastTick = firstTick;
        int idleTicks = 0;
        while (lastTick - firstTick <= ticks && idleTicks < scaled(20 * 15)) {
            int serverTick = world.getServer().computeOnServer(server -> condition.test(server) ? -1 : server.getTickCount());
            if (serverTick == -1) {
                return;
            }
            idleTicks = serverTick == lastTick ? idleTicks + 2 : 0;
            lastTick = serverTick;
            context.waitTicks(2);
        }
        throw new AssertionError(description + " after " + (lastTick - firstTick) + " server ticks");
    }

    static void waitForChunks(TestSingleplayerContext world) {
        int timeout = scaled(20 * 45);
        //? if >=26.2 {
        /*world.getConnection().waitForChunksRender(timeout);
        *///?} else {
        //? if >=26.1 {
        /*world.getClientLevel().waitForChunksRender(timeout);
        *///?} else {
        world.getClientWorld().waitForChunksRender(timeout);
        //?}
        //?}
    }
}
