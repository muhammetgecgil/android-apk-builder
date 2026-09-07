# BioNav Aviation v1.0.0

GNSS-denied navigation research/demo Android application. The app intentionally requests no GPS/GNSS/location permission.

## Active in v1

- Android rotation-vector attitude / magnetic heading estimate
- Linear-acceleration inertial propagation in an approximate East/North/Up world frame
- Relative North/East displacement from a user-defined origin
- Ground-speed estimate from dead reckoning
- Bearing and distance back to the origin ("home vector")
- Barometric pressure altitude using ISA standard sea-level pressure
- Per-source confidence display and simple navigation-integrity state
- Pause/resume and zero/origin controls
- GitHub Actions build, APK signature check, package-name check

## Architecture target

The project is structured as the phone demonstrator for a larger multi-sensor concept:

INS/IMU + magnetometer + barometer + visual-inertial odometry + terrain-relative navigation + magnetic-map matching + celestial/sun observations + optional aviation radio navigation references -> integrity-aware fusion engine.

Visual odometry, terrain matching, magnetic anomaly maps, celestial fixes, DME/VOR/TACAN integration, certified integrity monitoring and flight-control coupling are NOT implemented in v1.

## Important safety limitation

**Research / demonstration only. Not for flight guidance, primary navigation, IFR/VFR operational decision making, autopilot coupling, or any safety-critical aviation use.** Phone-grade MEMS sensors drift rapidly and this v1 estimator is not a certified INS. The displayed "DEMO GOOD" integrity label only indicates that multiple phone sensors are producing data; it is not aviation navigation integrity.

A flight-worthy implementation would require appropriate aviation-grade sensors, deterministic hardware/software architecture, calibrated error models, fault detection/isolation, independent verification/validation, environmental qualification and applicable airworthiness/certification processes.
