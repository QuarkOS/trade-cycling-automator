# Automator for Trade Cycling

Client-side Fabric addon for [Trade Cycling](https://modrinth.com/mod/trade-cycling). It keeps asking Trade Cycling to reroll a librarian until that librarian offers the enchanted book you chose, at the level you chose, for at most the emerald price you chose.

This is not Trade Cycling. Trade Cycling is a separate mod by Max Henkel and is required at runtime. This project does not include Trade Cycling's code or jar.

## What it does

1. You open a librarian who has never been traded with.
2. You press the toggle key (default `K`) or turn automation on from the config.
3. The addon calls Trade Cycling's cycle packet until a trade matches, then it stops.
4. It does not click the trade and it does not spend emeralds. You buy the book yourself.

It stops on its own when:

- the trade screen closes
- the villager you last interacted with is not a librarian
- Trade Cycling cannot cycle (the villager already has trade experience)
- the attempt cap is hit (default 500)
- you press the key again, disable the addon, or use **Panic stop** in the config

## Versions

One repository, built with [Stonecutter](https://stonecutter.kikugie.dev/) against the Fabric files of Trade Cycling 1.0.22:

| Minecraft | Trade Cycling artifact | This addon's range |
| --- | --- | --- |
| 1.21 and 1.21.1 | `fabric-1.21.1-1.0.22` | `>=1.21 <=1.21.1` |
| 1.21.11 | `fabric-1.21.11-1.0.22` | `1.21.11` |
| 26.1, 26.1.1, 26.1.2 | `fabric-1.0.22+26.1.2` | `>=26.1 <26.2` |
| 26.2 | `fabric-1.0.22+26.2` | `~26.2` |
| 26.3 | `fabric-1.0.22+26.3` | `~26.3` |

The 1.21.x jars are compiled for Java 21. The 26.x jars are compiled for Java 25. Gradle itself has to run on Java 25 because Fabric Loom 1.18 requires that runtime. 1.21.11 is the version checked into `src/`.

Also required, per Minecraft version: Fabric Loader, Fabric API, [Cloth Config](https://modrinth.com/mod/cloth-config), and [Mod Menu](https://modrinth.com/mod/modmenu).

## How to use

Install Fabric, Fabric API, Trade Cycling, Cloth Config, Mod Menu, and this addon.

Open Mod Menu, find **Automator for Trade Cycling** under Trade Cycling, and set:

- enchantment
- level
- maximum emeralds
- enabled
- attempt cap
- panic stop (turn on and save; it is not stored)

Impossible combinations are rejected in that screen: a level above the enchantment's maximum, or a price below the wiki minimum or above the wiki maximum.

Break and replace the lectern until the librarian is a novice, open the trade screen, and press `K`. Change the key in Controls, under Miscellaneous on 1.21.11 and newer, or under **Automator for Trade Cycling** on 1.21.1.

The price that is compared is the emerald count shown on the trade (`MerchantOffer.getCostA()`), after reputation and Hero of the Village. The number you configure is still limited to the wiki's pre-discount range.

## Wiki price rule

Librarian enchanted-book prices are taken from the Minecraft Wiki, not invented:

- https://minecraft.wiki/w/Librarian#Trading
- https://minecraft.wiki/w/Enchanted_Book
- https://minecraft.wiki/w/Trading#Librarian

The wiki rule, retrieved 2026-09-24:

> The price in emeralds is `2 + 3 × level` to `6 + 13 × level`, doubled for treasure enchantments, then capped at 64 before further discounts.

| Level | Normal | Treasure (after the 64 cap) |
| --- | --- | --- |
| I | 5–19 | 10–38 |
| II | 8–32 | 16–64 |
| III | 11–45 | 22–64 (22–90 before the cap) |
| IV | 14–58 | 28–64 (28–116 before the cap) |
| V | 17–64 (17–71 before the cap) | 34–64 (34–142 before the cap) |

Treasure books librarians sell: Curse of Binding, Curse of Vanishing, Frost Walker, and Mending. Soul Speed, Swift Sneak, and Wind Burst are treasure enchantments, and the wiki says librarians do not sell them, so they are not in the list. Lunge is included from 1.21.11 onward and uses the normal table. The rows live in `src/main/resources/assets/trade-cycling-automator/librarian_book_prices.json`.

The uncapped numbers cannot appear on a trade. The config rejects a maximum above the capped wiki maximum, so Efficiency V at 71 emeralds is invalid even though 71 is the formula before the cap.

## How it calls Trade Cycling

Trade Cycling 1.0.22 has no addon API. The public client entry point is:

```java
de.maxhenkel.tradecycling.FabricTradeCyclingClientMod.instance().sendCycleTradesPacket()
```

`sendCycleTradesPacket()` is the method Trade Cycling's own cycle button and cycle key use. It sends `de.maxhenkel.tradecycling.net.CycleTradesPacket` through Fabric's `ClientPlayNetworking`. This addon calls that method and nothing else in Trade Cycling. Whether a cycle is legal is checked with the public `CycleTradesButton.canCycle(MerchantMenu)` before each send.

The dependency is `modCompileOnly`. The jar is not bundled or redistributed.

## Build

```bash
# Gradle must be running on Java 25 (Loom 1.18). The 1.21.x jars still target Java 21.
./gradlew build
```

Each jar is written to `versions/<minecraft>/build/libs/`. `./gradlew test` checks the price table against the wiki formula.

The Java package is `dev.tradecycling.automator`.
