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

/** V7 high-detail procedural stealth fighter renderer.
 *  Higher section count, smooth per-vertex normals, subtle panel seams,
 *  glossy canopy, nozzle petals and eased camera/IMU motion.
 */
public final class Jet3DViewV7 extends GLSurfaceView {
    private final R r;
    public Jet3DViewV7(Context c){ super(c); setEGLContextClientVersion(2); r=new R(); setRenderer(r); setRenderMode(RENDERMODE_CONTINUOUSLY); setPreserveEGLContextOnPause(true); }
    public void setTelemetry(float roll,float pitch,float yaw,float thr,float hz,int drops,boolean live){ r.set(roll,pitch,yaw,thr,live); }
    @Override public boolean onTouchEvent(MotionEvent e){ if(e.getAction()==MotionEvent.ACTION_UP) r.nextCamera(); return true; }

    private static final class R implements Renderer {
        final float[] proj=new float[16],view=new float[16],model=new float[16],mv=new float[16],mvp=new float[16];
        float tr,tp,ty,thr=.7f,roll,pitch,yaw,vr,vp,vy; boolean live; long last;
        int camTarget,program,aP,aN,aC,uMvp,uModel,uLight,uEm,uGloss; float camBlend=1f;
        Mesh jet,flame;
        void set(float a,float b,float c,float t,boolean l){ tr=a;tp=b;ty=c;thr=t;live=l; }
        void nextCamera(){ camTarget=(camTarget+1)%5; camBlend=0f; }

        @Override public void onSurfaceCreated(GL10 gl,EGLConfig cfg){
            GLES20.glClearColor(.020f,.070f,.135f,1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST); GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glEnable(GLES20.GL_BLEND); GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
            program=prog(VS,FS); aP=GLES20.glGetAttribLocation(program,"aPos"); aN=GLES20.glGetAttribLocation(program,"aNormal"); aC=GLES20.glGetAttribLocation(program,"aColor");
            uMvp=GLES20.glGetUniformLocation(program,"uMvp"); uModel=GLES20.glGetUniformLocation(program,"uModel"); uLight=GLES20.glGetUniformLocation(program,"uLight"); uEm=GLES20.glGetUniformLocation(program,"uEmissive"); uGloss=GLES20.glGetUniformLocation(program,"uGloss");
            jet=buildJet(); flame=buildFlame(); last=System.nanoTime();
        }
        @Override public void onSurfaceChanged(GL10 gl,int w,int h){ GLES20.glViewport(0,0,w,h); Matrix.perspectiveM(proj,0,41f,(float)w/Math.max(1,h),.08f,120f); }
        @Override public void onDrawFrame(GL10 gl){
            long n=System.nanoTime(); float dt=Math.min(.035f,Math.max(.001f,(n-last)/1e9f)); last=n;
            // slightly softer critically damped response for a heavy aircraft feel
            float kr=24f,kp=21f,ky=13f,dam=2f*(float)Math.sqrt(kr); vr+=(shortest(tr-roll)*kr-vr*dam)*dt; roll+=vr*dt;
            dam=2f*(float)Math.sqrt(kp); vp+=((tp-pitch)*kp-vp*dam)*dt; pitch+=vp*dt;
            dam=2f*(float)Math.sqrt(ky); vy+=(shortest(ty-yaw)*ky-vy*dam)*dt; yaw+=vy*dt;
            camBlend=Math.min(1f,camBlend+dt*1.9f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT); camera();
            Matrix.setIdentityM(model,0); Matrix.rotateM(model,0,-yaw*.18f,0,1,0); Matrix.rotateM(model,0,pitch,1,0,0); Matrix.rotateM(model,0,-roll,0,0,1);
            Matrix.multiplyMM(mv,0,view,0,model,0); Matrix.multiplyMM(mvp,0,proj,0,mv,0);
            GLES20.glUseProgram(program); GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0); GLES20.glUniformMatrix4fv(uModel,1,false,model,0);
            GLES20.glUniform3f(uLight,-.34f,.86f,.48f); GLES20.glUniform1f(uEm,0f); GLES20.glUniform1f(uGloss,.42f); jet.draw(aP,aN,aC);
            if(live && thr>.48f){ GLES20.glUniform1f(uGloss,0f); GLES20.glUniform1f(uEm,.86f+.12f*(float)Math.sin(n/47_000_000.0)); flame.draw(aP,aN,aC); }
        }
        void camera(){
            float[][] e={{0,2.25f,9.15f},{0,1.30f,6.15f},{4.85f,3.55f,6.65f},{-6.15f,2.15f,3.85f},{0,4.9f,5.6f}};
            float[] q=e[camTarget]; float s=camBlend*camBlend*(3f-2f*camBlend); float ex=q[0]*s,ey=2.25f+(q[1]-2.25f)*s,ez=9.15f+(q[2]-9.15f)*s;
            Matrix.setLookAtM(view,0,ex,ey,ez,0,.02f,-.18f,0,1,0);
        }
        static float shortest(float d){ while(d>180)d-=360; while(d<-180)d+=360; return d; }

        Mesh buildJet(){
            B b=new B();
            float[] skin={.52f,.55f,.58f,1f}, skin2={.49f,.52f,.55f,1f}, side={.46f,.49f,.52f,1f}, under={.43f,.46f,.49f,1f};
            float[] seam={.33f,.35f,.37f,1f}, glass={.045f,.17f,.23f,.92f}, nozzle={.12f,.12f,.125f,1f};

            // 15 fuselage stations, 24 points around each section: much smoother silhouette.
            float[] z={-3.72f,-3.42f,-3.05f,-2.62f,-2.18f,-1.72f,-1.20f,-.66f,-.10f,.48f,1.02f,1.48f,1.90f,2.28f,2.58f};
            float[] wx={.015f,.065f,.16f,.30f,.43f,.55f,.65f,.72f,.75f,.74f,.72f,.69f,.64f,.56f,.43f};
            float[] hy={.012f,.045f,.09f,.15f,.23f,.30f,.36f,.40f,.42f,.41f,.39f,.36f,.33f,.28f,.22f};
            b.smoothLoft(z,wx,hy,24,skin,side,under,0f);

            // Blended chine shoulders that visually connect nose/fuselage to wing roots.
            b.chine(-1,skin2,side); b.chine(1,skin2,side);
            wing(b,-1,skin,skin2,side,under); wing(b,1,skin,skin2,side,under);
            stab(b,-1,skin,side); stab(b,1,skin,side); fin(b,-.69f,-1,skin,side); fin(b,.69f,1,skin,side);

            // Smooth glossy canopy bubble.
            b.smoothLoft(new float[]{-1.88f,-1.60f,-1.25f,-.86f,-.52f},new float[]{.035f,.16f,.25f,.27f,.11f},new float[]{.03f,.14f,.22f,.23f,.08f},20,glass,glass,glass,.38f);

            // Engine shoulders and petal nozzles.
            b.roundBox(-.35f,-.14f,1.34f,.50f,.42f,1.42f,8,skin2,side); b.roundBox(.35f,-.14f,1.34f,.50f,.42f,1.42f,8,skin2,side);
            b.tube(-.35f,-.14f,2.50f,.238f,.33f,32,nozzle); b.tube(.35f,-.14f,2.50f,.238f,.33f,32,nozzle);
            for(int i=0;i<12;i++){ double a=2*Math.PI*i/12; float x=(float)Math.cos(a)*.238f,y=(float)Math.sin(a)*.238f; b.panelPetal(-.35f+x,-.14f+y,2.65f,(float)a,seam); b.panelPetal(.35f+x,-.14f+y,2.65f,(float)a,seam); }

            // Intake lips/openings, subtly darker but not black.
            float[] intake={.22f,.25f,.27f,1f};
            b.q(p(-.76f,-.07f,-.78f),p(-.43f,-.13f,-.35f),p(-.39f,.13f,-.35f),p(-.69f,.14f,-.78f),intake);
            b.q(p(.43f,-.13f,-.35f),p(.76f,-.07f,-.78f),p(.69f,.14f,-.78f),p(.39f,.13f,-.35f),intake);

            // Very subtle control-surface/panel seams.
            b.seam(-1.0f,.045f,-.35f,1.15f,1.45f,seam); b.seam(1.0f,.045f,-.35f,1.15f,1.45f,seam);
            b.seam(-1.0f,.055f,1.46f,.55f,1.05f,seam); b.seam(1.0f,.055f,1.46f,.55f,1.05f,seam);
            return b.mesh();
        }
        Mesh buildFlame(){ B b=new B(); float[] o={1f,.24f,.025f,.72f},i={1f,.84f,.16f,.94f}; b.cone(-.35f,-.14f,2.70f,.185f,1.10f,28,o); b.cone(.35f,-.14f,2.70f,.185f,1.10f,28,o); b.cone(-.35f,-.14f,2.72f,.085f,.70f,22,i); b.cone(.35f,-.14f,2.72f,.085f,.70f,22,i); return b.mesh(); }

        static void wing(B b,float s,float[] top,float[] root,float[] side,float[] bot){
            float yt=.085f,yb=-.065f; float[] a=p(s*.46f,yt,-1.34f),m=p(s*1.15f,yt,-.82f),c=p(s*2.66f,yt,.00f),d=p(s*1.86f,yt,1.37f),e=p(s*.54f,yt,1.18f);
            float[] a2=p(a[0],yb,a[2]),m2=p(m[0],yb,m[2]),c2=p(c[0],yb,c[2]),d2=p(d[0],yb,d[2]),e2=p(e[0],yb,e[2]);
            b.q(a,m,c,e,root); b.t(c,d,e,top); b.q(e2,d2,c2,a2,bot); b.t(a2,c2,m2,bot); b.q(a,a2,m2,m,side); b.q(m,m2,c2,c,side); b.q(c,c2,d2,d,side); b.q(d,d2,e2,e,side);
        }
        static void stab(B b,float s,float[] t,float[] side){ float[] a=p(s*.45f,.015f,1.30f),c=p(s*1.63f,.02f,1.80f),e=p(s*1.32f,.02f,2.42f),f=p(s*.50f,.02f,2.10f); b.q(a,c,e,f,t); }
        static void fin(B b,float x,float s,float[] t,float[] side){ float[] a=p(x,.20f,1.00f),c=p(x+s*.24f,1.40f,1.48f),e=p(x+s*.30f,1.10f,2.24f),f=p(x,.22f,2.12f); b.q(a,c,e,f,t); }

        static int prog(String v,String f){ int a=sh(GLES20.GL_VERTEX_SHADER,v),b=sh(GLES20.GL_FRAGMENT_SHADER,f),p=GLES20.glCreateProgram(); GLES20.glAttachShader(p,a); GLES20.glAttachShader(p,b); GLES20.glLinkProgram(p); return p; }
        static int sh(int t,String s){ int a=GLES20.glCreateShader(t); GLES20.glShaderSource(a,s); GLES20.glCompileShader(a); return a; }
        static final String VS="uniform mat4 uMvp;uniform mat4 uModel;attribute vec3 aPos;attribute vec3 aNormal;attribute vec4 aColor;varying vec3 n;varying vec4 c;varying vec3 wp;void main(){vec4 w=uModel*vec4(aPos,1.0);wp=w.xyz;n=normalize(mat3(uModel)*aNormal);c=aColor;gl_Position=uMvp*vec4(aPos,1.0);}";
        static final String FS="precision mediump float;uniform vec3 uLight;uniform float uEmissive;uniform float uGloss;varying vec3 n;varying vec4 c;varying vec3 wp;void main(){vec3 N=normalize(n);vec3 L=normalize(uLight);float diff=max(0.0,dot(N,L));float fres=pow(1.0-abs(N.z),3.0);float spec=pow(max(0.0,dot(reflect(-L,N),vec3(0.0,0.25,0.97))),24.0)*uGloss;float sky=.05*(N.y*.5+.5);float l=.36+.56*diff+sky+.13*fres+spec+uEmissive;gl_FragColor=vec4(c.rgb*l,c.a);}";
    }

    static final class Mesh{ final FloatBuffer p,n,c; final int count; Mesh(float[] a,float[] b,float[] d){p=buf(a);n=buf(b);c=buf(d);count=a.length/3;} void draw(int ap,int an,int ac){GLES20.glEnableVertexAttribArray(ap);GLES20.glVertexAttribPointer(ap,3,GLES20.GL_FLOAT,false,0,p);GLES20.glEnableVertexAttribArray(an);GLES20.glVertexAttribPointer(an,3,GLES20.GL_FLOAT,false,0,n);GLES20.glEnableVertexAttribArray(ac);GLES20.glVertexAttribPointer(ac,4,GLES20.GL_FLOAT,false,0,c);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);} static FloatBuffer buf(float[] a){FloatBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(a).position(0);return b;} }

    static final class B{
        final ArrayList<Float>P=new ArrayList<>(),N=new ArrayList<>(),C=new ArrayList<>();
        void add(float[]v,float[]n,float[]co){for(float x:v)P.add(x);for(float x:n)N.add(x);for(float x:co)C.add(x);} void t(float[]a,float[]b,float[]c,float[]co){float[]n=norm(a,b,c);add(a,n,co);add(b,n,co);add(c,n,co);} void q(float[]a,float[]b,float[]c,float[]d,float[]co){t(a,b,c,co);t(a,c,d,co);}
        void smoothLoft(float[]z,float[]wx,float[]hy,int seg,float[]top,float[]side,float[]bottom,float yOff){
            for(int k=0;k<z.length-1;k++) for(int i=0;i<seg;i++){
                double a=2*Math.PI*i/seg,bb=2*Math.PI*(i+1)/seg; float[] p0=p((float)Math.cos(a)*wx[k],yOff+(float)Math.sin(a)*hy[k],z[k]); float[] p1=p((float)Math.cos(bb)*wx[k],yOff+(float)Math.sin(bb)*hy[k],z[k]); float[] p2=p((float)Math.cos(bb)*wx[k+1],yOff+(float)Math.sin(bb)*hy[k+1],z[k+1]); float[] p3=p((float)Math.cos(a)*wx[k+1],yOff+(float)Math.sin(a)*hy[k+1],z[k+1]);
                float[] n0=loftN(a,wx[k],hy[k]),n1=loftN(bb,wx[k],hy[k]),n2=loftN(bb,wx[k+1],hy[k+1]),n3=loftN(a,wx[k+1],hy[k+1]); float mid=(float)Math.sin((a+bb)/2); float[] co=mid>.18?top:(mid<-.35?bottom:side);
                add(p0,n0,co);add(p1,n1,co);add(p2,n2,co); add(p0,n0,co);add(p2,n2,co);add(p3,n3,co);
            }
        }
        static float[] loftN(double a,float wx,float hy){ float nx=(float)Math.cos(a)/Math.max(.01f,wx),ny=(float)Math.sin(a)/Math.max(.01f,hy),l=(float)Math.sqrt(nx*nx+ny*ny);return new float[]{nx/l,ny/l,0}; }
        void chine(float s,float[]top,float[]side){ q(p(s*.34f,.18f,-2.35f),p(s*.72f,.16f,-1.28f),p(s*.95f,.12f,-.35f),p(s*.52f,.20f,-.82f),top); q(p(s*.52f,.20f,-.82f),p(s*.95f,.12f,-.35f),p(s*.72f,.10f,.52f),p(s*.48f,.18f,.25f),side); }
        void roundBox(float cx,float cy,float cz,float sx,float sy,float sz,int seg,float[]top,float[]side){float[] z={cz-sz/2,cz+sz/2},wx={sx/2,sx/2},hy={sy/2,sy/2};smoothLoft(z,wx,hy,seg,top,side,side,cy);}
        void tube(float cx,float cy,float z,float r,float len,int seg,float[]co){for(int i=0;i<seg;i++){double a=2*Math.PI*i/seg,b=2*Math.PI*(i+1)/seg;float[]p0=p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z-len/2),p1=p(cx+(float)Math.cos(b)*r,cy+(float)Math.sin(b)*r,z-len/2),p2=p(cx+(float)Math.cos(b)*r,cy+(float)Math.sin(b)*r,z+len/2),p3=p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z+len/2);q(p0,p1,p2,p3,co);}}
        void cone(float cx,float cy,float z,float r,float len,int seg,float[]co){float[]tip=p(cx,cy,z+len);for(int i=0;i<seg;i++){double a=2*Math.PI*i/seg,b=2*Math.PI*(i+1)/seg;t(p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z),p(cx+(float)Math.cos(b)*r,cy+(float)Math.sin(b)*r,z),tip,co);}}
        void panelPetal(float x,float y,float z,float a,float[]co){float dx=(float)Math.cos(a)*.035f,dy=(float)Math.sin(a)*.035f;t(p(x-dx,y-dy,z-.10f),p(x+dx,y+dy,z-.10f),p(x,y,z+.08f),co);}
        void seam(float s,float y,float z,float len,float span,float[]co){float x0=s*.58f,x1=s*span;float e=.009f;q(p(x0,y,z),p(x1,y,z+len*.35f),p(x1,y+.004f,z+len*.35f+.018f),p(x0,y+.004f,z+.018f),co);}
        Mesh mesh(){return new Mesh(arr(P),arr(N),arr(C));} static float[]arr(ArrayList<Float>a){float[]r=new float[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;} static float[]norm(float[]a,float[]b,float[]c){float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2],nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6)l=1;return new float[]{nx/l,ny/l,nz/l};}
    }
    static float[] p(float x,float y,float z){return new float[]{x,y,z};}
}
