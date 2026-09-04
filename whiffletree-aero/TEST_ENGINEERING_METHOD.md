# Whiffletree Aero v8.2 — Test Engineering Method

This release treats the app as a structural-test-system engineering aid, not only a load calculator.

## Workflow
1. Define the 100% DLL/reference six-component load requirement.
2. Select physically accessible load-introduction pad/strap points.
3. Define each point coordinate and allowed load direction.
4. Reverse-solve pad loads from force/moment equilibrium.
5. In EFT Paper Mode, keep X translation and roll couple as separate load trains; synthesize Y+Yaw and Z+Pitch through whiffletrees.
6. Group adjacent pads into practical root channels and calculate each root resultant and equivalent load-line position.
7. Calculate each passive beam pivot from moment equilibrium, FL*a = FR*b. The pivot is not assumed to be centered.
8. Generate an actuator-excluded rig BOM and instrumentation checklist.
9. Require independent section shear/moment transfer-function or FEA verification before test release.
10. Require pad local-load, fixture, beam/pin/clevis/slider, load-cell, LCS interlock, DT/SG, pressure/leak, dry-run, TRR and post-test inspection gates.
11. Generate a paper-inspired load profile: 30% preliminary, unload, 10% increments to 100%, then 5% increments to 115% limit or 150% ultimate.
12. During execution, compare command (CMD) and load-cell feedback (FDK) for every root channel and hold the test if an editable error limit is exceeded.

## Important engineering boundary
Global force/moment closure does not prove that the correct internal shear and bending-moment distribution has been reproduced in the test article. The app therefore does not mark a campaign TEST READY until the transfer-function/FEA verification gate is manually confirmed. Mechanical component sizing is likewise a separate release gate.

## Source basis
The EFT Paper Mode follows the workflow and architecture described in *Verification of the Structural Soundness of Aircraft External Fuel Tanks Through Structural Static Testing* (International Journal of Aeronautical and Space Sciences, 2026): load-point selection, force/moment conversion, transfer-function verification, whiffletree planning, reduced test-channel count, preliminary/limit/ultimate profiles, load-cell feedback monitoring, strain/displacement monitoring and post-test inspection.
