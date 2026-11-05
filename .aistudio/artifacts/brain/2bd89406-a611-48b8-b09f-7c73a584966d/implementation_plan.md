# Implementation Plan: NovaCraft 1.0.5 — Visible POV Hand, Destroy Particles & Release APK

Deliver the **NovaCraft 1.0.5 Update** featuring:
1. **Fully Visible First-Person POV Hand**:
   - Fix the viewmodel projection matrix and arm geometry in `ViewmodelRenderer.kt`.
   - Ensure Steve's forearm (cyan sleeve) and hand (peach fist) enter from the bottom-right corner and extend into clear view when empty or holding items, with walking bobbing and punch swings.
2. **Block Destruction Particles (16 Debris Cubes with Physics)**:
   - When a block breaks, spawn 16 small debris particles centered at the destroyed voxel.
   - Debris particles sample the destroyed block's texture colors, burst outward in 3D with random trajectories ($v_x, v_y, v_z$), and fall under gravity with ground bounce/fade.
   - Render particles efficiently in a dedicated OpenGL pass using instanced/batched quads.
3. **1.0.5 Release Build & Export**:
   - Update `versionCode = 6` and `versionName = "1.0.5"` in `app/build.gradle.kts`.
   - Update title version in `MainMenuScreen.kt` to `v1.0.5`.
   - Build signed release APK and export to `ApkCraft/novacraft-1.0.5.apk`.

---

### Technical Architecture & Changes

#### 1. `ViewmodelRenderer.kt` (POV Hand Visibility)
- **Problem**: Steve's hand coordinates were translated too far down/right (`posX = 0.28f, posY = -0.22f, posZ = -0.48f`), and the arm cuboid was constructed with negative $Z$ pointing away from the camera while the model was rotated, placing the hand below the bottom screen crop.
- **Fix**:
  - Re-anchor Steve's arm:
    - Base position: `posX = 0.22f, posY = -0.16f, posZ = -0.38f`.
    - Forearm starts at bottom-right corner of the viewport and angles forward-left toward the screen center.
    - Sleeve segment: cyan `#00A8A8` cuboid.
    - Fist segment: peach `#C88E68` cuboid.
    - When holding a block: block rests cleanly in the palm/fingers of the hand.
    - When empty: hand forms a classic Steve punch fist.
  - Disable face culling during viewmodel pass (`GLES20.glDisable(GLES20.GL_CULL_FACE)`) to ensure all faces of the arm are always rendered.

#### 2. `ParticleSystem.kt` & `GameRenderer.kt` (Block Destroy Particles)
- Create `com.example.game.engine.ParticleSystem.kt`:
  - `Particle(x, y, z, vx, vy, vz, size, colorR, colorG, colorB, maxLife, currentLife)`
  - `spawnBlockBreak(blockX, blockY, blockZ, blockType)`:
    - Spawns 16 debris particles from the broken voxel.
    - Particles burst outward in all directions with random velocities:
      $$v_x \in [-1.8, 1.8], \quad v_y \in [1.5, 4.0], \quad v_z \in [-1.8, 1.8]$$
    - Gravity: $g = -9.8\text{ m/s}^2$.
    - Size: $0.08\text{m}$ mini-cubes.
    - Colors sampled directly from the block's texture palette (e.g. grass green + dirt brown for grass, wood brown for logs, gray for stone).
  - `render(mvpMatrix, shader)`:
    - Renders all active debris particles as 3D camera-facing billboards/mini-cubes with fading alpha as lifetime approaches zero.
- Wire into `GameRenderer.kt`:
  - In `onDrawFrame`, update particles with `deltaSec`.
  - When `miningProgress >= 1.0f` breaks a block: call `particleSystem.spawnBlockBreak(...)`.
  - Render particles after chunk rendering in `PASS 2.5`.

#### 3. Version Bump & APK Release
- In `app/build.gradle.kts`:
  - `versionCode = 6`
  - `versionName = "1.0.5"`
- In `MainMenuScreen.kt`:
  - Update version string to `NovaCraft v1.0.5 • Infinite Creative`.
- Run Gradle `assembleRelease` with keystore credentials:
  - Copy `app/build/outputs/apk/release/app-release.apk` to `ApkCraft/novacraft-1.0.5.apk`.
  - Ensure file permissions `644`.

---

### Verification Checklist
- [ ] Steve's first-person arm with cyan sleeve and peach fist is clearly visible in the lower right when holding an empty slot (`AIR`).
- [ ] When holding a block, the hand supports the block at a natural angle.
- [ ] Destroying any block produces 16 flying debris particles matching the block's color that scatter and fall with gravity.
- [ ] Main menu displays `v1.0.5`.
- [ ] `compile_applet` succeeds without errors.
- [ ] Signed release APK built and exported to `ApkCraft/novacraft-1.0.5.apk`.
