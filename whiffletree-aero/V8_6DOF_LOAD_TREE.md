# Whiffletree Aero v8 — 6DOF Moment-Driven Load Tree

## Purpose

v8 converts a target structural-test resultant into physical pad-force demands before the whiffletree topology is synthesized.

Target vector:

`b = [Fx, Fy, Fz, Mx, My, Mz]^T`

For pad `i`, define position `r_i=[x_i,y_i,z_i]` in mm and an allowed unit load direction `u_i=[ux_i,uy_i,uz_i]`. The scalar pad/load-train demand `q_i` generates:

`F_i = q_i u_i`

`M_i = r_i × F_i`

Each pad contributes one column to the 6DOF equilibrium matrix `A`, and the solver finds `q` from:

`A q = b`

The moment equations are internally scaled by a characteristic rig length for numerical conditioning, while reported values remain N and Nmm.

## Physical modes

### Tension / compression

`q_i` may be positive or negative. This represents a load train that can transfer both pull and push, or a geometry where sign is carried by the defined direction.

### Tension only

The solver enforces `q_i >= 0` with an active-set process. A pad that requires negative force is removed and the remaining system is solved again. If the remaining geometry cannot close the requested target, the residual is reported as infeasible rather than hidden.

## Real-world architecture families represented

- unequal-arm two-way passive whiffletree
- multi-level passive whiffletree
- LE/TE or fore/aft force-couple moment tree
- tension-only cable/rod tree
- tension/compression whiffletree
- separate-axis X/Y/Z load trees
- multi-actuator 3D/gimbaled load-train architecture

The existing topology engine computes unequal passive-beam pivot arms from:

`F_left * a = F_right * b`

so the pivot is not fixed at the beam center.

## DOF authority

The application reports matrix rank.

- rank 6: the defined pad positions/directions have full independent 6DOF authority.
- rank < 6: some combinations of forces and moments cannot be independently generated with that physical geometry.

A rank-deficient geometry may still exactly reproduce a particular requested target if that target lies in its controllable subspace.

Example: purely vertical pads commonly provide Fz and selected bending/torsion moments, but cannot independently create arbitrary Fx, Fy and Mz.

## Real-world validity checks

v8 reports:

- force closure residual
- moment closure residual
- matrix rank
- active/inactive pads in tension-only mode
- pad force demands
- force-direction spread
- passive beam moment residual
- actuator grouping / capacity checks from the existing optimizer
- recommended physical load-tree family

If pad directions differ too much, the software warns against combining them in one passive beam and recommends separate-axis or multi-actuator load trains.

## Current engineering boundary

This is a structural-test load-path synthesis and preliminary rig-sizing tool, not a release-to-manufacture stress substantiation package. Final hardware still requires beam, pin, clevis, rod-end, bearing, fatigue, buckling, secondary-load, joint-slip, fixture stiffness and test-safety substantiation.
