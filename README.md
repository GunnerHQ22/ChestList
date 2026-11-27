# Chest List View

A client-side Minecraft Java 1.21.1 mod for NeoForge by **GunnerHQ22**.

# Chest List View

Chest List View is a client-side mod for Minecraft Java Edition 1.21.1 and NeoForge. It adds a scrollable list view to vanilla chests and compatible storage containers so you can read item names, enchantments, and other useful details without hovering over every stack.

## Features

- **Grid, List, and Compact views** - Click **G** for the standard grid, **L** to list every slot, or **H** to hide empty slots. The buttons stay in a fixed position to the left of the chest, or above it on narrow screens.
- **Single and double chests** — Browse all 27 or 54 slots. The header shows your current position, such as `16–22 of 54`.
- **Item details at a glance** — Rows show item icons, stack counts, and useful details, with optional item names. The preview can display enchantments, potion effects, durability, equipment stats, and information supplied by other mods, such as storage capacity.
- **Readable previews** — The list retains tooltip colors and filters common keyboard hints. Hover over an item to see its full tooltip.
- **Saved views** - The mod remembers Grid, List, or Compact for recognizable stationary storage, including compatible modded chests and barrels, across game restarts. Both halves of a recognized double chest share one preference. The client periodically clears preferences for missing or replaced containers in loaded chunks. Unseen containers start in Grid.
- **Normal inventory controls** — Use Grid or the full List to place items into empty slots. Clicking, shift-clicking, hotbar swaps, double-click collection, and drag distribution use Minecraft's inventory handling.

## Controls

- Click **\*** to open List settings. Choose 4-10 rows and toggle item names, enchantments, durability, potion effects, attributes, storage and status, and other tooltip text. Short windows may show fewer rows or spread the settings across pages. More rows leave less space for details. With **Names: Off**, items use that line for details when available and show their name when no details are visible. These settings apply across containers and persist between game sessions. Full hover tooltips and search still include hidden details. Modded tooltip lines that the preview cannot identify by type use the Other tooltip text setting.

- Drag the scrollbar or click its track to jump through the list. The mouse wheel still scrolls one row at a time.
- Use **Text size** in List settings to choose **-25%**, **-10%**, **Default**, **+10%**, or **+20%** for item names and details. Smaller text can fit more lines per row. The size is saved between game sessions.
- Click **S** to search this chest. Search terms are case-insensitive and all words must match somewhere in the item name, ID, mod name, tooltip, or durability information. Click **G** to clear search and use the grid.

## Installation

1. Install **NeoForge** for **Minecraft Java Edition 1.21.1**.
2. Place the Chest List View JAR in your instance's `mods` folder.
3. Launch Minecraft, open a supported chest, and use the view buttons.

Chest List View runs on the client and requires no server-side installation. Compatible modded storage uses the standard Minecraft chest screen and nine-column menu with one to six rows (9-54 slots). A stationary modded container can save its view when the client can match the open menu's title and slot count to the storage block you used. Moving containers and other compatible menus without a recognized block start in Grid on each opening. Storage with a custom screen or menu uses its own interface.

## Build from source

With **Java 21** installed, run the Gradle wrapper from the project directory.

- Windows: `.\gradlew.bat build`
- Linux or macOS: `./gradlew build`

The built JAR is located in `build/libs/`.

## Project information
- **Coding** — Codex
- **Author** — GunnerHQ22
- **Mod ID** — `chestlistview`
- **Minecraft** — Java Edition 1.21.1
- **Mod loader** — NeoForge
- **License** - [MIT](LICENSE)
