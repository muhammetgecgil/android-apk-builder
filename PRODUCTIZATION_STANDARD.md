# Productization Standard

This repository uses the same stepwise productization method for engineering applications.

## Stage 1 — User Goal First
Define one primary job-to-be-done. The first screen must ask only for inputs the user genuinely knows. Derived engineering quantities must not be mandatory inputs.

## Stage 2 — Minimum Guided Input
Use a wizard or guided flow. Separate required inputs from advanced constraints. Allow signed values where physically meaningful. Validate units, ranges and missing data before solving.

## Stage 3 — AUTO Engineering Solution
The application should calculate design variables instead of asking the user to guess them. When several valid solutions exist, automatically recommend the best one and hide alternatives under an advanced view.

## Stage 4 — Visual Result
Show the physical system, load path, forces, moments, displacements and key margins on a clear 2D/3D engineering view. Each component must be identifiable and selectable.

## Stage 5 — Engineering Proof
Use a single source-of-truth solved data set. Show force/moment closure, required-vs-applied comparison, capacity utilization, margins, warnings and assumptions. No screen may silently recompute a different result.

## Stage 6 — Physical Component Selection
Translate calculated demand into actuator, load-cell, bearing, pin, clevis, beam/profile, sensor and fixture requirements. Prefer verified real manufacturer data. Mark unverified geometry as generic/envelope data.

## Stage 7 — CAD / BOM / Assembly
Build an assembly chain from the solved design. Provide BOM, interfaces, envelope dimensions and CAD-export preparation. Never label generic geometry as original vendor CAD.

## Stage 8 — Test & Simulation
Run preload/limit/ultimate/unload or application-specific test sequences on the same active design. Show target/actual values, overloads, expected displacement/strain and failure/warning conditions.

## Stage 9 — Release Quality
Keep package/application identity stable, increment versionCode, preserve signing continuity, run compile/build checks, provide an installable artifact, and block release if critical validation fails.

## UI Rule
The first screen should never expose expert-only controls that can be calculated automatically. Advanced controls stay behind an Advanced button.

## Whiffletree Specialization
Required EFT/load data -> pad loads -> optimum actuator count -> optimum tree topology -> lever/pivot geometry -> beam sizing -> pin/clevis sizing -> load-cell selection -> actuator selection -> BOM -> 2D/3D assembly -> test simulation.
