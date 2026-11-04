# KotlinCraft 0.2.5: MCPE Touch Controls & Destruction Mechanics

Plan for **Update 0.2.5**, transitioning KotlinCraft to classic Pocket Edition (MCPE) touch mechanics: deleting the crosshair and separate break/place buttons, introducing the hollow touch circle with radial progress fill and block hardness, progressive block cracking animations, smooth auto-jump, an authentic Bedrock settings menu, and a clean immersive HUD with empty starter hotbar.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following design choices were confirmed in Phase 1 clarifying answers and will be implemented upon approval:

- **Confirmed Decision 1 (Touch Controls)**: Crosshair and manual Break/Place buttons are removed. Touching a block taps to place, holding initiates mining with the MCPE hollow circle, and swiping in empty areas looks around.
- **Confirmed Decision 2 (Auto-Jump)**: Player automatically and smoothly steps up single-block obstacles while walking forward without needing to press jump.
- **Confirmed Decision 3 (Hotbar Setup)**: All 9 hotbar slots start completely empty (`AIR`), ready to be populated from the Creative Inventory.

---

### 1. Overview & Core Concept

- **What It Delivers**:
  1. **Classic MCPE Touch Interaction**: Crosshair removed. Tap on a block in the 3D world to place currently held block; press and hold to display the animated MCPE hollow circle that fills from the center as the block is mined.
  2. **Block Hardness Cooldowns**: Blocks have authentic destruction times (e.g. Leaves 0.3s, Dirt 0.7s, Wood 1.8s, Stone 2.2s).
  3. **Progressive Breaking Cracks**: Multi-stage cracking lines rendered on the target block face as it takes damage.
  4. **Smooth Auto-Jump**: Smooth step-up physics when walking against 1-block terrain elevation.
  5. **Redesigned Bedrock Jump Button**: Dedicated square stone button on bottom-right with iconic jump chevron.
  6. **Clean HUD**: Removed top-left coordinates/FPS and crosshair for a cinematic, uncluttered view.
  7. **Redesigned Settings Menu**: Bedrock stone aesthetic with sliders for FOV and sensitivity; removed "Invert Y" and "Fly Mode".
  8. **D-Pad Center Jump Removed**: D-pad center button is a passive visual anchor; jumping is handled by the dedicated Jump button and auto-jump.
  9. **Empty Hotbar & 0.2.5 Release**: Starts with 9 empty slots; version bumped to `0.2.5` and exported to `ApkCraft/kotlincraft-0.2.5.apk`.

---

### 2. User Experience & Visual Design

#### HUD Layout (Update 0.2.5)
```
┌────────────────────────────────────────────────────────┐
│                                            [ ❚❚ Pause ]│
│                                                        │
│                  (⭕ Hollow Circle)                    │
│                [Fills from center...]                  │
│                                                        │
│                                                        │
│      ▲                                                 │
│   ◄ [•] ►                                      [ ▲ ]   │
│      ▼                                         Jump    │
│               [ · · · · · · · · · ••• ]                │
│                 Empty Bedrock Hotbar                   │
└────────────────────────────────────────────────────────┘
```

- **Hollow Touch Circle**: When holding a block to mine, a circular ring (`#FFFFFF` with subtle drop shadow) appears at the touch point. An inner disc expands outward from the center proportional to destruction progress `0.0 .. 1.0`.
- **Breaking Animation**: The targeted block face renders cracking patterns that intensify through 10 stages (0 to 9) before popping.
- **Clean Horizon**: No crosshair or text chips blocking the view.

---

### 3. Technical Architecture & Data Strategy

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    KotlinCraft 0.2.5 Touch & Physics Pipeline                │
├──────────────────────────────────────────────────────────────────────────────┤
│ 1. Player.kt                                                                 │
│    └── Smooth auto-jump: checks block at foot vs knee level and lifts player │
│                                                                              │
│ 2. GameRenderer.kt                                                           │
│    ├── Raycasting directly from touch screen coordinates (screenX, screenY)  │
│    ├── Destruction timer & stage tracker (0..9) based on BlockType hardness │
│    └── Cracking wireframe/crack overlay pass on damaged block face           │
│                                                                              │
│ 3. GameControlsOverlay.kt                                                    │
│    ├── Touch listener: tap -> placeBlock(), hold -> mineBlock()              │
│    ├── Canvas rendering of MCPE Hollow Circle with center radial fill       │
│    ├── Redesigned Jump button on bottom right                                │
│    └── Removed top-left coordinates, crosshair, break/place buttons          │
│                                                                              │
│ 4. GameSettingsDialog.kt                                                     │
│    └── Removed Invert Y & Fly; authentic Minecraft pause layout             │
│                                                                              │
│ 5. MainActivity.kt                                                           │
│    ├── Default hotbar: 9 x BlockType.AIR                                     │
│    └── Hooking touch interaction into GameRenderer                           │
│                                                                              │
│ 6. app/build.gradle.kts                                                      │
│    └── versionCode=4, versionName="0.2.5" -> ApkCraft/kotlincraft-0.2.5.apk  │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Detailed Component Changes:

1. **Touch Interaction & Hollow Circle (`GameControlsOverlay.kt`)**:
   - Replace look drag area with combined Look/Tap/Hold touch detector:
     - Short tap (< 250ms) on a block places the held block against that face.
     - Hold (> 250ms) starts mining cooldown; renders the hollow circle at touch coordinate `(touchX, touchY)` with expanding inner progress circle.
     - Dragging your finger rotates the camera if not holding a block.
2. **Block Cracking Shader Pass (`GameRenderer.kt`, `VoxelShader.kt`)**:
   - `blockHardness(type)`: Dirt/Sand (0.6s), Wood (1.6s), Stone (2.0s), Leaves (0.3s), Glass (0.4s).
   - Draw procedural cracking lines or overlay on the damaged block face during mining.
3. **Smooth Auto-Jump (`Player.kt`)**:
   - In `Player.update()`: when moving forward (`velX != 0` or `velZ != 0`), inspect block ahead at foot level. If solid and space above is empty, smoothly elevate `posY += 1.0f` with positive vertical velocity.
4. **Settings Dialog (`GameSettingsDialog.kt`)**:
   - Bedrock stone gray dialog with "Resume Game", "Sensitivity", "FOV", and "Haptic Feedback".
   - Completely removes "Invert Y" and "Fly Mode".
5. **Initial Hotbar (`MainActivity.kt`)**:
   - Set initial `hotbarSlots = List(9) { BlockType.AIR }`.
6. **Packaging**:
   - Update `app/build.gradle.kts` to `0.2.5` (versionCode 4).
   - Build signed release APK and export to `ApkCraft/kotlincraft-0.2.5.apk`.

---

### 4. Technical Execution Steps (Upon Approval)

1. **Physics & Engine**:
   - Implement smooth auto-jump in `Player.kt`.
   - Implement block hardness, cracking progress, and touch raycasting in `GameRenderer.kt` and `VoxelShader.kt`.
2. **HUD & Controls**:
   - Overhaul `GameControlsOverlay.kt`: remove crosshair, coordinates, and break/place buttons; add MCPE hollow circle with center fill and redesigned Bedrock jump button; disable jump on D-pad center.
   - Clean up `GameSettingsDialog.kt`: remove Invert Y and Fly mode.
   - Update `MainActivity.kt` with empty starter hotbar and touch hooks.
3. **Build & Export**:
   - Bump version to `0.2.5` in `app/build.gradle.kts`.
   - Compile signed release APK to `ApkCraft/kotlincraft-0.2.5.apk`.
4. **Verification**:
   - Verify with `compile_applet` and check files in `ApkCraft/`.
