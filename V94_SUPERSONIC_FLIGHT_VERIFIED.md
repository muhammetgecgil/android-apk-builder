# Aircraft Simulator 3D v94 — Supersonic Flight Verified

Baseline: `aircraft-sim-v93-variable-nozzle-verified`
Development branch: `aircraft-sim-v94-supersonic-flight`
Verified branch: `aircraft-sim-v94-supersonic-flight-verified`
CI run: `33672078039`
Successful source commit: `3a1475655c5638793fc447009549836ec82c65a8`

Debug APK: `Aircraft_Simulator_3D_v94_Supersonic_Flight_Test.apk`
SHA-256: `01d40d1e47036789a6fbbf758bf353da8e056a3270ab95dc71582d982495dc91`
versionCode: `94`
versionName: `26.12-avm23.0-supersonic-flight-debug`
targetSdk: `36`

## Implemented supersonic package

- Shared altitude-aware Mach calculation based on local speed of sound
- Real afterburner-assisted acceleration capable of crossing and sustaining Mach 1+
- Mach-dependent wave-drag rise through the high-subsonic/transonic region
- Transonic buffet band centered around Mach 1, coupled into flight attitude and camera shake
- Mach 1 edge crossing detection and short sonic-boom pulse state
- Screen-space transonic condensation / moving shock halo
- Mach-cone visual effect whose cone angle narrows as Mach increases
- Mach-dependent camera field of view and high-speed camera lag
- Fifth low, world-fixed ground-observer camera for physically sensible sonic-boom playback
- Sustained supersonic audio rumble plus short N-wave-like observer sonic boom
- HUD Mach readout with wave-drag, buffet and SUBSONIC / TRANSONIC / MACH 1+ / SUPERSONIC state labels
- Existing v93 variable nozzle, v92 fighter audio, v91 attached lights, v89 FCS and v88 mechanical systems retained

## Verification

- v94 source integration check passed
- `SupersonicFlightModelTest` passed
- `FighterSoundModelTest` passed
- `FighterFlightControlSystemTest` passed
- `FighterLightingSystemTest` passed
- legacy unit-test suite passed in this CI run
- debug APK, unsigned release APK and unsigned Play AAB all built successfully
- APK package/version/targetSdk/signature/icon verification passed
- downloaded APK SHA-256 independently rechecked against the CI-generated checksum and matched
