from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JET=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/Jet3DView.java'
RUNTIME=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/FlightRuntimeActivity.java'


def rep(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v89 fighter-controls patch anchor missing: {label}')
    return text.replace(old, new, 1)

# ---------------------------------------------------------------------------
# Renderer: use the actual FCS actuator states and add one dorsal speed brake.
# This patch intentionally runs AFTER all v87/v88 transformation scripts.
# ---------------------------------------------------------------------------
j=JET.read_text()
j=rep(j,
'import com.mg.fixturecockpitsim.visual.MechanicalDynamicsOverlay;\n',
'import com.mg.fixturecockpitsim.visual.MechanicalDynamicsOverlay;\nimport com.mg.fixturecockpitsim.visual.FighterSpeedBrakeOverlay;\n',
'speed brake import')

j=rep(j,
'    public void setControlInputs(float pitch,float roll,float yaw,float throttle){r.controls(pitch,roll,yaw,throttle);}\n',
'    public void setControlInputs(float pitch,float roll,float yaw,float throttle){r.controls(pitch,roll,yaw,throttle);}\n'
'    public void setFighterSurfaceState(float stabL,float stabR,float rudderL,float rudderR,float flapL,float flapR,float leL,float leR,float speedBrakeDeg){r.fighter(stabL,stabR,rudderL,rudderR,flapL,flapR,leL,leR,speedBrakeDeg);}\n',
'fighter surface public setter')

j=rep(j,
'        volatile float tr,tp,ty,thr=.6f,tg=1,tm,tn,ws,speed,vertical,tsl,tsr,trl,trr,tfl,tfr,tleL,tleR,tvec;\n',
'        volatile float tr,tp,ty,thr=.6f,tg=1,tm,tn,ws,speed,vertical,tsl,tsr,trl,trr,tfl,tfr,tleL,tleR,tsb,tvec;\n',
'speed brake target field')
j=rep(j,
'        float roll,pitch,yaw,gear=1,mc,nc,spin,aspect=1.7f,t,sl,sr,rl,rrd,fl,fr,leL,leR,vec,camX,camY,camZ;\n',
'        float roll,pitch,yaw,gear=1,mc,nc,spin,aspect=1.7f,t,sl,sr,rl,rrd,fl,fr,leL,leR,sbr,vec,camX,camY,camZ;\n',
'speed brake smoothed field')
j=rep(j,
'        int pg,ap,an,apart,umvp,umodel,uc,ul,ut,ug,um,un,uws,usl,usr,url,urr,ufl,ufr,ulel,uler,uvec,utime,ucam,uspeed,uair,uroll,ubird;\n',
'        int pg,ap,an,apart,umvp,umodel,uc,ul,ut,ug,um,un,uws,usl,usr,url,urr,ufl,ufr,ulel,uler,usb,uvec,utime,ucam,uspeed,uair,uroll,ubird;\n',
'speed brake uniform handle')
j=rep(j,
'        FloatBuffer vbOpaque,vbCanopy,detailBuffer,mechanicalBuffer,engineSolidBuffer,engineTransparentBuffer,obOpaque,obGlass,vortexBuffer,birdBuffer;\n',
'        FloatBuffer vbOpaque,vbCanopy,detailBuffer,mechanicalBuffer,speedBrakeBuffer,engineSolidBuffer,engineTransparentBuffer,obOpaque,obGlass,vortexBuffer,birdBuffer;\n',
'speed brake buffer')
j=rep(j,
'        int opaqueCount,canopyCount,detailCount,mechanicalCount,engineSolidCount,engineTransparentCount,ordnanceCount,glassCount,vortexCount,birdCount;\n',
'        int opaqueCount,canopyCount,detailCount,mechanicalCount,speedBrakeCount,engineSolidCount,engineTransparentCount,ordnanceCount,glassCount,vortexCount,birdCount;\n',
'speed brake count')

j=rep(j,
'        synchronized void controls(float p,float r,float y,float th){map.update(p,r,y,th);tsl=map.leftStabilatorDeg;tsr=map.rightStabilatorDeg;trl=map.leftRudderDeg;trr=map.rightRudderDeg;tfl=map.leftFlaperonDeg;tfr=map.rightFlaperonDeg;tleL=map.leftLeadingEdgeFlapDeg;tleR=map.rightLeadingEdgeFlapDeg;tvec=cl(p,-1,1)*8f;}\n',
'        synchronized void controls(float p,float r,float y,float th){map.update(p,r,y,th);tsl=map.leftStabilatorDeg;tsr=map.rightStabilatorDeg;trl=map.leftRudderDeg;trr=map.rightRudderDeg;tfl=map.leftFlaperonDeg;tfr=map.rightFlaperonDeg;tleL=map.leftLeadingEdgeFlapDeg;tleR=map.rightLeadingEdgeFlapDeg;tvec=cl(p,-1,1)*8f;}\n'
'        synchronized void fighter(float a,float b,float c,float d,float e,float f,float g,float h,float speedBrake){tsl=a;tsr=b;trl=c;trr=d;tfl=e;tfr=f;tleL=g;tleR=h;tsb=cl(speedBrake,0,45);}\n',
'actual FCS actuator setter')

j=rep(j,
'ufl=GLES20.glGetUniformLocation(pg,"uFlapL");ufr=GLES20.glGetUniformLocation(pg,"uFlapR");ulel=GLES20.glGetUniformLocation(pg,"uLeFlapL");uler=GLES20.glGetUniformLocation(pg,"uLeFlapR");uvec=GLES20.glGetUniformLocation(pg,"uVector");',
'ufl=GLES20.glGetUniformLocation(pg,"uFlapL");ufr=GLES20.glGetUniformLocation(pg,"uFlapR");ulel=GLES20.glGetUniformLocation(pg,"uLeFlapL");uler=GLES20.glGetUniformLocation(pg,"uLeFlapR");usb=GLES20.glGetUniformLocation(pg,"uSpeedBrake");uvec=GLES20.glGetUniformLocation(pg,"uVector");',
'speed brake uniform lookup')

j=rep(j,
'            float[] mech=MechanicalDynamicsOverlay.build();mechanicalBuffer=buffer(mech);mechanicalCount=mech.length/7;\n            float[] es=EngineDynamicsOverlay.buildSolid();',
'            float[] mech=MechanicalDynamicsOverlay.build();mechanicalBuffer=buffer(mech);mechanicalCount=mech.length/7;\n'
'            float[] speedBrakeMesh=FighterSpeedBrakeOverlay.build();speedBrakeBuffer=buffer(speedBrakeMesh);speedBrakeCount=speedBrakeMesh.length/7;\n'
'            float[] es=EngineDynamicsOverlay.buildSolid();',
'speed brake mesh construction')

j=rep(j,
'sl+=(tsl-sl)*ks;sr+=(tsr-sr)*ks;rl+=(trl-rl)*ks;rrd+=(trr-rrd)*ks;fl+=(tfl-fl)*ks;fr+=(tfr-fr)*ks;leL+=(tleL-leL)*ks;leR+=(tleR-leR)*ks;vec+=(tvec-vec)*ks;',
'sl+=(tsl-sl)*ks;sr+=(tsr-sr)*ks;rl+=(trl-rl)*ks;rrd+=(trr-rrd)*ks;fl+=(tfl-fl)*ks;fr+=(tfr-fr)*ks;leL+=(tleL-leL)*ks;leR+=(tleR-leR)*ks;sbr+=(tsb-sbr)*(1-(float)Math.exp(-dt*5.5f));vec+=(tvec-vec)*ks;',
'speed brake smoothing')

j=rep(j,
'GLES20.glUniform1f(ufl,fl);GLES20.glUniform1f(ufr,fr);GLES20.glUniform1f(ulel,leL);GLES20.glUniform1f(uler,leR);GLES20.glUniform1f(uvec,vec);',
'GLES20.glUniform1f(ufl,fl);GLES20.glUniform1f(ufr,fr);GLES20.glUniform1f(ulel,leL);GLES20.glUniform1f(uler,leR);GLES20.glUniform1f(usb,sbr);GLES20.glUniform1f(uvec,vec);',
'speed brake uniform upload')

j=rep(j,
'bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);bindAndDraw(mechanicalBuffer,mechanicalCount);bindAndDraw(engineSolidBuffer,engineSolidCount);',
'bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);bindAndDraw(mechanicalBuffer,mechanicalCount);bindAndDraw(speedBrakeBuffer,speedBrakeCount);bindAndDraw(engineSolidBuffer,engineSolidCount);',
'speed brake draw')

j=rep(j,
'uniform float uThrottle,uGear,uMainComp,uNoseComp,uWheelSpin,uStabL,uStabR,uRudderL,uRudderR,uFlapL,uFlapR,uLeFlapL,uLeFlapR,uVector,uTime,uBirdAge;',
'uniform float uThrottle,uGear,uMainComp,uNoseComp,uWheelSpin,uStabL,uStabR,uRudderL,uRudderR,uFlapL,uFlapR,uLeFlapL,uLeFlapR,uSpeedBrake,uVector,uTime,uBirdAge;',
'speed brake shader uniform')

le_shader='''                \"if(aPart>49.5&&aPart<51.5){float a=(aPart<50.5?uLeFlapL:uLeFlapR)*d;float hz=-2.65+.50*abs(p.x);vec2 piv=vec2(.31,hz);p.yz=rr(-a)*(p.yz-piv)+piv;n.yz=rr(-a)*n.yz;}\"+\n'''
sb_shader=le_shader+'''                \"if(aPart>51.5&&aPart<52.5){float a=uSpeedBrake*d;vec2 piv=vec2(.92,.55);p.yz=rr(-a)*(p.yz-piv)+piv;n.yz=rr(-a)*n.yz;}\"+\n'''
j=rep(j,le_shader,sb_shader,'speed brake shader transform')
JET.write_text(j)

# ---------------------------------------------------------------------------
# Runtime controls: one physical button is context-sensitive.
# On the ground it is wheel BRAKE; airborne it commands the dorsal SPD BRK.
# ---------------------------------------------------------------------------
r=RUNTIME.read_text()
r=rep(r,
'    private double localThrottle=.10,localBrake,localYawHold;\n',
'    private double localThrottle=.10,localBrake,localSpeedBrake,localYawHold;\n',
'local speed brake state')

r=rep(r,
'        brakeButton.setOnTouchListener((v,e)->{if(!localManual)return false;if(e.getAction()==MotionEvent.ACTION_DOWN){localBrake=1;updateButtons();return true;}if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){localBrake=0;updateButtons();return true;}return true;});\n',
'        brakeButton.setOnTouchListener((v,e)->{if(!localManual)return false;if(e.getAction()==MotionEvent.ACTION_DOWN){if(state.onGround)localBrake=1;else localSpeedBrake=1;updateButtons();return true;}if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){localBrake=0;localSpeedBrake=0;updateButtons();return true;}return true;});\n',
'context-sensitive brake button')

r=rep(r,
'            autoRecovery=false;autoRecoveryStableSec=0;seedFreeNavigation();localThrottle=Math.max(.08,state.throttle);localGearDown=state.gearPosition>.5;localBrake=0;localYawHold=0;\n',
'            autoRecovery=false;autoRecoveryStableSec=0;seedFreeNavigation();localThrottle=Math.max(.08,state.throttle);localGearDown=state.gearPosition>.5;localBrake=0;localSpeedBrake=0;localYawHold=0;\n',
'manual entry reset')
r=rep(r,
'            localBrake=0;localYawHold=0;imuRoll=imuPitch=imuYaw=0;seedFreeNavigation();autoRecovery=true;autoRecoveryStableSec=0;\n',
'            localBrake=0;localSpeedBrake=0;localYawHold=0;imuRoll=imuPitch=imuYaw=0;seedFreeNavigation();autoRecovery=true;autoRecoveryStableSec=0;\n',
'manual exit reset')
r=rep(r,
'        if(localManual){localManual=false;localBrake=0;localYawHold=0;}\n',
'        if(localManual){localManual=false;localBrake=0;localSpeedBrake=0;localYawHold=0;}\n',
'link reset')

r=rep(r,
'        boolean wasGround=state.onGround;\n        boolean btPilot=remoteTakeover&&connected&&linkArmed;\n',
'        boolean wasGround=state.onGround;\n        boolean btPilot=remoteTakeover&&connected&&linkArmed;\n        controls.speedBrake=0; // every mode must explicitly request airborne speed brake\n',
'default speed brake reset')

r=rep(r,
'        if(btPilot){controls.roll=remoteRoll;controls.pitch=remotePitch;controls.yaw=remoteYaw;controls.throttle=remoteThrottle;controls.brake=remoteBrake;controls.gearDown=remoteGearDown;controls.clamp();}\n',
'        if(btPilot){controls.roll=remoteRoll;controls.pitch=remotePitch;controls.yaw=remoteYaw;controls.throttle=remoteThrottle;controls.brake=state.onGround?remoteBrake:0;controls.speedBrake=state.onGround?0:remoteBrake;controls.gearDown=remoteGearDown;controls.clamp();}\n',
'BT brake/speed brake split')

r=rep(r,
'            controls.throttle=localThrottle;controls.brake=localBrake;controls.gearDown=localGearDown;\n',
'            controls.throttle=localThrottle;controls.brake=state.onGround?localBrake:0;controls.speedBrake=state.onGround?0:localSpeedBrake;controls.gearDown=localGearDown;\n',
'local brake/speed brake split')

r=rep(r,
'        jet.setTelemetry((float)state.rollDeg,(float)state.pitchDeg,(float)state.headingDeg,(float)state.throttle,50,0,true);jet.setControlInputs((float)controls.pitch,(float)controls.roll,(float)controls.yaw,(float)controls.throttle);jet.setSimulationState((float)state.gearPosition,(float)state.mainStrutCompression01,(float)state.noseStrutCompression01,(float)state.brake01,state.onGround);',
'        jet.setTelemetry((float)state.rollDeg,(float)state.pitchDeg,(float)state.headingDeg,(float)state.throttle,50,0,true);jet.setControlInputs((float)controls.pitch,(float)controls.roll,(float)controls.yaw,(float)controls.throttle);jet.setFighterSurfaceState((float)state.leftStabilatorDeg,(float)state.rightStabilatorDeg,(float)state.leftRudderDeg,(float)state.rightRudderDeg,(float)state.leftFlaperonDeg,(float)state.rightFlaperonDeg,(float)state.leftLeadingEdgeFlapDeg,(float)state.rightLeadingEdgeFlapDeg,(float)state.speedBrakeDeg);jet.setSimulationState((float)state.gearPosition,(float)state.mainStrutCompression01,(float)state.noseStrutCompression01,(float)state.brake01,state.onGround);',
'render actual FCS surfaces')

r=rep(r,
'    private void crash(String reason){if(crashed)return;crashed=true;crashReason=reason;crashRollTarget=state.rollDeg>=0?34:-34;remoteThrottle=0;remoteBrake=1;localThrottle=0;localBrake=1;',
'    private void crash(String reason){if(crashed)return;crashed=true;crashReason=reason;crashRollTarget=state.rollDeg>=0?34:-34;remoteThrottle=0;remoteBrake=1;localThrottle=0;localBrake=1;localSpeedBrake=0;',
'crash speed brake reset')
r=rep(r,
'localManual=false;localThrottle=.10;localBrake=0;localYawHold=0;localGearDown=true;',
'localManual=false;localThrottle=.10;localBrake=0;localSpeedBrake=0;localYawHold=0;localGearDown=true;',
'reset speed brake')

r=rep(r,
'brakeButton.setText(localBrake>.5?(state.onGround?"BRAKE ●":"AIR BRK ●"):(state.onGround?"BRAKE":"AIR BRK"));',
'brakeButton.setText(state.onGround?(localBrake>.5?"BRAKE ●":"BRAKE"):(localSpeedBrake>.5?"SPD BRK ●":"SPD BRK"));',
'speed brake button label')
RUNTIME.write_text(r)

print('v89 fighter control surfaces applied: FCS actuator rendering, automatic LE flaps, differential tail/flaperons, twin rudders, autotrim/yaw damper and dorsal speed brake')
