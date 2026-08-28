#!/usr/bin/env python3
"""Numerical regression checks for Whiffletree Aero v8 6DOF synthesis.
Uses only the Python standard library so it can run in GitHub Actions without dependencies.
"""
import math


def solve_linear(a, b):
    n = len(b)
    m = [list(a[r]) + [b[r]] for r in range(n)]
    scale = max(1.0, max(abs(v) for row in a for v in row))
    eps = scale * 1e-12
    for col in range(n):
        piv = max(range(col, n), key=lambda r: abs(m[r][col]))
        if abs(m[piv][col]) < eps:
            continue
        m[col], m[piv] = m[piv], m[col]
        p = m[col][col]
        for c in range(col, n + 1):
            m[col][c] /= p
        for r in range(n):
            if r == col:
                continue
            f = m[r][col]
            if abs(f) < eps:
                continue
            for c in range(col, n + 1):
                m[r][c] -= f * m[col][c]
    return [m[i][n] for i in range(n)]


def min_norm(raw_a, target, lref):
    n = len(raw_a[0])
    a = [row[:] for row in raw_a]
    b = target[:]
    for r in (3, 4, 5):
        b[r] /= lref
        for i in range(n):
            a[r][i] /= lref
    g = [[0.0] * 6 for _ in range(6)]
    for r in range(6):
        for c in range(6):
            g[r][c] = sum(a[r][i] * a[c][i] for i in range(n))
    trace = sum(g[i][i] for i in range(6))
    lam = max(1e-12, trace * 1e-10)
    for i in range(6):
        g[i][i] += lam
    y = solve_linear(g, b)
    q = [sum(a[r][i] * y[r] for r in range(6)) for i in range(n)]
    applied = [sum(raw_a[r][i] * q[i] for i in range(n)) for r in range(6)]
    return q, applied


def column(x, y, z, ux, uy, uz):
    un = math.sqrt(ux*ux + uy*uy + uz*uz)
    ux, uy, uz = ux/un, uy/un, uz/un
    return [
        ux, uy, uz,
        y*uz - z*uy,
        z*ux - x*uz,
        x*uy - y*ux,
    ]


def matrix_from_columns(cols):
    return [[cols[i][r] for i in range(len(cols))] for r in range(6)]


def check(name, a, target, lref, force_tol=0.5, moment_tol=2.0):
    q, applied = min_norm(a, target, lref)
    res = [target[i] - applied[i] for i in range(6)]
    ferr = math.sqrt(sum(res[i]**2 for i in range(3)))
    merr = math.sqrt(sum(res[i]**2 for i in range(3, 6)))
    print(name)
    print("  q:", [round(v, 3) for v in q])
    print("  applied:", [round(v, 6) for v in applied])
    print("  residual force/moment:", ferr, merr)
    assert ferr <= force_tol, f"{name}: force closure regression {ferr} N"
    assert merr <= moment_tol, f"{name}: moment closure regression {merr} Nmm"


def vertical_case():
    L, D, n = 5500.0, 638.5, 8
    cols = []
    for i in range(n):
        x = -L/2 + (i + 0.5)*L/n
        y = (-1 if i % 2 == 0 else 1) * D * 0.35
        cols.append(column(x, y, 0, 0, 0, 1))
    a = matrix_from_columns(cols)
    target = [0, 0, 120000, 0, 18_000_000, 0]
    check("vertical 8-pad Fz+My", a, target, L)


def full_6dof_case():
    L, D, n = 5500.0, 638.5, 12
    cols = []
    for i in range(n):
        seg = i // 3
        seg_count = max(4, math.ceil(n / 3.0))
        x = -L/2 + (seg + 0.5)*L/seg_count
        y = (-1 if seg % 2 == 0 else 1) * D * 0.50
        z = (-1 if (seg // 2) % 2 == 0 else 1) * D * 0.33
        axis = i % 3
        sign = 1 if (seg + i) % 2 == 0 else -1
        ux = sign if axis == 0 else 0
        uy = sign if axis == 1 else 0
        uz = sign if axis == 2 else 0
        cols.append(column(x, y, z, ux, uy, uz))
    a = matrix_from_columns(cols)
    target = [10000, -5000, 120000, 2_000_000, 18_000_000, -3_000_000]
    check("12-pad full 6DOF", a, target, L)


if __name__ == "__main__":
    vertical_case()
    full_6dof_case()
    print("Whiffletree Aero v8 6DOF regression: PASS")
