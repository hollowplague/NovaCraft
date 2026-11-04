# Visual Parity Plan: 3D Held Hand & Blocks, 3D Inventory Icons, Bedrock UI

Comprehensive plan to upgrade **Minecraft Kotlin Edition** to full visual parity with classic Minecraft: first-person 3D arm and held block with swing animations, 3D isometric rendered block icons in the hotbar and inventory, Bedrock mobile beveled stone UI slots, and authentic directional block shading.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following core design decisions are confirmed:

- **Confirmed 3D Viewmodel Hand**: Render a 3D pixelated player arm and held block in the lower-right viewport with walking bobbing motion and an energetic punch/swing animation on mining and block placement.
- **Confirmed 3D Isometric Icons**: Replace 2D color swatches in hotbar slots and creative inventory with authentic 3D isometric voxel cubes (top face with full light, left face with shadow, right face with medium diffuse shading).
- **Confirmed Bedrock Mobile UI Style**: Classic Minecraft Bedrock mobile gray stone beveled hotbar slots with light/dark beveled borders, white active slot selector frame, and authentic beveled touch action buttons.
- **Confirmed Block Shading & Lighting**: Faithful Minecraft face illumination constants (Top = 1.0, North/South = 0.8, East/West = 0.65, Bottom = 0.5) blended with sunlight and distance fog.

---

## 1. Overview & Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Compose HUD Layer                          │
│                                                                 │
│   (Top Bar: Coords, FPS, Target, Settings)                      │
│                                                                 │
│                              +                                  │
│                         (Crosshair)                             │
│                                                                 │
│                                                    ┌───┐ ┌───┐  │
│   ┌──────┐                                        │BRK│ │PLC│  │
│   │  ●   │                                        └───┘ └───┘  │
│   │      │                       ┌───┐               ┌─────┐   │
│   └──────┘                       │BL │ (3D Block)    │JUMP │   │
│  (Joystick)                      └───┘               └─────┘   │
│                              ┌──────────┐                      │
│                              │3D ARM    │ (First-Person Hand)   │
│                              └──────────┘                      │
│   ┌───┬───┬───┬───┬───┬───┬───┬───┬───┐                         │
│   │⬡GR│⬡DR│⬡ST│⬡WD│⬡PL│⬡BR│⬡GL│⬡WT│⬡GW│ [...] (Bedrock Hotbar) │
│   └───┴───┴───┴───┴───┴───┴───┴───┴───┘                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Detailed Technical Breakdown

### 1. 3D First-Person Hand & Held Block Viewmodel (`GameRenderer.kt`)
- **Mesh Construction**:
  - Arm: 3D cuboid representing Steve's classic player forearm with cyan sleeve cuff and tanned skin.
  - Held Block: Miniature 3D cube rendered in front of the hand showing the currently active hotbar block with its exact textures from the atlas.
- **Viewmodel Rendering Pass**:
  - Rendered in a dedicated pass with depth buffer cleared or near-plane projection so the held item never clips into nearby walls or terrain.
  - **Natural Walking Bobbing**: Gentle sinusoidal sway synchronized with player horizontal velocity.
  - **Action Swing Animation**: When breaking or placing a block, triggers a 200ms downward and inward rotation swing arc.

### 2. 3D Isometric Inventory & Hotbar Block Icons (`IsometricBlockIcon.kt`)
- In Minecraft, blocks in the hotbar and inventory appear as small 3D cubes viewed at an isometric 30° angle.
- Create a dedicated Compose Canvas component `IsometricBlockIcon`:
  - **Top Face**: Diamond/rhombus polygon illuminated at 100% brightness using the block's top texture color and pixel pattern.
  - **Left Face**: Left vertical parallelogram shaded at 60% brightness (shadow).
  - **Right Face**: Right vertical parallelogram shaded at 80% brightness (diffuse).
  - Side textures (like grass side fringe, oak log bark, wood plank lines, cobblestone cracks) are rendered directly on the visible isometric faces.

### 3. Bedrock Mobile UI Styling (`GameControlsOverlay.kt`, `CreativeInventoryDialog.kt`)
- **Hotbar Slots**:
  - Contiguous 9-slot bar with Bedrock mobile stone beveling: `#8F8F8F` background, `#373737` top and left inner bevel, `#FFFFFF` bottom and right bevel highlights.
  - Active slot indicator: Classic white raised frame `#FFFFFF` with outer drop shadow that encases the selected slot.
- **Action Buttons (Break, Place, Jump)**:
  - Textured with classic Minecraft button frames (top-left highlight `#D0D0D0`, bottom-right shadow `#4A4A4A`, stone center `#8B8B8B`).
  - Tactile pressed depression effect when touched.
- **Creative Inventory Window**:
  - Classic Bedrock GUI stone-gray dialog `#C6C6C6` with dark inset item grid slots and crisp slot borders.

### 4. Authentic Directional Face Lighting (`Chunk.kt`)
- Embed classic Minecraft per-face brightness constants directly into the vertex ambient occlusion/lighting attribute:
  - Top Face (+Y): `1.0f`
  - Bottom Face (-Y): `0.50f`
  - North Face (+Z): `0.80f`
  - South Face (-Z): `0.80f`
  - East Face (+X): `0.65f`
  - West Face (-X): `0.65f`
- This ensures adjacent blocks naturally pop with 3D contrast and depth even when illuminated under direct overhead sunlight.

---

## 3. Verification Plan

1. **Compilation**: Verify build with `compile_applet`.
2. **Unit Tests**: Run `gradle :app:testDebugUnitTest` to verify engine stability.
3. **Visual & Interaction Validation**:
   - Inspect the lower-right corner of the screen: verify 3D arm and miniature held block are visible, bob naturally when walking, and execute a swing animation upon breaking or placing a block.
   - Verify all 9 hotbar slots render crisp 3D isometric blocks with shaded top, left, and right faces.
   - Verify the hotbar uses authentic Minecraft Bedrock beveled gray slots with the active white frame.
   - Open Creative Inventory: verify all blocks are presented as 3D isometric cubes.
   - Inspect world terrain: verify clean directional face lighting contrast across all block orientations.
