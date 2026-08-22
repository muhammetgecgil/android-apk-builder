package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import java.nio.*;
import java.util.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** V11 poster-inspired wide-body stealth fighter renderer.
 *  Visual/game model only: broad blended fuselage, swept wings, twin tails,
 *  large canopy, shoulder engines and poster-like proportions.
 */
public final class Jet3DViewV11 extends GLSurfaceView {
    private final R r;
    public Jet3DViewV11(Context c){
        super(c);
        setEGLContextClientVersion(2);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(false);
        r=new R();
        setRenderer(r);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
    }
    public void setTelemetry(float roll,float pitch,float yaw,float thr,float hz,int drops,boolean live){r.set(roll,pitch,yaw,thr,live);}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP)r.nextCamera();return true;}

    private static final class R implements Renderer{
        final float[] proj=new float[16],view=new float[16],model=new float[16],mv=new float[16],mvp=new float[16];
        float tr,tp,ty,thr=.72f,roll,pitch,yaw,vr,vp,vy;boolean live;long last;
        int cam,program,aP,aN,aC,uMvp,uModel,uLight,uEm,uGloss;float camBlend=1f;
        Mesh airframe,glass,nozzles,flame;
        void set(float a,float b,float c,float t,boolean l){tr=a;tp=b;ty=c;thr=t;live=l;}
        void nextCamera(){cam=(cam+1)%5;camBlend=0f;}

        @Override public void onSurfaceCreated(GL10 gl,EGLConfig cfg){
            GLES20.glClearColor(0,0,0,0);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
            program=prog(VS,FS);aP=GLES20.glGetAttribLocation(program,"aPos");aN=GLES20.glGetAttribLocation(program,"aNormal");aC=GLES20.glGetAttribLocation(program,"aColor");
            uMvp=GLES20.glGetUniformLocation(program,"uMvp");uModel=GLES20.glGetUniformLocation(program,"uModel");uLight=GLES20.glGetUniformLocation(program,"uLight");uEm=GLES20.glGetUniformLocation(program,"uEmissive");uGloss=GLES20.glGetUniformLocation(program,"uGloss");
            airframe=buildAirframe();glass=buildGlass();nozzles=buildNozzles();flame=buildFlame();last=System.nanoTime();
        }
        @Override public void onSurfaceChanged(GL10 gl,int w,int h){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,38f,(float)w/Math.max(1,h),.08f,140f);}
        @Override public void onDrawFrame(GL10 gl){
            long n=System.nanoTime();float dt=Math.min(.035f,Math.max(.001f,(n-last)/1e9f));last=n;
            float kr=22f,kp=20f,ky=12f,dam=2f*(float)Math.sqrt(kr);vr+=(shortest(tr-roll)*kr-vr*dam)*dt;roll+=vr*dt;
            dam=2f*(float)Math.sqrt(kp);vp+=((tp-pitch)*kp-vp*dam)*dt;pitch+=vp*dt;
            dam=2f*(float)Math.sqrt(ky);vy+=(shortest(ty-yaw)*ky-vy*dam)*dt;yaw+=vy*dt;
            camBlend=Math.min(1f,camBlend+dt*1.8f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);camera();
            Matrix.setIdentityM(model,0);Matrix.rotateM(model,0,-yaw*.14f,0,1,0);Matrix.rotateM(model,0,pitch,1,0,0);Matrix.rotateM(model,0,-roll,0,0,1);
            Matrix.multiplyMM(mv,0,view,0,model,0);Matrix.multiplyMM(mvp,0,proj,0,mv,0);
            GLES20.glUseProgram(program);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,model,0);GLES20.glUniform3f(uLight,-.38f,.86f,.54f);
            draw(airframe,.48f,0f);draw(nozzles,.18f,0f);draw(glass,.92f,0f);
            if(live&&thr>.46f)draw(flame,0f,.82f+.16f*(float)Math.sin(n/45_000_000.0));
        }
        void draw(Mesh m,float gloss,float em){GLES20.glUniform1f(uGloss,gloss);GLES20.glUniform1f(uEm,em);m.draw(aP,aN,aC);}
        void camera(){
            float[][] e={{0,2.35f,9.4f},{0,1.55f,6.45f},{4.8f,3.25f,6.0f},{-5.7f,2.0f,4.0f},{0,5.4f,5.1f}};
            float[] q=e[cam];float s=camBlend*camBlend*(3f-2f*camBlend);float ex=q[0]*s,ey=2.35f+(q[1]-2.35f)*s,ez=9.4f+(q[2]-9.4f)*s;
            Matrix.setLookAtM(view,0,ex,ey,ez,0,.03f,-.20f,0,1,0);
        }
        static float shortest(float d){while(d>180)d-=360;while(d<-180)d+=360;return d;}

        Mesh buildAirframe(){
            B b=new B();
            float[] top={.57f,.59f,.61f,1},top2={.52f,.55f,.58f,1},side={.46f,.49f,.52f,1},under={.40f,.43f,.46f,1};
            // Broad poster-like central body: short sharp nose, wide shoulders, powerful rear fuselage.
            float[] z={-4.05f,-3.82f,-3.48f,-3.05f,-2.55f,-2.00f,-1.38f,-.72f,-.05f,.62f,1.24f,1.78f,2.20f,2.52f};
            float[] wx={.01f,.07f,.18f,.34f,.53f,.72f,.88f,.98f,1.02f,1.00f,.94f,.86f,.73f,.56f};
            float[] hy={.01f,.04f,.10f,.18f,.28f,.38f,.47f,.52f,.55f,.53f,.48f,.42f,.34f,.25f};
            b.smoothLoft(z,wx,hy,28,top,side,under,0);
            // Wide blended shoulders/chines.
            chine(b,-1,top2,side);chine(b,1,top2,side);
            // Large swept wings with broad root and clipped tips.
            wing(b,-1,top,top2,side,under);wing(b,1,top,top2,side,under);
            // Larger horizontal tails.
            htail(b,-1,top,side);htail(b,1,top,side);
            // Tall canted twin tails, poster silhouette.
            vtail(b,-.78f,-1,top2,side);vtail(b,.78f,1,top2,side);
            // Raised engine shoulders to thicken rear body.
            b.roundBox(-.46f,-.10f,1.18f,.66f,.52f,1.95f,12,top2,side);b.roundBox(.46f,-.10f,1.18f,.66f,.52f,1.95f,12,top2,side);
            // Intake lips.
            float[] intake={.19f,.22f,.24f,1};
            b.q(p(-1.00f,-.10f,-.95f),p(-.56f,-.18f,-.35f),p(-.51f,.19f,-.32f),p(-.88f,.18f,-.93f),intake);
            b.q(p(.56f,-.18f,-.35f),p(1.00f,-.10f,-.95f),p(.88f,.18f,-.93f),p(.51f,.19f,-.32f),intake);
            // subtle wing control-surface seams
            float[] seam={.34f,.36f,.38f,1};b.seam(-1,.09f,.50f,.92f,1.55f,seam);b.seam(1,.09f,.50f,.92f,1.55f,seam);
            return b.mesh();
        }
        Mesh buildGlass(){B b=new B();float[] g={.035f,.12f,.16f,.93f};b.smoothLoft(new float[]{-2.15f,-1.82f,-1.45f,-1.02f,-.68f},new float[]{.04f,.20f,.32f,.34f,.13f},new float[]{.03f,.16f,.27f,.28f,.10f},24,g,g,g,.50f);return b.mesh();}
        Mesh buildNozzles(){B b=new B();float[] n={.10f,.105f,.11f,1},rim={.22f,.23f,.24f,1};b.tube(-.46f,-.12f,2.58f,.31f,.40f,36,n);b.tube(.46f,-.12f,2.58f,.31f,.40f,36,n);b.tube(-.46f,-.12f,2.79f,.34f,.06f,36,rim);b.tube(.46f,-.12f,2.79f,.34f,.06f,36,rim);return b.mesh();}
        Mesh buildFlame(){B b=new B();float[] o={1,.22f,.02f,.70f},i={1,.78f,.10f,.94f};b.cone(-.46f,-.12f,2.82f,.25f,1.35f,32,o);b.cone(.46f,-.12f,2.82f,.25f,1.35f,32,o);b.cone(-.46f,-.12f,2.84f,.12f,.82f,24,i);b.cone(.46f,-.12f,2.84f,.12f,.82f,24,i);return b.mesh();}

        static void chine(B b,float s,float[]t,float[]d){b.q(p(s*.40f,.21f,-2.65f),p(s*.92f,.18f,-1.55f),p(s*1.24f,.12f,-.45f),p(s*.64f,.24f,-.78f),t);b.q(p(s*.64f,.24f,-.78f),p(s*1.24f,.12f,-.45f),p(s*.94f,.10f,.72f),p(s*.57f,.20f,.30f),d);}
        static void wing(B b,float s,float[]top,float[]root,float[]side,float[]bot){
            float yt=.11f,yb=-.075f;float[] a=p(s*.58f,yt,-1.58f),m=p(s*1.32f,yt,-.95f),c=p(s*3.15f,yt,.05f),d=p(s*2.30f,yt,1.48f),e=p(s*.72f,yt,1.24f);
            float[] a2=p(a[0],yb,a[2]),m2=p(m[0],yb,m[2]),c2=p(c[0],yb,c[2]),d2=p(d[0],yb,d[2]),e2=p(e[0],yb,e[2]);
            b.q(a,m,c,e,root);b.t(c,d,e,top);b.q(e2,d2,c2,a2,bot);b.t(a2,c2,m2,bot);b.q(a,a2,m2,m,side);b.q(m,m2,c2,c,side);b.q(c,c2,d2,d,side);b.q(d,d2,e2,e,side);
        }
        static void htail(B b,float s,float[]t,float[]d){b.q(p(s*.58f,.03f,1.24f),p(s*1.92f,.03f,1.78f),p(s*1.53f,.03f,2.54f),p(s*.62f,.03f,2.16f),t);}
        static void vtail(B b,float x,float s,float[]t,float[]d){b.q(p(x,.24f,.92f),p(x+s*.38f,1.72f,1.36f),p(x+s*.46f,1.34f,2.48f),p(x,.26f,2.22f),t);}

        static int prog(String v,String f){int a=sh(GLES20.GL_VERTEX_SHADER,v),b=sh(GLES20.GL_FRAGMENT_SHADER,f),p=GLES20.glCreateProgram();GLES20.glAttachShader(p,a);GLES20.glAttachShader(p,b);GLES20.glLinkProgram(p);return p;}
        static int sh(int t,String s){int a=GLES20.glCreateShader(t);GLES20.glShaderSource(a,s);GLES20.glCompileShader(a);return a;}
        static final String VS="uniform mat4 uMvp;uniform mat4 uModel;attribute vec3 aPos;attribute vec3 aNormal;attribute vec4 aColor;varying vec3 n;varying vec4 c;void main(){n=normalize(mat3(uModel)*aNormal);c=aColor;gl_Position=uMvp*vec4(aPos,1.0);}";
        static final String FS="precision mediump float;uniform vec3 uLight;uniform float uEmissive;uniform float uGloss;varying vec3 n;varying vec4 c;void main(){vec3 N=normalize(n);vec3 L=normalize(uLight);float diff=max(0.0,dot(N,L));float fres=pow(1.0-abs(N.z),2.6);float spec=pow(max(0.0,dot(reflect(-L,N),vec3(0.0,0.22,0.98))),28.0)*uGloss;float l=.34+.58*diff+.15*fres+spec+uEmissive;gl_FragColor=vec4(c.rgb*l,c.a);}";
    }

    static final class Mesh{final FloatBuffer p,n,c;final int count;Mesh(float[]a,float[]b,float[]d){p=buf(a);n=buf(b);c=buf(d);count=a.length/3;}void draw(int ap,int an,int ac){GLES20.glEnableVertexAttribArray(ap);GLES20.glVertexAttribPointer(ap,3,GLES20.GL_FLOAT,false,0,p);GLES20.glEnableVertexAttribArray(an);GLES20.glVertexAttribPointer(an,3,GLES20.GL_FLOAT,false,0,n);GLES20.glEnableVertexAttribArray(ac);GLES20.glVertexAttribPointer(ac,4,GLES20.GL_FLOAT,false,0,c);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);}static FloatBuffer buf(float[]a){FloatBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(a).position(0);return b;}}
    static final class B{
        final ArrayList<Float>P=new ArrayList<>(),N=new ArrayList<>(),C=new ArrayList<>();
        void add(float[]v,float[]n,float[]co){for(float x:v)P.add(x);for(float x:n)N.add(x);for(float x:co)C.add(x);}void t(float[]a,float[]b,float[]c,float[]co){float[]n=norm(a,b,c);add(a,n,co);add(b,n,co);add(c,n,co);}void q(float[]a,float[]b,float[]c,float[]d,float[]co){t(a,b,c,co);t(a,c,d,co);}
        void smoothLoft(float[]z,float[]wx,float[]hy,int seg,float[]top,float[]side,float[]bottom,float yOff){for(int k=0;k<z.length-1;k++)for(int i=0;i<seg;i++){double a=2*Math.PI*i/seg,bb=2*Math.PI*(i+1)/seg;float[]p0=p((float)Math.cos(a)*wx[k],yOff+(float)Math.sin(a)*hy[k],z[k]),p1=p((float)Math.cos(bb)*wx[k],yOff+(float)Math.sin(bb)*hy[k],z[k]),p2=p((float)Math.cos(bb)*wx[k+1],yOff+(float)Math.sin(bb)*hy[k+1],z[k+1]),p3=p((float)Math.cos(a)*wx[k+1],yOff+(float)Math.sin(a)*hy[k+1],z[k+1]);float[]n0=loftN(a,wx[k],hy[k]),n1=loftN(bb,wx[k],hy[k]),n2=loftN(bb,wx[k+1],hy[k+1]),n3=loftN(a,wx[k+1],hy[k+1]);float mid=(float)Math.sin((a+bb)/2),coo[]=mid>.18?top:(mid<-.35?bottom:side);add(p0,n0,coo);add(p1,n1,coo);add(p2,n2,coo);add(p0,n0,coo);add(p2,n2,coo);add(p3,n3,coo);}}
        static float[]loftN(double a,float wx,float hy){float nx=(float)Math.cos(a)/Math.max(.01f,wx),ny=(float)Math.sin(a)/Math.max(.01f,hy),l=(float)Math.sqrt(nx*nx+ny*ny);return new float[]{nx/l,ny/l,0};}
        void roundBox(float cx,float cy,float cz,float sx,float sy,float sz,int seg,float[]top,float[]side){float[]z={cz-sz/2,cz+sz/2},wx={sx/2,sx/2},hy={sy/2,sy/2};smoothLoft(z,wx,hy,seg,top,side,side,cy);}
        void tube(float cx,float cy,float z,float r,float len,int seg,float[]co){for(int i=0;i<seg;i++){double a=2*Math.PI*i/seg,bb=2*Math.PI*(i+1)/seg;float[]p0=p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z-len/2),p1=p(cx+(float)Math.cos(bb)*r,cy+(float)Math.sin(bb)*r,z-len/2),p2=p(cx+(float)Math.cos(bb)*r,cy+(float)Math.sin(bb)*r,z+len/2),p3=p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z+len/2);q(p0,p1,p2,p3,co);}}
        void cone(float cx,float cy,float z,float r,float len,int seg,float[]co){float[]tip=p(cx,cy,z+len);for(int i=0;i<seg;i++){double a=2*Math.PI*i/seg,bb=2*Math.PI*(i+1)/seg;t(p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z),p(cx+(float)Math.cos(bb)*r,cy+(float)Math.sin(bb)*r,z),tip,co);}}
        void seam(float s,float y,float z,float len,float span,float[]co){float x0=s*.70f,x1=s*span;q(p(x0,y,z),p(x1,y,z+len*.34f),p(x1,y+.004f,z+len*.34f+.018f),p(x0,y+.004f,z+.018f),co);}
        Mesh mesh(){return new Mesh(arr(P),arr(N),arr(C));}static float[]arr(ArrayList<Float>a){float[]r=new float[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;}static float[]norm(float[]a,float[]b,float[]c){float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2],nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6)l=1;return new float[]{nx/l,ny/l,nz/l};}
    }
    static float[]p(float x,float y,float z){return new float[]{x,y,z};}
}
