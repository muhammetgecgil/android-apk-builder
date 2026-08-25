# Aircraft Visual v19

The previous exterior model looks toy-like because geometry, scale, materials, lighting and camera are all simplified at the same time. v19 treats the aircraft as a dedicated visual system rather than a single hard-coded mesh.

## Geometry
- Use real-world scale: 18.90 m length, 13.56 m wingspan, 5.08 m height.
- Replace the procedural low-poly jet with a high-detail external asset path.
- Preserve separate nodes for canopy, left/right stabilators, rudders, nozzles, landing gear and control surfaces.
- Keep stealth edges sharp while using smooth normals only on curved fuselage/canopy regions.
- Add three LOD levels for mobile performance.

## Materials
- PBR airframe material with separate base-color, normal, roughness and metallic/AO channels.
- Dedicated canopy shader with tint, Fresnel reflection and controlled transparency.
- Heat-resistant nozzle material plus animated emissive afterburner core.
- Decals/panel markings should be separate overlays so they remain crisp without inflating geometry.

## Lighting and camera
- Image-based/environment lighting plus one sun/key light.
- Exterior FOV around 36 degrees to remove wide-angle toy distortion.
- Camera presets: chase, close chase, orbit, cinematic side, fly-by and cockpit.
- Use a physically scaled near/far clip range and shadow cascade appropriate for a ~19 m aircraft.

## Animation
- Stabilators, rudders, flaperons and nozzles driven from FlightState/control commands.
- Landing gear and canopy use explicit animation state machines.
- Afterburner length/intensity follows engine command; no static cones.

## Asset pipeline
- Development may keep the current reference asset only as a temporary source.
- Final Play Store model must have a license compatible with the intended distribution model.
- Prefer GLB/glTF 2.0 with named nodes and PBR textures over the old flattened custom mesh.
- CI validates required node names, bounding-box proportions, texture sizes and triangle budgets.

## Mobile budgets
- LOD0: 120k–220k triangles, 2K texture set, close exterior shots.
- LOD1: 45k–90k triangles, 1K–2K textures, normal gameplay.
- LOD2: 8k–25k triangles, 512–1K textures, distant aircraft.
- Target 60 FPS on flagship Android; quality scaler may reduce shadows/reflections before geometry quality.

## Acceptance criteria
1. Aircraft silhouette matches official top/front/side proportions.
2. No faceted shading on canopy/fuselage curves and no melted stealth edges.
3. Exterior model does not look miniature at chase-camera distance.
4. Control surfaces visibly respond to flight controls.
5. Materials remain convincing under daylight, sunset and overcast lighting.
6. LOD transitions are not visibly distracting during normal play.
