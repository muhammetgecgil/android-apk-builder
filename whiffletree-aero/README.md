# Whiffletree Aero v7.21.0

Android aircraft structural-test preliminary load-distribution and whiffletree design application.

## Update identity (do not change)

- Android applicationId: `com.mg.whiffletreeaero`
- v7.21.0 versionCode: `86`
- Future versions must keep the same applicationId and use a strictly higher versionCode.
- Sideload in-place updates must be signed with the same stable Whiffletree release key.

## Stable update channel

The sideload flavor reads update metadata from `whiffletree-aero/update/latest.json`.

The updater verifies remote versionCode, optional APK SHA-256, package identity, and APK signer identity before opening Android's installer.

Historical test/debug APKs were produced with different signing certificates, so they cannot all update each other. Install the first stable-signed baseline once; every later stable-signed build can then update in place.

See `SIGNING_SETUP.md`.
