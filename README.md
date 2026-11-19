# Chest List View

A client-side Minecraft Java 1.21.1 mod for NeoForge by **GunnerHQ22**. The current display name is *Chest List View* and the mod ID is `chestlistview`. Suggested release name: **Chest Slot List**.

## Build and launch

Install a Java 21 JDK. Set `JAVA_HOME` to that JDK if another Java version is selected by default. The first build needs access to Gradle, the Gradle Plugin Portal, NeoForge and Minecraft dependencies.

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

On Linux or macOS, use `sh ./gradlew build` and `sh ./gradlew runClient`. To install a built mod, copy `build/libs/chestlistview-0.1.0.jar` into the Minecraft client's `mods` folder. The project targets NeoForge 21.1.200. Other 21.1.x builds allowed by the manifest have not been validated.

## How it works

- `ChestListView` registers client interaction and screen-opening listeners. `ChestListClient` supports the exact vanilla three-row screen and matches six-row screens to recent physical double-chest clicks. Both layouts use the menu Minecraft already opened.
- `ChestViewport` maps a visible row to the original menu slot at `firstSlot + row`. With offset 5, the first visible row resolves to menu slot 5. Empty slots occupy rows and accept item placement. The viewport renders up to seven rows, depending on available GUI height.
- `ChestListScreen` overrides the rectangle check used by vanilla's slot lookup. It maps each original chest grid rectangle to its visible row, suppresses hidden chest slots, and translates the inventory rectangles with their icons. Vanilla resolves the actual `Slot`, then handles the click type and submits its existing container click through the normal client game-mode path. No menu slots or slot coordinates are rewritten.
- The vanilla screen continues to handle hover tooltips, carried-item drawing, player inventory input and closing. The row-wide target works for both item and blank rows. The scrollbar shows position and the mouse wheel moves the viewport.
- `ChestItemText` previews up to two lines of normal item tooltip text, including potion effects, enchantments, and descriptions supplied by mods. Hover for the full tooltip.

## Compatibility and gesture scope

- Unseen chests open in Grid. For identified chests, List preferences are saved in `config/chestlistview-chests.properties` by world save path or server address, dimension, and block position. Both halves of a verified double chest share one position key. The previous global view file is no longer read. The screen retains the scroll offset while switching views and clamps it on resize. New openings start at the first slot.
- Vanilla uses the same three-row menu for several containers, including ender chests, and servers can send it for custom GUIs. The open-screen event supplies the menu and title but lacks a reliable originating block identity. The three-row toggle is still available on these menus, with a view preference that lasts only for the open screen. Custom-titled menus, delayed or unmatched openings, and unverifiable chest pairs are excluded from persistent matching. Six-row menus without a matching double-chest click keep their original screen.
- Custom screen subclasses and touchscreen mode stay on their original screens. A very short window can disable the list toggle. Scrollbar dragging, filters, search and sorting are outside this prototype.
- Mouse drag distribution (quick craft), double-click collection and hotbar swaps still run through vanilla input using original slot identities. Scrolling and switching views are suspended during quick craft. Vanilla double-click collection can affect matching stacks outside the viewport. Validate these gestures in-game before relying on them.
- The mod registers no gameplay content or custom network payloads. A server without the mod is the intended setup. Multiplayer handshake and inventory behavior still require testing.

## Manual validation plan

| Test | Expected result |
| --- | --- |
| Open a single chest and switch twice | Grid is the default. List appears, then Grid restores the same stacks in the same positions. |
| Place distinct stacks across all 27 slots and scroll over the list | The first through last slots appear in order. Blank rows retain their place. The scrollbar stays within its track. |
| Add an enchanted book and enchanted equipment | Both show localized enchantments and applicable levels. Hover the icon or row text to view the vanilla tooltip. |
| Left- and right-click item rows and empty rows, including after scrolling | Pickup, half-stack pickup and single-item placement affect the indicated original slot. The carried stack follows the mouse. |
| Shift-click in both directions | Quick-move transfers between chest and player inventory with vanilla placement rules. |
| Use number keys over a row, then repeat immediately after scrolling | The just-hovered slot swaps with the selected hotbar slot. |
| Drag a carried stack across several rows and inventory slots | Only the slots passed over receive items. Wheel input during the drag leaves the viewport fixed. |
| Double-click matching items on one row | Vanilla collects matching stacks, including eligible stacks outside the viewport. |
| Switch views while carrying a stack, then close and reopen | View switching leaves the carried stack intact. Closing follows vanilla return/drop behavior. Reopening an identified chest restores its own chosen view. |
| Set chest A to List and leave chest B in Grid, then restart | Chest A opens in List. Chest B opens in Grid. Repeat with matching coordinates in separate dimensions, worlds, or servers. |
| Change GUI scale and resize with the list scrolled down | The screen stays centered, visible row count adjusts, and the last slot remains reachable. |
| Join a vanilla 1.21.1 server without the mod | Connect successfully and repeat pickup, placement and quick-move. Reopen or reconnect to confirm the server retained the correct contents. |
| Open a double chest by either half | Both halves restore the same view. In List, scroll through 54 slots and repeat the click and hotbar checks at the beginning, middle and end. |
| Open an ender chest or enable touchscreen mode before opening | Ender chest view changes only apply to the open screen. Touchscreen mode uses the original screen. |

Run the single-player interaction checks first, then repeat them on a server without the mod before packaging a public release.
