# Start/stop keybind

## Sub-features

- `AutomatorKeys.TOGGLE`, translation `key.trade-cycling-automator.toggle` ("Start or stop Trade Cycling automation"). Default key is GLFW 75 (`K`). `McBridge.registerToggle` uses `KeyMapping.Category.MISC` on 1.21.11 and newer, and `key.categories.trade-cycling-automator` on 1.21.1.
- With a screen open, `McBridge.hookScreenKeys` listens for the key on `MerchantScreen` and calls `CycleAutomator.toggle`.
- With no screen, the client tick consumes the key and calls `toggle`. `start` then stops with `NEED_SCREEN` ("Open a librarian trade screen first") because the screen is not a `MerchantScreen`.
- `toggle` calls `stop(KEYBIND)` when `running` is already true, and `start` otherwise.
- `start` requires a `MerchantScreen`, `AutomatorConfig.enabled()`, and no `problem()` from the price table. It sets `running`, clears the attempt count, and shows "Cycling for the chosen book".
- Stopping from the key shows "Stopped cycling" only if a run was actually in progress.

## How to get to it (user POV)

Change the key under Controls. On 1.21.11 and newer it is in Miscellaneous. On 1.21.1 it is under "Automator for Trade Cycling". Break and replace the lectern until the librarian is a novice, open the trade screen, and press `K`. Press `K` again to stop. The addon never clicks the trade.

## Driving it with Gradle JUnit

No test calls `CycleAutomator.toggle`, `AutomatorKeys`, or `McBridge`. `start` and `tick` call `Minecraft.getInstance()` and Trade Cycling.

```bash
.cursor/skills/verify-trade-cycling-automator/scripts/verify.sh drive start-stop-keybind
```

That exits 2. `:1.21.11:runClient` is registered by Loom and was not launched. Do not synthesize a key event.

## Gotchas

- The key does nothing useful on a non-merchant screen: `hookScreenKeys` only toggles when the screen is a `MerchantScreen`.
- `stop` for `KEYBIND` still clears `running`. The action-bar line is sent only when a run was active (`wasRunning`) or the reason is `PANIC`.
- Disabled config: `start` reports `DISABLED` and does not set `running`.
- An illegal enchantment, level, or price reports the invalid line and does not set `running`.
