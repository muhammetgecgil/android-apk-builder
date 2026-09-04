from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
VIS=PKG/'visual'
SMOOTHER=SIM/'AutonomousTurnSmoother.java'
JET=PKG/'Jet3DView.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:return text
    if old not in text:raise SystemExit(f'v107 anchor missing: {label}')
    return text.replace(old,new,1)

def rx(text,pattern,repl,label):
    ntext,n=re.subn(pattern,repl,text,count=1,flags=re.S)
    if n!=1:raise SystemExit(f'v107 regex anchor missing: {label}')
    return ntext

# ---------------------------------------------------------------------------
# 1) Autonomous anti-spin governor.
#    This is deliberately inside AutonomousTurnSmoother so manual/BT flight is
#    untouched. It prevents an accumulated angular rate from carrying the demo
#    aircraft through repeated 180/360 degree rolls before controls can recover.
# ---------------------------------------------------------------------------
a=SMOOTHER.read_text()
a=rep(a,
'''        dt=clamp(dt,.001,.08);\n        if(s.onGround){rollOut=0;pitchOut=0;yawOut=c.yaw;c.roll=0;c.pitch=0;return;}\n''',
'''        dt=clamp(dt,.001,.08);\n        boolean antiSpinRecovery=protectAutonomousState(s);\n        if(s.onGround){rollOut=0;pitchOut=0;yawOut=c.yaw;c.roll=0;c.pitch=0;return;}\n''','anti-spin entry')
a=rep(a,
'''        double desiredBank=clamp(c.roll*92.0,-34.0,34.0);\n''',
'''        double desiredBank=clamp(c.roll*86.0,-30.0,30.0);\n''','bank envelope')
a=rep(a,
'''        if(Math.abs(s.rollDeg)>46.0)desiredBank=0.0;\n''',
'''        if(Math.abs(s.rollDeg)>40.0||antiSpinRecovery)desiredBank=0.0;\n''','recovery threshold')
a=rep(a,
'''        double desiredRoll=clamp(bankErr/30.0-s.rollRateDegSec/95.0,-.38,.38);\n''',
'''        double desiredRoll=clamp(bankErr/27.0-s.rollRateDegSec/72.0,-.44,.44);\n''','roll recovery authority')
a=rep(a,
'''        double desiredYaw=clamp(c.yaw*.24-s.yawRateDegSec/120.0-s.sideslipDeg/48.0,-.14,.14);\n''',
'''        double desiredYaw=antiSpinRecovery?0.0:clamp(c.yaw*.20-s.yawRateDegSec/95.0-s.sideslipDeg/42.0,-.11,.11);\n''','yaw recovery')
a=rep(a,
'''        rollOut=approach(rollOut,desiredRoll,.48*dt);\n        pitchOut=approach(pitchOut,desiredPitchCmd,.42*dt);\n        yawOut=approach(yawOut,desiredYaw,.38*dt);\n''',
'''        rollOut=approach(rollOut,desiredRoll,(antiSpinRecovery?.70:.46)*dt);\n        pitchOut=approach(pitchOut,desiredPitchCmd,.40*dt);\n        yawOut=approach(yawOut,desiredYaw,(antiSpinRecovery?.78:.34)*dt);\n''','recovery slew')
a=rep(a,
'''    public double getRollOut(){return rollOut;}\n''',
'''    private boolean protectAutonomousState(FlightState s){\n        if(s.onGround)return false;\n        boolean recovery=Math.abs(s.rollDeg)>40.0||Math.abs(s.rollRateDegSec)>42.0||Math.abs(s.yawRateDegSec)>24.0||s.spin01>.10;\n        // Absolute state/rate envelope.  The dynamics step can add only a small\n        // delta before this runs again, so a multi-turn drill-like roll cannot form.\n        s.rollRateDegSec=clamp(s.rollRateDegSec,-52.0,52.0);\n        s.yawRateDegSec=clamp(s.yawRateDegSec,-28.0,28.0);\n        if(Math.abs(s.rollDeg)>55.0)s.rollDeg=Math.copySign(55.0,s.rollDeg);\n        if(Math.abs(s.rollDeg)>40.0&&s.rollDeg*s.rollRateDegSec>0)s.rollRateDegSec*=.16;\n        if(s.spin01>.10){s.rollRateDegSec*=.34;s.yawRateDegSec*=.28;}\n        if(Math.abs(s.pitchDeg)>34.0&&s.pitchDeg*s.pitchRateDegSec>0)s.pitchRateDegSec*=.30;\n        return recovery;\n    }\n\n    public double getRollOut(){return rollOut;}\n''','state guard method')
SMOOTHER.write_text(a)

# ---------------------------------------------------------------------------
# 2) True OpenGL world: sea, coastline, islands and mountains share the same
#    depth buffer / lighting pipeline as the aircraft. The existing dynamic sky
#    remains behind the transparent GLSurfaceView.
# ---------------------------------------------------------------------------
j=JET.read_text()
if 'import com.mg.fixturecockpitsim.visual.True3DWorldMesh;' not in j:
    j=rep(j,'import com.mg.fixturecockpitsim.visual.WingtipVortexMesh;\n',
          'import com.mg.fixturecockpitsim.visual.WingtipVortexMesh;\nimport com.mg.fixturecockpitsim.visual.True3DWorldMesh;\n','world import')

if 'worldMd=new float[16]' not in j:
    j=rx(j,r'final float\[] pr=new float\[16\],vw=new float\[16\],md=new float\[16\],vp=new float\[16\],mvp=new float\[16\];',
         'final float[] pr=new float[16],vw=new float[16],md=new float[16],vp=new float[16],mvp=new float[16],worldMd=new float[16],worldMvp=new float[16];','world matrices')

if 'FloatBuffer seaBuffer' not in j:
    j=rx(j,r'(\s*FloatBuffer [^;]*;\n)',r'\1        FloatBuffer seaBuffer,landBuffer,shoreBuffer,islandBuffer,mountainBuffer,snowBuffer;\n','world buffers')
if 'int seaCount' not in j:
    j=rx(j,r'(\s*int opaqueCount[^;]*;\n)',r'\1        int seaCount,landCount,shoreCount,islandCount,mountainCount,snowCount;\n','world counts')
if 'float worldTravel' not in j:
    j=rep(j,'        long last;\n','        long last;float worldTravel;\n','world travel field')

if 'True3DWorldMesh.sea()' not in j:
    j=rep(j,'last=System.nanoTime();',
'''float[] wSea=True3DWorldMesh.sea(),wLand=True3DWorldMesh.coastLand(),wShore=True3DWorldMesh.shoreline(),wIslands=True3DWorldMesh.islands(),wMount=True3DWorldMesh.mountains(),wSnow=True3DWorldMesh.snowCaps();\n            seaBuffer=buffer(wSea);seaCount=wSea.length/7;landBuffer=buffer(wLand);landCount=wLand.length/7;shoreBuffer=buffer(wShore);shoreCount=wShore.length/7;islandBuffer=buffer(wIslands);islandCount=wIslands.length/7;mountainBuffer=buffer(wMount);mountainCount=wMount.length/7;snowBuffer=buffer(wSnow);snowCount=wSnow.length/7;last=System.nanoTime();''','world buffer init')

if 'worldTravel=(worldTravel+' not in j:
    j=rep(j,'last=n;t+=dt;','last=n;t+=dt;if(!onGround)worldTravel=(worldTravel+speed*dt*.018f)%120f;','world motion integration')

if 'drawTrue3DWorld();Matrix.setIdentityM(md,0);' not in j:
    j=rep(j,'camera(sp);Matrix.multiplyMM(vp,0,pr,0,vw,0);Matrix.setIdentityM(md,0);',
          'camera(sp);Matrix.multiplyMM(vp,0,pr,0,vw,0);drawTrue3DWorld();Matrix.setIdentityM(md,0);','world draw hook')

if 'private void drawTrue3DWorld()' not in j:
    helper=r'''        private void drawTrue3DWorld(){
            float alt=AirfieldWorldView.getSharedAltitudeM();if(onGround||alt<28f)return;
            float cross=AirfieldWorldView.getSharedCrossTrackM(),heading=shortest(yaw-270f);
            float drop=3.0f+Math.min(31f,alt/58f),rough=cl(WeatherEffectsView.getSharedSeaRoughness(),0,1),haze=cl(WeatherEffectsView.getSharedSkyHaze(),0,1),hum=cl(WeatherEffectsView.getSharedHumidity01(),0,1),temp=WeatherEffectsView.getSharedTemperatureC();
            float sr=.025f+.045f*haze,sg=.13f+.12f*(1-haze),sb=.22f+.23f*(1-haze);
            float lr=.24f+.13f*(1-hum),lg=.27f+.22f*hum,lb=.18f+.08f*haze;
            float mr=.25f+.20f*haze,mg=.27f+.18f*(1-haze),mb=.25f+.16f*haze;
            GLES20.glUseProgram(pg);GLES20.glUniform3f(ul,-.38f,.86f,-.33f);GLES20.glUniform3f(ucam,camX,camY,camZ);GLES20.glUniform1f(ut,thr);GLES20.glUniform1f(utime,t);GLES20.glUniform1f(uspeed,speed);GLES20.glUniform1f(uair,1f);GLES20.glUniform1f(uroll,roll);GLES20.glDisable(GLES20.GL_CULL_FACE);
            float off=worldTravel;
            drawWorldTile(off,drop,cross,heading,rough,temp,sr,sg,sb,lr,lg,lb,mr,mg,mb);
            drawWorldTile(off-120f,drop,cross,heading,rough,temp,sr,sg,sb,lr,lg,lb,mr,mg,mb);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
        }

        private void drawWorldTile(float zOff,float drop,float cross,float heading,float rough,float temp,float sr,float sg,float sb,float lr,float lg,float lb,float mr,float mg,float mb){
            Matrix.setIdentityM(worldMd,0);Matrix.translateM(worldMd,0,-cross*.012f,-drop,zOff);Matrix.rotateM(worldMd,0,heading*.72f,0,1,0);
            float[] base=worldMd.clone();Matrix.scaleM(worldMd,0,1f,.55f+2.2f*rough,1f);drawWorldBuffer(seaBuffer,seaCount,worldMd,sr,sg,sb);
            System.arraycopy(base,0,worldMd,0,16);drawWorldBuffer(landBuffer,landCount,worldMd,lr,lg,lb);drawWorldBuffer(islandBuffer,islandCount,worldMd,lr*.92f,lg*1.03f,lb*.92f);
            drawWorldBuffer(shoreBuffer,shoreCount,worldMd,.58f,.49f,.31f);drawWorldBuffer(mountainBuffer,mountainCount,worldMd,mr,mg,mb);
            if(temp<7f)drawWorldBuffer(snowBuffer,snowCount,worldMd,.82f,.86f,.86f);
        }

        private void drawWorldBuffer(FloatBuffer b,int vertices,float[] model,float r,float g,float bl){
            if(b==null||vertices<=0)return;Matrix.multiplyMM(worldMvp,0,vp,0,model,0);GLES20.glUniformMatrix4fv(umvp,1,false,worldMvp,0);GLES20.glUniformMatrix4fv(umodel,1,false,model,0);GLES20.glUniform4f(uc,r,g,bl,1f);bindAndDraw(b,vertices);
        }

'''
    j=rep(j,'        private void bindAndDraw(FloatBuffer b,int vertices){',helper+'        private void bindAndDraw(FloatBuffer b,int vertices){','world draw helpers')
JET.write_text(j)

# Version.
g=GRADLE.read_text()
g=re.sub(r'versionCode\s+\d+','versionCode 107',g,count=1)
g=re.sub(r"versionName\s+['\"][^'\"]+['\"]","versionName '26.25-avm34.0-true-3d-terrain-anti-spin'",g,count=1)
GRADLE.write_text(g)
print('v107 applied: autonomous anti-spin hard envelope + true OpenGL sea/coast/island/mountain world')
