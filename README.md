# Chest List View

A client-side Minecraft Java 1.21.1 mod for NeoForge by **GunnerHQ22**.

# Chest List View

Chest List View is a client-side mod for Minecraft Java Edition 1.21.1 and NeoForge. It adds a scrollable list view to vanilla chests so you can read item names, enchantments, and other useful details without hovering over every stack.

## Features

- **Grid and List views** — Switch between the vanilla inventory grid and a scrollable list using the button at the top of the chest screen.
- **Single and double chests** — Browse all 27 or 54 slots. The header shows your current position, such as `16–22 of 54`.
- **Item details at a glance** — Rows show item icons, names, stack counts, and up to two lines of useful details. The preview can display enchantments, potion effects, durability, equipment stats, and information supplied by other mods, such as storage capacity.
- **Readable previews** — The list retains tooltip colors and filters common keyboard hints. Hover over an item to see its full tooltip.
- **Per-chest preferences** — The mod remembers Grid or List for each chest it can identify, including across game restarts. Both halves of a double chest share one preference. Unseen chests start in Grid. When the chest cannot be identified, your choice lasts for that opening.
- **Normal inventory controls** — Empty slots remain usable. Clicking, shift-clicking, hotbar swaps, double-click collection, and drag distribution use Minecraft's inventory handling.

## Installation

1. Install **NeoForge** for **Minecraft Java Edition 1.21.1**.
2. Place the Chest List View JAR in your instance's `mods` folder.
3. Launch Minecraft, open a chest, and click **List**. Click **Grid** to switch back.

Chest List View runs on the client and requires no server-side installation. Support is intended for vanilla chests. Modded items can show their tooltip details inside supported chests, while custom storage screens may use their own interfaces.

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
- **License** — All Rights Reserved
