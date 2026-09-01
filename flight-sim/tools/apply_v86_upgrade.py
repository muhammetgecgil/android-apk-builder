from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JET=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/Jet3DView.java'
RUNTIME=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/FlightRuntimeActivity.java'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v86 patch anchor missing: {label}')
    return text.replace(old,new,1)

j=JET.read_text()
j=rep(j,
'import com.mg.fixturecockpitsim.visual.EngineDynamicsOverlay;\n',
'import com.mg.fixturecockpitsim.visual.EngineDynamicsOverlay;\nimport com.mg.fixturecockpitsim.visual.DynamicControlSurfaceOverlay;\n','control import')
j=rep(j,
'/** AVM-16.1 premium renderer: animated fan/nozzle/afterburner, heat haze, bird and water impact. */',
'/** AVM-17.0 renderer: full control surfaces, clean retracting gear, attached stores and engine dynamics. */','renderer version')
j=rep(j,
'public void setSimulationState(float gear,float mainComp,float noseComp,float brake,boolean onGround){r.sim(gear,mainComp,noseComp);sg=gear;sb=brake;ground=onGround;sound.update(st,st*230,sg,sb,ground);}',
'public void setSimulationState(float gear,float mainComp,float noseComp,float brake,boolean onGround){r.sim(gear,mainComp,noseComp,brake);sg=gear;sb=brake;ground=onGround;sound.update(st,st*230,sg,sb,ground);}','brake telemetry')
j=rep(j,
'volatile float tr,tp,ty,thr=.6f,tg=1,tm,tn,ws,speed,vertical,tsl,tsr,trl,trr,tfl,tfr,tvec;',
'volatile float tr,tp,ty,thr=.6f,tg=1,tm,tn,tbrake,ws,speed,vertical,tsl,tsr,trl,trr,tfl,tfr,tvec;','brake target')
j=rep(j,
'float roll,pitch,yaw,gear=1,mc,nc,spin,aspect=1.7f,t,sl,sr,rl,rrd,fl,fr,vec,camX,camY,camZ;',
'float roll,pitch,yaw,gear=1,mc,nc,brake,spin,aspect=1.7f,t,sl,sr,rl,rrd,fl,fr,vec,camX,camY,camZ;','brake state')
j=rep(j,
'int pg,ap,an,apart,umvp,umodel,uc,ul,ut,ug,um,un,uws,usl,usr,url,urr,ufl,ufr,uvec,utime,ucam,uspeed,uair,uroll,ubird;',
'int pg,ap,an,apart,umvp,umodel,uc,ul,ut,ug,um,un,ubrake,uws,usl,usr,url,urr,ufl,ufr,uvec,utime,ucam,uspeed,uair,uroll,ubird;','brake uniform handle')
j=rep(j,
'FloatBuffer vbOpaque,vbCanopy,detailBuffer,engineSolidBuffer,engineTransparentBuffer,obOpaque,obGlass,vortexBuffer,birdBuffer;',
'FloatBuffer vbOpaque,vbCanopy,detailBuffer,controlBuffer,engineSolidBuffer,engineTransparentBuffer,obOpaque,obGlass,vortexBuffer,birdBuffer;','control buffer')
j=rep(j,
'int opaqueCount,canopyCount,detailCount,engineSolidCount,engineTransparentCount,ordnanceCount,glassCount,vortexCount,birdCount;',
'int opaqueCount,canopyCount,detailCount,controlCount,engineSolidCount,engineTransparentCount,ordnanceCount,glassCount,vortexCount,birdCount;','control count')
j=rep(j,
'void sim(float g,float m,float n){tg=cl(g,0,1);tm=cl(m,0,1);tn=cl(n,0,1);}',
'void sim(float g,float m,float n,float b){tg=cl(g,0,1);tm=cl(m,0,1);tn=cl(n,0,1);tbrake=cl(b,0,1);}','sim brake')
j=rep(j,
'um=GLES20.glGetUniformLocation(pg,"uMainComp");un=GLES20.glGetUniformLocation(pg,"uNoseComp");uws=',
'um=GLES20.glGetUniformLocation(pg,"uMainComp");un=GLES20.glGetUniformLocation(pg,"uNoseComp");ubrake=GLES20.glGetUniformLocation(pg,"uBrake");uws=','uniform location')
j=rep(j,
'float[] detail=AdvancedAirframeOverlay.build();detailBuffer=buffer(detail);detailCount=detail.length/7;\n            float[] es=EngineDynamicsOverlay.buildSolid();',
'float[] detail=AdvancedAirframeOverlay.build();detailBuffer=buffer(detail);detailCount=detail.length/7;\n            float[] ctrl=DynamicControlSurfaceOverlay.build();controlBuffer=buffer(ctrl);controlCount=ctrl.length/7;\n            float[] es=EngineDynamicsOverlay.buildSolid();','control mesh build')
j=rep(j,
'float k=1-(float)Math.exp(-dt*8),kg=1-(float)Math.exp(-dt*2.2),ks=1-(float)Math.exp(-dt*11);roll+=(tr-roll)*k;pitch+=(tp-pitch)*k;yaw+=shortest(ty-yaw)*k*.65f;gear+=(tg-gear)*kg;mc+=(tm-mc)*kg;nc+=(tn-nc)*kg;sl+=',
'float k=1-(float)Math.exp(-dt*8),kg=1-(float)Math.exp(-dt*2.2),ks=1-(float)Math.exp(-dt*11);roll+=(tr-roll)*k;pitch+=(tp-pitch)*k;yaw+=shortest(ty-yaw)*k*.65f;gear+=(tg-gear)*kg;mc+=(tm-mc)*kg;nc+=(tn-nc)*kg;brake+=(tbrake-brake)*ks;sl+=','brake smoothing')
j=rep(j,
'GLES20.glUniform1f(ug,gear);GLES20.glUniform1f(um,mc);GLES20.glUniform1f(un,nc);GLES20.glUniform1f(uws,spin);',
'GLES20.glUniform1f(ug,gear);GLES20.glUniform1f(um,mc);GLES20.glUniform1f(un,nc);GLES20.glUniform1f(ubrake,brake);GLES20.glUniform1f(uws,spin);','brake uniform upload')
j=rep(j,
'bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);bindAndDraw(engineSolidBuffer,engineSolidCount);bindAndDraw(obOpaque,ordnanceCount);',
'bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);bindAndDraw(controlBuffer,controlCount);bindAndDraw(engineSolidBuffer,engineSolidCount);bindAndDraw(obOpaque,ordnanceCount);','draw controls')
j=rep(j,
'uniform mat4 uMvp,uModel;uniform float uThrottle,uGear,uMainComp,uNoseComp,uWheelSpin,uStabL,uStabR,uRudderL,uRudderR,uFlapL,uFlapR,uVector,uTime,uBirdAge;',
'uniform mat4 uMvp,uModel;uniform float uThrottle,uGear,uMainComp,uNoseComp,uBrake,uWheelSpin,uStabL,uStabR,uRudderL,uRudderR,uFlapL,uFlapR,uVector,uTime,uBirdAge;','VS brake uniform')
# Insert dynamic LE flaps, dorsal speed brake and corrected store attachment just before nozzle heat-shield animation.
j=rep(j,
'"if(aPart>27.5&&aPart<28.5){float op=mix(.93,1.08,smoothstep(.18,1.,uThrottle));',
'"if(aPart>49.5&&aPart<51.5){float low=1.-smoothstep(58.,145.,uSpeed);float dep=max(low,uGear*.72);float ax=abs(p.x);float hz=-2.58+(ax-1.02)*.58;float a=-dep*18.*d;vec2 piv=vec2(.22,hz);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}"+\n                "if(aPart>51.5&&aPart<52.5){float a=uBrake*52.*d;vec2 piv=vec2(.90,.61);p.yz=rr(-a)*(p.yz-piv)+piv;n.yz=rr(-a)*n.yz;}"+\n                "if(aPart>29.5&&aPart<32.5){float lift=mix(.27,.43,smoothstep(1.05,1.55,abs(p.x)));p.y+=lift;}"+\n                "if(aPart>27.5&&aPart<28.5){float op=mix(.93,1.08,smoothstep(.18,1.,uThrottle));','dynamic surface shader')
# Fragment shader needs gear state to fully hide retracting door/gear detail leftovers.
j=rep(j,
'uniform vec4 uColor;uniform vec3 uLightDir,uCameraPos;uniform float uThrottle,uTime,uSpeed,uAirborne,uRollDeg,uBirdAge;',
'uniform vec4 uColor;uniform vec3 uLightDir,uCameraPos;uniform float uThrottle,uGear,uTime,uSpeed,uAirborne,uRollDeg,uBirdAge;','FS gear uniform')
j=rep(j,
'if(((vP>12.5&&vP<14.5)||(vP>23.5&&vP<24.5)))alpha*=1.-smoothstep(.78,.94,vR);',
'if(((vP>12.5&&vP<15.5)||(vP>23.5&&vP<24.5)))alpha*=1.-smoothstep(.78,.94,vR);if(vP>28.5&&vP<29.5&&vPos.y<-.88)alpha*=smoothstep(.03,.34,uGear);','gear residue fade')
JET.write_text(j)

r=RUNTIME.read_text()
r=rep(r,
'else if(demoMode){mission.update(state,controls,dt);hangarDeparted=true;}',
'else if(demoMode){mission.update(state,controls,dt);hangarDeparted=true;if(isDemoLandingPhase())applyDemoRunwayFinalCapture(dt);}','demo final capture hook')
method='''    private boolean isDemoLandingPhase(){\n        AutonomousFlightMission.Phase p=mission.getPhase();\n        return p==AutonomousFlightMission.Phase.APPROACH||p==AutonomousFlightMission.Phase.FLARE||p==AutonomousFlightMission.Phase.ROLLOUT||p==AutonomousFlightMission.Phase.TAXI_IN;\n    }\n\n    private void applyDemoRunwayFinalCapture(double dt){\n        seedFreeNavigation();\n        double rate=state.altitudeM<180?2.45:1.35;\n        double blend=1.0-Math.exp(-dt*rate);\n        runwayCrossTrackM+=(0-runwayCrossTrackM)*blend;\n        double correction=clampd(runwayCrossTrackM*.62,-42,42);\n        double target=RUNWAY_HDG-correction;\n        double err=wrap180(target-state.headingDeg);\n        controls.roll=clampd(controls.roll+err/52.0-runwayCrossTrackM/1700.0,-.34,.34);\n        controls.yaw=clampd(controls.yaw+err*.026-runwayCrossTrackM*.0018,-.55,.55);\n        controls.gearDown=true;\n        // Never descend through the ground away from the runway centreline.\n        if(state.altitudeM<145&&Math.abs(runwayCrossTrackM)>85){\n            controls.pitch=Math.max(controls.pitch,.045);\n            controls.throttle=Math.max(controls.throttle,.30);\n        }\n        if(state.altitudeM<22&&Math.abs(runwayCrossTrackM)<42)runwayCrossTrackM*=Math.exp(-dt*4.5);\n        controls.clamp();\n    }\n\n'''
r=rep(r,'    private void applyAutoRunwayRecovery(double dt){\n',method+'    private void applyAutoRunwayRecovery(double dt){\n','insert demo runway director')
r=rep(r,
'        boolean takeoff=ph==AutonomousFlightMission.Phase.TAXI_OUT||ph==AutonomousFlightMission.Phase.RUNWAY_HOLD||ph==AutonomousFlightMission.Phase.TAKEOFF_ROLL||ph==AutonomousFlightMission.Phase.ROTATE_CLIMB;\n\n        controls.brake=0;',
'        boolean takeoff=ph==AutonomousFlightMission.Phase.TAXI_OUT||ph==AutonomousFlightMission.Phase.RUNWAY_HOLD||ph==AutonomousFlightMission.Phase.TAKEOFF_ROLL||ph==AutonomousFlightMission.Phase.ROTATE_CLIMB;\n        if(landing){double b=1.0-Math.exp(-dt*(state.altitudeM<180?2.35:1.25));runwayCrossTrackM+=(0-runwayCrossTrackM)*b;cross=runwayCrossTrackM;correction=clampd(cross*.62,-42,42);targetHeading=RUNWAY_HDG-correction;headingErr=wrap180(targetHeading-state.headingDeg);}\n\n        controls.brake=0;','recovery cross-track capture')
r=rep(r,
'if(landing){controls.gearDown=true;controls.pitch=state.altitudeM<40?.045:-.025;controls.throttle=state.altitudeM<55?.34:.42;}',
'if(landing){controls.gearDown=true;if(state.altitudeM<145&&Math.abs(cross)>85){controls.pitch=.055;controls.throttle=.34;}else{controls.pitch=state.altitudeM<40?.045:-.025;controls.throttle=state.altitudeM<55?.34:.42;}}','prevent off-runway touchdown')
RUNTIME.write_text(r)

print('v86 source upgrade applied')
