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

/** AVM-13.6 layered exterior renderer with procedural PBR-like materials and transparent canopy pass. */
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
        float roll,pitch,yaw,gear=1,mc,nc,spin,aspect=1.7f,t,sl,sr,rl,rrd,fl,fr,vec,camX,camY,camZ;
        int pg,ap,an,apart,umvp,umodel,uc,ul,ut,ug,um,un,uws,usl,usr,url,urr,ufl,ufr,uvec,utime,ucam;
        FloatBuffer vbOpaque,vbCanopy,obOpaque,obGlass; int opaqueCount,canopyCount,ordnanceCount,glassCount; long last;

        void tele(float a,float b,float c,float d,boolean x){tr=a;tp=b;ty=c;thr=cl(d,0,1);live=x;}
        synchronized void controls(float p,float r,float y,float th){map.update(p,r,y,th);tsl=map.leftStabilatorDeg;tsr=map.rightStabilatorDeg;trl=map.leftRudderDeg;trr=map.rightRudderDeg;tfl=map.leftFlaperonDeg;tfr=map.rightFlaperonDeg;tvec=cl(p,-1,1)*8f;}
        void sim(float g,float m,float n){tg=cl(g,0,1);tm=cl(m,0,1);tn=cl(n,0,1);}

        @Override public void onSurfaceCreated(GL10 g,EGLConfig e){
            GLES20.glClearColor(0,0,0,0);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glDepthFunc(GLES20.GL_LEQUAL);GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glCullFace(GLES20.GL_BACK);GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
            pg=prog(VS,FS);ap=GLES20.glGetAttribLocation(pg,"aPos");an=GLES20.glGetAttribLocation(pg,"aNormal");apart=GLES20.glGetAttribLocation(pg,"aPart");umvp=GLES20.glGetUniformLocation(pg,"uMvp");umodel=GLES20.glGetUniformLocation(pg,"uModel");uc=GLES20.glGetUniformLocation(pg,"uColor");ul=GLES20.glGetUniformLocation(pg,"uLightDir");ut=GLES20.glGetUniformLocation(pg,"uThrottle");ug=GLES20.glGetUniformLocation(pg,"uGear");um=GLES20.glGetUniformLocation(pg,"uMainComp");un=GLES20.glGetUniformLocation(pg,"uNoseComp");uws=GLES20.glGetUniformLocation(pg,"uWheelSpin");usl=GLES20.glGetUniformLocation(pg,"uStabL");usr=GLES20.glGetUniformLocation(pg,"uStabR");url=GLES20.glGetUniformLocation(pg,"uRudderL");urr=GLES20.glGetUniformLocation(pg,"uRudderR");ufl=GLES20.glGetUniformLocation(pg,"uFlapL");ufr=GLES20.glGetUniformLocation(pg,"uFlapR");uvec=GLES20.glGetUniformLocation(pg,"uVector");utime=GLES20.glGetUniformLocation(pg,"uTime");ucam=GLES20.glGetUniformLocation(pg,"uCameraPos");
            ProceduralFighterMesh.Mesh m=RealisticFighterMesh.build();float[][] ms=splitPart(m.data,1f);vbOpaque=buffer(ms[0]);opaqueCount=ms[0].length/7;vbCanopy=buffer(ms[1]);canopyCount=ms[1].length/7;
            float[] stores=VisualOrdnanceMesh.build();float[][] os=splitPart(stores,36f);obOpaque=buffer(os[0]);ordnanceCount=os[0].length/7;obGlass=buffer(os[1]);glassCount=os[1].length/7;last=System.nanoTime();
        }
        private FloatBuffer buffer(float[] data){FloatBuffer b=ByteBuffer.allocateDirect(Math.max(1,data.length)*4).order(ByteOrder.nativeOrder()).asFloatBuffer();if(data.length>0)b.put(data);b.position(0);return b;}
        private static float[][] splitPart(float[] src,float target){int a=0,b=0;for(int i=0;i<src.length;i+=7){if(Math.abs(src[i+6]-target)<.1f)b+=7;else a+=7;}float[] opaque=new float[a],special=new float[b];int oi=0,si=0;for(int i=0;i<src.length;i+=7){boolean hit=Math.abs(src[i+6]-target)<.1f;float[] dst=hit?special:opaque;int d=hit?si:oi;System.arraycopy(src,i,dst,d,7);if(hit)si+=7;else oi+=7;}return new float[][]{opaque,special};}
        @Override public void onSurfaceChanged(GL10 g,int w,int h){GLES20.glViewport(0,0,w,h);aspect=(float)w/Math.max(1,h);}

        @Override public void onDrawFrame(GL10 g){
            long n=System.nanoTime();float dt=Math.min(.05f,Math.max(.001f,(n-last)/1e9f));last=n;t+=dt;
            float k=1-(float)Math.exp(-dt*8),kg=1-(float)Math.exp(-dt*2.2),ks=1-(float)Math.exp(-dt*11);
            roll+=(tr-roll)*k;pitch+=(tp-pitch)*k;yaw+=shortest(ty-yaw)*k*.65f;gear+=(tg-gear)*kg;mc+=(tm-mc)*kg;nc+=(tn-nc)*kg;sl+=(tsl-sl)*ks;sr+=(tsr-sr)*ks;rl+=(trl-rl)*ks;rrd+=(trr-rrd)*ks;fl+=(tfl-fl)*ks;fr+=(tfr-fr)*ks;vec+=(tvec-vec)*ks;
            if(gear>.72f)spin=(spin+ws*dt*2.55f)%6.2831855f;
            float sp=cl(speed/270f,0,1),fov=31f+sp*5f;Matrix.perspectiveM(pr,0,fov,aspect,.08f,220f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);camera(sp);Matrix.multiplyMM(vp,0,pr,0,vw,0);Matrix.setIdentityM(md,0);
            float shake=(onGround?cl(speed/90f,0,1)*.10f:cl((speed-150f)/190f,0,1)*.028f),sx=(float)Math.sin(t*31f)*shake+(float)Math.sin(t*47f)*shake*.35f;Matrix.translateM(md,0,sx,sx*.30f,0);float runwayRelativeYaw=shortest(yaw-270f);Matrix.rotateM(md,0,-runwayRelativeYaw*.12f,0,1,0);Matrix.rotateM(md,0,pitch+vertical*.007f,1,0,0);Matrix.rotateM(md,0,-roll,0,0,1);Matrix.multiplyMM(mvp,0,vp,0,md,0);
            GLES20.glUseProgram(pg);GLES20.glUniformMatrix4fv(umvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(umodel,1,false,md,0);GLES20.glUniform4f(uc,.34f,.36f,.38f,1);GLES20.glUniform3f(ul,-.34f,.88f,-.31f);GLES20.glUniform3f(ucam,camX,camY,camZ);GLES20.glUniform1f(ut,thr);GLES20.glUniform1f(utime,t);GLES20.glUniform1f(ug,gear);GLES20.glUniform1f(um,mc);GLES20.glUniform1f(un,nc);GLES20.glUniform1f(uws,spin);GLES20.glUniform1f(usl,sl);GLES20.glUniform1f(usr,sr);GLES20.glUniform1f(url,rl);GLES20.glUniform1f(urr,rrd);GLES20.glUniform1f(ufl,fl);GLES20.glUniform1f(ufr,fr);GLES20.glUniform1f(uvec,vec);
            bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(obOpaque,ordnanceCount);
            GLES20.glDepthMask(false);GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(obGlass,glassCount);bindAndDraw(vbCanopy,canopyCount);GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glDepthMask(true);
        }
        private void bindAndDraw(FloatBuffer b,int vertices){if(b==null||vertices<=0)return;b.position(0);GLES20.glEnableVertexAttribArray(ap);GLES20.glVertexAttribPointer(ap,3,GLES20.GL_FLOAT,false,28,b);b.position(3);GLES20.glEnableVertexAttribArray(an);GLES20.glVertexAttribPointer(an,3,GLES20.GL_FLOAT,false,28,b);b.position(6);GLES20.glEnableVertexAttribArray(apart);GLES20.glVertexAttribPointer(apart,1,GLES20.GL_FLOAT,false,28,b);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertices);}
        void camera(float sp){float lag=sp*1.55f,bob=(float)Math.sin(t*1.5f)*.025f*(1-sp);if(cam==1){camX=0;camY=1.55f;camZ=12.9f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.00f,1.45f,0,1,0);}else if(cam==2){camX=11.9f+sp*.7f;camY=4.65f;camZ=12.7f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.00f,.18f,0,1,0);}else if(cam==3){camX=-11.9f-sp*.7f;camY=4.65f;camZ=12.7f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.00f,.18f,0,1,0);}else{camX=0;camY=4.85f+bob;camZ=18.5f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.10f,-.68f-sp*.22f,0,1,0);}}
        static float shortest(float d){while(d>180)d-=360;while(d<-180)d+=360;return d;}static float cl(float v,float a,float b){return Math.max(a,Math.min(b,v));}
        static int sh(int type,String src){int x=GLES20.glCreateShader(type);GLES20.glShaderSource(x,src);GLES20.glCompileShader(x);int[] ok=new int[1];GLES20.glGetShaderiv(x,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(x));return x;}
        static int prog(String v,String f){int p=GLES20.glCreateProgram();GLES20.glAttachShader(p,sh(GLES20.GL_VERTEX_SHADER,v));GLES20.glAttachShader(p,sh(GLES20.GL_FRAGMENT_SHADER,f));GLES20.glLinkProgram(p);int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(p));return p;}

        static final String VS="uniform mat4 uMvp,uModel;uniform float uThrottle,uGear,uMainComp,uNoseComp,uWheelSpin,uStabL,uStabR,uRudderL,uRudderR,uFlapL,uFlapR,uVector;attribute vec3 aPos,aNormal;attribute float aPart;varying vec3 vN,vPos;varying float vP,vR;mat2 rr(float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c);}void main(){vec3 p=aPos,n=aNormal;vR=0.;float d=.0174532925;if(aPart>3.5&&aPart<4.5){float a=uStabL*d;vec2 piv=vec2(.30,1.78);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}else if(aPart>4.5&&aPart<5.5){float a=uStabR*d;vec2 piv=vec2(.30,1.78);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}else if(aPart>5.5&&aPart<6.5){float a=uRudderL*d;vec2 piv=vec2(-.94,2.38);p.xz=rr(a)*(p.xz-piv)+piv;n.xz=rr(a)*n.xz;}else if(aPart>6.5&&aPart<7.5){float a=uRudderR*d;vec2 piv=vec2(.94,2.38);p.xz=rr(a)*(p.xz-piv)+piv;n.xz=rr(a)*n.xz;}else if(aPart>8.5&&aPart<9.5){float a=uFlapL*d;vec2 piv=vec2(.22,.70);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}else if(aPart>9.5&&aPart<10.5){float a=uFlapR*d;vec2 piv=vec2(.22,.70);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}if((aPart>1.5&&aPart<2.5)||(aPart>7.5&&aPart<8.5)||(aPart>11.5&&aPart<12.5)||(aPart>20.5&&aPart<22.5)||(aPart>27.5&&aPart<28.5)){float a=uVector*d;vec2 piv=vec2(-.10,3.18);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}if((aPart>12.5&&aPart<15.5)||(aPart>23.5&&aPart<24.5)){float nose=step(p.z,-2.),r=1.-uGear,fold=smoothstep(.08,.82,r);vR=r;if(aPart>13.5&&aPart<14.5&&uGear>.70){vec2 ctr=nose>.5?vec2(-1.62,-3.78):vec2(-1.67,1.18);p.yz=rr(uWheelSpin)*(p.yz-ctr)+ctr;}if(nose>.5){vec2 piv=vec2(-.46,-3.76),q=p.yz-piv;q=rr(-1.34*fold)*q;p.yz=piv+q;p.x*=mix(1.,.55,fold);p.y=min(p.y,-.40);}else{float s=p.x<0.?-1.:1.;vec2 piv=vec2(1.34*s,-.38),q=vec2(p.x,p.y)-piv;q=rr(-s*1.14*fold)*q;p.x=piv.x+q.x;p.y=piv.y+q.y;p.x=mix(p.x,.72*s,smoothstep(.68,.96,r));p.z=mix(p.z,.94+(p.z-.94)*.42,fold);p.y=min(p.y,-.38);}if(aPart>14.5&&aPart<15.5){float op=smoothstep(.03,.18,r)*(1.-smoothstep(.62,.92,r));p.x+=(p.x<0.?-.24:.24)*op;p.y-=.08*op;}}vN=normalize(mat3(uModel)*n);vP=aPart;vPos=p;gl_Position=uMvp*vec4(p,1.);}";
        static final String FS="precision mediump float;uniform vec4 uColor;uniform vec3 uLightDir,uCameraPos;uniform float uThrottle,uTime;varying vec3 vN,vPos;varying float vP,vR;float sat(float x){return clamp(x,0.,1.);}vec3 envc(vec3 r){float k=sat(r.y*.5+.5);return mix(vec3(.075,.080,.075),vec3(.25,.43,.61),k);}void main(){vec3 N=normalize(vN),V=normalize(uCameraPos-vPos),L=normalize(uLightDir);float micro=sin(vPos.x*67.+vPos.z*43.)*sin(vPos.y*53.-vPos.z*29.);if(vP<.5)N=normalize(N+vec3(micro*.018,micro*.010,0.));float ndl=max(dot(N,L),0.),ndv=max(dot(N,V),0.);vec3 H=normalize(L+V);float ndh=max(dot(N,H),0.);vec3 base=uColor.rgb,emitc=vec3(0.);float rough=.55,metal=.10,ao=.92,alpha=1.;if(vP<.5){float upper=smoothstep(-.20,.55,N.y),grain=.006*sin(vPos.z*31.+vPos.x*17.)+.004*sin(vPos.z*73.-vPos.x*37.);base=mix(vec3(.245,.258,.272),vec3(.375,.392,.410),upper)+vec3(grain);float seam=smoothstep(.487,.5,abs(fract(vPos.z*.78+vPos.x*.075)-.5));base*=1.-.035*seam;rough=.46+.08*seam;metal=.24;ao=.94;}else if(vP>.5&&vP<1.5){vec3 R=reflect(-V,N);float fr=pow(1.-ndv,3.0);vec3 glass=mix(vec3(.010,.034,.050),envc(R),.42+.34*fr);glass+=vec3(.32,.43,.48)*pow(ndh,92.)*.55;gl_FragColor=vec4(glass,.26+.25*fr);return;}else if(vP>1.5&&vP<2.5){base=vec3(.16,.17,.18);rough=.30;metal=.82;}else if(vP>2.5&&vP<3.5){base=vec3(.095,.105,.112);rough=.58;metal=.36;}else if(vP>10.5&&vP<11.5){base=vec3(.035,.040,.045);rough=.48;metal=.42;}else if(vP>12.5&&vP<13.5){base=vec3(.55,.57,.585);rough=.24;metal=.88;}else if(vP>13.5&&vP<14.5){base=vec3(.012,.013,.014);rough=.88;metal=.02;}else if(vP>14.5&&vP<15.5){base=vec3(.235,.245,.255);rough=.40;metal=.66;}else if(vP>15.5&&vP<18.5){base=vec3(.026,.030,.034);rough=.68;metal=.18;}else if(vP>18.5&&vP<20.5){base=vec3(.050,.057,.062);rough=.72;metal=.18;}else if(vP>20.5&&vP<21.5){base=vec3(.13,.135,.14);rough=.34;metal=.86;}else if(vP>21.5&&vP<22.5){float flick=.72+.28*sin(uTime*26.+vPos.z*18.);base=vec3(.12,.035,.012);emitc=mix(vec3(.10,.16,.80),vec3(1.0,.20,.015),uThrottle)*uThrottle*(1.5+.8*flick);rough=.18;metal=.25;alpha=.68+.25*uThrottle;}else if(vP>22.5&&vP<23.5){base=vec3(.31,.325,.34);rough=.48;metal=.38;}else if(vP>24.5&&vP<25.5){emitc=vec3(1.,.025,.015)*1.3;base=vec3(.15,0.,0.);rough=.2;}else if(vP>25.5&&vP<26.5){emitc=vec3(.02,1.,.16)*1.2;base=vec3(0.,.12,.02);rough=.2;}else if(vP>26.5&&vP<27.5){emitc=vec3(1.,.92,.72)*1.15;base=vec3(.12,.10,.07);rough=.2;}else if(vP>27.5&&vP<28.5){float heat=sat((vPos.z-2.65)/1.1);base=mix(vec3(.17,.175,.18),vec3(.24,.125,.065),heat*.55);rough=.34;metal=.86;}else if(vP>28.5&&vP<29.5){base=vec3(.105,.115,.123);rough=.48;metal=.52;}else if(vP>29.5&&vP<30.5){base=vec3(.19,.205,.215);rough=.46;metal=.52;}else if(vP>30.5&&vP<31.5){base=vec3(.34,.35,.335);rough=.58;metal=.22;}else if(vP>31.5&&vP<32.5){base=vec3(.17,.18,.175);rough=.56;metal=.26;}else if(vP>32.5&&vP<33.5){base=vec3(.17,.22,.18);rough=.82;metal=.02;}else if(vP>33.5&&vP<34.5){base=vec3(.58,.60,.59);rough=.38;metal=.15;}else if(vP>34.5&&vP<35.5){vec3 R=reflect(-V,N);base=mix(vec3(.012,.016,.020),envc(R),.42);rough=.12;metal=.72;}else if(vP>35.5&&vP<36.5){vec3 R=reflect(-V,N);float fr=pow(1.-ndv,3.);vec3 hud=mix(vec3(.015,.12,.075),envc(R),.16);gl_FragColor=vec4(hud+vec3(.02,.20,.09)*.20,.18+.16*fr);return;}else if(vP>36.5&&vP<37.5){base=vec3(.015,.025,.020);emitc=vec3(.02,.42,.17)*(.65+.15*sin(uTime*2.));rough=.30;metal=.10;}else if(vP>7.5&&vP<8.5){float flick=.75+.25*sin(uTime*23.+vPos.z*15.);base=vec3(.06,.03,.02);emitc=mix(vec3(.06,.16,.72),vec3(1.,.16,.01),uThrottle)*uThrottle*(1.3+.7*flick);rough=.22;metal=.12;alpha=.48+.38*uThrottle;}if(((vP>12.5&&vP<14.5)||(vP>23.5&&vP<24.5)))alpha*=1.-smoothstep(.78,.94,vR);vec3 F0=mix(vec3(.035),base,metal);float fres=pow(1.-ndv,5.);vec3 F=mix(F0,vec3(1.),fres);float spow=mix(110.,9.,rough),sp=pow(ndh,spow)*(1.-rough*.62);vec3 diff=base*(1.-metal)*(.18+.82*ndl)*ao;vec3 R=reflect(-V,N),env=envc(R);vec3 spec=F*(sp*(.38+.62*ndl)+env*(.08+.38*(1.-rough)));vec3 outc=diff+spec+emitc;gl_FragColor=vec4(outc,alpha);}";
    }
    private static float cl(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
