package com.mg.tennistv;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import java.nio.*;
import java.util.Random;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Tennis3DView extends GLSurfaceView {
    public static final int MODE_AI=0, MODE_HOST=1, MODE_CONTROLLER=2;
    private final TennisRenderer renderer;

    public Tennis3DView(Context c) {
        super(c);
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        renderer = new TennisRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public void setPlayerTilt(float t){ renderer.playerTilt=t; }
    public void localSwing(float p,float d){ renderer.localSwing(p,d); }
    public void remoteSwing(float p,float d){ renderer.remoteSwing(p,d); }
    public void flashSwing(float p){ renderer.swingFlash=System.currentTimeMillis(); renderer.pendingPower=p; }
    public void setMode(int m){ renderer.mode=m; renderer.resetBall(true); }

    @Override public boolean onTouchEvent(MotionEvent e){ return true; }

    static class TennisRenderer implements GLSurfaceView.Renderer {
        final Random rnd = new Random();
        int program, aPos, aNormal, uMvp, uModel, uColor, uLight;
        final float[] proj=new float[16], view=new float[16], vp=new float[16], model=new float[16], mvp=new float[16];
        FloatBuffer cube, sphere;
        ShortBuffer sphereIdx;
        int sphereIndexCount;

        volatile int mode=MODE_AI;
        volatile float playerTilt=0, pendingPower=0, pendingDir=0, remotePower=0, remoteDir=0;
        volatile long swingFlash=0;
        float playerX=0, oppX=0;
        float ballX=0, ballY=1.45f, ballZ=4, vx=.55f, vy=3.0f, vz=-8.0f;
        boolean bounced=false;
        int myScore=0, oppScore=0;
        long lastNs=0;
        float time=0;

        static final float COURT_W=8.23f, COURT_L=23.77f, HALF_L=11.885f;

        @Override public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
            GLES20.glClearColor(0.045f,0.085f,0.12f,1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);
            program = link(VS,FS);
            aPos=GLES20.glGetAttribLocation(program,"aPos");
            aNormal=GLES20.glGetAttribLocation(program,"aNormal");
            uMvp=GLES20.glGetUniformLocation(program,"uMVP");
            uModel=GLES20.glGetUniformLocation(program,"uModel");
            uColor=GLES20.glGetUniformLocation(program,"uColor");
            uLight=GLES20.glGetUniformLocation(program,"uLight");
            cube=makeCube();
            makeSphere(18,14);
            lastNs=System.nanoTime();
        }

        @Override public void onSurfaceChanged(GL10 gl,int w,int h) {
            GLES20.glViewport(0,0,w,h);
            Matrix.perspectiveM(proj,0,43f,(float)w/Math.max(1,h),0.1f,85f);
        }

        @Override public void onDrawFrame(GL10 gl) {
            long now=System.nanoTime();
            float dt=Math.min(.033f,(now-lastNs)/1_000_000_000f); lastNs=now; time+=dt;
            update(dt);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glUseProgram(program);
            GLES20.glUniform3f(uLight,-.35f,.85f,.42f);

            float camX=playerX*.20f;
            Matrix.setLookAtM(view,0,camX,7.0f,18.5f, 0,1.2f,-1.8f, 0,1,0);
            Matrix.multiplyMM(vp,0,proj,0,view,0);

            drawArena();
            drawCourt();
            drawNet();
            drawHuman(playerX,9.55f,0f,0.10f,0.34f,0.88f,true);
            drawHuman(oppX,-9.55f,180f,0.92f,0.18f,0.12f,false);
            drawBall();
        }

        void update(float dt){
            if(mode==MODE_CONTROLLER) return;
            playerX=clamp(playerTilt*.62f,-3.55f,3.55f);
            if(mode==MODE_AI){
                float target=clamp(ballX,-3.35f,3.35f);
                oppX += (target-oppX)*Math.min(1f,dt*3.25f);
            }
            ballX += vx*dt; ballY += vy*dt; ballZ += vz*dt;
            vy -= 9.81f*dt;

            if(ballY<=.18f){
                ballY=.18f;
                if(!bounced){
                    vy=Math.abs(vy)*.72f;
                    vx*=.93f; vz*=.96f;
                    bounced=true;
                } else if(Math.abs(vy)<1.1f){ vy=1.1f; }
            }

            if(ballZ>7.6f && vz>0){
                if(pendingPower>0 && Math.abs(ballX-playerX)<2.05f && ballY<3.4f){
                    float p=clamp(pendingPower,.75f,2.35f);
                    vx=clamp(pendingDir*4.3f + (ballX-playerX)*-.45f,-6.2f,6.2f);
                    vz=-clamp(8.5f+3.0f*p,9f,15.2f);
                    vy=3.3f+2.4f*p;
                    pendingPower=0; bounced=false; swingFlash=System.currentTimeMillis();
                }
            }

            if(ballZ<-7.6f && vz<0){
                if(mode==MODE_AI && Math.abs(ballX-oppX)<2.15f && ballY<3.7f){
                    vz=9.4f+rnd.nextFloat()*3.5f;
                    vx=clamp((rnd.nextFloat()-.5f)*6.3f + (oppX-ballX)*.25f,-5.8f,5.8f);
                    vy=4.0f+rnd.nextFloat()*2.5f;
                    bounced=false;
                } else if(mode==MODE_HOST && remotePower>0 && Math.abs(ballX-oppX)<2.3f){
                    float p=clamp(remotePower,.75f,2.35f);
                    vz=clamp(8.5f+3.0f*p,9f,15.2f);
                    vx=clamp(remoteDir*4.3f,-6.2f,6.2f);
                    vy=3.3f+2.4f*p;
                    remotePower=0; bounced=false;
                }
            }

            if(ballZ>14.3f){ oppScore++; resetBall(false); }
            else if(ballZ<-14.3f){ myScore++; resetBall(true); }
            else if(Math.abs(ballX)>6.3f && ballY<.6f){
                if(ballZ>0) oppScore++; else myScore++;
                resetBall(ballZ<0);
            }
        }

        void localSwing(float p,float d){ pendingPower=p; pendingDir=d; swingFlash=System.currentTimeMillis(); }
        void remoteSwing(float p,float d){ remotePower=p; remoteDir=d; }
        void resetBall(boolean towardOpponent){
            ballX=0; ballY=1.65f; ballZ=towardOpponent?7.8f:-7.8f;
            vx=(rnd.nextFloat()-.5f)*1.2f;
            vz=towardOpponent?-8.6f:8.6f;
            vy=4.4f; bounced=false; pendingPower=remotePower=0;
        }

        void drawArena(){
            drawBox(0,-.32f,0, 22f,.45f,34f, 0.08f,.18f,.15f);
            drawBox(0,.20f,-17.2f, 22f,1.2f,.55f, .08f,.11f,.14f);
            for(int r=0;r<4;r++){
                float y=.55f+r*.52f;
                float z=-18.0f-r*.70f;
                drawBox(0,y,z, 20f,.34f,.55f, .18f+.025f*r,.21f+.025f*r,.24f+.025f*r);
            }
            for(int side=-1;side<=1;side+=2){
                float x=side*8.2f;
                for(int r=0;r<4;r++) drawBox(x+side*r*.45f,.45f+r*.45f,0, .55f,.32f,27f, .15f,.18f,.20f);
            }
            // stadium light masts
            for(int side=-1;side<=1;side+=2){
                drawBox(side*8.0f,4.1f,-7.8f,.16f,8.0f,.16f,.20f,.22f,.24f);
                drawBox(side*8.0f,4.1f,7.8f,.16f,8.0f,.16f,.20f,.22f,.24f);
                drawBox(side*8.0f,8.05f,-7.8f,1.5f,.16f,.25f,.80f,.83f,.76f);
                drawBox(side*8.0f,8.05f,7.8f,1.5f,.16f,.25f,.80f,.83f,.76f);
            }
        }

        void drawCourt(){
            drawBox(0,.01f,0, COURT_W+.35f,.08f,COURT_L+.35f, .055f,.38f,.34f);
            // doubles alley surrounding color
            drawBox(0,-.035f,0, 10.97f,.04f,23.77f, .08f,.31f,.29f);
            drawBox(0,.005f,0, COURT_W,.055f,COURT_L, .07f,.43f,.38f);
            float white=.94f;
            lineX(-COURT_W/2,0,COURT_L,.055f,white);
            lineX(COURT_W/2,0,COURT_L,.055f,white);
            lineZ(0,-HALF_L,COURT_W,.055f,white);
            lineZ(0,HALF_L,COURT_W,.055f,white);
            lineZ(0,-6.40f,COURT_W,.055f,white);
            lineZ(0,6.40f,COURT_W,.055f,white);
            lineX(0,0,12.80f,.045f,white);
            // subtle service zone tint
            drawBox(-2.05f,.025f,-3.2f,4.0f,.02f,6.25f,.075f,.47f,.41f);
            drawBox(2.05f,.025f,-3.2f,4.0f,.02f,6.25f,.075f,.47f,.41f);
            drawBox(-2.05f,.025f,3.2f,4.0f,.02f,6.25f,.075f,.47f,.41f);
            drawBox(2.05f,.025f,3.2f,4.0f,.02f,6.25f,.075f,.47f,.41f);
            // redraw service lines over tint
            lineZ(0,-6.40f,COURT_W,.055f,white); lineZ(0,6.40f,COURT_W,.055f,white); lineX(0,0,12.80f,.045f,white);
        }

        void lineX(float x,float z,float len,float t,float c){ drawBox(x,.075f,z,t,.025f,len,c,c,c); }
        void lineZ(float x,float z,float len,float t,float c){ drawBox(x,.075f,z,len,.025f,t,c,c,c); }

        void drawNet(){
            drawBox(-5.0f,.72f,0,.10f,1.44f,.10f,.14f,.14f,.15f);
            drawBox(5.0f,.72f,0,.10f,1.44f,.10f,.14f,.14f,.15f);
            drawBox(0,1.02f,0,10.0f,.07f,.07f,.92f,.92f,.90f);
            // net mesh strips
            for(int i=-10;i<=10;i++) drawBox(i*.47f,.55f,0,.018f,1.0f,.025f,.16f,.17f,.18f);
            for(int j=0;j<5;j++) drawBox(0,.16f+j*.20f,0,9.5f,.018f,.025f,.16f,.17f,.18f);
        }

        void drawHuman(float x,float z,float yaw,float cr,float cg,float cb,boolean near){
            float move=(float)Math.sin(time*5.0f+(near?0:1.5f))*.09f;
            float swing=(System.currentTimeMillis()-swingFlash<330 && near)?1f:0f;
            float skinR=.72f, skinG=.49f, skinB=.34f;
            // legs
            drawPart(x-.18f,.64f,z+move,.21f,1.05f,.28f, yaw, .11f,.12f,.14f);
            drawPart(x+.18f,.64f,z-move,.21f,1.05f,.28f, yaw, .11f,.12f,.14f);
            // shoes
            drawPart(x-.18f,.12f,z-.10f,.28f,.16f,.55f,yaw,.86f,.87f,.89f);
            drawPart(x+.18f,.12f,z-.10f,.28f,.16f,.55f,yaw,.86f,.87f,.89f);
            // torso and shorts
            drawPart(x,1.65f,z,.92f,1.05f,.48f,yaw,cr,cg,cb);
            drawPart(x,1.08f,z,.82f,.34f,.45f,yaw,.07f,.08f,.10f);
            // neck/head
            drawPart(x,2.28f,z,.22f,.28f,.22f,yaw,skinR,skinG,skinB);
            drawSphere(x,2.62f,z,.32f,skinR,skinG,skinB);
            // hair cap
            drawPart(x,2.87f,z-.02f,.55f,.13f,.48f,yaw,.055f,.045f,.035f);
            // arms, racket arm exaggerates during swing
            float armZ = swing>.5f ? -.42f : .03f;
            drawPart(x-.58f,1.72f,z,.20f,.88f,.20f,yaw+8f,skinR,skinG,skinB);
            drawPart(x+.58f,1.72f,z+armZ,.20f,.92f,.20f,yaw-(swing*.55f*90f),skinR,skinG,skinB);
            drawRacket(x+.84f,1.47f,z+(swing>.5f?-1.0f:.18f),yaw-(swing*65f));
        }

        void drawRacket(float x,float y,float z,float yaw){
            drawPart(x,y,z,.06f,.78f,.06f,yaw,.15f,.10f,.06f);
            drawPart(x,y+.58f,z,.58f,.06f,.06f,yaw,.12f,.12f,.13f);
            drawPart(x-.28f,y+.84f,z,.06f,.55f,.06f,yaw,.12f,.12f,.13f);
            drawPart(x+.28f,y+.84f,z,.06f,.55f,.06f,yaw,.12f,.12f,.13f);
            drawPart(x,y+1.10f,z,.58f,.06f,.06f,yaw,.12f,.12f,.13f);
            for(int i=-2;i<=2;i++) drawPart(x+i*.10f,y+.84f,z,.015f,.48f,.02f,yaw,.62f,.63f,.64f);
        }

        void drawBall(){
            drawSphere(ballX,ballY,ballZ,.18f,.82f,.95f,.08f);
            // soft fake shadow
            float s=.30f+ballY*.055f;
            drawBox(ballX,.055f,ballZ,s,.012f,s,.07f,.10f,.09f);
        }

        void drawPart(float x,float y,float z,float sx,float sy,float sz,float yaw,float r,float g,float b){
            Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z); Matrix.rotateM(model,0,yaw,0,1,0); Matrix.scaleM(model,0,sx,sy,sz); drawMesh(cube,36,r,g,b,false);
        }
        void drawBox(float x,float y,float z,float sx,float sy,float sz,float r,float g,float b){ drawPart(x,y,z,sx,sy,sz,0,r,g,b); }
        void drawSphere(float x,float y,float z,float s,float r,float g,float b){
            Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z); Matrix.scaleM(model,0,s,s,s); drawMesh(sphere,sphereIndexCount,r,g,b,true);
        }

        void drawMesh(FloatBuffer buf,int count,float r,float g,float b,boolean indexed){
            Matrix.multiplyMM(mvp,0,vp,0,model,0);
            GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0); GLES20.glUniformMatrix4fv(uModel,1,false,model,0); GLES20.glUniform4f(uColor,r,g,b,1f);
            buf.position(0); GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,24,buf); GLES20.glEnableVertexAttribArray(aPos);
            buf.position(3); GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,24,buf); GLES20.glEnableVertexAttribArray(aNormal);
            if(indexed){ sphereIdx.position(0); GLES20.glDrawElements(GLES20.GL_TRIANGLES,count,GLES20.GL_UNSIGNED_SHORT,sphereIdx); }
            else GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
        }

        FloatBuffer makeCube(){
            float[] v={
                -0.5f,-0.5f,0.5f, 0,0,1,  0.5f,-0.5f,0.5f,0,0,1, 0.5f,0.5f,0.5f,0,0,1,
                -0.5f,-0.5f,0.5f,0,0,1, 0.5f,0.5f,0.5f,0,0,1, -0.5f,0.5f,0.5f,0,0,1,
                0.5f,-0.5f,-0.5f,0,0,-1, -0.5f,-0.5f,-0.5f,0,0,-1, -0.5f,0.5f,-0.5f,0,0,-1,
                0.5f,-0.5f,-0.5f,0,0,-1, -0.5f,0.5f,-0.5f,0,0,-1, 0.5f,0.5f,-0.5f,0,0,-1,
                -0.5f,-0.5f,-0.5f,-1,0,0, -0.5f,-0.5f,0.5f,-1,0,0, -0.5f,0.5f,0.5f,-1,0,0,
                -0.5f,-0.5f,-0.5f,-1,0,0, -0.5f,0.5f,0.5f,-1,0,0, -0.5f,0.5f,-0.5f,-1,0,0,
                0.5f,-0.5f,0.5f,1,0,0, 0.5f,-0.5f,-0.5f,1,0,0, 0.5f,0.5f,-0.5f,1,0,0,
                0.5f,-0.5f,0.5f,1,0,0, 0.5f,0.5f,-0.5f,1,0,0, 0.5f,0.5f,0.5f,1,0,0,
                -0.5f,0.5f,0.5f,0,1,0, 0.5f,0.5f,0.5f,0,1,0, 0.5f,0.5f,-0.5f,0,1,0,
                -0.5f,0.5f,0.5f,0,1,0, 0.5f,0.5f,-0.5f,0,1,0, -0.5f,0.5f,-0.5f,0,1,0,
                -0.5f,-0.5f,-0.5f,0,-1,0, 0.5f,-0.5f,-0.5f,0,-1,0, 0.5f,-0.5f,0.5f,0,-1,0,
                -0.5f,-0.5f,-0.5f,0,-1,0, 0.5f,-0.5f,0.5f,0,-1,0, -0.5f,-0.5f,0.5f,0,-1,0};
            return fb(v);
        }

        void makeSphere(int lon,int lat){
            float[] v=new float[(lon+1)*(lat+1)*6]; int k=0;
            for(int j=0;j<=lat;j++){
                float p=(float)Math.PI*j/lat;
                for(int i=0;i<=lon;i++){
                    float t=(float)(2*Math.PI*i/lon);
                    float x=(float)(Math.sin(p)*Math.cos(t)), y=(float)Math.cos(p), z=(float)(Math.sin(p)*Math.sin(t));
                    v[k++]=x;v[k++]=y;v[k++]=z;v[k++]=x;v[k++]=y;v[k++]=z;
                }
            }
            short[] idx=new short[lon*lat*6]; k=0;
            for(int j=0;j<lat;j++) for(int i=0;i<lon;i++){
                short a=(short)(j*(lon+1)+i), b=(short)(a+lon+1), c=(short)(a+1), d=(short)(b+1);
                idx[k++]=a;idx[k++]=b;idx[k++]=c; idx[k++]=c;idx[k++]=b;idx[k++]=d;
            }
            sphere=fb(v); sphereIdx=ByteBuffer.allocateDirect(idx.length*2).order(ByteOrder.nativeOrder()).asShortBuffer(); sphereIdx.put(idx).position(0); sphereIndexCount=idx.length;
        }

        FloatBuffer fb(float[] a){ FloatBuffer f=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer(); f.put(a).position(0); return f; }
        int shader(int type,String s){ int x=GLES20.glCreateShader(type); GLES20.glShaderSource(x,s); GLES20.glCompileShader(x); return x; }
        int link(String v,String f){ int p=GLES20.glCreateProgram(); GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,v)); GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,f)); GLES20.glLinkProgram(p); return p; }
        static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}

        static final String VS=
                "uniform mat4 uMVP; uniform mat4 uModel; attribute vec3 aPos; attribute vec3 aNormal; varying vec3 vN; varying vec3 vP;"+
                "void main(){ vec4 wp=uModel*vec4(aPos,1.0); vP=wp.xyz; vN=normalize(mat3(uModel)*aNormal); gl_Position=uMVP*vec4(aPos,1.0); }";
        static final String FS=
                "precision mediump float; uniform vec4 uColor; uniform vec3 uLight; varying vec3 vN; varying vec3 vP;"+
                "void main(){ float d=max(dot(normalize(vN),normalize(uLight)),0.0); float amb=.34; float rim=pow(1.0-max(abs(vN.z),0.0),2.0)*.10; vec3 c=uColor.rgb*(amb+d*.66)+rim; gl_FragColor=vec4(c,uColor.a); }";
    }
}
