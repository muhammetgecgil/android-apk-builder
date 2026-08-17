package com.muhammetgecgil.modelviewer;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.*;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity {
    static final int OPEN_MODEL = 501;
    ModelView view;
    TextView status, info;
    Button surfaceBtn, wireBtn, pointBtn, autoBtn;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        buildUi();
    }

    TextView txt(String s,float sp){ TextView t=new TextView(this); t.setText(s);t.setTextSize(sp);t.setTextColor(Color.WHITE);t.setPadding(10,6,10,6);return t; }
    Button btn(String s, View.OnClickListener l){ Button b=new Button(this);b.setText(s);b.setOnClickListener(l);return b; }

    void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        view=new ModelView(this);root.addView(view,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setPadding(8,6,8,6);top.setBackgroundColor(0xcc111111);
        top.addView(btn("DOSYA AÇ",v->openFile()));
        top.addView(btn("MERKEZ",v->view.resetView()));
        surfaceBtn=btn("YÜZEY",v->{view.mode=0;view.requestRender();updateModeButtons();});top.addView(surfaceBtn);
        wireBtn=btn("TEL",v->{view.mode=1;view.requestRender();updateModeButtons();});top.addView(wireBtn);
        pointBtn=btn("NOKTA",v->{view.mode=2;view.requestRender();updateModeButtons();});top.addView(pointBtn);
        autoBtn=btn("OTO DÖN",v->{view.autoRotate=!view.autoRotate;autoBtn.setText(view.autoRotate?"OTO: AÇIK":"OTO DÖN");});top.addView(autoBtn);
        top.addView(btn("ÖN",v->view.preset(0)));top.addView(btn("ARKA",v->view.preset(1)));top.addView(btn("SAĞ",v->view.preset(2)));top.addView(btn("SOL",v->view.preset(3)));top.addView(btn("ÜST",v->view.preset(4)));top.addView(btn("ALT",v->view.preset(5)));top.addView(btn("İZO",v->view.preset(6)));
        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);tp.setMargins(8,6,8,0);root.addView(top,tp);

        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(12,6,12,8);bottom.setBackgroundColor(0xbb000000);
        status=txt("OBJ / STL / PLY aç • parmakla döndür • iki parmakla zoom • iki parmak sürükle ile kaydır",13);
        info=txt("Model yüklenmedi",12);info.setTextColor(0xff8fdcff);
        bottom.addView(status);bottom.addView(info);
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);bp.setMargins(14,0,14,12);root.addView(bottom,bp);
        setContentView(root);updateModeButtons();
    }

    void updateModeButtons(){
        surfaceBtn.setEnabled(view.mode!=0);wireBtn.setEnabled(view.mode!=1);pointBtn.setEnabled(view.mode!=2);
    }

    void openFile(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/obj","model/stl","application/octet-stream","text/plain","application/x-ply"});startActivityForResult(i,OPEN_MODEL);
    }

    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(r!=OPEN_MODEL||c!=RESULT_OK||data==null)return;Uri u=data.getData();if(u==null)return;getContentResolver().takePersistableUriPermission(u,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));status.setText("Model okunuyor…");new Thread(()->loadModel(u)).start();}

    String displayName(Uri u){try(Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int x=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(x>=0)return c.getString(x);}}catch(Throwable ignored){}return "model";}

    void loadModel(Uri u){
        try{
            String n=displayName(u);String lower=n.toLowerCase(Locale.ROOT);Mesh m;
            if(lower.endsWith(".obj"))m=parseObj(readAllText(u));
            else if(lower.endsWith(".stl"))m=parseStl(readAllBytes(u));
            else if(lower.endsWith(".ply"))m=parsePly(readAllText(u));
            else throw new Exception("Desteklenen: OBJ, STL, PLY");
            if(m.triangles==null||m.triangles.length<9)throw new Exception("Geçerli üçgen yüzey bulunamadı");
            m.name=n;m.prepare();runOnUiThread(()->{view.setMesh(m);status.setText(n+" yüklendi");info.setText("Köşe: "+m.vertexCount+"   Üçgen: "+m.triangleCount+"   Boyut oranı X/Y/Z: "+fmt(m.sizeX)+" / "+fmt(m.sizeY)+" / "+fmt(m.sizeZ));});
        }catch(Throwable e){runOnUiThread(()->status.setText("Açılamadı: "+e.getMessage()));}
    }

    String fmt(float v){return String.format(Locale.US,"%.3f",v);}
    byte[] readAllBytes(Uri u)throws Exception{try(InputStream in=getContentResolver().openInputStream(u);ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[]b=new byte[65536];int n;while((n=in.read(b))>0)o.write(b,0,n);return o.toByteArray();}}
    String readAllText(Uri u)throws Exception{return new String(readAllBytes(u),StandardCharsets.UTF_8);}

    Mesh parseObj(String s)throws Exception{
        ArrayList<float[]> vs=new ArrayList<>();ArrayList<Float> tri=new ArrayList<>();
        for(String raw:s.split("\\r?\\n")){String l=raw.trim();if(l.startsWith("v ")){String[]p=l.split("\\s+");if(p.length>=4)vs.add(new float[]{Float.parseFloat(p[1]),Float.parseFloat(p[2]),Float.parseFloat(p[3])});}
            else if(l.startsWith("f ")){String[]p=l.substring(2).trim().split("\\s+");if(p.length<3)continue;int[]idx=new int[p.length];for(int i=0;i<p.length;i++){String a=p[i].split("/")[0];int q=Integer.parseInt(a);idx[i]=q<0?vs.size()+q:q-1;}for(int i=1;i<idx.length-1;i++){addV(tri,vs.get(idx[0]));addV(tri,vs.get(idx[i]));addV(tri,vs.get(idx[i+1]));}}
        }return new Mesh(toFloat(tri));
    }
    void addV(ArrayList<Float>a,float[]v){a.add(v[0]);a.add(v[1]);a.add(v[2]);}
    float[]toFloat(ArrayList<Float>a){float[]r=new float[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;}

    Mesh parseStl(byte[] b)throws Exception{
        if(b.length<84)throw new Exception("STL çok küçük");ByteBuffer bb=ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);long count=Integer.toUnsignedLong(bb.getInt(80));if(84+count*50L==b.length){float[]t=new float[(int)count*9];int k=0;bb.position(84);for(int i=0;i<count;i++){bb.position(bb.position()+12);for(int v=0;v<3;v++){t[k++]=bb.getFloat();t[k++]=bb.getFloat();t[k++]=bb.getFloat();}bb.getShort();}return new Mesh(t);}String s=new String(b,StandardCharsets.UTF_8);ArrayList<Float>a=new ArrayList<>();for(String l:s.split("\\r?\\n")){l=l.trim();if(l.startsWith("vertex ")){String[]p=l.split("\\s+");if(p.length>=4){a.add(Float.parseFloat(p[1]));a.add(Float.parseFloat(p[2]));a.add(Float.parseFloat(p[3]));}}}return new Mesh(toFloat(a));
    }

    Mesh parsePly(String s)throws Exception{
        String[]lines=s.split("\\r?\\n");if(lines.length<5||!lines[0].trim().equals("ply"))throw new Exception("PLY değil");int vc=0,fc=0,end=-1;boolean ascii=false;for(int i=1;i<lines.length;i++){String l=lines[i].trim();if(l.startsWith("format ascii"))ascii=true;else if(l.startsWith("element vertex"))vc=Integer.parseInt(l.split("\\s+")[2]);else if(l.startsWith("element face"))fc=Integer.parseInt(l.split("\\s+")[2]);else if(l.equals("end_header")){end=i;break;}}if(!ascii)throw new Exception("Şimdilik ASCII PLY destekleniyor");ArrayList<float[]>v=new ArrayList<>();for(int i=0;i<vc;i++){String[]p=lines[end+1+i].trim().split("\\s+");v.add(new float[]{Float.parseFloat(p[0]),Float.parseFloat(p[1]),Float.parseFloat(p[2])});}ArrayList<Float>a=new ArrayList<>();for(int i=0;i<fc;i++){String[]p=lines[end+1+vc+i].trim().split("\\s+");int n=Integer.parseInt(p[0]);if(n<3)continue;int first=Integer.parseInt(p[1]);for(int j=2;j<n;j++){addV(a,v.get(first));addV(a,v.get(Integer.parseInt(p[j])));addV(a,v.get(Integer.parseInt(p[j+1])));}}return new Mesh(toFloat(a));
    }

    static class Mesh{
        String name="";float[]triangles,normals,wire;FloatBuffer triBuf,normBuf,wireBuf;int vertexCount,triangleCount;float sizeX,sizeY,sizeZ;
        Mesh(float[]t){triangles=t;}
        void prepare(){
            float minx=Float.MAX_VALUE,miny=Float.MAX_VALUE,minz=Float.MAX_VALUE,maxx=-Float.MAX_VALUE,maxy=-Float.MAX_VALUE,maxz=-Float.MAX_VALUE;for(int i=0;i<triangles.length;i+=3){float x=triangles[i],y=triangles[i+1],z=triangles[i+2];minx=Math.min(minx,x);miny=Math.min(miny,y);minz=Math.min(minz,z);maxx=Math.max(maxx,x);maxy=Math.max(maxy,y);maxz=Math.max(maxz,z);}float cx=(minx+maxx)/2,cy=(miny+maxy)/2,cz=(minz+maxz)/2;sizeX=maxx-minx;sizeY=maxy-miny;sizeZ=maxz-minz;float r=Math.max(.000001f,Math.max(sizeX,Math.max(sizeY,sizeZ))/2f);for(int i=0;i<triangles.length;i+=3){triangles[i]=(triangles[i]-cx)/r;triangles[i+1]=(triangles[i+1]-cy)/r;triangles[i+2]=(triangles[i+2]-cz)/r;}triangleCount=triangles.length/9;vertexCount=triangles.length/3;normals=new float[triangles.length];wire=new float[triangleCount*18];int wk=0;for(int i=0;i<triangles.length;i+=9){float ax=triangles[i],ay=triangles[i+1],az=triangles[i+2],bx=triangles[i+3],by=triangles[i+4],bz=triangles[i+5],cx2=triangles[i+6],cy2=triangles[i+7],cz2=triangles[i+8];float ux=bx-ax,uy=by-ay,uz=bz-az,vx=cx2-ax,vy=cy2-ay,vz=cz2-az;float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(len>1e-8){nx/=len;ny/=len;nz/=len;}for(int q=0;q<3;q++){int o=i+q*3;normals[o]=nx;normals[o+1]=ny;normals[o+2]=nz;}float[]e={ax,ay,az,bx,by,bz,bx,by,bz,cx2,cy2,cz2,cx2,cy2,cz2,ax,ay,az};System.arraycopy(e,0,wire,wk,18);wk+=18;}triBuf=buf(triangles);normBuf=buf(normals);wireBuf=buf(wire);
        }
        static FloatBuffer buf(float[]a){ByteBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder());FloatBuffer f=b.asFloatBuffer();f.put(a).position(0);return f;}
    }

    class ModelView extends GLSurfaceView implements GLSurfaceView.Renderer,View.OnTouchListener{
        Mesh mesh;int program,aPos,aNorm,uMvp,uModel,uMode;float rotX=-18,rotY=28,zoom=3.2f,panX=0,panY=0,lastX,lastY;int mode=0;boolean autoRotate=false;ScaleGestureDetector scale;
        ModelView(Context c){super(c);setEGLContextClientVersion(2);setRenderer(this);setRenderMode(RENDERMODE_CONTINUOUSLY);setOnTouchListener(this);scale=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){zoom/=d.getScaleFactor();zoom=Math.max(1.35f,Math.min(12f,zoom));return true;}});}
        void setMesh(Mesh m){mesh=m;resetView();}
        void resetView(){rotX=-18;rotY=28;zoom=3.2f;panX=panY=0;requestRender();}
        void preset(int p){if(p==0){rotX=0;rotY=0;}else if(p==1){rotX=0;rotY=180;}else if(p==2){rotX=0;rotY=90;}else if(p==3){rotX=0;rotY=-90;}else if(p==4){rotX=-90;rotY=0;}else if(p==5){rotX=90;rotY=0;}else{rotX=-25;rotY=35;}zoom=3.2f;panX=panY=0;requestRender();}
        @Override public void onSurfaceCreated(GL10 g,EGLConfig c){GLES20.glClearColor(.015f,.018f,.022f,1);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glDisable(GLES20.GL_CULL_FACE);String vs="uniform mat4 mvp;uniform mat4 model;attribute vec3 p;attribute vec3 n;varying vec3 N;varying vec3 P;void main(){vec4 wp=model*vec4(p,1.0);P=wp.xyz;N=mat3(model)*n;gl_Position=mvp*vec4(p,1.0);gl_PointSize=4.0;}";String fs="precision mediump float;uniform int mode;varying vec3 N;varying vec3 P;void main(){if(mode==2){gl_FragColor=vec4(.15,.8,1.,1.);return;}vec3 nn=normalize(N);vec3 l=normalize(vec3(.5,.8,1.0));float d=max(.16,abs(dot(nn,l)));vec3 base=mode==1?vec3(.1,.75,1.0):vec3(.25,.62,.82);gl_FragColor=vec4(base*d+.05,1.0);}";program=GLES20.glCreateProgram();GLES20.glAttachShader(program,shader(GLES20.GL_VERTEX_SHADER,vs));GLES20.glAttachShader(program,shader(GLES20.GL_FRAGMENT_SHADER,fs));GLES20.glLinkProgram(program);aPos=GLES20.glGetAttribLocation(program,"p");aNorm=GLES20.glGetAttribLocation(program,"n");uMvp=GLES20.glGetUniformLocation(program,"mvp");uModel=GLES20.glGetUniformLocation(program,"model");uMode=GLES20.glGetUniformLocation(program,"mode");}
        int shader(int t,String s){int q=GLES20.glCreateShader(t);GLES20.glShaderSource(q,s);GLES20.glCompileShader(q);return q;}
        @Override public void onSurfaceChanged(GL10 g,int w,int h){GLES20.glViewport(0,0,w,h);}
        @Override public void onDrawFrame(GL10 g){GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);if(mesh==null)return;if(autoRotate)rotY+=.35f;float[]proj=new float[16],viewM=new float[16],modelM=new float[16],tmp=new float[16],mvp=new float[16];Matrix.perspectiveM(proj,0,45f,(float)getWidth()/Math.max(1,getHeight()),.05f,50);Matrix.setLookAtM(viewM,0,panX,panY,zoom,panX,panY,0,0,1,0);Matrix.setIdentityM(modelM,0);Matrix.rotateM(modelM,0,rotX,1,0,0);Matrix.rotateM(modelM,0,rotY,0,1,0);Matrix.multiplyMM(tmp,0,viewM,0,modelM,0);Matrix.multiplyMM(mvp,0,proj,0,tmp,0);GLES20.glUseProgram(program);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,modelM,0);GLES20.glUniform1i(uMode,mode);if(mode==1){mesh.wireBuf.position(0);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,mesh.wireBuf);GLES20.glEnableVertexAttribArray(aPos);GLES20.glDisableVertexAttribArray(aNorm);GLES20.glVertexAttrib3f(aNorm,0,0,1);GLES20.glDrawArrays(GLES20.GL_LINES,0,mesh.wire.length/3);}else{mesh.triBuf.position(0);mesh.normBuf.position(0);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,mesh.triBuf);GLES20.glEnableVertexAttribArray(aPos);GLES20.glVertexAttribPointer(aNorm,3,GLES20.GL_FLOAT,false,0,mesh.normBuf);GLES20.glEnableVertexAttribArray(aNorm);GLES20.glDrawArrays(mode==2?GLES20.GL_POINTS:GLES20.GL_TRIANGLES,0,mesh.vertexCount);}}
        @Override public boolean onTouch(View v,MotionEvent e){scale.onTouchEvent(e);if(e.getPointerCount()==1&&!scale.isInProgress()){if(e.getAction()==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getX()-lastX,dy=e.getY()-lastY;rotY+=dx*.35f;rotX+=dy*.35f;lastX=e.getX();lastY=e.getY();return true;}}else if(e.getPointerCount()==2&&!scale.isInProgress()&&e.getAction()==MotionEvent.ACTION_MOVE){float mx=(e.getX(0)+e.getX(1))/2f,my=(e.getY(0)+e.getY(1))/2f;if(lastX!=0||lastY!=0){panX-=(mx-lastX)/getWidth()*2f;panY+=(my-lastY)/getHeight()*2f;}lastX=mx;lastY=my;return true;}if(e.getAction()==MotionEvent.ACTION_UP){lastX=lastY=0;}return true;}
    }
}
