package com.mg.fixturecockpitsim;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
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
    private volatile long lastPacketMs;
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
    private long lastSimNs;

    private final Runnable simTick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long now = System.nanoTime();
            double dt = lastSimNs == 0 ? 0.02 : Math.min(0.05, Math.max(0.005, (now-lastSimNs)/1_000_000_000.0));
            lastSimNs = now;
            if (!connected) {
                mission.update(simState, simControls, dt);
                dynamics.step(simState, simControls, dt);
                roll=(float)simState.rollDeg; pitch=(float)simState.pitchDeg; yaw=(float)simState.headingDeg; throttle=(float)simState.throttle;
                jetView.setTelemetry(roll,pitch,yaw,throttle,50f,0,true);
                jetView.setSimulationState((float)simState.gearPosition,(float)simState.mainStrutCompression01,(float)simState.noseStrutCompression01,(float)simState.brake01,simState.onGround);
                runwayView.setFlightState(simState.altitudeM,simState.trueAirspeedMps,simState.onGround,mission.getPhase().name());
                updateMissionHud();
            }
            simHandler.postDelayed(this,20);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        bt = BluetoothAdapter.getDefaultAdapter();
        mission.reset(simState);
        simControls.gearDown=true;
        jetView = new Jet3DView(this);
        runwayView = new RunwayHudView(this);
        FrameLayout root = new FrameLayout(this);
        root.addView(jetView, new FrameLayout.LayoutParams(-1,-1));
        root.addView(runwayView, new FrameLayout.LayoutParams(-1,-1));

        missionHud=new TextView(this);
        missionHud.setTextColor(0xffffffff); missionHud.setTextSize(14f); missionHud.setPadding(dp(12),dp(8),dp(12),dp(8));
        missionHud.setBackgroundColor(0x66000000);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT); hp.setMargins(dp(10),dp(10),0,0); root.addView(missionHud,hp);

        Button back = new Button(this); back.setText("MOD"); back.setAllCaps(false);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(96),dp(48), Gravity.TOP|Gravity.RIGHT); bp.setMargins(0,10,10,0); root.addView(back,bp);
        back.setOnClickListener(v -> finish());
        setContentView(root);
        jetView.setSimulationState((float)simState.gearPosition,0f,0f,0f,true);
        runwayView.setFlightState(0,0,true,mission.getPhase().name());
        updateMissionHud();
        simHandler.post(simTick);
        requestBtThenStart();
    }

    private void updateMissionHud(){
        String phase=mission.getPhase().name().replace('_',' ');
        String extra=mission.getPhase()== AutonomousFlightMission.Phase.ORBIT ? String.format(Locale.US,"  TUR %.0f/300 s",mission.getOrbitTimeSec()) : "";
        missionHud.setText(String.format(Locale.US,
                "AUTO %s%s\nALT %.0f m   SPD %.0f m/s   HDG %03.0f\nGEAR %.0f%%   BRK %.0f%%   WOW %s\nSTRUT M %.0f%% N %.0f%%   SINK %.1f m/s",
                phase,extra,simState.altitudeM,simState.trueAirspeedMps,simState.headingDeg,
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
        else Toast.makeText(this,"Bluetooth izni gerekli — otomatik demo çalışmaya devam eder",Toast.LENGTH_LONG).show();
    }

    private void startServer(){
        if(bt==null){Toast.makeText(this,"Bluetooth yok — otomatik uçuş aktif",Toast.LENGTH_LONG).show();return;}
        if(!bt.isEnabled()){Toast.makeText(this,"Bluetooth kapalı — otomatik uçuş aktif",Toast.LENGTH_SHORT).show();return;}
        io.execute(() -> {
            while(running){
                try{
                    server=bt.listenUsingRfcommWithServiceRecord("FixtureCockpit3D",SIM_UUID);
                    socket=server.accept(); connected=true; lastPacketMs=System.currentTimeMillis();
                    runOnUiThread(() -> missionHud.setText("MANUAL COCKPIT LINK"));
                    writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));
                    BufferedReader r=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                    String line;
                    while(running && (line=r.readLine())!=null){
                        String[] a=line.split(","); if(a.length<7 || !"V2".equals(a[0])) continue;
                        try{
                            int seq=Integer.parseInt(a[1]); float nr=Float.parseFloat(a[3]),np=Float.parseFloat(a[4]),ny=Float.parseFloat(a[5]),nt=Float.parseFloat(a[6]); long now=System.currentTimeMillis();
                            if(lastSeq>0 && seq>lastSeq+1)drops+=seq-lastSeq-1; lastSeq=seq;
                            if(previousRxMs>0){float d=Math.max(1,now-previousRxMs);linkHz=linkHz+(1000f/d-linkHz)*0.15f;}previousRxMs=now;
                            roll=approach(roll,nr,7.5f);pitch=approach(pitch,np,5f);yaw=angleLerp(yaw,ny,0.24f);throttle+=(nt-throttle)*0.20f;lastPacketMs=now;
                            jetView.setTelemetry(roll,pitch,yaw,throttle,linkHz,drops,true);
                            synchronized(this){writer.write("A,"+seq+"\n");writer.flush();}
                        }catch(Exception ignored){}
                    }
                }catch(Exception ignored){connected=false;}
                finally{closeLink();}
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
