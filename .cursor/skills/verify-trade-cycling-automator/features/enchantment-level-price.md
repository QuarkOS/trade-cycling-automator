# Enchantment, level, and max price

## Sub-features

- Enchantment selector. Default `minecraft:mending`. Options are the ids in `librarian_book_prices.json` after the version predicate (`LibrarianBookPrices.get`).
- Level slider from 1 to 5. The error supplier calls `LibrarianBookPrices.validateLevel`. A level above that enchantment's `maxLevel` is `LEVEL_TOO_HIGH`.
- Maximum emeralds slider from 5 to 64. Default 10. The error supplier calls `LibrarianBookPrices.validate` with the enchantment and level currently on the screen.
- Enabled toggle. Default true. Turning it off calls `CycleAutomator.stop(DISABLED)`.
- Attempt cap field. Default 500, screen bounds 1 to 10000. See [attempt-cap.md](attempt-cap.md).
- Panic stop. The toggle always opens as false and is not a field on `AutomatorConfig`. Saving it on calls `CycleAutomator.stop(PANIC)`.
- `ensureListedEnchantment` replaces a saved id that is not in this version's list (for example `minecraft:lunge` on 1.21.1) with the first listed enchantment, level 1, and that row's wiki maximum.

## How to get to it (user POV)

Install Fabric, Fabric API, Trade Cycling, Cloth Config, and Mod Menu. Open Mod Menu. The addon is listed under Trade Cycling (`fabric.mod.json` sets `custom.modmenu.parent` to `trade_cycling`). The screen title is "Automator for Trade Cycling". The category is "Librarian book". Saving writes `trade-cycling-automator.json` under the Fabric config directory.

## Driving it with Gradle JUnit

The screen is not constructed by any test. `AutomatorConfigScreen.create` needs Cloth Config and a Minecraft `Screen`. `AutomatorConfig` load and save need `FabricLoader`.

```bash
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh drive enchantment-level-price
```

That exits 2. Do not report the config screen as verified.

The numbers the sliders accept or reject are `LibrarianBookPrices.validate`, which [wiki-price-rejection.md](wiki-price-rejection.md) drives. That test does not save a config file and does not open Mod Menu.

## Gotchas

- The price slider is clamped to 5–64 before it is shown, so a stored value outside that range is not what the slider displays.
- The level slider is clamped to 1–5. `LEVEL_TOO_LOW` is still a `validateLevel` result, but the slider cannot send 0.
- The selector's default value in code is `minecraft:mending`. Mending is a treasure book, so its legal prices are the treasure row (10–38 at level I), not the normal row (5–19).
- Impossible combinations can sit in the widgets until save. `CycleAutomator.start` checks `config.problem()` and will not start while `validate` returns a problem.
