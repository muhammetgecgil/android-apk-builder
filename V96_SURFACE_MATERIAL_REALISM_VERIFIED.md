# Aircraft Simulator 3D v96 — Surface & Material Realism Verified

Baseline: `aircraft-sim-v95-airfield-environment-realism-verified`
Development branch: `aircraft-sim-v96-surface-material-realism`
Verified branch: `aircraft-sim-v96-surface-material-realism-verified`
CI run: `33676453770`
Successful source commit: `29eb66a5ec41d73ed23b4c6e55fe806a420c7e6c`

Debug APK: `Aircraft_Simulator_3D_v96_Surface_Material_Realism_Test.apk`
SHA-256: `a9a3759c159b30135870bdfae0789dee089219e35fcde2030d86883185b78df3`
versionCode: `96`
versionName: `26.14-avm25.0-surface-material-realism-debug`
targetSdk: `36`

## Implemented

- Removed the all-over procedural panel/rivet grid from the primary fighter skin shader
- Clean semi-matte RAM-coated main airframe with subtle micro/macro roughness variation
- Forward fuselage/radome receives a distinct non-metallic, more matte material response
- RAM tape only at plausible radome, wing/control-surface and tail structural joins
- Sparse large maintenance/removable panel outlines instead of uniform surface lines
- Flush fasteners grouped only around maintainable access panels
- Canopy given sharper glossy/environment reflection behavior
- Variable nozzle/metal hardware given strongly metallic low-roughness response
- Landing-gear tyres given near-fully-matte non-metallic response
- v95 environment, v94 supersonic flight, v93 variable nozzle, v92 audio, v91 lighting, v89 FCS and v88 mechanics retained

## Verification

- v96 source integration checks passed
- `SurfaceMaterialProfileTest` passed
- Environment, supersonic, fighter sound, fighter FCS and fighter lighting regression tests passed
- debug APK, unsigned release APK and Play AAB built successfully
- APK package/version/targetSdk/signature/icon verification passed
- downloaded APK SHA-256 independently matched the CI-generated checksum
