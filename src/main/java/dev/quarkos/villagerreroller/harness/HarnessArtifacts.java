package dev.quarkos.villagerreroller.harness;

import java.nio.file.Path;

/** Output directory for gametests and the standalone client harnesses. */
public final class HarnessArtifacts {
    private HarnessArtifacts() {
    }

    public static Path directory() {
        String path = System.getProperty("villager_reroller.artifacts");
        return path == null || path.isBlank() ? Path.of(System.getProperty("user.dir"), "artifacts") : Path.of(path);
    }
}
