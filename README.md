# Villager Reroller

Not affiliated with or endorsed by Trade Cycling or its author.

Client-side mod for Fabric, Minecraft Forge, and NeoForge. It adds a vanilla-style button to the villager trade screen. The button opens a list of every offer that profession can roll at its current level, with the vanilla minimum and maximum emerald price. Start breaks the job-site block and places it again until one of the chosen offers appears at or under the price you set.

This project does not include Trade Cycling. If [Trade Cycling](https://modrinth.com/mod/trade-cycling) 1.0.23 is installed, the panel can use that mod's cycle action instead and leave the job block alone. The addon is a suggestion, not a hard dependency, on every version.

## What it does

1. Right-click a villager to open the normal trade screen.
2. Press the book button at the top right of that screen. It uses the vanilla recipe-book button sprite.
3. The panel lists that level's trades: item, name, and emerald range. Enchanted books use the 1.21.11 formula, including treasure doubling and the 64 emerald cap. Trade Rebalance uses the experimental table when that experiment is on, with the same prices.
4. Select one or more rows. A buy defaults to the minimum emerald price. Click the price to raise it, or set it to Any. Shift is not required: left click raises, right click lowers. The choices and ceilings are saved for that profession in `config/villager_reroller-picks.json` and come back when you open the panel again, including after a restart or after you trade with a different profession.
5. Press Start. By default the mod closes the screen, mines the job site with a real tool, waits until the villager is unemployed, places the block, and opens the new offers. It repeats until a target matches, then leaves the trade screen open, plays an experience-orb chime, and still sends the chat line and toast. Names that do not fit the row scroll back and forth, the same way vanilla buttons do, and the price stays in its column. If Trade Cycling is installed, a toggle on the panel switches the reroll to that mod's cycle packet and leaves the job block alone.
6. Stop, Esc, or the toggle key (default `K`) cancels. Esc while the screen is closed also cancels, and the pause menu is closed so the player stays in the world.

It refuses to start when:

- the villager has trade experience, a level above novice, or an offer that has been used
- no job-site block is next to the villager, or it is out of reach
- the job-site block is not in the inventory
- survival mode has no tool with a mining speed above the hand for that block
- no trade is selected

Break, place, and reopen each send the same packets vanilla uses, at most once per action per tick. A cycle gives up if the block, the profession, or the trade screen does not come back in time.

## Versions

Built with [Stonecutter](https://stonecutter.kikugie.dev/) 0.9.8. The sources in `src/` are the 1.21.11 Fabric tree. The root branch is Fabric. The `forge` branch shares those sources through `forge/src` and builds `:forge:<version>`. The `neoforge` branch shares them through `neoforge/src` and builds `:neoforge:<version>`. Every shipped version breaks and replaces the job site. Each jar's Minecraft range is the exact version that was launched. 1.21.6 covers only 1.21.6, and 1.21.9 covers only 1.21.9, until 1.21.7, 1.21.8, and 1.21.10 are launched.

| Minecraft | Fabric | Forge | NeoForge |
| --- | --- | --- | --- |
| 1.19 | `villager_reroller-fabric-1.1.0+1.19.jar` | `villager_reroller-forge-1.1.0+1.19.jar` (Forge 41.1.0) | — |
| 1.19.1 | `villager_reroller-fabric-1.1.0+1.19.1.jar` | `villager_reroller-forge-1.1.0+1.19.1.jar` (Forge 42.0.9) | — |
| 1.19.2 | `villager_reroller-fabric-1.1.0+1.19.2.jar` | `villager_reroller-forge-1.1.0+1.19.2.jar` (Forge 43.5.0) | — |
| 1.19.3 | `villager_reroller-fabric-1.1.0+1.19.3.jar` | `villager_reroller-forge-1.1.0+1.19.3.jar` (Forge 44.1.0) | — |
| 1.19.4 | `villager_reroller-fabric-1.1.0+1.19.4.jar` | `villager_reroller-forge-1.1.0+1.19.4.jar` (Forge 45.4.0) | — |
| 1.20.1 | `villager_reroller-fabric-1.1.0+1.20.1.jar` | `villager_reroller-forge-1.1.0+1.20.1.jar` (Forge 47.4.28) | — |
| 1.20.2 | `villager_reroller-fabric-1.1.0+1.20.2.jar` | `villager_reroller-forge-1.1.0+1.20.2.jar` (Forge 48.1.0) | `villager_reroller-neoforge-1.1.0+1.20.2.jar` (NeoForge 20.2.93) |
| 1.20.4 | `villager_reroller-fabric-1.1.0+1.20.4.jar` | `villager_reroller-forge-1.1.0+1.20.4.jar` (Forge 49.2.13) | `villager_reroller-neoforge-1.1.0+1.20.4.jar` (NeoForge 20.4.253) |
| 1.20.6 | `villager_reroller-fabric-1.1.0+1.20.6.jar` | `villager_reroller-forge-1.1.0+1.20.6.jar` (Forge 50.0.8) | `villager_reroller-neoforge-1.1.0+1.20.6.jar` (NeoForge 20.6.144) |
| 1.21 | `villager_reroller-fabric-1.1.0+1.21.jar` | `villager_reroller-forge-1.1.0+1.21.jar` (Forge 51.0.33) | `villager_reroller-neoforge-1.1.0+1.21.jar` (NeoForge 21.0.167) |
| 1.21.1 | `villager_reroller-fabric-1.1.0+1.21.1.jar` | `villager_reroller-forge-1.1.0+1.21.1.jar` (Forge 52.1.20) | `villager_reroller-neoforge-1.1.0+1.21.1.jar` (NeoForge 21.1.265) |
| 1.21.3 | `villager_reroller-fabric-1.1.0+1.21.3.jar` | `villager_reroller-forge-1.1.0+1.21.3.jar` (Forge 53.1.16) | `villager_reroller-neoforge-1.1.0+1.21.3.jar` (NeoForge 21.3.100) |
| 1.21.4 | `villager_reroller-fabric-1.1.0+1.21.4.jar` | `villager_reroller-forge-1.1.0+1.21.4.jar` (Forge 54.1.22) | `villager_reroller-neoforge-1.1.0+1.21.4.jar` (NeoForge 21.4.161) |
| 1.21.5 | `villager_reroller-fabric-1.1.0+1.21.5.jar` | `villager_reroller-forge-1.1.0+1.21.5.jar` (Forge 55.1.18) | `villager_reroller-neoforge-1.1.0+1.21.5.jar` (NeoForge 21.5.101) |
| 1.21.6 | `villager_reroller-fabric-1.1.0+1.21.6.jar` | `villager_reroller-forge-1.1.0+1.21.6.jar` (Forge 56.0.9) | `villager_reroller-neoforge-1.1.0+1.21.6.jar` (NeoForge 21.6.20-beta) |
| 1.21.9 | `villager_reroller-fabric-1.1.0+1.21.9.jar` | `villager_reroller-forge-1.1.0+1.21.9.jar` (Forge 59.0.5) | `villager_reroller-neoforge-1.1.0+1.21.9.jar` (NeoForge 21.9.16-beta) |
| 1.21.11 | `villager_reroller-fabric-1.1.0+1.21.11.jar` | `villager_reroller-forge-1.1.0+1.21.11.jar` (Forge 61.2.4) | `villager_reroller-neoforge-1.1.0+1.21.11.jar` (NeoForge 21.11.48) |
| 26.1.2 | `villager_reroller-fabric-1.1.0+26.1.2.jar` | `villager_reroller-forge-1.1.0+26.1.2.jar` (Forge 64.1.5) | `villager_reroller-neoforge-1.1.0+26.1.2.jar` (NeoForge 26.1.2.124) |
| 26.2 | `villager_reroller-fabric-1.1.0+26.2.jar` | `villager_reroller-forge-1.1.0+26.2.jar` (Forge 65.1.5) | `villager_reroller-neoforge-1.1.0+26.2.jar` (NeoForge 26.2.0.93) |
| 26.3 | `villager_reroller-fabric-1.1.0+26.3.jar` | `villager_reroller-forge-1.1.0+26.3.jar` (Forge 66.1.0) | `villager_reroller-neoforge-1.1.0+26.3.jar` (NeoForge 26.3.0.99) |

NeoForge is skipped on 1.19, 1.19.1, 1.19.2, 1.19.3, and 1.19.4 because that loader did not exist yet. 1.20.1 is skipped because the only NeoForge-named artifact for it is the legacy Forge fork `net.neoforged:forge:1.20.1-47.1.x`, which still uses the Forge packages. The `net.neoforged:neoforge` artifact and the `net.neoforged` packages start at 1.20.2. 1.21.6 and 1.21.9 use the last published NeoForge betas (`21.6.20-beta` and `21.9.16-beta`); those Minecraft versions have no stable NeoForge release, and Trade Cycling publishes a NeoForge jar for both. 1.21.2 is not built. Trade Cycling does not publish a 1.21.2 jar, and Forge has no 1.21.2 promotion. NeoForge 1.21.2 is beta-only and is not one of this repo's versions. 1.19 through 1.20.4 store trades in NBT. 1.20.2 uses the same listing field shapes as 1.20.4. 1.19 through 1.20.1 use the older shapes: an item plus a count, a single stew effect, and every tradeable enchantment. 1.19.4 draws the trade panel with `PoseStack` and `renderWidget`. 1.19.3 still uses `renderButton` and draws items without a pose. 1.19, 1.19.1, and 1.19.2 use `Registry`, public widget coordinates, and `new Button(...)`.

Fabric needs Fabric Loader and Fabric API. The settings screen is a vanilla `Screen` on Fabric, Forge, and NeoForge and writes `villager_reroller.json`. Fabric opens it with the `J` key. [Mod Menu](https://modrinth.com/mod/modmenu) is optional: when it is installed, its config button opens the same screen. Forge 1.20.4 and newer register that screen with `MinecraftForge.registerConfigScreen`. Forge 1.19 through 1.20.2 register `ConfigScreenHandler.ConfigScreenFactory`. NeoForge 1.20.2 and 1.20.4 register `ConfigScreenHandler.ConfigScreenFactory`, and NeoForge 1.20.6 and newer register `IConfigScreenFactory`. The mods list Config button opens it.

Forge 1.20.6 is pinned to 50.0.8. Trade Cycling 1.0.11 calls `CustomPayloadEvent.Context.getDirection()`, and Forge removed that method in 50.0.9. A newer 1.20.6 Forge disconnects the client on the first cycle.

Trade Cycling by Max Henkel is optional on every version ([Modrinth](https://modrinth.com/mod/trade-cycling), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/trade-cycling)). 1.21 reuses the 1.21.1 Trade Cycling jar.

1.19 through 1.20.4 are compiled for Java 17. 1.20.6 and the 1.21.x jars are compiled for Java 21. The 26.x jars are compiled for Java 25. Gradle itself has to run on Java 25 because Fabric Loom 1.18 requires that runtime.

The Forge 1.19 through 1.20.4 dev clients remap Trade Cycling's SRG jar to mojmap before launch. That remap is only for the official-mapped userdev runtime. The published mod jar does not include Trade Cycling. Those same Forge versions ship Minecraft under SRG names, so `reobfJar` remaps the release jar (and `accesstransformer.cfg`) from official names to SRG. 1.20.6 and newer Forge builds already run official names. Forge 1.19 through 1.20.2 userdev clients copy the official-mapped jar into the run `mods` folder, because those Forge builds do not discover an exploded module-path directory.

## How to use

Install Fabric (or Forge 1.21.1), the loader's dependencies above, and this mod. Open a novice villager (no trades used yet) who is standing next to their job site, with that block and a suitable tool in your inventory.

The book button opens the list. Search filters by name. Start rerolls until a selected row appears. A match is an offer at or under the vanilla maximum you see on that row. Reputation can make a trade cheaper than the vanilla minimum; that still matches. Sells, where the villager pays you emeralds, match on the item rather than a ceiling.

The settings screen has an enabled switch, a durability floor (default 10 uses left), a bare-hand fallback, a match chime (on by default) with a volume from 0 to 100, an attempt cap (default 500), a server-friendly pace, and a panic stop. Tools at or below the floor are skipped, and a run stops with a chat message when nothing safe is left. Server-friendly slows the cycle, the timeouts, and the camera. The same screen is used on Fabric, Forge, and NeoForge. Open it with `J`, from Mod Menu when that mod is installed, or from the Forge or NeoForge mods list. Saved picks live in `config/villager_reroller-picks.json`, next to `villager_reroller.json`.

The toggle key is `K`. It starts the selected targets while the trade screen is open, and stops a reroll from anywhere. Change it in Controls, under Miscellaneous. Trade Cycling is offered only when the connected server advertises that mod's channel. Otherwise the toggle is greyed out and the cycle packet is not sent.

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

`src/main/java/dev/quarkos/villagerreroller/price/VanillaBookPrices.java` is that math with no Minecraft bootstrap. `librarian_book_prices.json` is the same capped table, and the trade list uses it.

## Odds

Before Start, the panel shows the chance that one reroll contains any selected trade at its chosen max price, written as "About 1 in X attempts", plus a time estimate for the method currently selected. Job-site breaking uses a nominal 7 seconds per attempt. Trade Cycling uses the configured gap between retries, 8 ticks normally and 20 with the server-friendly preset. The math follows vanilla offer generation for that profession and level, including the 64 emerald cap, and it treats Trade Rebalance as off unless the world has the experiment on. Assumptions and the version differences are in [docs/odds.md](docs/odds.md).

## Build

```bash
# Gradle must be running on Java 25 (Loom 1.18). 1.20.6 and 1.21.x jars target Java 21.
export JAVA_HOME=/opt/jdk25
./gradlew :1.21.11:test
./gradlew build -x test

# Fabric 1.21.5 and newer use the client gametest. Older Fabric versions, and every
# Forge version, use the tick harness. The daemon does not see xvfb-run's DISPLAY.
xvfb-run -a -s "-screen 0 1280x720x24" bash -c \
  './gradlew :1.21.11:runClientGameTest -Pdisplay=$DISPLAY -Pxauth=$XAUTHORITY'
xvfb-run -a -s "-screen 0 1280x720x24" bash -c \
  './gradlew :1.20.6:runClient -Pharness=true -Pdisplay=$DISPLAY -Pxauth=$XAUTHORITY'
xvfb-run -a -s "-screen 0 1280x720x24" bash -c \
  './gradlew :forge:1.20.6:runClient -Pharness=true -PwithTradeCycling=true -Pdisplay=$DISPLAY -Pxauth=$XAUTHORITY'
```

`:1.21.11:test` checks the wiki price table, the vanilla roll, and the scrolling-name offset. The client gametest and the harness spawn a librarian next to a lectern. Without `-PwithTradeCycling=true` the run breaks the lectern. With the property it cycles through Trade Cycling and leaves the lectern in place. The published jars do not bundle Trade Cycling.

## Validate release jars

`./tools/validate` is the production check. It builds every Fabric, Forge, and NeoForge jar, then launches that jar (the file under `build/libs`, not the Loom, ForgeGradle, or ModDevGradle dev classes) in a headless client. The jar is copied into `mods/` with the dependencies that version already uses: Fabric API on Fabric. Cloth Config is not required, and Mod Menu is not installed for the run, so the vanilla settings screen is what opens. Forge and NeoForge use the same screen from the mods list. Each jar runs twice, once without Trade Cycling and once with the matching Trade Cycling jar. Pass `--loader neoforge` to launch only the NeoForge rows. Versions without a NeoForge build are omitted from that loader.

The client is the real loader for that Minecraft version. `-Dvillager_reroller.harness=true` turns on the tick harness inside the jar, the same librarian, lectern, and reroll scenario as the dev gametest. A run passes only when the log shows the mod loaded, the config screen opened, the trade panel rendered, and a reroll reached a matching offer. Lag timeouts (a world that never finished loading, a reroll that hit the deadline) are retried. Other failures are not.

Fabric API 1.19 and 1.19.1 are published to Maven as the base module only (mod id `fabric`, a few kilobytes, no nested jars). The validator uses the Modrinth bundle for those two versions, which is the jar players install and which nests the rest of the modules. 1.19.2 and newer resolve the bundled jar from Maven.

A later run with `--version` or `--loader` rewrites those rows and keeps every other row already in `report.json`.

```bash
./tools/validate
./tools/validate --version 1.21.1 --loader fabric
./tools/validate --version 1.20.1,1.21.11 --loader forge --jobs 1 --retries 2
./tools/validate --loader neoforge
./gradlew validateAll
```

Gradle itself still needs Java 25 (`JAVA_HOME=/opt/jdk25`, which the script sets). The game uses Java 17 for 1.19 through 1.20.4, Java 21 for 1.20.6 through 1.21.11, and Java 25 for 26.x. Those JDKs are expected at `/opt/jdk17`, `/usr/lib/jvm/java-21-openjdk-amd64`, and `/opt/jdk25`. `xvfb` has to be on `PATH`. The first run downloads the Minecraft client, assets, and loaders into `build/validation-minecraft/`. 26.3 preloads `tools/libglx-srgb.so`. The script compiles it from `tools/libglx_srgb.c` when `gcc` is on `PATH`. The library strips the sRGB GLX attribute that Xvfb does not advertise.

`--jobs` defaults to however many clients fit in memory and CPU, capped at 2. `--retries` defaults to 1 extra attempt after a lag timeout. `--timeout-minutes` defaults to 40 for one attempt. `--skip-build` launches jars that are already built. `./gradlew validateAll` builds the jars, then runs the script with `--skip-build`.

The report is `artifacts/validation/report.md` and `artifacts/validation/report.json`. One row is a Minecraft version and a loader. Columns are the without-Trade-Cycling result, the with-Trade-Cycling result, the release jar's SHA-256, the combined duration, and the failure reason. Screenshots are `artifacts/validation/screenshots/<version>/<loader>/<without-tc|with-tc>.png`. The process exits 0 only when every selected row passes.

The Java package is `dev.quarkos.villagerreroller`. The mod id is `villager_reroller`.
