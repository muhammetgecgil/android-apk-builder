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
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

/** V8.1: S24 camera+phone mic reference, Fyvadio roaming USB probe, IMU gate, full-colour acoustic map. */
public final class MainActivity extends Activity {
    private static final int REQ=81;
    private TextureView cameraView;
    private HeatmapOverlayView overlay;
    private TextView usbText,refText,deltaText,statusText,imuText;
    private ProbeAudioEngine audio;
    private CameraController camera;
    private ImuEngine imu;
    private final ProbeVisionTracker vision=new ProbeVisionTracker();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private float probeX=.5f,probeY=.72f,visionConf=0f;
    private boolean visionValid=false,scanning=true;
    private Button modeButton,bandButton,scanButton;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.rgb(4,12,18));getWindow().setNavigationBarColor(Color.rgb(4,12,18));
        buildUi();
        if(hasPermissions())startAll();else requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},REQ);
    }

    private void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
        cameraView=new TextureView(this);root.addView(cameraView,new FrameLayout.LayoutParams(-1,-1));
        overlay=new HeatmapOverlayView(this);root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        overlay.setTargetListener((x,y)->{probeX=x;probeY=y;vision.seed(x,y);visionValid=true;Toast.makeText(this,"Fyvadio konumu yeniden kilitlendi",Toast.LENGTH_SHORT).show();});

        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(10),dp(5),dp(10),dp(6));top.setBackgroundColor(Color.argb(215,4,12,18));
        root.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        TextView title=label("SES GÖRÜNTÜ HARİTASI V8.1 • FULL COLOR",16,true);title.setTextColor(Color.WHITE);top.addView(title);
        statusText=label("Fyvadio prob aranıyor • prob kaynak adayından hariç",10,false);statusText.setTextColor(Color.CYAN);top.addView(statusText);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);top.addView(row);
        usbText=metric("USB PROBE\n-- dBFS");refText=metric("PHONE REF\n-- dBFS");deltaText=metric("USB-REF\n-- dB");imuText=metric("IMU\n--");
        row.addView(usbText,weight());row.addView(refText,weight());row.addView(deltaText,weight());row.addView(imuText,weight());

        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(dp(8),dp(5),dp(8),dp(8));bottom.setBackgroundColor(Color.argb(230,4,12,18));
        FrameLayout.LayoutParams blp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);blp.bottomMargin=dp(56);root.addView(bottom,blp);
        root.setOnApplyWindowInsetsListener((v,insets)->{int nav=insets.getSystemWindowInsetBottom();FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)bottom.getLayoutParams();lp.bottomMargin=Math.max(dp(12),nav+dp(10));bottom.setLayoutParams(lp);return insets;});root.requestApplyInsets();

        TextView help=label("MAVİ→YEŞİL→SARI→TURUNCU→KIRMIZI = artan USB-REF • Fyvadio yalnız probdur",10,false);help.setTextColor(Color.WHITE);bottom.addView(help);
        LinearLayout r1=new LinearLayout(this);r1.setOrientation(LinearLayout.HORIZONTAL);bottom.addView(r1);
        modeButton=button("PROBE-REF");bandButton=button("BAND: TÜM");Button calibrate=button("REF KALİBRE");Button clear=button("HARİTA SİL");
        r1.addView(modeButton,weight());r1.addView(bandButton,weight());r1.addView(calibrate,weight());r1.addView(clear,weight());
        LinearLayout r2=new LinearLayout(this);r2.setOrientation(LinearLayout.HORIZONTAL);bottom.addView(r2);
        scanButton=button("TARAMAYI BİTİR");Button auto=button("PROB: AUTO");Button reset=button("YENİ TARAMA");
        r2.addView(scanButton,weight());r2.addView(auto,weight());r2.addView(reset,weight());

        modeButton.setOnClickListener(v->cycleMode());bandButton.setOnClickListener(v->cycleBand());
        calibrate.setOnClickListener(v->{if(audio!=null){audio.stop();audio.start();Toast.makeText(this,"S24 referans mikrofonu yeniden ölçülüyor",Toast.LENGTH_SHORT).show();}});
        clear.setOnClickListener(v->{overlay.clearMap();scanning=true;scanButton.setText("TARAMAYI BİTİR");});
        scanButton.setOnClickListener(v->{if(scanning){scanning=false;overlay.finishScan();scanButton.setText("CANLIYA DÖN");Toast.makeText(this,"Nihai tam ekran ses haritası oluşturuldu",Toast.LENGTH_SHORT).show();}else{scanning=true;overlay.resumeScan();scanButton.setText("TARAMAYI BİTİR");}});
        auto.setOnClickListener(v->{vision.seed(probeX,probeY);visionValid=true;Toast.makeText(this,"Otomatik Fyvadio takip yeniden başlatıldı",Toast.LENGTH_SHORT).show();});
        reset.setOnClickListener(v->{overlay.clearMap();scanning=true;scanButton.setText("TARAMAYI BİTİR");Toast.makeText(this,"Yeni tarama başladı",Toast.LENGTH_SHORT).show();});
    }

    private void startAll(){
        if(camera==null)camera=new CameraController(this,cameraView);camera.start();
        if(imu==null)imu=new ImuEngine(this);imu.start();
        if(audio==null)audio=new ProbeAudioEngine(this,this::onProbe);
        audio.setMode(ProbeAudioEngine.MODE_USB_MINUS_REF);audio.start();
        handler.removeCallbacks(visionLoop);handler.post(visionLoop);
    }

    private final Runnable visionLoop=new Runnable(){@Override public void run(){
        if(cameraView!=null&&cameraView.isAvailable()){
            try{
                Bitmap b=cameraView.getBitmap(144,256);
                ProbeVisionTracker.Result r=vision.track(b);
                if(b!=null)b.recycle();
                if(r.valid){probeX=r.x01;probeY=r.y01;visionConf=r.confidence;visionValid=true;}else{visionConf=r.confidence;visionValid=false;}
                overlay.setTracker(probeX,probeY,visionConf,visionValid);
                if(scanning&&overlay.getSamples()>100&&overlay.getCoverage()>.52f&&System.currentTimeMillis()-overlay.getLastNewCellMs()>3500){scanning=false;overlay.finishScan();scanButton.setText("CANLIYA DÖN");Toast.makeText(MainActivity.this,"Tarama kapsaması tamamlandı • nihai harita hazır",Toast.LENGTH_SHORT).show();}
            }catch(Exception ignored){}
        }
        handler.postDelayed(this,110);
    }};

    private void onProbe(ProbeAudioEngine.Snapshot s){
        ImuEngine.Snapshot m=imu==null?null:imu.getLatest();float motion=m==null?0f:m.motion01;
        boolean cameraStable=motion<.16f;
        if(scanning&&visionValid&&cameraStable)overlay.updateProbe(probeX,probeY,s.bandEnergy01,s.deltaDb,s.usbActive);
        runOnUiThread(()->{
            usbText.setText(String.format(Locale.US,"USB PROBE\n%.1f dBFS",s.usbDbfs));refText.setText(String.format(Locale.US,"PHONE REF\n%.1f dBFS",s.refDbfs));deltaText.setText(String.format(Locale.US,"USB-REF\n%+.1f dB",s.deltaDb));imuText.setText(String.format(Locale.US,"IMU\n%.0f%%",motion*100f));
            String usb=s.usbActive?"USB ✓":"USB ✗";String ref=s.phoneRefActive?(s.dualLive?"REF LIVE ✓":"REF CAL ✓"):"REF ✗";String pv=visionValid?String.format(Locale.US,"PROB AUTO %d%% • MASK ✓",Math.round(visionConf*100)):"PROB ARANIYOR";String stable=cameraStable?"KAMERA SABİT":"KAMERA HAREKETLİ • ÖLÇÜM BEKLE";
            statusText.setText(usb+" • "+ref+" • "+pv+" • "+stable+"\n"+s.usbName+" • "+s.status);
        });
    }

    private void cycleMode(){if(audio==null)return;int n=(audio.getMode()+1)%3;audio.setMode(n);modeButton.setText(n==ProbeAudioEngine.MODE_USB_ABSOLUTE?"USB MUTLAK":n==ProbeAudioEngine.MODE_USB_MINUS_REF?"PROBE-REF":"FREKANS");}
    private void cycleBand(){if(audio==null)return;int n=(audio.getBand()+1)%4;audio.setBand(n);bandButton.setText(n==ProbeAudioEngine.BAND_ALL?"BAND: TÜM":n==ProbeAudioEngine.BAND_LOW?"BAND: DÜŞÜK":n==ProbeAudioEngine.BAND_VOICE?"BAND: KONUŞMA":"BAND: TİZ");}

    private boolean hasPermissions(){return checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&hasPermissions())startAll();else Toast.makeText(this,"Kamera ve mikrofon izinleri gerekli",Toast.LENGTH_LONG).show();}
    @Override protected void onResume(){super.onResume();if(hasPermissions())startAll();}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(visionLoop);if(camera!=null)camera.stop();if(audio!=null)audio.stop();if(imu!=null)imu.stop();}
    @Override protected void onDestroy(){handler.removeCallbacks(visionLoop);if(camera!=null)camera.stop();if(audio!=null)audio.stop();if(imu!=null)imu.stop();super.onDestroy();}

    private TextView label(String s,int sp,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setPadding(dp(4),dp(2),dp(4),dp(2));if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);return t;}
    private TextView metric(String s){TextView t=label(s,10,true);t.setGravity(Gravity.CENTER);t.setTextColor(Color.WHITE);t.setBackgroundColor(Color.argb(95,0,80,100));return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(9);b.setAllCaps(false);b.setMinHeight(dp(42));return b;}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-2,1f);}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
}
