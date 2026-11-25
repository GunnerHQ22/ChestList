# Chest List View

A client-side Minecraft Java 1.21.1 mod for NeoForge by **GunnerHQ22**.

# Chest List View

Chest List View is a client-side mod for Minecraft Java Edition 1.21.1 and NeoForge. It adds a scrollable list view to vanilla chests and compatible storage containers so you can read item names, enchantments, and other useful details without hovering over every stack.

## Features

- **Grid, List, and Compact views** - Click **G** for the standard grid, **L** to list every slot, or **H** to hide empty slots. The buttons stay in a fixed position to the left of the chest, or above it on narrow screens.
- **Single and double chests** — Browse all 27 or 54 slots. The header shows your current position, such as `16–22 of 54`.
- **Item details at a glance** — Rows show item icons, names, stack counts, and up to two lines of useful details. The preview can display enchantments, potion effects, durability, equipment stats, and information supplied by other mods, such as storage capacity.
- **Readable previews** — The list retains tooltip colors and filters common keyboard hints. Hover over an item to see its full tooltip.
- **Per-chest preferences** — The mod remembers Grid, List, or Compact for each chest it can identify, including across game restarts. Both halves of a double chest share one preference. Unseen chests start in Grid. When the chest cannot be identified, your choice lasts for that opening.
- **Normal inventory controls** — Use Grid or the full List to place items into empty slots. Clicking, shift-clicking, hotbar swaps, double-click collection, and drag distribution use Minecraft's inventory handling.

## Controls

- Drag the scrollbar or click its track to jump through the list. The mouse wheel still scrolls one row at a time.
- Click **S** to search this chest. Search terms are case-insensitive and all words must match somewhere in the item name, ID, mod name, tooltip, or durability information. Click **G** to clear search and use the grid.

## Installation

1. Install **NeoForge** for **Minecraft Java Edition 1.21.1**.
2. Place the Chest List View JAR in your instance's `mods` folder.
3. Launch Minecraft, open a supported chest, and use the view buttons.

Chest List View runs on the client and requires no server-side installation. Compatible modded storage uses the standard Minecraft chest screen and nine-column menu with one to six rows (9-54 slots). Modded containers start in Grid on each opening. Storage with a custom screen or menu uses its own interface.

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
