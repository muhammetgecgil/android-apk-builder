# Aircraft asset slot — v19.1

Place the production aircraft at `fighter_v19.glb`.

Required node names are defined by `AircraftSceneContract` so the renderer can animate control surfaces without depending on a specific aircraft mesh. Use real-world metre scale, +Y up, aircraft nose toward -Z. Keep pivots at each hinge axis.

Materials should be PBR metallic/roughness. Recommended texture set: base color, normal, metallic-roughness and AO. The canopy must remain a separate material/node.

The old procedural `Jet3DView` remains only as a fallback until a validated GLB is committed. Do not rename an arbitrary model to fighter_v19.glb: geometry, licence and node contract must be checked first.
