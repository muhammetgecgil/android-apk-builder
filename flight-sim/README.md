# Aircraft Simulator / Fixture Cockpit Sim v19 Foundation

This branch is the clean development foundation for the Android aircraft simulator.

## Imported evidence checked
- Android source package supplied as `android-apk-builder-agent-fixture-cockpit-v3-apk.zip`.
- Play listing art package supplied as `Fixture_Cockpit_Sim_PlayStore_Assets_v18.zip`.
- v18 installable APK supplied as `Fixture-Cockpit-Sim-F22-Game-Render-v18.0-APK.zip` and used as a binary reference, not committed to source control.

## Architecture direction
1. `sim/` — deterministic flight state, normalized controls, flight dynamics.
2. Android activity/input layer — phone IMU, touch/game controls and Bluetooth data link.
3. Rendering layer — Filament-based 3D aircraft/world/cockpit renderer.
4. Assets pipeline — model conversion and texture-atlas generation at build time.
5. CI — clean API 36 build, dependency resolution, APK verification and artifact publication.

## Security
No production keystore or signing password belongs in the repository. Debug APKs use the Android debug key. Play releases should use Play App Signing / protected CI secrets.

## Model licensing
The supplied v18 source pipeline references the SamJD261/F-22-Raptor model at a pinned revision and identifies it as GPL-3.0. Before commercial Play distribution, keep the required license/source obligations or replace it with an asset whose distribution terms match the product plan.

## Next simulator milestones
- Integrate `FlightDynamicsEngine` into the live render loop.
- Replace view-specific ad-hoc state with one `FlightState` source of truth.
- Add terrain/sky/world streaming and runway scene.
- Add cockpit instruments driven by simulation state.
- Add pause/reset/scenario selection and deterministic replay.
- Add performance telemetry and S24-class 60 FPS quality tiers.
