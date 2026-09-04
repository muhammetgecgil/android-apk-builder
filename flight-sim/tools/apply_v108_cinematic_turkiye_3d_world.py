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

# ---------------------------------------------------------------------------
# Runtime: add route-specific environment overlay and advance the cinematic
# journey from real simulation time. The aircraft still starts on the runway.
# ---------------------------------------------------------------------------
r=RUNTIME.read_text()
r=rep(r,
'        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);\n        root.addView(world,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));',
'        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);CinematicJourneyOverlayView journey=new CinematicJourneyOverlayView(this);\n        root.addView(world,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(journey,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));',
'journey overlay layer')
r=rep(r,
'        mission.reset(state);controls.gearDown=true;hangarDeparted=true;',
'        mission.reset(state);com.mg.fixturecockpitsim.sim.CinematicJourneyState.reset();controls.gearDown=true;hangarDeparted=true;',
'journey initial reset')
r=rep(r,
'        if(demoMode&&!localManual&&!btPilot&&!linkArmed)com.mg.fixturecockpitsim.sim.AutonomousAttitudeGuard.apply(state,dt);\n        if(freeNavSeeded)updateRunwayPosition(dt);',
'        if(demoMode&&!localManual&&!btPilot&&!linkArmed)com.mg.fixturecockpitsim.sim.AutonomousAttitudeGuard.apply(state,dt);\n        if(demoMode&&!localManual&&!btPilot&&!linkArmed)com.mg.fixturecockpitsim.sim.CinematicJourneyState.update(state.trueAirspeedMps,dt,state.onGround,mission.getPhase().name());\n        if(freeNavSeeded)updateRunwayPosition(dt);',
'journey state update')
RUNTIME.write_text(r)

# ---------------------------------------------------------------------------
# Jet renderer: real OpenGL 3D terrain is drawn in the same depth buffer as the
# aircraft. An opaque belly/wing-root closure mesh removes remaining underside
# see-through holes. All opaque geometry writes depth before transparent FX.
# ---------------------------------------------------------------------------
j=JET.read_text()
j=rep(j,
'import com.mg.fixturecockpitsim.visual.WingtipVortexMesh;\n',
'import com.mg.fixturecockpitsim.visual.WingtipVortexMesh;\nimport com.mg.fixturecockpitsim.visual.CinematicTerrainMesh;\nimport com.mg.fixturecockpitsim.visual.AirframeUndersideSeal;\nimport com.mg.fixturecockpitsim.sim.CinematicJourneyState;\n',
'v108 renderer imports')
j=rep(j,
'        FloatBuffer vbOpaque,vbCanopy,detailBuffer,engineSolidBuffer,engineTransparentBuffer,obOpaque,obGlass,vortexBuffer,birdBuffer;',
'        FloatBuffer vbOpaque,vbCanopy,detailBuffer,engineSolidBuffer,engineTransparentBuffer,obOpaque,obGlass,vortexBuffer,birdBuffer,undersideBuffer,terrain0,terrain1,terrain2,terrain3,terrain4;\n        final float[] terrainModel=new float[16],terrainMvp=new float[16];',
'v108 buffers')
j=rep(j,
'        int opaqueCount,canopyCount,detailCount,engineSolidCount,engineTransparentCount,ordnanceCount,glassCount,vortexCount,birdCount;',
'        int opaqueCount,canopyCount,detailCount,engineSolidCount,engineTransparentCount,ordnanceCount,glassCount,vortexCount,birdCount,undersideCount,terrainCount0,terrainCount1,terrainCount2,terrainCount3,terrainCount4;',
'v108 counts')
# Append mesh builds directly before renderer clock initialization.
anchor='float[] vortex=WingtipVortexMesh.build();vortexBuffer=buffer(vortex);vortexCount=vortex.length/7;float[] bird=buildBirdMesh();birdBuffer=buffer(bird);birdCount=bird.length/7;last=System.nanoTime();'
replacement='float[] vortex=WingtipVortexMesh.build();vortexBuffer=buffer(vortex);vortexCount=vortex.length/7;float[] bird=buildBirdMesh();birdBuffer=buffer(bird);birdCount=bird.length/7;float[] belly=AirframeUndersideSeal.build();undersideBuffer=buffer(belly);undersideCount=belly.length/7;float[] q0=CinematicTerrainMesh.build(0),q1=CinematicTerrainMesh.build(1),q2=CinematicTerrainMesh.build(2),q3=CinematicTerrainMesh.build(3),q4=CinematicTerrainMesh.build(4);terrain0=buffer(q0);terrain1=buffer(q1);terrain2=buffer(q2);terrain3=buffer(q3);terrain4=buffer(q4);terrainCount0=q0.length/7;terrainCount1=q1.length/7;terrainCount2=q2.length/7;terrainCount3=q3.length/7;terrainCount4=q4.length/7;last=System.nanoTime();'
j=rep(j,anchor,replacement,'terrain and belly mesh build')

# Opaque pass first: terrain, airframe, underside closure. Transparent effects
# remain separate and never punch holes through the external fuselage.
oldpass='GLES20.glDisable(GLES20.GL_BLEND);bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);bindAndDraw(engineSolidBuffer,engineSolidCount);bindAndDraw(obOpaque,ordnanceCount);bindAndDraw(vbCanopy,canopyCount);if(birdAge>=0f&&birdAge<5.8f)bindAndDraw(birdBuffer,birdCount);'
newpass='GLES20.glDisable(GLES20.GL_BLEND);GLES20.glDepthMask(true);drawJourneyTerrain();bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);bindAndDraw(engineSolidBuffer,engineSolidCount);bindAndDraw(obOpaque,ordnanceCount);GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(undersideBuffer,undersideCount);GLES20.glEnable(GLES20.GL_CULL_FACE);bindAndDraw(vbCanopy,canopyCount);if(birdAge>=0f&&birdAge<5.8f)bindAndDraw(birdBuffer,birdCount);'
j=rep(j,oldpass,newpass,'opaque terrain and underside pass')

# Add terrain draw helper immediately before the generic vertex-buffer draw.
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
j=rep(j,
'        private void bindAndDraw(FloatBuffer b,int vertices){',
helper+'        private void bindAndDraw(FloatBuffer b,int vertices){',
'3D terrain draw helper')

# GPU materials for 3D terrain and the opaque underside closure. Terrain colors
# use lighting + distance haze; the belly always returns alpha=1.
shader_anchor='"void main(){if(vP>39.5&&vP<40.5){'
shader_repl='"void main(){if((vP>59.5&&vP<63.5)||(vP>64.5&&vP<65.5)){vec3 N=normalize(vN);float lit=.34+.66*max(dot(N,normalize(uLightDir)),0.);vec3 tc=vP<60.5?vec3(.26,.31,.25):(vP<61.5?vec3(.25,.34,.29):(vP<62.5?vec3(.62,.46,.25):(vP<63.5?vec3(.49,.37,.22):vec3(.10,.15,.21))));float fog=smoothstep(45.,210.,-vPos.z);tc=mix(tc,vec3(.48,.57,.61),fog*.72);gl_FragColor=vec4(tc*lit,1.);return;}if(vP>63.5&&vP<64.5){vec3 N=normalize(vN);float lit=.30+.70*max(dot(N,normalize(uLightDir)),0.);gl_FragColor=vec4(vec3(.205,.218,.230)*lit,1.);return;}if(vP>39.5&&vP<40.5){'
j=rep(j,shader_anchor,shader_repl,'terrain and underside shader')
JET.write_text(j)

# Version.
g=GRADLE.read_text()
g=re.sub(r'versionCode\s+\d+','versionCode 108',g,count=1)
g=re.sub(r"versionName\s+['\"][^'\"]+['\"]","versionName '26.26-avm35.0-cinematic-turkiye-3d-world'",g,count=1)
GRADLE.write_text(g)
print('v108 applied: opaque underside seal + cinematic Türkiye journey + true OpenGL 3D mountain terrain')
