package com.muhammetgecgil.mg3dscanner;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.*;
import android.widget.*;
import java.nio.*;
import java.util.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ProCadViewerActivity extends CadViewerActivity {
    ProRenderer pro;
    TextView measureInfo;
    SeekBar sectionBar;
    int measureMode = 0; // 0 off, 1 distance, 2 angle
    final ArrayList<Integer> picks = new ArrayList<>();

    @Override void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8,9,12));
        root.setPadding(8,8,8,8);

        TextView title = tv("MG 3D • PRO CAD MOBILE",21);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        Button open = btn("📂 AÇ"); open.setOnClickListener(v->pick()); top.addView(open,new LinearLayout.LayoutParams(0,-2,1));
        Button fit = btn("⌂ FIT"); fit.setOnClickListener(v->{pro.zoom=1;pro.rx=20;pro.ry=-35;picks.clear();updateMeasure();}); top.addView(fit,new LinearLayout.LayoutParams(0,-2,1));
        Button proj = btn("PERS/ORTO"); proj.setOnClickListener(v->{pro.ortho=!pro.ortho;viewName.setText("Projeksiyon: "+(pro.ortho?"ORTOGRAFİK":"PERSPEKTİF"));}); top.addView(proj,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(top);

        info = tv("Profesyonel mobil CAD: ölç • kesit • surface • ortografik görünüş",12);
        info.setTextColor(Color.LTGRAY); root.addView(info);
        viewName = tv("Görünüş: İzometrik • Perspektif",12); viewName.setTextColor(Color.LTGRAY); root.addView(viewName);
        measureInfo = tv("Ölçüm: kapalı",12); measureInfo.setTextColor(Color.rgb(130,210,255)); root.addView(measureInfo);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        LinearLayout bar = new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL);
        addTool(bar,"ISO",v->setView(20,-35,"İzometrik"));
        addTool(bar,"ÖN",v->setView(0,0,"Ön XY"));
        addTool(bar,"SAĞ",v->setView(0,90,"Sağ YZ"));
        addTool(bar,"ÜST",v->setView(90,0,"Üst XZ"));
        addTool(bar,"YÜZEY",v->{pro.wireframe=false;viewName.setText("Görünüş: Dolu Surface");});
        addTool(bar,"KENAR",v->{pro.wireframe=!pro.wireframe;viewName.setText(pro.wireframe?"Görünüş: Surface + Kenar":"Görünüş: Dolu Surface");});
        addTool(bar,"MESAFE",v->{measureMode=measureMode==1?0:1;picks.clear();updateMeasure();});
        addTool(bar,"AÇI",v->{measureMode=measureMode==2?0:2;picks.clear();updateMeasure();});
        addTool(bar,"KESİT X",v->setSection(1));
        addTool(bar,"KESİT Y",v->setSection(2));
        addTool(bar,"KESİT Z",v->setSection(3));
        addTool(bar,"KESİT KAPAT",v->setSection(0));
        hs.addView(bar); root.addView(hs);

        sectionBar = new SeekBar(this); sectionBar.setMax(1000); sectionBar.setProgress(500);
        sectionBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){pro.clipValue=-1.45f+2.9f*p/1000f;}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        root.addView(sectionBar);

        gl = new android.opengl.GLSurfaceView(this); gl.setEGLContextClientVersion(2);
        pro = new ProRenderer(); renderer = pro; gl.setRenderer(pro); gl.setRenderMode(android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        gl.setOnTouchListener(new ProTouch());
        root.addView(gl,new LinearLayout.LayoutParams(-1,0,1));

        TextView foot=tv("Tek parmak: orbit • iki parmak: zoom • MESAFE/AÇI: model üstüne dokun\nKESİT X/Y/Z seç → slider ile kesit düzlemini hareket ettir. Telefonda hızlı inceleme için optimize edildi.",11);
        foot.setTextColor(Color.GRAY); root.addView(foot);
        setContentView(root);
    }

    void addTool(LinearLayout bar,String name,View.OnClickListener l){Button b=btn(name);b.setOnClickListener(l);bar.addView(b);}
    void setSection(int axis){pro.clipAxis=axis;sectionBar.setProgress(500);viewName.setText(axis==0?"Kesit: kapalı":"Kesit: "+(axis==1?"X":axis==2?"Y":"Z"));}

    @Override void updateInfo(){
        if(current==null)return;
        String ext=ext(fileName).toUpperCase(Locale.US);
        String dim=bounds!=null&&bounds.valid()?String.format(Locale.US,"X %.3f • Y %.3f • Z %.3f",bounds.dx(),bounds.dy(),bounds.dz()):"Boyut okunamadı";
        info.setText(fileName+"\n"+ext+" • "+current.vertexCount()+" vertex • "+current.triangleCount()+" triangle\n"+dim+" (dosya birimi)\nSurface topology + mobil LOD görüntüleme");
    }

    void updateMeasure(){
        if(measureMode==0){measureInfo.setText("Ölçüm: kapalı");return;}
        if(current==null){measureInfo.setText("Ölçüm: önce model aç");return;}
        if(measureMode==1){
            if(picks.size()<2){measureInfo.setText("MESAFE: "+picks.size()+"/2 nokta seç");return;}
            double d=dist(picks.get(0),picks.get(1))*unitScale();
            measureInfo.setText(String.format(Locale.US,"MESAFE = %.4f dosya birimi",d));
        }else{
            if(picks.size()<3){measureInfo.setText("AÇI: "+picks.size()+"/3 nokta seç (köşe ikinci nokta)");return;}
            double a=angle(picks.get(0),picks.get(1),picks.get(2));
            measureInfo.setText(String.format(Locale.US,"AÇI = %.2f°",a));
        }
    }
    double unitScale(){if(bounds==null||!bounds.valid())return 1;double span=Math.max(bounds.dx(),Math.max(bounds.dy(),bounds.dz()));return span/2.6;}
    double dist(int a,int b){float[]v=current.v;double x=v[a*3]-v[b*3],y=v[a*3+1]-v[b*3+1],z=v[a*3+2]-v[b*3+2];return Math.sqrt(x*x+y*y+z*z);}
    double angle(int a,int b,int c){float[]v=current.v;double ax=v[a*3]-v[b*3],ay=v[a*3+1]-v[b*3+1],az=v[a*3+2]-v[b*3+2],cx=v[c*3]-v[b*3],cy=v[c*3+1]-v[b*3+1],cz=v[c*3+2]-v[b*3+2];double dot=ax*cx+ay*cy+az*cz,na=Math.sqrt(ax*ax+ay*ay+az*az),nc=Math.sqrt(cx*cx+cy*cy+cz*cz);if(na*nc<1e-9)return 0;return Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,dot/(na*nc)))));}

    class ProTouch implements View.OnTouchListener{
        float lx,ly,ld; boolean moved=false;
        public boolean onTouch(View v,android.view.MotionEvent e){
            if(measureMode>0 && e.getPointerCount()==1){
                if(e.getAction()==MotionEvent.ACTION_DOWN){lx=e.getX();ly=e.getY();moved=false;return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){if(Math.hypot(e.getX()-lx,e.getY()-ly)>12)moved=true;return true;}
                if(e.getAction()==MotionEvent.ACTION_UP&&!moved){int idx=pro.pickVertex(e.getX(),e.getY());if(idx>=0){int need=measureMode==1?2:3;if(picks.size()>=need)picks.clear();picks.add(idx);updateMeasure();}return true;}
            }
            if(e.getPointerCount()==1){float x=e.getX(),y=e.getY();if(e.getAction()==MotionEvent.ACTION_MOVE){pro.ry+=(x-lx)*.35f;pro.rx+=(y-ly)*.35f;}lx=x;ly=y;}
            else if(e.getPointerCount()>=2){float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1),d=(float)Math.hypot(dx,dy);if(ld>0&&e.getAction()==MotionEvent.ACTION_MOVE)pro.zoom=Math.max(.2f,Math.min(8f,pro.zoom*d/ld));ld=d;}
            if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL)ld=0;return true;
        }
    }

    static class ProRenderer extends CadViewerActivity.SurfaceRenderer{
        boolean ortho=false; int clipAxis=0; float clipValue=0; int w=1,h=1;
        int pp,pPos,pMvp,pColor,pClip; FloatBuffer pvb; IntBuffer pib; int picount;
        float[] lastMvp=new float[16];
        @Override void setModel(ViewerActivity.Model m){super.setModel(m);pvb=null;pib=null;picount=0;}
        @Override public void onSurfaceCreated(GL10 g,EGLConfig c){
            GLES20.glClearColor(.025f,.03f,.04f,1);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glCullFace(GLES20.GL_BACK);
            String vs="attribute vec3 a;uniform mat4 m;varying vec3 p;varying float shade;void main(){p=a;gl_Position=m*vec4(a,1.0);shade=.84+clamp(a.z*.10,-.12,.12);}";
            String fs="precision mediump float;uniform vec4 c;uniform vec4 clip;varying vec3 p;varying float shade;void main(){if(length(clip.xyz)>.5 && dot(p,clip.xyz)>clip.w)discard;gl_FragColor=vec4(c.rgb*shade,c.a);}";
            pp=link2(vs,fs);pPos=GLES20.glGetAttribLocation(pp,"a");pMvp=GLES20.glGetUniformLocation(pp,"m");pColor=GLES20.glGetUniformLocation(pp,"c");pClip=GLES20.glGetUniformLocation(pp,"clip");
        }
        @Override public void onSurfaceChanged(GL10 g,int W,int H){w=Math.max(1,W);h=Math.max(1,H);GLES20.glViewport(0,0,w,h);}
        void matrices(float[]mvp){float asp=w/(float)h;float[]pr=new float[16],mv=new float[16];if(ortho){float sy=1.7f/Math.max(.25f,zoom),sx=sy*asp;Matrix.orthoM(pr,0,-sx,sx,-sy,sy,.1f,100);}else Matrix.perspectiveM(pr,0,45,asp,.1f,100);Matrix.setIdentityM(mv,0);Matrix.translateM(mv,0,0,0,-4f/Math.max(.25f,zoom));Matrix.rotateM(mv,0,rx,1,0,0);Matrix.rotateM(mv,0,ry,0,1,0);Matrix.multiplyMM(mvp,0,pr,0,mv,0);}
        @Override public void onDrawFrame(GL10 g){
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);ViewerActivity.Model m=model;if(m==null||m.v==null||m.v.length==0||m.idx==null||m.idx.length<3)return;
            if(pvb==null){pvb=ByteBuffer.allocateDirect(m.v.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();pvb.put(m.v).position(0);pib=ByteBuffer.allocateDirect(m.idx.length*4).order(ByteOrder.nativeOrder()).asIntBuffer();pib.put(m.idx).position(0);picount=m.idx.length;}
            matrices(lastMvp);GLES20.glUseProgram(pp);GLES20.glUniformMatrix4fv(pMvp,1,false,lastMvp,0);GLES20.glEnableVertexAttribArray(pPos);GLES20.glVertexAttribPointer(pPos,3,GLES20.GL_FLOAT,false,12,pvb);GLES20.glUniform4f(pColor,.30f,.72f,.88f,1);
            if(clipAxis==1)GLES20.glUniform4f(pClip,1,0,0,clipValue);else if(clipAxis==2)GLES20.glUniform4f(pClip,0,1,0,clipValue);else if(clipAxis==3)GLES20.glUniform4f(pClip,0,0,1,clipValue);else GLES20.glUniform4f(pClip,0,0,0,99);
            pib.position(0);GLES20.glDrawElements(GLES20.GL_TRIANGLES,picount,GLES20.GL_UNSIGNED_INT,pib);
            if(wireframe){GLES20.glDisable(GLES20.GL_CULL_FACE);GLES20.glUniform4f(pColor,.05f,.07f,.09f,1);pib.position(0);GLES20.glDrawElements(GLES20.GL_LINES,picount,GLES20.GL_UNSIGNED_INT,pib);GLES20.glEnable(GLES20.GL_CULL_FACE);}GLES20.glDisableVertexAttribArray(pPos);
        }
        int pickVertex(float sx,float sy){ViewerActivity.Model m=model;if(m==null||m.v==null)return-1;float best=42*42;int bi=-1;float[]in=new float[4],out=new float[4];int step=Math.max(1,(m.v.length/3)/30000);for(int i=0;i<m.v.length/3;i+=step){in[0]=m.v[i*3];in[1]=m.v[i*3+1];in[2]=m.v[i*3+2];in[3]=1;Matrix.multiplyMV(out,0,lastMvp,0,in,0);if(Math.abs(out[3])<1e-6)continue;float nx=out[0]/out[3],ny=out[1]/out[3],nz=out[2]/out[3];if(nz<-1||nz>1)continue;float px=(nx*.5f+.5f)*w,py=(1-(ny*.5f+.5f))*h,dx=px-sx,dy=py-sy,d=dx*dx+dy*dy;if(d<best){best=d;bi=i;}}return bi;}
    }
}
