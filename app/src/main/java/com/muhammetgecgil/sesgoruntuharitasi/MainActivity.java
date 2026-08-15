package com.muhammetgecgil.sesgoruntuharitasi;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

/** V7 Probe Fusion: Fyvadio USB mic = roaming probe, S24 mic = reference, camera + IMU = spatial reference. */
public final class MainActivity extends Activity {
    private static final int REQ=70;
    private TextureView cameraView;
    private HeatmapOverlayView overlay;
    private TextView usbText,refText,deltaText,statusText,imuText;
    private ProbeAudioEngine audio;
    private CameraController camera;
    private ImuEngine imu;
    private float probeX=.5f,probeY=.5f;
    private Button modeButton,bandButton;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildUi();
        if(hasPermissions()) startAll();
        else requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},REQ);
    }

    private void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
        cameraView=new TextureView(this);root.addView(cameraView,new FrameLayout.LayoutParams(-1,-1));
        overlay=new HeatmapOverlayView(this);root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        overlay.setTargetListener((x,y)->{probeX=x;probeY=y;});

        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(12),dp(8),dp(12),dp(8));top.setBackgroundColor(Color.argb(220,4,12,18));
        root.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        TextView title=label("SES GÖRÜNTÜ HARİTASI V7 • PROBE FUSION",17,true);title.setTextColor(Color.WHITE);top.addView(title);
        statusText=label("Fyvadio USB prob aranıyor • S24 referansı hazırlanıyor",11,false);statusText.setTextColor(Color.CYAN);top.addView(statusText);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);top.addView(row);
        usbText=metric("USB PROBE\n-- dBFS");refText=metric("PHONE REF\n-- dBFS");deltaText=metric("USB-REF\n-- dB");imuText=metric("IMU\n--");
        row.addView(usbText,weight());row.addView(refText,weight());row.addView(deltaText,weight());row.addView(imuText,weight());

        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(dp(8),dp(6),dp(8),dp(10));bottom.setBackgroundColor(Color.argb(225,4,12,18));
        root.addView(bottom,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        TextView help=label("Ekranda probun baktığı noktaya dokun • Fyvadio'yu o bölgeye yaklaştır • kırmızı alan en güçlü kaynak",11,false);help.setTextColor(Color.WHITE);bottom.addView(help);
        LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);bottom.addView(buttons);
        modeButton=button("PROBE-REF");bandButton=button("BAND: TÜM");Button calibrate=button("REF KALİBRE");Button clear=button("HARİTA SİL");
        buttons.addView(modeButton,weight());buttons.addView(bandButton,weight());buttons.addView(calibrate,weight());buttons.addView(clear,weight());
        modeButton.setOnClickListener(v->cycleMode());
        bandButton.setOnClickListener(v->cycleBand());
        calibrate.setOnClickListener(v->{if(audio!=null){audio.stop();audio.start();Toast.makeText(this,"Telefon referansı yeniden ölçülüyor",Toast.LENGTH_SHORT).show();}});
        clear.setOnClickListener(v->overlay.clearMap());
    }

    private void startAll(){
        if(camera==null)camera=new CameraController(this,cameraView);camera.start();
        if(imu==null)imu=new ImuEngine(this);imu.start();
        if(audio==null) audio=new ProbeAudioEngine(this,this::onProbe);
        audio.setMode(ProbeAudioEngine.MODE_USB_MINUS_REF);audio.start();
    }

    private void onProbe(ProbeAudioEngine.Snapshot s){
        overlay.updateProbe(probeX,probeY,s.bandEnergy01,s.deltaDb,s.usbActive);
        runOnUiThread(()->{
            usbText.setText(String.format(Locale.US,"USB PROBE\n%.1f dBFS",s.usbDbfs));
            refText.setText(String.format(Locale.US,"PHONE REF\n%.1f dBFS",s.refDbfs));
            deltaText.setText(String.format(Locale.US,"USB-REF\n%+.1f dB",s.deltaDb));
            ImuEngine.Snapshot m=imu==null?null:imu.getLatest();
            imuText.setText(m==null?"IMU\n--":String.format(Locale.US,"IMU\n%.0f%%",m.motion01*100f));
            String usb=s.usbActive?"USB PROBE ✓":"USB PROBE ✗";
            String ref=s.phoneRefActive?(s.dualLive?"PHONE REF LIVE ✓":"PHONE REF CAL ✓"):"PHONE REF ✗";
            String dual=s.dualLive?"DUAL LIVE ✓":"DUAL LIVE ✗";
            statusText.setText(usb+"  •  "+ref+"  •  CAMERA ✓  •  IMU ✓  •  "+dual+"\n"+s.usbName+" • "+s.status);
        });
    }

    private void cycleMode(){
        if(audio==null)return;int n=(audio.getMode()+1)%3;audio.setMode(n);
        modeButton.setText(n==ProbeAudioEngine.MODE_USB_ABSOLUTE?"USB MUTLAK":n==ProbeAudioEngine.MODE_USB_MINUS_REF?"PROBE-REF":"FREKANS");
    }
    private void cycleBand(){
        if(audio==null)return;int n=(audio.getBand()+1)%4;audio.setBand(n);
        bandButton.setText(n==ProbeAudioEngine.BAND_ALL?"BAND: TÜM":n==ProbeAudioEngine.BAND_LOW?"BAND: DÜŞÜK":n==ProbeAudioEngine.BAND_VOICE?"BAND: KONUŞMA":"BAND: TİZ");
    }

    private boolean hasPermissions(){return checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&hasPermissions())startAll();else Toast.makeText(this,"Kamera ve mikrofon izinleri gerekli",Toast.LENGTH_LONG).show();}
    @Override protected void onResume(){super.onResume();if(hasPermissions())startAll();}
    @Override protected void onPause(){super.onPause();if(camera!=null)camera.stop();if(audio!=null)audio.stop();if(imu!=null)imu.stop();}
    @Override protected void onDestroy(){if(camera!=null)camera.stop();if(audio!=null)audio.stop();if(imu!=null)imu.stop();super.onDestroy();}

    private TextView label(String s,int sp,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setPadding(dp(4),dp(3),dp(4),dp(3));if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);return t;}
    private TextView metric(String s){TextView t=label(s,11,true);t.setGravity(Gravity.CENTER);t.setTextColor(Color.WHITE);t.setBackgroundColor(Color.argb(90,0,80,100));return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(10);b.setAllCaps(false);return b;}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-2,1f);}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
}
