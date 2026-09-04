from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
JET=PKG/'Jet3DView.java'
GRADLE=ROOT/'app/build.gradle'
SEAL=PKG/'visual/AirframeUndersideSeal.java'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v109 anchor missing: {label}')
    return text.replace(old,new,1)

j=JET.read_text()

# ---------------------------------------------------------------------------
# Correct the v108 underside approach. Do NOT add a blanket belly plate.
# Instead, render the aircraft's own opaque shell two-sided with depth writes.
# This fixes back-face transparency without changing the aircraft silhouette,
# hiding landing-gear detail, or inventing a slab under the fuselage.
# ---------------------------------------------------------------------------
j=j.replace('import com.mg.fixturecockpitsim.visual.AirframeUndersideSeal;\n','')
j=rep(j,'birdBuffer,undersideBuffer,terrain0','birdBuffer,terrain0','remove underside buffer')
j=rep(j,'birdCount,undersideCount,terrainCount0','birdCount,terrainCount0','remove underside count')
j=rep(j,
'float[] belly=AirframeUndersideSeal.build();undersideBuffer=buffer(belly);undersideCount=belly.length/7;',
'',
'remove underside seal mesh build')

old_draw='drawJourneyTerrain();bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);bindAndDraw(engineSolidBuffer,engineSolidCount);bindAndDraw(obOpaque,ordnanceCount);GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(undersideBuffer,undersideCount);GLES20.glEnable(GLES20.GL_CULL_FACE);bindAndDraw(vbCanopy,canopyCount);'
new_draw='drawJourneyTerrain();GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);GLES20.glEnable(GLES20.GL_CULL_FACE);bindAndDraw(engineSolidBuffer,engineSolidCount);bindAndDraw(obOpaque,ordnanceCount);bindAndDraw(vbCanopy,canopyCount);'
j=rep(j,old_draw,new_draw,'replace blanket underside seal with two-sided aircraft shell')

# Terrain materials now include explicit rock, snow and limestone/cliff faces.
# The deleted part-64 branch was the v108 artificial underside material.
old_shader='if((vP>59.5&&vP<63.5)||(vP>64.5&&vP<65.5)){vec3 N=normalize(vN);float lit=.34+.66*max(dot(N,normalize(uLightDir)),0.);vec3 tc=vP<60.5?vec3(.26,.31,.25):(vP<61.5?vec3(.25,.34,.29):(vP<62.5?vec3(.62,.46,.25):(vP<63.5?vec3(.49,.37,.22):vec3(.10,.15,.21))));float fog=smoothstep(45.,210.,-vPos.z);tc=mix(tc,vec3(.48,.57,.61),fog*.72);gl_FragColor=vec4(tc*lit,1.);return;}if(vP>63.5&&vP<64.5){vec3 N=normalize(vN);float lit=.30+.70*max(dot(N,normalize(uLightDir)),0.);gl_FragColor=vec4(vec3(.205,.218,.230)*lit,1.);return;}if(vP>39.5&&vP<40.5){'
new_shader='if((vP>59.5&&vP<63.5)||(vP>64.5&&vP<68.5)){vec3 N=normalize(vN);float ndl=max(dot(N,normalize(uLightDir)),0.);float lit=.28+.72*ndl;vec3 tc=vP<60.5?vec3(.22,.29,.20):(vP<61.5?vec3(.22,.32,.28):(vP<62.5?vec3(.66,.49,.27):(vP<63.5?vec3(.47,.33,.20):(vP<65.5?vec3(.08,.13,.19):(vP<66.5?vec3(.29,.30,.30):(vP<67.5?vec3(.78,.82,.84):vec3(.48,.45,.39)))))));float slope=1.-clamp(N.y,0.,1.);float micro=.91+.09*sin(vPos.x*.43+vPos.z*.19);tc*=mix(1.,.73,slope*.72)*micro;float fog=smoothstep(55.,255.,-vPos.z);tc=mix(tc,vec3(.48,.57,.61),fog*.68);gl_FragColor=vec4(tc*lit,1.);return;}if(vP>39.5&&vP<40.5){'
j=rep(j,old_shader,new_shader,'high-detail terrain materials and removal of seal material')
JET.write_text(j)

# The source can remain in Git history, but it must not be part of the v109
# compiled source tree: the renderer no longer uses artificial closure geometry.
if SEAL.exists():
    SEAL.unlink()

# Version.
g=GRADLE.read_text()
g=re.sub(r'versionCode\s+\d+','versionCode 109',g,count=1)
g=re.sub(r"versionName\s+['\"][^'\"]+['\"]","versionName '26.27-avm36.0-opaque-airframe-highdetail-toros'",g,count=1)
GRADLE.write_text(g)
print('v109 applied: no underside slab; original opaque shell rendered two-sided + high-detail 3D Taurus terrain')
