# Test the reroll

Run this before debugging a villager reroll by hand. The command runs the unit tests, then a dedicated server and a scripted client for each selected version and loader.

```bash
./gradlew rerollTest
```

Gradle needs Java 25 (`JAVA_HOME=/opt/jdk25`). The game uses Java 17, 21, or 25 the same way `./tools/validate` does. `xvfb-run` has to be on `PATH`. The first run downloads the client runtime and the dedicated server into `build/`.

## Narrow the matrix

```bash
./gradlew rerollTest -PrerollVersion=26.3 -PrerollLoader=fabric
./gradlew rerollTest -PrerollVersion=1.21.1,1.21.11 -PrerollLoader=forge,neoforge
./gradlew rerollTest -PrerollChanged=true
```

`-PrerollChanged=true` reads the diff against `origin/main`. A change under `src/`, `tools/`, or the build scripts runs every supported version, because those sources are shared. A change that only names a version runs that version.

The same flags work without Gradle:

```bash
./tools/reroll-test --version 26.3 --loader fabric
./tools/reroll-test --changed
./tools/reroll-test --unit-only
```

`--no-tc` skips Trade Cycling. `--tc-only` skips the job-site rows. `--jobs 1` runs one server at a time. `--skip-build` uses jars already in `build/libs`.

## What a row does

Each row is one Minecraft version, one loader, and Trade Cycling on or off. The dedicated server places a villager and a lectern. The client joins, opens the trade screen, and rerolls for 30 seconds in survival, then 30 seconds in creative while flying. It then selects the offer on the counter and the reroll has to stop because that offer matched.

Without Trade Cycling the client breaks the lectern and replaces it. With Trade Cycling the lectern stays and the cycle packet runs. The server mod is present so it can build that world, and its own reroll channel is off, so the client uses the path the row is testing.

The row fails on a disconnect, an `Invalid move player packet received` kick, an exception, or a crash. A startup timeout is retried once. A kick is not retried.

## Results

`artifacts/reroll-test/results.md` and `artifacts/reroll-test/results.json` are the summary. Columns are version, loader, Trade Cycling, survival, creative, pass/fail, and a short failure reason. Full logs are written only for failures, under `artifacts/reroll-test/logs/`. Trade-screen screenshots are under `artifacts/reroll-test/screenshots/`.

`blocked` means that version could not be launched. It is not a pass. The failure reason says why. NeoForge is omitted before 1.20.2 because the mod does not ship that loader there. Versions that are not in `settings.gradle.kts` are not in the matrix.

The process exits 0 only when every selected row passes.

## Invalid-move kick

The 26.3 job-site row is the regression for `Invalid move player packet received`. The fixed client sends a rotation packet. The pre-fix client sends a second position packet and the dedicated server kicks it.

Build the pre-fix jars in a worktree of `main`, then point the client at that jar and the server at a fixed jar (the fixed server only prepares the world):

```bash
./tools/reroll-test --version 26.3 --loader fabric --no-tc --skip-unit --skip-build \
  --client-jar /path/to/main/build/libs/villager_reroller-fabric-1.1.1+26.3.jar \
  --server-jar build/libs/villager_reroller-fabric-1.1.1+26.3.jar \
  --expect-kick
```

`--expect-kick` passes only when the server log contains `Invalid move player packet received`. The same command without `--expect-kick`, using the fixed jars, passes only when that kick is absent and both holds finish.

`./tools/validate` is the separate release-jar screen check. It does not join a dedicated server. Use `rerollTest` for the kick and the 30 second holds.
