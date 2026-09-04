# Fixture Cockpit Sim — Play Store release pack

## Source of truth
The complete Android project lives under `flight-sim/`. The v18 renderer, model converter, texture-atlas builder, Android sources and GitHub Actions build are kept in this folder.

## 2026 Play readiness
- Move `compileSdk` and `targetSdk` to API 36 before submitting a new app/update on or after 2026-08-31.
- Produce an Android App Bundle (`bundleRelease`) for Play distribution.
- Keep the existing applicationId if this is an update to an existing Play listing.
- Do not publish the repository's development signing password/keystore. Configure Play/App Signing and CI secrets for production release signing.

## Icon set
Use an adaptive launcher icon with separate foreground/background layers and a monochrome layer. Design on a 108 x 108 dp canvas and keep the critical aircraft mark inside the safe central region. Also export a 512 x 512 PNG for the Play listing.

Suggested art direction: dark graphite/black rounded-square background, silver aircraft silhouette viewed from above/three-quarter angle, small amber/orange cockpit highlight, no tiny text. This remains recognizable at launcher size.

## Store graphic set
Create/export:
- 512 x 512 PNG Play Store icon
- 1024 x 500 feature graphic
- Phone screenshots showing the 3D aircraft view, cockpit/telemetry and controls
- Tablet screenshots after adaptive-layout verification

## Release commands
```bash
cd flight-sim
gradle :app:assembleDebug
gradle :app:bundleRelease
```

## Recommended next development branch
Keep experimental renderer/model work on this branch and merge to `main` only after the APK/AAB build, install/update path and model-license checks pass.
