package com.mg.fixturecockpitsim;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
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

/**
 * AVM-12.1 aircraft runtime.
 * Demo mode flies autonomously until a V3 controller connects; then the second phone owns every pilot axis.
 */
public final class FlightRuntimeActivity extends Activity {
    private static final UUID SIM_UUID=UUID.fromString("6d9b6c72-4d47-4d8e-9b58-b5e7465b4a22");
    private static final int REQ_BT=72;
    private static final double RUNWAY_HDG=270.0;
    private final ExecutorService io=Executors.newCachedThreadPool();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Object writeLock=new Object();
    private final FlightDynamicsEngine dynamics=new FlightDynamicsEngine();
    private final FlightState state=new FlightState();
    private final FlightControls controls=new FlightControls();
    private final AutonomousFlightMission mission=new AutonomousFlightMission();

    private BluetoothAdapter bt;
    private BluetoothServerSocket server;
    private BluetoothSocket socket;
    private BufferedWriter writer;
    private volatile boolean running=true,connected,remoteTakeover,remoteEver;
    private volatile float remoteRoll,remotePitch,remoteYaw,remoteThrottle=.08f,remoteBrake;
    private volatile boolean remoteGearDown=true;
    private volatile int lastRemoteSeq;
    private long lastSimNs,lastTelemetryMs;
    private boolean demoMode,crashed;
    private String crashReason="";
    private double runwayCrossTrackM,runwayAlongTrackM,crashRollTarget;

    private AirfieldWorldView world;
    private Jet3DView jet;
    private TextView hud,linkBanner,crashBanner;
    private Button resetButton;

    private final Runnable simLoop=new Runnable(){@Override public void run(){
        if(!running)return;
        long now=System.nanoTime();double dt=lastSimNs==0?.02:Math.min(.05,Math.max(.005,(now-lastSimNs)/1e9));lastSimNs=now;
        stepSimulation(dt);
        renderState();
        if(connected&&System.currentTimeMillis()-lastTelemetryMs>=90){lastTelemetryMs=System.currentTimeMillis();sendTelemetry();}
        handler.postDelayed(this,20);
    }};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        demoMode=getIntent()!=null&&getIntent().getBooleanExtra(LauncherActivity.EXTRA_DEMO_MODE,false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        bt=BluetoothAdapter.getDefaultAdapter();
        mission.reset(state);controls.gearDown=true;

        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(3,9,13));
        world=new AirfieldWorldView(this);jet=new Jet3DView(this);root.addView(world,new FrameLayout.LayoutParams(-1,-1));root.addView(jet,new FrameLayout.LayoutParams(-1,-1));
        hud=new TextView(this);hud.setTextColor(Color.WHITE);hud.setTextSize(14);hud.setPadding(dp(12),dp(8),dp(12),dp(8));hud.setBackgroundColor(0x72000000);FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT);hp.setMargins(dp(10),dp(10),0,0);root.addView(hud,hp);
        linkBanner=new TextView(this);linkBanner.setTextColor(Color.rgb(170,255,205));linkBanner.setTextSize(13);linkBanner.setGravity(Gravity.CENTER);linkBanner.setPadding(dp(10),dp(6),dp(10),dp(6));linkBanner.setBackgroundColor(0x8a10251c);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);lp.setMargins(0,dp(10),0,0);root.addView(linkBanner,lp);
        Button back=button("GERİ");FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(82),dp(45),Gravity.TOP|Gravity.RIGHT);bp.setMargins(0,dp(8),dp(10),0);root.addView(back,bp);back.setOnClickListener(v->finish());
        resetButton=button("RESET");FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(dp(82),dp(45),Gravity.TOP|Gravity.RIGHT);rp.setMargins(0,dp(58),dp(10),0);root.addView(resetButton,rp);resetButton.setVisibility(View.GONE);resetButton.setOnClickListener(v->resetSimulation());
        crashBanner=new TextView(this);crashBanner.setTextColor(Color.WHITE);crashBanner.setTextSize(26);crashBanner.setGravity(Gravity.CENTER);crashBanner.setBackgroundColor(0xaa7c170d);crashBanner.setVisibility(View.GONE);FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(480),dp(105),Gravity.CENTER);root.addView(crashBanner,cp);
        setContentView(root);

        jet.setCameraMode(Jet3DView.CAMERA_CHASE);jet.setSimulationState(1,0,0,1,true);jet.setFlightMotion(0,0,true);jet.setControlInputs(0,0,0,0);
        renderState();requestBtThenStart();handler.post(simLoop);
        Toast.makeText(this,demoMode?"DEMO AUTO çalışıyor. Advanced Controller bağlanınca kontrol tamamen ikinci telefona geçer.":"Uçak ekranı Bluetooth kumanda telefonu bekliyor.",Toast.LENGTH_LONG).show();
    }

    private void stepSimulation(double dt){
        if(crashed){
            state.throttle=0;state.brake01=1;state.altitudeM=0;state.verticalSpeedMps=0;state.onGround=true;
            state.trueAirspeedMps=Math.max(0,state.trueAirspeedMps-19*dt);
            state.rollDeg+=(crashRollTarget-state.rollDeg)*Math.min(1,dt*2.4);
            state.pitchDeg+=(-11-state.pitchDeg)*Math.min(1,dt*2.0);
            return;
        }

        boolean wasGround=state.onGround;
        if(remoteTakeover&&connected){
            controls.roll=remoteRoll;controls.pitch=remotePitch;controls.yaw=remoteYaw;controls.throttle=remoteThrottle;controls.brake=remoteBrake;controls.gearDown=remoteGearDown;controls.clamp();
        }else if(remoteEver&&!connected){
            controls.roll=controls.pitch=controls.yaw=0;controls.throttle=state.onGround?0:.48;controls.brake=state.onGround?1:0;controls.gearDown=state.gearPosition>.5;controls.clamp();
        }else if(demoMode){
            mission.update(state,controls,dt);
        }else{
            controls.roll=controls.pitch=controls.yaw=0;controls.throttle=0;controls.brake=1;controls.gearDown=true;controls.clamp();
        }

        dynamics.step(state,controls,dt);
        updateRunwayPosition(dt);

        if(remoteTakeover&&connected){
            applyOffRunwayDrag(dt);
            evaluateRemoteLanding(wasGround);
        }

        if(!remoteEver&&demoMode&&mission.getPhase()==AutonomousFlightMission.Phase.HANGAR_START&&mission.getPhaseTimeSec()<.12){runwayCrossTrackM=0;runwayAlongTrackM=0;}
    }

    private void updateRunwayPosition(double dt){
        if(!remoteEver)return;
        double v=state.trueAirspeedMps*Math.max(.15,Math.cos(Math.toRadians(state.pitchDeg)));
        double err=Math.toRadians(wrap180(state.headingDeg-RUNWAY_HDG));
        runwayAlongTrackM+=v*Math.cos(err)*dt;
        runwayCrossTrackM+=v*Math.sin(err)*dt;
    }

    private void applyOffRunwayDrag(double dt){
        if(state.onGround&&Math.abs(runwayCrossTrackM)>31){
            double extra=.9+state.trueAirspeedMps*.018;state.trueAirspeedMps=Math.max(0,state.trueAirspeedMps-extra*dt);
            if(Math.abs(runwayCrossTrackM)>85&&state.trueAirspeedMps>43)crash("HIGH-SPEED RUNWAY EXCURSION");
        }
    }

    private void evaluateRemoteLanding(boolean wasGround){
        if(crashed||wasGround)return;
        boolean impact=state.onGround||state.altitudeM<=.06;
        if(!impact)return;
        double sink=Math.max(state.touchdownSinkMps,Math.max(0,-state.verticalSpeedMps));
        double bank=Math.abs(state.rollDeg),pitch=Math.abs(state.pitchDeg),alignment=Math.abs(wrap180(state.headingDeg-RUNWAY_HDG)),x=Math.abs(runwayCrossTrackM);
        if(state.gearPosition<.80){crash("GEAR NOT DOWN AT TOUCHDOWN");return;}
        if(sink>5.8){crash(String.format(Locale.US,"HARD LANDING  %.1f m/s",sink));return;}
        if(bank>14){crash(String.format(Locale.US,"EXCESS BANK  %.0f°",bank));return;}
        if(pitch>16){crash(String.format(Locale.US,"UNSAFE PITCH  %.0f°",pitch));return;}
        if(alignment>20){crash(String.format(Locale.US,"RUNWAY MISALIGNMENT  %.0f°",alignment));return;}
        if(x>33){crash(String.format(Locale.US,"TOUCHDOWN OUTSIDE RUNWAY  %.0f m",x));}
    }

    private void crash(String reason){
        if(crashed)return;crashed=true;crashReason=reason;crashRollTarget=state.rollDeg>=0?34:-34;remoteThrottle=0;remoteBrake=1;
        runOnUiThread(()->{crashBanner.setText("AIRCRAFT CRASH\n"+reason);crashBanner.setVisibility(View.VISIBLE);resetButton.setVisibility(View.VISIBLE);});
    }

    private void resetSimulation(){
        crashed=false;crashReason="";runwayCrossTrackM=runwayAlongTrackM=0;mission.reset(state);controls.gearDown=true;
        crashBanner.setVisibility(View.GONE);resetButton.setVisibility(View.GONE);
        if(connected&&remoteEver){remoteTakeover=true;remoteThrottle=.08f;remoteBrake=1;remoteGearDown=true;}
    }

    private String scenePhase(){
        if(crashed)return"CRASH";
        if(remoteEver){
            if(state.onGround&&runwayAlongTrackM<12&&Math.abs(runwayCrossTrackM)<12)return"HANGAR_START";
            if(state.onGround)return Math.abs(runwayCrossTrackM)>31?"REMOTE_OFFRUNWAY":"REMOTE_GROUND";
            if(state.altitudeM<360&&Math.abs(wrap180(state.headingDeg-RUNWAY_HDG))<75)return"APPROACH_REMOTE";
            return"ORBIT_REMOTE";
        }
        return demoMode?mission.getPhase().name():"HANGAR_START";
    }

    private double sceneAlong(){
        if(remoteEver)return runwayAlongTrackM;
        if(!demoMode)return 0;
        switch(mission.getPhase()){
            case HANGAR_START:return 0;
            case TAXI_OUT:return mission.getPhaseProgress01()*95;
            case RUNWAY_HOLD:return 100;
            case TAKEOFF_ROLL:return 100+mission.getPhaseProgress01()*420;
            default:return 540;
        }
    }

    private void renderState(){
        String phase=scenePhase();double cross=remoteEver?runwayCrossTrackM:0,along=sceneAlong();
        world.setState(state.altitudeM,state.trueAirspeedMps,state.onGround,phase,state.headingDeg,cross,along,crashed,crashReason);
        jet.setTelemetry((float)state.rollDeg,(float)state.pitchDeg,(float)state.headingDeg,(float)state.throttle,50,0,true);
        jet.setControlInputs((float)controls.pitch,(float)controls.roll,(float)controls.yaw,(float)controls.throttle);
        jet.setSimulationState((float)state.gearPosition,(float)state.mainStrutCompression01,(float)state.noseStrutCompression01,(float)state.brake01,state.onGround);
        jet.setFlightMotion((float)state.trueAirspeedMps,(float)state.verticalSpeedMps,state.onGround);jet.setWheelSpeed((float)(state.onGround?state.trueAirspeedMps:0));

        String mode=crashed?"CRASHED":remoteTakeover&&connected?"BT REMOTE FULL CONTROL":remoteEver&&!connected?"REMOTE LINK LOST / SAFE HOLD":demoMode?"DEMO AUTO":"WAITING CONTROLLER";
        hud.setText(String.format(Locale.US,"%s   %s\nALT %.0f m   VS %.1f m/s   SPD %.0f m/s   HDG %03.0f   ROLL %.0f°   PITCH %.0f°\nTHR %.0f%%   GEAR %.0f%%   BRK %.0f%%   X-TRK %.0f m   %s",mode,phase.replace('_',' '),state.altitudeM,state.verticalSpeedMps,state.trueAirspeedMps,state.headingDeg,state.rollDeg,state.pitchDeg,state.throttle*100,state.gearPosition*100,state.brake01*100,cross,state.onGround?"GROUND":"AIR"));
        linkBanner.setText(connected?(remoteTakeover?"● ADVANCED CONTROLLER — MASTER CONTROL":"● BLUETOOTH LINKED"):"○ CONTROLLER LINK WAITING");
    }

    private void requestBtThenStart(){
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN},REQ_BT);else startServer();
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_BT&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startServer();}

    private void startServer(){
        if(bt==null){Toast.makeText(this,"Bluetooth donanımı yok",Toast.LENGTH_LONG).show();return;}
        if(!bt.isEnabled()){startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));Toast.makeText(this,"Bluetooth'u aç. Uçak ekranı ardından controller bekleyecek.",Toast.LENGTH_LONG).show();return;}
        io.execute(()->{
            while(running){
                try{
                    server=bt.listenUsingRfcommWithServiceRecord("AircraftSimulator3D-v53",SIM_UUID);socket=server.accept();connected=true;
                    writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));
                    BufferedReader r=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                    runOnUiThread(()->Toast.makeText(this,"Advanced Controller bağlandı",Toast.LENGTH_SHORT).show());
                    String line;
                    while(running&&(line=r.readLine())!=null)parseRemote(line);
                }catch(Exception ignored){}finally{
                    connected=false;remoteTakeover=false;try{if(socket!=null)socket.close();}catch(Exception ignored){}try{if(server!=null)server.close();}catch(Exception ignored){}socket=null;server=null;writer=null;
                }
            }
        });
    }

    private void parseRemote(String line){
        String[] a=line.split(",");if(a.length<10||!"V3".equals(a[0]))return;
        try{
            lastRemoteSeq=Integer.parseInt(a[1]);remoteRoll=clamp(Float.parseFloat(a[3]),-1,1);remotePitch=clamp(Float.parseFloat(a[4]),-1,1);remoteYaw=clamp(Float.parseFloat(a[5]),-1,1);
            remoteThrottle=clamp(Float.parseFloat(a[6]),0,1);remoteBrake=clamp(Float.parseFloat(a[7]),0,1);remoteGearDown=Integer.parseInt(a[8])!=0;
            remoteEver=true;remoteTakeover=true;
        }catch(Exception ignored){}
    }

    private void sendTelemetry(){
        BufferedWriter w=writer;if(w==null)return;
        String msg=String.format(Locale.US,"T3,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%.2f,%d,%d\n",lastRemoteSeq,state.altitudeM,state.trueAirspeedMps,state.headingDeg,state.rollDeg,state.pitchDeg,state.verticalSpeedMps,state.onGround?1:0,runwayCrossTrackM,crashed?1:0,state.gearPosition>.5?1:0);
        io.execute(()->{try{synchronized(writeLock){if(writer!=null){writer.write(msg);writer.flush();}}}catch(Exception ignored){}});
    }

    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(10);b.setMinWidth(0);return b;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
    private static double wrap180(double d){while(d>180)d-=360;while(d<-180)d+=360;return d;}

    @Override protected void onResume(){super.onResume();if(jet!=null)jet.onResume();}
    @Override protected void onPause(){if(jet!=null)jet.onPause();super.onPause();}
    @Override protected void onDestroy(){running=false;handler.removeCallbacksAndMessages(null);try{if(socket!=null)socket.close();}catch(Exception ignored){}try{if(server!=null)server.close();}catch(Exception ignored){}io.shutdownNow();super.onDestroy();}
}
