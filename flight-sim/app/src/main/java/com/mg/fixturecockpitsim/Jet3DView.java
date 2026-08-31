package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import com.mg.fixturecockpitsim.visual.AdvancedAirframeOverlay;
import com.mg.fixturecockpitsim.visual.AircraftControlSurfaces;
import com.mg.fixturecockpitsim.visual.ProceduralFighterMesh;
import com.mg.fixturecockpitsim.visual.RealisticFighterMesh;
import com.mg.fixturecockpitsim.visual.VisualOrdnanceMesh;
import com.mg.fixturecockpitsim.visual.WingtipVortexMesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** AVM-15.0 premium renderer: animated nozzle/afterburner, takeoff bird and water-impact submergence. */
public final class Jet3DView extends GLSurfaceView {
    public static final int CAMERA_CHASE=0, CAMERA_REAR=1, CAMERA_RIGHT_QUARTER=2, CAMERA_LEFT_QUARTER=3;
    private final R r;
    private final FlightSoundEngine sound=new FlightSoundEngine();
    private float st=.6f,sg=1f,sb;
    private boolean ground=true;

    public Jet3DView(Context c){
        super(c);setEGLContextClientVersion(2);setEGLConfigChooser(8,8,8,8,24,0);getHolder().setFormat(PixelFormat.TRANSLUCENT);setZOrderOnTop(true);
        r=new R();setRenderer(r);setRenderMode(RENDERMODE_CONTINUOUSLY);
    }
    public void setTelemetry(float roll,float pitch,float yaw,float throttle,float linkHz,int drops,boolean live){r.tele(roll,pitch,yaw,throttle,live);st=cl(throttle,0,1);sound.update(st,st*230,sg,sb,ground);}
    public void setControlInputs(float pitch,float roll,float yaw,float throttle){r.controls(pitch,roll,yaw,throttle);}
    public void setSimulationState(float gear,float mainComp,float noseComp,float brake,boolean onGround){r.sim(gear,mainComp,noseComp);sg=gear;sb=brake;ground=onGround;sound.update(st,st*230,sg,sb,ground);}
    public void setFlightMotion(float speed,float vertical,boolean onGround){r.speed=Math.max(0,speed);r.vertical=vertical;r.onGround=onGround;}
    public void setWheelSpeed(float v){r.ws=Math.max(0,v);}
    public void setCameraMode(int m){r.cam=Math.max(0,Math.min(3,m));}
    public int getCameraMode(){return r.cam;}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP)r.cam=(r.cam+1)%4;return true;}
    @Override public void onResume(){super.onResume();sound.start();}
    @Override public void onPause(){sound.stop();super.onPause();}

    private static final class R implements Renderer {
        final float[] pr=new float[16],vw=new float[16],md=new float[16],vp=new float[16],mvp=new float[16];
        final AircraftControlSurfaces map=new AircraftControlSurfaces();
        volatile float tr,tp,ty,thr=.6f,tg=1,tm,tn,ws,speed,vertical,tsl,tsr,trl,trr,tfl,tfr,tvec;
        volatile boolean onGround=true,live;volatile int cam;
        float roll,pitch,yaw,gear=1,mc,nc,spin,aspect=1.7f,t,sl,sr,rl,rrd,fl,fr,vec,camX,camY,camZ;
        float birdAge=-1f;boolean birdTriggered;
        int pg,ap,an,apart,umvp,umodel,uc,ul,ut,ug,um,un,uws,usl,usr,url,urr,ufl,ufr,uvec,utime,ucam,uspeed,uair,uroll,ubird;
        FloatBuffer vbOpaque,vbCanopy,detailBuffer,obOpaque,obGlass,vortexBuffer,birdBuffer;
        int opaqueCount,canopyCount,detailCount,ordnanceCount,glassCount,vortexCount,birdCount;
        long last;

        void tele(float a,float b,float c,float d,boolean x){tr=a;tp=b;ty=c;thr=cl(d,0,1);live=x;}
        synchronized void controls(float p,float r,float y,float th){map.update(p,r,y,th);tsl=map.leftStabilatorDeg;tsr=map.rightStabilatorDeg;trl=map.leftRudderDeg;trr=map.rightRudderDeg;tfl=map.leftFlaperonDeg;tfr=map.rightFlaperonDeg;tvec=cl(p,-1,1)*8f;}
        void sim(float g,float m,float n){tg=cl(g,0,1);tm=cl(m,0,1);tn=cl(n,0,1);}

        @Override public void onSurfaceCreated(GL10 g,EGLConfig e){
            GLES20.glClearColor(0,0,0,0);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glDepthFunc(GLES20.GL_LEQUAL);GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glCullFace(GLES20.GL_BACK);GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
            pg=prog(VS,FS);ap=GLES20.glGetAttribLocation(pg,"aPos");an=GLES20.glGetAttribLocation(pg,"aNormal");apart=GLES20.glGetAttribLocation(pg,"aPart");
            umvp=GLES20.glGetUniformLocation(pg,"uMvp");umodel=GLES20.glGetUniformLocation(pg,"uModel");uc=GLES20.glGetUniformLocation(pg,"uColor");ul=GLES20.glGetUniformLocation(pg,"uLightDir");ut=GLES20.glGetUniformLocation(pg,"uThrottle");ug=GLES20.glGetUniformLocation(pg,"uGear");um=GLES20.glGetUniformLocation(pg,"uMainComp");un=GLES20.glGetUniformLocation(pg,"uNoseComp");uws=GLES20.glGetUniformLocation(pg,"uWheelSpin");usl=GLES20.glGetUniformLocation(pg,"uStabL");usr=GLES20.glGetUniformLocation(pg,"uStabR");url=GLES20.glGetUniformLocation(pg,"uRudderL");urr=GLES20.glGetUniformLocation(pg,"uRudderR");ufl=GLES20.glGetUniformLocation(pg,"uFlapL");ufr=GLES20.glGetUniformLocation(pg,"uFlapR");uvec=GLES20.glGetUniformLocation(pg,"uVector");utime=GLES20.glGetUniformLocation(pg,"uTime");ucam=GLES20.glGetUniformLocation(pg,"uCameraPos");uspeed=GLES20.glGetUniformLocation(pg,"uSpeed");uair=GLES20.glGetUniformLocation(pg,"uAirborne");uroll=GLES20.glGetUniformLocation(pg,"uRollDeg");ubird=GLES20.glGetUniformLocation(pg,"uBirdAge");
            ProceduralFighterMesh.Mesh m=RealisticFighterMesh.build();float[][] ms=splitPart(m.data,1f);vbOpaque=buffer(ms[0]);opaqueCount=ms[0].length/7;vbCanopy=buffer(ms[1]);canopyCount=ms[1].length/7;
            float[] detail=AdvancedAirframeOverlay.build();detailBuffer=buffer(detail);detailCount=detail.length/7;float[] stores=VisualOrdnanceMesh.build();float[][] os=splitPart(stores,36f);obOpaque=buffer(os[0]);ordnanceCount=os[0].length/7;obGlass=buffer(os[1]);glassCount=os[1].length/7;float[] vortex=WingtipVortexMesh.build();vortexBuffer=buffer(vortex);vortexCount=vortex.length/7;float[] bird=buildBirdMesh();birdBuffer=buffer(bird);birdCount=bird.length/7;last=System.nanoTime();
        }

        private FloatBuffer buffer(float[] data){FloatBuffer b=ByteBuffer.allocateDirect(Math.max(1,data.length)*4).order(ByteOrder.nativeOrder()).asFloatBuffer();if(data.length>0)b.put(data);b.position(0);return b;}
        private static float[][] splitPart(float[] src,float target){int a=0,b=0;for(int i=0;i<src.length;i+=7){if(Math.abs(src[i+6]-target)<.1f)b+=7;else a+=7;}float[] opaque=new float[a],special=new float[b];int oi=0,si=0;for(int i=0;i<src.length;i+=7){boolean hit=Math.abs(src[i+6]-target)<.1f;float[] dst=hit?special:opaque;int d=hit?si:oi;System.arraycopy(src,i,dst,d,7);if(hit)si+=7;else oi+=7;}return new float[][]{opaque,special};}
        @Override public void onSurfaceChanged(GL10 g,int w,int h){GLES20.glViewport(0,0,w,h);aspect=(float)w/Math.max(1,h);}

        @Override public void onDrawFrame(GL10 g){
            long n=System.nanoTime();float dt=Math.min(.05f,Math.max(.001f,(n-last)/1e9f));last=n;t+=dt;
            float k=1-(float)Math.exp(-dt*8),kg=1-(float)Math.exp(-dt*2.2),ks=1-(float)Math.exp(-dt*11);roll+=(tr-roll)*k;pitch+=(tp-pitch)*k;yaw+=shortest(ty-yaw)*k*.65f;gear+=(tg-gear)*kg;mc+=(tm-mc)*kg;nc+=(tn-nc)*kg;sl+=(tsl-sl)*ks;sr+=(tsr-sr)*ks;rl+=(trl-rl)*ks;rrd+=(trr-rrd)*ks;fl+=(tfl-fl)*ks;fr+=(tfr-fr)*ks;vec+=(tvec-vec)*ks;if(gear>.72f)spin=(spin+ws*dt*2.55f)%6.2831855f;

            String ph=AirfieldWorldView.getSharedPhase();float along=AirfieldWorldView.getSharedAlongTrackM();
            boolean resetBird=(ph.contains("TAXI_OUT")&&speed<5f)||(onGround&&speed<2f&&along<160f);
            if(resetBird){birdTriggered=false;birdAge=-1f;}
            boolean takeoffWindow=ph.contains("TAKEOFF_ROLL")||ph.contains("ROTATE_CLIMB")||(onGround&&speed>28f&&along<700f&&!ph.contains("ROLLOUT")&&!ph.contains("TAXI_IN"));
            if(!birdTriggered&&takeoffWindow&&speed>28f){birdTriggered=true;birdAge=0f;}if(birdTriggered&&birdAge<6.2f)birdAge+=dt;

            float sink=CinematicEnvironmentView.isWaterCrashActive()?CinematicEnvironmentView.getWaterCrashProgress():0f;
            float sp=cl(speed/270f,0,1),fov=29.5f+sp*5.0f;Matrix.perspectiveM(pr,0,fov,aspect,.08f,240f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            if(sink>=.995f)return;
            camera(sp);Matrix.multiplyMM(vp,0,pr,0,vw,0);Matrix.setIdentityM(md,0);
            float shake=(onGround?cl(speed/90f,0,1)*.085f:cl((speed-155f)/190f,0,1)*.020f),sx=(float)Math.sin(t*31f)*shake+(float)Math.sin(t*47f)*shake*.30f;Matrix.translateM(md,0,sx,sx*.26f,0);
            if(sink>0){float e=sink*sink*(3f-2f*sink);Matrix.translateM(md,0,0,-4.4f*e,0);Matrix.rotateM(md,0,11f*e,1,0,0);Matrix.rotateM(md,0,(float)Math.sin(t*2.4f)*5f*(1-e),0,0,1);}
            float runwayRelativeYaw=shortest(yaw-270f);Matrix.rotateM(md,0,-runwayRelativeYaw*.12f,0,1,0);Matrix.rotateM(md,0,pitch+vertical*.007f,1,0,0);Matrix.rotateM(md,0,-roll,0,0,1);Matrix.multiplyMM(mvp,0,vp,0,md,0);

            GLES20.glUseProgram(pg);GLES20.glUniformMatrix4fv(umvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(umodel,1,false,md,0);GLES20.glUniform4f(uc,.34f,.36f,.38f,1);GLES20.glUniform3f(ul,-.38f,.86f,-.33f);GLES20.glUniform3f(ucam,camX,camY,camZ);GLES20.glUniform1f(ut,thr);GLES20.glUniform1f(utime,t);GLES20.glUniform1f(uspeed,speed);GLES20.glUniform1f(uair,onGround?0f:1f);GLES20.glUniform1f(uroll,roll);GLES20.glUniform1f(ug,gear);GLES20.glUniform1f(um,mc);GLES20.glUniform1f(un,nc);GLES20.glUniform1f(uws,spin);GLES20.glUniform1f(usl,sl);GLES20.glUniform1f(usr,sr);GLES20.glUniform1f(url,rl);GLES20.glUniform1f(urr,rrd);GLES20.glUniform1f(ufl,fl);GLES20.glUniform1f(ufr,fr);GLES20.glUniform1f(uvec,vec);GLES20.glUniform1f(ubird,birdAge);

            bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);bindAndDraw(obOpaque,ordnanceCount);if(birdAge>=0f&&birdAge<5.8f)bindAndDraw(birdBuffer,birdCount);
            GLES20.glDepthMask(false);GLES20.glDisable(GLES20.GL_CULL_FACE);bindAndDraw(vortexBuffer,vortexCount);bindAndDraw(obGlass,glassCount);bindAndDraw(vbCanopy,canopyCount);GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glDepthMask(true);
        }

        private void bindAndDraw(FloatBuffer b,int vertices){if(b==null||vertices<=0)return;b.position(0);GLES20.glEnableVertexAttribArray(ap);GLES20.glVertexAttribPointer(ap,3,GLES20.GL_FLOAT,false,28,b);b.position(3);GLES20.glEnableVertexAttribArray(an);GLES20.glVertexAttribPointer(an,3,GLES20.GL_FLOAT,false,28,b);b.position(6);GLES20.glEnableVertexAttribArray(apart);GLES20.glVertexAttribPointer(apart,1,GLES20.GL_FLOAT,false,28,b);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertices);}
        void camera(float sp){float lag=sp*1.40f,bob=(float)Math.sin(t*1.5f)*.018f*(1-sp);if(cam==1){camX=0;camY=1.50f;camZ=12.8f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.02f,1.42f,0,1,0);}else if(cam==2){camX=11.6f+sp*.7f;camY=4.55f;camZ=12.5f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.12f,0,1,0);}else if(cam==3){camX=-11.6f-sp*.7f;camY=4.55f;camZ=12.5f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.12f,0,1,0);}else{camX=0;camY=4.72f+bob;camZ=18.2f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.10f,-.65f-sp*.20f,0,1,0);}}

        static float shortest(float d){while(d>180)d-=360;while(d<-180)d+=360;return d;}static float cl(float v,float a,float b){return Math.max(a,Math.min(b,v));}
        static int sh(int type,String src){int x=GLES20.glCreateShader(type);GLES20.glShaderSource(x,src);GLES20.glCompileShader(x);int[] ok=new int[1];GLES20.glGetShaderiv(x,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(x));return x;}
        static int prog(String v,String f){int p=GLES20.glCreateProgram();GLES20.glAttachShader(p,sh(GLES20.GL_VERTEX_SHADER,v));GLES20.glAttachShader(p,sh(GLES20.GL_FRAGMENT_SHADER,f));GLES20.glLinkProgram(p);int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(p));return p;}

        static float[] buildBirdMesh(){
            ArrayList<Float> a=new ArrayList<>();float[] nose={0,.02f,-.72f},tail={0,.02f,.68f},top={0,.22f,-.02f},bot={0,-.14f,.05f},left={-.19f,.02f,0},right={.19f,.02f,0};
            tri(a,nose,top,left,41);tri(a,nose,right,top,41);tri(a,nose,left,bot,41);tri(a,nose,bot,right,41);tri(a,tail,left,top,41);tri(a,tail,top,right,41);tri(a,tail,bot,left,41);tri(a,tail,right,bot,41);
            float[] lr={-.10f,.06f,-.10f},lt={-1.55f,.03f,.10f},lb={-.55f,.05f,.52f};tri(a,lr,lt,lb,42);tri(a,lb,lt,lr,42);
            float[] rr={.10f,.06f,-.10f},rt={1.55f,.03f,.10f},rb={.55f,.05f,.52f};tri(a,rr,rb,rt,43);tri(a,rt,rb,rr,43);
            float[] b0={0,.03f,-.80f},b1={-.09f,.01f,-1.05f},b2={.09f,.01f,-1.05f};tri(a,b0,b1,b2,44);tri(a,b2,b1,b0,44);
            float[] out=new float[a.size()];for(int i=0;i<a.size();i++)out[i]=a.get(i);return out;
        }
        static void tri(ArrayList<Float> a,float[] A,float[] B,float[] C,float part){float ux=B[0]-A[0],uy=B[1]-A[1],uz=B[2]-A[2],vx=C[0]-A[0],vy=C[1]-A[1],vz=C[2]-A[2],nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-5f){nx=0;ny=1;nz=0;l=1;}nx/=l;ny/=l;nz/=l;put(a,A,nx,ny,nz,part);put(a,B,nx,ny,nz,part);put(a,C,nx,ny,nz,part);}
        static void put(ArrayList<Float>a,float[]v,float nx,float ny,float nz,float part){a.add(v[0]);a.add(v[1]);a.add(v[2]);a.add(nx);a.add(ny);a.add(nz);a.add(part);}

        static final String VS=
                "uniform mat4 uMvp,uModel;uniform float uThrottle,uGear,uMainComp,uNoseComp,uWheelSpin,uStabL,uStabR,uRudderL,uRudderR,uFlapL,uFlapR,uVector,uTime,uBirdAge;"+
                "attribute vec3 aPos,aNormal;attribute float aPart;varying vec3 vN,vPos;varying float vP,vR;mat2 rr(float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c);}"+
                "void main(){vec3 p=aPos,n=aNormal;vR=0.;float d=.0174532925;"+
                "if(aPart>3.5&&aPart<4.5){float a=uStabL*d;vec2 piv=vec2(.30,1.78);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}else if(aPart>4.5&&aPart<5.5){float a=uStabR*d;vec2 piv=vec2(.30,1.78);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}else if(aPart>5.5&&aPart<6.5){float a=uRudderL*d;vec2 piv=vec2(-.94,2.38);p.xz=rr(a)*(p.xz-piv)+piv;n.xz=rr(a)*n.xz;}else if(aPart>6.5&&aPart<7.5){float a=uRudderR*d;vec2 piv=vec2(.94,2.38);p.xz=rr(a)*(p.xz-piv)+piv;n.xz=rr(a)*n.xz;}else if(aPart>8.5&&aPart<9.5){float a=uFlapL*d;vec2 piv=vec2(.22,.70);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}else if(aPart>9.5&&aPart<10.5){float a=uFlapR*d;vec2 piv=vec2(.22,.70);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}"+
                "if(aPart>27.5&&aPart<28.5){float op=mix(.93,1.08,smoothstep(.18,1.,uThrottle));float cx=p.x<0.?-.72:.72;p.x=cx+(p.x-cx)*op;p.y=-.10+(p.y+.10)*op;}"+
                "if(aPart>21.5&&aPart<22.5){float ab=smoothstep(.73,.88,uThrottle);float cx=p.x<0.?-.72:.72;float flutter=1.+.045*sin(uTime*31.+p.z*12.);float rad=mix(.03,1.,ab)*flutter;p.x=cx+(p.x-cx)*rad;p.y=-.10+(p.y+.10)*rad;p.z=3.42+(p.z-3.42)*mix(.015,.92+ab*.15,ab);}"+
                "if((aPart>1.5&&aPart<2.5)||(aPart>7.5&&aPart<8.5)||(aPart>11.5&&aPart<12.5)||(aPart>20.5&&aPart<22.5)||(aPart>27.5&&aPart<28.5)){float a=uVector*d;vec2 piv=vec2(-.10,3.18);p.yz=rr(a)*(p.yz-piv)+piv;n.yz=rr(a)*n.yz;}"+
                "if((aPart>12.5&&aPart<15.5)||(aPart>23.5&&aPart<24.5)){float nose=step(p.z,-2.),r=1.-uGear,fold=smoothstep(.08,.82,r);vR=r;if(aPart>13.5&&aPart<14.5&&uGear>.70){vec2 ctr=nose>.5?vec2(-1.62,-3.78):vec2(-1.67,1.18);p.yz=rr(uWheelSpin)*(p.yz-ctr)+ctr;}if(nose>.5){vec2 piv=vec2(-.46,-3.76),q=p.yz-piv;q=rr(-1.34*fold)*q;p.yz=piv+q;p.x*=mix(1.,.55,fold);p.y=min(p.y,-.40);}else{float s=p.x<0.?-1.:1.;vec2 piv=vec2(1.34*s,-.38),q=vec2(p.x,p.y)-piv;q=rr(-s*1.14*fold)*q;p.x=piv.x+q.x;p.y=piv.y+q.y;p.x=mix(p.x,.72*s,smoothstep(.68,.96,r));p.z=mix(p.z,.94+(p.z-.94)*.42,fold);p.y=min(p.y,-.38);}if(aPart>14.5&&aPart<15.5){float op=smoothstep(.03,.18,r)*(1.-smoothstep(.62,.92,r));p.x+=(p.x<0.?-.24:.24)*op;p.y-=.08*op;}}"+
                "if(aPart>39.5&&aPart<40.5){float q=clamp((p.z-.02)/5.85,0.,1.);float wob=.018+.075*q;p.x+=sin(uTime*6.4+p.z*4.7)*wob;p.y+=cos(uTime*5.3+p.z*3.9)*wob*.62;}"+
                "if(aPart>40.5&&aPart<44.5){float age=max(uBirdAge,0.);float flap=sin(uTime*8.4);if(aPart>41.5&&aPart<43.5)p.y+=abs(p.x)*flap*.26;float drift=smoothstep(1.4,5.7,age);p*=mix(1.22,1.05,drift);p+=vec3(-4.8-drift*1.2,1.28+.16*sin(uTime*1.6),-.35+drift*12.5);}"+
                "vN=normalize(mat3(uModel)*n);vP=aPart;vPos=p;gl_Position=uMvp*vec4(p,1.);}";

        static final String FS=
                "precision mediump float;uniform vec4 uColor;uniform vec3 uLightDir,uCameraPos;uniform float uThrottle,uTime,uSpeed,uAirborne,uRollDeg,uBirdAge;varying vec3 vN,vPos;varying float vP,vR;float sat(float x){return clamp(x,0.,1.);}float hash(vec3 p){return fract(sin(dot(p,vec3(12.9898,78.233,37.719)))*43758.5453);}vec3 envc(vec3 r){float sky=sat(r.y*.5+.5);float side=sat(r.x*.5+.5);vec3 low=vec3(.055,.062,.070),high=vec3(.31,.49,.68);vec3 e=mix(low,high,pow(sky,.72));e+=vec3(.045,.032,.020)*(1.-sky)*side;return e;}"+
                "void main(){if(vP>39.5&&vP<40.5){float fade=smoothstep(.03,.42,vPos.z)*(1.-smoothstep(4.35,5.88,vPos.z));float spd=smoothstep(42.,128.,uSpeed),bank=smoothstep(18.,125.,abs(uRollDeg)),pulse=.72+.28*sin(uTime*8.2+vPos.z*5.4);vec3 vc=mix(vec3(.48,.68,.82),vec3(.94,.985,1.),spd);float a=uAirborne*fade*spd*(.075+.19*spd+.115*bank)*pulse;gl_FragColor=vec4(vc*(.36+.22*pulse),a);return;}"+
                "vec3 N=normalize(vN),V=normalize(uCameraPos-vPos),L=normalize(uLightDir);float ndv=max(dot(N,V),0.001);vec3 H=normalize(L+V);float ndl=max(dot(N,L),0.),ndh=max(dot(N,H),0.);vec3 base=uColor.rgb,emitc=vec3(0.);float rough=.55,metal=.10,ao=.94,alpha=1.;"+
                "if(vP<.5){float upper=smoothstep(-.28,.60,N.y);float fine=(hash(floor(vPos*95.))-0.5)*.010;float panelA=abs(fract(vPos.z*.37+vPos.x*.055)-.5),panelB=abs(fract(vPos.x*.31-vPos.z*.041)-.5);float seam=1.-smoothstep(.010,.027,min(panelA,panelB)),ram=1.-smoothstep(.010,.032,abs(fract((vPos.x+vPos.z)*.115)-.5));base=mix(vec3(.225,.238,.250),vec3(.365,.382,.400),upper)+vec3(fine);base*=1.-.050*seam-.026*ram;rough=.40+.11*seam+.035*(1.-upper);metal=.16;ao=.96-.055*seam;N=normalize(N+vec3(fine*.55,fine*.30,0.));}"+
                "else if(vP>.5&&vP<1.5){vec3 R=reflect(-V,N);float fr=.08+.92*pow(1.-ndv,4.2);vec3 glass=mix(vec3(.010,.027,.038),envc(R),.34+.52*fr);float sun=pow(ndh,120.);glass+=vec3(.80,.88,.90)*sun*.72;glass+=vec3(.11,.075,.035)*pow(1.-ndv,2.2)*.20;gl_FragColor=vec4(glass,.20+.34*fr);return;}"+
                "else if(vP>1.5&&vP<2.5){base=vec3(.145,.153,.160);rough=.24;metal=.90;}else if(vP>2.5&&vP<3.5){base=vec3(.075,.084,.091);rough=.64;metal=.30;ao=.82;}else if(vP>10.5&&vP<11.5){base=vec3(.028,.032,.036);rough=.42;metal=.52;}else if(vP>12.5&&vP<13.5){base=vec3(.50,.52,.54);rough=.22;metal=.92;}else if(vP>13.5&&vP<14.5){base=vec3(.010,.011,.012);rough=.92;metal=.01;}else if(vP>14.5&&vP<15.5){base=vec3(.205,.215,.225);rough=.34;metal=.72;}else if(vP>15.5&&vP<18.5){base=vec3(.022,.026,.030);rough=.70;metal=.16;}else if(vP>18.5&&vP<20.5){base=vec3(.042,.048,.053);rough=.76;metal=.16;ao=.80;}else if(vP>20.5&&vP<21.5){base=vec3(.105,.110,.115);rough=.27;metal=.92;}"+
                "else if(vP>21.5&&vP<22.5){float ab=smoothstep(.73,.88,uThrottle);float flick=.72+.28*sin(uTime*27.+vPos.z*19.),diamond=.78+.22*sin(vPos.z*10.5-uTime*5.);base=vec3(.025,.018,.012);vec3 core=mix(vec3(.08,.28,1.45),vec3(1.35,.30,.025),smoothstep(3.7,6.3,vPos.z));emitc=core*ab*(2.0+1.25*flick)*diamond;rough=.10;metal=.08;alpha=ab*(.16+.58*flick);}"+
                "else if(vP>22.5&&vP<23.5){base=vec3(.285,.300,.315);rough=.44;metal=.42;}else if(vP>24.5&&vP<25.5){emitc=vec3(1.,.018,.010)*1.35;base=vec3(.12,0.,0.);rough=.18;}else if(vP>25.5&&vP<26.5){emitc=vec3(.015,1.,.13)*1.25;base=vec3(0.,.10,.015);rough=.18;}else if(vP>26.5&&vP<27.5){emitc=vec3(1.,.92,.70)*1.18;base=vec3(.10,.085,.055);rough=.18;}else if(vP>27.5&&vP<28.5){float heat=sat((vPos.z-2.55)/1.35),band=.5+.5*sin(vPos.z*17.+vPos.x*5.);base=mix(vec3(.135,.145,.155),vec3(.28,.115,.048),heat*.50);base=mix(base,vec3(.12,.16,.24),heat*.12*band);rough=.28+.10*band;metal=.90;}else if(vP>28.5&&vP<29.5){float edge=.5+.5*sin(vPos.z*29.+vPos.x*13.);base=mix(vec3(.082,.091,.098),vec3(.135,.145,.150),edge*.22);rough=.43;metal=.58;}else if(vP>29.5&&vP<30.5){base=vec3(.18,.195,.205);rough=.43;metal=.55;}else if(vP>30.5&&vP<31.5){base=vec3(.32,.33,.315);rough=.56;metal=.22;}else if(vP>31.5&&vP<32.5){base=vec3(.16,.17,.165);rough=.55;metal=.28;}else if(vP>32.5&&vP<33.5){base=vec3(.16,.205,.17);rough=.82;metal=.02;}else if(vP>33.5&&vP<34.5){base=vec3(.55,.57,.56);rough=.36;metal=.18;}else if(vP>34.5&&vP<35.5){vec3 R=reflect(-V,N);base=mix(vec3(.010,.014,.018),envc(R),.48);rough=.10;metal=.78;}else if(vP>35.5&&vP<36.5){vec3 R=reflect(-V,N);float fr=pow(1.-ndv,3.);vec3 hud=mix(vec3(.010,.10,.060),envc(R),.18);gl_FragColor=vec4(hud+vec3(.01,.23,.10)*.20,.16+.18*fr);return;}else if(vP>36.5&&vP<37.5){base=vec3(.012,.022,.017);emitc=vec3(.015,.46,.17)*(.65+.15*sin(uTime*2.));rough=.28;metal=.10;}else if(vP>7.5&&vP<8.5){float flick=.75+.25*sin(uTime*23.+vPos.z*15.);base=vec3(.045,.018,.010);emitc=mix(vec3(.05,.18,.82),vec3(1.,.13,.006),uThrottle)*uThrottle*(1.5+.8*flick);rough=.18;metal=.14;alpha=.45+.40*uThrottle;}"+
                "else if(vP>40.5&&vP<41.5){base=vec3(.62,.64,.61);rough=.78;metal=.02;alpha=1.-smoothstep(4.1,5.7,uBirdAge);}else if(vP>41.5&&vP<43.5){base=vec3(.24,.26,.27);rough=.82;metal=.01;alpha=1.-smoothstep(4.1,5.7,uBirdAge);}else if(vP>43.5&&vP<44.5){base=vec3(.88,.54,.15);rough=.70;metal=.01;alpha=1.-smoothstep(4.1,5.7,uBirdAge);}"+
                "if(((vP>12.5&&vP<14.5)||(vP>23.5&&vP<24.5)))alpha*=1.-smoothstep(.78,.94,vR);float hemi=.38+.62*sat(N.y*.5+.5),lowerOccl=mix(.76,1.,sat(N.y+.30));ao*=lowerOccl;vec3 R=reflect(-V,N),env=envc(R),F0=mix(vec3(.032),base,metal);float fres=pow(1.-ndv,5.);vec3 F=mix(F0,vec3(1.),fres);float spow=mix(150.,8.,rough),specSun=pow(ndh,spow)*(1.-rough*.58);vec3 diffuse=base*(1.-metal)*(0.13+0.87*ndl)*hemi*ao,spec=F*(specSun*(.46+.58*ndl)+env*(.10+.44*(1.-rough)));float rim=pow(1.-ndv,2.8)*(.04+.08*(1.-rough));gl_FragColor=vec4(diffuse+spec+env*rim+emitc,alpha);}";
    }
    private static float cl(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
