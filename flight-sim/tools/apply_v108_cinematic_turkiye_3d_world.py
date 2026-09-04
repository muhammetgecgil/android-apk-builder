from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
JET=PKG/'Jet3DView.java'
RUNTIME=PKG/'FlightRuntimeActivity.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:return text
    if old not in text:raise SystemExit(f'v108 anchor missing: {label}')
    return text.replace(old,new,1)

# Runtime: route atmosphere and journey progression.
r=RUNTIME.read_text()
if 'new CinematicJourneyOverlayView(this)' not in r:
    m=re.search(r'(?m)^\s*root\.addView\(world,new FrameLayout\.LayoutParams\(-1,-1\)\);\s*$',r)
    if not m:raise SystemExit('v108 anchor missing: world view insertion point')
    indent=re.match(r'\s*',m.group(0)).group(0)
    r=r[:m.start()]+m.group(0)+'\n'+indent+'root.addView(new CinematicJourneyOverlayView(this),new FrameLayout.LayoutParams(-1,-1));'+r[m.end():]
if 'CinematicJourneyState.reset()' not in r:
    anchor='mission.reset(state);'
    if anchor not in r:raise SystemExit('v108 anchor missing: mission reset')
    r=r.replace(anchor,anchor+'com.mg.fixturecockpitsim.sim.CinematicJourneyState.reset();',1)
if 'CinematicJourneyState.update(state.trueAirspeedMps' not in r:
    pat=r'(?m)^(\s*)if\(demoMode&&!localManual&&!btPilot&&!linkArmed\)com\.mg\.fixturecockpitsim\.sim\.AutonomousAttitudeGuard\.apply\(state,dt\);\s*$'
    m=re.search(pat,r)
    if not m:raise SystemExit('v108 anchor missing: autonomous attitude guard')
    indent=m.group(1)
    add=indent+'if(demoMode&&!localManual&&!btPilot&&!linkArmed)com.mg.fixturecockpitsim.sim.CinematicJourneyState.update(state.trueAirspeedMps,dt,state.onGround,mission.getPhase().name());'
    r=r[:m.end()]+'\n'+add+r[m.end():]
RUNTIME.write_text(r)

# Renderer imports.
j=JET.read_text()
j=rep(j,
'import com.mg.fixturecockpitsim.visual.WingtipVortexMesh;\n',
'import com.mg.fixturecockpitsim.visual.WingtipVortexMesh;\nimport com.mg.fixturecockpitsim.visual.CinematicTerrainMesh;\nimport com.mg.fixturecockpitsim.visual.AirframeUndersideSeal;\nimport com.mg.fixturecockpitsim.sim.CinematicJourneyState;\n',
'v108 renderer imports')

# Add independent declarations rather than assuming the old field list is exact.
if 'FloatBuffer undersideBuffer' not in j:
    m=re.search(r'(?m)^(\s*)FloatBuffer\s+[^;]*vbOpaque[^;]*;\s*$',j)
    if not m:raise SystemExit('v108 anchor missing: renderer FloatBuffer declaration')
    indent=m.group(1)
    extra='\n'+indent+'FloatBuffer undersideBuffer,terrain0,terrain1,terrain2,terrain3,terrain4;\n'+indent+'final float[] terrainModel=new float[16],terrainMvp=new float[16];'
    j=j[:m.end()]+extra+j[m.end():]
if 'int undersideCount' not in j:
    m=re.search(r'(?m)^(\s*)int\s+[^;]*opaqueCount[^;]*;\s*$',j)
    if not m:raise SystemExit('v108 anchor missing: renderer count declaration')
    indent=m.group(1)
    extra='\n'+indent+'int undersideCount,terrainCount0,terrainCount1,terrainCount2,terrainCount3,terrainCount4;'
    j=j[:m.end()]+extra+j[m.end():]

# Build the extra GPU meshes immediately before the renderer clock starts.
if 'AirframeUndersideSeal.build()' not in j:
    anchor='last=System.nanoTime();'
    if anchor not in j:raise SystemExit('v108 anchor missing: renderer initialization clock')
    build='float[] belly=AirframeUndersideSeal.build();undersideBuffer=buffer(belly);undersideCount=belly.length/7;float[] q0=CinematicTerrainMesh.build(0),q1=CinematicTerrainMesh.build(1),q2=CinematicTerrainMesh.build(2),q3=CinematicTerrainMesh.build(3),q4=CinematicTerrainMesh.build(4);terrain0=buffer(q0);terrain1=buffer(q1);terrain2=buffer(q2);terrain3=buffer(q3);terrain4=buffer(q4);terrainCount0=q0.length/7;terrainCount1=q1.length/7;terrainCount2=q2.length/7;terrainCount3=q3.length/7;terrainCount4=q4.length/7;'
    j=j.replace(anchor,build+anchor,1)

# Put the 3D terrain before the aircraft opaque draw, then close the belly after
# other opaque external geometry. This avoids dependency on a full draw line.
if 'drawJourneyTerrain();bindAndDraw(vbOpaque,opaqueCount);' not in j:
    anchor='GLES20.glDisable(GLES20.GL_BLEND);bindAndDraw(vbOpaque,opaqueCount);'
    if anchor not in j:raise SystemExit('v108 anchor missing: opaque aircraft draw')
    j=j.replace(anchor,'GLES20.glDisable(GLES20.GL_BLEND);GLES20.glDepthMask(true);drawJourneyTerrain();bindAndDraw(vbOpaque,opaqueCount);',1)
if 'bindAndDraw(undersideBuffer,undersideCount);' not in j:
    anchor='bindAndDraw(obOpaque,ordnanceCount);'
    if anchor not in j:raise SystemExit('v108 anchor missing: opaque external-store draw')
    j=j.replace(anchor,anchor+'GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(undersideBuffer,undersideCount);GLES20.glEnable(GLES20.GL_CULL_FACE);',1)

helper='''        private void drawJourneyTerrain(){
            int s=CinematicJourneyState.getStage();FloatBuffer b=null;int count=0;
            if(s==CinematicJourneyState.TOROS){b=terrain0;count=terrainCount0;}
            else if(s==CinematicJourneyState.AEGEAN){b=terrain1;count=terrainCount1;}
            else if(s==CinematicJourneyState.PATARA){b=terrain2;count=terrainCount2;}
            else if(s==CinematicJourneyState.KARAPINAR){b=terrain3;count=terrainCount3;}
            else if(s==CinematicJourneyState.MOONLIT){b=terrain4;count=terrainCount4;}
            else return;
            Matrix.setIdentityM(terrainModel,0);float q=CinematicJourneyState.getStageBlend01();Matrix.translateM(terrainModel,0,(float)Math.sin(Math.toRadians(yaw))*4.0f,0,(q-.5f)*11.0f);Matrix.multiplyMM(terrainMvp,0,vp,0,terrainModel,0);
            GLES20.glUniformMatrix4fv(umvp,1,false,terrainMvp,0);GLES20.glUniformMatrix4fv(umodel,1,false,terrainModel,0);GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(b,count);GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glUniformMatrix4fv(umvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(umodel,1,false,md,0);
        }

'''
if 'private void drawJourneyTerrain()' not in j:
    anchor='        private void bindAndDraw(FloatBuffer b,int vertices){'
    if anchor not in j:raise SystemExit('v108 anchor missing: generic draw helper')
    j=j.replace(anchor,helper+anchor,1)

# Opaque materials for actual 3D ground and the underside closure.
if 'vP>59.5&&vP<63.5' not in j:
    shader_anchor='"void main(){if(vP>39.5&&vP<40.5){'
    shader_repl='"void main(){if((vP>59.5&&vP<63.5)||(vP>64.5&&vP<65.5)){vec3 N=normalize(vN);float lit=.34+.66*max(dot(N,normalize(uLightDir)),0.);vec3 tc=vP<60.5?vec3(.26,.31,.25):(vP<61.5?vec3(.25,.34,.29):(vP<62.5?vec3(.62,.46,.25):(vP<63.5?vec3(.49,.37,.22):vec3(.10,.15,.21))));float fog=smoothstep(45.,210.,-vPos.z);tc=mix(tc,vec3(.48,.57,.61),fog*.72);gl_FragColor=vec4(tc*lit,1.);return;}if(vP>63.5&&vP<64.5){vec3 N=normalize(vN);float lit=.30+.70*max(dot(N,normalize(uLightDir)),0.);gl_FragColor=vec4(vec3(.205,.218,.230)*lit,1.);return;}if(vP>39.5&&vP<40.5){'
    if shader_anchor not in j:raise SystemExit('v108 anchor missing: shader main material start')
    j=j.replace(shader_anchor,shader_repl,1)
JET.write_text(j)

# Version.
g=GRADLE.read_text()
g=re.sub(r'versionCode\s+\d+','versionCode 108',g,count=1)
g=re.sub(r"versionName\s+['\"][^'\"]+['\"]","versionName '26.26-avm35.0-cinematic-turkiye-3d-world'",g,count=1)
GRADLE.write_text(g)
print('v108 applied: opaque underside seal + cinematic Türkiye journey + true OpenGL 3D mountain terrain')
