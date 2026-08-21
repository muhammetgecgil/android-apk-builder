package com.mg.stonefluorescence;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 1001;
    private static final int GRID_X = 8, GRID_Y = 6;

    private TextureView textureView;
    private ScienceOverlay overlay;
    private TextView scoreText, detailText, modeText;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private CaptureRequest.Builder previewBuilder;
    private CameraCharacteristics cameraCharacteristics;
    private Rect activeArray;

    private double baseR=-1, baseG=-1, baseB=-1, baseY=-1;
    private double darkR=0, darkG=0, darkB=0;
    private int warmupFrames=0, darkFrames=0;
    private double darkSumR=0, darkSumG=0, darkSumB=0;
    private boolean capturingDark=false, measurementLocked=false;
    private long lastUiMs=0;
    private String currentMode="GERÇEK";

    static class Stat {
        double r,g,b,y,saturation;
        double[] grid=new double[GRID_X*GRID_Y];
        double[] gridRG=new double[GRID_X*GRID_Y];
    }

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(buildUi());
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) startCameraFlow();
        else requestPermissions(new String[]{Manifest.permission.CAMERA},REQ_CAMERA);
    }

    private View buildUi(){
        FrameLayout root=new FrameLayout(this); root.setBackgroundColor(Color.BLACK);
        textureView=new TextureView(this);
        textureView.setOnTouchListener((v,e)->{ if(e.getAction()==MotionEvent.ACTION_UP){ focusAt(e.getX(),e.getY()); overlay.showFocus(e.getX(),e.getY()); } return true;});
        root.addView(textureView,new FrameLayout.LayoutParams(-1,-1));
        overlay=new ScienceOverlay(this); root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.VERTICAL); top.setPadding(dp(12),dp(14),dp(12),dp(8)); top.setBackgroundColor(0xA0000000);
        top.addView(label("STONE GLOW • SCIENTIFIC",19,true,Color.WHITE));
        modeText=label("MOD: GERÇEK",12,true,Color.rgb(170,220,255)); modeText.setPadding(0,dp(3),0,dp(3)); top.addView(modeText);
        top.addView(label("365 nm UV + 420 nm long-pass önerilir • dokun: netlik",11,false,Color.LTGRAY));
        HorizontalScrollView hs=new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        addButton(row,"GERÇEK",v->setDisplayMode("GERÇEK"));
        addButton(row,"EMİSYON+",v->setDisplayMode("EMİSYON+"));
        addButton(row,"R/G HARİTA",v->setDisplayMode("R/G HARİTA"));
        addButton(row,"ÖLÇÜM HARİTA",v->setDisplayMode("ÖLÇÜM HARİTA"));
        addButton(row,"KALİBRE",v->calibrate());
        addButton(row,"DARK REF",v->startDarkReference());
        hs.addView(row); top.addView(hs,new LinearLayout.LayoutParams(-1,dp(50)));
        root.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));

        LinearLayout bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.VERTICAL); bottom.setPadding(dp(12),dp(9),dp(12),dp(14)); bottom.setBackgroundColor(0xB8000000);
        scoreText=label("Kamera hazırlanıyor…",19,true,Color.WHITE); scoreText.setGravity(Gravity.CENTER_HORIZONTAL); bottom.addView(scoreText);
        detailText=label("UV efekti üretilmez; yalnız kameranın ölçtüğü görünür floresans işlenir.",11,false,Color.LTGRAY); detailText.setPadding(0,dp(5),0,0); bottom.addView(detailText);
        root.addView(bottom,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        return root;
    }

    private void addButton(LinearLayout row,String text,View.OnClickListener l){ Button b=new Button(this); b.setText(text); b.setTextSize(10); b.setAllCaps(false); b.setOnClickListener(l); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(105),dp(44)); lp.setMargins(dp(2),dp(3),dp(2),0); row.addView(b,lp); }
    private TextView label(String s,int sp,boolean bold,int color){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD); return t; }

    private void setDisplayMode(String mode){
        currentMode=mode; modeText.setText("MOD: "+mode); overlay.setMode(mode);
        if(!"EMİSYON+".equals(mode)){ textureView.setLayerType(View.LAYER_TYPE_NONE,null); return; }
        float c=1.45f,t=128f*(1f-c);
        ColorMatrix m=new ColorMatrix(new float[]{c,0,0,0,t, 0,c,0,0,t, 0,0,c,0,t, 0,0,0,1,0});
        Paint p=new Paint(); p.setColorFilter(new ColorMatrixColorFilter(m)); textureView.setLayerType(View.LAYER_TYPE_HARDWARE,p); textureView.invalidate();
    }

    private void calibrate(){ baseR=baseG=baseB=baseY=-1; warmupFrames=0; measurementLocked=false; setAeAwbLock(false); scoreText.setText("KALİBRASYON • sabit tut"); detailText.setText("30 kare referans alınacak; ardından pozlama ve beyaz dengesi kilitlenecek."); }
    private void startDarkReference(){ darkFrames=0; darkSumR=darkSumG=darkSumB=0; capturingDark=true; scoreText.setText("DARK REF • lensi kapat"); detailText.setText("20 karanlık kare sensör/ofset referansı olarak ölçülüyor."); }

    private void setAeAwbLock(boolean lock){
        if(previewBuilder==null||captureSession==null)return;
        try{ previewBuilder.set(CaptureRequest.CONTROL_AE_LOCK,lock); previewBuilder.set(CaptureRequest.CONTROL_AWB_LOCK,lock); captureSession.setRepeatingRequest(previewBuilder.build(),null,cameraHandler); measurementLocked=lock; }catch(Exception ignored){}
    }

    private void startCameraFlow(){
        startBackgroundThread();
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){
            @Override public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){openCamera();}
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){}
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture s){return true;}
            @Override public void onSurfaceTextureUpdated(SurfaceTexture s){}
        });
        if(textureView.isAvailable())openCamera();
    }
    private void startBackgroundThread(){ if(cameraThread!=null)return; cameraThread=new HandlerThread("StoneGlowScientific"); cameraThread.start(); cameraHandler=new Handler(cameraThread.getLooper()); }

    private void openCamera(){
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
        CameraManager m=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            String selected=null;
            for(String id:m.getCameraIdList()){ CameraCharacteristics cc=m.getCameraCharacteristics(id); Integer f=cc.get(CameraCharacteristics.LENS_FACING); if(f!=null&&f==CameraCharacteristics.LENS_FACING_BACK){selected=id;cameraCharacteristics=cc;break;} }
            if(selected==null){selected=m.getCameraIdList()[0];cameraCharacteristics=m.getCameraCharacteristics(selected);} activeArray=cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            imageReader=ImageReader.newInstance(640,480,android.graphics.ImageFormat.YUV_420_888,3);
            imageReader.setOnImageAvailableListener(r->{Image im=r.acquireLatestImage(); if(im==null)return; try{analyze(im);}finally{im.close();}},cameraHandler);
            m.openCamera(selected,new CameraDevice.StateCallback(){
                @Override public void onOpened(CameraDevice c){cameraDevice=c;createSession();}
                @Override public void onDisconnected(CameraDevice c){c.close();cameraDevice=null;}
                @Override public void onError(CameraDevice c,int e){c.close();cameraDevice=null;}
            },cameraHandler);
        }catch(Exception e){runOnUiThread(()->scoreText.setText("Kamera açılamadı"));}
    }

    private void createSession(){
        if(cameraDevice==null||!textureView.isAvailable())return;
        try{
            SurfaceTexture st=textureView.getSurfaceTexture(); st.setDefaultBufferSize(1280,720); Surface preview=new Surface(st), analysis=imageReader.getSurface();
            previewBuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW); previewBuilder.addTarget(preview); previewBuilder.addTarget(analysis);
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE); previewBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON); previewBuilder.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_AUTO);
            cameraDevice.createCaptureSession(Arrays.asList(preview,analysis),new CameraCaptureSession.StateCallback(){
                @Override public void onConfigured(CameraCaptureSession s){captureSession=s;try{s.setRepeatingRequest(previewBuilder.build(),null,cameraHandler);runOnUiThread(()->calibrate());}catch(Exception ignored){}}
                @Override public void onConfigureFailed(CameraCaptureSession s){}
            },cameraHandler);
        }catch(Exception ignored){}
    }

    private void focusAt(float x,float y){
        if(captureSession==null||previewBuilder==null)return;
        try{
            if(activeArray!=null){ float nx=x/Math.max(1f,textureView.getWidth()),ny=y/Math.max(1f,textureView.getHeight()); int sx=activeArray.left+(int)(nx*activeArray.width()),sy=activeArray.top+(int)(ny*activeArray.height()),half=Math.max(80,Math.min(activeArray.width(),activeArray.height())/20); Rect r=new Rect(Math.max(activeArray.left,sx-half),Math.max(activeArray.top,sy-half),Math.min(activeArray.right,sx+half),Math.min(activeArray.bottom,sy+half)); Integer maxAf=cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF); if(maxAf!=null&&maxAf>0)previewBuilder.set(CaptureRequest.CONTROL_AF_REGIONS,new MeteringRectangle[]{new MeteringRectangle(r,MeteringRectangle.METERING_WEIGHT_MAX)}); }
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_AUTO); previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START); captureSession.capture(previewBuilder.build(),null,cameraHandler); previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_IDLE); previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE); captureSession.setRepeatingRequest(previewBuilder.build(),null,cameraHandler);
            detailText.setText("Netlik dokunulan noktaya ayarlandı; pozlama kilidi korunuyor.");
        }catch(Exception ignored){}
    }

    private void analyze(Image image){
        Stat s=statFromYuv(image); if(s==null)return;
        if(capturingDark){ darkSumR+=s.r;darkSumG+=s.g;darkSumB+=s.b;darkFrames++; if(darkFrames>=20){darkR=darkSumR/darkFrames;darkG=darkSumG/darkFrames;darkB=darkSumB/darkFrames;capturingDark=false;runOnUiThread(()->{scoreText.setText("DARK REF kaydedildi");detailText.setText(String.format(Locale.US,"Taban R %.3f • G %.3f • B %.3f",darkR,darkG,darkB));});} return; }
        double r=Math.max(0,s.r-darkR),g=Math.max(0,s.g-darkG),b=Math.max(0,s.b-darkB),y=Math.max(0,s.y-(.2126*darkR+.7152*darkG+.0722*darkB));
        if(baseR<0){baseR=r;baseG=g;baseB=b;baseY=y;warmupFrames=1;return;}
        if(warmupFrames<30){baseR=ema(baseR,r,.12);baseG=ema(baseG,g,.12);baseB=ema(baseB,b,.12);baseY=ema(baseY,y,.12);warmupFrames++;if(warmupFrames==30){setAeAwbLock(true);runOnUiThread(()->scoreText.setText("ÖLÇÜM KİLİTLİ • HAZIR"));}return;}
        double lumRise=(y-baseY)/(baseY+.01),rgShift=r/(g+.01)-baseR/(baseG+.01),bgShift=b/(g+.01)-baseB/(baseG+.01),emission=Math.max(0,lumRise);
        double score=100*clamp(.60*clamp(emission/.60)+.25*clamp(Math.abs(rgShift)/.45)+.15*clamp(Math.abs(bgShift)/.45));
        overlay.setGrid(s.grid,s.gridRG,baseY,currentMode);
        long now=System.currentTimeMillis(); if(now-lastUiMs>140){lastUiMs=now;double fs=score,frg=rgShift,fbg=bgShift,fy=emission;runOnUiThread(()->{String level=fs>=70?"GÜÇLÜ GÖRÜNÜR FLORESANS":fs>=40?"BELİRGİN FLORESANS":fs>=20?"ZAYIF TEPKİ":"REFERANSA YAKIN";scoreText.setText(String.format(Locale.US,"%s  %.0f/100",level,fs));detailText.setText(String.format(Locale.US,"Sinyal artışı %.1f%% • ΔR/G %+.3f • ΔB/G %+.3f • AE/AWB %s",fy*100,frg,fbg,measurementLocked?"KİLİTLİ":"AÇIK"));});}
    }

    private Stat statFromYuv(Image image){
        if(image.getPlanes().length<3)return null; Image.Plane yp=image.getPlanes()[0],up=image.getPlanes()[1],vp=image.getPlanes()[2]; ByteBuffer yb=yp.getBuffer(),ub=up.getBuffer(),vb=vp.getBuffer(); int w=image.getWidth(),h=image.getHeight(),yRow=yp.getRowStride(),yPix=yp.getPixelStride(),uRow=up.getRowStride(),uPix=up.getPixelStride(),vRow=vp.getRowStride(),vPix=vp.getPixelStride(); double sr=0,sg=0,sb=0,sy=0,ss=0;int n=0;double[] gs=new double[GRID_X*GRID_Y],gr=new double[GRID_X*GRID_Y];int[] gn=new int[GRID_X*GRID_Y];
        for(int yy=0;yy<h;yy+=5)for(int xx=0;xx<w;xx+=5){int yi=yy*yRow+xx*yPix,ui=(yy/2)*uRow+(xx/2)*uPix,vi=(yy/2)*vRow+(xx/2)*vPix;if(yi>=yb.limit()||ui>=ub.limit()||vi>=vb.limit())continue;int Y=yb.get(yi)&255,U=(ub.get(ui)&255)-128,V=(vb.get(vi)&255)-128,R=clamp255((int)(Y+1.402*V)),G=clamp255((int)(Y-.344136*U-.714136*V)),B=clamp255((int)(Y+1.772*U));double rf=R/255.0,gf=G/255.0,bf=B/255.0,lum=.2126*rf+.7152*gf+.0722*bf;int mx=Math.max(R,Math.max(G,B)),mn=Math.min(R,Math.min(G,B));sr+=rf;sg+=gf;sb+=bf;sy+=lum;ss+=mx==0?0:(mx-mn)/(double)mx;n++;int gx=Math.min(GRID_X-1,xx*GRID_X/w),gy=Math.min(GRID_Y-1,yy*GRID_Y/h),i=gy*GRID_X+gx;gs[i]+=lum;gr[i]+=rf/(gf+.01);gn[i]++;}
        if(n==0)return null;Stat s=new Stat();s.r=sr/n;s.g=sg/n;s.b=sb/n;s.y=sy/n;s.saturation=ss/n;for(int i=0;i<s.grid.length;i++)if(gn[i]>0){s.grid[i]=gs[i]/gn[i];s.gridRG[i]=gr[i]/gn[i];}return s;
    }

    private double ema(double a,double b,double k){return a*(1-k)+b*k;} private double clamp(double v){return Math.max(0,Math.min(1,v));} private int clamp255(int v){return Math.max(0,Math.min(255,v));} private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==REQ_CAMERA&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)startCameraFlow();}
    @Override protected void onDestroy(){super.onDestroy();try{if(captureSession!=null)captureSession.close();}catch(Exception ignored){}try{if(cameraDevice!=null)cameraDevice.close();}catch(Exception ignored){}try{if(imageReader!=null)imageReader.close();}catch(Exception ignored){}if(cameraThread!=null){cameraThread.quitSafely();cameraThread=null;}}

    static class ScienceOverlay extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private String mode="GERÇEK";private double[] grid=new double[GRID_X*GRID_Y],gridRG=new double[GRID_X*GRID_Y];private double baseY=.1;private float fx=-1,fy=-1;private long focusUntil=0;
        ScienceOverlay(Context c){super(c);setWillNotDraw(false);}void setMode(String m){mode=m;invalidate();}void setGrid(double[] g,double[] rg,double b,String m){grid=g.clone();gridRG=rg.clone();baseY=b;mode=m;postInvalidate();}void showFocus(float x,float y){fx=x;fy=y;focusUntil=System.currentTimeMillis()+1200;invalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),h=getHeight();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.argb(210,90,255,150));c.drawRoundRect(new RectF(w*.18f,h*.24f,w*.82f,h*.76f),22,22,p);
            if("ÖLÇÜM HARİTA".equals(mode)||"R/G HARİTA".equals(mode)){float cw=w/(float)GRID_X,ch=h/(float)GRID_Y;p.setStyle(Paint.Style.FILL);for(int y=0;y<GRID_Y;y++)for(int x=0;x<GRID_X;x++){int i=y*GRID_X+x;double v="R/G HARİTA".equals(mode)?Math.min(1,Math.abs(gridRG[i]-1.0)):Math.min(1,Math.max(0,(grid[i]-baseY)/(baseY+.01))/.8);int a=(int)(v*150);int col=v<.33?Color.argb(a,0,120,255):v<.66?Color.argb(a,0,255,120):Color.argb(a,255,80,30);p.setColor(col);c.drawRect(x*cw,y*ch,(x+1)*cw,(y+1)*ch,p);}p.setColor(Color.WHITE);p.setTextSize(28);c.drawText("ÖLÇÜM HARİTASI • FALSE COLOR",20,h-180,p);}
            if(System.currentTimeMillis()<focusUntil&&fx>=0){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(Color.WHITE);c.drawCircle(fx,fy,34,p);c.drawLine(fx-48,fy,fx-18,fy,p);c.drawLine(fx+18,fy,fx+48,fy,p);c.drawLine(fx,fy-48,fx,fy-18,p);c.drawLine(fx,fy+18,fx,fy+48,p);postInvalidateDelayed(80);}
        }
    }
}
