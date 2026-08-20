package com.mg.fixturecockpitsim;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import java.io.*;
import java.nio.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * V14 renderer for the real F-22 mesh imported at CI time from the open-source
 * FlightGear F-22 model. The geometry is no longer generated from primitive
 * wings / boxes in Android code.
 */
public final class F22InternetModelView extends GLSurfaceView {
    private final R renderer;

    public F22InternetModelView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(false);
        renderer = new R(context.getAssets());
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
    }

    public void setTelemetry(float roll,float pitch,float yaw,float thr,float hz,int drops,boolean live) {
        renderer.set(roll,pitch,yaw,thr,live);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction()==MotionEvent.ACTION_UP) renderer.nextCamera();
        return true;
    }

    private static final class R implements Renderer {
        private final AssetManager assets;
        private final float[] proj=new float[16],view=new float[16],model=new float[16],mv=new float[16],mvp=new float[16];
        private FloatBuffer mesh;
        private int count;
        private int program,aPos,aNormal,aColor,uMvp,uModel,uLight,uEye,uExposure;
        private float targetRoll,targetPitch,targetYaw,thr=.72f,roll,pitch,yaw,vr,vp,vy;
        private boolean live;
        private long last;
        private int camera=0;
        private float cameraBlend=1f;

        R(AssetManager a){ assets=a; }
        void set(float r,float p,float y,float t,boolean l){targetRoll=r;targetPitch=p;targetYaw=y;thr=t;live=l;}
        void nextCamera(){camera=(camera+1)%5;cameraBlend=0f;}

        @Override public void onSurfaceCreated(GL10 gl,EGLConfig cfg){
            GLES20.glClearColor(0,0,0,0);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            // The source aircraft contains legitimate thin / two-sided surfaces.
            // Keep culling disabled so both canted vertical stabilizers remain visible.
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
            program=link(VS,FS);
            aPos=GLES20.glGetAttribLocation(program,"aPos");
            aNormal=GLES20.glGetAttribLocation(program,"aNormal");
            aColor=GLES20.glGetAttribLocation(program,"aColor");
            uMvp=GLES20.glGetUniformLocation(program,"uMvp");
            uModel=GLES20.glGetUniformLocation(program,"uModel");
            uLight=GLES20.glGetUniformLocation(program,"uLight");
            uEye=GLES20.glGetUniformLocation(program,"uEye");
            uExposure=GLES20.glGetUniformLocation(program,"uExposure");
            loadMesh();
            last=System.nanoTime();
        }

        private void loadMesh(){
            try(InputStream in=new BufferedInputStream(assets.open("models/f22_v14.mesh"))){
                byte[] magic=new byte[8]; readFully(in,magic);
                String m=new String(magic,"US-ASCII");
                if(!"F22MSH14".equals(m)) throw new IOException("bad model header "+m);
                byte[] ib=new byte[4]; readFully(in,ib);
                count=ByteBuffer.wrap(ib).order(ByteOrder.LITTLE_ENDIAN).getInt();
                if(count<=0 || count>3_000_000) throw new IOException("bad vertex count "+count);
                byte[] raw=new byte[count*40]; readFully(in,raw);
                mesh=ByteBuffer.allocateDirect(raw.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
                mesh.put(ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer());
                mesh.position(0);
            }catch(Exception e){
                // Never crash the display phone if the downloaded model is damaged.
                count=0; mesh=ByteBuffer.allocateDirect(40).order(ByteOrder.nativeOrder()).asFloatBuffer();
            }
        }
        private static void readFully(InputStream in,byte[] b)throws IOException{
            int p=0,n; while(p<b.length && (n=in.read(b,p,b.length-p))>0)p+=n;
            if(p!=b.length)throw new EOFException();
        }

        @Override public void onSurfaceChanged(GL10 gl,int w,int h){
            GLES20.glViewport(0,0,w,h);
            Matrix.perspectiveM(proj,0,37f,(float)w/Math.max(1,h),.08f,180f);
        }

        @Override public void onDrawFrame(GL10 gl){
            long now=System.nanoTime();
            float dt=Math.min(.034f,Math.max(.001f,(now-last)/1e9f)); last=now;
            // Critically damped flight response: smooth without looking detached from pilot IMU.
            float k=20f,d=2f*(float)Math.sqrt(k);
            vr+=(shortest(targetRoll-roll)*k-vr*d)*dt; roll+=vr*dt;
            vp+=((targetPitch-pitch)*k-vp*d)*dt; pitch+=vp*dt;
            k=11f;d=2f*(float)Math.sqrt(k);vy+=(shortest(targetYaw-yaw)*k-vy*d)*dt;yaw+=vy*dt;
            cameraBlend=Math.min(1f,cameraBlend+dt*1.7f);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            setupCamera();
            Matrix.setIdentityM(model,0);
            Matrix.rotateM(model,0,-yaw*.12f,0,1,0);
            Matrix.rotateM(model,0,pitch,1,0,0);
            Matrix.rotateM(model,0,-roll,0,0,1);
            Matrix.multiplyMM(mv,0,view,0,model,0);
            Matrix.multiplyMM(mvp,0,proj,0,mv,0);

            GLES20.glUseProgram(program);
            GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);
            GLES20.glUniformMatrix4fv(uModel,1,false,model,0);
            GLES20.glUniform3f(uLight,-.46f,.82f,.36f);
            GLES20.glUniform3f(uEye,0,2.5f,9.4f);
            GLES20.glUniform1f(uExposure,1.02f+.10f*thr);
            if(count>0){
                mesh.position(0);GLES20.glEnableVertexAttribArray(aPos);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,40,mesh);
                mesh.position(3);GLES20.glEnableVertexAttribArray(aNormal);GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,40,mesh);
                mesh.position(6);GLES20.glEnableVertexAttribArray(aColor);GLES20.glVertexAttribPointer(aColor,4,GLES20.GL_FLOAT,false,40,mesh);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
            }
        }

        private void setupCamera(){
            float[][] cams={{0,2.55f,9.7f},{0,1.55f,7.0f},{4.8f,3.15f,6.5f},{-5.4f,2.4f,5.8f},{0,5.7f,5.8f}};
            float[] q=cams[camera];float s=cameraBlend*cameraBlend*(3-2*cameraBlend);
            float ex=q[0]*s,ey=2.55f+(q[1]-2.55f)*s,ez=9.7f+(q[2]-9.7f)*s;
            Matrix.setLookAtM(view,0,ex,ey,ez,0,.05f,-.15f,0,1,0);
        }
        static float shortest(float d){while(d>180)d-=360;while(d<-180)d+=360;return d;}

        private static int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s;}
        private static int link(String v,String f){int p=GLES20.glCreateProgram();GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,v));GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,f));GLES20.glLinkProgram(p);return p;}

        private static final String VS=
                "uniform mat4 uMvp;uniform mat4 uModel;attribute vec3 aPos;attribute vec3 aNormal;attribute vec4 aColor;"+
                "varying vec3 n;varying vec3 wp;varying vec4 c;void main(){vec4 w=uModel*vec4(aPos,1.0);wp=w.xyz;n=normalize(mat3(uModel)*aNormal);c=aColor;gl_Position=uMvp*vec4(aPos,1.0);}";
        private static final String FS=
                "precision highp float;uniform vec3 uLight;uniform vec3 uEye;uniform float uExposure;varying vec3 n;varying vec3 wp;varying vec4 c;"+
                "void main(){vec3 N=normalize(n);vec3 L=normalize(uLight);vec3 V=normalize(uEye-wp);vec3 H=normalize(L+V);"+
                "float ndl=max(dot(N,L),0.0);float spec=pow(max(dot(N,H),0.0),48.0);float fres=pow(1.0-max(dot(N,V),0.0),3.0);"+
                "vec3 sky=vec3(.18,.29,.40);vec3 base=mix(c.rgb,c.rgb*sky*1.45,.18);vec3 col=base*(.36+.72*ndl)+vec3(.62,.68,.72)*spec*.34+sky*fres*.13;"+
                "col=vec3(1.0)-exp(-col*uExposure);gl_FragColor=vec4(col,c.a);}";
    }
}
