from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
VIS=PKG/'visual'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim'
RUNTIME=PKG/'FlightRuntimeActivity.java'
SOUND=PKG/'FlightSoundEngine.java'
REAL=VIS/'RealisticFighterMesh.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:return text
    if old not in text:raise SystemExit(f'v110 anchor missing: {label}')
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Pure mute state: UI/persistence behavior can be unit-tested without Android audio.
# ---------------------------------------------------------------------------
(SIM/'SoundMuteState.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

/** Small deterministic state holder for the simulator master-audio switch. */
public final class SoundMuteState {
    private boolean muted;
    public SoundMuteState(){}
    public SoundMuteState(boolean initial){muted=initial;}
    public void setMuted(boolean value){muted=value;}
    public boolean isMuted(){return muted;}
    public boolean toggle(){muted=!muted;return muted;}
    public String buttonLabel(){return muted?"SES KAPALI":"SES AÇIK";}
}
''')

(TEST/'sim').mkdir(parents=True,exist_ok=True)
(TEST/'sim/SoundMuteStateTest.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class SoundMuteStateTest {
    @Test public void toggleChangesStateAndLabel(){
        SoundMuteState s=new SoundMuteState(false);
        assertFalse(s.isMuted());assertEquals("SES AÇIK",s.buttonLabel());
        assertTrue(s.toggle());assertEquals("SES KAPALI",s.buttonLabel());
        assertFalse(s.toggle());assertEquals("SES AÇIK",s.buttonLabel());
    }
}
''')

# ---------------------------------------------------------------------------
# Audio engine: true master mute. Keep synthesis alive so unmuting has no spool jump.
# ---------------------------------------------------------------------------
a=SOUND.read_text()
a=rep(a,
'    private volatile boolean running;\n',
'    private volatile boolean running,muted;\n',
'audio mute state')
a=rep(a,
'            track.play();running=true;thread=new Thread(this::loop,"FighterAudio");thread.start();\n',
'            track.setVolume(muted?0f:1f);track.play();running=true;thread=new Thread(this::loop,"FighterAudio");thread.start();\n',
'apply mute when audio starts')
if 'public void setMuted(boolean value)' not in a:
    anchor='    /** Backward-compatible update. */\n'
    method='''    public void setMuted(boolean value){\n        muted=value;AudioTrack tr=track;if(tr!=null){try{tr.setVolume(value?0f:1f);}catch(Exception ignored){}}\n    }\n    public boolean isMuted(){return muted;}\n\n'''
    if anchor not in a:raise SystemExit('v110 anchor missing: audio update marker')
    a=a.replace(anchor,method+anchor,1)
SOUND.write_text(a)

# ---------------------------------------------------------------------------
# Runtime UI: persistent Turkish SES AÇIK / SES KAPALI control, separate from
# the already crowded flight-control strip.
# ---------------------------------------------------------------------------
r=RUNTIME.read_text()
r=rep(r,
'import com.mg.fixturecockpitsim.sim.FlightState;\n',
'import com.mg.fixturecockpitsim.sim.FlightState;\nimport com.mg.fixturecockpitsim.sim.SoundMuteState;\n',
'mute state import')
r=rep(r,
'    private final FlightSoundEngine sound=new FlightSoundEngine();\n',
'    private final FlightSoundEngine sound=new FlightSoundEngine();\n    private final SoundMuteState soundMute=new SoundMuteState();\n',
'mute controller field')
if 'soundButton' not in r:
    m=re.search(r'(?m)^\s*private Button\s+resetButton[^;]*;\s*$',r)
    if not m:raise SystemExit('v110 anchor missing: runtime button fields')
    line=m.group(0);r=r[:m.start()]+line[:-1]+',soundButton;'+r[m.end():]

if 'audio_muted' not in r:
    anchor='        demoMode=getIntent()==null||getIntent().getBooleanExtra(LauncherActivity.EXTRA_DEMO_MODE,true);\n'
    add='        boolean initialMute=getSharedPreferences("sim_prefs",MODE_PRIVATE).getBoolean("audio_muted",false);soundMute.setMuted(initialMute);sound.setMuted(initialMute);\n'
    if anchor not in r:raise SystemExit('v110 anchor missing: onCreate demo mode')
    r=r.replace(anchor,anchor+add,1)

if 'toggleSoundMute()' not in r:
    anchor='resetButton.setOnClickListener(v->resetSimulation());'
    idx=r.find(anchor)
    if idx<0:raise SystemExit('v110 anchor missing: reset button setup')
    idx+=len(anchor)
    add='soundButton=button(soundMute.buttonLabel());FrameLayout.LayoutParams sap=new FrameLayout.LayoutParams(dp(102),dp(43),Gravity.TOP|Gravity.RIGHT);sap.setMargins(0,dp(104),dp(10),0);root.addView(soundButton,sap);soundButton.setOnClickListener(v->toggleSoundMute());'
    r=r[:idx]+add+r[idx:]
    method='''    private void toggleSoundMute(){\n        boolean muted=soundMute.toggle();sound.setMuted(muted);getSharedPreferences("sim_prefs",MODE_PRIVATE).edit().putBoolean("audio_muted",muted).apply();if(soundButton!=null)soundButton.setText(soundMute.buttonLabel());Toast.makeText(this,muted?"Uçuş sesi kapatıldı":"Uçuş sesi açıldı",Toast.LENGTH_SHORT).show();\n    }\n\n'''
    hold='    private void hold(Button b,Runnable press,Runnable release)'
    if hold not in r:raise SystemExit('v110 anchor missing: hold helper')
    r=r.replace(hold,method+hold,1)
RUNTIME.write_text(r)

# ---------------------------------------------------------------------------
# Main airframe geometry refinement. No fake underside cap and no large overlay
# plate is added. Existing aircraft surfaces themselves become smoother/deeper.
# ---------------------------------------------------------------------------
g=REAL.read_text()
# The old secondary belly prism is redundant with the closed 360-degree fuselage
# and can read as a leftover plate from below. Keep the real fuselage shell only.
g=g.replace('        b.bellyBlend();\n','        // v110: closed fuselage already defines the belly; omit the old secondary belly overlay.\n',1)

# More circumferential resolution for the primary fuselage shell.
g=rep(g,'        smoothLoft(z,rx,ry,cy,56,.80f);\n','        smoothLoft(z,rx,ry,cy,72,.80f);\n','fuselage tessellation')

# Intake lip now flows into a deeper, rounder duct rather than ending at a flat opening.
old_intake='''    private void intakeDuct(float side){\n        duct(side,1.38f,-.02f,-2.55f,.29f,.25f,1.18f,-.07f,-1.84f,.25f,.22f,20);\n        duct(side,1.18f,-.07f,-1.84f,.25f,.22f,.94f,-.09f,-1.16f,.23f,.20f,20);\n    }\n'''
new_intake='''    private void intakeDuct(float side){\n        // Short bell-mouth transition gives the intake lip physical thickness and depth.\n        duct(side,1.43f,-.01f,-2.68f,.345f,.300f,1.37f,-.02f,-2.52f,.295f,.255f,32);\n        duct(side,1.37f,-.02f,-2.52f,.295f,.255f,1.18f,-.07f,-1.84f,.250f,.220f,32);\n        duct(side,1.18f,-.07f,-1.84f,.250f,.220f,.94f,-.09f,-1.16f,.230f,.200f,32);\n    }\n'''
g=rep(g,old_intake,new_intake,'intake duct depth')

old_nacelle='''    private void engineNacelle(float x){\n        float[] z={-.98f,-.66f,-.28f,.16f,.66f,1.20f,1.72f,2.18f,2.58f,2.90f,3.15f,3.32f};\n        float[] rx={.24f,.31f,.40f,.50f,.58f,.63f,.66f,.66f,.63f,.58f,.52f,.46f};\n        float[] ry={.15f,.19f,.24f,.29f,.34f,.37f,.39f,.39f,.37f,.33f,.29f,.25f};\n        smoothTube(x,-.10f,z,rx,ry,40);\n    }\n\n    private void smoothTube(float cx,float cy,float[] z,float[] rx,float[] ry,int sides){\n        for(int s=0;s<z.length-1;s++)for(int i=0;i<sides;i++){\n            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;\n            V a=tubeV(cx,cy,z[s],rx[s],ry[s],a0),b=tubeV(cx,cy,z[s+1],rx[s+1],ry[s+1],a0),c=tubeV(cx,cy,z[s+1],rx[s+1],ry[s+1],a1),d=tubeV(cx,cy,z[s],rx[s],ry[s],a1);\n            quadSmooth(a,b,c,d);\n        }\n    }\n\n    private V tubeV(float cx,float cy,float z,float rx,float ry,double a){float ca=(float)Math.cos(a),sa=(float)Math.sin(a);return new V(cx+rx*ca,cy+ry*sa,z,ca,sa,0);}\n\n    private void heatShield(float x){tubeSurface(x,-.10f,2.70f,3.42f,.61f,.44f,.61f,36);}\n    private void nozzleOuter(float x){tubeSurface(x,-.10f,3.08f,3.56f,.49f,.405f,.61f,40);}\n'''
new_nacelle='''    private void engineNacelle(float x){\n        // Denser longitudinal stations avoid the stepped/capsule look around the engines.\n        float[] z={-1.02f,-.84f,-.62f,-.36f,-.06f,.26f,.62f,1.00f,1.38f,1.74f,2.08f,2.38f,2.64f,2.86f,3.04f,3.20f,3.34f};\n        float[] rx={.23f,.27f,.32f,.38f,.45f,.52f,.58f,.63f,.66f,.675f,.670f,.655f,.630f,.590f,.540f,.490f,.455f};\n        float[] ry={.145f,.160f,.185f,.220f,.260f,.300f,.340f,.370f,.390f,.402f,.400f,.385f,.365f,.340f,.310f,.275f,.245f};\n        smoothTube(x,-.10f,z,rx,ry,56);\n    }\n\n    private void smoothTube(float cx,float cy,float[] z,float[] rx,float[] ry,int sides){\n        for(int s=0;s<z.length-1;s++){\n            float dz=Math.max(.001f,z[s+1]-z[s]),drx=(rx[s+1]-rx[s])/dz,dry=(ry[s+1]-ry[s])/dz;\n            for(int i=0;i<sides;i++){\n                double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;\n                V a=tubeV(cx,cy,z[s],rx[s],ry[s],a0,drx,dry),b=tubeV(cx,cy,z[s+1],rx[s+1],ry[s+1],a0,drx,dry),c=tubeV(cx,cy,z[s+1],rx[s+1],ry[s+1],a1,drx,dry),d=tubeV(cx,cy,z[s],rx[s],ry[s],a1,drx,dry);\n                quadSmooth(a,b,c,d);\n            }\n        }\n    }\n\n    private V tubeV(float cx,float cy,float z,float rx,float ry,double a,float drx,float dry){\n        float ca=(float)Math.cos(a),sa=(float)Math.sin(a);float nx=ca/Math.max(.03f,rx),ny=sa/Math.max(.03f,ry),nz=-(drx*ca+dry*sa);\n        return new V(cx+rx*ca,cy+ry*sa,z,nx,ny,nz);\n    }\n\n    private void heatShield(float x){tubeSurface(x,-.10f,2.70f,3.42f,.61f,.44f,.61f,52);}\n    private void nozzleOuter(float x){tubeSurface(x,-.10f,3.08f,3.56f,.49f,.405f,.61f,56);}\n'''
g=rep(g,old_nacelle,new_nacelle,'engine nacelle loft')

g=rep(g,'        int petals=20;float z0=3.34f,z1=3.82f,r0=.425f,r1=.315f;\n','        int petals=24;float z0=3.34f,z1=3.82f,r0=.425f,r1=.315f;\n','nozzle petal density')
g=rep(g,'            double a0=2*Math.PI*i/petals+.020,a1=2*Math.PI*(i+1)/petals-.020;\n','            double a0=2*Math.PI*i/petals+.015,a1=2*Math.PI*(i+1)/petals-.015;\n','nozzle petal gap')
g=rep(g,'    private void nozzleInner(float x){tubeSurface(x,-.10f,3.55f,3.96f,.285f,.205f,.60f,36);}\n','    private void nozzleInner(float x){tubeSurface(x,-.10f,3.55f,3.96f,.285f,.205f,.60f,48);}\n','nozzle inner density')
g=rep(g,'        int arcs=30;\n','        int arcs=42;\n','canopy tessellation')
g=rep(g,'        torusWheel(0f,-1.62f,-3.78f,.205f,.065f,22,10);\n        torusWheel(-1.70f,-1.67f,1.18f,.295f,.082f,24,10);\n        torusWheel(1.70f,-1.67f,1.18f,.295f,.082f,24,10);\n','        torusWheel(0f,-1.62f,-3.78f,.205f,.065f,26,12);\n        torusWheel(-1.70f,-1.67f,1.18f,.295f,.082f,30,12);\n        torusWheel(1.70f,-1.67f,1.18f,.295f,.082f,30,12);\n','wheel tessellation')
REAL.write_text(g)

(TEST/'visual').mkdir(parents=True,exist_ok=True)
(TEST/'visual/AirframeGeometryQualityTest.java').write_text(r'''package com.mg.fixturecockpitsim.visual;

import org.junit.Test;
import static org.junit.Assert.*;

public class AirframeGeometryQualityTest {
    @Test public void refinedAirframeIsDenseFiniteAndThreeDimensional(){
        ProceduralFighterMesh.Mesh m=RealisticFighterMesh.build();
        assertTrue(m.vertexCount()>18000);
        float minX=999,maxX=-999,minY=999,maxY=-999,minZ=999,maxZ=-999;
        for(int i=0;i<m.data.length;i+=7){
            for(int k=0;k<7;k++)assertTrue(Float.isFinite(m.data[i+k]));
            minX=Math.min(minX,m.data[i]);maxX=Math.max(maxX,m.data[i]);
            minY=Math.min(minY,m.data[i+1]);maxY=Math.max(maxY,m.data[i+1]);
            minZ=Math.min(minZ,m.data[i+2]);maxZ=Math.max(maxZ,m.data[i+2]);
        }
        assertTrue(maxX-minX>9f);assertTrue(maxY-minY>2f);assertTrue(maxZ-minZ>10f);
    }
}
''')

# Version.
v=GRADLE.read_text()
v=re.sub(r'versionCode\s+\d+','versionCode 110',v,count=1)
v=re.sub(r"versionName\s+['\"][^'\"]+['\"]","versionName '26.28-avm37.0-mute-airframe-geometry-realism'",v,count=1)
GRADLE.write_text(v)
print('v110 applied: persistent master mute + denser native airframe/intake/nacelle/nozzle/canopy geometry; no fake belly cap')
