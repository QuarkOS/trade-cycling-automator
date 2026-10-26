package dev.quarkos.villagerreroller.picker;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolGuardTest {
    @Test
    void skipsToolsAtOrBelowTheFloorAndKeepsTheFasterOne() {
        ToolGuard.Choice choice = ToolGuard.choose(List.of(
                tool(0, 8.0F, 6, false, false),
                tool(1, 2.0F, 59, false, false)
        ), 10, true, true);
        assertEquals(ToolGuard.Decision.TOOL, choice.decision());
        assertEquals(1, choice.slot());
    }

    @Test
    void equalSpeedPrefersUnenchantedThenNonMending() {
        ToolGuard.Choice mendingLoses = ToolGuard.choose(List.of(
                tool(0, 6.0F, 250, true, true),
                tool(1, 6.0F, 250, false, false)
        ), 10, false, false);
        assertEquals(1, mendingLoses.slot());

        ToolGuard.Choice plainEnchantLosesToBare = ToolGuard.choose(List.of(
                tool(3, 6.0F, 40, true, false),
                tool(4, 6.0F, 40, false, false)
        ), 10, false, false);
        assertEquals(4, plainEnchantLosesToBare.slot());

        ToolGuard.Choice mendingLosesToUnbreaking = ToolGuard.choose(List.of(
                tool(5, 6.0F, 100, true, true),
                tool(6, 6.0F, 80, true, false)
        ), 10, false, false);
        assertEquals(6, mendingLosesToUnbreaking.slot());
    }

    @Test
    void fasterToolStillWinsWhenItIsEnchanted() {
        ToolGuard.Choice choice = ToolGuard.choose(List.of(
                tool(0, 8.0F, 100, true, true),
                tool(1, 2.0F, 59, false, false)
        ), 10, true, true);
        assertEquals(0, choice.slot());
    }

    @Test
    void exactlyTheFloorIsSkippedAndOneAboveIsKept() {
        ToolGuard.Choice worn = ToolGuard.choose(List.of(tool(0, 8.0F, 10, false, false)), 10, false, false);
        assertEquals(ToolGuard.Decision.WORN, worn.decision());

        ToolGuard.Choice kept = ToolGuard.choose(List.of(tool(2, 8.0F, 11, false, false)), 10, false, false);
        assertEquals(ToolGuard.Decision.TOOL, kept.decision());
        assertEquals(2, kept.slot());
    }

    @Test
    void fallsBackToTheHandOnlyWhenThatIsAllowed() {
        List<ToolGuard.Candidate> worn = List.of(tool(0, 8.0F, 4, false, false));
        assertEquals(ToolGuard.Decision.HAND, ToolGuard.choose(worn, 10, true, true).decision());
        assertEquals(ToolGuard.HAND, ToolGuard.choose(worn, 10, true, true).slot());
        assertEquals(ToolGuard.Decision.WORN, ToolGuard.choose(worn, 10, false, true).decision());
        assertEquals(ToolGuard.Decision.WORN, ToolGuard.choose(worn, 10, true, false).decision());
    }

    @Test
    void noToolAtAllAsksForAToolInsteadOfReportingWear() {
        assertEquals(ToolGuard.Decision.NONE, ToolGuard.choose(List.of(), 10, false, true).decision());
        assertEquals(ToolGuard.Decision.HAND, ToolGuard.choose(List.of(), 10, true, true).decision());
        assertEquals(ToolGuard.Decision.NONE, ToolGuard.choose(List.of(tool(0, 1.0F, 100, false, false)), 10, false, false).decision());
    }

    @Test
    void unbreakableToolsIgnoreTheFloor() {
        ToolGuard.Choice choice = ToolGuard.choose(List.of(
                tool(0, 8.0F, Integer.MAX_VALUE, false, false)
        ), 10, false, false);
        assertEquals(0, choice.slot());
    }

    private static ToolGuard.Candidate tool(int slot, float speed, int remaining, boolean enchanted, boolean mending) {
        return new ToolGuard.Candidate(slot, speed, remaining, enchanted, mending);
    }
}
