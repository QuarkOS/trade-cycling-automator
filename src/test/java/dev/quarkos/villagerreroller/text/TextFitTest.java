package dev.quarkos.villagerreroller.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFitTest {
    @Test
    void keepsLabelsThatAlreadyFit() {
        assertEquals("Save", fit("Save", width("Save")));
        assertEquals("", fit(null, 10));
    }

    @Test
    void usesTheEllipsisOnlyWhenTheWholeGlyphFits() {
        assertEquals("", fit("Start", 2));
        assertEquals("…", fit("Start", 3));
        assertEquals("S…", fit("Start", 7));
    }

    @Test
    void longTranslatedLabelsAndCountsStayWithinThePixelBudget() {
        String label = "🔎 Verzauberungsbuch 2 147 483 647";
        assertEquals("🔎…", fit(label, 11));
        for (int budget = 0; budget <= width(label); budget++) {
            assertTrue(width(fit(label, budget)) <= budget);
        }
    }

    private static String fit(String text, int maxWidth) {
        return TextFit.ellipsis(text, maxWidth, TextFitTest::width, TextFitTest::prefix);
    }

    private static int width(String text) {
        return text.codePoints().map(TextFitTest::glyphWidth).sum();
    }

    private static int glyphWidth(int codePoint) {
        return codePoint == '…' ? 3 : Character.isSupplementaryCodePoint(codePoint) ? 8 : 4;
    }

    private static String prefix(String text, int budget) {
        int length = 0;
        while (length < text.length()) {
            int codePoint = text.codePointAt(length);
            budget -= glyphWidth(codePoint);
            if (budget < 0) {
                break;
            }
            length += Character.charCount(codePoint);
        }
        return text.substring(0, length);
    }
}
