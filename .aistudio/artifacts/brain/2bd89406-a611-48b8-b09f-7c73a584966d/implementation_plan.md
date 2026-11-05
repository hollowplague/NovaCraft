# NovaCraft Update 1.1.5: Worlds System, Auto-Save, Creative Inventory Redesign & Release APK

Comprehensive expansion adding authentic Minecraft Bedrock worlds management, customizable world creation, background auto-save persistence, and an overhauled category-based creative inventory.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed based on your preferences:
> - **Inventory Slot Assignment**: Tap any catalog block in the upper pane to instantly place it into the currently selected hand inventory (hotbar) slot.
> - **World Persistence**: Fast local JSON storage in the app's internal files directory (`context.filesDir/worlds/`), automatically serializing world metadata, seed, player position, hotbar, and modified block coordinates on auto-save and pause.
> - **Flat World Generation**: Generates the authentic classic Bedrock flat preset: 1 Bedrock layer ($Y=0$), 2 Dirt layers ($Y=1, 2$), and 1 Grass block surface ($Y=3$), with all upper layers as air.
> - **Release APK**: Signed release APK will be built and exported to `ApkCraft/novacraft-1.1.5.apk`.

---

## 1. Overview & Core Concept

- **What It Delivers**:
  1. **Worlds Screen**: The "Play" button on the Title Screen opens a dedicated Worlds tab listing all existing worlds with world name, creation date, seed, type badge ("Infinite" or "Flat"), and Play/Delete actions.
  2. **Create New World Screen**: Top-right "New" button on the Worlds tab opens a Bedrock-themed creation form where players can name their world, select the generation type ("Infinite" vs "Flat"), enter or randomize a numeric/text seed, and generate the world.
  3. **Auto-Save & World Persistence**: Continuous background auto-save every 25 seconds and immediate save when pausing or quitting to the title screen. Re-opening a world restores exact player coordinates, look orientation, hotbar loadout, and all placed/broken blocks.
  4. **Redesigned Bedrock Creative Inventory**:
     - Left category sidebar with 4 tabs: 🔍 All / Search, 🧱 Construction, 🌿 Nature, and 💎 Precious/Items.
     - Upper pane: Grid of 3D isometric block cards in the active category.
     - Bottom pane: 9-slot Hand Inventory (Hotbar) with active slot highlighted. Tapping any catalog item fills the active slot.
  5. **Signed Release APK**: Version bumped to `1.1.5` (`versionCode = 10`) and signed release APK exported to `ApkCraft/novacraft-1.1.5.apk`.

---

## 2. User Experience & Visual Design

### Screen Navigation Flow
```
┌────────────────────────┐
│     Title Screen       │
│  [Play]    [Settings]  │
└───────────┬────────────┘
            │ [Play]
            ▼
┌──────────────────────────────────────────────┐
│  Worlds Tab              [+ New World] Button│
│  ┌────────────────────────────────────────┐  │
│  │ 🌍 Survival Alpha • Infinite • Today   │  │
│  │    [Play World]   [Delete]             │  │
│  ├────────────────────────────────────────┤  │
│  │ 🟩 Flat Testing • Flat • Yesterday     │  │
│  │    [Play World]   [Delete]             │  │
│  └────────────────────────────────────────┘  │
└──────────────┬───────────────────────────────┘
               │ [+ New World]
               ▼
┌──────────────────────────────────────────────┐
│  Create World Screen (Bedrock Style)         │
│  - World Name: [ My Creative World       ]   │
│  - World Type: [ (•) Infinite   ( ) Flat ]   │
│  - Seed:       [ 482910452               ]   │
│  [Create World]               [Cancel]       │
└──────────────────────────────────────────────┘
```

### Redesigned Creative Inventory Layout
```
┌─────────────────────────────────────────────────────────────┐
│  Creative Inventory                                    [✕]  │
├───────┬─────────────────────────────────────────────────────┤
│ 🔍 All│  [Stone]    [Cobble]   [Brick]   [Planks]  [Glass]  │
│ 🧱 Bld│  [Sand]     [Leaves]   [Wood]    [Gold]    [TNT]    │
│ 🌿 Nat│  [Bedrock]  [Books]    [Water]   ...       ...      │
│ 💎 Itm│                                                     │
├───────┴─────────────────────────────────────────────────────┤
│  Hand Inventory (Hotbar)                                    │
│  ┌───┬───┬───┬───┬───┬───┬───┬───┬───┐                      │
│  │[1]│[2]│[3]│[4]│[5]│[6]│[7]│[8]│[9]│                      │
│  └───┴───┴───┴───┴───┴───┴───┴───┴───┘                      │
└─────────────────────────────────────────────────────────────┘
```

- **Styling**:
  - Classic Bedrock stone headers and darkened backdrop panels (`#1F1B14`, `#3C3C3C`, `#222222`).
  - Active hotbar slot framed with bold white outline and emerald-green selection glow (`#55FF55`).
  - Left category selector with tab icons and Bedrock beveled highlight buttons.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Fast Delta-Based Local JSON Storage**:
  - *Chosen Approach*: Store world configuration (seed, type, name, timestamp, player position, hotbar) and block modifications as a sparse delta map `modifiedBlocks: Map<Long, Int>`.
  - *Why*: Procedural generation generates the base terrain from the seed in milliseconds; storing only user-authored edits makes world files tiny (<50KB), saves instantaneously without freezing the GL thread, and loads instantly.
- **Decision 2: Flat World Implementation**:
  - *Chosen Approach*: Support `WorldType.FLAT` in `VoxelWorld` with bedrock at $Y=0$, dirt at $Y=1, 2$, and grass at $Y=3$.
  - *Why*: Matches Bedrock standard flat preset and allows players to build immediately on a pristine level plane.
- **Decision 3: Category Filtering**:
  - *Chosen Approach*: Categorize `BlockType` enum into `ALL`, `CONSTRUCTION`, `NATURE`, and `ITEMS`.
  - *Why*: Makes finding specific blocks fast on mobile without scrolling through large unorganized lists.

---

## 4. Technical Architecture & Implementation Steps

```
┌─────────────────────────────────────────────────────────────┐
│                      WorldSaveManager                       │
│  - loadWorlds(): List<WorldMetadata>                        │
│  - saveWorld(worldData): Boolean                            │
│  - deleteWorld(worldId): Boolean                            │
│  - loadWorldData(worldId): WorldSaveData                    │
└──────────────────────────────┬──────────────────────────────┘
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
┌─────────────────────────────┐ ┌─────────────────────────────┐
│         VoxelWorld          │ │     Navigation State        │
│  - seed: Long               │ │  MAIN_MENU                  │
│  - worldType: FLAT/INFINITE │ │  WORLDS_LIST                │
│  - modifiedBlocks: Map      │ │  CREATE_WORLD               │
│  - getModifiedBlockDeltas() │ │  IN_GAME                    │
└─────────────────────────────┘ └─────────────────────────────┘
```

### Planned File Changes

1. **`WorldSaveManager.kt` (New Engine / Data Layer)**:
   - Data classes: `WorldMetadata`, `WorldSaveData`, `BlockDelta`.
   - File I/O for `worlds.json` and per-world JSON save files in `context.filesDir/worlds/`.
   - Auto-save integration.
2. **`VoxelWorld.kt`**:
   - Add `worldType` parameter ("infinite" vs "flat").
   - Flat terrain generation routine.
   - Record block edits in a delta map for persistence, and add `applyBlockDeltas()`.
3. **`WorldsScreen.kt` & `CreateWorldScreen.kt` (New UI)**:
   - `WorldsScreen`: Worlds list, "New" button in top-right, play world, delete world, back to menu.
   - `CreateWorldScreen`: Name input, Infinite/Flat selector, Seed input, Create & Cancel actions.
4. **`CreativeInventoryDialog.kt` (Overhaul)**:
   - Add left category sidebar (All, Construction, Nature, Items).
   - Display upper category blocks grid.
   - Display lower 9-slot Hand Inventory with active slot assignment.
5. **`MainActivity.kt`**:
   - State machine: `ScreenState` enum (`MAIN_MENU`, `WORLDS_LIST`, `CREATE_WORLD`, `IN_GAME`).
   - Auto-save timer and lifecycle hooks.
   - World switching and initialization.
6. **`app/build.gradle.kts` & `MainMenuScreen.kt`**:
   - Bump version to `1.1.5` (`versionCode = 10`, `versionName = "1.1.5"`).
   - Build signed release APK and export to `ApkCraft/novacraft-1.1.5.apk`.
