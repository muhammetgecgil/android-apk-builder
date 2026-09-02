# Aircraft Simulator 3D — v89 Fighter Control Surfaces

Baseline: `aircraft-sim-v88-final-reference` (canonical user APK SHA-256 `1eb3fe5529ba5bee800182652d3c0afb69c70dba78ac12d078f86b308e495447`).

## Fighter configuration decision

This airframe is treated as a generic modern twin-engine, twin-tail fighter with:

- all-moving left/right stabilators with differential roll mixing;
- trailing-edge left/right flaperons (roll + automatic low-speed camber + small pitch sharing);
- twin rudders with yaw damping and coordinated-turn assistance;
- independent automatic left/right leading-edge flaps scheduled by AoA, speed and gear state;
- one dorsal speed-brake system, separately commanded from wheel brakes;
- fighter-style autotrim carried by the all-moving stabilators;
- speed-scheduled control authority plus soft AoA and positive-g limiting.

## Deliberately not added

- No separate elevator: this aircraft uses all-moving stabilators.
- No separate conventional flap: flaperons and automatic leading-edge flaps provide the required high-lift/control functions.
- No second `air brake`: `speed brake` and `air brake` are one dorsal system on this configuration.
- No canards: the existing v88 geometry is a conventional twin-tail/stabilator fighter and has no canard planform.
- No spoilerons: differential stabilators + flaperons already provide the intended roll-control architecture and spoilerons would add unsupported geometry.

## Version

- versionCode: 89
- versionName: `26.7-avm19.0-fighter-control-surfaces`
- package/debug applicationId: `com.mg.fixturecockpitsim.dev`
- targetSdk: 36

## Verified build

- GitHub Actions run: `33661739848`
- Successful source head: `4e8142d263616d3d9528039801ec281392b70157`
- Artifact ID: `9859026359`
- Debug APK: `Aircraft_Simulator_3D_v89_Fighter_Control_Surfaces_Test.apk`
- APK SHA-256: `f4f259016e06e2b847fe9cd84d443b9e7a2f538a0151fcf96c62e824be75443a`
- APK Signature Scheme v2: verified
- Developer certificate SHA-256: `7b7e5d01e3dc954d95c67aa00add5909e04fb4d4b9dbdf4925718c82e2a45d96`
- Approved `ic_aircraft_simulator_final` launcher icon verified inside APK.
- Dedicated `FighterFlightControlSystemTest` suite is blocking and passed.
- Existing AVM migration tests remain non-blocking; they are tracked separately from fighter FCS acceptance.

## Development rule

v88 remains immutable as the final rollback/reference baseline. v89 work lives on `aircraft-sim-v89-fighter-control-surfaces` and may only advance from that branch after a successful CI APK build.
