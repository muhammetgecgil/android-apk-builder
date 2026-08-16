package com.muhammetgecgil.camera3d;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity implements GLSurfaceView.Renderer {
    private static final int REQ_CAMERA = 71;
    private GLSurfaceView glView;
    private CoverageView coverageView;
    private Session session;
    private BackgroundRenderer backgroundRenderer;
    private volatile boolean scanning = false;
    private final ArrayList<float[]> points = new ArrayList<>();
    private final HashSet<Long> voxelKeys = new HashSet<>();
    private final boolean[][] covered = new boolean[5][12];
    private TextView status, countText, guideText;
    private ProgressBar progress;
    private Button scanButton, exportButton;
    private long lastUiUpdate = 0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else setupAr();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setPreserveEGLContextOnPause(true);
        glView.setRenderer(this);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        coverageView = new CoverageView();
        body.addView(glView, new LinearLayout.LayoutParams(0, -1, 1f));
        body.addView(coverageView, new LinearLayout.LayoutParams(0, -1, 1f));
        root.addView(body, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(22, 18, 22, 14);
        top.setBackgroundColor(0xcc111111);
        TextView title = new TextView(this);
        title.setText("CAMERA 3D • CAD SCAN 1.2");
        title.setTextColor(Color.WHITE); title.setTextSize(19);
        status = new TextView(this); status.setText("ARCore hazırlanıyor…"); status.setTextColor(0xffdddddd); status.setTextSize(13);
        countText = new TextView(this); countText.setText("3D nokta: 0 • kapsama: %0"); countText.setTextColor(0xff9ee7ff); countText.setTextSize(15);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); progress.setMax(100);
        guideText = new TextView(this); guideText.setText("SOL: canlı kamera • SAĞ: ön/yan görünüş + eksik tarama bölgeleri"); guideText.setTextColor(Color.WHITE); guideText.setTextSize(12);
        top.addView(title); top.addView(status); top.addView(countText); top.addView(progress); top.addView(guideText);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP); tp.setMargins(10, 34, 10, 0); root.addView(top, tp);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL); bottom.setPadding(14, 12, 14, 18); bottom.setBackgroundColor(0xdd111111);
        LinearLayout row = new LinearLayout(this);
        scanButton = new Button(this); scanButton.setText("3D TARAMAYI BAŞLAT"); scanButton.setOnClickListener(v -> toggleScan());
        Button clear = new Button(this); clear.setText("TEMİZLE"); clear.setOnClickListener(v -> clearScan());
        row.addView(scanButton, new LinearLayout.LayoutParams(0, -2, 2f)); row.addView(clear, new LinearLayout.LayoutParams(0, -2, 1f));
        bottom.addView(row);
        exportButton = new Button(this); exportButton.setText("CAD VERİSİ DIŞA AKTAR • PLY + OBJ"); exportButton.setEnabled(false); exportButton.setOnClickListener(v -> exportCadData()); bottom.addView(exportButton);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM); bp.setMargins(10, 0, 10, 28); root.addView(bottom, bp);
        setContentView(root);
    }

    private void setupAr() {
        try {
            ArCoreApk.Availability a = ArCoreApk.getInstance().checkAvailability(this);
            if (a.isUnsupported()) { status.setText("Bu cihaz ARCore desteklemiyor"); return; }
            if (session != null) return;
            session = new Session(this);
            Config c = new Config(session);
            c.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            session.configure(c);
            status.setText("ARCore hazır • kamera açılıyor");
        } catch (Throwable t) { status.setText("ARCore başlatılamadı: " + t.getClass().getSimpleName()); }
    }

    private void toggleScan() {
        if (session == null) { setupAr(); if (session == null) return; }
        scanning = !scanning;
        scanButton.setText(scanning ? "TARAMAYI BİTİR" : "3D TARAMAYI BAŞLAT");
        guideText.setText(scanning ? "Eksik kırmızı bölgeleri kapatmak için nesnenin çevresinde yavaşça hareket et" : "Tarama durdu • sağ panelden eksik yüzleri kontrol et");
        if (!scanning) exportButton.setEnabled(points.size() > 100);
    }

    private synchronized void clearScan() {
        scanning = false; points.clear(); voxelKeys.clear();
        for (int y=0;y<5;y++) for(int x=0;x<12;x++) covered[y][x]=false;
        scanButton.setText("3D TARAMAYI BAŞLAT"); exportButton.setEnabled(false); countText.setText("3D nokta: 0 • kapsama: %0"); progress.setProgress(0);
        coverageView.updateData(new ArrayList<>(), covered, -1, -1, "Tarama bekleniyor");
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        backgroundRenderer = new BackgroundRenderer();
        backgroundRenderer.create();
        GLES20.glClearColor(0,0,0,1);
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        if (session != null) session.setDisplayGeometry(getWindowManager().getDefaultDisplay().getRotation(), width, height);
    }

    @Override public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        Session s = session; BackgroundRenderer br = backgroundRenderer;
        if (s == null || br == null || br.textureId < 0) return;
        try {
            s.setCameraTextureName(br.textureId);
            Frame frame = s.update();
            br.draw(frame);
            Camera cam = frame.getCamera();
            if (cam.getTrackingState() != TrackingState.TRACKING) {
                runOnUiThread(() -> status.setText("Kamera açık • AR takibi için telefonu yavaşça hareket ettir"));
                return;
            }
            if (scanning) {
                markCoverage(cam.getPose());
                collectPointCloud(frame);
            } else {
                long now=System.currentTimeMillis();
                if(now-lastUiUpdate>1200){lastUiUpdate=now;runOnUiThread(()->status.setText("Kamera aktif • taramayı başlat"));}
            }
        } catch (Throwable t) { runOnUiThread(() -> status.setText("AR kamera hatası: " + t.getClass().getSimpleName())); }
    }

    private void markCoverage(Pose pose) {
        float[] z = pose.getZAxis();
        float dx=-z[0], dy=-z[1], dz=-z[2];
        double az=Math.atan2(dx,dz); // -pi..pi
        double el=Math.asin(Math.max(-1,Math.min(1,dy)));
        int ax=(int)Math.floor((az+Math.PI)/(2*Math.PI)*12); ax=Math.max(0,Math.min(11,ax));
        int ey=(int)Math.floor((el+Math.PI/2)/Math.PI*5); ey=Math.max(0,Math.min(4,ey));
        covered[ey][ax]=true;
    }

    private void collectPointCloud(Frame frame) {
        try (PointCloud cloud = frame.acquirePointCloud()) {
            FloatBuffer fb=cloud.getPoints(); int count=fb.remaining()/4; int stride=Math.max(1,count/900);
            synchronized(this){
                for(int i=0;i<count;i+=stride){int p=i*4; float x=fb.get(p),y=fb.get(p+1),z=fb.get(p+2),c=fb.get(p+3); if(c<0.25f)continue; if(Math.abs(x)>6||Math.abs(y)>6||Math.abs(z)>6)continue; long key=voxelKey(x,y,z,0.007f); if(voxelKeys.add(key))points.add(new float[]{x,y,z,c}); if(points.size()>150000){scanning=false;break;}}
                updateUiFromScan();
            }
        } catch(Throwable ignored){}
    }

    private void updateUiFromScan(){
        long now=System.currentTimeMillis(); if(now-lastUiUpdate<400)return; lastUiUpdate=now;
        int cov=0; for(int y=0;y<5;y++)for(int x=0;x<12;x++)if(covered[y][x])cov++;
        int pct=(int)(100f*cov/60f); int n=points.size();
        String hint=coverageHint();
        ArrayList<float[]> sample=new ArrayList<>(); int step=Math.max(1,n/2500); for(int i=0;i<n;i+=step)sample.add(points.get(i));
        int curX=-1,curY=-1;
        runOnUiThread(()->{countText.setText("3D nokta: "+n+" • kapsama: %"+pct);progress.setProgress(pct);status.setText(n==0?"Kamera aktif • özellik noktaları aranıyor":"Tarama aktif • "+hint);guideText.setText(hint);coverageView.updateData(sample,covered,curX,curY,hint);exportButton.setEnabled(n>100);});
    }

    private String coverageHint(){
        int top=0,bottom=0,left=0,right=0,back=0;
        for(int y=0;y<5;y++)for(int x=0;x<12;x++)if(!covered[y][x]){if(y>=3)top++;if(y<=1)bottom++;if(x<=2)left++;if(x>=9)right++;if(x>=4&&x<=7)back++;}
        int m=Math.max(top,Math.max(bottom,Math.max(left,Math.max(right,back))));
        if(m==0)return "Tarama kapsaması tamamlandı";
        if(m==top)return "EKSİK: üst yüzey • telefonu biraz yükselt";
        if(m==bottom)return "EKSİK: alt yüzey • kamerayı daha aşağı indir";
        if(m==left)return "EKSİK: sol taraf • nesnenin soluna geç";
        if(m==right)return "EKSİK: sağ taraf • nesnenin sağına geç";
        return "EKSİK: arka taraf • nesnenin arkasını tara";
    }

    private long voxelKey(float x,float y,float z,float c){long ix=(long)Math.floor(x/c)+1048576,iy=(long)Math.floor(y/c)+1048576,iz=(long)Math.floor(z/c)+1048576;return((ix&0x1fffffL)<<42)|((iy&0x1fffffL)<<21)|(iz&0x1fffffL);}

    private void exportCadData(){
        final ArrayList<float[]> copy; synchronized(this){copy=new ArrayList<>(points);} if(copy.size()<10){Toast.makeText(this,"Önce tarama yap",Toast.LENGTH_SHORT).show();return;}
        new Thread(()->{try{String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());writeDownload("CADScan_"+stamp+".ply","application/octet-stream",buildPly(copy));writeDownload("CADScan_"+stamp+".obj","text/plain",buildObj(copy));runOnUiThread(()->Toast.makeText(this,"PLY + OBJ Download/Camera3D içine kaydedildi",Toast.LENGTH_LONG).show());}catch(Throwable t){runOnUiThread(()->Toast.makeText(this,"Dışa aktarma hatası",Toast.LENGTH_LONG).show());}}).start();
    }
    private byte[] buildPly(ArrayList<float[]> p){StringBuilder s=new StringBuilder();s.append("ply\nformat ascii 1.0\nelement vertex ").append(p.size()).append("\nproperty float x\nproperty float y\nproperty float z\nend_header\n");for(float[]v:p)s.append(v[0]).append(' ').append(v[1]).append(' ').append(v[2]).append('\n');return s.toString().getBytes(StandardCharsets.UTF_8);}
    private byte[] buildObj(ArrayList<float[]> p){StringBuilder s=new StringBuilder("# Camera3D CAD Scan\n");for(float[]v:p)s.append("v ").append(v[0]).append(' ').append(v[1]).append(' ').append(v[2]).append('\n');return s.toString().getBytes(StandardCharsets.UTF_8);}
    private Uri writeDownload(String n,String m,byte[] d)throws Exception{ContentValues cv=new ContentValues();cv.put(MediaStore.Downloads.DISPLAY_NAME,n);cv.put(MediaStore.Downloads.MIME_TYPE,m);cv.put(MediaStore.Downloads.RELATIVE_PATH,"Download/Camera3D");Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);try(OutputStream o=getContentResolver().openOutputStream(u)){o.write(d);}return u;}

    @Override protected void onResume(){super.onResume();if(session==null&&checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)setupAr();if(session!=null){try{session.resume();}catch(CameraNotAvailableException e){status.setText("Kamera ARCore tarafından açılamadı");}}if(glView!=null)glView.onResume();}
    @Override protected void onPause(){if(glView!=null)glView.onPause();if(session!=null)session.pause();super.onPause();}
    @Override protected void onDestroy(){if(session!=null){session.close();session=null;}super.onDestroy();}
    @Override public void onRequestPermissionsResult(int r,String[]p,int[]g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_CAMERA&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED){setupAr();try{session.resume();}catch(Throwable ignored){}}else status.setText("Kamera izni gerekli");}

    private class CoverageView extends View {
        Paint paint=new Paint(1); ArrayList<float[]> sample=new ArrayList<>(); boolean[][] map=new boolean[5][12]; String hint="";
        CoverageView(){super(MainActivity.this);paint.setStrokeWidth(2f);setBackgroundColor(0xff101318);}
        synchronized void updateData(ArrayList<float[]> s,boolean[][] c,int cx,int cy,String h){sample=s;for(int y=0;y<5;y++)System.arraycopy(c[y],0,map[y],0,12);hint=h;invalidate();}
        @Override protected synchronized void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();paint.setTextSize(28);paint.setColor(Color.WHITE);c.drawText("3D KONTROL",18,145,paint);drawProjection(c,18,175,w-36,(h-430)/2,true);drawProjection(c,18,205+(h-430)/2,w-36,(h-430)/2,false);drawCoverage(c,18,h-210,w-36,130);paint.setTextSize(20);paint.setColor(0xffffcc66);c.drawText(hint,18,h-55,paint);}
        void drawProjection(Canvas c,float x,float y,float w,float h,boolean front){paint.setStyle(Paint.Style.STROKE);paint.setColor(0xff555555);c.drawRect(x,y,x+w,y+h,paint);paint.setStyle(Paint.Style.FILL);paint.setTextSize(18);paint.setColor(0xffbbbbbb);c.drawText(front?"ÖN GÖRÜNÜŞ":"YAN GÖRÜNÜŞ",x+8,y+22,paint);if(sample.isEmpty())return;float minA=999,minB=999,maxA=-999,maxB=-999;for(float[]p:sample){float a=front?p[0]:p[2],b=p[1];minA=Math.min(minA,a);maxA=Math.max(maxA,a);minB=Math.min(minB,b);maxB=Math.max(maxB,b);}float da=Math.max(.01f,maxA-minA),db=Math.max(.01f,maxB-minB);paint.setColor(0xff66ddff);for(float[]p:sample){float a=front?p[0]:p[2],b=p[1];float px=x+8+(a-minA)/da*(w-16),py=y+h-8-(b-minB)/db*(h-35);c.drawCircle(px,py,1.5f,paint);}}
        void drawCoverage(Canvas c,float x,float y,float w,float h){float cw=w/12f,ch=h/5f;for(int r=0;r<5;r++)for(int col=0;col<12;col++){paint.setColor(map[r][col]?0xff2e9d58:0xff7d2832);paint.setStyle(Paint.Style.FILL);c.drawRect(x+col*cw+1,y+r*ch+1,x+(col+1)*cw-1,y+(r+1)*ch-1,paint);}paint.setStyle(Paint.Style.STROKE);paint.setColor(Color.WHITE);c.drawRect(x,y,x+w,y+h,paint);paint.setStyle(Paint.Style.FILL);paint.setTextSize(16);c.drawText("YEŞİL=tarandı  KIRMIZI=eksik",x,y-8,paint);}
    }

    private static class BackgroundRenderer {
        int textureId=-1,program=-1,aPos,aUv,uTex; FloatBuffer verts,uvs; final float[] quad={-1,-1,1,-1,-1,1,1,1};
        void create(){int[]t=new int[1];GLES20.glGenTextures(1,t,0);textureId=t[0];GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);String vs="attribute vec2 a_Position;attribute vec2 a_TexCoord;varying vec2 v_TexCoord;void main(){gl_Position=vec4(a_Position,0.0,1.0);v_TexCoord=a_TexCoord;}";String fs="#extension GL_OES_EGL_image_external : require\nprecision mediump float;uniform samplerExternalOES u_Texture;varying vec2 v_TexCoord;void main(){gl_FragColor=texture2D(u_Texture,v_TexCoord);}";program=link(vs,fs);aPos=GLES20.glGetAttribLocation(program,"a_Position");aUv=GLES20.glGetAttribLocation(program,"a_TexCoord");uTex=GLES20.glGetUniformLocation(program,"u_Texture");verts=buf(quad);uvs=buf(new float[8]);}
        void draw(Frame f){FloatBuffer in=buf(quad);uvs.position(0);f.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,in,Coordinates2d.TEXTURE_NORMALIZED,uvs);GLES20.glDisable(GLES20.GL_DEPTH_TEST);GLES20.glUseProgram(program);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);GLES20.glUniform1i(uTex,0);verts.position(0);uvs.position(0);GLES20.glEnableVertexAttribArray(aPos);GLES20.glVertexAttribPointer(aPos,2,GLES20.GL_FLOAT,false,0,verts);GLES20.glEnableVertexAttribArray(aUv);GLES20.glVertexAttribPointer(aUv,2,GLES20.GL_FLOAT,false,0,uvs);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);GLES20.glDisableVertexAttribArray(aPos);GLES20.glDisableVertexAttribArray(aUv);}
        static FloatBuffer buf(float[]a){FloatBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(a).position(0);return b;}
        static int shader(int type,String s){int x=GLES20.glCreateShader(type);GLES20.glShaderSource(x,s);GLES20.glCompileShader(x);return x;}
        static int link(String v,String f){int p=GLES20.glCreateProgram();GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,v));GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,f));GLES20.glLinkProgram(p);return p;}
    }
}
