package com.mg.stonefluorescence;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
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
import android.widget.ScrollView;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 1001;
    private static final String PREFS = "stone_glow_v15";

    private TextureView textureView;
    private ScienceOverlay overlay;
    private TextView statusText, detailText, modeText, candidateText;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private CaptureRequest.Builder previewBuilder;
    private CameraCharacteristics characteristics;
    private Rect activeArray;

    private String mode = "CANLI";
    private boolean locked = false;
    private boolean darkCaptured = false;
    private double darkR = 0, darkG = 0, darkB = 0, darkY = 0;
    private double baseR = -1, baseG = -1, baseB = -1, baseY = -1, baseSat = -1;
    private int warmup = 0;
    private long lastUi = 0;
    private double peakY = 0;
    private long peakTime = 0;
    private double pleoMinHue = 999, pleoMaxHue = -1;
    private final double[] zoneHistory = new double[24];
    private int zoneIndex = 0;
    private Stat lastStat;

    static class Stat {
        double r, g, b, y, sat, hue;
        double brightFraction, darkFraction, spatialStd, edgeDensity;
        double centerY, outerY;
        double rg, gb, rb;
    }

    static class Candidate {
        String name;
        double score;
        String reason;
        Candidate(String n, double s, String r) { name=n; score=s; reason=r; }
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
        textureView.setOnTouchListener((v,e)->{
            if (e.getAction()==MotionEvent.ACTION_UP) {
                focusAt(e.getX(), e.getY());
                overlay.showFocus(e.getX(), e.getY());
            }
            return true;
        });
        root.addView(textureView, new FrameLayout.LayoutParams(-1,-1));

        overlay = new ScienceOverlay(this);
        root.addView(overlay, new FrameLayout.LayoutParams(-1,-1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(12),dp(12),dp(12),dp(8));
        top.setBackgroundColor(0xB0000000);
        top.addView(label("STONE GLOW • MINERAL LAB v1.5",19,true,Color.WHITE));
        modeText = label("MOD: CANLI",12,true,Color.rgb(160,220,255));
        top.addView(modeText);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] modes = {"CANLI","UV365","UV395","AFTERGLOW","POLAR","TRANSMİSYON","KARANLIK ALAN","MAKRO","PLEOKROİZM","ZONLAMA","PARLAKLIK","FINGERPRINT"};
        for (String m : modes) {
            Button b = new Button(this);
            b.setText(m); b.setTextSize(10); b.setAllCaps(false);
            b.setOnClickListener(v->setMode(m));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(110),dp(43));
            lp.setMargins(dp(2),dp(3),dp(2),0);
            row.addView(b,lp);
        }
        hs.addView(row);
        top.addView(hs,new LinearLayout.LayoutParams(-1,dp(48)));

        LinearLayout calRow = new LinearLayout(this);
        calRow.setOrientation(LinearLayout.HORIZONTAL);
        Button dark = new Button(this); dark.setText("DARK REF"); dark.setTextSize(11); dark.setOnClickListener(v->captureDark());
        Button cal = new Button(this); cal.setText("KALİBRE + KİLİT"); cal.setTextSize(11); cal.setOnClickListener(v->calibrateAndLock());
        Button save = new Button(this); save.setText("FP KAYDET"); save.setTextSize(11); save.setOnClickListener(v->saveFingerprint());
        calRow.addView(dark,new LinearLayout.LayoutParams(0,dp(45),1));
        calRow.addView(cal,new LinearLayout.LayoutParams(0,dp(45),1));
        calRow.addView(save,new LinearLayout.LayoutParams(0,dp(45),1));
        top.addView(calRow);
        root.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));

        ScrollView bottomScroll = new ScrollView(this);
        bottomScroll.setFillViewport(false);
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(dp(12),dp(9),dp(12),dp(14));
        bottom.setBackgroundColor(0xC0000000);
        statusText = label("Kamera hazırlanıyor…",18,true,Color.WHITE);
        detailText = label("Bilimsel ölçüm: gerçek kamera sinyali; sahte UV renk filtresi kullanılmaz.",12,false,Color.rgb(225,230,235));
        candidateText = label("Mineral adayları ölçümden sonra burada görünür.",12,true,Color.rgb(170,235,190));
        bottom.addView(statusText); bottom.addView(detailText); bottom.addView(candidateText);
        bottomScroll.addView(bottom);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1,dp(190),Gravity.BOTTOM);
        root.addView(bottomScroll,bp);
        return root;
    }

    private TextView label(String s,int sp,boolean bold,int color) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(null,android.graphics.Typeface.BOLD); return t;
    }

    private void setMode(String m) {
        mode=m; modeText.setText("MOD: "+m); peakY=0; peakTime=0; pleoMinHue=999; pleoMaxHue=-1;
        String help;
        switch(m) {
            case "UV365": help="365 nm UV ile uyar. Kameranın önünde ~420 nm long-pass filtre önerilir. Ölçülen görünür floresans analiz edilir."; break;
            case "UV395": help="395 nm UV ile uyar. 365 nm sonucuyla karşılaştır; emisyon rengi ve göreli şiddet farkı kaydedilir."; break;
            case "AFTERGLOW": help="UV ışığı birkaç saniye tut, sonra kapat. Uygulama görünür ışımanın sönümünü izler."; break;
            case "POLAR": help="Kamera önüne polarizer, ışık önüne ikinci polarizer koy ve çaprazla. Yazılım gerçek renk/şiddet değişimini ölçer."; break;
            case "TRANSMİSYON": help="Taşı arkadan homojen beyaz ışıkla aydınlat. Geçirgenlik, zon ve inklüzyon kontrastı ölçülür."; break;
            case "KARANLIK ALAN": help="Arka plan karanlık kalsın, taşı yandan aydınlat. Saçılma, çatlak ve inklüzyon sinyali ölçülür."; break;
            case "MAKRO": help="Taşa yaklaş ve dokunarak netleştir. Kenar yoğunluğu, lokal kontrast ve inklüzyon ipuçları ölçülür."; break;
            case "PLEOKROİZM": help="Taşı sabit ışıkta yavaşça döndür. Hue aralığı ve kanal oranı değişimi kaydedilir."; break;
            case "ZONLAMA": help="Taşı sabit tut. Merkez/çevre parlaklık ve renk farkları ile renk zonlanması aranır."; break;
            case "PARLAKLIK": help="Dar açılı beyaz ışık kullan. Parlak piksel oranı + saçılma ile yüzey parlaklığı sınıfı tahmin edilir."; break;
            case "FINGERPRINT": help="UV, transmisyon, polar ve normal ölçümleri birleştir. Kaydedilmiş parmak iziyle karşılaştırma yapılır."; break;
            default: help="Gerçek kamera görüntüsü ve ham optik özellikler. Netlik için taşa dokun.";
        }
        detailText.setText(help);
        overlay.setMode(m);
    }

    private void startCameraFlow() {
        if (cameraThread==null) { cameraThread=new HandlerThread("MineralLabCamera"); cameraThread.start(); cameraHandler=new Handler(cameraThread.getLooper()); }
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){openCamera();}
            public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){}
            public boolean onSurfaceTextureDestroyed(SurfaceTexture s){return true;}
            public void onSurfaceTextureUpdated(SurfaceTexture s){}
        });
        if (textureView.isAvailable()) openCamera();
    }

    private void openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) return;
        CameraManager mgr=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            String id=null;
            for(String x:mgr.getCameraIdList()) {
                CameraCharacteristics c=mgr.getCameraCharacteristics(x);
                Integer f=c.get(CameraCharacteristics.LENS_FACING);
                if(f!=null&&f==CameraCharacteristics.LENS_FACING_BACK){id=x;characteristics=c;break;}
            }
            if(id==null){String[] ids=mgr.getCameraIdList(); if(ids.length==0)return; id=ids[0]; characteristics=mgr.getCameraCharacteristics(id);}
            activeArray=characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            imageReader=ImageReader.newInstance(640,480,android.graphics.ImageFormat.YUV_420_888,3);
            imageReader.setOnImageAvailableListener(r->{Image im=r.acquireLatestImage(); if(im!=null){try{analyze(im);}finally{im.close();}}},cameraHandler);
            mgr.openCamera(id,new CameraDevice.StateCallback(){
                public void onOpened(CameraDevice c){cameraDevice=c;createSession();}
                public void onDisconnected(CameraDevice c){c.close();cameraDevice=null;}
                public void onError(CameraDevice c,int e){c.close();cameraDevice=null;runOnUiThread(()->statusText.setText("Kamera hatası"));}
            },cameraHandler);
        } catch(Exception e){runOnUiThread(()->detailText.setText("Kamera başlatılamadı: "+e.getMessage()));}
    }

    private void createSession() {
        if(cameraDevice==null||!textureView.isAvailable()||imageReader==null)return;
        try {
            SurfaceTexture st=textureView.getSurfaceTexture(); if(st==null)return; st.setDefaultBufferSize(1280,720);
            Surface preview=new Surface(st), analysis=imageReader.getSurface();
            previewBuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(preview); previewBuilder.addTarget(analysis);
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
            previewBuilder.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_AUTO);
            cameraDevice.createCaptureSession(Arrays.asList(preview,analysis),new CameraCaptureSession.StateCallback(){
                public void onConfigured(CameraCaptureSession s){captureSession=s;try{s.setRepeatingRequest(previewBuilder.build(),null,cameraHandler);runOnUiThread(()->statusText.setText("CANLI ÖLÇÜM • KALİBRASYON"));}catch(Exception ignored){}}
                public void onConfigureFailed(CameraCaptureSession s){runOnUiThread(()->statusText.setText("Kamera oturumu kurulamadı"));}
            },cameraHandler);
        }catch(Exception e){runOnUiThread(()->detailText.setText("Kamera yapılandırma hatası"));}
    }

    private void focusAt(float x,float y) {
        if(captureSession==null||previewBuilder==null)return;
        try {
            if(activeArray!=null){
                int sx=activeArray.left+(int)(x/Math.max(1f,textureView.getWidth())*activeArray.width());
                int sy=activeArray.top +(int)(y/Math.max(1f,textureView.getHeight())*activeArray.height());
                int h=Math.max(80,Math.min(activeArray.width(),activeArray.height())/18);
                Rect r=new Rect(Math.max(activeArray.left,sx-h),Math.max(activeArray.top,sy-h),Math.min(activeArray.right,sx+h),Math.min(activeArray.bottom,sy+h));
                MeteringRectangle mr=new MeteringRectangle(r,MeteringRectangle.METERING_WEIGHT_MAX);
                Integer af=characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF), ae=characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
                if(af!=null&&af>0)previewBuilder.set(CaptureRequest.CONTROL_AF_REGIONS,new MeteringRectangle[]{mr});
                if(ae!=null&&ae>0)previewBuilder.set(CaptureRequest.CONTROL_AE_REGIONS,new MeteringRectangle[]{mr});
            }
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_AUTO);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
            captureSession.capture(previewBuilder.build(),null,cameraHandler);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureSession.setRepeatingRequest(previewBuilder.build(),null,cameraHandler);
        }catch(Exception ignored){}
    }

    private void captureDark() {
        if(lastStat==null){detailText.setText("Önce kamera ölçümü oluşmalı.");return;}
        darkR=lastStat.r; darkG=lastStat.g; darkB=lastStat.b; darkY=lastStat.y; darkCaptured=true;
        detailText.setText("DARK REF alındı. Doğru sonuç için lens kapalıyken alınmalıdır.");
    }

    private void calibrateAndLock() {
        if(lastStat==null||captureSession==null||previewBuilder==null){detailText.setText("Kalibrasyon için canlı görüntü gerekli.");return;}
        baseR=lastStat.r; baseG=lastStat.g; baseB=lastStat.b; baseY=lastStat.y; baseSat=lastStat.sat; warmup=30;
        try {
            Boolean aeLock=characteristics.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE);
            if(Boolean.TRUE.equals(aeLock))previewBuilder.set(CaptureRequest.CONTROL_AE_LOCK,true);
            Boolean awbLock=characteristics.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE);
            if(Boolean.TRUE.equals(awbLock))previewBuilder.set(CaptureRequest.CONTROL_AWB_LOCK,true);
            captureSession.setRepeatingRequest(previewBuilder.build(),null,cameraHandler); locked=true;
            detailText.setText("Referans kaydedildi; destekleniyorsa pozlama ve beyaz dengesi kilitlendi.");
        }catch(Exception e){detailText.setText("Referans alındı; kamera kilidi bu cihazda sınırlı olabilir.");}
    }

    private void analyze(Image image) {
        Stat s=statFromYuv(image); if(s==null)return; lastStat=s;
        if(baseY<0){baseR=s.r;baseG=s.g;baseB=s.b;baseY=s.y;baseSat=s.sat;warmup=1;return;}
        if(warmup<20&&!locked){baseR=ema(baseR,s.r,.15);baseG=ema(baseG,s.g,.15);baseB=ema(baseB,s.b,.15);baseY=ema(baseY,s.y,.15);baseSat=ema(baseSat,s.sat,.15);warmup++;return;}

        double rr=Math.max(0,s.r-(darkCaptured?darkR:0));
        double gg=Math.max(0,s.g-(darkCaptured?darkG:0));
        double bb=Math.max(0,s.b-(darkCaptured?darkB:0));
        double yy=Math.max(0,s.y-(darkCaptured?darkY:0));
        double relY=(yy+0.01)/(Math.max(0.01,baseY-(darkCaptured?darkY:0))+0.01);
        double colorShift=Math.sqrt(sq(rr/(gg+.01)-baseR/(baseG+.01))+sq(bb/(gg+.01)-baseB/(baseG+.01)));
        double fluorescence=clamp((relY-1.03)/1.2)*0.55+clamp((s.sat-baseSat)/0.45)*0.25+clamp(colorShift/1.0)*0.20;
        double anomaly=100*clamp(fluorescence);

        long now=System.currentTimeMillis();
        if("AFTERGLOW".equals(mode)) {
            if(yy>peakY){peakY=yy;peakTime=now;}
        }
        if("PLEOKROİZM".equals(mode)) {
            pleoMinHue=Math.min(pleoMinHue,s.hue); pleoMaxHue=Math.max(pleoMaxHue,s.hue);
        }
        zoneHistory[zoneIndex++%zoneHistory.length]=Math.abs(s.centerY-s.outerY);

        if(now-lastUi>180){lastUi=now;
            String metrics=metricsForMode(s,rr,gg,bb,yy,relY,fluorescence,now);
            List<Candidate> cands=candidates(s,fluorescence);
            String candText=formatCandidates(cands);
            double fpSimilarity=fingerprintSimilarity(s,fluorescence);
            runOnUiThread(()->{
                statusText.setText(String.format(Locale.US,"ÖLÇÜM %.0f/100 • %s",anomaly,mode));
                statusText.setTextColor(anomaly>60?Color.rgb(255,130,100):anomaly>30?Color.rgb(255,220,120):Color.rgb(150,235,185));
                detailText.setText(metrics+(fpSimilarity>=0?String.format(Locale.US,"\nKayıtlı fingerprint benzerliği: %.0f%%",fpSimilarity*100):""));
                candidateText.setText(candText);
                overlay.setScore((float)anomaly);
            });
        }
        if(!locked&&anomaly<15){baseR=ema(baseR,s.r,.008);baseG=ema(baseG,s.g,.008);baseB=ema(baseB,s.b,.008);baseY=ema(baseY,s.y,.008);baseSat=ema(baseSat,s.sat,.008);}
    }

    private String metricsForMode(Stat s,double r,double g,double b,double y,double relY,double fl,long now) {
        String common=String.format(Locale.US,"R/G %.2f • G/B %.2f • R/B %.2f • Y %.3f • Sat %.2f",r/(g+.01),g/(b+.01),r/(b+.01),y,s.sat);
        if("AFTERGLOW".equals(mode)){
            double dt=peakTime==0?0:(now-peakTime)/1000.0; double remain=peakY<=0?0:y/(peakY+.001);
            return common+String.format(Locale.US,"\nAfterglow: tepe sonrası %.1f s • kalan sinyal %.0f%%",dt,clamp(remain)*100);
        }
        if("POLAR".equals(mode)) return common+String.format(Locale.US,"\nPolar değişim ipucu: renk açısı %.1f° • uzaysal std %.3f",s.hue,s.spatialStd);
        if("TRANSMİSYON".equals(mode)) return common+String.format(Locale.US,"\nGöreli geçirgenlik %.0f%% • merkez/çevre oranı %.2f",clamp(relY/1.8)*100,s.centerY/(s.outerY+.01));
        if("KARANLIK ALAN".equals(mode)) return common+String.format(Locale.US,"\nSaçılma/inklüzyon ipucu: parlak piksel %.1f%% • kenar %.1f%%",s.brightFraction*100,s.edgeDensity*100);
        if("MAKRO".equals(mode)) return common+String.format(Locale.US,"\nMikroyapı: kenar yoğunluğu %.1f%% • lokal değişkenlik %.3f",s.edgeDensity*100,s.spatialStd);
        if("PLEOKROİZM".equals(mode)) {double range=(pleoMaxHue>=0&&pleoMinHue<999)?Math.abs(pleoMaxHue-pleoMinHue):0; return common+String.format(Locale.US,"\nDöndürme boyunca hue aralığı %.1f°",range);}
        if("ZONLAMA".equals(mode)) return common+String.format(Locale.US,"\nZon farkı merkez-çevre %.3f • renk hue %.1f°",Math.abs(s.centerY-s.outerY),s.hue);
        if("PARLAKLIK".equals(mode)) return common+"\n"+lusterClass(s);
        if("FINGERPRINT".equals(mode)) return common+String.format(Locale.US,"\nFingerprint: F %.2f • Edge %.2f • Std %.3f",fl,s.edgeDensity,s.spatialStd);
        return common+String.format(Locale.US,"\nFloresans/anomali bileşik indeksi %.2f",fl);
    }

    private String lusterClass(Stat s){
        double p=s.brightFraction, e=s.edgeDensity;
        if(p>0.14&&e>0.16)return "Parlaklık adayı: yüksek speküler / adamantin-metallic benzeri";
        if(p>0.07)return "Parlaklık adayı: camsı / vitreous benzeri";
        if(s.spatialStd<0.08)return "Parlaklık adayı: mat-düşük yansımalı";
        return "Parlaklık adayı: orta / reçinemsi-sedefimsi olabilir";
    }

    private List<Candidate> candidates(Stat s,double fl) {
        List<Candidate> a=new ArrayList<>();
        double trans=clamp((s.y-.12)/.55), pleo=(pleoMaxHue>=0&&pleoMinHue<999)?clamp(Math.abs(pleoMaxHue-pleoMinHue)/90.0):0;
        add(a,"Florit",.38*fl+.18*s.sat+.12*trans+.12*(1-s.edgeDensity),"UV floresansı ve renk doygunluğu ile uyum");
        add(a,"Kalsit",.30*fl+.20*trans+.10*(1-s.sat)+.12*s.spatialStd,"floresans/transmisyon ve iç yapı ile kısmi uyum");
        add(a,"Korindon (safir/rubi)",.18*fl+.25*s.sat+.20*trans+.14*pleo,"doygun renk, geçirgenlik ve pleokroizm ile uyum");
        add(a,"Turmalin",.12*fl+.28*s.sat+.18*pleo+.12*(1-trans),"güçlü renk ve pleokroizm ihtimali");
        add(a,"Kuvars",.08*fl+.18*trans+.16*(1-s.sat)+.16*(1-s.edgeDensity),"zayıf floresans ve camsı/şeffaf davranışla uyum");
        add(a,"Feldspat",.12*fl+.14*s.spatialStd+.13*(1-trans)+.10*s.edgeDensity,"zon/doku ve orta optik tepki ile uyum");
        add(a,"Opal",.16*fl+.18*s.spatialStd+.16*(1-s.edgeDensity)+.10*s.sat,"homojen/opalize saçılma ve olası floresans");
        add(a,"Apatit",.18*fl+.16*s.sat+.14*trans+.08*pleo,"floresans, renk ve geçirgenlikle kısmi uyum");
        Collections.sort(a,(x,y)->Double.compare(y.score,x.score)); return a;
    }

    private void add(List<Candidate>a,String n,double s,String r){a.add(new Candidate(n,clamp(s),r));}
    private String formatCandidates(List<Candidate> a){
        StringBuilder sb=new StringBuilder("Mineral adayları (kesin teşhis değildir): ");
        for(int i=0;i<Math.min(3,a.size());i++){Candidate c=a.get(i); if(i>0)sb.append(" • "); sb.append(c.name).append(String.format(Locale.US," %.0f%%",c.score*100));}
        sb.append("\nKamera tek başına element/atom tayini yapamaz; Raman/XRF/NIR gibi araçlarla doğrulama gerekir."); return sb.toString();
    }

    private void saveFingerprint(){
        if(lastStat==null){detailText.setText("Fingerprint için canlı ölçüm gerekli.");return;}
        SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
        p.edit().putFloat("r",(float)lastStat.r).putFloat("g",(float)lastStat.g).putFloat("b",(float)lastStat.b)
                .putFloat("sat",(float)lastStat.sat).putFloat("edge",(float)lastStat.edgeDensity).putFloat("std",(float)lastStat.spatialStd).apply();
        detailText.setText("Mineral Fingerprint kaydedildi. Sonraki taşlar bu optik imza ile karşılaştırılabilir.");
    }

    private double fingerprintSimilarity(Stat s,double fl){
        SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE); if(!p.contains("r"))return -1;
        double d=0; d+=sq((s.r-p.getFloat("r",0))/0.35); d+=sq((s.g-p.getFloat("g",0))/0.35); d+=sq((s.b-p.getFloat("b",0))/0.35);
        d+=sq((s.sat-p.getFloat("sat",0))/0.5); d+=sq((s.edgeDensity-p.getFloat("edge",0))/0.3); d+=sq((s.spatialStd-p.getFloat("std",0))/0.25);
        return Math.exp(-Math.sqrt(d)/2.2);
    }

    private Stat statFromYuv(Image im){
        if(im.getPlanes().length<3)return null;
        Image.Plane yp=im.getPlanes()[0],up=im.getPlanes()[1],vp=im.getPlanes()[2];
        ByteBuffer yb=yp.getBuffer(),ub=up.getBuffer(),vb=vp.getBuffer();
        int w=im.getWidth(),h=im.getHeight(),step=5;
        int x0=(int)(w*.12),x1=(int)(w*.88),y0=(int)(h*.12),y1=(int)(h*.88);
        int yr=yp.getRowStride(),yx=yp.getPixelStride(),ur=up.getRowStride(),ux=up.getPixelStride(),vr=vp.getRowStride(),vx=vp.getPixelStride();
        double sr=0,sg=0,sb=0,sy=0,ss=0,sHueX=0,sHueY=0,sum2=0,center=0,outer=0; int n=0,bright=0,dark=0,edges=0,nc=0,no=0; int prevY=-1;
        for(int yy=y0;yy<y1;yy+=step){for(int xx=x0;xx<x1;xx+=step){
            int yi=yy*yr+xx*yx,ui=(yy/2)*ur+(xx/2)*ux,vi=(yy/2)*vr+(xx/2)*vx; if(yi>=yb.limit()||ui>=ub.limit()||vi>=vb.limit())continue;
            int Y=yb.get(yi)&255,U=(ub.get(ui)&255)-128,V=(vb.get(vi)&255)-128;
            int R=c255((int)(Y+1.402*V)),G=c255((int)(Y-.344136*U-.714136*V)),B=c255((int)(Y+1.772*U));
            double rn=R/255.0,gn=G/255.0,bn=B/255.0,yn=Y/255.0; int mx=Math.max(R,Math.max(G,B)),mn=Math.min(R,Math.min(G,B)); double sat=mx==0?0:(mx-mn)/(double)mx;
            float[] hsv=new float[3]; Color.RGBToHSV(R,G,B,hsv); double rad=Math.toRadians(hsv[0]);
            sr+=rn;sg+=gn;sb+=bn;sy+=yn;ss+=sat;sHueX+=Math.cos(rad)*sat;sHueY+=Math.sin(rad)*sat;sum2+=yn*yn;
            if(yn>.82)bright++; if(yn<.12)dark++; if(prevY>=0&&Math.abs(Y-prevY)>28)edges++; prevY=Y;
            boolean c=xx>w*.32&&xx<w*.68&&yy>h*.32&&yy<h*.68; if(c){center+=yn;nc++;}else{outer+=yn;no++;} n++;
        }}
        if(n==0)return null; Stat s=new Stat(); s.r=sr/n;s.g=sg/n;s.b=sb/n;s.y=sy/n;s.sat=ss/n; s.hue=Math.toDegrees(Math.atan2(sHueY,sHueX));if(s.hue<0)s.hue+=360;
        s.brightFraction=bright/(double)n;s.darkFraction=dark/(double)n;s.edgeDensity=edges/(double)Math.max(1,n);s.spatialStd=Math.sqrt(Math.max(0,sum2/n-s.y*s.y));s.centerY=center/Math.max(1,nc);s.outerY=outer/Math.max(1,no);
        s.rg=s.r/(s.g+.01);s.gb=s.g/(s.b+.01);s.rb=s.r/(s.b+.01);return s;
    }

    private double ema(double a,double b,double k){return a*(1-k)+b*k;}
    private double clamp(double x){return Math.max(0,Math.min(1,x));}
    private double sq(double x){return x*x;}
    private int c255(int x){return Math.max(0,Math.min(255,x));}
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}

    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_CAMERA&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startCameraFlow();}
    @Override protected void onPause(){super.onPause();closeCamera();}
    @Override protected void onResume(){super.onResume();if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED&&cameraDevice==null&&textureView!=null)startCameraFlow();}
    private void closeCamera(){try{if(captureSession!=null)captureSession.close();}catch(Exception ignored){}captureSession=null;try{if(cameraDevice!=null)cameraDevice.close();}catch(Exception ignored){}cameraDevice=null;try{if(imageReader!=null)imageReader.close();}catch(Exception ignored){}imageReader=null;if(cameraThread!=null){cameraThread.quitSafely();cameraThread=null;cameraHandler=null;}}

    static class ScienceOverlay extends View {
        Paint p=new Paint(1); String mode="CANLI"; float score=0,fx=-1,fy=-1; long focusUntil=0;
        ScienceOverlay(Context c){super(c);setWillNotDraw(false);}
        void setMode(String m){mode=m;invalidate();}
        void setScore(float s){score=s;invalidate();}
        void showFocus(float x,float y){fx=x;fy=y;focusUntil=System.currentTimeMillis()+900;invalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();float l=w*.18f,r=w*.82f,t=h*.25f,b=h*.72f;
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(score>60?Color.rgb(255,120,90):score>30?Color.rgb(255,215,90):Color.rgb(80,230,150));c.drawRoundRect(new RectF(l,t,r,b),22,22,p);
            p.setStyle(Paint.Style.FILL);p.setTextSize(28);p.setColor(Color.WHITE);c.drawText("ROI • "+mode,l,t-18,p);
            if(System.currentTimeMillis()<focusUntil&&fx>=0){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.WHITE);c.drawCircle(fx,fy,42,p);postInvalidateDelayed(80);}
        }
    }
}
