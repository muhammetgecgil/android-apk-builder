package com.mg.fixturecockpitsim;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import java.io.*;
import java.nio.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * V18 aircraft renderer.
 * The geometry is the full open-source F-22 airframe, converted with crease-aware smooth
 * normals. Rendering uses a compact physically-inspired GGX/Fresnel pipeline instead of the
 * old flat triangle lighting. It keeps hard stealth edges while making the nose, canopy,
 * inlet shoulders and fuselage read as continuous aircraft surfaces.
 */
public final class F22InternetModelView extends GLSurfaceView {
    private final R renderer;
    public F22InternetModelView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(false);
        renderer=new R(context.getAssets());
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
    }
    public void setTelemetry(float roll,float pitch,float yaw,float thr,float hz,int drops,boolean live){renderer.set(roll,pitch,yaw,thr,live);}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP)renderer.nextCamera();return true;}

    private static final class R implements Renderer {
        private final AssetManager assets;
        private final float[] proj=new float[16],view=new float[16],model=new float[16],mv=new float[16],mvp=new float[16];
        private FloatBuffer mesh; private int count,texId;
        private int program,aPos,aNormal,aColor,aUv,uMvp,uModel,uLight,uFill,uEye,uExposure,uAtlas;
        private float targetRoll,targetPitch,targetYaw,thr=.72f,roll,pitch,yaw,vr,vp,vy;
        private float eyeX,eyeY,eyeZ;
        private long last; private int camera=0; private float cameraBlend=1f;
        R(AssetManager a){assets=a;}
        void set(float r,float p,float y,float t,boolean l){targetRoll=r;targetPitch=p;targetYaw=y;thr=t;}
        void nextCamera(){camera=(camera+1)%5;cameraBlend=0f;}

        @Override public void onSurfaceCreated(GL10 gl,EGLConfig cfg){
            GLES20.glClearColor(0,0,0,0);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            // Some source parts contain intentionally mirrored winding. Two-sided render keeps
            // both canted tails and thin control surfaces visible from every camera angle.
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
            program=link(VS,FS);
            aPos=GLES20.glGetAttribLocation(program,"aPos");
            aNormal=GLES20.glGetAttribLocation(program,"aNormal");
            aColor=GLES20.glGetAttribLocation(program,"aColor");
            aUv=GLES20.glGetAttribLocation(program,"aUv");
            uMvp=GLES20.glGetUniformLocation(program,"uMvp");
            uModel=GLES20.glGetUniformLocation(program,"uModel");
            uLight=GLES20.glGetUniformLocation(program,"uLight");
            uFill=GLES20.glGetUniformLocation(program,"uFill");
            uEye=GLES20.glGetUniformLocation(program,"uEye");
            uExposure=GLES20.glGetUniformLocation(program,"uExposure");
            uAtlas=GLES20.glGetUniformLocation(program,"uAtlas");
            loadMesh(); loadTexture(); last=System.nanoTime();
        }

        private void loadTexture(){
            try(InputStream in=assets.open("models/f22_atlas_v18.png")){
                Bitmap b=BitmapFactory.decodeStream(in);
                int[] ids=new int[1]; GLES20.glGenTextures(1,ids,0); texId=ids[0];
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texId);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR_MIPMAP_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,b,0);
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D); b.recycle();
            }catch(Exception e){texId=0;}
        }

        private void loadMesh(){
            try(InputStream in=new BufferedInputStream(assets.open("models/f22_v18.mesh"))){
                byte[] magic=new byte[8];readFully(in,magic);
                String m=new String(magic,"US-ASCII");
                if(!"F22MSH18".equals(m))throw new IOException("bad model header "+m);
                byte[] ib=new byte[4];readFully(in,ib);
                count=ByteBuffer.wrap(ib).order(ByteOrder.LITTLE_ENDIAN).getInt();
                if(count<=0||count>3_000_000)throw new IOException("bad vertex count "+count);
                byte[] raw=new byte[count*48];readFully(in,raw);
                mesh=ByteBuffer.allocateDirect(raw.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
                mesh.put(ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer());mesh.position(0);
            }catch(Exception e){
                count=0;mesh=ByteBuffer.allocateDirect(48).order(ByteOrder.nativeOrder()).asFloatBuffer();
            }
        }
        private static void readFully(InputStream in,byte[] b)throws IOException{int p=0,n;while(p<b.length&&(n=in.read(b,p,b.length-p))>0)p+=n;if(p!=b.length)throw new EOFException();}

        @Override public void onSurfaceChanged(GL10 gl,int w,int h){
            GLES20.glViewport(0,0,w,h);
            Matrix.perspectiveM(proj,0,34f,(float)w/Math.max(1,h),.08f,180f);
        }

        @Override public void onDrawFrame(GL10 gl){
            long now=System.nanoTime();float dt=Math.min(.034f,Math.max(.001f,(now-last)/1e9f));last=now;
            float k=20f,d=2f*(float)Math.sqrt(k);
            vr+=(shortest(targetRoll-roll)*k-vr*d)*dt;roll+=vr*dt;
            vp+=((targetPitch-pitch)*k-vp*d)*dt;pitch+=vp*dt;
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
            // broad daylight key + cool sky fill, chosen to reveal F-22 chine/wing facets
            // without making each source triangle visible.
            GLES20.glUniform3f(uLight,-.42f,.84f,.34f);
            GLES20.glUniform3f(uFill,.38f,.48f,-.79f);
            GLES20.glUniform3f(uEye,eyeX,eyeY,eyeZ);
            GLES20.glUniform1f(uExposure,1.16f+.08f*thr);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texId);
            GLES20.glUniform1i(uAtlas,0);

            if(count>0){
                mesh.position(0);GLES20.glEnableVertexAttribArray(aPos);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,48,mesh);
                mesh.position(3);GLES20.glEnableVertexAttribArray(aNormal);GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,48,mesh);
                mesh.position(6);GLES20.glEnableVertexAttribArray(aColor);GLES20.glVertexAttribPointer(aColor,4,GLES20.GL_FLOAT,false,48,mesh);
                mesh.position(10);GLES20.glEnableVertexAttribArray(aUv);GLES20.glVertexAttribPointer(aUv,2,GLES20.GL_FLOAT,false,48,mesh);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
            }
        }

        private void setupCamera(){
            float[][] cams={{0,2.22f,10.0f},{0,1.16f,7.55f},{5.05f,2.75f,6.65f},{-5.35f,2.28f,5.95f},{0,5.35f,6.25f}};
            float[] q=cams[camera];float s=cameraBlend*cameraBlend*(3-2*cameraBlend);
            eyeX=q[0]*s;eyeY=2.22f+(q[1]-2.22f)*s;eyeZ=10.0f+(q[2]-10.0f)*s;
            Matrix.setLookAtM(view,0,eyeX,eyeY,eyeZ,0,.02f,-.12f,0,1,0);
        }
        static float shortest(float d){while(d>180)d-=360;while(d<-180)d+=360;return d;}
        private static int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s;}
        private static int link(String v,String f){int p=GLES20.glCreateProgram();GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,v));GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,f));GLES20.glLinkProgram(p);return p;}

        private static final String VS=
                "uniform mat4 uMvp;uniform mat4 uModel;attribute vec3 aPos;attribute vec3 aNormal;attribute vec4 aColor;attribute vec2 aUv;"+
                "varying vec3 n;varying vec3 wp;varying vec4 c;varying vec2 uv;"+
                "void main(){vec4 w=uModel*vec4(aPos,1.0);wp=w.xyz;n=normalize(mat3(uModel)*aNormal);c=aColor;uv=aUv;gl_Position=uMvp*vec4(aPos,1.0);}";

        // Mobile microfacet BRDF. Material classification comes from the source AC3D material
        // tint: blue-black pieces become canopy clearcoat, dark neutral parts become metallic
        // nozzle/intake material, and the rest gets the rough RAM-coated airframe response.
        private static final String FS=
                "precision highp float;uniform vec3 uLight;uniform vec3 uFill;uniform vec3 uEye;uniform float uExposure;uniform sampler2D uAtlas;"+
                "varying vec3 n;varying vec3 wp;varying vec4 c;varying vec2 uv;"+
                "const float PI=3.14159265;"+
                "float sat(float x){return clamp(x,0.0,1.0);}"+
                "float Dggx(float NoH,float a){float a2=a*a;float d=NoH*NoH*(a2-1.0)+1.0;return a2/max(PI*d*d,0.0001);}"+
                "float G1(float NoV,float k){return NoV/(NoV*(1.0-k)+k);}"+
                "vec3 Fsch(vec3 f0,float VoH){return f0+(1.0-f0)*pow(1.0-VoH,5.0);}"+
                "vec3 env(vec3 r){float t=sat(r.y*.5+.5);vec3 ground=vec3(.08,.09,.095);vec3 sky=vec3(.20,.35,.52);vec3 e=mix(ground,sky,t);e+=vec3(1.0,.78,.48)*pow(sat(dot(r,normalize(vec3(-.45,.82,.34)))),96.0)*1.5;return e;}"+
                "void main(){"+
                "vec3 N=normalize(n);vec3 V=normalize(uEye-wp);vec3 L=normalize(uLight);vec3 H=normalize(L+V);"+
                "float NoL=sat(dot(N,L)),NoV=max(sat(dot(N,V)),.001),NoH=sat(dot(N,H)),VoH=sat(dot(V,H));"+
                "vec3 tex=texture2D(uAtlas,uv).rgb;vec3 base=mix(c.rgb,tex,c.a);"+
                "float ml=dot(c.rgb,vec3(.299,.587,.114));float blue=c.b-max(c.r,c.g);"+
                "float glass=sat((blue-.015)*9.0)*sat((.46-ml)*3.0);"+
                "float dark=sat((.38-ml)*3.2)*(1.0-glass);"+
                "float metallic=mix(.08,.78,dark);float rough=mix(.43,.22,dark);rough=mix(rough,.075,glass);"+
                "vec3 f0=mix(vec3(.045),base,metallic);f0=mix(f0,vec3(.26,.38,.48),glass);"+
                "float a=max(.035,rough*rough);float k=(rough+1.0)*(rough+1.0)/8.0;"+
                "vec3 F=Fsch(f0,VoH);float D=Dggx(NoH,a);float G=G1(NoV,k)*G1(NoL,k);vec3 spec=(D*G*F)/max(4.0*NoV*max(NoL,.001),.001);"+
                "vec3 kd=(1.0-F)*(1.0-metallic);vec3 diffuse=kd*base/PI;"+
                "float fill=sat(dot(N,normalize(uFill)))*.22;vec3 ambient=base*(.19+.16*sat(N.y*.5+.5)+fill);"+
                "vec3 R=reflect(-V,N);vec3 refl=env(R)*Fsch(f0,NoV)*(1.0-rough*.72);"+
                "float rim=pow(1.0-NoV,3.0);vec3 col=(diffuse+spec)*NoL*2.20+ambient+refl*.55+vec3(.14,.23,.34)*rim*.14;"+
                "col=mix(col,col+env(R)*.42,glass);"+
                "col=vec3(1.0)-exp(-col*uExposure);col=pow(col,vec3(.94));"+
                "gl_FragColor=vec4(col,1.0);}";
    }
}
