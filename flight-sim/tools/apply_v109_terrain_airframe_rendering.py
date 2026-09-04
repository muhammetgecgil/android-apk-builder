from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
JET=PKG/'Jet3DView.java'
GRADLE=ROOT/'app/build.gradle'


def require(text,needle,label):
    if needle not in text: raise SystemExit(f'v109 anchor missing: {label}')

j=JET.read_text()

# ---------------------------------------------------------------------------
# Remove the v108 artificial underside closure completely.  The user's intent
# is to avoid exposing an incomplete/transparent underside, not to fabricate a
# broad plate over it.  No replacement belly geometry is created here.
# ---------------------------------------------------------------------------
j=j.replace('import com.mg.fixturecockpitsim.visual.AirframeUndersideSeal;\n','')
j=j.replace('FloatBuffer undersideBuffer,terrain0,terrain1,terrain2,terrain3,terrain4;','FloatBuffer terrain0,terrain1,terrain2,terrain3,terrain4;')
j=j.replace('int undersideCount,terrainCount0,terrainCount1,terrainCount2,terrainCount3,terrainCount4;','int terrainCount0,terrainCount1,terrainCount2,terrainCount3,terrainCount4;')
j=j.replace('float[] belly=AirframeUndersideSeal.build();undersideBuffer=buffer(belly);undersideCount=belly.length/7;','')
j=j.replace('GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(undersideBuffer,undersideCount);GLES20.glEnable(GLES20.GL_CULL_FACE);','')

# Draw the aircraft's own opaque shell two-sided during the external solid pass.
# This uses only existing airframe triangles; it does not add a fake skin/plate.
old='GLES20.glDepthMask(true);drawJourneyTerrain();bindAndDraw(vbOpaque,opaqueCount);'
new='GLES20.glDepthMask(true);drawJourneyTerrain();GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(vbOpaque,opaqueCount);GLES20.glEnable(GLES20.GL_CULL_FACE);'
if old in j:j=j.replace(old,new,1)
elif new not in j:raise SystemExit('v109 anchor missing: opaque airframe draw')

# ---------------------------------------------------------------------------
# External/cinematic cameras stay above the aircraft reference plane so the
# incomplete underside is not deliberately showcased.  Manual flight remains
# untouched; only camera viewpoints are raised.
# ---------------------------------------------------------------------------
cam_replacements={
    'camY=1.50f;camZ=12.8f+lag':'camY=3.15f;camZ=12.8f+lag',
    'camY=1.18f;camZ=8.0f':'camY=3.05f;camZ=8.0f',
    'camY=1.32f+bob*.2f;camZ=16.5f+lag':'camY=3.10f+bob*.2f;camZ=16.5f+lag',
    'camY=1.30f;camZ=3.6f':'camY=3.20f;camZ=3.6f',
    'camY=2.3f+(float)Math.sin(a*.6f)*1.5f;camZ=(float)Math.cos(a)*20f':'camY=4.25f+(float)Math.sin(a*.6f)*1.10f;camZ=(float)Math.cos(a)*20f',
    'camY=.62f;camZ=25.5f':'camY=3.00f;camZ=25.5f'
}
changed=0
for a,b in cam_replacements.items():
    if b in j:continue
    if a in j:j=j.replace(a,b,1);changed+=1
if changed<4 and not all(b in j for b in cam_replacements.values()):
    raise SystemExit('v109 camera geometry did not expose enough safe anchors')

# Cinema rotation no longer selects the intentionally low chase/runway cameras.
oldcin='int q=((int)(t/7.0f))%6;mode=q==0?CAMERA_LOW_CHASE:q==1?CAMERA_WING:q==2?CAMERA_FLY_BY:q==3?CAMERA_TOWER:q==4?CAMERA_ORBIT:CAMERA_RUNWAY;'
newcin='int q=((int)(t/7.0f))%6;mode=q==0?CAMERA_REAR:q==1?CAMERA_RIGHT_QUARTER:q==2?CAMERA_FLY_BY:q==3?CAMERA_TOWER:q==4?CAMERA_ORBIT:CAMERA_LEFT_QUARTER;'
if oldcin in j:j=j.replace(oldcin,newcin,1)
elif newcin not in j:raise SystemExit('v109 anchor missing: cinema camera schedule')

# ---------------------------------------------------------------------------
# Replace the simple terrain material with slope/elevation-aware lighting.  The
# denser v109 geometry supplies smooth normals, so the ridges read as actual 3D
# volumes rather than a flat background silhouette.
# ---------------------------------------------------------------------------
oldterrain='if((vP>59.5&&vP<63.5)||(vP>64.5&&vP<65.5)){vec3 N=normalize(vN);float lit=.34+.66*max(dot(N,normalize(uLightDir)),0.);vec3 tc=vP<60.5?vec3(.26,.31,.25):(vP<61.5?vec3(.25,.34,.29):(vP<62.5?vec3(.62,.46,.25):(vP<63.5?vec3(.49,.37,.22):vec3(.10,.15,.21))));float fog=smoothstep(45.,210.,-vPos.z);tc=mix(tc,vec3(.48,.57,.61),fog*.72);gl_FragColor=vec4(tc*lit,1.);return;}if(vP>63.5&&vP<64.5){vec3 N=normalize(vN);float lit=.30+.70*max(dot(N,normalize(uLightDir)),0.);gl_FragColor=vec4(vec3(.205,.218,.230)*lit,1.);return;}'
newterrain='if((vP>59.5&&vP<63.5)||(vP>64.5&&vP<65.5)){vec3 N=normalize(vN);float sun=max(dot(N,normalize(uLightDir)),0.);float slope=1.-clamp(N.y,0.,1.);float grain=.92+.08*sin(vPos.x*.73+vPos.z*.29)*sin(vPos.x*.19-vPos.z*.61);vec3 tc;if(vP<60.5){float elev=smoothstep(-3.,22.,vPos.y);tc=mix(vec3(.13,.20,.13),vec3(.36,.34,.30),clamp(slope*1.15+elev*.62,0.,1.));tc=mix(tc,vec3(.62,.62,.59),smoothstep(17.,30.,vPos.y)*.52);}else if(vP<61.5){tc=mix(vec3(.17,.29,.20),vec3(.46,.39,.29),clamp(slope*.95+smoothstep(5.,18.,vPos.y)*.5,0.,1.));}else if(vP<62.5){tc=mix(vec3(.68,.50,.27),vec3(.86,.68,.38),.35+.35*(1.-slope));}else if(vP<63.5){tc=mix(vec3(.42,.30,.18),vec3(.58,.43,.25),.42*(1.-slope));}else{tc=mix(vec3(.065,.095,.14),vec3(.16,.20,.25),.42*(1.-slope));}float lit=.24+.76*sun;tc*=lit*grain;tc+=vec3(.10,.12,.13)*slope*.16;float fog=smoothstep(92.,242.,-vPos.z);tc=mix(tc,vP>64.5?vec3(.12,.17,.24):vec3(.47,.56,.60),fog*.63);gl_FragColor=vec4(tc,1.);return;}'
if oldterrain in j:j=j.replace(oldterrain,newterrain,1)
elif newterrain not in j:raise SystemExit('v109 anchor missing: v108 terrain shader')

# There must be no runtime reference to the removed artificial closure.
if 'AirframeUndersideSeal' in j or 'undersideBuffer' in j or 'undersideCount' in j:
    raise SystemExit('v109 artificial underside closure still referenced')

JET.write_text(j)

g=GRADLE.read_text()
g=re.sub(r'versionCode\s+\d+','versionCode 109',g,count=1)
g=re.sub(r"versionName\s+['\"][^'\"]+['\"]","versionName '26.27-avm36.0-terrain-airframe-rendering'",g,count=1)
GRADLE.write_text(g)
print('v109 applied: no fake belly cap; underside-safe cameras; dense smooth-lit 3D terrain')
