package com.mg.stonefluorescence;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
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

public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 1001;
    private TextureView textureView;
    private TextView modeTitle;
    private TextView status;
    private ModePanel modePanel;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private CaptureRequest.Builder previewBuilder;
    private CameraCharacteristics cameraCharacteristics;
    private Rect activeArray;
    private String currentMode = "NORMAL";
    private double baselineBrightness = -1;
    private double baselineR = -1, baselineG = -1, baselineB = -1;
    private double previousBrightness = -1;
    private double previousR = -1, previousG = -1, previousB = -1;
    private double darkBrightness = 0;
    private boolean darkCaptured = false;
    private boolean exposureLocked = false;
    private long lastUi = 0;

    static class Stat {
        double brightness, saturation, r, g, b, scatter, variation;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCameraFlow();
        else requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
    }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        textureView = new TextureView(this);
        textureView.setOnTouchListener((v,e) -> {
            if (e.getAction() == MotionEvent.ACTION_UP) {
                focusAt(e.getX(),e.getY());
                status.setText("Netlik/pozlama: dokunulan nokta");
            }
            return true;
        });
        root.addView(textureView,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(10),dp(10),dp(10),dp(8));
        top.setBackgroundColor(0xB0000000);
        modeTitle = text("STONE GLOW • NORMAL",18,true,Color.WHITE);
        top.addView(modeTitle);
        status = text("Kamera hazırlanıyor…",12,false,Color.rgb(200,220,235));
        top.addView(status);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        String[] names = {"NORMAL","UV365","UV395","TRANSMİSYON","POLARİZE","KARANLIK ALAN","PLEOKROİZM","ZONLAMA","MAKRO","PARLAKLIK","FINGERPRINT"};
        for (String n: names) {
            Button b = new Button(this);
            b.setText(n);
            b.setTextSize(10);
            b.setAllCaps(false);
            b.setOnClickListener(v -> selectMode(n));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(118),dp(44));
            lp.setMargins(dp(2),dp(3),dp(2),0);
            modes.addView(b,lp);
        }
        hs.addView(modes);
        top.addView(hs,new LinearLayout.LayoutParams(-1,dp(50)));

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        Button dark = new Button(this);
        dark.setText("DARK REF"); dark.setTextSize(10);
        dark.setOnClickListener(v -> captureDarkReference());
        Button lock = new Button(this);
        lock.setText("AE/AWB KİLİT"); lock.setTextSize(10);
        lock.setOnClickListener(v -> toggleExposureLock());
        tools.addView(dark,new LinearLayout.LayoutParams(0,dp(42),1));
        tools.addView(lock,new LinearLayout.LayoutParams(0,dp(42),1));
        top.addView(tools);
        root.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));

        modePanel = new ModePanel(this);
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);
        mp.setMargins(dp(8),0,dp(8),dp(8));
        root.addView(modePanel,mp);
        return root;
    }

    private void selectMode(String mode) {
        currentMode = mode;
        modeTitle.setText("STONE GLOW • " + mode);
        baselineBrightness = -1;
        previousBrightness = -1;
        previousR = previousG = previousB = -1;
        status.setText(protocolFor(mode));
    }

    private String protocolFor(String m) {
        if (m.equals("UV365")) return "365 nm UV + tercihen 420 nm long-pass; görünür floresans ölçülüyor.";
        if (m.equals("UV395")) return "395 nm UV + tercihen long-pass; görünür floresans ölçülüyor.";
        if (m.equals("TRANSMİSYON")) return "Taşın arkasından homojen beyaz ışık ver.";
        if (m.equals("POLARİZE")) return "Çapraz polarizer kullan ve taşı yavaşça döndür.";
        if (m.equals("KARANLIK ALAN")) return "Işığı yandan ver; doğrudan yansımayı kameradan uzak tut.";
        if (m.equals("PLEOKROİZM")) return "Sabit beyaz ışıkta taşı döndür; AE/AWB kilidi önerilir.";
        if (m.equals("ZONLAMA")) return "Taşı sabit tut; bölgesel renk/homojenlik izleniyor.";
        if (m.equals("MAKRO")) return "Yaklaş ve ekrana dokunarak netleştir.";
        if (m.equals("PARLAKLIK")) return "Sabit açılı noktasal beyaz ışık kullan.";
        if (m.equals("FINGERPRINT")) return "Farklı modlardan optik imza oluşturmak için taşı sabit tut.";
        return "Gerçek kamera görüntüsü; bilimsel mod seç.";
    }

    private void captureDarkReference() {
        darkCaptured = true;
        darkBrightness = Math.max(0, previousBrightness);
        status.setText("DARK REF kaydedildi; sensör/ofset tabanı çıkarılacak.");
    }

    private void toggleExposureLock() {
        exposureLocked = !exposureLocked;
        if (previewBuilder == null || captureSession == null) return;
        try {
            previewBuilder.set(CaptureRequest.CONTROL_AE_LOCK, exposureLocked);
            previewBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, exposureLocked);
            captureSession.setRepeatingRequest(previewBuilder.build(),null,cameraHandler);
            status.setText(exposureLocked ? "AE/AWB kilitli" : "AE/AWB otomatik");
        } catch (Exception ignored) {}
    }

    private void startCameraFlow() {
        cameraThread = new HandlerThread("StoneGlowCamera");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){ openCamera(); }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){}
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture s){ return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture s){}
        });
        if (textureView.isAvailable()) openCamera();
    }

    private void openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            String selected = null;
            for (String id: manager.getCameraIdList()) {
                CameraCharacteristics cc = manager.getCameraCharacteristics(id);
                Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) { selected=id; cameraCharacteristics=cc; break; }
            }
            if (selected == null) { selected=manager.getCameraIdList()[0]; cameraCharacteristics=manager.getCameraCharacteristics(selected); }
            activeArray = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            imageReader = ImageReader.newInstance(640,480,android.graphics.ImageFormat.YUV_420_888,3);
            imageReader.setOnImageAvailableListener(r -> {
                Image im = r.acquireLatestImage();
                if (im == null) return;
                try { analyze(im); } finally { im.close(); }
            },cameraHandler);
            manager.openCamera(selected,new CameraDevice.StateCallback(){
                @Override public void onOpened(CameraDevice c){ cameraDevice=c; createSession(); }
                @Override public void onDisconnected(CameraDevice c){ c.close(); }
                @Override public void onError(CameraDevice c,int e){ c.close(); runOnUiThread(() -> status.setText("Kamera hatası: "+e)); }
            },cameraHandler);
        } catch(Exception e){ status.setText("Kamera açılamadı: "+e.getMessage()); }
    }

    private void createSession() {
        try {
            SurfaceTexture st = textureView.getSurfaceTexture();
            if (st == null) return;
            st.setDefaultBufferSize(1280,720);
            Surface preview = new Surface(st);
            Surface analysis = imageReader.getSurface();
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(preview);
            previewBuilder.addTarget(analysis);
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
            previewBuilder.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_AUTO);
            cameraDevice.createCaptureSession(Arrays.asList(preview,analysis),new CameraCaptureSession.StateCallback(){
                @Override public void onConfigured(CameraCaptureSession s){
                    captureSession=s;
                    try { s.setRepeatingRequest(previewBuilder.build(),null,cameraHandler); runOnUiThread(() -> status.setText(protocolFor(currentMode))); }
                    catch(Exception e){ runOnUiThread(() -> status.setText("Akış başlatılamadı")); }
                }
                @Override public void onConfigureFailed(CameraCaptureSession s){ runOnUiThread(() -> status.setText("Kamera oturumu kurulamadı")); }
            },cameraHandler);
        } catch(Exception e){ status.setText("Kamera yapılandırma hatası"); }
    }

    private void focusAt(float x,float y) {
        if (captureSession == null || previewBuilder == null) return;
        try {
            if (activeArray != null) {
                int sx = activeArray.left + (int)((x/Math.max(1f,textureView.getWidth()))*activeArray.width());
                int sy = activeArray.top + (int)((y/Math.max(1f,textureView.getHeight()))*activeArray.height());
                int half = Math.max(100,Math.min(activeArray.width(),activeArray.height())/18);
                Rect r = new Rect(Math.max(activeArray.left,sx-half),Math.max(activeArray.top,sy-half),Math.min(activeArray.right,sx+half),Math.min(activeArray.bottom,sy+half));
                MeteringRectangle mr = new MeteringRectangle(r,MeteringRectangle.METERING_WEIGHT_MAX);
                Integer af = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
                Integer ae = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
                if (af != null && af>0) previewBuilder.set(CaptureRequest.CONTROL_AF_REGIONS,new MeteringRectangle[]{mr});
                if (ae != null && ae>0) previewBuilder.set(CaptureRequest.CONTROL_AE_REGIONS,new MeteringRectangle[]{mr});
            }
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_AUTO);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
            captureSession.capture(previewBuilder.build(),null,cameraHandler);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureSession.setRepeatingRequest(previewBuilder.build(),null,cameraHandler);
        } catch(Exception ignored){}
    }

    private void analyze(Image image) {
        Stat s = stat(image);
        if (s == null) return;
        if (darkCaptured) s.brightness = Math.max(0,s.brightness-darkBrightness);
        if (baselineBrightness < 0) {
            baselineBrightness=s.brightness; baselineR=s.r; baselineG=s.g; baselineB=s.b;
            previousBrightness=s.brightness; previousR=s.r; previousG=s.g; previousB=s.b;
            return;
        }
        double temporal = Math.abs(s.brightness-previousBrightness)+Math.abs(s.r-previousR)+Math.abs(s.g-previousG)+Math.abs(s.b-previousB);
        double baselineDelta = Math.abs(s.brightness-baselineBrightness)+Math.abs(s.r-baselineR)+Math.abs(s.g-baselineG)+Math.abs(s.b-baselineB);
        s.variation = Math.min(1.0,0.55*baselineDelta + 0.45*temporal);
        previousBrightness=s.brightness; previousR=s.r; previousG=s.g; previousB=s.b;
        long now=System.currentTimeMillis();
        if (now-lastUi>120) {
            lastUi=now;
            Stat ui=s;
            runOnUiThread(() -> modePanel.render(currentMode,ui.brightness,ui.r,ui.g,ui.b,ui.saturation,ui.scatter,ui.variation));
        }
    }

    private Stat stat(Image image) {
        if (image.getPlanes().length<3) return null;
        Image.Plane yp=image.getPlanes()[0], up=image.getPlanes()[1], vp=image.getPlanes()[2];
        ByteBuffer yb=yp.getBuffer(), ub=up.getBuffer(), vb=vp.getBuffer();
        int w=image.getWidth(), h=image.getHeight();
        int yRow=yp.getRowStride(), yPix=yp.getPixelStride();
        int uRow=up.getRowStride(), uPix=up.getPixelStride();
        int vRow=vp.getRowStride(), vPix=vp.getPixelStride();
        int x0=(int)(w*0.15),x1=(int)(w*0.85),y0=(int)(h*0.15),y1=(int)(h*0.85);
        double sr=0,sg=0,sb=0,sv=0,ss=0,sdev=0;
        int n=0;
        for(int yy=y0;yy<y1;yy+=8){
            for(int xx=x0;xx<x1;xx+=8){
                int yi=yy*yRow+xx*yPix, ui=(yy/2)*uRow+(xx/2)*uPix, vi=(yy/2)*vRow+(xx/2)*vPix;
                if(yi>=yb.limit()||ui>=ub.limit()||vi>=vb.limit()) continue;
                int Y=yb.get(yi)&0xff, U=(ub.get(ui)&0xff)-128, V=(vb.get(vi)&0xff)-128;
                int r=clamp255((int)(Y+1.402*V));
                int g=clamp255((int)(Y-0.344136*U-0.714136*V));
                int b=clamp255((int)(Y+1.772*U));
                int max=Math.max(r,Math.max(g,b)), min=Math.min(r,Math.min(g,b));
                double val=max/255.0, sat=max==0?0:(max-min)/(double)max;
                sr+=r/255.0; sg+=g/255.0; sb+=b/255.0; sv+=val; ss+=sat;
                sdev+=Math.abs(Y-128)/128.0;
                n++;
            }
        }
        if(n==0) return null;
        Stat s=new Stat();
        s.r=sr/n; s.g=sg/n; s.b=sb/n; s.brightness=sv/n; s.saturation=ss/n;
        s.scatter=Math.min(1.0,sdev/n);
        return s;
    }

    private int clamp255(int x){ return Math.max(0,Math.min(255,x)); }
    private int dp(int x){ return (int)(x*getResources().getDisplayMetrics().density+0.5f); }
    private TextView text(String s,int sp,boolean bold,int color){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD); return t; }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==REQ_CAMERA && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED) startCameraFlow();
    }

    @Override protected void onDestroy(){
        super.onDestroy();
        try{ if(captureSession!=null)captureSession.close(); }catch(Exception ignored){}
        try{ if(cameraDevice!=null)cameraDevice.close(); }catch(Exception ignored){}
        try{ if(imageReader!=null)imageReader.close(); }catch(Exception ignored){}
        if(cameraThread!=null)cameraThread.quitSafely();
    }
}
