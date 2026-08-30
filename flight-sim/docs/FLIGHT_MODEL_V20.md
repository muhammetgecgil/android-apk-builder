# Aircraft Simulator 3D — Flight Model v20 / AVM-12.0

## Purpose
This increment keeps the AVM-11.0 Filament aircraft, dual-phone data link, manual aerobatic roll mode, autonomous demo route, landing gear animation and world renderer intact while improving the simulation core.

## Added simulation state
- Indicated airspeed (IAS)
- Air density exponential atmosphere approximation
- Mach number and dynamic pressure
- Angle-of-attack derived envelope state
- Bounded load factor
- Stall margin and stall warning
- Overspeed warning
- Landing-gear configuration warning
- Deterministic wind components and low-amplitude turbulence

## Flight-model changes
- Gear drag while airborne
- AoA-dependent drag
- Load-factor / induced-drag term
- Progressive stall lift loss and sink instead of an abrupt scripted stall
- Wind affects ground track while true airspeed remains aircraft-relative
- Existing near-full-stick aerobatic roll and inverted-flight capability retained
- Existing landing-strut compression and braking behaviour retained

## Autonomous mission changes
A new `GO_AROUND` phase monitors the low approach. If the aircraft is significantly misaligned, descending too rapidly, too slow, or in a stall-warning condition below the stabilization gate, the demo commands one go-around, climbs, re-aligns with RWY27 and retries the approach.

## Regression coverage
- Full hangar -> taxi -> takeoff -> scenic flight -> approach -> landing -> taxi -> hangar loop
- RWY27 ground alignment
- Atmosphere / IAS / Mach / dynamic-pressure telemetry
- Stall warning and progressive sink
- Gear and overspeed warnings
- Full-stick inverted-roll authority
- Unstable-approach go-around trigger

## Release
- versionCode: 48
- versionName: `23.0-avm12.0-flight-envelope`
- development branch: `aircraft-sim-v20-flight-envelope`
