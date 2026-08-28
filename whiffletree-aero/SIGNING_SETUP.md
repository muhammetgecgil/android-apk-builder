# Whiffletree Aero stable signing setup

Never commit a release JKS or its passwords to this public repository.

The CI workflow supports a persistent release key through four GitHub Actions secrets:

- `WHIFFLETREE_KEYSTORE_B64`
- `WHIFFLETREE_STORE_PASSWORD`
- `WHIFFLETREE_KEY_PASSWORD`
- `WHIFFLETREE_KEY_ALIAS`

The workflow decodes the JKS only inside the runner, exports `WHIFFLETREE_KEYSTORE_PATH`, builds `sideloadRelease`, and publishes `update/latest.apk.b64` plus SHA-256 metadata.

## Compatibility rule

Android permits an APK to update an installed app only when both are true:

1. `applicationId` is unchanged (`com.mg.whiffletreeaero`), and
2. the update is signed by the same accepted signing identity.

`versionCode` must also increase for an upgrade.

Historical Whiffletree debug/test APKs used more than one signing certificate. Those builds require one clean migration to the stable-signed baseline. Do not delete or rotate the stable release key after that baseline is deployed.
