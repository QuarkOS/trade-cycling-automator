# Platform fields

Do not upload yet. The icon file is not in the repository. `fabric.mod.json` points at `assets/villager_reroller/icon.png`, and that file is intentionally absent until Emilio adds a human-made icon. Put it at `src/main/resources/assets/villager_reroller/icon.png` and rebuild before a public upload. The jar built without it will not contain an icon.

Project name: Villager Reroller
Mod id: `villager_reroller`
Suggested Modrinth slug: `villager-reroller`
Version: `1.1.0+1.21.11`
License: MIT
Author: Emilio Schwaiger
Sources and issues: https://github.com/QuarkOS/trade-cycling-automator

Listing text is `LISTING.md`. The one-line summary is `SUMMARY.txt`. The changelog is `CHANGELOG.md`. Gallery shots are in `GALLERY/`.

## Modrinth

- Project type: mod
- Environment: client_only. On the project form that is Client `required` and Server `unsupported`.
- License: MIT
- Categories: `utility` and `game-mechanics`
- Loaders: Fabric
- Game versions: 1.21.11
- Dependencies:
  - `fabric-api`: required
  - `modmenu`: optional
  - `trade-cycling` (Trade Cycling by Max Henkel): optional. The server must have it too, or the toggle stays disabled.
- AI-generated code disclosure: set it. The source was written with an AI coding agent and reviewed before release. On the version upload, enable the disclosure that this version contains AI-generated code.

## CurseForge

- Main category: Utility & QoL
- Additional categories (up to 4): Miscellaneous
- Related projects:
  - Fabric API: required
  - Mod Menu: optional
  - Trade Cycling: optional. The server must have it too, or the toggle stays disabled.
- Game version: 1.21.11
- Mod loader: Fabric
- Release type: Release, after the icon is in the jar
