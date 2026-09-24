---
name: verify-trade-cycling-automator
description: >-
  Verifies the Fabric client addon trade-cycling-automator (Automator for Trade
  Cycling): Mod Menu config for enchantment, level, and max emerald price; the
  K start/stop keybind; librarian-only cycling stop; and the attempt cap. Reach
  for it when checking those surfaces. The executable path here is Gradle JUnit
  on the wiki price table. It does not launch a Minecraft client.
---

# Verify trade-cycling-automator

Client-only Fabric addon. A player touches three things: the Mod Menu config screen (`AutomatorConfigScreen`, parent mod `trade_cycling`), the toggle keybind (default `K`, `AutomatorKeys.TOGGLE`), and a librarian trade screen (`CycleAutomator` calling Trade Cycling's `sendCycleTradesPacket()`). The only automated harness in the repo is `src/test/java/dev/tradecycling/automator/price/LibrarianBookPricesTest.java` (four JUnit 5 tests). There is no test for the screen, the keybind, `LibrarianTracker`, or the attempt counter.

Checked-in sources are the 1.21.11 Stonecutter node (`stonecutter active "1.21.11"`). `JAVA_HOME=/opt/jdk25 ./gradlew build` has already produced jars at `versions/<minecraft>/build/libs/trade-cycling-automator-1.0.0+<minecraft>.jar` for `1.21.1`, `1.21.11`, `26.1.2`, `26.2`, and `26.3`. Gradle itself must run on Java 25 (`/opt/jdk25`). The `java` on `PATH` is 21 and is the wrong runtime for Loom 1.18.

`:1.21.11:runClient` is a real Loom task (`runDirectory` is the shared repo `run/` folder). It was not started. This skill does not treat it as a harness: there is no scripted client, Trade Cycling is `modCompileOnly`, and two clients would share one `run/` directory. Do not invent keypresses or screen selectors.

Each verification run is its own Gradle invocation. `org.gradle.parallel=true` lets `./gradlew test` execute the five version projects in one build. That is five test workers, not two game instances. Drive one node when you want a single result.

Run every command from the repo root. The helper is executable.

## Launch

No process stays up. Launch only proves the wrapper starts on Java 25, then exits.

```bash
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh launch
```

That runs `JAVA_HOME=/opt/jdk25 ./gradlew --version`. The printed JVM must be 25. It does not call `runClient` or `runServer`.

A full price-table run, when you want every version node, is:

```bash
JAVA_HOME=/opt/jdk25 ./gradlew test
```

There is a root `test` task. Per-version tasks are `:1.21.1:test`, `:1.21.11:test`, `:26.1.2:test`, `:26.2:test`, and `:26.3:test`.

## Doctor

```bash
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh doctor
```

The script exits non-zero unless all of these are true:

- `/opt/jdk25/bin/java` reports version 25
- `./gradlew` is executable
- `stonecutter.gradle.kts` contains `stonecutter active "1.21.11"`
- `src/test/java/dev/tradecycling/automator/price/LibrarianBookPricesTest.java` contains `rejectsLevelsAndPricesOutsideTheWiki`
- `src/main/java/dev/tradecycling/automator/price/LibrarianBookPrices.java` exists
- `src/main/resources/assets/trade-cycling-automator/librarian_book_prices.json` exists

It also prints whether the five mod jars are present under `versions/<minecraft>/build/libs/`. Missing jars are a warning. `test` recompiles the price table; it does not need a previous jar. A missing Java 25 or a missing test method is a failure. Fix the checkout before driving.

## Drive

Drive one mapped feature. The helper refuses every feature that has no test, with exit code 2. A refusal is not a pass.

Wiki price rejection is the feature this harness can execute. The test calls `LibrarianBookPrices.load` and `validate`. It does not open Cloth Config, and it does not read `AutomatorConfig`.

```bash
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh drive wiki-price-rejection
```

That executes:

```bash
JAVA_HOME=/opt/jdk25 ./gradlew :1.21.11:test --tests dev.tradecycling.automator.price.LibrarianBookPricesTest.rejectsLevelsAndPricesOutsideTheWiki --rerun-tasks
```

`--rerun-tasks` is required. An `UP-TO-DATE` line from a previous build is not this run.

The method must actually run. The helper fails the drive unless the JUnit XML contains `testcase` `rejectsLevelsAndPricesOutsideTheWiki()`, `failures="0"`, and `errors="0"`. Those assertions reject a level above the enchantment max (`minecraft:mending` at 2), a price under the wiki minimum (mending I at 9, protection IV at 13), and a price over the capped wiki maximum (mending I at 39, efficiency V and sharpness V at 71). Mending I at 10 and 38, efficiency V at 64, and protection IV at 14 are accepted.

These exit 2. Do not replace them with a client script.

```bash
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh drive enchantment-level-price
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh drive start-stop-keybind
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh drive librarian-only-stop
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh drive attempt-cap
```

What the code does for those four is in `features/`. The config screen's error text is `LibrarianBookPrices.validate` and `validateLevel`, which is the same code the wiki test calls. Opening the screen still needs Minecraft, Cloth Config, and Mod Menu.

## Evidence

`drive wiki-price-rejection` copies the result out of the Gradle build tree into:

`.cursor/skills/verify-trade-cycling-automator/evidence/wiki-price-rejection/`

| File | What it proves |
| --- | --- |
| `command.txt` | The Gradle command that was executed |
| `gradle-test.log` | The task output for that command |
| `gradle-exit-code` | The process exit code |
| `TEST-dev.tradecycling.automator.price.LibrarianBookPricesTest.xml` | The JUnit suite: testcase name, `tests`, `failures`, `errors` |

The XML is the state after the action. Read `testcase` and `failures` from that file. Do not treat the task name alone as success.

Gradle also writes the same XML to `versions/1.21.11/build/test-results/test/` until cleanup. The copy under `evidence/` is the one that must remain.

## Cleanup

```bash
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh cleanup
```

Run this after a drive, including a drive that failed. It runs `JAVA_HOME=/opt/jdk25 ./gradlew :1.21.11:cleanTest`, which deletes `versions/1.21.11/build/test-results/` and the HTML report for that node. It does not delete `versions/*/build/libs/`, it does not call `clean`, and it does not delete `.cursor/skills/verify-trade-cycling-automator/evidence/`.

After cleanup, this file must still be on disk:

`.cursor/skills/verify-trade-cycling-automator/evidence/wiki-price-rejection/TEST-dev.tradecycling.automator.price.LibrarianBookPricesTest.xml`

And this path must be gone:

`versions/1.21.11/build/test-results/test/TEST-dev.tradecycling.automator.price.LibrarianBookPricesTest.xml`

Nothing else is running, so there is no client or daemon to kill. The Gradle daemon may stay in the background; that is Gradle, not a game instance.

## Helpers

`.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh` is the only helper. Subcommands: `launch`, `doctor`, `drive <feature>`, `cleanup`. Feature ids match the files in `features/`.

```bash
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh launch
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh doctor
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh drive wiki-price-rejection
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh cleanup
```
