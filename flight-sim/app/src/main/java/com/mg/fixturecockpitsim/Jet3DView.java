package com.mg.fixturecockpitsim;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class Jet3DView extends GLSurfaceView {
    private final JetRenderer renderer;

    public Jet3DView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new JetRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
    }

    public void setTelemetry(float roll, float pitch, float yaw, float throttle, float linkHz, int drops, boolean live) {
        renderer.setTelemetry(roll, pitch, yaw, throttle, linkHz, drops, live);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) renderer.nextCamera();
        return true;
    }

    private static final class JetRenderer implements GLSurfaceView.Renderer {
        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mv = new float[16];
        private final float[] mvp = new float[16];

        private volatile float targetRoll, targetPitch, targetYaw, throttle = 0.62f;
        private volatile boolean live;
        private float roll, pitch, yaw;
        private int cameraMode;
        private long lastNs;

        private int program, aPos, aNormal, aColor, uMvp, uModel, uLight, uEmissive;
        private Mesh aircraft, flames;

        void setTelemetry(float r,float p,float y,float t,float hz,int drops,boolean l){
            targetRoll=r; targetPitch=p; targetYaw=y; throttle=t; live=l;
        }
        void nextCamera(){ cameraMode=(cameraMode+1)%4; }

        @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0.025f,0.09f,0.18f,1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
            program=buildProgram(VS,FS);
            aPos=GLES20.glGetAttribLocation(program,"aPos");
            aNormal=GLES20.glGetAttribLocation(program,"aNormal");
            aColor=GLES20.glGetAttribLocation(program,"aColor");
            uMvp=GLES20.glGetUniformLocation(program,"uMvp");
            uModel=GLES20.glGetUniformLocation(program,"uModel");
            uLight=GLES20.glGetUniformLocation(program,"uLight");
            uEmissive=GLES20.glGetUniformLocation(program,"uEmissive");
            aircraft=buildAircraft();
            flames=buildFlames();
            lastNs=System.nanoTime();
        }

        @Override public void onSurfaceChanged(GL10 gl,int width,int height){
            GLES20.glViewport(0,0,width,height);
            float aspect=(float)width/Math.max(1,height);
            Matrix.perspectiveM(projection,0,44f,aspect,0.1f,80f);
        }

        @Override public void onDrawFrame(GL10 gl){
            long now=System.nanoTime();
            float dt=Math.min(0.05f,Math.max(0.001f,(now-lastNs)/1_000_000_000f));
            lastNs=now;
            float k=1f-(float)Math.exp(-dt*9f);
            roll += shortest(targetRoll-roll)*k;
            pitch += (targetPitch-pitch)*k;
            yaw += shortest(targetYaw-yaw)*k*0.65f;

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            setCamera();
            Matrix.setIdentityM(model,0);
            Matrix.rotateM(model,0,-yaw*0.28f,0,1,0);
            Matrix.rotateM(model,0,pitch,1,0,0);
            Matrix.rotateM(model,0,-roll,0,0,1);
            Matrix.multiplyMM(mv,0,view,0,model,0);
            Matrix.multiplyMM(mvp,0,projection,0,mv,0);

            GLES20.glUseProgram(program);
            GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);
            GLES20.glUniformMatrix4fv(uModel,1,false,model,0);
            GLES20.glUniform3f(uLight,-0.35f,0.80f,0.55f);
            GLES20.glUniform1f(uEmissive,0f);
            aircraft.draw(aPos,aNormal,aColor);

            if(live && throttle>0.48f){
                float pulse=0.85f+0.15f*(float)Math.sin(now/42_000_000.0);
                GLES20.glUniform1f(uEmissive,pulse);
                flames.draw(aPos,aNormal,aColor);
            }
        }

        private void setCamera(){
            switch(cameraMode){
                case 1: Matrix.setLookAtM(view,0,0f,1.55f,6.2f,0f,0.05f,0.15f,0,1,0); break;
                case 2: Matrix.setLookAtM(view,0,4.9f,4.2f,6.6f,0f,0f,0f,0,1,0); break;
                case 3: Matrix.setLookAtM(view,0,-6.4f,2.2f,3.7f,0f,0f,0.2f,0,1,0); break;
                default: Matrix.setLookAtM(view,0,0f,2.35f,8.5f,0f,0f,-0.25f,0,1,0); break;
            }
        }

        private static float shortest(float d){ while(d>180)d-=360; while(d<-180)d+=360; return d; }

        private Mesh buildAircraft(){
            Builder b=new Builder();
            float[] skin={0.38f,0.42f,0.46f,1f};
            float[] edge={0.24f,0.28f,0.31f,1f};
            float[] dark={0.08f,0.11f,0.13f,1f};
            float[] canopy={0.03f,0.16f,0.23f,1f};
            float[] nozzle={0.10f,0.10f,0.11f,1f};

            // Faceted stealth fuselage.
            float[] n=p(0,0.04f,-3.35f);
            float[] tL=p(-0.55f,0.30f,-1.35f), tR=p(0.55f,0.30f,-1.35f);
            float[] bL=p(-0.62f,-0.18f,-1.18f), bR=p(0.62f,-0.18f,-1.18f);
            float[] aTL=p(-0.53f,0.26f,1.90f), aTR=p(0.53f,0.26f,1.90f);
            float[] aBL=p(-0.56f,-0.23f,2.18f), aBR=p(0.56f,-0.23f,2.18f);
            b.tri(n,tL,tR,skin); b.tri(n,bL,tL,edge); b.tri(n,tR,bR,edge); b.tri(n,bR,bL,dark);
            b.quad(tL,aTL,aTR,tR,skin); b.quad(bL,bR,aBR,aBL,dark);
            b.quad(tL,bL,aBL,aTL,edge); b.quad(tR,aTR,aBR,bR,edge); b.quad(aTL,aBL,aBR,aTR,edge);

            // Thick delta wings.
            wing(b,-1f,skin,edge,dark); wing(b,1f,skin,edge,dark);
            // Stabilators and canted twin tails.
            stabilator(b,-1f,edge,dark); stabilator(b,1f,edge,dark);
            fin(b,-0.67f,-1f,edge,dark); fin(b,0.67f,1f,edge,dark);

            // Canopy.
            float[] c0=p(0,0.61f,-1.48f), c1=p(-0.28f,0.32f,-0.96f), c2=p(0.28f,0.32f,-0.96f), c3=p(0,0.49f,-0.38f);
            b.tri(c0,c1,c2,canopy); b.tri(c0,c3,c1,canopy); b.tri(c0,c2,c3,canopy); b.tri(c1,c3,c2,canopy);

            // Engine shoulders + nozzles.
            b.box(-0.34f,-0.15f,1.35f,0.42f,0.40f,1.25f,edge,dark);
            b.box(0.34f,-0.15f,1.35f,0.42f,0.40f,1.25f,edge,dark);
            b.tube(-0.34f,-0.15f,2.47f,0.23f,0.28f,14,nozzle);
            b.tube(0.34f,-0.15f,2.47f,0.23f,0.28f,14,nozzle);

            // Intake openings.
            b.quad(p(-0.73f,-0.08f,-0.67f),p(-0.39f,-0.12f,-0.32f),p(-0.36f,0.12f,-0.32f),p(-0.67f,0.12f,-0.67f),dark);
            b.quad(p(0.39f,-0.12f,-0.32f),p(0.73f,-0.08f,-0.67f),p(0.67f,0.12f,-0.67f),p(0.36f,0.12f,-0.32f),dark);
            return b.mesh();
        }

        private Mesh buildFlames(){
            Builder b=new Builder();
            float[] o={1f,0.22f,0.025f,0.80f}, i={1f,0.78f,0.10f,0.95f};
            b.cone(-0.34f,-0.15f,2.72f,0.17f,0.90f,12,o); b.cone(0.34f,-0.15f,2.72f,0.17f,0.90f,12,o);
            b.cone(-0.34f,-0.15f,2.74f,0.08f,0.58f,10,i); b.cone(0.34f,-0.15f,2.74f,0.08f,0.58f,10,i);
            return b.mesh();
        }

        private static void wing(Builder b,float s,float[] top,float[] side,float[] bottom){
            float yt=0.10f,yb=-0.08f;
            float[] a=p(s*0.42f,yt,-1.16f), c=p(s*2.45f,yt,0.10f), d=p(s*1.55f,yt,1.34f), e=p(s*0.48f,yt,1.12f);
            float[] a2=p(a[0],yb,a[2]),c2=p(c[0],yb,c[2]),d2=p(d[0],yb,d[2]),e2=p(e[0],yb,e[2]);
            b.quad(a,c,d,e,top); b.quad(e2,d2,c2,a2,bottom);
            b.quad(a,a2,c2,c,side); b.quad(c,c2,d2,d,side); b.quad(d,d2,e2,e,side); b.quad(e,e2,a2,a,side);
        }
        private static void stabilator(Builder b,float s,float[] top,float[] bottom){
            float[] a=p(s*0.43f,0.03f,1.23f), c=p(s*1.55f,0.03f,1.76f), d=p(s*1.23f,0.03f,2.35f), e=p(s*0.48f,0.03f,2.08f);
            b.quad(a,c,d,e,top);
            float[] a2=p(a[0],-0.05f,a[2]),c2=p(c[0],-0.05f,c[2]),d2=p(d[0],-0.05f,d[2]),e2=p(e[0],-0.05f,e[2]);
            b.quad(e2,d2,c2,a2,bottom); b.quad(c,c2,d2,d,bottom);
        }
        private static void fin(Builder b,float x,float s,float[] color,float[] side){
            float[] a=p(x,0.20f,0.96f), c=p(x+s*0.18f,1.28f,1.45f), d=p(x+s*0.24f,1.00f,2.17f), e=p(x,0.22f,2.08f);
            float[] a2=p(x-s*0.07f,0.18f,0.96f),c2=p(c[0]-s*0.07f,c[1],c[2]),d2=p(d[0]-s*0.07f,d[1],d[2]),e2=p(x-s*0.07f,0.20f,2.08f);
            b.quad(a,c,d,e,color); b.quad(e2,d2,c2,a2,side); b.quad(c,c2,d2,d,side);
        }

        private static int buildProgram(String vs,String fs){
            int v=compile(GLES20.GL_VERTEX_SHADER,vs), f=compile(GLES20.GL_FRAGMENT_SHADER,fs);
            int p=GLES20.glCreateProgram(); GLES20.glAttachShader(p,v); GLES20.glAttachShader(p,f); GLES20.glLinkProgram(p);
            int[] ok=new int[1]; GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);
            if(ok[0]==0) throw new RuntimeException(GLES20.glGetProgramInfoLog(p));
            return p;
        }
        private static int compile(int type,String src){
            int s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,src); GLES20.glCompileShader(s);
            int[] ok=new int[1]; GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);
            if(ok[0]==0) throw new RuntimeException(GLES20.glGetShaderInfoLog(s));
            return s;
        }

        private static final String VS=
                "uniform mat4 uMvp; uniform mat4 uModel; attribute vec3 aPos; attribute vec3 aNormal; attribute vec4 aColor; varying vec3 vN; varying vec4 vC; void main(){ gl_Position=uMvp*vec4(aPos,1.0); vN=normalize(mat3(uModel)*aNormal); vC=aColor; }";
        private static final String FS=
                "precision mediump float; uniform vec3 uLight; uniform float uEmissive; varying vec3 vN; varying vec4 vC; void main(){ float d=max(0.0,dot(normalize(vN),normalize(uLight))); float l=0.28+0.72*d+uEmissive; gl_FragColor=vec4(vC.rgb*l,vC.a); }";
    }

    private static final class Mesh {
        final FloatBuffer pos,nrm,col; final int count;
        Mesh(float[] p,float[] n,float[] c){ pos=buf(p); nrm=buf(n); col=buf(c); count=p.length/3; }
        void draw(int aPos,int aNormal,int aColor){
            GLES20.glEnableVertexAttribArray(aPos); GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,pos);
            GLES20.glEnableVertexAttribArray(aNormal); GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,0,nrm);
            GLES20.glEnableVertexAttribArray(aColor); GLES20.glVertexAttribPointer(aColor,4,GLES20.GL_FLOAT,false,0,col);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
            GLES20.glDisableVertexAttribArray(aPos); GLES20.glDisableVertexAttribArray(aNormal); GLES20.glDisableVertexAttribArray(aColor);
        }
        private static FloatBuffer buf(float[] a){ FloatBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer(); b.put(a).position(0); return b; }
    }

    private static final class Builder {
        final ArrayList<Float> p=new ArrayList<>(), n=new ArrayList<>(), c=new ArrayList<>();
        void tri(float[] a,float[] b,float[] d,float[] color){
            float[] no=normal(a,b,d); add(a,no,color); add(b,no,color); add(d,no,color);
        }
        void quad(float[] a,float[] b,float[] d,float[] e,float[] color){ tri(a,b,d,color); tri(a,d,e,color); }
        void add(float[] v,float[] no,float[] color){ for(float x:v)p.add(x); for(float x:no)n.add(x); for(float x:color)c.add(x); }
        void box(float cx,float cy,float cz,float sx,float sy,float sz,float[] top,float[] side){
            float x=sx/2,y=sy/2,z=sz/2;
            float[] p000=p(cx-x,cy-y,cz-z), p001=p(cx-x,cy-y,cz+z), p010=p(cx-x,cy+y,cz-z), p011=p(cx-x,cy+y,cz+z);
            float[] p100=p(cx+x,cy-y,cz-z), p101=p(cx+x,cy-y,cz+z), p110=p(cx+x,cy+y,cz-z), p111=p(cx+x,cy+y,cz+z);
            quad(p010,p110,p111,p011,top); quad(p000,p001,p101,p100,side); quad(p000,p010,p011,p001,side); quad(p100,p101,p111,p110,side); quad(p001,p011,p111,p101,side); quad(p000,p100,p110,p010,side);
        }
        void tube(float cx,float cy,float z,float r,float len,int seg,float[] color){
            float z0=z-len/2,z1=z+len/2;
            for(int i=0;i<seg;i++){ double a=2*Math.PI*i/seg,b=2*Math.PI*(i+1)/seg; float[] p0=p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z0); float[] p1=p(cx+(float)Math.cos(b)*r,cy+(float)Math.sin(b)*r,z0); float[] p2=p(cx+(float)Math.cos(b)*r,cy+(float)Math.sin(b)*r,z1); float[] p3=p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z1); quad(p0,p1,p2,p3,color); }
        }
        void cone(float cx,float cy,float z,float r,float len,int seg,float[] color){
            float[] tip=p(cx,cy,z+len);
            for(int i=0;i<seg;i++){ double a=2*Math.PI*i/seg,b=2*Math.PI*(i+1)/seg; tri(p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z),p(cx+(float)Math.cos(b)*r,cy+(float)Math.sin(b)*r,z),tip,color); }
        }
        Mesh mesh(){ return new Mesh(arr(p),arr(n),arr(c)); }
        private static float[] arr(ArrayList<Float> a){ float[] r=new float[a.size()]; for(int i=0;i<r.length;i++)r[i]=a.get(i); return r; }
        private static float[] normal(float[] a,float[] b,float[] c){ float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2]; float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz); if(l<1e-6f)l=1; return new float[]{nx/l,ny/l,nz/l}; }
    }

    private static float[] p(float x,float y,float z){ return new float[]{x,y,z}; }
}
