package com.mg.fixturecockpitsim;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Temporary v19 fallback renderer. The production aircraft will be loaded from
 * assets/aircraft/fighter_v19.glb through the Filament visual pipeline.
 */
public final class Jet3DView extends GLSurfaceView {
    private final JetRenderer renderer;

    public Jet3DView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new JetRenderer();
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
    }

    public void setTelemetry(float roll, float pitch, float yaw, float throttle,
                             float linkHz, int drops, boolean live) {
        renderer.setTelemetry(roll, pitch, yaw, throttle, live);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) renderer.nextCamera();
        return true;
    }

    private static final class JetRenderer implements GLSurfaceView.Renderer {
        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] vp = new float[16];
        private final float[] mvp = new float[16];

        private volatile float targetRoll, targetPitch, targetYaw, throttle = 0.62f;
        private volatile boolean live;
        private float roll, pitch, yaw;
        private int cameraMode;
        private int program, aPos, uMvp, uColor;
        private FloatBuffer vertices;
        private int vertexCount;
        private long lastNs;

        void setTelemetry(float r, float p, float y, float t, boolean l) {
            targetRoll = r; targetPitch = p; targetYaw = y;
            throttle = Math.max(0f, Math.min(1f, t)); live = l;
        }

        void nextCamera() { cameraMode = (cameraMode + 1) % 4; }

        @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0.025f, 0.065f, 0.11f, 1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            program = buildProgram(VS, FS);
            aPos = GLES20.glGetAttribLocation(program, "aPos");
            uMvp = GLES20.glGetUniformLocation(program, "uMvp");
            uColor = GLES20.glGetUniformLocation(program, "uColor");
            buildFallbackAircraft();
            lastNs = System.nanoTime();
        }

        @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float aspect = Math.max(0.1f, (float) width / Math.max(1, height));
            Matrix.perspectiveM(projection, 0, 36f, aspect, 0.1f, 120f);
        }

        @Override public void onDrawFrame(GL10 gl) {
            long now = System.nanoTime();
            float dt = Math.min(0.05f, Math.max(0.001f, (now-lastNs)/1_000_000_000f));
            lastNs = now;
            float k = 1f - (float)Math.exp(-dt * 8f);
            roll += shortest(targetRoll-roll)*k;
            pitch += (targetPitch-pitch)*k;
            yaw += shortest(targetYaw-yaw)*k*0.65f;

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            setCamera();
            Matrix.multiplyMM(vp,0,projection,0,view,0);
            Matrix.setIdentityM(model,0);
            Matrix.rotateM(model,0,-yaw*0.30f,0,1,0);
            Matrix.rotateM(model,0,pitch,1,0,0);
            Matrix.rotateM(model,0,-roll,0,0,1);
            Matrix.multiplyMM(mvp,0,vp,0,model,0);

            GLES20.glUseProgram(program);
            GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);
            float boost = live ? throttle : 0f;
            GLES20.glUniform4f(uColor,0.28f+0.08f*boost,0.31f+0.08f*boost,0.34f+0.08f*boost,1f);
            GLES20.glEnableVertexAttribArray(aPos);
            GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,vertices);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertexCount);
            GLES20.glDisableVertexAttribArray(aPos);
        }

        private void setCamera() {
            switch (cameraMode) {
                case 1: Matrix.setLookAtM(view,0,0,2.2f,11.5f,0,0,0,0,1,0); break;
                case 2: Matrix.setLookAtM(view,0,9,6.5f,12,0,0,0,0,1,0); break;
                case 3: Matrix.setLookAtM(view,0,-13,3.5f,4,0,0,0,0,1,0); break;
                default: Matrix.setLookAtM(view,0,0,4.2f,16.5f,0,0,-1.5f,0,1,0); break;
            }
        }

        private void buildFallbackAircraft() {
            float[] v = {
                0,0.25f,-4.8f,  -1.05f,0,1.8f,  1.05f,0,1.8f,
                0,0.25f,-4.8f,  -3.7f,0,0.25f, -1.05f,0,1.8f,
                0,0.25f,-4.8f,   1.05f,0,1.8f,  3.7f,0,0.25f,
                -1.05f,0,1.8f,  -3.7f,0,0.25f, -1.5f,0,2.9f,
                 1.05f,0,1.8f,   1.5f,0,2.9f,   3.7f,0,0.25f,
                -0.85f,0,1.6f,  -0.65f,1.55f,2.25f, -0.25f,0,2.85f,
                 0.85f,0,1.6f,   0.25f,0,2.85f,  0.65f,1.55f,2.25f,
                -1.05f,0,1.8f,   1.05f,0,1.8f,  0, -0.45f,-2.8f,
                -1.05f,0,1.8f,   0,-0.45f,-2.8f, -3.7f,0,0.25f,
                 1.05f,0,1.8f,   3.7f,0,0.25f,  0,-0.45f,-2.8f
            };
            ByteBuffer bb = ByteBuffer.allocateDirect(v.length*4).order(ByteOrder.nativeOrder());
            vertices = bb.asFloatBuffer(); vertices.put(v).position(0);
            vertexCount = v.length/3;
        }

        private static float shortest(float d) {
            while (d>180f) d-=360f; while (d<-180f) d+=360f; return d;
        }

        private static int buildProgram(String vs, String fs) {
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

        private static final String VS =
                "uniform mat4 uMvp; attribute vec3 aPos; void main(){ gl_Position=uMvp*vec4(aPos,1.0); }";
        private static final String FS =
                "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }";
    }
}
