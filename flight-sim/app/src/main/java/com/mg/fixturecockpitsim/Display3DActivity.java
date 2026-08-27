package com.mg.fixturecockpitsim;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mg.fixturecockpitsim.sim.AutonomousFlightMission;
import com.mg.fixturecockpitsim.sim.FlightControls;
import com.mg.fixturecockpitsim.sim.FlightDynamicsEngine;
import com.mg.fixturecockpitsim.sim.FlightState;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Display3DActivity extends Activity {
    private static final UUID SIM_UUID = UUID.fromString("6d9b6c72-4d47-4d8e-9b58-b5e7465b4a22");
    private static final int REQ_BT = 61;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler simHandler = new Handler(Looper.getMainLooper());
    private final FlightDynamicsEngine dynamics = new FlightDynamicsEngine();
    private final FlightState simState = new FlightState();
    private final FlightControls simControls = new FlightControls();
    private final AutonomousFlightMission mission = new AutonomousFlightMission();
    private volatile boolean running = true, connected;
    private volatile float roll, pitch, yaw, throttle = 0.62f, linkHz;
    private volatile int lastSeq, drops;
    private volatile long previousRxMs;
    private BluetoothAdapter bt;
    private BluetoothServerSocket server;
    private BluetoothSocket socket;
    private BufferedWriter writer;
    private Jet3DView jetView;
    private RunwayHudView runwayView;
    private TextView missionHud;
    private LinearLayout waitingPanel;
    private long lastSimNs;
    private boolean demoMode;
    private int demoCameraMode=Jet3DView.CAMERA_CHASE;

    private final Runnable simTick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long now = System.nanoTime();
            double dt = lastSimNs == 0 ? 0.02 : Math.min(0.05, Math.max(0.005, (now-lastSimNs)/1_000_000_000.0));
            lastSimNs = now;
            // Autonomous mission belongs ONLY to Demo Mode. In aircraft-display mode the old
            // stand-alone aircraft scene must never appear before the pilot phone connects.
            if (demoMode) {
                mission.update(simState, simControls, dt);
                dynamics.step(simState, simControls, dt);
                roll=(float)simState.rollDeg; pitch=(float)simState.pitchDeg; yaw=(float)simState.headingDeg; throttle=(float)simState.throttle;
                jetView.setTelemetry(roll,pitch,yaw,throttle,50f,0,true);
                jetView.setSimulationState((float)simState.gearPosition,(float)simState.mainStrutCompression01,(float)simState.noseStrutCompression01,(float)simState.brake01,simState.onGround);
                runwayView.setFlightState(simState.altitudeM,simState.trueAirspeedMps,simState.onGround,mission.getPhase().name());
                runwayView.setDemoProgress(mission.getOrbitTimeSec());
                updateDemoCameraDirector();
                updateMissionHud();
            }
            simHandler.postDelayed(this,20);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        demoMode=getIntent()!=null && getIntent().getBooleanExtra(LauncherActivity.EXTRA_DEMO_MODE,false);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        bt = BluetoothAdapter.getDefaultAdapter();
        mission.reset(simState);
        simControls.gearDown=true;

        runwayView = new RunwayHudView(this);
        runwayView.setDemoMode(demoMode);
        jetView = new Jet3DView(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(3,9,13));
        root.addView(runwayView, new FrameLayout.LayoutParams(-1,-1));
        root.addView(jetView, new FrameLayout.LayoutParams(-1,-1));

        missionHud=new TextView(this);
        missionHud.setTextColor(0xffffffff); missionHud.setTextSize(14f); missionHud.setPadding(dp(12),dp(8),dp(12),dp(8));
        missionHud.setBackgroundColor(0x66000000);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT); hp.setMargins(dp(10),dp(10),0,0); root.addView(missionHud,hp);

        waitingPanel=new LinearLayout(this);
        waitingPanel.setOrientation(LinearLayout.VERTICAL);
        waitingPanel.setGravity(Gravity.CENTER);
        waitingPanel.setPadding(dp(36),dp(28),dp(36),dp(28));
        waitingPanel.setBackgroundColor(Color.rgb(3,9,13));
        TextView waitTitle=new TextView(this);
        waitTitle.setText("UÇAK EKRANI"); waitTitle.setTextColor(Color.rgb(160,255,190)); waitTitle.setTextSize(28f); waitTitle.setGravity(Gravity.CENTER);
        TextView waitText=new TextView(this);
        waitText.setText("Pilot telefonu bağlantısı bekleniyor\n\nPilot telefonunda PİLOT TELEFONU / KOKPİT seçeneğini aç ve bu telefona bağlan.\nBağlantı kurulunca 3D uçak ekranı otomatik açılacak.");
        waitText.setTextColor(Color.LTGRAY); waitText.setTextSize(16f); waitText.setGravity(Gravity.CENTER); waitText.setPadding(0,dp(18),0,0);
        waitingPanel.addView(waitTitle,new LinearLayout.LayoutParams(-1,-2));
        waitingPanel.addView(waitText,new LinearLayout.LayoutParams(-1,-2));
        FrameLayout.LayoutParams wp=new FrameLayout.LayoutParams(-1,-1); root.addView(waitingPanel,wp);

        Button back = new Button(this); back.setText("MOD"); back.setAllCaps(false);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(96),dp(48), Gravity.TOP|Gravity.RIGHT); bp.setMargins(0,10,10,0); root.addView(back,bp);
        back.setOnClickListener(v -> finish());
        setContentView(root);

        jetView.setSimulationState((float)simState.gearPosition,0f,0f,0f,true);
        jetView.setCameraMode(Jet3DView.CAMERA_CHASE);
        runwayView.setFlightState(0,0,true,mission.getPhase().name());

        if(demoMode){
            showFlightScene();
            updateMissionHud();
            simHandler.post(simTick);
            Toast.makeText(this,"DEMO MODE — sinematik kamera, otomatik kalkış, gezi ve iniş",Toast.LENGTH_LONG).show();
        } else {
            showWaitingScreen();
            requestBtThenStart();
        }
    }

    private void showWaitingScreen(){
        waitingPanel.setVisibility(View.VISIBLE);
        runwayView.setVisibility(View.GONE);
        jetView.setVisibility(View.GONE);
        missionHud.setVisibility(View.GONE);
    }

    private void showFlightScene(){
        waitingPanel.setVisibility(View.GONE);
        runwayView.setVisibility(View.VISIBLE);
        jetView.setVisibility(View.VISIBLE);
        missionHud.setVisibility(View.VISIBLE);
    }

    private void updateDemoCameraDirector(){
        AutonomousFlightMission.Phase p=mission.getPhase();
        int wanted;
        switch(p){
            case RUNWAY_HOLD: wanted=Jet3DView.CAMERA_RIGHT_QUARTER; break;
            case TAKEOFF_ROLL: wanted=Jet3DView.CAMERA_CHASE; break;
            case ROTATE_CLIMB: wanted=Jet3DView.CAMERA_REAR; break;
            case ORBIT:
                int shot=((int)(mission.getOrbitTimeSec()/18.0))%4;
                wanted=shot==0?Jet3DView.CAMERA_CHASE:shot==1?Jet3DView.CAMERA_RIGHT_QUARTER:shot==2?Jet3DView.CAMERA_LEFT_QUARTER:Jet3DView.CAMERA_REAR;
                break;
            case APPROACH: wanted=Jet3DView.CAMERA_RIGHT_QUARTER; break;
            case FLARE: wanted=Jet3DView.CAMERA_REAR; break;
            case ROLLOUT: wanted=Jet3DView.CAMERA_LEFT_QUARTER; break;
            case COMPLETE: wanted=Jet3DView.CAMERA_RIGHT_QUARTER; break;
            default: wanted=Jet3DView.CAMERA_CHASE;
        }
        if(wanted!=demoCameraMode){demoCameraMode=wanted;jetView.setCameraMode(wanted);}
    }

    private String cameraName(){
        int c=demoMode?demoCameraMode:jetView.getCameraMode();
        switch(c){case Jet3DView.CAMERA_REAR:return "REAR";case Jet3DView.CAMERA_RIGHT_QUARTER:return "RIGHT 3/4";case Jet3DView.CAMERA_LEFT_QUARTER:return "LEFT 3/4";default:return "CHASE";}
    }

    private void updateMissionHud(){
        if(!demoMode && !connected) return;
        String phase=mission.getPhase().name().replace('_',' ');
        String extra=mission.getPhase()== AutonomousFlightMission.Phase.ORBIT ? String.format(Locale.US,"  GEZİ %.0f/300 s",mission.getOrbitTimeSec()) : "";
        String mode=demoMode?"DEMO":"UÇAK EKRANI / PILOT BAĞLI";
        missionHud.setText(String.format(Locale.US,
                "%s  %s%s   CAM %s\nALT %.0f m   SPD %.0f m/s   HDG %03.0f\nGEAR %.0f%%   BRK %.0f%%   WOW %s\nSTRUT M %.0f%% N %.0f%%   SINK %.1f m/s",
                mode,phase,extra,cameraName(),simState.altitudeM,simState.trueAirspeedMps,simState.headingDeg,
                simState.gearPosition*100.0,simState.brake01*100.0,simState.onGround?"GROUND":"AIR",
                simState.mainStrutCompression01*100.0,simState.noseStrutCompression01*100.0,simState.touchdownSinkMps));
    }

    private void requestBtThenStart(){
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN},REQ_BT);
        } else startServer();
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQ_BT && grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) startServer();
        else Toast.makeText(this,"Bluetooth izni yok — uçak ekranı pilot bağlantısını bekleyemez",Toast.LENGTH_LONG).show();
    }

    private void startServer(){
        if(bt==null){Toast.makeText(this,"Bluetooth donanımı yok",Toast.LENGTH_LONG).show();return;}
        if(!bt.isEnabled()){Toast.makeText(this,"Bluetooth kapalı — açıp tekrar UÇAK EKRANI'nı seç",Toast.LENGTH_LONG).show();return;}
        io.execute(() -> {
            while(running && !demoMode){
                try{
                    server=bt.listenUsingRfcommWithServiceRecord("FixtureCockpit3D",SIM_UUID);
                    socket=server.accept(); connected=true;
                    runOnUiThread(() -> {
                        showFlightScene();
                        missionHud.setText("UÇAK EKRANI — PILOT BAĞLANDI");
                        Toast.makeText(this,"Pilot bağlandı — 3D uçak ekranı aktif",Toast.LENGTH_SHORT).show();
                    });
                    writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));
                    BufferedReader r=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                    String line;
                    while(running && (line=r.readLine())!=null){
                        String[] a=line.split(","); if(a.length<7 || !"V2".equals(a[0])) continue;
                        try{
                            int seq=Integer.parseInt(a[1]); float nr=Float.parseFloat(a[3]),np=Float.parseFloat(a[4]),ny=Float.parseFloat(a[5]),nt=Float.parseFloat(a[6]); long now=System.currentTimeMillis();
                            if(lastSeq>0 && seq>lastSeq+1)drops+=seq-lastSeq-1; lastSeq=seq;
                            if(previousRxMs>0){float d=Math.max(1,now-previousRxMs);linkHz=linkHz+(1000f/d-linkHz)*0.15f;}previousRxMs=now;
                            roll=approach(roll,nr,7.5f);pitch=approach(pitch,np,5f);yaw=angleLerp(yaw,ny,0.24f);throttle+=(nt-throttle)*0.20f;
                            jetView.setTelemetry(roll,pitch,yaw,throttle,linkHz,drops,true);
                            synchronized(this){writer.write("A,"+seq+"\n");writer.flush();}
                        }catch(Exception ignored){}
                    }
                }catch(Exception ignored){connected=false;}
                finally{
                    closeLink();
                    if(running && !demoMode) runOnUiThread(this::showWaitingScreen);
                }
            }
        });
    }

    private static float approach(float c,float t,float step){float d=t-c;if(d>step)d=step;if(d<-step)d=-step;return c+d;}
    private static float angleLerp(float a,float b,float k){float d=b-a;while(d>180)d-=360;while(d<-180)d+=360;return a+d*k;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void closeLink(){try{if(socket!=null)socket.close();}catch(Exception ignored){}try{if(server!=null)server.close();}catch(Exception ignored){}socket=null;server=null;writer=null;connected=false;}
    @Override protected void onPause(){super.onPause();jetView.onPause();}
    @Override protected void onResume(){super.onResume();jetView.onResume();lastSimNs=0;}
    @Override protected void onDestroy(){running=false;simHandler.removeCallbacks(simTick);closeLink();io.shutdownNow();super.onDestroy();}
}
