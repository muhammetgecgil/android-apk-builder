# Aircraft Simulator 3D — Canonical Project Reference

This file is the canonical product reference for the Android aircraft simulator in this repository.

## Identity
- Product name: Aircraft Simulator 3D
- Repository: muhammetgecgil/android-apk-builder
- Active branch: aircraft-sim-v19-foundation
- Android module: flight-sim/app
- Base applicationId: com.mg.fixturecockpitsim
- Debug applicationId: com.mg.fixturecockpitsim.dev
- Platform: Android phone/tablet
- Rendering: Filament + procedural 3D fighter mesh + Android Canvas world layer
- Minimum SDK: 26
- Target SDK: 36
- Compile SDK: 36

## Product concept
Aircraft Simulator 3D is a mobile 3D flight-simulation experience focused on a modern generic fighter aircraft. The demo mission is continuous and cinematic: hangar start, taxi, RWY 27 hold, takeoff roll, rotation/climb, scenic flight, approach, flare, rollout, taxi-in and hangar park. The ground phase is designed to keep the aircraft nose aligned with RWY 27 until liftoff.

## Core product principles
1. Continuous simulation; avoid movie-like cuts.
2. Aircraft nose, runway centerline and ground-motion cues must remain coherent.
3. Hangar, apron, taxiway, runway, coastline, sea, mountains and sky should feel like one world.
4. Landing gear must deploy/retract without intersecting the fuselage or appearing detached.
5. Manual takeover in flight must remain possible while demo/autopilot logic is active.
6. Visual quality should improve without breaking package identity or update compatibility.
7. Production releases must use a proper release/upload key; the public development key is never a production signing key.

## Current release lineage
AVM versions are internal visual/mission milestones. The Play Store release should use a user-facing semantic version and monotonically increasing versionCode. Do not expose internal fixture naming in store marketing.

## Current Play requirement baseline
As of 2026-08-30, new Google Play apps and app updates must target Android 16 / API 36 from 2026-08-31. This project already targets API 36.

## Canonical decision rule
When implementation notes, chat instructions and older documents conflict, this file plus the latest code on `aircraft-sim-v19-foundation` are the product source of truth. Update this file whenever product identity, package ID, release policy, major mission flow, privacy behavior or Play Store declarations change.
