# M80 — Engineering Beta Milestone

Status: IN PROGRESS

## Definition of Done
Machine Elements Pro must solve a representative mechanical transmission problem from requirements through sizing, verification, standardization, product candidates, design review, project persistence, and engineering report without requiring an external spreadsheet for the supported calculation chain.

## M80 Work Packages

### M80.1 Verification Matrix
- Minimum 25 independent reference problems.
- Every critical solver has expected numerical results and tolerance.
- Distinguish simplified engineering methods from standards-based methods in the UI/report.
- Unit and invalid-input tests.

### M80.2 End-to-End Transmission Design
Input target case: 7.5 kW, 1450 rpm, approximately 100 rpm output.
Chain: requirements -> ratio -> gear forces -> gear sizing -> shafts -> keys -> bearings -> fits -> coupling -> bolted joints -> safety factors/life.
No manual re-entry where an upstream result is already known.

### M80.3 Automatic Standard Sizing
- Preferred shaft diameters.
- Metric bolt size/property class.
- Bearing capacity/bore selection.
- Gear dimensions.
- Fits and tolerances.
- Coupling/drive requirement handoff.

### M80.4 Real Product Matching
- Turkey + Europe.
- Bearing, bolt, coupling, gearbox, belt, chain first.
- Technical requirement must never be relaxed merely to return a product.
- Unknown stock/price/lead time must remain unknown until externally verified.

### M80.5 Project Manager
- New / save / open / duplicate / revision.
- Multiple elements per project.
- Persistent inputs, results, selected standards and product candidates.

### M80.6 Engineering Report
- Inputs and units.
- Method/standard revision and assumptions.
- Equations/results.
- FoS/life and governing failure mode.
- Selected standard sizes and product candidates.
- Warnings and unresolved checks.

### M80.7 Design Review
- PASS / WARNING / FAIL summary.
- Rank governing FoS/life issues.
- Detect missing required checks and incomplete product verification.

### M80.8 Acceptance Scenario
The 7.5 kW / 1450 rpm / ~100 rpm transmission scenario must complete the supported chain and generate a saved project plus engineering report. M80 is not complete until this scenario passes the verification matrix.

## Standard-control rule
The application records the method/standard revision used. As of 2026-08, ISO 281:2007 remains the published bearing rating-life standard while ISO/DIS 281 Edition 3 is under development; draft methods must not silently replace the published method.
