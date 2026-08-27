# Unit Master X — Production Release Gate

A build may be called **Production Candidate** only when every gate below is green.

## 1. Mathematical integrity
- Catalog contains at least 50 categories and 350 units.
- Every category and unit has non-empty metadata.
- Identity conversion is stable for every unit.
- Every unit pair round-trips across multiple magnitudes within numerical tolerance.
- Known engineering references pass: m↔mm, N↔kgf, psi↔bar, °C↔°F, rpm↔rad/s.
- Debug and release unit-test suites contain zero failures and zero errors.

## 2. Android runtime stability
The same candidate APK is installed and tested on API 29, 33, 35 and 36.
Each device test must:
- install successfully;
- cold-start successfully;
- keep the app process alive;
- render the Unit Master X UI;
- survive background/resume;
- survive orientation changes;
- survive repeated cold launches;
- survive a constrained Monkey interaction pass without an application crash.

## 3. Release build quality
- `targetSdk 36`.
- Release minification enabled.
- Release APK and AAB both build successfully.
- APK/AAB ZIP integrity and payload checks pass.
- Release lint passes.
- Debug package is isolated from production package.
- App requests no Android permissions.
- Native Android UI only; WebView shell is prohibited.

## 4. Distribution gate
A verified distribution artifact is uploaded **only after** build-quality and the complete device matrix pass.
The distribution bundle includes:
- installable RC APK;
- release APK;
- release AAB;
- SHA-256 checksums;
- machine-readable product quality report.

## Release classification
- Any failed gate: **Development / Beta**
- All automated gates green: **Production Candidate**
- Production Candidate + successful physical-device acceptance run: **Production**
