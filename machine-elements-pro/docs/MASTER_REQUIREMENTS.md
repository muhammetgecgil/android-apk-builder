# Machine Elements Pro — Master Requirements Specification

Status: Living engineering specification
Target product: Android engineering design and verification suite
Primary unit systems: SI, later US customary
Safety position: engineering aid, not a substitute for responsible design approval

## 1. Product mission
Machine Elements Pro shall turn machine-element calculations from isolated spreadsheet cells into a traceable engineering workflow. The user shall be able to start from loads, geometry, material and service conditions; calculate relevant failure modes; compare alternatives; select a standard size; understand the governing failure mode; save the design case; and generate a calculation report with assumptions and warnings.

The application shall deliberately separate: (a) preliminary sizing, (b) detailed standard-based verification, (c) catalogue selection, and (d) final engineering approval. Every result shall state which level was used.

## 2. Core design principles
REQ-GEN-001 The application shall work offline for all deterministic calculation modules.
REQ-GEN-002 Every numerical output shall expose units.
REQ-GEN-003 Inputs shall reject NaN, infinity, impossible signs and invalid geometry.
REQ-GEN-004 Each module shall identify its governing failure mode.
REQ-GEN-005 Each module shall calculate a margin or factor of safety where physically meaningful.
REQ-GEN-006 Results shall be classified PASS, MARGINAL, FAIL or INFORMATIONAL.
REQ-GEN-007 PASS thresholds shall be configurable by project policy.
REQ-GEN-008 The user shall be able to choose a design safety factor independent of material strength.
REQ-GEN-009 Each calculation shall preserve a timestamped input snapshot.
REQ-GEN-010 Each calculation shall preserve the equation-set version.
REQ-GEN-011 Each calculation shall preserve the selected standard family and edition metadata.
REQ-GEN-012 The app shall distinguish nominal, mean, alternating, peak and proof loads.
REQ-GEN-013 The app shall support load combinations.
REQ-GEN-014 The app shall support static, cyclic, shock and spectrum-based service descriptions.
REQ-GEN-015 The app shall display assumptions before accepting a final design verdict.
REQ-GEN-016 The app shall never silently substitute a missing engineering input with an unsafe default.
REQ-GEN-017 Suggested defaults shall be visibly marked as suggested rather than measured or specified.
REQ-GEN-018 All formula constants shall have named provenance metadata.
REQ-GEN-019 Calculation code shall be separated from Android UI code.
REQ-GEN-020 Each calculation engine shall be unit-testable without Android UI.
REQ-GEN-021 Numerical regression tests shall use independently calculated reference cases.
REQ-GEN-022 The app shall support significant-digit control.
REQ-GEN-023 The app shall support scientific notation.
REQ-GEN-024 The app shall support decimal comma and decimal point entry.
REQ-GEN-025 The app shall support dark and light appearance.
REQ-GEN-026 The app shall support Turkish and English engineering terminology.
REQ-GEN-027 The app shall support project-level notes and assumptions.
REQ-GEN-028 The app shall support copy/export of input and result tables.
REQ-GEN-029 The app shall retain calculation history locally.
REQ-GEN-030 A destructive clear operation shall require an explicit user action.

## 3. Engineering workflow
REQ-WF-001 A design case shall contain project, assembly, component and revision identifiers.
REQ-WF-002 A design case shall contain load cases and combinations.
REQ-WF-003 A design case shall contain material definitions.
REQ-WF-004 A design case shall contain geometry definitions.
REQ-WF-005 A design case shall contain environment and duty-cycle conditions.
REQ-WF-006 A design case shall contain manufacturing assumptions.
REQ-WF-007 A design case shall contain analysis results.
REQ-WF-008 A design case shall contain warnings and unresolved assumptions.
REQ-WF-009 The app shall identify the minimum missing information preventing detailed verification.
REQ-WF-010 The app shall allow preliminary sizing even when detailed verification inputs are incomplete.
REQ-WF-011 Preliminary results shall be visually distinct from standard-based verification results.
REQ-WF-012 The app shall provide a “why failed?” explanation.
REQ-WF-013 The app shall provide a “what parameter helps most?” sensitivity view.
REQ-WF-014 The app shall provide a “minimum required size” solver where equations permit monotonic solving.
REQ-WF-015 The app shall provide a “nearest preferred size” function.
REQ-WF-016 The app shall show whether the selected preferred size still passes all checks.
REQ-WF-017 The app shall support scenario comparison.
REQ-WF-018 The app shall rank scenarios by mass, margin, size or user-defined objective.
REQ-WF-019 The app shall flag non-physical combinations even if equations return a number.
REQ-WF-020 The app shall show the chain from input → derived quantity → check → verdict.

## 4. Materials system
REQ-MAT-001 Material records shall support E, G, Poisson ratio, density, yield strength, ultimate strength and elongation.
REQ-MAT-002 Material records shall support shear yield or a documented derivation rule.
REQ-MAT-003 Material records shall support fatigue endurance data where available.
REQ-MAT-004 Material records shall support temperature-dependent allowables.
REQ-MAT-005 Material records shall support source and condition, e.g. annealed, normalized, quenched-tempered.
REQ-MAT-006 Material records shall support surface condition and heat treatment.
REQ-MAT-007 Material records shall support user-defined materials.
REQ-MAT-008 User-defined material properties shall be visibly distinguished from validated library data.
REQ-MAT-009 The app shall prevent mixing incompatible strength units.
REQ-MAT-010 Material allowables used by a result shall be frozen into the saved calculation snapshot.

## 5. Bolted joints
REQ-BLT-001 Calculate direct tensile stress.
REQ-BLT-002 Calculate direct shear stress.
REQ-BLT-003 Calculate combined von Mises stress.
REQ-BLT-004 Support tensile stress area rather than only nominal shank area.
REQ-BLT-005 Support property classes and user-defined proof/yield/ultimate values.
REQ-BLT-006 Calculate target preload.
REQ-BLT-007 Calculate tightening torque using a configurable nut factor for preliminary sizing.
REQ-BLT-008 Support thread and under-head friction in detailed torque-preload calculation.
REQ-BLT-009 Calculate bolt axial stiffness.
REQ-BLT-010 Calculate clamped-member stiffness.
REQ-BLT-011 Calculate load fraction transferred to bolt.
REQ-BLT-012 Calculate residual clamp force.
REQ-BLT-013 Check joint separation.
REQ-BLT-014 Check bolt yield under preload plus service load.
REQ-BLT-015 Check fatigue from fluctuating external axial load.
REQ-BLT-016 Check thread stripping for internal and external threads.
REQ-BLT-017 Check bearing pressure under head/nut/washer where applicable.
REQ-BLT-018 Check shear slip resistance for friction-grip joints.
REQ-BLT-019 Support eccentric single-bolt joint workflow.
REQ-BLT-020 Support bolt groups under force and moment.
REQ-BLT-021 Resolve bolt-group axial loads from overturning moment.
REQ-BLT-022 Resolve bolt-group shear from direct shear and in-plane torsion.
REQ-BLT-023 Identify the critical bolt.
REQ-BLT-024 Support preload scatter.
REQ-BLT-025 Support embedment/settlement loss.
REQ-BLT-026 Support temperature-induced preload change.
REQ-BLT-027 Support hollow bolts and studs.
REQ-BLT-028 Support tapped-hole engagement checks.
REQ-BLT-029 Support washers and flange-bearing diameter effects.
REQ-BLT-030 Provide a VDI-2230-oriented detailed calculation path without reproducing copyrighted standard text.

## 6. Shafts and axles
REQ-SHF-001 Calculate bending stress in solid circular shafts.
REQ-SHF-002 Calculate torsional shear stress.
REQ-SHF-003 Calculate combined von Mises stress.
REQ-SHF-004 Support hollow shafts.
REQ-SHF-005 Support multiple stations along a stepped shaft.
REQ-SHF-006 Support point forces, distributed loads, torques and moments.
REQ-SHF-007 Solve bearing reactions in two planes.
REQ-SHF-008 Generate shear-force diagrams.
REQ-SHF-009 Generate bending-moment diagrams.
REQ-SHF-010 Generate torque diagrams.
REQ-SHF-011 Calculate deflection and slope.
REQ-SHF-012 Calculate angle of twist.
REQ-SHF-013 Support shoulder stress concentration factors.
REQ-SHF-014 Support keyway reduction factors.
REQ-SHF-015 Support spline root concentration factors.
REQ-SHF-016 Support fatigue Marin modifiers.
REQ-SHF-017 Support Goodman fatigue criterion.
REQ-SHF-018 Support Gerber fatigue criterion.
REQ-SHF-019 Support Soderberg fatigue criterion.
REQ-SHF-020 Calculate minimum required diameter for static strength.
REQ-SHF-021 Calculate minimum required diameter for fatigue.
REQ-SHF-022 Calculate first-pass critical speed using a documented simplified method.
REQ-SHF-023 Flag cases requiring rotor-dynamic analysis.
REQ-SHF-024 Check torsional rigidity.
REQ-SHF-025 Check bending rigidity.

## 7. Rolling bearings
REQ-BRG-001 Calculate basic L10 rating life.
REQ-BRG-002 Support ball-bearing exponent.
REQ-BRG-003 Support roller-bearing exponent.
REQ-BRG-004 Calculate L10h from rpm.
REQ-BRG-005 Support equivalent dynamic bearing load from radial and axial components.
REQ-BRG-006 Support bearing-type-specific X/Y factors as a data-driven table.
REQ-BRG-007 Calculate static safety factor from C0 and equivalent static load.
REQ-BRG-008 Support required reliability above 90% through documented reliability factors.
REQ-BRG-009 Support lubrication and contamination modifiers in an ISO-281-oriented advanced path.
REQ-BRG-010 Support variable load spectra through equivalent load calculation.
REQ-BRG-011 Support variable speed duty cycle.
REQ-BRG-012 Support paired bearing arrangements.
REQ-BRG-013 Check minimum load requirement when manufacturer data exists.
REQ-BRG-014 Check speed limit when manufacturer data exists.
REQ-BRG-015 Check misalignment limits when manufacturer data exists.
REQ-BRG-016 Support catalogue selection by bore, OD, width, C and C0.
REQ-BRG-017 Rank candidate bearings by life margin and envelope.
REQ-BRG-018 Warn that wear, corrosion and electrical erosion are outside ISO 281 fatigue-life scope.

## 8. Gears
REQ-GER-001 Support spur gears.
REQ-GER-002 Support helical gears.
REQ-GER-003 Support external gear pairs.
REQ-GER-004 Support internal gear pairs as an advanced module.
REQ-GER-005 Calculate pitch diameters from module and tooth count.
REQ-GER-006 Calculate center distance.
REQ-GER-007 Calculate ratio and output speed.
REQ-GER-008 Calculate tangential tooth load from torque.
REQ-GER-009 Calculate radial force.
REQ-GER-010 Calculate axial force for helical gears.
REQ-GER-011 Provide Lewis bending as preliminary sizing only.
REQ-GER-012 Provide ISO-6336-oriented root bending verification.
REQ-GER-013 Provide ISO-6336-oriented contact stress verification.
REQ-GER-014 Support application/service factors.
REQ-GER-015 Support dynamic factors.
REQ-GER-016 Support face-load distribution factors.
REQ-GER-017 Support size/life factors where relevant.
REQ-GER-018 Support material hardness and allowable stress data.
REQ-GER-019 Check contact ratio.
REQ-GER-020 Check undercut risk.
REQ-GER-021 Support profile shift in advanced mode.
REQ-GER-022 Support efficiency estimate.
REQ-GER-023 Calculate heat generation from power loss.
REQ-GER-024 Support planetary gear kinematics as a separate module.
REQ-GER-025 Support bevel gears as a separate module.
REQ-GER-026 Support worm gear kinematics and efficiency as a separate module.

## 9. Keys, splines and pins
REQ-KSP-001 Calculate rectangular key shear stress.
REQ-KSP-002 Calculate rectangular key bearing/crushing stress.
REQ-KSP-003 Calculate required key length.
REQ-KSP-004 Support parallel key preferred sizes.
REQ-KSP-005 Support Woodruff key preliminary sizing.
REQ-KSP-006 Support involute spline torque capacity with explicit assumptions.
REQ-KSP-007 Support straight-sided spline preliminary capacity.
REQ-KSP-008 Calculate pin single-shear stress.
REQ-KSP-009 Calculate pin double-shear stress.
REQ-KSP-010 Calculate pin bearing stress.
REQ-KSP-011 Calculate pin bending when joint geometry produces bending.
REQ-KSP-012 Check lug net-section tension.
REQ-KSP-013 Check lug shear-out/tear-out.
REQ-KSP-014 Check combined pin-lug governing margin.

## 10. Springs
REQ-SPR-001 Support helical compression springs.
REQ-SPR-002 Calculate spring index.
REQ-SPR-003 Calculate Wahl stress correction.
REQ-SPR-004 Calculate maximum shear stress.
REQ-SPR-005 Calculate spring rate.
REQ-SPR-006 Calculate deflection.
REQ-SPR-007 Calculate solid height.
REQ-SPR-008 Check clash allowance.
REQ-SPR-009 Check buckling slenderness.
REQ-SPR-010 Check static yielding.
REQ-SPR-011 Support fluctuating-load fatigue check.
REQ-SPR-012 Support extension springs.
REQ-SPR-013 Support torsion springs.
REQ-SPR-014 Support Belleville/disc springs as an advanced module.
REQ-SPR-015 Provide minimum/maximum force over travel.
REQ-SPR-016 Provide candidate wire diameter and coil diameter search.

## 11. Welded joints
REQ-WLD-001 Support fillet weld direct shear.
REQ-WLD-002 Support multiple weld segments.
REQ-WLD-003 Calculate weld-group centroid.
REQ-WLD-004 Calculate weld-group polar property.
REQ-WLD-005 Resolve shear from in-plane moment.
REQ-WLD-006 Resolve normal stress from out-of-plane bending where applicable.
REQ-WLD-007 Combine directional weld stresses using the selected design method.
REQ-WLD-008 Support intermittent weld geometry.
REQ-WLD-009 Check minimum effective throat input.
REQ-WLD-010 Support material and electrode allowables as data rather than hard-coded values.
REQ-WLD-011 Support fatigue category as an advanced workflow.
REQ-WLD-012 Generate a weld-group sketch from entered coordinates.

## 12. Power screws and threads
REQ-SCR-001 Support square-thread power screws.
REQ-SCR-002 Support trapezoidal/Acme geometry with appropriate thread-angle effect.
REQ-SCR-003 Calculate lead and lead angle.
REQ-SCR-004 Calculate raising torque.
REQ-SCR-005 Calculate lowering torque.
REQ-SCR-006 Calculate efficiency.
REQ-SCR-007 Determine self-locking tendency.
REQ-SCR-008 Calculate thread bearing pressure.
REQ-SCR-009 Calculate screw core compression/tension stress.
REQ-SCR-010 Calculate torsional stress.
REQ-SCR-011 Calculate combined stress.
REQ-SCR-012 Check column buckling for long screws.
REQ-SCR-013 Check critical speed for rotating screws using a documented approximation.
REQ-SCR-014 Support ball screw life calculation as a separate catalogue-driven module.

## 13. Fits, interference and hubs
REQ-FIT-001 Support clearance, transition and interference fit classes as library data.
REQ-FIT-002 Calculate minimum and maximum diametral interference from tolerances.
REQ-FIT-003 Calculate press-fit contact pressure for documented cylinder assumptions.
REQ-FIT-004 Calculate transferable torque by friction.
REQ-FIT-005 Calculate transferable axial force by friction.
REQ-FIT-006 Calculate hub hoop stress.
REQ-FIT-007 Calculate shaft stress due to interference.
REQ-FIT-008 Support thermal assembly temperature estimate.
REQ-FIT-009 Check risk of yielding from maximum interference.
REQ-FIT-010 Distinguish nominal, minimum and maximum fit cases.

## 14. Belts, chains and couplings
REQ-TRN-001 Support flat-belt tension ratio.
REQ-TRN-002 Support V-belt preliminary power sizing.
REQ-TRN-003 Support synchronous belt kinematics.
REQ-TRN-004 Calculate belt speed.
REQ-TRN-005 Calculate shaft radial load from belt tensions.
REQ-TRN-006 Support roller-chain kinematics.
REQ-TRN-007 Calculate chain speed and polygonal-effect warning.
REQ-TRN-008 Support chain catalogue selection when data are available.
REQ-TRN-009 Support rigid coupling bolt torque distribution.
REQ-TRN-010 Support flexible coupling selection envelope from torque/speed/service factor.

## 15. Beams, columns and simple structural checks
REQ-STR-001 Support common beam boundary conditions.
REQ-STR-002 Calculate reactions.
REQ-STR-003 Calculate peak bending stress.
REQ-STR-004 Calculate peak shear stress for supported section types.
REQ-STR-005 Calculate deflection.
REQ-STR-006 Calculate slope.
REQ-STR-007 Support Euler column buckling.
REQ-STR-008 Calculate slenderness ratio.
REQ-STR-009 Warn when Euler assumptions are not appropriate.
REQ-STR-010 Support rectangular, circular, hollow circular and user-defined section properties.

## 16. Fatigue and fracture-aware workflow
REQ-FAT-001 Support mean and alternating stress decomposition.
REQ-FAT-002 Support Goodman.
REQ-FAT-003 Support Gerber.
REQ-FAT-004 Support Soderberg.
REQ-FAT-005 Support Marin endurance-limit modifiers.
REQ-FAT-006 Support surface factor.
REQ-FAT-007 Support size factor.
REQ-FAT-008 Support load factor.
REQ-FAT-009 Support temperature factor.
REQ-FAT-010 Support reliability factor.
REQ-FAT-011 Support stress concentration Kt.
REQ-FAT-012 Support notch sensitivity and fatigue factor Kf.
REQ-FAT-013 Support finite-life S-N interpolation from validated data.
REQ-FAT-014 Support Miner cumulative damage for load spectra.
REQ-FAT-015 Support separate axial, bending and torsional fatigue inputs.
REQ-FAT-016 Flag cases where multiaxial fatigue method selection matters.
REQ-FAT-017 Provide fracture-mechanics inputs only as a separate advanced module with strong applicability warnings.

## 17. Standard and provenance framework
REQ-STD-001 Standard references shall be metadata, not copied copyrighted text.
REQ-STD-002 Each standard-based method shall store standard family, edition, clause-reference field and implementation revision.
REQ-STD-003 The application shall distinguish current, superseded and draft standard metadata.
REQ-STD-004 Standards data updates shall not silently alter saved historical results.
REQ-STD-005 Recalculation under a newer equation set shall create a new result revision.
REQ-STD-006 The app shall support VDI-oriented bolted-joint workflows.
REQ-STD-007 The app shall support ISO-281-oriented rolling-bearing workflows.
REQ-STD-008 The app shall support ISO-6336-oriented cylindrical-gear workflows.
REQ-STD-009 The app shall support selected ISO/DIN fit and fastener dimensional libraries.
REQ-STD-010 The app shall allow project-specific internal allowables and rules to override generic recommendations, with a visible provenance tag.

## 18. Automated engineering assistant
REQ-AI-001 The deterministic equation engine shall remain authoritative for numerical results.
REQ-AI-002 Any language-model assistant shall explain rather than invent numerical answers.
REQ-AI-003 The assistant shall identify missing inputs.
REQ-AI-004 The assistant shall map natural-language loads to candidate input fields, requiring user review before calculation.
REQ-AI-005 The assistant shall explain the governing failure mode.
REQ-AI-006 The assistant shall suggest which geometric parameter has the highest leverage based on deterministic sensitivity analysis.
REQ-AI-007 The assistant shall not claim standard compliance if required checks are incomplete.
REQ-AI-008 The assistant shall generate a calculation narrative from the frozen deterministic result.
REQ-AI-009 The assistant shall list unresolved assumptions.
REQ-AI-010 The assistant shall support Turkish engineering terminology and English report output.

## 19. Optimizer
REQ-OPT-001 Support one-dimensional minimum-size solving.
REQ-OPT-002 Support preferred-series snapping.
REQ-OPT-003 Support discrete catalogue candidate search.
REQ-OPT-004 Support multi-constraint filtering.
REQ-OPT-005 Support mass minimization where density and geometry are known.
REQ-OPT-006 Support cost ranking when user-supplied cost data exist.
REQ-OPT-007 Support safety-factor target constraints.
REQ-OPT-008 Support stiffness constraints.
REQ-OPT-009 Support life constraints.
REQ-OPT-010 Optimizer results shall be re-run through the full verification engine before being labelled acceptable.

## 20. Reports and traceability
REQ-RPT-001 Generate an engineering calculation report.
REQ-RPT-002 Report shall include project identifiers.
REQ-RPT-003 Report shall include revision and timestamp.
REQ-RPT-004 Report shall include all inputs and units.
REQ-RPT-005 Report shall include all material properties used.
REQ-RPT-006 Report shall include intermediate derived quantities needed for auditability.
REQ-RPT-007 Report shall include final checks and margins.
REQ-RPT-008 Report shall include assumptions and warnings.
REQ-RPT-009 Report shall include standard-method metadata.
REQ-RPT-010 Report shall include application version and equation-set version.
REQ-RPT-011 Report shall distinguish user-entered, library and calculated values.
REQ-RPT-012 Report shall identify the governing failure mode.
REQ-RPT-013 Report shall include a verification status summary.
REQ-RPT-014 Report export shall not modify the calculation state.

## 21. Validation strategy
REQ-VAL-001 Every module shall have hand-check reference cases.
REQ-VAL-002 Every module shall have boundary-value tests.
REQ-VAL-003 Every module shall have invalid-input tests.
REQ-VAL-004 Every module shall have unit-consistency tests.
REQ-VAL-005 Every module shall have monotonicity sanity tests where applicable.
REQ-VAL-006 Every standard-based implementation shall have at least one independently sourced benchmark case that can legally be used.
REQ-VAL-007 Numerical tolerances shall be defined per result type.
REQ-VAL-008 Regression results shall be stored with equation-set version.
REQ-VAL-009 UI tests shall verify that hidden fields cannot contribute stale values.
REQ-VAL-010 Release CI shall fail when deterministic regression tests fail.

## 22. Release maturity levels
Level A — Formula prototype: equation implemented, basic validation only.
Level B — Engineering preliminary: input validation, assumptions, factor of safety, reference tests.
Level C — Detailed verification: complete required failure modes for defined scope, standard-method metadata, traceability.
Level D — Catalogue-assisted design: discrete commercial/standard size selection.
Level E — Project-qualified: internal validation set, controlled material data, locked equation version and signed report workflow.

No module shall present itself as Level C or above until its required validation matrix is complete.

## 23. Initial implementation milestones
M1 Core: existing eight modules, strict input validation, module metadata.
M2 Strength: pins/lugs, thread stripping, beam/column, power screw, press fit, torsion/deflection.
M3 Bolted Joint Pro: preload, stiffness, separation, fatigue, group loads.
M4 Shaft System: reactions, diagrams, fatigue, deflection, critical-speed screening.
M5 Bearing Pro: radial/axial equivalent load, static check, modified life, catalogue selection.
M6 Gear Pro: geometry, forces, ISO-oriented bending/contact calculation architecture.
M7 Transmission: belts, chains, couplings, keys/splines.
M8 Fatigue Pro: Marin factors, S-N life, Miner damage.
M9 Optimizer: solve and standard-size selection.
M10 Reports: complete audit report and project history.

## 24. Definition of done for any engineering module
A module is not done merely because it returns a number. It is done only when: input domain is defined; units are explicit; governing equations are implemented; assumptions are visible; invalid geometry is rejected; all intended failure modes are listed; applicable failure modes are calculated; a safety/margin criterion is applied; a reference benchmark passes; boundary tests pass; report fields exist; version/provenance metadata exist; and limitations are stated.

This specification is intentionally modular and traceable. Detailed module specifications, equation catalogues, UI field dictionaries, validation cases and data-library schemas are maintained as companion volumes so the total controlled design specification can grow beyond a conventional single spreadsheet and into a multi-volume engineering software baseline.