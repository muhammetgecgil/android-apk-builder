# Aircraft Simulator 3D v92 — Fighter Audio Verified

Baseline: `aircraft-sim-v91-wingtip-lighting-verified`
Active development branch: `aircraft-sim-v92-fighter-audio`
CI run: `33667320388`

Debug APK: `Aircraft_Simulator_3D_v92_Fighter_Audio_Test.apk`
SHA-256: `c4edeb8aad254706f502f800813035fe04a46c1262139281312ba8bdff2a0bd7`
versionCode: `92`
versionName: `26.10-avm21.0-fighter-audio-debug`
targetSdk: `36`

Implemented audio layers:
- twin-engine core/rumble
- intake airflow
- fan whine
- turbine whine
- jet exhaust
- afterburner roar/crackle
- speed-dependent wind
- altitude-aware Mach/transonic buffet
- gear hydraulic movement and endpoint clunk
- flight-control-surface hydraulic/servo movement
- tyre/runway rolling noise
- brake squeal
- touchdown thump
- stereo twin-engine separation
- optional cockpit low-pass/muffling path for a future internal cockpit camera

Important physical decision: sonic boom is not played for the current aircraft-following chase/rear/quarter cameras. A true boom is reserved for a future world-fixed fly-by/tower observer crossing the shock cone.

Architecture correction: the duplicate `FlightSoundEngine` previously owned by `Jet3DView` was removed. `FlightRuntimeActivity` is now the single AudioTrack owner and feeds actual flight/mechanical state to the sound engine.
