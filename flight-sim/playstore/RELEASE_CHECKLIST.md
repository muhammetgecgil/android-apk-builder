# Aircraft Simulator 3D — Google Play Production Release Checklist

## Build and identity
- [ ] applicationId is `com.mg.fixturecockpitsim` for production.
- [ ] versionCode is higher than every previously published production version.
- [ ] versionName is user-facing and does not rely on debug/internal naming.
- [ ] targetSdk = 36 or higher for submissions from 2026-08-31.
- [ ] compileSdk supports the selected target.
- [ ] Build a signed Android App Bundle (.aab), not only an APK.
- [ ] Production bundle uses the production upload key / Google Play App Signing flow.
- [ ] Public development keystore is not used for production.

## Code and permissions
- [ ] Review AndroidManifest.xml permissions.
- [ ] Remove unused sensitive permissions.
- [ ] Review all Gradle dependencies and third-party SDK data behavior.
- [ ] Confirm no accidental debug endpoints, secrets or test credentials.
- [ ] Confirm release build launches and completes demo mission.
- [ ] Confirm hangar → taxi → RWY27 → takeoff → scenic flight → approach → landing loop.
- [ ] Confirm aircraft remains visually aligned with runway during ground phase.
- [ ] Confirm landing gear does not intersect the fuselage.
- [ ] Confirm audio lifecycle behaves correctly on pause/resume/background.

## Play Console app content
- [ ] Privacy policy URL is live and public.
- [ ] Privacy policy is accessible from inside the app if required by policy/data behavior.
- [ ] Data Safety form matches real app behavior exactly.
- [ ] Ads declaration completed.
- [ ] App access declaration completed.
- [ ] Target audience declaration completed.
- [ ] IARC content rating questionnaire completed.
- [ ] Government app declaration completed if shown by Console.
- [ ] News / health / financial feature declarations answered accurately.
- [ ] Account deletion declaration completed; if account creation is ever added, deletion paths exist.

## Store listing
- [ ] App name finalized: Aircraft Simulator 3D.
- [ ] Short description finalized.
- [ ] Full description finalized.
- [ ] 512x512 Play icon uploaded.
- [ ] 1024x500 feature graphic uploaded.
- [ ] At least 2 compliant screenshots uploaded; recommended 8-scene set prepared.
- [ ] Support email is verified and monitored.
- [ ] Website/privacy policy URL works without login.
- [ ] No unlicensed manufacturer, military or government trademarks in marketing assets.

## Testing and rollout
- [ ] Internal test completed.
- [ ] Closed testing requirements applicable to the developer account are satisfied.
- [ ] Pre-launch report reviewed.
- [ ] Android 16 / API 36 device test completed.
- [ ] At least one lower supported API device test completed.
- [ ] Different screen aspect ratios checked.
- [ ] Crash/ANR status reviewed.
- [ ] Staged rollout used for first production release when practical.

## Archive
- [ ] Save final AAB, mapping/native symbols if generated, screenshots, listing text and release notes.
- [ ] Tag the exact production commit in GitHub.
- [ ] Update `PROJECT_REFERENCE.md` with production version and release date.
