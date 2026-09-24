# Attempt cap

## Sub-features

- `AutomatorConfig.maxAttempts` defaults to 500. `setMaxAttempts` and load both clamp with `Math.max(1, value)`.
- The config screen field is "Attempt cap", minimum 1, maximum 10000, default 500.
- `CycleAutomator` waits `TICKS_BETWEEN_CYCLES` (8) ticks while the offer signature is unchanged, then sends another cycle.
- Before each send, if `attempts >= maxAttempts`, `tick` stops with `ATTEMPT_CAP` ("Stopped after the attempt cap").
- A send increments `attempts`, stores the offer signature, and calls `FabricTradeCyclingClientMod.sendCycleTradesPacket()`.
- `start` sets `attempts` back to 0.

## How to get to it (user POV)

Set "Attempt cap" on the Mod Menu screen and save. Open a novice librarian and press `K`. Each cycle request counts as one attempt. When the count reaches the cap, cycling stops and the action bar says it stopped after the attempt cap. The book is not bought. Panic stop, the key, a closed screen, a non-librarian, or a match also stop, and those do not wait for the cap.

## Driving it with Gradle JUnit

Nothing in `src/test` sets `maxAttempts` or calls `CycleAutomator.tick`. The counter sits next to `Minecraft`, `MerchantScreen`, and Trade Cycling's packet send.

```bash
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh drive attempt-cap
```

That exits 2. Do not treat the default of 500 in source as a run that hit the cap.

## Gotchas

- The cap is compared before the increment, so a cap of 1 allows one `sendCycleTradesPacket` and then stops.
- If the offer signature changes, the 8-tick wait is skipped. The attempt still counts.
- Empty offers do not increment `attempts`. They stop with `NO_OFFERS` after 40 ticks.
- `canCycle` false stops with `CANNOT_CYCLE` without counting another attempt.
