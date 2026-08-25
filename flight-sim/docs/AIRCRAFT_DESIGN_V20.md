# Aircraft Design v20 — High Fidelity Mobile Jet

The flat fallback silhouette is rejected as a production aircraft.

## Geometry target
- Full volumetric fuselage, not a planar wing polygon.
- Long blended nose with separate radome region.
- Raised canopy with transparent PBR glazing.
- Twin side intakes with visible depth and intake lips.
- Blended wing roots and realistic wing thickness/taper.
- Twin canted vertical tails.
- Independent stabilators, rudders and flaperons.
- Twin engine nacelles and detailed circular/petal nozzles.
- Separate nose/main landing gear and doors.

## Visual target
- Real metre scale and believable fighter proportions.
- PBR base-color + normal + metallic/roughness + AO.
- Subtle panel lines and RAM-style surface variation; avoid toy-like high contrast.
- Canopy Fresnel/reflection treatment.
- Nozzle metallic roughness variation and heat tint.
- Directional sun, IBL/environment lighting, contact shadows.
- Afterburner core, plume and heat distortion as separate effects.

## Mobile budgets
- LOD0: 140k–220k triangles, close inspection.
- LOD1: 55k–90k triangles, normal chase camera.
- LOD2: 10k–25k triangles, distant aircraft.
- 2K/4K source textures with device quality tiers; compressed GPU formats for release.

## Animation node contract
Airframe, Canopy, Stabilator_L/R, Rudder_L/R, Flaperon_L/R, Nozzle_L/R,
Gear_Nose/L/R, Afterburner_L/R.

Every control-surface pivot must sit on its physical hinge axis. FlightDynamicsEngine drives aircraft attitude; AircraftControlSurfaces drives local surface deflection.

## Acceptance views
The model is not accepted from a single screenshot. Review nose 3/4, side, top, rear 3/4, underside, canopy close-up and nozzle close-up. Silhouette, volume and shading must all pass before replacing the fallback asset.

## Integration rule
`assets/aircraft/fighter_v19.glb` remains the production slot. Do not ship a renamed arbitrary model. Geometry, hierarchy, scale, materials, performance and redistribution licence must be verified first.
