# Structural AI Static

Android-first autonomous structural static-analysis workspace.

## Product intent
The user imports a model and the software should automate as much of the structural analyst workflow as technically defensible: geometry cleanup/classification, material candidate inference, support/load/contact proposals, mesh strategy, solver execution, convergence checks, result interpretation and report generation.

## v0.1 implemented
- Standalone Android application workspace.
- OBJ and ASCII STL geometry import.
- Geometry bounding-box and slenderness classification.
- Autonomous first-pass material/support/load assumptions.
- Fast physics screening with stress/deflection/FOS indicators.
- Explicit automation confidence score and engineering disclaimer.
- GitHub Actions APK build workflow.

## Roadmap to real FEA
1. STEP/IGES/BREP import through a native CAD kernel.
2. Automatic defeaturing and midsurface/beam/solid classification.
3. Tetrahedral volume meshing, local feature sizing and mesh-quality metrics.
4. Sparse 3D linear elastic FEM solver; then nonlinear material/contact.
5. Automatic boundary-condition inference from assembly interfaces, fastener/contact geometry and user-provided load context.
6. Von Mises/principal stress, displacement, strain energy, reactions and contact pressure fields.
7. Automatic mesh convergence and singularity detection.
8. Buckling, modal, thermal-stress, bolt/joint and fatigue modules.
9. AI verification layer: equilibrium checks, unit sanity checks, result confidence and evidence trail.
10. One-tap PDF engineering report plus solver deck export.

## Important engineering rule
AI may automate setup and interpretation, but every generated assumption must remain inspectable and traceable. A low-confidence automatic support/load/material inference must be surfaced rather than silently treated as fact.
