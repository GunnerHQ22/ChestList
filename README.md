# Chest List View

Chest List View is a client-side mod for **Minecraft Java Edition 1.21.1** on **NeoForge**. It adds a scrollable list to chests, barrels, and compatible modded storage so you can read enchantments, potion effects, durability, and other item details at a glance.

## Chest controls

| Button | What it does |
| --- | --- |
| **G** | Return to the standard inventory grid. |
| **L** | List every storage slot, including empty slots. |
| **H** | Switch to Compact view and hide empty slots. |
| **S** | Search the open container by item name, registry ID, mod name, tooltip text, or durability. Search is case-insensitive and every typed word must match. |
| **\*** | Open List settings. |

Scroll with the mouse wheel, drag the scrollbar, or click its track. The header shows the visible range, such as `16-22 of 54`. Each row uses the original inventory slot, so shift-clicking, hotbar swaps, double-click collection, and drag distribution work through Minecraft's normal inventory controls. Use Grid or the full List to place items into empty slots.

## List settings

- **Visible rows** can be set from 4 to 10. Smaller windows or higher GUI scales can show fewer.
- **Text size** has five presets relative to Default. Choose **−25%**, **−10%**, **Default**, **+10%**, or **+20%**. Taller rows and smaller text can fit more detail lines.
- **Item details** can be toggled independently for enchantments, durability, potion effects, attributes, storage and status, and other tooltip text. The preview keeps tooltip colors and filters common keyboard hints.
- **Item names** can be hidden to make room for details. A row still shows the item's name if it has no visible details.

List settings apply across containers and persist between game sessions. Full hover tooltips and search still use item details hidden from the list preview.

## Saved views and compatibility

Vanilla single and double chests, trapped chests, and barrels are supported. Modded storage can use List View when it opens the standard Minecraft chest screen and menu with one to six rows of nine slots. Containers with custom screens or menus use their own interface.

New containers open in Grid. The mod remembers List or Compact for recognized stationary storage, including compatible modded containers whose block can be matched to the open menu. Both halves of a recognized double chest share a preference. Compatible moving containers can use List View during an opening and return to Grid when reopened.

Saved views are scoped to the world or server, dimension, and container location and type. The client checks saved locations in loaded chunks on entering a world or dimension and then roughly every 30 seconds. It removes the preference if the expected container is missing at the time of the check.

## Installation

Use **NeoForge 21.1.200 or later in the 21.1 series** for **Minecraft Java Edition 1.21.1**. Place the Chest List View JAR in your client's `mods` folder. Multiplayer servers require no installation.

Created by **GunnerHQ22**. Mod ID `chestlistview`. Licensed under [MIT](LICENSE).
