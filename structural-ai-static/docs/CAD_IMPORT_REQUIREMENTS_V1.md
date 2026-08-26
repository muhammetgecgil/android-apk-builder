# Structural AI Static — CAD Import Baseline V1

Locked requirements for the autonomous structural-analysis product.

## P0 formats
- STEP: .stp / .step, AP203 / AP214 / AP242
- IGES: .igs / .iges
- OCCT BREP: .brep
- STL: ASCII + binary
- OBJ

## P1 formats
- glTF / GLB
- PLY
- VRML/WRL

## Assembly requirements
- Preserve assembly hierarchy where source format provides it.
- Preserve component instance transforms.
- Preserve part/product names where available.
- Preserve colors and layers where available.
- Preserve material/validation metadata where available; never infer that metadata was present if absent.
- Imported component identity must survive tessellation and FEM preprocessing so contacts and fasteners can be traced back to CAD parts.
- STEP assembly import must use an XDE-style document model rather than flattening everything into one anonymous triangle soup.

## Geometry requirements
- Keep exact BREP/NURBS geometry for STEP/IGES/BREP until meshing.
- Perform shape validity checks and shape healing before meshing.
- Extract units from CAD metadata when available and convert internally to SI; do not use magnitude-based unit guessing when authoritative metadata exists.
- Generate analysis tessellation/volume mesh from the healed CAD representation, not by reparsing a display screenshot or render mesh.

## Android implementation target
Use Open CASCADE Technology (OCCT) native C++ through JNI. The native layer shall expose:
1. import(path, format)
2. assembly tree enumeration
3. part metadata enumeration
4. shape healing/validity status
5. BREP face/edge/solid topology
6. controlled tessellation for display
7. export of analysis-ready surfaces/solids to the FEM pipeline

## Safety rule
If the native CAD kernel is unavailable for STEP/IGES/BREP in a given build, the UI must report the format as unavailable; it must not silently rename, flatten, or pretend the file was imported.
