# Changelog

## 1.1.0

- Tools at or below a durability floor (default 10 uses left) are skipped. Equally fast tools prefer an unenchanted one, then one without Mending. The bare hand is the fallback when the block allows it. The run stops with a chat message when no safe tool remains.
- The settings screen is a vanilla screen on Fabric and Forge. Cloth Config is no longer required. Mod Menu is optional and opens the same screen when it is installed. Settings still save to `villager_reroller.json`.
- A server-friendly preset slows the cycle spacing, the timeouts, and the camera turn. The camera eases toward the block instead of snapping.
- Trade Cycling is used only when the connected server advertises its channel. The toggle is greyed out otherwise, and the cycle packet is not sent.
- The enchantment, level, and maximum-emerald settings are gone. Older config files that still contain those keys keep loading.

## 1.0.0

First release, for Minecraft 1.21.11 (jar version `1.0.0+1.21.11`).

- A book button on the villager trade screen opens every offer that profession can roll at the current level, with the vanilla minimum and maximum emerald price.
- Start breaks the job-site block and places it again until a chosen offer is at or under the price you set. Stop or Esc cancels.
- Names that do not fit scroll inside their own box, and the price column stays put.
- If Trade Cycling is installed, a toggle on the panel can use that mod's cycle action instead of breaking the job block. Trade Cycling is optional.
- A villager who has already been traded with is refused, and the reroll stops once a match is found.
