# Librarian-only stop

## Sub-features

- `UseEntityCallback` in `TradeCyclingAutomator` calls `LibrarianTracker.remember` on the client.
- `remember` stores the entity id when `McBridge.isLibrarian` is true. If the entity is a `Merchant` and not a librarian, it clears the id. Other entities are ignored.
- `McBridge.isLibrarian` on 1.21.11 requires `net.minecraft.world.entity.npc.villager.Villager` whose profession is `VillagerProfession.LIBRARIAN`. The 1.21.1 branch uses `VillagerData.getProfession()`.
- Each `CycleAutomator.tick`, if `LibrarianTracker.currentIsLibrarian` is false, the run stops with `NOT_LIBRARIAN` ("Stopped because this is not a librarian").
- `currentIsLibrarian` also requires the client level, a remembered id, and a living entity that is still a librarian.
- The trade menu's merchant is a `ClientSideMerchant`. The profession check uses the last entity the player used, not the menu.

## How to get to it (user POV)

Use a villager. If that villager is a librarian, automation may keep cycling while their trade screen is open. If the last merchant you used is not a librarian, or the remembered librarian is gone, cycling stops and the action bar says this is not a librarian. Closing the trade screen stops for a different reason (`GUI_CLOSED`). A villager who already has trade experience stops with `CANNOT_CYCLE` after `CycleTradesButton.canCycle` returns false.

## Driving it with Gradle JUnit

No test builds a villager, a level, or a `MerchantScreen`. `isLibrarian` and `currentIsLibrarian` are not called from `src/test`.

```bash
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh drive librarian-only-stop
```

That exits 2. There is no selector for a villager in this repo.

## Gotchas

- Interacting with a non-merchant does not clear a previously remembered librarian.
- An empty remembered id (`Integer.MIN_VALUE`) fails the librarian check, so a trade screen opened with no prior librarian use stops on the first tick.
- `NOT_LIBRARIAN` is checked before offers, the target book, and the attempt cap.
