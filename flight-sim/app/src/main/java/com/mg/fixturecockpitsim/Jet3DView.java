package com.mg.fixturecockpitsim;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import com.mg.fixturecockpitsim.visual.ProceduralFighterMesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** v20 transition renderer until the production GLB replaces it. */
public final class Jet3DView extends GLSurfaceView {
    private final JetRenderer renderer;
    public Jet3DView(Context context) {
        super(context); setEGLContextClientVersion(2); renderer=new JetRenderer(); setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY); setPreserveEGLContextOnPause(true);
    }
    public void setTelemetry(float roll,float pitch,float yaw,float throttle,float linkHz,int drops,boolean live){renderer.setTelemetry(roll,pitch,yaw,throttle,live);}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP)renderer.nextCamera(); return true;}

    private static final class JetRenderer implements GLSurfaceView.Renderer {
        private final float[] projection=new float[16],view=new float[16],model=new float[16],vp=new float[16],mvp=new float[16];
        private volatile float targetRoll,targetPitch,targetYaw,throttle=0.62f; private volatile boolean live;
        private float roll,pitch,yaw; private int cameraMode;
        private int program,aPos,aNormal,aPart,uMvp,uModel,uColor,uLightDir,uThrottle;
        private FloatBuffer vertices; private int vertexCount; private long lastNs;
        void setTelemetry(float r,float p,float y,float t,boolean l){targetRoll=r;targetPitch=p;targetYaw=y;throttle=Math.max(0f,Math.min(1f,t));live=l;}
        void nextCamera(){cameraMode=(cameraMode+1)%4;}
        @Override public void onSurfaceCreated(GL10 gl,EGLConfig config){
            GLES20.glClearColor(0.018f,0.045f,0.075f,1f); GLES20.glEnable(GLES20.GL_DEPTH_TEST); GLES20.glEnable(GLES20.GL_CULL_FACE); GLES20.glCullFace(GLES20.GL_BACK);
            program=buildProgram(VS,FS); aPos=GLES20.glGetAttribLocation(program,"aPos"); aNormal=GLES20.glGetAttribLocation(program,"aNormal"); aPart=GLES20.glGetAttribLocation(program,"aPart");
            uMvp=GLES20.glGetUniformLocation(program,"uMvp");uModel=GLES20.glGetUniformLocation(program,"uModel");uColor=GLES20.glGetUniformLocation(program,"uColor");uLightDir=GLES20.glGetUniformLocation(program,"uLightDir");uThrottle=GLES20.glGetUniformLocation(program,"uThrottle");
            ProceduralFighterMesh.Mesh mesh=ProceduralFighterMesh.build(); ByteBuffer bb=ByteBuffer.allocateDirect(mesh.data.length*4).order(ByteOrder.nativeOrder()); vertices=bb.asFloatBuffer(); vertices.put(mesh.data).position(0); vertexCount=mesh.vertexCount(); lastNs=System.nanoTime();
        }
        @Override public void onSurfaceChanged(GL10 gl,int width,int height){GLES20.glViewport(0,0,width,height);float aspect=Math.max(0.1f,(float)width/Math.max(1,height));Matrix.perspectiveM(projection,0,34f,aspect,0.1f,150f);}
        @Override public void onDrawFrame(GL10 gl){
            long now=System.nanoTime(); float dt=Math.min(0.05f,Math.max(0.001f,(now-lastNs)/1_000_000_000f)); lastNs=now; float k=1f-(float)Math.exp(-dt*8f);
            roll+=shortest(targetRoll-roll)*k; pitch+=(targetPitch-pitch)*k; yaw+=shortest(targetYaw-yaw)*k*0.65f;
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT); setCamera(); Matrix.multiplyMM(vp,0,projection,0,view,0); Matrix.setIdentityM(model,0);
            Matrix.rotateM(model,0,-yaw*0.30f,0,1,0);Matrix.rotateM(model,0,pitch,1,0,0);Matrix.rotateM(model,0,-roll,0,0,1);Matrix.multiplyMM(mvp,0,vp,0,model,0);
            GLES20.glUseProgram(program);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,model,0);
            float boost=live?throttle:0f;GLES20.glUniform4f(uColor,0.31f+0.045f*boost,0.34f+0.045f*boost,0.37f+0.05f*boost,1f);GLES20.glUniform3f(uLightDir,-0.35f,0.82f,-0.45f);GLES20.glUniform1f(uThrottle,boost);
            final int stride=28; vertices.position(0);GLES20.glEnableVertexAttribArray(aPos);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,stride,vertices);
            vertices.position(3);GLES20.glEnableVertexAttribArray(aNormal);GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,stride,vertices);
            vertices.position(6);GLES20.glEnableVertexAttribArray(aPart);GLES20.glVertexAttribPointer(aPart,1,GLES20.GL_FLOAT,false,stride,vertices);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertexCount);GLES20.glDisableVertexAttribArray(aPos);GLES20.glDisableVertexAttribArray(aNormal);GLES20.glDisableVertexAttribArray(aPart);
        }
        private void setCamera(){switch(cameraMode){case 1:Matrix.setLookAtM(view,0,0,2.4f,12.8f,0,0,0.5f,0,1,0);break;case 2:Matrix.setLookAtM(view,0,10.5f,7f,13.8f,0,0,0.2f,0,1,0);break;case 3:Matrix.setLookAtM(view,0,-13.5f,4.5f,5.5f,0,0,0.8f,0,1,0);break;default:Matrix.setLookAtM(view,0,0,4.8f,18.2f,0,0,-0.6f,0,1,0);}}
        private static float shortest(float d){while(d>180f)d-=360f;while(d<-180f)d+=360f;return d;}
        private static int buildProgram(String vs,String fs){int v=compile(GLES20.GL_VERTEX_SHADER,vs),f=compile(GLES20.GL_FRAGMENT_SHADER,fs),p=GLES20.glCreateProgram();GLES20.glAttachShader(p,v);GLES20.glAttachShader(p,f);GLES20.glLinkProgram(p);int[]ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new RuntimeException(GLES20.glGetProgramInfoLog(p));return p;}
        private static int compile(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);int[]ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new RuntimeException(GLES20.glGetShaderInfoLog(s));return s;}
        private static final String VS="uniform mat4 uMvp;uniform mat4 uModel;attribute vec3 aPos;attribute vec3 aNormal;attribute float aPart;varying vec3 vN;varying float vPart;void main(){vN=normalize(mat3(uModel)*aNormal);vPart=aPart;gl_Position=uMvp*vec4(aPos,1.0);}";
        private static final String FS="precision mediump float;uniform vec4 uColor;uniform vec3 uLightDir;uniform float uThrottle;varying vec3 vN;varying float vPart;void main(){float nd=max(dot(normalize(vN),normalize(uLightDir)),0.0);float rim=pow(1.0-max(abs(vN.z),0.0),2.0);float light=0.22+0.70*nd+0.10*rim;vec3 c=uColor.rgb;if(vPart>0.5&&vPart<1.5)c=mix(vec3(0.035,0.09,0.12),vec3(0.17,0.30,0.34),0.55+0.35*nd);else if(vPart>1.5&&vPart<2.5)c=mix(vec3(0.10,0.09,0.085),vec3(0.34,0.28,0.20),0.35+0.45*nd)+vec3(0.12,0.035,0.0)*uThrottle;else if(vPart>2.5)c=vec3(0.07,0.085,0.09);gl_FragColor=vec4(c*light,1.0);}";
    }
}
