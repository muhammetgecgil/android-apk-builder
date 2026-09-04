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
# Correct v108 underside handling. There is NO replacement belly plate in v109.
# Remove the artificial closure mesh and render the aircraft's own opaque shell
# two-sided while depth writes remain enabled. That makes back-facing aircraft
# skin visible from below without changing the silhouette or covering gear bays.
# ---------------------------------------------------------------------------
j=j.replace('import com.mg.fixturecockpitsim.visual.AirframeUndersideSeal;\n','')

# v108 deliberately adds these as independent declarations. Keep the terrain
# fields and remove only the artificial underside fields.
if 'FloatBuffer undersideBuffer' in j:
    j=j.replace('FloatBuffer undersideBuffer,terrain0,terrain1,terrain2,terrain3,terrain4;',
                'FloatBuffer terrain0,terrain1,terrain2,terrain3,terrain4;',1)
if 'int undersideCount' in j:
    j=j.replace('int undersideCount,terrainCount0,terrainCount1,terrainCount2,terrainCount3,terrainCount4;',
                'int terrainCount0,terrainCount1,terrainCount2,terrainCount3,terrainCount4;',1)

build='float[] belly=AirframeUndersideSeal.build();undersideBuffer=buffer(belly);undersideCount=belly.length/7;'
if build not in j:
    raise SystemExit('v109 anchor missing: underside seal mesh build')
j=j.replace(build,'',1)

seal_draw='GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(undersideBuffer,undersideCount);GLES20.glEnable(GLES20.GL_CULL_FACE);'
if seal_draw not in j:
    raise SystemExit('v109 anchor missing: underside seal draw')
j=j.replace(seal_draw,'',1)

# Put only the actual aircraft skin/detail into a two-sided opaque pass. Terrain,
# engine, stores and canopy retain their existing culling rules.
start_anchor='drawJourneyTerrain();bindAndDraw(vbOpaque,opaqueCount);'
if start_anchor not in j:
    raise SystemExit('v109 anchor missing: aircraft opaque draw')
j=j.replace(start_anchor,
            'drawJourneyTerrain();GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(vbOpaque,opaqueCount);',1)
start=j.find('drawJourneyTerrain();GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(vbOpaque,opaqueCount);')
detail='bindAndDraw(detailBuffer,detailCount);'
dpos=j.find(detail,start)
if dpos<0:
    raise SystemExit('v109 anchor missing: aircraft detail draw')
dend=dpos+len(detail)
j=j[:dend]+'GLES20.glEnable(GLES20.GL_CULL_FACE);'+j[dend:]

# Terrain materials: 60-63 base regions, 65 moonlit, 66 rock, 67 snow,
# 68 limestone/cliff. Part 64 (the removed belly slab) is intentionally absent.
old_shader='if((vP>59.5&&vP<63.5)||(vP>64.5&&vP<65.5)){vec3 N=normalize(vN);float lit=.34+.66*max(dot(N,normalize(uLightDir)),0.);vec3 tc=vP<60.5?vec3(.26,.31,.25):(vP<61.5?vec3(.25,.34,.29):(vP<62.5?vec3(.62,.46,.25):(vP<63.5?vec3(.49,.37,.22):vec3(.10,.15,.21))));float fog=smoothstep(45.,210.,-vPos.z);tc=mix(tc,vec3(.48,.57,.61),fog*.72);gl_FragColor=vec4(tc*lit,1.);return;}if(vP>63.5&&vP<64.5){vec3 N=normalize(vN);float lit=.30+.70*max(dot(N,normalize(uLightDir)),0.);gl_FragColor=vec4(vec3(.205,.218,.230)*lit,1.);return;}if(vP>39.5&&vP<40.5){'
new_shader='if((vP>59.5&&vP<63.5)||(vP>64.5&&vP<68.5)){vec3 N=normalize(vN);float ndl=max(dot(N,normalize(uLightDir)),0.);float lit=.28+.72*ndl;vec3 tc=vP<60.5?vec3(.22,.29,.20):(vP<61.5?vec3(.22,.32,.28):(vP<62.5?vec3(.66,.49,.27):(vP<63.5?vec3(.47,.33,.20):(vP<65.5?vec3(.08,.13,.19):(vP<66.5?vec3(.29,.30,.30):(vP<67.5?vec3(.78,.82,.84):vec3(.48,.45,.39)))))));float slope=1.-clamp(N.y,0.,1.);float micro=.91+.09*sin(vPos.x*.43+vPos.z*.19);tc*=mix(1.,.73,slope*.72)*micro;float fog=smoothstep(55.,255.,-vPos.z);tc=mix(tc,vec3(.48,.57,.61),fog*.68);gl_FragColor=vec4(tc*lit,1.);return;}if(vP>39.5&&vP<40.5){'
j=rep(j,old_shader,new_shader,'high-detail terrain materials / remove seal material')

# Defensive verification: no artificial underside geometry may remain in renderer.
if 'undersideBuffer' in j or 'undersideCount' in j or 'AirframeUndersideSeal' in j:
    raise SystemExit('v109 artificial underside geometry still referenced')
JET.write_text(j)

# Remove the artificial source from the build-time tree too.
if SEAL.exists():
    SEAL.unlink()

# Version.
g=GRADLE.read_text()
g=re.sub(r'versionCode\s+\d+','versionCode 109',g,count=1)
g=re.sub(r"versionName\s+['\"][^'\"]+['\"]","versionName '26.27-avm36.0-opaque-airframe-highdetail-toros'",g,count=1)
GRADLE.write_text(g)
print('v109 applied: underside slab removed; actual aircraft shell two-sided and opaque + high-detail 3D Taurus terrain')
