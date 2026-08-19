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

/**
 * Lightweight procedural stealth-fighter renderer.
 * No external 3D asset is required: the aircraft is built from real 3D triangles,
 * normals and separate engine / canopy / tail geometry at runtime.
 */
public final class Jet3DView extends GLSurfaceView {
    private final JetRenderer jetRenderer;

    public Jet3DView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        jetRenderer = new JetRenderer();
        setRenderer(jetRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
    }

    public void setTelemetry(float roll, float pitch, float yaw, float throttle, float linkHz, int drops, boolean live) {
        jetRenderer.setTelemetry(roll, pitch, yaw, throttle, linkHz, drops, live);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            jetRenderer.nextCamera();
            return true;
        }
        return true;
    }

    private static final class JetRenderer implements GLSurfaceView.Renderer {
        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mv = new float[16];
        private final float[] mvp = new float[16];

        private volatile float targetRoll, targetPitch, targetYaw, throttle = 0.62f, linkHz;
        private volatile int drops;
        private volatile boolean live;
        private float roll, pitch, yaw;
        private int cameraMode = 0;
        private long lastNs;

        private int program;
        private int aPos, aNormal, aColor;
        private int uMvp, uModel, uLight, uEmissive;
        private Mesh jet;
        private Mesh flames;

        void setTelemetry(float r, float p, float y, float t, float hz, int d, boolean l) {
            targetRoll = r; targetPitch = p; targetYaw = y; throttle = t; linkHz = hz; drops = d; live = l;
        }
        void nextCamera() { cameraMode = (cameraMode + 1) % 4; }

        @Override public void onSurfaceCreated(javax.microedition.khronos.egl.EGLConfig config) {
            GLES20.glClearColor(0.035f, 0.105f, 0.18f, 1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER);
            aPos = GLES20.glGetAttribLocation(program, "aPos");
            aNormal = GLES20.glGetAttribLocation(program, "aNormal");
            aColor = GLES20.glGetAttribLocation(program, "aColor");
            uMvp = GLES20.glGetUniformLocation(program, "uMvp");
            uModel = GLES20.glGetUniformLocation(program, "uModel");
            uLight = GLES20.glGetUniformLocation(program, "uLight");
            uEmissive = GLES20.glGetUniformLocation(program, "uEmissive");
            jet = buildJet();
            flames = buildFlames();
            lastNs = System.nanoTime();
        }

        @Override public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float aspect = Math.max(0.1f, (float) width / Math.max(1, height));
            Matrix.perspectiveM(projection, 0, 46f, aspect, 0.1f, 60f);
        }

        @Override public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
            long now = System.nanoTime();
            float dt = Math.min(0.05f, Math.max(0.001f, (now - lastNs) / 1_000_000_000f));
            lastNs = now;
            float k = 1f - (float)Math.exp(-dt * 8.5f);
            roll += shortest(targetRoll - roll) * k;
            pitch += (targetPitch - pitch) * k;
            yaw += shortest(targetYaw - yaw) * k * 0.7f;

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            setCamera();

            Matrix.setIdentityM(model, 0);
            Matrix.rotateM(model, 0, -yaw * 0.35f, 0, 1, 0);
            Matrix.rotateM(model, 0, pitch, 1, 0, 0);
            Matrix.rotateM(model, 0, -roll, 0, 0, 1);
            Matrix.multiplyMM(mv, 0, view, 0, model, 0);
            Matrix.multiplyMM(mvp, 0, projection, 0, mv, 0);

            GLES20.glUseProgram(program);
            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
            GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
            GLES20.glUniform3f(uLight, -0.35f, 0.8f, 0.55f);
            GLES20.glUniform1f(uEmissive, 0f);
            jet.draw(aPos, aNormal, aColor);

            if (throttle > 0.52f && live) {
                float pulse = 0.92f + 0.08f * (float)Math.sin(now / 45_000_000.0);
                GLES20.glUniform1f(uEmissive, pulse * Math.min(1f, 0.35f + throttle));
                flames.draw(aPos, aNormal, aColor);
            }
        }

        private void setCamera() {
            switch (cameraMode) {
                case 1: // close chase
                    Matrix.setLookAtM(view, 0, 0f, 1.55f, 6.2f, 0f, 0.05f, 0.25f, 0, 1, 0); break;
                case 2: // high quarter
                    Matrix.setLookAtM(view, 0, 4.8f, 4.4f, 6.8f, 0f, 0f, 0f, 0, 1, 0); break;
                case 3: // cinematic side
                    Matrix.setLookAtM(view, 0, -6.2f, 2.5f, 4.0f, 0f, 0f, 0f, 0, 1, 0); break;
                default: // rear chase
                    Matrix.setLookAtM(view, 0, 0f, 2.4f, 8.4f, 0f, 0f, -0.25f, 0, 1, 0); break;
            }
        }

        private static float shortest(float d) {
            while (d > 180f) d -= 360f;
            while (d < -180f) d += 360f;
            return d;
        }

        private Mesh buildJet() {
            Builder b = new Builder();
            float[] body = {0.39f,0.43f,0.46f,1f};
            float[] edge = {0.27f,0.31f,0.34f,1f};
            float[] dark = {0.12f,0.16f,0.18f,1f};
            float[] canopy = {0.045f,0.16f,0.22f,1f};
            float[] nozzle = {0.10f,0.10f,0.105f,1f};

            // Main faceted fuselage / chines.
            float[] nose = p(0, 0.05f, -3.35f);
            float[] tl = p(-0.58f, 0.28f, -1.45f), tr = p(0.58f,0.28f,-1.45f);
            float[] bl = p(-0.62f,-0.20f,-1.25f), br = p(0.62f,-0.20f,-1.25f);
            float[] atl = p(-0.54f,0.26f,1.95f), atr = p(0.54f,0.26f,1.95f);
            float[] abl = p(-0.55f,-0.25f,2.15f), abr = p(0.55f,-0.25f,2.15f);
            b.tri(nose, tl, tr, body); b.tri(nose, bl, tl, edge); b.tri(nose, tr, br, edge); b.tri(nose, br, bl, dark);
            b.quad(tl, atl, atr, tr, body); b.quad(bl, br, abr, abl, dark); b.quad(tl, bl, abl, atl, edge); b.quad(tr, atr, abr, br, edge);
            b.quad(atl, abl, abr, atr, edge);

            // Stealth delta wings - real thickness, top and bottom faces.
            wing(b, -1f, body, edge, dark);
            wing(b,  1f, body, edge, dark);

            // Horizontal stabilators.
            stabilator(b, -1f, edge, dark);
            stabilator(b,  1f, edge, dark);

            // Twin canted vertical tails.
            fin(b, -0.66f, -1f, edge, dark);
            fin(b,  0.66f,  1f, edge, dark);

            // Canopy wedge.
            float[] c0=p(0,0.58f,-1.55f), c1=p(-0.28f,0.33f,-0.98f), c2=p(0.28f,0.33f,-0.98f), c3=p(0,0.52f,-0.42f);
            b.tri(c0,c1,c2,canopy); b.tri(c0,c3,c1,canopy); b.tri(c0,c2,c3,canopy); b.tri(c1,c3,c2,canopy);

            // Engine nacelles and circular-ish nozzles.
            b.box(-0.34f,-0.16f,1.35f,0.42f,0.42f,1.35f,edge,dark);
            b.box( 0.34f,-0.16f,1.35f,0.42f,0.42f,1.35f,edge,dark);
            b.cylinder(-0.34f,-0.16f,2.46f,0.24f,0.28f,16,nozzle);
            b.cylinder( 0.34f,-0.16f,2.46f,0.24f,0.28f,16,nozzle);

            // Intake lips / dark openings.
            b.quad(p(-0.72f,-0.08f,-0.65f),p(-0.38f,-0.12f,-0.35f),p(-0.36f,0.12f,-0.35f),p(-0.66f,0.12f,-0.65f),dark);
            b.quad(p(0.38f,-0.12f,-0.35f),p(0.72f,-0.08f,-0.65f),p(0.66f,0.12f,-0.65f),p(0.36f,0.12f,-0.35f),dark);
            return b.mesh();
        }

        private static void wing(Builder b, float s, float[] top, float[] side, float[] bottom) {
            float yTop=0.10f,yBot=-0.08f;
            float[] a=p(s*0.42f,yTop,-1.15f), c=p(s*2.45f,yTop,0.12f), d=p(s*1.55f,yTop,1.35f), e=p(s*0.48f,yTop,1.12f);
            float[] ab=p(a[0],yBot,a[2]), cb=p(c[0],yBot,c[2]), db=p(d[0],yBot,d[2]), eb=p(e[0],yBot,e[2]);
            b.quad(a,c,d,e,top); b.quad(eb,db,cb,ab,bottom); b.quad(a,ab,cb,c,side); b.quad(c,cb,db,d,side); b.quad(d,db,eb,e,side);
        }

        private static void stabilator(Builder b,float s,float[] top,float[] bottom){
            float[] a=p(s*0.42f,0.02f,1.25f), c=p(s*1.55f,0.03f,1.78f), d=p(s*1.25f,0.02f,2.35f), e=p(s*0.48f,0.02f,2.08f);
            b.quad(a,c,d,e,top);
            float[] a2=p(a[0],-0.06f,a[2]),c2=p(c[0],-0.06f,c[2]),d2=p(d[0],-0.06f,d[2]),e2=p(e[0],-0.06f,e[2]);
            b.quad(e2,d2,c2,a2,bottom); b.quad(c,c2,d2,d,bottom);
        }

        private static void fin(Builder b,float x,float s,float[] color,float[] side){
            float[] a=p(x,0.20f,0.95f), c=p(x+s*0.16f,1.28f,1.45f), d=p(x+s*0.22f,1.00f,2.16f), e=p(x,0.22f,2.08f);
            float[] a2=p(x-s*0.07f,0.18f,0.95f),c2=p(c[0]-s*0.07f,c[1],c[2]),d2=p(d[0]-s*0.07f,d[1],d[2]),e2=p(x-s*0.07f,0.20f,2.08f);
            b.quad(a,c,d,e,color); b.quad(e2,d2,c2,a2,side); b.quad(c,c2,d2,d,side);
        }

        private Mesh buildFlames(){
            Builder b=new Builder();
            float[] outer={1.0f,0.28f,0.035f,0.82f};
            float[] inner={1.0f,0.82f,0.16f,0.95f};
            b.cone(-0.34f,-0.16f,2.62f,0.19f,0.95f,14,outer);
            b.cone( 0.34f,-0.16f,2.62f,0.19f,0.95f,14,outer);
            b.cone(-0.34f,-0.16f,2.64f,0.09f,0.64f,12,inner);
            b.cone( 0.34f,-0.16f,2.64f,0.09f,0.64f,12,inner);
            return b.mesh();
        }

        private static int buildProgram(String vs,String fs){
            int v=compile(GLES20.GL_VERTEX_SHADER,vs), f=compile(GLES20.GL_FRAGMENT_SHADER,fs);
            int p=GLES20.glCreateProgram(); GLES20.glAttachShader(p,v); GLES20.glAttachShader(p,f); GLES20.glLinkProgram(p);
            int[] ok=new int[1]; GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);
            if(ok[0]==0) throw new RuntimeException("GL link: "+GLES20.glGetProgramInfoLog(p));
            return p;
        }
        private static int compile(int type,String src){
            int s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,src); GLES20.glCompileShader(s);
            int[] ok=new int[1]; GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);
            if(ok[0]==0) throw new RuntimeException("GL shader: "+GLES20.glGetShaderInfoLog(s));
            return s;
        }

        private static final String VERTEX_SHADER =
                "uniform mat4 uMvp; uniform mat4 uModel; uniform vec3 uLight; uniform float uEmissive;"+
                "attribute vec3 aPos; attribute vec3 aNormal; attribute vec4 aColor; varying vec4 vColor;"+
                "void main(){ vec3 n=normalize(mat3(uModel)*aNormal); float d=max(dot(n,normalize(uLight)),0.0);"+
                "float lit=mix(0.25+0.75*d,1.0,uEmissive); vColor=vec4(aColor.rgb*lit,aColor.a); gl_Position=uMvp*vec4(aPos,1.0); }";
        private static final String FRAGMENT_SHADER =
                "precision mediump float; varying vec4 vColor; void main(){ gl_FragColor=vColor; }";
    }

    private static final class Mesh {
        private final FloatBuffer data; private final int count; private static final int STRIDE=10*4;
        Mesh(float[] v){count=v.length/10; data=ByteBuffer.allocateDirect(v.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();data.put(v).position(0);}
        void draw(int aPos,int aNormal,int aColor){
            data.position(0); GLES20.glEnableVertexAttribArray(aPos); GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,STRIDE,data);
            data.position(3); GLES20.glEnableVertexAttribArray(aNormal); GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,STRIDE,data);
            data.position(6); GLES20.glEnableVertexAttribArray(aColor); GLES20.glVertexAttribPointer(aColor,4,GLES20.GL_FLOAT,false,STRIDE,data);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
        }
    }

    private static final class Builder {
        final ArrayList<Float> v=new ArrayList<>();
        void tri(float[] a,float[] b,float[] c,float[] color){float[] n=normal(a,b,c);vertex(a,n,color);vertex(b,n,color);vertex(c,n,color);}
        void quad(float[] a,float[] b,float[] c,float[] d,float[] color){tri(a,b,c,color);tri(a,c,d,color);}
        void vertex(float[] p,float[] n,float[] c){for(float x:p)v.add(x);for(float x:n)v.add(x);for(float x:c)v.add(x);}
        void box(float cx,float cy,float cz,float sx,float sy,float sz,float[] top,float[] side){
            float x=sx/2,y=sy/2,z=sz/2; float[] p000=p(cx-x,cy-y,cz-z),p001=p(cx-x,cy-y,cz+z),p010=p(cx-x,cy+y,cz-z),p011=p(cx-x,cy+y,cz+z),p100=p(cx+x,cy-y,cz-z),p101=p(cx+x,cy-y,cz+z),p110=p(cx+x,cy+y,cz-z),p111=p(cx+x,cy+y,cz+z);
            quad(p010,p110,p111,p011,top); quad(p000,p001,p101,p100,side); quad(p000,p010,p011,p001,side); quad(p100,p101,p111,p110,side); quad(p001,p011,p111,p101,side); quad(p000,p100,p110,p010,side);
        }
        void cylinder(float cx,float cy,float z,float r,float len,int seg,float[] color){
            for(int i=0;i<seg;i++){double a=2*Math.PI*i/seg,b=2*Math.PI*(i+1)/seg;float[] p0=p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z);float[] p1=p(cx+(float)Math.cos(b)*r,cy+(float)Math.sin(b)*r,z);float[] q0=p(p0[0],p0[1],z+len);float[] q1=p(p1[0],p1[1],z+len);quad(p0,p1,q1,q0,color);}
        }
        void cone(float cx,float cy,float z,float r,float len,int seg,float[] color){float[] tip=p(cx,cy,z+len);for(int i=0;i<seg;i++){double a=2*Math.PI*i/seg,b=2*Math.PI*(i+1)/seg;tri(p(cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,z),p(cx+(float)Math.cos(b)*r,cy+(float)Math.sin(b)*r,z),tip,color);}}
        Mesh mesh(){float[] a=new float[v.size()];for(int i=0;i<a.length;i++)a[i]=v.get(i);return new Mesh(a);}
    }

    private static float[] p(float x,float y,float z){return new float[]{x,y,z};}
    private static float[] normal(float[] a,float[] b,float[] c){float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;float l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-5f)l=1f;return new float[]{nx/l,ny/l,nz/l};}
}
