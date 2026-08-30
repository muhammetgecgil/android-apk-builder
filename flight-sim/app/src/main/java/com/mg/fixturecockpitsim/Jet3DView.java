package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import com.mg.fixturecockpitsim.visual.AircraftControlSurfaces;
import com.mg.fixturecockpitsim.visual.ProceduralFighterMesh;
import com.mg.fixturecockpitsim.visual.RealisticFighterMesh;
import com.mg.fixturecockpitsim.visual.VisualOrdnanceMesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** AVM-12.0 high-detail exterior fighter renderer. */
public final class Jet3DView extends GLSurfaceView {
    public static final int CAMERA_CHASE=0, CAMERA_REAR=1, CAMERA_RIGHT_QUARTER=2, CAMERA_LEFT_QUARTER=3;
    private final R r;
    private final FlightSoundEngine sound=new FlightSoundEngine();
    private float st=.6f,sg=1f,sb;
    private boolean ground=true;

    public Jet3DView(Context c){
        super(c);setEGLContextClientVersion(2);setEGLConfigChooser(8,8,8,8,24,0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);setZOrderOnTop(true);
        r=new R();setRenderer(r);setRenderMode(RENDERMODE_CONTINUOUSLY);
    }
    public void setTelemetry(float roll,float pitch,float yaw,float throttle,float linkHz,int drops,boolean live){r.tele(roll,pitch,yaw,throttle,live);st=cl(throttle,0,1);sound.update(st,st*230,sg,sb,ground);}
    public void setControlInputs(float pitch,float roll,float yaw,float throttle){r.controls(pitch,roll,yaw,throttle);}
    public void setSimulationState(float gear,float mainComp,float noseComp,float brake,boolean onGround){r.sim(gear,mainComp,noseComp);sg=gear;sb=brake;ground=onGround;sound.update(st,st*230,sg,sb,ground);}
    public void setFlightMotion(float speed,float vertical,boolean onGround){r.speed=Math.max(0,speed);r.vertical=vertical;r.onGround=onGround;}
    public void setWheelSpeed(float v){r.ws=Math.max(0,v);} public void setCameraMode(int m){r.cam=Math.max(0,Math.min(3,m));} public int getCameraMode(){return r.cam;}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP)r.cam=(r.cam+1)%4;return true;}
    @Override public void onResume(){super.onResume();sound.start();} @Override public void onPause(){sound.stop();super.onPause();}

    private static final class R implements Renderer {
        final float[] pr=new float[16],vw=new float[16],md=new float[16],vp=new float[16],mvp=new float[16];
        final AircraftControlSurfaces map=new AircraftControlSurfaces();
        volatile float tr,tp,ty,thr=.6f,tg=1,tm,tn,ws,speed,vertical,tsl,tsr,trl,trr,tfl,tfr,tvec;
        volatile boolean onGround=true,live; volatile int cam;
        float roll,pitch,yaw,gear=1,mc,nc,spin,aspect=1.7f,t,sl,sr,rl,rrd,fl,fr,vec;
        int pg,ap,an,apart,umvp,umodel,uc,ul,ut,ug,um,un,uws,usl,usr,url,urr,ufl,ufr,uvec;
        FloatBuffer vb,ob; int count,ordnanceCount; long last;

        void tele(float a,float b,float c,float d,boolean x){tr=a;tp=b;ty=c;thr=cl(d,0,1);live=x;}
        synchronized void controls(float p,float r,float y,float th){map.update(p,r,y,th);tsl=map.leftStabilatorDeg;tsr=map.rightStabilatorDeg;trl=map.leftRudderDeg;trr=map.rightRudderDeg;tfl=map.leftFlaperonDeg;tfr=map.rightFlaperonDeg;tvec=cl(p,-1,1)*8f;}
        void sim(float g,float m,float n){tg=cl(g,0,1);tm=cl(m,0,1);tn=cl(n,0,1);}

        @Override public void onSurfaceCreated(GL10 g,EGLConfig e){
            GLES20.glClearColor(0,0,0,0);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glDepthFunc(GLES20.GL_LEQUAL);GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glCullFace(GLES20.GL_BACK);GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
            pg=prog(VS,FS);ap=GLES20.glGetAttribLocation(pg,"aPos");an=GLES20.glGetAttribLocation(pg,"aNormal");apart=GLES20.glGetAttribLocation(pg,"aPart");umvp=GLES20.glGetUniformLocation(pg,"uMvp");umodel=GLES20.glGetUniformLocation(pg,"uModel");uc=GLES20.glGetUniformLocation(pg,"uColor");ul=GLES20.glGetUniformLocation(pg,"uLightDir");ut=GLES20.glGetUniformLocation(pg,"uThrottle");ug=GLES20.glGetUniformLocation(pg,"uGear");um=GLES20.glGetUniformLocation(pg,"uMainComp");un=GLES20.glGetUniformLocation(pg,"uNoseComp");uws=GLES20.glGetUniformLocation(pg,"uWheelSpin");usl=GLES20.glGetUniformLocation(pg,"uStabL");usr=GLES20.glGetUniformLocation(pg,"uStabR");url=GLES20.glGetUniformLocation(pg,"uRudderL");urr=GLES20.glGetUniformLocation(pg,"uRudderR");ufl=GLES20.glGetUniformLocation(pg,"uFlapL");ufr=GLES20.glGetUniformLocation(pg,"uFlapR");uvec=GLES20.glGetUniformLocation(pg,"uVector");
            ProceduralFighterMesh.Mesh m=RealisticFighterMesh.build();vb=buffer(m.data);count=m.vertexCount();float[] stores=VisualOrdnanceMesh.build();ob=buffer(stores);ordnanceCount=stores.length/7;last=System.nanoTime();
        }
        private FloatBuffer buffer(float[] data){FloatBuffer b=ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(data).position(0);return b;}
        @Override public void onSurfaceChanged(GL10 g,int w,int h){GLES20.glViewport(0,0,w,h);aspect=(float)w/Math.max(1,h);}

        @Override public void onDrawFrame(GL10 g){
            long n=System.nanoTime();float dt=Math.min(.05f,Math.max(.001f,(n-last)/1e9f));last=n;t+=dt;
            float k=1-(float)Math.exp(-dt*8),kg=1-(float)Math.exp(-dt*2.2),ks=1-(float)Math.exp(-dt*11);
            roll+=(tr-roll)*k;pitch+=(tp-pitch)*k;yaw+=shortest(ty-yaw)*k*.65f;gear+=(tg-gear)*kg;mc+=(tm-mc)*kg;nc+=(tn-nc)*kg;sl+=(tsl-sl)*ks;sr+=(tsr-sr)*ks;rl+=(trl-rl)*ks;rrd+=(trr-rrd)*ks;fl+=(tfl-fl)*ks;fr+=(tfr-fr)*ks;vec+=(tvec-vec)*ks;
            if(gear>.72f)spin=(spin+ws*dt*2.55f)%6.2831855f;
            float sp=cl(speed/270f,0,1),fov=31f+sp*5f;Matrix.perspectiveM(pr,0,fov,aspect,.08f,220f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);camera(sp);Matrix.multiplyMM(vp,0,pr,0,vw,0);Matrix.setIdentityM(md,0);
            float shake=(onGround?cl(speed/90f,0,1)*.10f:cl((speed-150f)/190f,0,1)*.028f);float sx=(float)Math.sin(t*31f)*shake+(float)Math.sin(t*47f)*shake*.35f;Matrix.translateM(md,0,sx,sx*.30f,0);float runwayRelativeYaw=shortest(yaw-270f);Matrix.rotateM(md,0,-runwayRelativeYaw*.12f,0,1,0);Matrix.rotateM(md,0,pitch+vertical*.007f,1,0,0);Matrix.rotateM(md,0,-roll,0,0,1);Matrix.multiplyMM(mvp,0,vp,0,md,0);
            GLES20.glUseProgram(pg);GLES20.glUniformMatrix4fv(umvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(umodel,1,false,md,0);GLES20.glUniform4f(uc,.33f,.35f,.37f,1);GLES20.glUniform3f(ul,-.34f,.88f,-.31f);GLES20.glUniform1f(ut,live?thr:0);GLES20.glUniform1f(ug,gear);GLES20.glUniform1f(um,mc);GLES20.glUniform1f(un,nc);GLES20.glUniform1f(uws,spin);GLES20.glUniform1f(usl,sl);GLES20.glUniform1f(usr,sr);GLES20.glUniform1f(url,rl);GLES20.glUniform1f(urr,rrd);GLES20.glUniform1f(ufl,fl);GLES20.glUniform1f(ufr,fr);GLES20.glUniform1f(uvec,vec);bindAndDraw(vb,count);bindAndDraw(ob,ordnanceCount);
        }
        private void bindAndDraw(FloatBuffer b,int vertices){if(b==null||vertices<=0)return;b.position(0);GLES20.glEnableVertexAttribArray(ap);GLES20.glVertexAttribPointer(ap,3,GLES20.GL_FLOAT,false,28,b);b.position(3);GLES20.glEnableVertexAttribArray(an);GLES20.glVertexAttribPointer(an,3,GLES20.GL_FLOAT,false,28,b);b.position(6);GLES20.glEnableVertexAttribArray(apart);GLES20.glVertexAttribPointer(apart,1,GLES20.GL_FLOAT,false,28,b);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertices);}
        void camera(float sp){float lag=sp*1.55f,bob=(float)Math.sin(t*1.5f)*.025f*(1-sp);if(cam==1)Matrix.setLookAtM(vw,0,0,1.55f,12.9f+lag,0,.00f,1.45f,0,1,0);else if(cam==2)Matrix.setLookAtM(vw,0,11.9f+sp*.7f,4.65f,12.7f+lag,0,.00f,.18f,0,1,0);else if(cam==3)Matrix.setLookAtM(vw,0,-11.9f-sp*.7f,4.65f,12.7f+lag,0,.00f,.18f,0,1,0);else Matrix.setLookAtM(vw,0,0,4.85f+bob,18.5f+lag,0,.10f,-.68f-sp*.22f,0,1,0);}
        static float shortest(float d){while(d>180)d-=360;while(d<-180)d+=360;return d;}static float cl(float v,float a,float b){return Math.max(a,Math.min(b,v));}
        static int sh(int type,String src){int x=GLES20.glCreateShader(type);GLES20.glShaderSource(x,src);GLES20.glCompileShader(x);int[] ok=new int[1];GLES20.glGetShaderiv(x,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(x));return x;}
        static int prog(String v,String f){int p=GLES20.glCreateProgram();GLES20.glAttachShader(p,sh(GLES20.GL_VERTEX_SHADER,v));GLES20.glAttachShader(p,sh(GLES20.GL_FRAGMENT_SHADER,f));GLES20.glLinkProgram(p);int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(p));return p;}

        static final String VS="uniform mat4 uMvp,uModel;uniform float uThrottle,uGear,uMainComp,uNoseComp,uWheelSpin,uStabL,uStabR,uRudderL,uRudderR,uFlapL,uFlapR,uVector;attribute vec3 aPos,aNormal;attribute float aPart;varying vec3 vN,vPos;varying float vP,vR;mat2 rr(float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c);}void main(){vec3 p=aPos,n=aNormal;vR=0.;float d=.0174532925;if(aPart>3.5&&aPart<4.5){float a=uStabL*d;vec2 piv=vec2(.30,1.78);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}else if(aPart>4.5&&aPart<5.5){float a=uStabR*d;vec2 piv=vec2(.30,1.78);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}else if(aPart>5.5&&aPart<6.5){float a=uRudderL*d;vec2 piv=vec2(-.94,2.38);p.xz=rr(a)*(p.xz-piv)+piv;n.xz=rr(a)*n.xz;}else if(aPart>6.5&&aPart<7.5){float a=uRudderR*d;vec2 piv=vec2(.94,2.38);p.xz=rr(a)*(p.xz-piv)+piv;n.xz=rr(a)*n.xz;}else if(aPart>8.5&&aPart<9.5){float a=uFlapL*d;vec2 piv=vec2(.22,.70);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}else if(aPart>9.5&&aPart<10.5){float a=uFlapR*d;vec2 piv=vec2(.22,.70);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}if((aPart>1.5&&aPart<2.5)||(aPart>7.5&&aPart<8.5)||(aPart>11.5&&aPart<12.5)||(aPart>20.5&&aPart<22.5)||(aPart>27.5&&aPart<28.5)){float a=uVector*d;vec2 piv=vec2(-.10,3.18);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}if((aPart>12.5&&aPart<15.5)||(aPart>23.5&&aPart<24.5)){float nose=step(p.z,-2.),r=1.-uGear,fold=smoothstep(.08,.82,r);vR=r;if(aPart>13.5&&aPart<14.5&&uGear>.70){vec2 ctr=nose>.5?vec2(-1.62,-3.78):vec2(-1.67,1.18);p.yz=rr(uWheelSpin)*(p.yz-ctr)+ctr;}if(nose>.5){vec2 piv=vec2(-.46,-3.76),q=p.yz-piv;q=rr(-1.34*fold)*q;p.yz=piv+q;p.x*=mix(1.,.55,fold);p.y=min(p.y,-.40);}else{float s=p.x<0.?-1.:1.;vec2 piv=vec2(1.34*s,-.38),q=vec2(p.x,p.y)-piv;q=rr(-s*1.14*fold)*q;p.x=piv.x+q.x;p.y=piv.y+q.y;p.x=mix(p.x,.72*s,smoothstep(.68,.96,r));p.z=mix(p.z,.94+(p.z-.94)*.42,fold);p.y=min(p.y,-.38);}if(aPart>14.5&&aPart<15.5){float op=smoothstep(.03,.18,r)*(1.-smoothstep(.62,.92,r));p.x+=(p.x<0.?-.24:.24)*op;p.y-=.08*op;}}vN=normalize(mat3(uModel)*n);vP=aPart;vPos=p;gl_Position=uMvp*vec4(p,1.);}";
        static final String FS="precision mediump float;uniform vec4 uColor;uniform vec3 uLightDir;uniform float uThrottle;varying vec3 vN,vPos;varying float vP,vR;void main(){vec3 N=normalize(vN),L=normalize(uLightDir),V=normalize(vec3(.0,.22,1.0)),H=normalize(L+V);float nd=max(dot(N,L),0.),hemi=.28+.18*(N.y*.5+.5),rim=pow(1.-abs(N.z),3.0),spec=pow(max(dot(N,H),0.),28.);float li=hemi+.62*nd,a=1.,sm=.07;vec3 c=uColor.rgb;if(vP<.5){float upper=smoothstep(-.15,.48,N.y);c=mix(vec3(.255,.270,.285),vec3(.365,.380,.395),upper);float panel=.008*sin(vPos.z*14.7+vPos.x*8.3)+.006*sin(vPos.z*27.1-vPos.x*5.2);c+=vec3(panel)+vec3(.018)*rim;li=.34+.68*nd+.12*(N.y*.5+.5);sm=.10;}else if(vP>.5&&vP<1.5){float fr=pow(1.-max(dot(N,V),0.),2.5);c=mix(vec3(.018,.055,.078),vec3(.08,.22,.29),fr);li=.55+.28*nd+.20*rim;sm=.52;a=.90;}else if(vP>10.5&&vP<11.5){c=vec3(.045,.050,.055);li=.43+.57*nd;sm=.16;}else if(vP>22.5&&vP<23.5){c=vec3(.305,.322,.338);li=.38+.70*nd;sm=.12;}else if(vP>12.5&&vP<13.5){c=vec3(.52,.55,.57);li=.50+.50*nd;sm=.36;}else if(vP>13.5&&vP<14.5){c=vec3(.010,.011,.012);li=.70+.16*nd;sm=.03;}else if(vP>14.5&&vP<15.5){c=vec3(.22,.23,.24);li=.46+.54*nd;sm=.22;}else if(vP>15.5&&vP<18.5){c=vec3(.025,.028,.032);li=.46+.42*nd;sm=.10;}else if(vP>18.5&&vP<20.5){c=vec3(.055,.060,.064);li=.34+.50*nd;sm=.10;}else if(vP>1.5&&vP<3.5){c=vec3(.155,.165,.175);li=.35+.70*nd;sm=.30;}else if(vP>7.5&&vP<8.5){c=mix(vec3(.06,.18,.70),vec3(1.,.18,.015),uThrottle);li=.78+1.35*uThrottle;sm=.05;a=.84;}else if(vP>20.5&&vP<22.5){c=vec3(.18,.17,.16);li=.40+.58*nd;sm=.34;}else if(vP>24.5&&vP<25.5){c=vec3(1.0,.025,.015);li=1.35;sm=.0;}else if(vP>25.5&&vP<26.5){c=vec3(.02,1.0,.16);li=1.25;sm=.0;}else if(vP>26.5&&vP<27.5){c=vec3(1.0,.92,.72);li=1.22;sm=.0;}else if(vP>27.5&&vP<28.5){c=vec3(.16,.17,.18);li=.42+.66*nd;sm=.42;}else if(vP>28.5&&vP<29.5){c=vec3(.10,.11,.12);li=.42+.58*nd;sm=.20;}else if(vP>29.5&&vP<30.5){c=vec3(.19,.20,.21);li=.44+.58*nd;sm=.14;}else if(vP>30.5&&vP<31.5){c=vec3(.35,.36,.34);li=.42+.62*nd;sm=.18;}else if(vP>31.5&&vP<32.5){c=vec3(.18,.19,.18);li=.38+.64*nd;sm=.18;}if(((vP>12.5&&vP<14.5)||(vP>23.5&&vP<24.5)))a*=1.-smoothstep(.78,.94,vR);vec3 outc=c*li+vec3(spec*sm);gl_FragColor=vec4(outc,a);}";
    }
    private static float cl(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
