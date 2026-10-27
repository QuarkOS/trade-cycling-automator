# Reroll odds

The panel estimates the chance that one fresh trade generation contains any selected offer at or under that offer's chosen emerald ceiling. It is shown before Start, as "About 1 in X attempts" and a time for the method currently selected.

The estimator is `dev.quarkos.villagerreroller.odds.TradeOdds`. It does not bootstrap Minecraft. The live catalog tags each row with the listing that produced it, and `TradeSession` feeds that pool into the same function the unit tests call. `TradeOddsTest` compares the closed form with a Monte Carlo of the generation loop below.

## What one attempt rolls

Classic villagers (`Villager.updateTrades`, 1.19 through 1.21.11) always ask `addOffersFromItemListings` for **2** offers from the profession's level list. The list is `VillagerTrades.TRADES`. `EXPERIMENTAL_TRADES` is used only when the world has `trade_rebalance` enabled, and only when that map has an entry. The catalog already follows that flag, and the odds use the same list. The default, experiment off, is `VanillaRoll.TRADE_REBALANCE_DEFAULT`.

1.20.5 did not move villager trades into JSON. Enchantments became data, and 1.21 librarian books read `#minecraft:tradeable`. The Java `VillagerTrades` tables stay in place through 1.21.11, including the experiment flag. **26.1** is the first version whose trades are data-driven `TradeSet`s. A set's `amount` is how many offers to roll. The mod reads a constant or an inclusive uniform. Any other number provider falls back to 2. A uniform whose span is wider than 16 is approximated as its minimum, midpoint, and maximum, each equally likely. `allow_duplicates` defaults to false. When it is false and `amount` is larger than the pool, vanilla still emits each trade at most once, which is the same as drawing `min(amount, pool)` without replacement.

Drawing:

- Copy the listings. While fewer than `amount` offers have been added and the copy is not empty, remove a listing at a uniform random index and call `getOffer`.
- A null offer does not count. The listing is already removed, and the loop continues. Listings that can return null (a cartographer map with no structure, a type-specific trade for another villager type) are omitted from the pool. That preserves the uniform draw among the listings that succeed. Map odds are therefore slightly optimistic in a world that has no buried treasure, ocean monument, woodland mansion, or trial chambers.
- `allow_duplicates` true draws with replacement: each of the `amount` offers is an independent uniform listing.

An attempt hits when any kept offer matches any selected id at that id's ceiling. Several selected targets are any-of. Sells ignore the emerald ceiling. A ceiling of `ANY` is `Integer.MAX_VALUE`.

## Enchanted books

Each book listing is one slot in the pool. Its enchantment outcomes are mutually exclusive. Treating every book as its own listing would make the odds far too high.

The enchantment is uniform over the listing's tradeable set, not `Enchantment.getWeight()`. `HolderSet.getRandomElement` and `EnchantRandomlyFunction` both use `Util.getRandomSafe`, which is a uniform index. The pre-1.21 Java listings use `list.get(random.nextInt(list.size()))` on `Enchantment.isTradeable()`. `VanillaRoll.UNIFORM_ENCHANTMENT_WEIGHT` is 1, and the live reader passes that. The estimator still accepts branch weights so a datapack that weighted a listing could be modeled; vanilla librarian books do not.

The level is uniform and inclusive on the intersection of the enchantment's own min/max and the listing's min/max. `expandBooks` already drops a level when that intersection is empty. One result per integer level is the same distribution.

The emerald cost is the wiki formula already in `VanillaBookPrices`:

```
roll = random.nextInt(5 + level * 10)   // 0 inclusive, bound exclusive
cost = 2 + roll + 3 * level
if treasure: cost *= 2                  // #minecraft:double_trade_price
cost = min(cost, 64)
```

Treasure doubles before the cap. After the cap, 64 is more likely than a single raw roll, so the displayed min–max is not a uniform range. A price below the raw minimum has probability 0 and the panel says "This price cannot roll". The JSON price table includes enchantments the live registry may not (Breach and Density on every version, Lunge only from 1.21.11). Odds use the enchantments the catalog actually listed for this game, not that JSON.

## Other listings

- A fixed item trade with one price is that price. A 26.x count range that is not a book or a piece of enchanted gear is an inclusive uniform integer from `emeraldMin` to `emeraldMax`.
- `EnchantedItemForEmeralds` (1.19–1.21.11) rolls `cost = 5 + random.nextInt(15)`, adds it to the base, and caps at 64. The same roll is the enchantment power. The catalog row matches any enchantment on that item, so the odds treat the row as one offer and only test the price. 26.x gear that the catalog shows as a count range uses the inclusive uniform above, not this 15-roll formula.
- A suspicious-stew listing applies every effect on that item. Those rows share one offer and match together: selecting any of the effect ids hits when that listing is rolled.
- Tipped arrows and dyed armor are one row that matches any potion or dye, so the price check is the only filter.
- Empty enchanted-book tags still produce a plain book for 1 emerald in vanilla. The catalog does not list that fallback, and neither do the odds.

## What the estimate leaves out

Reputation, Hero of the Village, and the demand multiplier are ignored. The number is the first-offer base price. A discount can only add matches, and the reroll already treats a price below the minimum as a hit.

Named random sequences in 26.x change the seed, not the distribution.

"About 1 in X" is `round(1 / p)`, and at least 1 when `p > 0`. Values that land a few ulps under an exact half, which is what `1 / (2/3)` does in binary, are treated as that half so the label stays "1 in 2". The time line uses that rounded X, so the two lines agree. Job-site cadence is `AttemptPace.JOB_SITE_TICKS` (140 ticks, 7 seconds): two look ticks, about 38 ticks of wooden-axe swings on a lectern (hardness 2.5, tool speed 2), about 20 ticks for the profession to drop, two look ticks, about 5 to place, about 50 to reclaim the profession, and about 15 to reopen the screen. A real attempt moves with chunk ticks and pathing. Trade Cycling uses `TICKS_BETWEEN_CYCLES` (8). That is the gap before a retry when the offers have not changed. A fast server can cycle sooner, so that time is an upper estimate of the steady state.

Pools whose combination count `C(n, k)` exceeds 100,000 use a seeded sample of 20,000 attempts. Vanilla villager pools draw 2 offers and stay on the exact sum. An empty selection shows no odds line. While a reroll is running, the status line replaces the odds.
