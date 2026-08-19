package com.mg.fixturecockpitsim;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import java.nio.*;
import java.util.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** V6 refined fighter: smooth fuselage loft, unified light-grey control surfaces and damped motion. */
public final class Jet3DViewV5 extends GLSurfaceView {
    private final R r;
    public Jet3DViewV5(Context c){ super(c); setEGLContextClientVersion(2); r=new R(); setRenderer(r); setRenderMode(RENDERMODE_CONTINUOUSLY); setPreserveEGLContextOnPause(true); }
    public void setTelemetry(float roll,float pitch,float yaw,float thr,float hz,int drops,boolean live){ r.set(roll,pitch,yaw,thr,live); }
    @Override public boolean onTouchEvent(MotionEvent e){ if(e.getAction()==MotionEvent.ACTION_UP)r.nextCamera(); return true; }

    private static final class R implements Renderer{
        final float[] proj=new float[16],view=new float[16],model=new float[16],mv=new float[16],mvp=new float[16];
        float tr,tp,ty,thr=.7f, roll,pitch,yaw, vr,vp,vy; boolean live; long last;
        int camTarget,program,aP,aN,aC,uMvp,uModel,uLight,uEm; float camBlend;
        Mesh jet,flame;
        void set(float a,float b,float c,float t,boolean l){tr=a;tp=b;ty=c;thr=t;live=l;}
        void nextCamera(){camTarget=(camTarget+1)%4;camBlend=0f;}
        @Override public void onSurfaceCreated(GL10 gl,EGLConfig cfg){GLES20.glClearColor(.018f,.055f,.11f,1);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);program=prog(VS,FS);aP=GLES20.glGetAttribLocation(program,"aPos");aN=GLES20.glGetAttribLocation(program,"aNormal");aC=GLES20.glGetAttribLocation(program,"aColor");uMvp=GLES20.glGetUniformLocation(program,"uMvp");uModel=GLES20.glGetUniformLocation(program,"uModel");uLight=GLES20.glGetUniformLocation(program,"uLight");uEm=GLES20.glGetUniformLocation(program,"uEmissive");jet=buildJet();flame=buildFlame();last=System.nanoTime();}
        @Override public void onSurfaceChanged(GL10 gl,int w,int h){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,43f,(float)w/Math.max(1,h),.1f,100f);}
        @Override public void onDrawFrame(GL10 gl){long n=System.nanoTime();float dt=Math.min(.04f,Math.max(.001f,(n-last)/1e9f));last=n;
            float kr=28f,kp=24f,ky=15f,dam=2f*(float)Math.sqrt(kr);vr+=(shortest(tr-roll)*kr-vr*dam)*dt;roll+=vr*dt;dam=2f*(float)Math.sqrt(kp);vp+=((tp-pitch)*kp-vp*dam)*dt;pitch+=vp*dt;dam=2f*(float)Math.sqrt(ky);vy+=(shortest(ty-yaw)*ky-vy*dam)*dt;yaw+=vy*dt;
            camBlend=Math.min(1f,camBlend+dt*2.5f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);camera();Matrix.setIdentityM(model,0);Matrix.rotateM(model,0,-yaw*.22f,0,1,0);Matrix.rotateM(model,0,pitch,1,0,0);Matrix.rotateM(model,0,-roll,0,0,1);Matrix.multiplyMM(mv,0,view,0,model,0);Matrix.multiplyMM(mvp,0,proj,0,mv,0);GLES20.glUseProgram(program);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,model,0);GLES20.glUniform3f(uLight,-.28f,.88f,.42f);GLES20.glUniform1f(uEm,0);jet.draw(aP,aN,aC);if(live&&thr>.5f){GLES20.glUniform1f(uEm,.8f+.18f*(float)Math.sin(n/50_000_000.0));flame.draw(aP,aN,aC);} }
        void camera(){float[][] e={{0,2.25f,8.7f},{0,1.35f,6.0f},{4.7f,3.7f,6.6f},{-6.0f,2.0f,3.8f}};float[] q=e[camTarget];float s=camBlend*camBlend*(3-2*camBlend);float ex=q[0]*s,ey=2.25f+(q[1]-2.25f)*s,ez=8.7f+(q[2]-8.7f)*s;Matrix.setLookAtM(view,0,ex,ey,ez,0,0,-.15f,0,1,0);}
        static float shortest(float d){while(d>180)d-=360;while(d<-180)d+=360;return d;}

        Mesh buildJet(){B b=new B();
            float[] top={.47f,.50f,.53f,1};
            float[] mid={.45f,.48f,.51f,1};
            float[] side={.42f,.45f,.48f,1};
            float[] under={.39f,.42f,.45f,1};
            float[] glass={.025f,.13f,.19f,.94f},noz={.09f,.09f,.10f,1};
            float[] z={-3.55f,-2.85f,-2.05f,-1.25f,-.35f,.65f,1.45f,2.15f,2.55f};
            float[] wx={.03f,.18f,.42f,.62f,.68f,.66f,.63f,.56f,.42f};
            float[] hy={.03f,.10f,.22f,.34f,.38f,.36f,.33f,.28f,.22f};
            b.loft(z,wx,hy,12,top,mid,under);
            wing(b,-1,top,side,under); wing(b,1,top,side,under);
            stab(b,-1,top,side); stab(b,1,top,side);
            fin(b,-.68f,-1,top,side); fin(b,.68f,1,top,side);
            b.loft(new float[]{-1.75f,-1.35f,-.90f,-.45f},new float[]{.05f,.22f,.27f,.12f},new float[]{.04f,.20f,.24f,.10f},12,glass,glass,glass,.34f);
            b.box(-.34f,-.15f,1.35f,.48f,.43f,1.35f,mid,side);b.box(.34f,-.15f,1.35f,.48f,.43f,1.35f,mid,side);b.tube(-.34f,-.15f,2.48f,.235f,.30f,24,noz);b.tube(.34f,-.15f,2.48f,.235f,.30f,24,noz);
            return b.mesh();}
        Mesh buildFlame(){B b=new B();float[] o={1,.22f,.02f,.78f},i={1,.80f,.12f,.92f};b.cone(-.34f,-.15f,2.66f,.19f,1.0f,20,o);b.cone(.34f,-.15f,2.66f,.19f,1.0f,20,o);b.cone(-.34f,-.15f,2.70f,.09f,.64f,16,i);b.cone(.34f,-.15f,2.70f,.09f,.64f,16,i);return b.mesh();}
        static void wing(B b,float s,float[] top,float[] side,float[] bot){float yt=.09f,yb=-.07f;float[] a=p(s*.40f,yt,-1.25f),c=p(s*2.58f,yt,.02f),d=p(s*1.72f,yt,1.38f),e=p(s*.50f,yt,1.15f);float[] a2=p(a[0],yb,a[2]),c2=p(c[0],yb,c[2]),d2=p(d[0],yb,d[2]),e2=p(e[0],yb,e[2]);b.q(a,c,d,e,top);b.q(e2,d2,c2,a2,bot);b.q(a,a2,c2,c,side);b.q(c,c2,d2,d,side);b.q(d,d2,e2,e,side);}
        static void stab(B b,float s,float[] t,float[] d){float[] a=p(s*.42f,.02f,1.25f),c=p(s*1.58f,.02f,1.78f),e=p(s*1.28f,.02f,2.38f),f=p(s*.48f,.02f,2.08f);b.q(a,c,e,f,t);}
        static void fin(B b,float x,float s,float[] t,float[] d){float[] a=p(x,.18f,.98f),c=p(x+s*.22f,1.35f,1.46f),e=p(x+s*.28f,1.06f,2.20f),f=p(x,.20f,2.10f);b.q(a,c,e,f,t);}
        static int prog(String v,String f){int a=sh(GLES20.GL_VERTEX_SHADER,v),b=sh(GLES20.GL_FRAGMENT_SHADER,f),p=GLES20.glCreateProgram();GLES20.glAttachShader(p,a);GLES20.glAttachShader(p,b);GLES20.glLinkProgram(p);return p;}static int sh(int t,String s){int a=GLES20.glCreateShader(t);GLES20.glShaderSource(a,s);GLES20.glCompileShader(a);return a;}
        static final String VS="uniform mat4 uMvp;uniform mat4 uModel;attribute vec3 aPos;attribute vec3 aNormal;attribute vec4 aColor;varying vec3 n;varying vec4 c;void main(){gl_Position=uMvp*vec4(aPos,1.0);n=normalize(mat3(uModel)*aNormal);c=aColor;}";
        static final String FS="precision mediump float;uniform vec3 uLight;uniform float uEmissive;varying vec3 n;varying vec4 c;void main(){float d=max(0.0,dot(normalize(n),normalize(uLight)));float rim=pow(1.0-max(0.0,n.z),2.0)*0.10;float l=0.34+0.62*d+rim+uEmissive;gl_FragColor=vec4(c.rgb*l,c.a);}";
    }
    static final class Mesh{final FloatBuffer p,n,c;final int count;Mesh(float[] a,float[] b,float[] d){p=buf(a);n=buf(b);c=buf(d);count=a.length/3;}void draw(int ap,int an,int ac){GLES20.glEnableVertexAttribArray(ap);GLES20.glVertexAttribPointer(ap,3,GLES20.GL_FLOAT,false,0,p);GLES20.glEnableVertexAttribArray(an);GLES20.glVertexAttribPointer(an,3,GLES20.GL_FLOAT,false,0,n);GLES20.glEnableVertexAttribArray(ac);GLES20.glVertexAttribPointer(ac,4,GLES20.GL_FLOAT,false,0,c);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);}static FloatBuffer buf(float[] a){FloatBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(a).position(0);return b;}}
    static final class B{final ArrayList<Float>P=new ArrayList<>(),N=new ArrayList<>(),C=new ArrayList<>();void t(float[]a,float[]b,float[]c,float[]co){float[]n=norm(a,b,c);add(a,n,co);add(b,n,co);add(c,n,co);}void q(float[]a,float[]b,float[]c,float[]d,float[]co){t(a,b,c,co);t(a,c,d,co);}void add(float[]v,float[]n,float[]co){for(float x:v)P.add(x);for(float x:n)N.add(x);for(float x:co)C.add(x);}void loft(float[]z,float[]wx,float[]hy,int seg,float[]top,float[]side,float[]bottom){loft(z,wx,hy,seg,top,side,bottom,0);}void loft(float[]z,float[]wx,float[]hy,int seg,float[]top,float[]side,float[]bottom,float yOff){for(int k=0;k<z.length-1;k++)for(int i=0;i<seg;i++){double a=2*Math.PI*i/seg,b=2*Math.PI*(i+1)/seg;float[]p0=p((float)Math.cos(a)*wx[k],yOff+(float)Math.sin(a)*hy[k],z[k]),p1=p((float)Math.cos(b)*wx[k],yOff+(float)Math.sin(b)*hy[k],z[k]),p2=p((float)Math.cos(b)*wx[k+1],yOff+(float)Math.sin(b)*hy[k+1],z[k+1]),p3=p((float)Math.cos(a)*wx[k+1],yOff+(float)Math.sin(a)*hy[k+1],z[k+1]);float[]co=(Math.sin((a+b)/2)>0)?top:bottom;q(p0,p1,p2,p3,co);}}void box(float cx,float cy,float cz,float sx,float sy,float sz,float[]top,float[]side){float x=sx/2,y=sy/2,z=sz/2;float[]a=p(cx-x,cy-y,cz-z),b=p(cx+x,cy-y,cz-z),c=p(cx+x,cy+y,cz-z),d=p(cx-x,cy+y,cz-z),e=p(cx-x,cy-y,cz+z),f=p(cx+x,cy-y,cz+z),g=p(cx+x,cy+y,cz+z),h=p(cx-x,cy+y,cz+z);q(d,c,g,h,top);q(a,e,f,b,side);q(a,d,h,e,side);q(b,f,g,c,side);q(e,h,g,f,side);}void tube(float cx,float cy,float z,float r,float len,int seg,float[]co){for(int i=0;i<seg;i++){double a=2*Math.PI*i/seg,b=2*Math.PI*(i+1)/seg;float[]p0=p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z-len/2),p1=p(cx+(float)Math.cos(b)*r,cy+(float)Math.sin(b)*r,z-len/2),p2=p(cx+(float)Math.cos(b)*r,cy+(float)Math.sin(b)*r,z+len/2),p3=p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z+len/2);q(p0,p1,p2,p3,co);}}void cone(float cx,float cy,float z,float r,float len,int seg,float[]co){float[]tip=p(cx,cy,z+len);for(int i=0;i<seg;i++){double a=2*Math.PI*i/seg,b=2*Math.PI*(i+1)/seg;t(p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z),p(cx+(float)Math.cos(b)*r,cy+(float)Math.sin(b)*r,z),tip,co);}}Mesh mesh(){return new Mesh(arr(P),arr(N),arr(C));}static float[]arr(ArrayList<Float>a){float[]r=new float[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;}static float[]norm(float[]a,float[]b,float[]c){float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2],nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6)l=1;return new float[]{nx/l,ny/l,nz/l};}}
    static float[]p(float x,float y,float z){return new float[]{x,y,z};}
}
