from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PATCH=ROOT/'tools/apply_v110_mute_airframe_geometry_realism.py'
p=PATCH.read_text()
start=p.find("g=rep(g,'        int petals=20;float z0=3.34f,z1=3.82f,r0=.425f,r1=.315f;")
end=p.find("g=rep(g,'        int arcs=30;",start)
if start<0 or end<0:
    raise SystemExit('v110 nozzle compat anchors missing')
replacement="""# v110 compatibility: v93 owns the advanced 18-petal variable-nozzle iris,
# inner liner and actuator geometry. Preserve that verified system rather than
# replacing it with anchors from the older pre-v93 mesh.
"""
p=p[:start]+replacement+p[end:]
PATCH.write_text(p)
print('v110 nozzle compatibility enabled: preserve v93 18-petal variable nozzle')
