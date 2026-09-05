# Aircraft Simulator 3D — Official Reference Baseline

Status: AUTHORITATIVE ARTIFACT BASELINE
Established: 2026-09-05
Baseline family: v108 — Cinematic Turkiye 3D World

## Immutable reference artifacts
- Aircraft_Simulator_3D_v108_Cinematic_Turkiye_3D_World_Test.apk
  - SHA-256: 2968e3bbca2a33dcac036dc2e58e3c71ba1df058c1d7667e4ff03fa38d302e8d
  - Uploaded copy may appear as `Aircraft_Simulator_3D_v108_Cinematic_Turkiye_3D_World_Test-2.apk`; its bytes/hash match the recorded Test APK.
- Aircraft_Simulator_3D_v108_Release_Unsigned.apk
  - SHA-256: 0a92322593f2475e62affb045e501ded0e69ea2bd55abf9fc1e0e0dbf813417c
- Aircraft_Simulator_3D_v108_Play_Unsigned.aab
  - SHA-256: fb77c51a262cdcc4e5bcf426ed441e6c4481a933dafce63133018db60c84a592
- Aircraft-Simulator-3D-v108-Cinematic-Turkiye-3D-World.zip
  - SHA-256: 8e2418761927a7db047e2b96837af088457afb9559884715152c01cd8c4ea9dc
- Aircraft_Simulator_3D_v108_Cinematic_Turkiye_3D_World_Test.sha256.txt

## Baseline policy
1. The v108 artifact set above is the official starting/reference point for future Aircraft Simulator 3D work.
2. These baseline artifacts are immutable and must not be overwritten.
3. All further development must occur on a copy/working branch created from the reference branch, never directly on this reference branch.
4. Regression checks should compare new builds/behavior against this v108 baseline.
5. Older reference declarations (including v88) are superseded by this v108 baseline.

## Source correspondence note
The uploaded v108 ZIP is a distribution/artifact package (APK/AAB/hash), not a complete Gradle source archive. The current GitHub source snapshot on this branch reports an older internal source version. Therefore this document establishes the uploaded v108 binaries as the authoritative artifact baseline, but does not claim that the current source tree can reproduce those binaries byte-for-byte. Source-to-artifact correspondence must be validated/reconstructed before claiming reproducible v108 source.

Reference branch: `aircraft-sim-v108-cinematic-turkiye-3d-world-verified`
Working development branch: `aircraft-sim-v108-working-copy`
