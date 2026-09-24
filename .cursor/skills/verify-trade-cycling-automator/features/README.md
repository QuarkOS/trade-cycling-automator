# Features

User-facing behavior of `trade-cycling-automator`, and the one Gradle JUnit path that can drive any of it. Checked-in sources are the 1.21.11 node.

| Feature | File | Gradle JUnit |
| --- | --- | --- |
| Enchantment, level, and max price | [enchantment-level-price.md](enchantment-level-price.md) | No screen driver. Validation is the wiki test. |
| Wiki price rejection | [wiki-price-rejection.md](wiki-price-rejection.md) | `LibrarianBookPricesTest.rejectsLevelsAndPricesOutsideTheWiki` |
| Start/stop keybind | [start-stop-keybind.md](start-stop-keybind.md) | None |
| Librarian-only stop | [librarian-only-stop.md](librarian-only-stop.md) | None |
| Attempt cap | [attempt-cap.md](attempt-cap.md) | None |

`scripts/verify.sh drive` runs the wiki test and exits 2 for the other four ids.
