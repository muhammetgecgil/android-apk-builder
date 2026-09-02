# Aircraft Simulator 3D v95 — Airfield & Environment Realism Verified

Baseline: `aircraft-sim-v94-supersonic-flight-verified`
Development branch: `aircraft-sim-v95-airfield-environment-realism`
Verified branch: `aircraft-sim-v95-airfield-environment-realism-verified`
CI run: `33673851196`
Successful source commit: `ee40fc757f3a322c2f9acc852e35b3b35e40ba0d`

Debug APK: `Aircraft_Simulator_3D_v95_Airfield_Environment_Realism_Test.apk`
SHA-256: `d4a52e3132cae4821815b0aa0421418da004f6abc411b8108dfd759a9ce7e73f`
versionCode: `95`
versionName: `26.13-avm24.0-airfield-environment-realism-debug`
targetSdk: `36`

## Implemented

- Weather-driven visibility range and fog-bank state
- Wet-runway sheen and light reflection streaks during rain
- Runway microdetail: asphalt crack cues and larger aiming-point markings
- Night runway edge and centerline lighting with weather/daylight intensity scheduling
- Approach-light cues and taxi blue/green lighting
- Distant settlement/city lights at night
- Low-altitude ground-motion cues tied to speed and altitude
- Expanded cinematic camera package: chase, rear, right/left quarter, observer, low chase, wing, fly-by, runway, tower, orbit and automatic cinema
- Supersonic sonic-boom observer policy extended to fixed/cinematic observer-style cameras
- Existing v94 supersonic flight, v93 variable nozzle, v92 fighter audio, v91 attached lighting, v89 FCS and v88 mechanical systems retained

## Verification

- v95 source integration check passed
- `EnvironmentRealismModelTest` passed
- `SupersonicFlightModelTest` passed
- `FighterSoundModelTest` passed
- `FighterFlightControlSystemTest` passed
- `FighterLightingSystemTest` passed
- legacy unit-test suite passed in this CI run
- debug APK, unsigned release APK and unsigned Play AAB built successfully
- APK package/version/targetSdk/signature/icon verification passed
- downloaded APK SHA-256 independently rechecked against CI checksum and matched
