# Aircraft Simulator 3D — Canonical v88 Final Reference

This branch is the authoritative development baseline for Aircraft Simulator 3D from 2 September 2026 onward.

## User-supplied canonical APK
- File: `Aircraft_Simulator_3D_v88_Control_Surface_Airfoils_Test.apk`
- SHA-256: `1eb3fe5529ba5bee800182652d3c0afb69c70dba78ac12d078f86b308e495447`
- versionCode: `88`
- versionName: `26.6-avm18.0-mechanical-dynamics-debug`
- Package: `com.mg.fixturecockpitsim.dev`
- Target SDK: `36`

## Exact GitHub provenance
- Source/build commit that produced the canonical APK: `94d1c6463de7b0988bf01630784c5e42c5249e15`
- Original branch: `aircraft-sim-v88-mechanical-upgrade`
- GitHub Actions run: `33634955402`
- Original artifact ID: `9848465481`
- Artifact filename: `Aircraft Simulator 3D v88 - Mechanical Dynamics Test.apk`
- Verification: downloaded GitHub artifact and user-supplied APK are byte-for-byte identical and have the same SHA-256.

## Approved icon integration
- Launcher icon asset: `app/src/main/res/drawable-xxxhdpi/ic_aircraft_simulator_final.webp`
- Icon SHA-256: `eeca9548ffbceab42c02b2a2f887ae9a1ca5706222e2803897de4080a6caf976`
- Manifest launcher icon and round icon point to `@drawable/ic_aircraft_simulator_final`.

## Development rule
All future Aircraft Simulator work must branch or advance from `aircraft-sim-v88-final-reference` unless the user explicitly selects another baseline. Older Aircraft Simulator branches and unrelated ChatGPT/GitHub projects must not be treated as the active baseline.
