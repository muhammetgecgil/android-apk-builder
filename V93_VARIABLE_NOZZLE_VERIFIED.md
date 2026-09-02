# Aircraft Simulator 3D v93 — Variable Fighter Nozzle Verified

Baseline: `aircraft-sim-v92-fighter-audio-verified`
Active development branch: `aircraft-sim-v93-variable-nozzle`
CI run: `33669199221`
Successful source commit: `baa97ff2ce329be3d1973f4932ea42ea35fc08da`

Debug APK: `Aircraft_Simulator_3D_v93_Variable_Nozzle_Test.apk`
SHA-256: `d8b39c2c2f6e28282d7a432dfbb83dc632c7db9abd490b80de66175793f44413`
versionCode: `93`
versionName: `26.11-avm22.0-variable-nozzle-debug`
targetSdk: `36`

Implemented nozzle package:
- 18 overlapping outer iris petals per nozzle
- staggered inner petal layer
- 18-link actuator/synchronizing-ring mechanism
- deeper exhaust liner with visible liner bands
- variable nozzle/throat schedule: open at idle, closes toward dry/military power, re-opens in afterburner
- outer, inner and heat-shield layers move coherently
- segmented translucent afterburner plume instead of an opaque pink light tube
- narrow blue/white flame core
- orange-biased 3D shock-diamond cells
- longer heat-haze/distortion volume
- existing v92 fighter audio, v91 attached lights, v89 FCS and v88 mechanical systems retained

Verification:
- v93 nozzle source integration passed
- FighterFlightControlSystem tests passed
- FighterLightingSystem tests passed
- FighterSoundModel tests passed
- full debug/release/AAB build passed
- APK package/version/targetSdk/signature/icon checks passed
