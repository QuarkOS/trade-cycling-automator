# Wiki price rejection

## Sub-features

- Price rows in `src/main/resources/assets/trade-cycling-automator/librarian_book_prices.json`. Formula after the 64 emerald cap: minimum `2 + 3 × level`, maximum `6 + 13 × level`, doubled for treasure, then capped at 64.
- `LibrarianBookPrices.validate` returns `UNKNOWN_ENCHANTMENT`, `LEVEL_TOO_LOW`, `LEVEL_TOO_HIGH`, `PRICE_BELOW_WIKI_MIN`, or `PRICE_ABOVE_WIKI_MAX`. The `limit` is the bound that was crossed (0 for an unknown id).
- Treasure books librarians sell: `minecraft:binding_curse`, `minecraft:vanishing_curse`, `minecraft:frost_walker`, `minecraft:mending`.
- Not listed: `minecraft:soul_speed`, `minecraft:swift_sneak`, `minecraft:wind_burst`.
- `minecraft:lunge` is present only when the `>=1.21.11` predicate matches. It is not treasure, and its max level is 3.
- The config screen shows the problem as `text.trade-cycling-automator.invalid.<kind>`. `CycleAutomator.start` refuses to run while `AutomatorConfig.problem()` is present.

## How to get to it (user POV)

On the Mod Menu screen, set an enchantment, a level, and a maximum emerald count, then save. A level the enchantment does not have, or a price outside that row, is shown on the control and is not a legal target. In a world, pressing the toggle with an illegal saved combination prints the same invalid line and does not cycle. The live trade check is separate: `CycleAutomator.containsTarget` compares `MerchantOffer.getCostA()` (emeralds after reputation) to the configured maximum. The configured maximum itself still has to sit inside the pre-discount wiki range.

## Driving it with Gradle JUnit

This is the feature the helper runs.

```bash
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh drive wiki-price-rejection
```

Command inside that drive:

```bash
JAVA_HOME=/opt/jdk25 ./gradlew :1.21.11:test --tests dev.tradecycling.automator.price.LibrarianBookPricesTest.rejectsLevelsAndPricesOutsideTheWiki --rerun-tasks
```

`rejectsLevelsAndPricesOutsideTheWiki` loads the table with a predicate that accepts every `minecraft` line, then asserts:

- `minecraft:mending` level 1 at 10 and at 38 are valid
- mending level 2 is `LEVEL_TOO_HIGH`
- mending level 1 at 9 is `PRICE_BELOW_WIKI_MIN` and at 39 is `PRICE_ABOVE_WIKI_MAX`
- `minecraft:efficiency` level 5 at 64 is valid; at 71 the limit is 64
- `minecraft:sharpness` level 5 at 71 is `PRICE_ABOVE_WIKI_MAX`
- `minecraft:protection` level 4 at 14 is valid; at 13 is `PRICE_BELOW_WIKI_MIN`; level 0 is `LEVEL_TOO_LOW`

The same class also has `rangesMatchTheWikiFormulaAfterTheEmeraldCap`, `librariansDoNotSellEveryTreasureEnchantment`, and `lungeIsOnlyListedWhenTheVersionPredicateMatches`. The drive filter does not run those three. `JAVA_HOME=/opt/jdk25 ./gradlew :1.21.11:test` runs all four on the active node. `JAVA_HOME=/opt/jdk25 ./gradlew test` runs them on every version node.

A pass is the copied XML at `.cursor/skills/verify-trade-cycling-automator/evidence/wiki-price-rejection/TEST-dev.tradecycling.automator.price.LibrarianBookPricesTest.xml` with that testcase and `failures="0"`. The Gradle task name is not the proof.

## Gotchas

- Efficiency V's uncapped formula maximum is 71. The table maximum is 64, so 71 is rejected.
- Tests call `LibrarianBookPrices.load(predicate)`, not `get()`. `get()` asks Fabric Loader for the Minecraft version. On 1.21.1 that hides lunge. The lunge test covers both predicates without a game.
- `containsTarget` is not what this test runs. A matching offer can cost less than the wiki minimum after Hero of the Village. The config still cannot be saved below the wiki minimum.
