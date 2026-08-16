package com.muhammetgecgil.sesgoruntuharitasi;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/** V8.9: robust source finder + ultra-fast black circular Fyvadio tracker. */
public final class MainActivity extends Activity {
    private static final int REQ=89;
    private static final float PARK_X=.14f,PARK_Y=.70f,PARK_R=.11f;
    private TextureView cameraView;
    private HeatmapOverlayView overlay;
    private TextView usbText,refText,deltaText,statusText,imuText;
    private ProbeAudioEngine audio;
    private CameraController camera;
    private ImuEngine imu;
    private final ProbeVisionTracker vision=new ProbeVisionTracker();
    private final Handler handler=new Handler(Looper.getMainLooper());

    private float probeX=.14f,probeY=.70f,probeR=.08f,visionConf=0f;
    private boolean visionValid=false,probeFrozen=true,probeMoving=false,parked=false,scanning=false,finished=false;
    private Button bandButton,scanButton;
    private final ArrayDeque<Float> refHistory=new ArrayDeque<>();
    private final ArrayDeque<Float> stationaryUsbHistory=new ArrayDeque<>();
    private final ArrayDeque<Float> mapMedianHistory=new ArrayDeque<>();
    private boolean variableSource=false,variableUncertain=false;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);requestWindowFeature(Window.FEATURE_NO_TITLE);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.rgb(4,12,18));getWindow().setNavigationBarColor(Color.rgb(4,12,18));
        buildUi();if(hasPermissions())startAll();else requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},REQ);
    }

    private void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
        cameraView=new TextureView(this);root.addView(cameraView,new FrameLayout.LayoutParams(-1,-1));
        overlay=new HeatmapOverlayView(this);root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));overlay.setPark(PARK_X,PARK_Y,PARK_R);
        overlay.setTargetListener((x,y)->{probeX=x;probeY=y;vision.seed(x,y);visionValid=true;probeFrozen=false;probeMoving=false;});

        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(10),dp(5),dp(10),dp(6));top.setBackgroundColor(Color.argb(215,4,12,18));root.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        TextView title=label("SES GÖRÜNTÜ HARİTASI V8.9 • BLACK CIRCLE FAST TRACK",15,true);title.setTextColor(Color.WHITE);top.addView(title);
        statusText=label("Siyah dairesel Fyvadio probu • çok hızlı takip • kayıpta merkez donar",10,false);statusText.setTextColor(Color.CYAN);top.addView(statusText);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);top.addView(row);
        usbText=metric("USB PROBE\n-- dBFS");refText=metric("PHONE REF\n-- dBFS");deltaText=metric("HARİTA\n-- dB");imuText=metric("IMU\n--");row.addView(usbText,weight());row.addView(refText,weight());row.addView(deltaText,weight());row.addView(imuText,weight());

        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(dp(8),dp(5),dp(8),dp(8));bottom.setBackgroundColor(Color.argb(230,4,12,18));
        FrameLayout.LayoutParams blp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);blp.bottomMargin=dp(72);root.addView(bottom,blp);
        root.setOnApplyWindowInsetsListener((v,insets)->{int nav=insets.getSystemWindowInsetBottom();FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)bottom.getLayoutParams();lp.bottomMargin=Math.max(dp(18),nav+dp(16));bottom.setLayoutParams(lp);return insets;});root.requestApplyInsets();
        TextView help=label("MOR=DÜŞÜK • MAVİ • CAMGÖBEĞİ • YEŞİL • SARI • TURUNCU • KIRMIZI=YÜKSEK",10,false);help.setTextColor(Color.WHITE);bottom.addView(help);
        LinearLayout r1=new LinearLayout(this);r1.setOrientation(LinearLayout.HORIZONTAL);bottom.addView(r1);
        Button mode=button("AUTO KAYNAK");bandButton=button("BAND: TÜM");Button cal=button("REF KALİBRE");Button clear=button("HARİTA SİL");r1.addView(mode,weight());r1.addView(bandButton,weight());r1.addView(cal,weight());r1.addView(clear,weight());
        LinearLayout r2=new LinearLayout(this);r2.setOrientation(LinearLayout.HORIZONTAL);bottom.addView(r2);
        scanButton=button("TARAMAYI BAŞLAT");Button auto=button("PROB: SİYAH DAİRE");Button reset=button("YENİ TARAMA");r2.addView(scanButton,weight());r2.addView(auto,weight());r2.addView(reset,weight());

        mode.setOnClickListener(v->Toast.makeText(this,"AUTO: sabit kaynakta prob seviyesi; değişken kaynakta canlı S24 referansı varsa USB−REF kullanılır",Toast.LENGTH_LONG).show());
        bandButton.setOnClickListener(v->cycleBand());
        cal.setOnClickListener(v->{if(audio!=null){audio.stop();audio.start();Toast.makeText(this,"S24 referansı yeniden kalibre ediliyor",Toast.LENGTH_SHORT).show();}});
        clear.setOnClickListener(v->resetScan());
        scanButton.setOnClickListener(v->{if(!scanning&&!finished){if(!parked){Toast.makeText(this,"Önce siyah probu sol alttaki başlangıç dairesine getir",Toast.LENGTH_LONG).show();return;}resetHistories();overlay.clearMap();overlay.beginScan();scanning=true;finished=false;scanButton.setText("TARAMAYI BİTİR");}else if(scanning){scanning=false;finished=true;overlay.finishScan();scanButton.setText("YENİ TARAMA");Toast.makeText(this,overlay.isHomogeneous()?"Tarama bitti • alan büyük ölçüde homojen":"Tarama bitti • kaynak adayı gerçek ölçüm noktalarından hesaplandı",Toast.LENGTH_LONG).show();}else resetScan();});
        auto.setOnClickListener(v->{vision.seed(probeX,probeY);visionValid=true;probeFrozen=false;probeMoving=false;Toast.makeText(this,"Siyah dairesel prob takibi yeniden kilitlendi",Toast.LENGTH_SHORT).show();});reset.setOnClickListener(v->resetScan());
    }

    private void resetHistories(){refHistory.clear();stationaryUsbHistory.clear();mapMedianHistory.clear();variableSource=false;variableUncertain=false;}
    private void resetScan(){scanning=false;finished=false;resetHistories();overlay.clearMap();overlay.setIdle();scanButton.setText("TARAMAYI BAŞLAT");}
    private void startAll(){if(camera==null)camera=new CameraController(this,cameraView);camera.start();if(imu==null)imu=new ImuEngine(this);imu.start();if(audio==null)audio=new ProbeAudioEngine(this,this::onProbe);audio.setMode(ProbeAudioEngine.MODE_USB_MINUS_REF);audio.setBand(ProbeAudioEngine.BAND_ALL);audio.start();handler.removeCallbacks(visionLoop);handler.post(visionLoop);}

    private final Runnable visionLoop=new Runnable(){@Override public void run(){
        if(cameraView!=null&&cameraView.isAvailable()){
            try{Bitmap b=cameraView.getBitmap(160,284);ProbeVisionTracker.Result r=vision.track(b);if(b!=null)b.recycle();probeX=r.x01;probeY=r.y01;probeR=r.radius01;visionConf=r.confidence;visionValid=r.valid;probeFrozen=r.frozen;probeMoving=r.moving;float dx=probeX-PARK_X,dy=probeY-PARK_Y;parked=visionValid&&!probeFrozen&&(dx*dx+dy*dy)<PARK_R*PARK_R;overlay.setTracker(probeX,probeY,probeR,visionConf,visionValid,probeFrozen);overlay.setParked(parked);}catch(Exception ignored){}
        }
        handler.postDelayed(this,25);
    }};

    private void onProbe(ProbeAudioEngine.Snapshot s){
        ImuEngine.Snapshot m=imu==null?null:imu.getLatest();float motion=m==null?0f:m.motion01;boolean phoneStable=motion<.18f;
        if(s.dualLive){push(refHistory,s.refDbfs,48);if(refHistory.size()>=16)variableSource=robustSpread(refHistory)>2.2f;variableUncertain=false;}
        else{if(!probeMoving&&visionValid&&!probeFrozen&&phoneStable)push(stationaryUsbHistory,s.usbDbfs,48);if(stationaryUsbHistory.size()>=20){variableSource=robustSpread(stationaryUsbHistory)>2.8f;variableUncertain=variableSource;}}

        float measure;if(audio!=null&&audio.getBand()!=ProbeAudioEngine.BAND_ALL)measure=s.bandDbfs;else if(variableSource&&s.dualLive)measure=s.deltaDb;else measure=s.usbDbfs;
        push(mapMedianHistory,measure,variableSource&&!s.dualLive?15:5);float robustMeasure=median(mapMedianHistory);
        boolean canWriteLocal=scanning&&visionValid&&!probeFrozen&&phoneStable&&s.refStable;
        if(variableSource&&!s.dualLive)canWriteLocal=canWriteLocal&&!probeMoving&&mapMedianHistory.size()>=9;
        if(canWriteLocal)overlay.updateProbe(probeX,probeY,robustMeasure,s.usbActive);

        final float fm=robustMeasure;final boolean canWrite=canWriteLocal;
        runOnUiThread(()->{
            usbText.setText(String.format(Locale.US,"USB PROBE\n%.1f dBFS",s.usbDbfs));refText.setText(String.format(Locale.US,"PHONE REF\n%.1f dBFS",s.refDbfs));deltaText.setText(String.format(Locale.US,"HARİTA\n%.1f dB",fm));imuText.setText(String.format(Locale.US,"IMU\n%.0f%%",motion*100f));
            String phase=finished?"SONUÇ HAZIR":scanning?"TARAMA AKTİF":parked?"PROB HAZIR":"PROBU SOL ALTA GETİR";
            String src=!variableSource?"SABİT/KARARLI SES":s.dualLive?"DEĞİŞKEN SES • S24 CANLI TELAFİ":"DEĞİŞKEN SES • DUAL YOK • YAVAŞ/BEKLEMELİ ÖLÇÜM";if(variableUncertain)src+=" • GÜVEN DÜŞÜK";
            String track=probeFrozen?"PROB KAYIP • MERKEZ DONDU":visionValid?String.format(Locale.US,"SİYAH DAİRE %d%%",Math.round(visionConf*100)):"PROB ARANIYOR";
            String write=scanning?(canWrite?"ÖLÇÜM ✓":"ÖLÇÜM BEKLE"):"ÖLÇÜM KAPALI";statusText.setText(phase+" • "+src+" • "+track+" • "+write+"\n"+s.status);
        });
    }

    private static void push(ArrayDeque<Float> q,float v,int max){if(Float.isNaN(v)||v<-119f)return;q.addLast(v);while(q.size()>max)q.removeFirst();}
    private static float median(ArrayDeque<Float> q){if(q.isEmpty())return -120f;ArrayList<Float>a=new ArrayList<>(q);Collections.sort(a);int n=a.size();return n%2==1?a.get(n/2):(a.get(n/2-1)+a.get(n/2))*.5f;}
    private static float robustSpread(ArrayDeque<Float> q){if(q.size()<5)return 0f;ArrayList<Float>a=new ArrayList<>(q);Collections.sort(a);int n=a.size();float p10=a.get(Math.max(0,(int)(.10f*(n-1)))),p90=a.get(Math.min(n-1,(int)(.90f*(n-1))));return p90-p10;}
    private void cycleBand(){if(audio==null)return;int n=(audio.getBand()+1)%5;audio.setBand(n);bandButton.setText(n==0?"BAND: TÜM":n==1?"BAND: DÜŞÜK":n==2?"BAND: KONUŞMA":n==3?"BAND: TİZ":"96 Hz TEST");audio.setMode(n==0?ProbeAudioEngine.MODE_USB_MINUS_REF:ProbeAudioEngine.MODE_BAND_SCAN);}
    private boolean hasPermissions(){return checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&hasPermissions())startAll();}
    @Override protected void onResume(){super.onResume();if(hasPermissions())startAll();}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(visionLoop);if(camera!=null)camera.stop();if(audio!=null)audio.stop();if(imu!=null)imu.stop();}
    private TextView label(String s,int sp,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setPadding(dp(4),dp(2),dp(4),dp(2));if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);return t;}
    private TextView metric(String s){TextView t=label(s,10,true);t.setGravity(Gravity.CENTER);t.setTextColor(Color.WHITE);t.setBackgroundColor(Color.argb(95,0,80,100));return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(9);b.setAllCaps(false);b.setMinHeight(dp(42));return b;}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-2,1f);}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
}
