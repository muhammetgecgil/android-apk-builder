# Aircraft Visual Maturity Program

Goal: replace the current transitional procedural fighter with a visually mature, mechanically believable production aircraft while preserving mobile performance and the dual-phone simulator architecture.

## AVM-0 — Baseline lock
Status: current baseline.
Acceptance:
- One stealth aircraft only across Demo and Aircraft Display.
- Pilot phone and Aircraft Display phone remain separate roles.
- Runway/scenery render behind aircraft; aircraft must never appear transparent because of overlay order.
- Current autonomous mission and CI remain green.

## AVM-1 — Primary form
Scope:
- Rebuild nose/radome proportions.
- Smooth nose-to-chine transition.
- Rework upper fuselage volume.
- Blend wing roots into fuselage.
- Rework rear fuselage around twin engines.
Acceptance:
- No card-like or intersecting silhouette from front 3/4, side, top, rear 3/4.
- Fuselage reads as continuous volume.
- Camera views do not expose open or broken geometry.

## AVM-2 — Canopy and cockpit crown
Scope:
- New canopy base/sill.
- Separate windscreen, side canopy surfaces and rear bow.
- Strong canopy frame geometry.
- Dark smoked glass material with controlled reflection.
- Smooth canopy-to-fuselage crown transition.
Acceptance:
- Canopy cannot look like a floating dome.
- Body remains opaque through canopy except intended glazing.
- Close chase/3/4 views show a coherent cockpit crown.

## AVM-3 — Intake and propulsion
Scope:
- Defined intake lips and sidewalls.
- Dark intake tunnel depth.
- Engine nacelle blending.
- Nozzle outer petals, inner nozzle and afterburner core.
- Throttle-linked nozzle/afterburner state.
Acceptance:
- Intakes have visible depth.
- Twin-engine architecture is clear from rear/underside views.
- Nozzle geometry does not intersect fuselage.

## AVM-4 — Wings and control surfaces
Scope:
- Proper wing thickness and leading/trailing edges.
- Separate flaperons/elevons.
- Stabilators and rudders with correct hinge locations.
- Control-surface limits and smoothing.
Acceptance:
- Each surface moves around a plausible physical pivot.
- No gaps/exploding geometry at maximum deflection.

## AVM-5 — Landing gear maturity
Scope:
- Nose/main struts, oleo sections, braces, hubs, tires and doors.
- Wheel rotation.
- Gear bay visual depth.
- FlightState gearPosition and strut compression drive geometry directly.
Acceptance:
- Extended, transition and retracted states are visually coherent.
- Touchdown compression is visible.
- Gear does not clip through runway or fuselage.

## AVM-6 — Materials and surface identity
Scope:
- Multi-tone aircraft skin instead of flat grey.
- Controlled metallic/roughness response.
- Canopy, skin, intake, tire, strut, nozzle and afterburner material separation.
- Subtle panel/maintenance-zone variation.
Acceptance:
- Major material families are distinguishable under the same lighting.
- Aircraft remains readable in dark/night scenes.

## AVM-7 — Detail pass
Scope:
- Panel-line cues, access panels, antennas/sensors, navigation lights, landing lights.
- Better nozzle petal detail and canopy framing.
- Remove temporary/intersecting decorative bars that do not represent aircraft structure.
Acceptance:
- Close 3/4 view no longer looks procedural or unfinished.

## AVM-8 — Production mesh / GLB conversion
Scope:
- Freeze accepted external form.
- Convert to one production aircraft hierarchy/GLB.
- Named animation nodes: Airframe, Canopy, Stabilator_L/R, Rudder_L/R, Flaperon_L/R, Nozzle_L/R, Gear_Nose/L/R, Afterburner_L/R.
- LOD0/LOD1/LOD2 and mobile texture tiers.
Acceptance:
- No arbitrary/fallback aircraft asset remains in production path.
- Model scale, pivots, materials and hierarchy verified.

## AVM-9 — Simulator integration validation
Scope:
- Demo takeoff/tour/landing.
- Dual-phone pilot/display session.
- Chase/rear/quarter camera validation.
- Day/night runway validation.
- Performance and memory checks.
Acceptance:
- Autonomous M1 mission remains passing.
- Dual-phone role workflow works consistently.
- No transparency, clipping, broken animation or fallback model in any normal route.

## Release gate — Aircraft Visual Maturity M1
AVM M1 is complete only when AVM-1 through AVM-6 pass their acceptance criteria and CI produces an installable APK. AVM-7 through AVM-9 continue toward production visual maturity.

## Execution rule
Work strictly in order. Each stage must keep the simulator buildable and the autonomous mission test passing. If a new visual change breaks physics, dual-phone roles, or CI, repair it before moving to the next stage.
