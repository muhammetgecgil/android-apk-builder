# Aircraft Simulator 3D v97 — Jet Blast & Ground Effects Verified

Baseline: `aircraft-sim-v96-surface-material-realism-verified`
Development branch: `aircraft-sim-v97-jet-blast-ground-effects`
Verified branch: `aircraft-sim-v97-jet-blast-ground-effects-verified`
CI run: `33677881419`
Successful source commit: `bc5b5b51f1b6cc5393d883059312b6846d8a4a26`

Debug APK: `Aircraft_Simulator_3D_v97_Jet_Blast_Ground_Effects_Test.apk`
SHA-256: `7cddfab6c559800a79df5037cd062a5be1b81b41e1fda31a67cc72480ffc9cc4`
versionCode: `97`
versionName: `26.15-avm26.0-jet-blast-ground-effects-debug`
targetSdk: `36`

## Implemented

- Throttle-driven jet-blast strength coupled to actual ground proximity and aircraft speed
- Stronger exhaust distortion and blast scheduling when afterburner is active
- Hot-air / heat-haze distortion behind the exhaust, with increased extent in afterburner
- Dry runway dust and fine particulate plume behind the engines
- Wet-runway water spray and mist fan linked to runway wetness
- Grass/dirt surface plume when the aircraft is outside the paved runway corridor
- Lightweight loose-object/FOD particles receive jet-blast impulse; fixed/heavy scenery is intentionally not unrealistically moved
- Effects fade with altitude and forward speed so airborne flight is not filled with ground dust/spray
- Bounded deterministic particle counts to keep Android rendering load controlled
- v96 material realism, v95 environment, v94 supersonic flight, v93 variable nozzle, v92 fighter audio, v91 lighting, v89 FCS and v88 mechanics retained

## Verification

- v97 source integration checks passed
- `JetBlastDynamicsModelTest` passed: afterburner strength, ground fade, wet/grass surface selection, debris threshold and high-AB distortion
- Surface material, environment, supersonic, fighter sound, fighter FCS and fighter lighting regression tests passed
- debug APK, unsigned release APK and Play AAB built successfully
- APK package/version/targetSdk/signature/icon verification passed
- downloaded APK SHA-256 independently matched the CI-generated checksum
