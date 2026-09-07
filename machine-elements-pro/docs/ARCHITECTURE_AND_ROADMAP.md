# Machine Elements Pro — Architecture and Roadmap

## Product architecture

The product is split into six layers so that engineering mathematics cannot be corrupted by UI changes.

1. **Domain model** — quantities, units, materials, geometry, load cases, standards metadata.
2. **Deterministic solvers** — pure calculation functions with no Android dependencies.
3. **Verification engines** — combine failure modes, safety criteria, applicability rules and governing-result logic.
4. **Selection/optimization engines** — minimum required geometry, preferred sizes, catalogue filtering and multi-constraint ranking.
5. **Persistence/reporting** — frozen snapshots, revisions, calculation history and report serialization.
6. **Android UI** — guided forms, diagrams, warnings, results, comparison, project navigation.

## Solver contract
Every solver receives a validated immutable input object and returns a result object containing: raw outputs, units, intermediate values, assumptions, warnings, source-method metadata, equation-set version, applicability status and numerical diagnostics. A solver must not directly return a green/red UI color.

## Verification contract
A verification engine aggregates solver outputs into checks. Every check has: check ID, failure mode, demand, capacity, utilization, margin, safety factor when defined, severity, completeness and explanation. The overall component verdict is governed by the worst applicable completed check. Missing required checks yield INCOMPLETE rather than PASS.

## Data model
Project → Assembly → Component → Design Case → Load Case → Calculation Revision.

A Calculation Revision contains:
- input snapshot;
- material snapshot;
- geometry snapshot;
- selected method/standard metadata;
- deterministic result snapshot;
- warnings;
- app version;
- equation-set version;
- creation time;
- parent revision when recalculated.

## Unit architecture
Internal canonical units are SI: N, mm, MPa, N·mm, rad, s, kg. UI may display N·m, kN, Pa/GPa, inch/lbf/psi later. Conversion occurs only at the boundaries. Solver code never parses localized text.

## Standards architecture
Standards are represented through implementation profiles. A profile stores standard family, edition, method identifier, applicable component types, required inputs, optional inputs, validation set ID and status. Copyrighted standard text and proprietary tables are not embedded unless licensed; only independently implementable equations/data and user-supplied licensed data are used.

## Module roadmap

### Release 1 — Core engineering calculator
Eight existing modules plus pin/lug, Euler column, beam deflection, torsion angle, power screw and thread stripping. Goal: robust preliminary sizing.

### Release 2 — Bolted Joint Pro
Bolt tensile stress area, preload, torque-preload, bolt/member stiffness, load factor, separation, residual clamp, fatigue, thread stripping, bearing, slip and eccentric/bolt-group load distribution.

### Release 3 — Shaft System Designer
Multi-station shaft model, supports, gears/pulleys/couplings as loads, automatic reactions, shear/moment/torque diagrams, static stress, fatigue, deflection, twist and critical-speed screening.

### Release 4 — Bearing Selector
Equivalent dynamic/static load, variable duty cycle, reliability/lubrication modifiers, paired arrangement, candidate bearing database and envelope/life ranking.

### Release 5 — Gearbox Designer
Gear geometry, forces, spur/helical pairs, preliminary Lewis mode, detailed ISO-oriented root/contact workflows, shaft force export, bearing-load propagation, ratio train and planetary kinematics.

### Release 6 — Joining and interfaces
Weld groups, pins/lugs, keys, splines, press fits, shrink fits, retaining features and coupling bolts.

### Release 7 — Flexible transmission
Belts, synchronous belts, chains and coupling selection.

### Release 8 — Fatigue and duty cycles
Marin factors, notch sensitivity, S-N finite life, variable amplitude, Miner damage and multiaxial-warning framework.

### Release 9 — Automated sizing
Constraint solver and discrete preferred-size search. The engine searches geometry but finalizes only after full deterministic re-verification.

### Release 10 — Engineering report and project control
Revisioned projects, comparison, calculation report, assumptions register, unresolved-input register, PDF/structured export and calculation audit trail.

## UI concept
Home screen uses component families rather than an enormous spinner. Families: Connections, Shafts & Rotation, Bearings, Gears & Transmission, Springs, Structural, Fatigue, Materials, Projects. Search finds a calculation by both Turkish and English engineering terms.

Each module follows five pages/tabs: Inputs → Model → Checks → Optimize → Report. Basic mode hides advanced parameters without deleting them. Expert mode exposes every coefficient and provenance.

## Engineering assistant concept
Natural-language input is never the numerical authority. The assistant may parse “35 kN axial plus 2 kN shear on an M12” into a draft input set, but the user sees and approves the interpreted fields before the deterministic solver runs. Explanations are generated from deterministic result objects, not from free-form arithmetic.

## Performance targets
Cold start < 2.5 s on modern flagship Android hardware. Basic calculation < 50 ms. Multi-station shaft solve < 200 ms for 100 stations. Candidate catalogue search < 500 ms for 20,000 local records. Optimization runs shall be cancellable. No internet is required for calculation.

## Quality gates
1. Compilation.
2. Unit tests.
3. Numerical regression tests.
4. Invalid-input tests.
5. Static analysis.
6. Instrumented smoke test.
7. Release APK verification.
8. Versioned benchmark report.

## Long-term differentiators over spreadsheets
- no hidden cell dependencies;
- traceable equations and versions;
- linked loads across shaft/gear/bearing systems;
- automatic governing-failure detection;
- design-space search instead of manual trial-and-error;
- project revision history;
- deterministic validation plus human-readable explanation;
- offline mobile use;
- reusable material and catalogue libraries;
- calculation audit reports.
