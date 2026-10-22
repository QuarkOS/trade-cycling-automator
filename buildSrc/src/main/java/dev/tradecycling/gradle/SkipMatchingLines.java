package dev.tradecycling.gradle;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Drops lines that contain {@code needles}. Used so fabric.mod.json can differ
 * per Minecraft version without capturing the Gradle script in the configuration cache.
 */
public final class SkipMatchingLines extends FilterReader {
    private String needles = "";
    private final BufferedReader lines;
    private String pending = "";
    private int index = 0;

    public SkipMatchingLines(Reader reader) {
        super(reader);
        this.lines = new BufferedReader(reader);
    }

    public void setNeedles(String needles) {
        this.needles = needles == null ? "" : needles;
    }

    @Override
    public int read() throws IOException {
        if (index >= pending.length()) {
            String line = lines.readLine();
            while (line != null && !needles.isEmpty() && line.contains(needles)) {
                line = lines.readLine();
            }
            if (line == null) {
                pending = "";
                return -1;
            }
            pending = line + "\n";
            index = 0;
        }
        return pending.charAt(index++);
    }
}
