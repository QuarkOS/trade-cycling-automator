package dev.quarkos.villagerreroller.text;

import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

public final class TextFit {
    private static final String ELLIPSIS = "…";

    private TextFit() {
    }

    public static String ellipsis(String text, int maxWidth, ToIntFunction<String> width,
                                 BiFunction<String, Integer, String> prefix) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (width.applyAsInt(text) <= maxWidth) {
            return text;
        }
        int budget = maxWidth - width.applyAsInt(ELLIPSIS);
        if (budget < 0) {
            return "";
        }
        return budget == 0 ? ELLIPSIS : prefix.apply(text, budget) + ELLIPSIS;
    }
}
