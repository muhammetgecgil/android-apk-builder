from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RUNTIME=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/FlightRuntimeActivity.java'
JET=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/Jet3DView.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v92 fighter-audio patch anchor missing: {label}')
    return text.replace(old,new,1)

# Centralize audio in FlightRuntimeActivity and feed the actual flight/mechanical state.
r=RUNTIME.read_text()
r=rep(r,
'        sound.update(state.throttle,state.trueAirspeedMps,state.gearPosition,state.brake01,state.onGround);\n',
'        sound.update(state.throttle,state.trueAirspeedMps,state.altitudeM,state.gearPosition,state.brake01,state.onGround,state.leftStabilatorDeg,state.rightStabilatorDeg,state.leftRudderDeg,state.rightRudderDeg,state.leftFlaperonDeg,state.rightFlaperonDeg,state.leftLeadingEdgeFlapDeg,state.rightLeadingEdgeFlapDeg,state.speedBrakeDeg,false);\n',
'central advanced sound state')
RUNTIME.write_text(r)

# v91 still had a second FlightSoundEngine inside Jet3DView. Remove it so one AudioTrack owns the mix.
j=JET.read_text()
j=rep(j,
'    private final FlightSoundEngine sound=new FlightSoundEngine();\n',
'',
'duplicate renderer sound engine')
j=rep(j,
'    private float st=.6f,sg=1f,sb;\n    private boolean ground=true;\n',
'',
'duplicate sound cache')
j=rep(j,
'    public void setTelemetry(float roll,float pitch,float yaw,float throttle,float linkHz,int drops,boolean live){r.tele(roll,pitch,yaw,throttle,live);st=cl(throttle,0,1);sound.update(st,st*230,sg,sb,ground);}\n',
'    public void setTelemetry(float roll,float pitch,float yaw,float throttle,float linkHz,int drops,boolean live){r.tele(roll,pitch,yaw,throttle,live);}\n',
'renderer telemetry sound removal')
j=rep(j,
'    public void setSimulationState(float gear,float mainComp,float noseComp,float brake,boolean onGround){r.sim(gear,mainComp,noseComp);sg=gear;sb=brake;ground=onGround;sound.update(st,st*230,sg,sb,ground);}\n',
'    public void setSimulationState(float gear,float mainComp,float noseComp,float brake,boolean onGround){r.sim(gear,mainComp,noseComp);}\n',
'renderer state sound removal')
j=rep(j,
'    @Override public void onResume(){super.onResume();sound.start();}\n    @Override public void onPause(){sound.stop();super.onPause();}\n',
'    @Override public void onResume(){super.onResume();}\n    @Override public void onPause(){super.onPause();}\n',
'renderer lifecycle sound removal')
JET.write_text(j)

g=GRADLE.read_text()
g=rep(g,'        versionCode 91\n','        versionCode 92\n','version code')
g=rep(g,"        versionName '26.9-avm20.1-attached-wingtip-lighting'\n","        versionName '26.10-avm21.0-fighter-audio'\n",'version name')
GRADLE.write_text(g)

print('v92 fighter audio applied: single central AudioTrack, intake/fan/turbine/exhaust/AB/wind/transonic/hydraulic/tyre/brake layers')
