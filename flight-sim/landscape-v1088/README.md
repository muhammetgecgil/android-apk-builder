# Aircraft Simulator 3D v108.8 — Realistic Landscape

This is a targeted working-copy update of the user-supplied v108.7 APK.
The original v108 reference and the supplied v108.7 APK remain unchanged.

## Baseline

- Input: `Aircraft_Simulator_3D_v108_7_WingRootClean_NoseGearRetractOnly_TestSigned.apk`
- SHA-256: `16e678c3bea41b359e99ea471cb97a84a72d110464793c6d7b5b2994d04e600f`
- Input package: `com.mg.fixturecockpitsim.v17`
- Output package: `com.mg.fixturecockpitsim.scenery1088`
- Output label: `Aircraft 3D Manzara`
- Output version code: `10808`
- Output version name: `108.8-realistic-landscape-test`
- Output signer: existing repository `flight-sim/ci/aircraft-dev.keystore.b64`, development use only.
- Output APK SHA-256: `ff10b59a42d760f9b0b17cb846938e5625198da401c0f705108914fdbaaa60bc`

The independent test package installs alongside v108.7 because the private key
of the uploaded APK is unavailable. It does not import the old app's settings.

## What changes

- Terrain grid: 80 × 60 to 161 × 129; 9,322 to 40,960 triangles per terrain.
- Procedural, deterministic warped ridges, meandering valleys, coastal hills,
  dunes, eroded plateaus and a moonlit coastline. These are synthetic landscapes,
  not satellite imagery or geographically exact elevation data.
- Smooth normals calculated from the sampled surface. Direct primitive buffers
  replace boxed per-vertex float collections. Geometry builds once, outside the
  frame loop. Five terrain buffers total about 17.2 MB.
- Slope and height dependent vegetation, layered rock and high-altitude snow;
  near-shore water, sand and fine ground variation; distance and height dependent haze.
- Base vertical field of view: 29.5° to 34.5°. Far clip: 280 to 480 scene units.
- Ground extends behind the camera to avoid a visible bottom edge.

Only `CinematicTerrainMesh`, the terrain branch of the existing renderer shader,
shader position/normal precision, and two projection constants change in code.
Existing aircraft material branches, flight controls, physics, weather, route,
wing-root cleanup and nose-gear retraction remain from the supplied APK.
The manifest changes only test package identity, app label and version metadata.

## Rebuilding the APK copy

Tools are collected by `.github/workflows/aircraft-v108-8-tools.yml`:
JDK 17, Android build-tools 35.0.0, smali/baksmali 2.5.2.
Run in a workspace containing this directory as `landscape-v1088/`, its sibling
`work/`, and the original APK. Never use the old repository source tree alone to
regenerate v108.7; that APK includes later binary patches.

1. Extract `classes3.dex` and `classes4.dex` from the input into `work/`.
2. Use baksmali to disassemble them into `work/smali3/` and `work/smali4/`.
3. Extract the complete original vertex/fragment shader strings from classes3
   into `work/classes3.dex-vertex.glsl` and `work/classes3.dex-fragment.glsl`.
4. Compile `src/com/mg/fixturecockpitsim/visual/CinematicTerrainMesh.java` with
   `javac --release 8`, D8 with `--min-api 26`, then baksmali into `work/terrain-smali/`.
5. Run `python landscape-v1088/patch_renderer.py`. It requires exact shader and
   instruction anchors and copies the new terrain class into `work/smali4/`.
6. Assemble `work/smali3/` and `work/smali4/` as `work/patched-classes3.dex` and
   `work/patched-classes4.dex` respectively.
7. Run `python landscape-v1088/package_apk.py ORIGINAL.apk work/landscape-unsigned.apk`.
8. Sign the result with the stable development signer using apksigner v2/v3.
   Keep the test package identity; do not present this as a Play production build.

## Validation and limits

- JVM checks: finite geometry, normal lengths, upward triangle winding,
  bounded elevation, nondegenerate triangles, deterministic repeated builds.
- GitHub workflow `.github/workflows/aircraft-v108-8-render.yml` compiles the exact
  vertex and fragment shaders under Mesa EGL/GLES and draws all five terrains from
  four camera angles. PNGs show the isolated 3D scenery against a reference sky;
  they are not screenshots from the Android app.
- Final DEX files disassemble identically to their assembled source for every class.
- APK ZIP integrity, v2/v3 signatures, 16 KiB native-library alignment and 4-byte
  alignment of other stored entries checked after signing.
- Only AndroidManifest.xml, classes3.dex and classes4.dex differ from the input;
  all native libraries, other DEX files, resources and assets retain identical bytes.
- No physical Android device or Android emulator was available. Installation,
  actual flight integration and frame rate on Samsung S24 Ultra need device testing.
