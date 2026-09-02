# Aircraft Simulator 3D v88 — Icon Embed Record

Baseline:
- versionCode: 88
- versionName: 26.6-avm18.0-mechanical-dynamics
- Baseline APK SHA-256: `1eb3fe5529ba5bee800182652d3c0afb69c70dba78ac12d078f86b308e495447`
- Baseline signing certificate SHA-256: `7b7e5d01e3dc954d95c67aa00add5909e04fb4d4b9dbdf4925718c82e2a45d96`

Correct same-signature icon update build:
- APK: `Aircraft_Simulator_3D_v88_NewIcon_Update_SameSignature.apk`
- APK SHA-256: `6a266f188512bcaf3f5e3570a1f726cb4287804b31b8f7dd5e2f7c7e7cbc82c5`
- Launcher icon SHA-256: `936b13b699ba0037c5f949c703fc5ca1fc077397eb350f3bb893552c152f7596`
- Signing certificate SHA-256: `7b7e5d01e3dc954d95c67aa00add5909e04fb4d4b9dbdf4925718c82e2a45d96`

Verification:
- Signing certificate is identical to the original v88 certificate.
- AndroidManifest.xml is unchanged from the v88 baseline.
- All DEX bytecode is unchanged from the v88 baseline.
- Only the launcher icon resource mapping and icon asset were changed.
- `resources.arsc` remains uncompressed and 4-byte aligned.
- APK Signature Scheme v2 signature was generated and cryptographically verified.

Important:
- Do not use the earlier `Aircraft_Simulator_3D_v88_NewIcon_Installable.apk` as an in-place update; it was signed with a different temporary certificate.
- Keep the stable Aircraft Simulator signing key private. Do not commit private key material or credentials into the public baseline branch.
