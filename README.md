# Villager Reroller

Not affiliated with or endorsed by Trade Cycling or its author.

Client-side Fabric mod. On Minecraft 1.21.11 it adds a vanilla-style button to the villager trade screen. The button opens a list of every offer that profession can roll at its current level, with the vanilla minimum and maximum emerald price. Start breaks the job-site block and places it again until one of the chosen offers appears at or under the price you set.

This project does not include Trade Cycling. Jars built for 1.21.1 and 26.x still call Trade Cycling's cycle packet for a single librarian book. On 1.21.11 the default is to break and replace the job block. If [Trade Cycling](https://modrinth.com/mod/trade-cycling) 1.0.23 is installed, the panel can use that mod's cycle action instead. The addon is not a hard dependency.

## What it does on 1.21.11

1. Right-click a villager to open the normal trade screen.
2. Press the book button at the top right of that screen. It uses the vanilla recipe-book button sprite.
3. The panel lists that level's trades: item, name, and emerald range. Enchanted books use the 1.21.11 formula, including treasure doubling and the 64 emerald cap. Trade Rebalance uses the experimental table when that experiment is on, with the same prices.
4. Select one or more rows. A buy defaults to the minimum emerald price. Click the price to raise it, or set it to Any. Shift is not required: left click raises, right click lowers.
5. Press Start. By default the mod closes the screen, mines the job site with a real tool, waits until the villager is unemployed, places the block, and opens the new offers. It repeats until a target matches, then leaves the trade screen open. Names that do not fit the row scroll back and forth, the same way vanilla buttons do, and the price stays in its column. If Trade Cycling is installed, a toggle on the panel switches the reroll to that mod's cycle packet and leaves the job block alone.
6. Stop, Esc, or the toggle key (default `K`) cancels. Esc while the screen is closed also cancels, and the pause menu is closed so the player stays in the world.

It refuses to start when:

- the villager has trade experience, a level above novice, or an offer that has been used
- no job-site block is next to the villager, or it is out of reach
- the job-site block is not in the inventory
- survival mode has no tool with a mining speed above the hand for that block
- no trade is selected

Break, place, and reopen each send the same packets vanilla uses, at most once per action per tick. A cycle gives up if the block, the profession, or the trade screen does not come back in time.

## Versions

Built with [Stonecutter](https://stonecutter.kikugie.dev/). The sources in `src/` are the 1.21.11 tree. The job-site reroller and its client gametest are compiled for 1.21.11 and for the 1.21.5 through 1.21.10 nodes below. 1.21.1 and the 26.x nodes keep the Trade Cycling packet driver. 1.21.4 compiles, and its Trade Cycling path completed in-game, but the job-site path did not finish a launch on this machine (the integrated server fell behind by more than 200 ticks), so that jar is not published.

| Minecraft | This addon's range | Reroll |
| --- | --- | --- |
| 1.21 and 1.21.1 | `>=1.21 <=1.21.1` | Trade Cycling `fabric-1.21.1-1.0.22` |
| 1.21.5 | `1.21.5` | Break and replace the job site. Optional Trade Cycling `fabric-1.21.5-1.0.20` |
| 1.21.6, 1.21.7, 1.21.8 | `>=1.21.6 <=1.21.8` | Break and replace the job site. Optional Trade Cycling for that Minecraft version |
| 1.21.9 and 1.21.10 | `>=1.21.9 <=1.21.10` | Break and replace the job site. Optional Trade Cycling `fabric-1.21.9-1.0.19` / `fabric-1.21.10-1.0.20` |
| 1.21.11 | `1.21.11` | Break and replace the job site. Optional Trade Cycling `fabric-1.21.11-1.0.23` |
| 26.1, 26.1.1, 26.1.2 | `>=26.1 <26.2` | Trade Cycling `fabric-1.0.22+26.1.2` |
| 26.2 | `~26.2` | Trade Cycling `fabric-1.0.22+26.2` |
| 26.3 | `~26.3` | Trade Cycling `fabric-1.0.22+26.3` |

The 1.21.x jars are compiled for Java 21. The 26.x jars are compiled for Java 25. Gradle itself has to run on Java 25 because Fabric Loom 1.18 requires that runtime.

Also required: Fabric Loader, Fabric API, [Cloth Config](https://modrinth.com/mod/cloth-config), and [Mod Menu](https://modrinth.com/mod/modmenu). Trade Cycling is required on the packet-driver versions. On 1.21.5 through 1.21.11 it is optional: [Trade Cycling](https://modrinth.com/mod/trade-cycling) by Max Henkel. There is no Forge jar for these nodes. Cloth Config's Forge builds stop at 1.21.3 (`17.0.144+forge`), and 1.21.4+ Forge releases of Trade Cycling are Lex Forge, which cannot use the NeoForge Cloth Config artifacts.

## How to use

Install Fabric, Fabric API, Cloth Config, Mod Menu, and this mod. Open a novice villager (no trades used yet) who is standing next to their job site, with that block and a suitable tool in your inventory.

The book button opens the list. Search filters by name. Start rerolls until a selected row appears. The price you set is the most you will pay in emeralds before you count it as a match. Reputation can make a trade cheaper than the vanilla minimum; that still matches. Sells, where the villager pays you emeralds, match on the item rather than a ceiling.

Mod Menu still has an enabled switch, an attempt cap (default 500), and a panic stop. The enchantment, level, and emerald sliders are saved for the older packet driver and are not read by the 1.21.11 reroller.

The toggle key is `K`. On 1.21.11 it starts the selected targets while the trade screen is open, and stops a reroll from anywhere. Change it in Controls, under Miscellaneous.

## Emerald price rule

Librarian enchanted-book prices follow the 1.21.11 trade code, which matches the Minecraft Wiki:

- https://minecraft.wiki/w/Librarian#Trading
- https://minecraft.wiki/w/Enchanted_Book
- https://minecraft.wiki/w/Trading#Librarian

The roll is `2 + random.nextInt(5 + level * 10) + 3 * level`. `nextInt` includes 0 and excludes the bound, so the range is `2 + 3 × level` through `6 + 13 × level`. Treasure enchantments (`#minecraft:double_trade_price`) double that, then the result is capped at 64.

| Level | Normal | Treasure (after the 64 cap) |
| --- | --- | --- |
| I | 5–19 | 10–38 |
| II | 8–32 | 16–64 |
| III | 11–45 | 22–64 (22–90 before the cap) |
| IV | 14–58 | 28–64 (28–116 before the cap) |
| V | 17–64 (17–71 before the cap) | 34–64 (34–142 before the cap) |

Treasure books librarians sell: Curse of Binding, Curse of Vanishing, Frost Walker, and Mending. Soul Speed, Swift Sneak, and Wind Burst are not sold. Lunge is included from 1.21.11 and uses the normal table.

Trade Rebalance does not change the cost formula. It only changes which book can roll. Desert special is Efficiency III (11–45), jungle special is Unbreaking II (8–32), plains special is Protection III (11–45), savanna special is Sharpness III (11–45), snow special is Silk Touch (5–19), swamp special is Mending (10–38), and taiga special is Fortune II (8–32).

Enchanted gear adds `5 + random.nextInt(15)` to its base cost, so the added emeralds are 5 through 19, then capped at 64. A fletcher's enchanted bow (base 2) is 7–21.

`src/main/java/dev/quarkos/villagerreroller/price/VanillaBookPrices.java` is that math with no Minecraft bootstrap. `librarian_book_prices.json` is the same capped table, still used to reject impossible prices in the older config screen.

## Build

```bash
# Gradle must be running on Java 25 (Loom 1.18). The 1.21.x jars still target Java 21.
export JAVA_HOME=/opt/jdk25
./gradlew :1.21.11:test
./gradlew :1.21.11:build
# Headless client gametest. Stop the daemon first if it was started without a display.
./gradlew --stop
xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew :1.21.11:runClientGameTest
# Same gametest with Trade Cycling on the dev runtime. The published jar still does not bundle it.
xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew :1.21.11:runClientGameTest -PwithTradeCycling=true
```

`:1.21.11:test` checks the wiki price table, the vanilla roll, and the scrolling-name offset. The client gametest spawns a librarian next to a lectern. Without the property it breaks the lectern. With `-PwithTradeCycling=true` it cycles through Trade Cycling and leaves the lectern in place.

The Java package is `dev.quarkos.villagerreroller`. The mod id is `villager_reroller`.
