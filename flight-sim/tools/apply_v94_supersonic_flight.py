from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
STATE=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/sim/FlightState.java'
DYN=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/sim/FlightDynamicsEngine.java'
RUNTIME=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/FlightRuntimeActivity.java'
JET=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/Jet3DView.java'
SOUND=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/FlightSoundEngine.java'
SMODEL=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/sim/FighterSoundModel.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v94 supersonic patch anchor missing: {label}')
    return text.replace(old,new,1)

# FlightState publishes the Mach-dependent state so physics, HUD, effects and audio
# all consume the same values.
s=STATE.read_text()
s=rep(s,
'    public double loadFactor = 1.0;\n',
'    public double loadFactor = 1.0;\n\n    // AVM-23 compressibility / supersonic state.\n    public double speedOfSoundMps = 340.3;\n    public double mach;\n    public double waveDrag01;\n    public double transonicBuffet01;\n    public double shockStrength01;\n    public double sonicBoomPulse;\n',
'supersonic state fields')
s=rep(s,
'        c.angleOfAttackDeg=angleOfAttackDeg; c.loadFactor=loadFactor;\n',
'        c.angleOfAttackDeg=angleOfAttackDeg; c.loadFactor=loadFactor;\n        c.speedOfSoundMps=speedOfSoundMps; c.mach=mach; c.waveDrag01=waveDrag01;\n        c.transonicBuffet01=transonicBuffet01; c.shockStrength01=shockStrength01; c.sonicBoomPulse=sonicBoomPulse;\n',
'supersonic copy state')
STATE.write_text(s)

# Integrate afterburner acceleration, wave drag and buffet into actual speed dynamics.
d=DYN.read_text()
d=rep(d,
'    private final FighterFlightControlSystem fighterFcs = new FighterFlightControlSystem();\n',
'    private final FighterFlightControlSystem fighterFcs = new FighterFlightControlSystem();\n    private final SupersonicFlightModel supersonic = new SupersonicFlightModel();\n',
'supersonic model field')
d=rep(d,
'        final double speedBrake01 = clamp01(s.speedBrake01);\n\n        if (s.onGround) {\n',
'        final double speedBrake01 = clamp01(s.speedBrake01);\n        final double previousMach = s.mach;\n\n        if (s.onGround) {\n',
'previous mach edge state')
old_air='''        } else {\n            double gearDrag = 18.0 * s.gearPosition;\n            double speedBrakeDrag = 78.0 * speedBrake01;\n            double highLiftDrag = 8.5 * le01;\n            double targetSpeed = Math.max(0.0, 55.0 + s.throttle * 250.0 - gearDrag - speedBrakeDrag - highLiftDrag);\n            s.trueAirspeedMps += (targetSpeed - s.trueAirspeedMps) * Math.min(1.0, dtSec * .48);\n            if (speedBrake01 > .05) s.trueAirspeedMps = Math.max(0.0, s.trueAirspeedMps - (1.0 + 5.8 * speedBrake01) * dtSec);\n        }\n\n        double uprightLift'''
new_air='''        } else {\n            SupersonicFlightModel.Output sup = supersonic.evaluate(s.altitudeM,s.trueAirspeedMps,s.throttle,s.gearPosition,speedBrake01,le01);\n            s.trueAirspeedMps += (sup.targetSpeedMps - s.trueAirspeedMps) * Math.min(1.0, dtSec * sup.speedResponsePerSec);\n            if (speedBrake01 > .05) s.trueAirspeedMps = Math.max(0.0, s.trueAirspeedMps - (1.0 + 7.2 * speedBrake01) * dtSec);\n        }\n\n        SupersonicFlightModel.Output supNow=supersonic.evaluate(s.altitudeM,s.trueAirspeedMps,s.throttle,s.gearPosition,speedBrake01,le01);\n        s.speedOfSoundMps=supNow.speedOfSoundMps;\n        s.mach=supNow.mach;\n        s.waveDrag01=supNow.waveDrag01;\n        s.transonicBuffet01=supNow.transonicBuffet01;\n        s.shockStrength01=supNow.shockStrength01;\n        s.sonicBoomPulse=Math.max(0.0,s.sonicBoomPulse-dtSec*1.30);\n        if(!s.onGround && s.timeSec>.5 && SupersonicFlightModel.crossedMachOne(previousMach,s.mach))s.sonicBoomPulse=1.0;\n        if(!s.onGround && s.transonicBuffet01>.01){\n            double b=s.transonicBuffet01;\n            s.rollDeg += (Math.sin(s.timeSec*19.1)+.45*Math.sin(s.timeSec*31.7))*b*dtSec*2.8;\n            s.pitchDeg += (Math.sin(s.timeSec*23.4+.8)+.35*Math.sin(s.timeSec*37.2))*b*dtSec*1.9;\n        }\n\n        double uprightLift'''
d=rep(d,old_air,new_air,'Mach-dependent airborne speed block')
DYN.write_text(d)

# Runtime: add visual overlay, feed real altitude/surface state to sound, display Mach,
# and expose a fifth low observer camera used for physically sensible sonic-boom playback.
r=RUNTIME.read_text()
r=rep(r,
'    private AirfieldWorldView world;\n    private WeatherEffectsView weather;\n    private Jet3DView jet;\n',
'    private AirfieldWorldView world;\n    private WeatherEffectsView weather;\n    private Jet3DView jet;\n    private SupersonicEffectsView supersonicFx;\n',
'supersonic overlay field')
r=rep(r,
'        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);\n        root.addView(world,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));\n',
'        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);supersonicFx=new SupersonicEffectsView(this);\n        root.addView(world,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(supersonicFx,new FrameLayout.LayoutParams(-1,-1));\n',
'supersonic overlay layer')
r=rep(r,
'        sound.update(state.throttle,state.trueAirspeedMps,state.altitudeM,state.gearPosition,state.brake01,state.onGround,state.leftStabilatorDeg,state.rightStabilatorDeg,state.leftRudderDeg,state.rightRudderDeg,state.leftFlaperonDeg,state.rightFlaperonDeg,state.leftLeadingEdgeFlapDeg,state.rightLeadingEdgeFlapDeg,state.speedBrakeDeg,false);\n',
'        sound.update(state.throttle,state.trueAirspeedMps,state.altitudeM,state.gearPosition,state.brake01,state.onGround,state.leftStabilatorDeg,state.rightStabilatorDeg,state.leftRudderDeg,state.rightRudderDeg,state.leftFlaperonDeg,state.rightFlaperonDeg,state.leftLeadingEdgeFlapDeg,state.rightLeadingEdgeFlapDeg,state.speedBrakeDeg,false);\n        sound.setSupersonicState(state.mach,state.sonicBoomPulse,jet!=null&&jet.getCameraMode()==Jet3DView.CAMERA_GROUND_OBSERVER);\n',
'supersonic audio state')
r=rep(r,
'cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%4;jet.setCameraMode(cameraMode);});',
'cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%5;jet.setCameraMode(cameraMode);});',
'five camera modes')
r=rep(r,
'jet.setWheelSpeed((float)(state.onGround?state.trueAirspeedMps:0));\n',
'jet.setWheelSpeed((float)(state.onGround?state.trueAirspeedMps:0));jet.setSupersonicState((float)state.mach,(float)state.transonicBuffet01,(float)state.sonicBoomPulse);if(supersonicFx!=null)supersonicFx.setState(state.mach,state.transonicBuffet01,state.shockStrength01,state.sonicBoomPulse,state.rollDeg,state.pitchDeg);\n',
'render Mach state')
r=rep(r,
'state.onGround?"GROUND":"AIR",landingCue()));updateButtons();',
'state.onGround?"GROUND":"AIR",landingCue()));hud.append(String.format(Locale.US,"\\nMACH %.2f   WAVE DRAG %.0f%%   BUFFET %.0f%%   %s",state.mach,state.waveDrag01*100,state.transonicBuffet01*100,supersonicLabel()));updateButtons();',
'Mach HUD line')
r=rep(r,
'    private void updateButtons(){if(modeButton==null)return;',
'    private String supersonicLabel(){if(state.mach<.78)return "SUBSONIC";if(state.mach<1.0)return "TRANSONIC";if(state.mach<1.20)return "MACH 1+";return "SUPERSONIC";}\n\n    private void updateButtons(){if(modeButton==null)return;',
'supersonic status label')
RUNTIME.write_text(r)

# 3D camera: transonic buffet, Mach-dependent FOV, boom kick and low observer view.
j=JET.read_text()
j=rep(j,
'    public static final int CAMERA_CHASE=0, CAMERA_REAR=1, CAMERA_RIGHT_QUARTER=2, CAMERA_LEFT_QUARTER=3;\n',
'    public static final int CAMERA_CHASE=0, CAMERA_REAR=1, CAMERA_RIGHT_QUARTER=2, CAMERA_LEFT_QUARTER=3, CAMERA_GROUND_OBSERVER=4;\n',
'ground observer camera constant')
j=rep(j,
'    public void setWheelSpeed(float v){r.ws=Math.max(0,v);}\n    public void setCameraMode(int m){r.cam=Math.max(0,Math.min(3,m));}\n    public int getCameraMode(){return r.cam;}\n    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP)r.cam=(r.cam+1)%4;return true;}\n',
'    public void setWheelSpeed(float v){r.ws=Math.max(0,v);}\n    public void setSupersonicState(float mach,float buffet,float boom){r.sup(mach,buffet,boom);}\n    public void setCameraMode(int m){r.cam=Math.max(0,Math.min(4,m));}\n    public int getCameraMode(){return r.cam;}\n    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP)r.cam=(r.cam+1)%5;return true;}\n',
'supersonic camera API')
j=rep(j,
'        volatile float tr,tp,ty,thr=.6f,tg=1,tm,tn,ws,speed,vertical,tsl,tsr,trl,trr,tfl,tfr,tvec;\n',
'        volatile float tr,tp,ty,thr=.6f,tg=1,tm,tn,ws,speed,vertical,tsl,tsr,trl,trr,tfl,tfr,tvec,mach,buffet,boom;\n',
'renderer Mach state')
j=rep(j,
'        void sim(float g,float m,float n){tg=cl(g,0,1);tm=cl(m,0,1);tn=cl(n,0,1);}\n',
'        void sim(float g,float m,float n){tg=cl(g,0,1);tm=cl(m,0,1);tn=cl(n,0,1);}\n        void sup(float m,float b,float bp){mach=Math.max(0,m);buffet=cl(b,0,1);boom=cl(bp,0,1);}\n',
'renderer supersonic state setter')
j=rep(j,
'            float sp=cl(speed/270f,0,1),fov=29.5f+sp*5.0f;Matrix.perspectiveM(pr,0,fov,aspect,.08f,240f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);\n',
'            float sp=cl(speed/520f,0,1),machBoost=cl((mach-.78f)/.62f,0,1),fov=29.5f+sp*3.6f+machBoost*4.8f+(cam==CAMERA_GROUND_OBSERVER?2.2f:0f);Matrix.perspectiveM(pr,0,fov,aspect,.08f,260f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);\n',
'Mach-dependent FOV')
j=rep(j,
'            float shake=(onGround?cl(speed/90f,0,1)*.085f:cl((speed-155f)/190f,0,1)*.020f),sx=(float)Math.sin(t*31f)*shake+(float)Math.sin(t*47f)*shake*.30f;Matrix.translateM(md,0,sx,sx*.26f,0);\n',
'            float shake=(onGround?cl(speed/90f,0,1)*.085f:cl((speed-155f)/300f,0,1)*.015f)+buffet*.052f+boom*.032f,sx=(float)Math.sin(t*31f)*shake+(float)Math.sin(t*47f)*shake*.30f;Matrix.translateM(md,0,sx,sx*.26f+boom*.018f,0);\n',
'transonic buffet camera shake')
old_cam='''        void camera(float sp){float lag=sp*1.40f,bob=(float)Math.sin(t*1.5f)*.018f*(1-sp);if(cam==1){camX=0;camY=1.50f;camZ=12.8f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.02f,1.42f,0,1,0);}else if(cam==2){camX=11.6f+sp*.7f;camY=4.55f;camZ=12.5f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.12f,0,1,0);}else if(cam==3){camX=-11.6f-sp*.7f;camY=4.55f;camZ=12.5f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.12f,0,1,0);}else{camX=0;camY=4.72f+bob;camZ=18.2f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.10f,-.65f-sp*.20f,0,1,0);}}\n'''
new_cam='''        void camera(float sp){float lag=sp*1.65f+cl((mach-1f)/.8f,0,1)*.65f,bob=(float)Math.sin(t*1.5f)*.018f*(1-sp);if(cam==1){camX=0;camY=1.50f;camZ=12.8f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.02f,1.42f,0,1,0);}else if(cam==2){camX=11.6f+sp*.7f;camY=4.55f;camZ=12.5f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.12f,0,1,0);}else if(cam==3){camX=-11.6f-sp*.7f;camY=4.55f;camZ=12.5f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.12f,0,1,0);}else if(cam==CAMERA_GROUND_OBSERVER){camX=8.9f;camY=1.18f;camZ=8.0f;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.35f,0,1,0);}else{camX=0;camY=4.72f+bob;camZ=18.2f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.10f,-.65f-sp*.20f,0,1,0);}}\n'''
j=rep(j,old_cam,new_cam,'ground observer camera geometry')
JET.write_text(j)

# Sound mix: retain the existing transonic layer, add sustained supersonic rumble and
# an N-wave-like boom only when the runtime reports the observer camera.
fm=SMODEL.read_text()
fm=rep(fm,
'        public double mach, intake, fan, turbine, exhaust, afterburner, wind, tyre, brake, gearHydraulic, surfaceHydraulic, transonic;\n',
'        public double mach, intake, fan, turbine, exhaust, afterburner, wind, tyre, brake, gearHydraulic, surfaceHydraulic, transonic, supersonic;\n',
'supersonic sound gain field')
fm=rep(fm,
'        m.transonic=d<.11?.12*(1-d/.11):0;\n',
'        m.transonic=d<.13?.15*(1-d/.13):0;\n        m.supersonic=.11*smooth(1.02,1.55,m.mach);\n',
'supersonic rumble gain')
SMODEL.write_text(fm)

snd=SOUND.read_text()
snd=rep(snd,
'    private volatile float gearMotion,surfaceMotion;\n',
'    private volatile float gearMotion,surfaceMotion,supersonicMach,boomPulseInput;\n    private volatile boolean worldFixedObserver;\n',
'supersonic audio inputs')
snd=rep(snd,
'    private float touchdownEnv,gearClunkEnv;\n    private boolean previousGround=true;\n',
'    private float touchdownEnv,gearClunkEnv,sonicBoomEnv,boomPhase;\n    private boolean previousGround=true,boomLatched;\n',
'sonic boom envelope state')
snd=rep(snd,
'    private void loop(){\n',
'    public void setSupersonicState(double mach,double boomPulse,boolean observer){supersonicMach=Math.max(0,(float)mach);boomPulseInput=cl((float)boomPulse,0,1);worldFixedObserver=observer;}\n\n    private void loop(){\n',
'supersonic audio setter')
snd=rep(snd,
'            float th=throttle,sp=speed,alt=altitude,gd=gear,br=brake,gm=gearMotion,sm=surfaceMotion;boolean wow=ground,internal=cockpitView;\n',
'            float th=throttle,sp=speed,alt=altitude,gd=gear,br=brake,gm=gearMotion,sm=surfaceMotion,bp=boomPulseInput;boolean wow=ground,internal=cockpitView,observer=worldFixedObserver;\n            if(observer&&bp>.82f&&!boomLatched){sonicBoomEnv=1f;boomPhase=0;boomLatched=true;}if(bp<.08f)boomLatched=false;\n',
'boom trigger latch')
snd=rep(snd,
'                float trans=(noise*.52f+lowNoise*.38f)*(float)mx.transonic;\n',
'                float trans=(noise*.52f+lowNoise*.38f)*(float)mx.transonic;\n                float supRumble=(lowNoise*.68f+noise*.16f+(float)Math.sin(phCoreL*.21)*.16f)*(float)mx.supersonic;\n                boomPhase+=1f/SR;float nWave=boomPhase<.018f?1f-(boomPhase/.018f)*1.70f:boomPhase<.090f?-.70f*(1f-(boomPhase-.018f)/.072f):0f;float sonicBoom=(nWave+lowNoise*.26f+noise*.15f)*sonicBoomEnv*.72f;sonicBoomEnv*=.99982f;\n',
'supersonic rumble and N-wave')
snd=rep(snd,
'                float common=intake+exhaust+ab+wind+trans+tyre+brakeTone+hydraulic+touchdown+clunk;\n',
'                float common=intake+exhaust+ab+wind+trans+supRumble+sonicBoom+tyre+brakeTone+hydraulic+touchdown+clunk;\n',
'supersonic audio mix')
SOUND.write_text(snd)

# v93 is applied immediately before this patch in CI.
g=GRADLE.read_text()
g=rep(g,'        versionCode 93\n','        versionCode 94\n','version code')
g=rep(g,"        versionName '26.11-avm22.0-variable-nozzle'\n","        versionName '26.12-avm23.0-supersonic-flight'\n",'version name')
GRADLE.write_text(g)

print('v94 supersonic flight applied: true Mach state, AB supersonic acceleration, wave drag, transonic buffet, Mach-cone overlay, Mach HUD, observer sonic boom and Mach camera behavior')
