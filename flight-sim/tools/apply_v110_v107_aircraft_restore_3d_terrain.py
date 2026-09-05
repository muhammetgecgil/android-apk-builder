from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
JET=PKG/'Jet3DView.java'
GRADLE=ROOT/'app/build.gradle'

j=JET.read_text()

# Restore the aircraft presentation/camera behavior to the v107-era geometry.
# Keep v108/v109 terrain rendering intact; only undo aircraft-facing v109 changes.
old='GLES20.glDepthMask(true);drawJourneyTerrain();GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(vbOpaque,opaqueCount);GLES20.glEnable(GLES20.GL_CULL_FACE);'
new='GLES20.glDepthMask(true);drawJourneyTerrain();bindAndDraw(vbOpaque,opaqueCount);'
if old in j:
    j=j.replace(old,new,1)
elif new not in j:
    raise SystemExit('v110 anchor missing: v109 two-sided airframe draw')

# Put external cameras back exactly where they were before v109 raised them.
replacements={
    'camY=3.15f;camZ=12.8f+lag':'camY=1.50f;camZ=12.8f+lag',
    'camY=3.05f;camZ=8.0f':'camY=1.18f;camZ=8.0f',
    'camY=3.10f+bob*.2f;camZ=16.5f+lag':'camY=1.32f+bob*.2f;camZ=16.5f+lag',
    'camY=3.20f;camZ=3.6f':'camY=1.30f;camZ=3.6f',
    'camY=4.25f+(float)Math.sin(a*.6f)*1.10f;camZ=(float)Math.cos(a)*20f':'camY=2.3f+(float)Math.sin(a*.6f)*1.5f;camZ=(float)Math.cos(a)*20f',
    'camY=3.00f;camZ=25.5f':'camY=.62f;camZ=25.5f'
}
for a,b in replacements.items():
    if a in j:j=j.replace(a,b,1)
    elif b not in j:raise SystemExit('v110 anchor missing: camera restore '+a)

v109cin='int q=((int)(t/7.0f))%6;mode=q==0?CAMERA_REAR:q==1?CAMERA_RIGHT_QUARTER:q==2?CAMERA_FLY_BY:q==3?CAMERA_TOWER:q==4?CAMERA_ORBIT:CAMERA_LEFT_QUARTER;'
v107cin='int q=((int)(t/7.0f))%6;mode=q==0?CAMERA_LOW_CHASE:q==1?CAMERA_WING:q==2?CAMERA_FLY_BY:q==3?CAMERA_TOWER:q==4?CAMERA_ORBIT:CAMERA_RUNWAY;'
if v109cin in j:j=j.replace(v109cin,v107cin,1)
elif v107cin not in j:raise SystemExit('v110 anchor missing: cinema schedule restore')

# Keep the fake underside cap removed. Aircraft remains its original mesh, with normal culling.
if 'AirframeUndersideSeal' in j or 'undersideBuffer' in j or 'undersideCount' in j:
    raise SystemExit('v110 must not reintroduce artificial underside geometry')

JET.write_text(j)

g=GRADLE.read_text()
g=re.sub(r'versionCode\s+\d+','versionCode 110',g,count=1)
g=re.sub(r"versionName\s+['\"][^'\"]+['\"]","versionName '26.28-avm36.1-v107-aircraft-restore-3d-terrain'",g,count=1)
GRADLE.write_text(g)
print('v110 applied: v107 aircraft presentation restored; v109 dense 3D terrain retained')
