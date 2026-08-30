package com.mg.fixturecockpitsim;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
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

/** AVM-12.4 runtime: post-hangar start, orientation-aware IMU manual flight, AUTO and optional BT takeover. */
public final class FlightRuntimeActivity extends Activity implements SensorEventListener {
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
    private volatile boolean running=true,connected,remoteTakeover,linkArmed,serverLoopRunning;
    private volatile float remoteRoll,remotePitch,remoteYaw,remoteThrottle=.08f,remoteBrake;
    private volatile boolean remoteGearDown=true;
    private volatile int lastRemoteSeq;

    private SensorManager sensors;
    private Sensor rotationSensor;
    private volatile float rawRollDeg,rawPitchDeg,rawYawDeg,zeroRollDeg,zeroPitchDeg,zeroYawDeg;
    private volatile float imuRoll,imuPitch,imuYaw;
    private volatile boolean imuCentered,imuHasSample,imuCenterPending=true;
    private volatile int imuDisplayRotation=Surface.ROTATION_0;

    private long lastSimNs,lastTelemetryMs;
    private boolean demoMode=true,localManual,crashed,freeNavSeeded,hangarDeparted=true;
    private String crashReason="";
    private double runwayCrossTrackM,runwayAlongTrackM,crashRollTarget;
    private double localThrottle=.10,localBrake,localYawHold;
    private boolean localGearDown=true;
    private int cameraMode=Jet3DView.CAMERA_CHASE;

    private AirfieldWorldView world;
    private Jet3DView jet;
    private TextView hud,crashBanner;
    private LinearLayout bottomPanel;
    private Button resetButton,modeButton,linkButton,brakeButton,gearButton;

    private final Runnable simLoop=new Runnable(){@Override public void run(){
        if(!running)return;
        long now=System.nanoTime();
        double dt=lastSimNs==0?.02:Math.min(.05,Math.max(.005,(now-lastSimNs)/1e9));
        lastSimNs=now;
        stepSimulation(dt);renderState();
        if(connected&&System.currentTimeMillis()-lastTelemetryMs>=90){lastTelemetryMs=System.currentTimeMillis();sendTelemetry();}
        handler.postDelayed(this,20);
    }};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        demoMode=getIntent()==null||getIntent().getBooleanExtra(LauncherActivity.EXTRA_DEMO_MODE,true);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        bt=BluetoothAdapter.getDefaultAdapter();
        sensors=(SensorManager)getSystemService(SENSOR_SERVICE);
        rotationSensor=sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if(rotationSensor==null)rotationSensor=sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mission.reset(state);controls.gearDown=true;hangarDeparted=true;

        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(3,9,13));
        world=new AirfieldWorldView(this);jet=new Jet3DView(this);root.addView(world,new FrameLayout.LayoutParams(-1,-1));root.addView(jet,new FrameLayout.LayoutParams(-1,-1));
        hud=new TextView(this);hud.setTextColor(Color.WHITE);hud.setTextSize(13);hud.setPadding(dp(12),dp(8),dp(12),dp(8));hud.setBackgroundColor(0x72000000);FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT);hp.setMargins(dp(10),dp(10),0,0);root.addView(hud,hp);
        Button back=button("GERİ");FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(78),dp(43),Gravity.TOP|Gravity.RIGHT);bp.setMargins(0,dp(8),dp(10),0);root.addView(back,bp);back.setOnClickListener(v->finish());
        resetButton=button("RESET");FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(dp(78),dp(43),Gravity.TOP|Gravity.RIGHT);rp.setMargins(0,dp(56),dp(10),0);root.addView(resetButton,rp);resetButton.setVisibility(View.GONE);resetButton.setOnClickListener(v->resetSimulation());
        crashBanner=new TextView(this);crashBanner.setTextColor(Color.WHITE);crashBanner.setTextSize(25);crashBanner.setGravity(Gravity.CENTER);crashBanner.setBackgroundColor(0xaa7c170d);crashBanner.setVisibility(View.GONE);FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(480),dp(105),Gravity.CENTER);root.addView(crashBanner,cp);
        buildBottomPanel(root);setContentView(root);
        jet.setCameraMode(cameraMode);jet.setSimulationState(1,0,0,1,true);jet.setFlightMotion(0,0,true);jet.setControlInputs(0,0,0,0);
        renderState();handler.post(simLoop);
        String imuText=rotationSensor==null?"IMU sensörü bulunamadı — AUTO/BT kullanılabilir.":"MANUEL IMU'da o anki telefon tutuşu nötr alınır; yatay/dikey ekran yönü otomatik eşlenir.";
        Toast.makeText(this,"Uçuş hangar sonrası taksiden başlar. "+imuText,Toast.LENGTH_LONG).show();
    }

    private void buildBottomPanel(FrameLayout root){
        bottomPanel=new LinearLayout(this);bottomPanel.setOrientation(LinearLayout.HORIZONTAL);bottomPanel.setGravity(Gravity.CENTER);bottomPanel.setPadding(dp(3),dp(3),dp(3),dp(3));bottomPanel.setBackgroundColor(0xd6080d10);
        modeButton=bottomButton("MANUEL IMU");linkButton=bottomButton("LINK");Button center=bottomButton("IMU 0");Button yawL=bottomButton("YAW ◀");Button yawR=bottomButton("YAW ▶");Button thrM=bottomButton("THR −");Button thrP=bottomButton("THR +");brakeButton=bottomButton("BRAKE");gearButton=bottomButton("GEAR D");Button cam=bottomButton("CAM");
        Button[] all={modeButton,linkButton,center,yawL,yawR,thrM,thrP,brakeButton,gearButton,cam};for(Button b:all)bottomPanel.addView(b,new LinearLayout.LayoutParams(0,-1,1f));
        FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(-1,dp(62),Gravity.BOTTOM);root.addView(bottomPanel,pp);
        modeButton.setOnClickListener(v->toggleLocalManual());linkButton.setOnClickListener(v->toggleLink());center.setOnClickListener(v->{requestImuCenter();Toast.makeText(this,"IMU nötr: mevcut telefon tutuşu",Toast.LENGTH_SHORT).show();});
        hold(yawL,()->localYawHold=-1,()->localYawHold=0);hold(yawR,()->localYawHold=1,()->localYawHold=0);
        thrM.setOnClickListener(v->{if(requireLocalManual())localThrottle=Math.max(0,localThrottle-.05);});thrP.setOnClickListener(v->{if(requireLocalManual())localThrottle=Math.min(1,localThrottle+.05);});
        brakeButton.setOnTouchListener((v,e)->{if(!localManual)return false;if(e.getAction()==MotionEvent.ACTION_DOWN){localBrake=1;updateButtons();return true;}if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){localBrake=0;updateButtons();return true;}return true;});
        gearButton.setOnClickListener(v->{if(requireLocalManual()){localGearDown=!localGearDown;updateButtons();}});cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%4;jet.setCameraMode(cameraMode);});updateButtons();
    }

    private void hold(Button b,Runnable press,Runnable release){b.setOnTouchListener((v,e)->{if(!localManual)return false;if(e.getAction()==MotionEvent.ACTION_DOWN){press.run();return true;}if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){release.run();return true;}return true;});}
    private boolean requireLocalManual(){if(localManual)return true;Toast.makeText(this,"Önce MANUEL IMU moduna geç",Toast.LENGTH_SHORT).show();return false;}

    private void toggleLocalManual(){
        if(remoteTakeover&&connected){Toast.makeText(this,"Bluetooth master aktif. Önce LINK'i kapat.",Toast.LENGTH_SHORT).show();return;}
        if(linkArmed){Toast.makeText(this,"LINK WAIT aktif. Önce LINK'i kapat.",Toast.LENGTH_SHORT).show();return;}
        if(rotationSensor==null){Toast.makeText(this,"Bu cihazda uygun yönelim sensörü yok.",Toast.LENGTH_LONG).show();return;}
        localManual=!localManual;
        if(localManual){
            seedFreeNavigation();localThrottle=Math.max(.08,state.throttle);localGearDown=state.gearPosition>.5;localBrake=0;localYawHold=0;
            requestImuCenter();
            Toast.makeText(this,"LOCAL IMU — bu tutuş nötr. Kendine yatır: PITCH UP • sağa yatır: ROLL RIGHT",Toast.LENGTH_LONG).show();
        }else{localBrake=0;localYawHold=0;imuRoll=imuPitch=imuYaw=0;Toast.makeText(this,"DEMO AUTO yeniden devrede",Toast.LENGTH_SHORT).show();}
        updateButtons();
    }

    private void toggleLink(){
        if(linkArmed||connected){cancelLink();Toast.makeText(this,"LINK kapatıldı — AUTO kullanıma hazır",Toast.LENGTH_SHORT).show();return;}
        if(localManual){localManual=false;localBrake=0;localYawHold=0;}
        seedFreeNavigation();linkArmed=true;remoteTakeover=false;updateButtons();requestBtThenStart();Toast.makeText(this,"LINK WAIT — uçak HOLD'da, ikinci telefon bekleniyor",Toast.LENGTH_LONG).show();
    }

    private void stepSimulation(double dt){
        if(crashed){state.throttle=0;state.brake01=1;state.altitudeM=0;state.verticalSpeedMps=0;state.onGround=true;state.trueAirspeedMps=Math.max(0,state.trueAirspeedMps-19*dt);state.rollDeg+=(crashRollTarget-state.rollDeg)*Math.min(1,dt*2.4);state.pitchDeg+=(-11-state.pitchDeg)*Math.min(1,dt*2.0);return;}
        boolean wasGround=state.onGround;
        if(remoteTakeover&&connected&&linkArmed){controls.roll=remoteRoll;controls.pitch=remotePitch;controls.yaw=remoteYaw;controls.throttle=remoteThrottle;controls.brake=remoteBrake;controls.gearDown=remoteGearDown;controls.clamp();}
        else if(linkArmed){controls.roll=controls.pitch=controls.yaw=0;controls.throttle=state.onGround?0:.48;controls.brake=state.onGround?1:0;controls.gearDown=state.onGround||state.gearPosition>.5;controls.clamp();}
        else if(localManual){controls.roll=imuRoll;controls.pitch=imuPitch;controls.yaw=localYawHold!=0?localYawHold:imuYaw;controls.throttle=localThrottle;controls.brake=localBrake;controls.gearDown=localGearDown;controls.clamp();}
        else if(demoMode){mission.update(state,controls,dt);hangarDeparted=true;}
        else{controls.roll=controls.pitch=controls.yaw=0;controls.throttle=0;controls.brake=1;controls.gearDown=true;controls.clamp();}
        dynamics.step(state,controls,dt);
        if(freeNavSeeded)updateRunwayPosition(dt);
        hangarDeparted=true;
        if(freeNavSeeded)applyOffRunwayDrag(dt,remoteTakeover&&connected&&linkArmed);
        if(remoteTakeover&&connected&&linkArmed)evaluateRemoteLanding(wasGround);
    }

    private void seedFreeNavigation(){if(freeNavSeeded)return;runwayAlongTrackM=Math.max(14,autoSceneAlong());runwayCrossTrackM=0;freeNavSeeded=true;hangarDeparted=true;}
    private void updateRunwayPosition(double dt){double v=state.trueAirspeedMps*Math.max(.15,Math.cos(Math.toRadians(state.pitchDeg)));double err=Math.toRadians(wrap180(state.headingDeg-RUNWAY_HDG));runwayAlongTrackM+=v*Math.cos(err)*dt;runwayCrossTrackM+=v*Math.sin(err)*dt;}
    private void applyOffRunwayDrag(double dt,boolean allowCrash){if(state.onGround&&Math.abs(runwayCrossTrackM)>31){double extra=.9+state.trueAirspeedMps*.018;state.trueAirspeedMps=Math.max(0,state.trueAirspeedMps-extra*dt);if(allowCrash&&Math.abs(runwayCrossTrackM)>85&&state.trueAirspeedMps>43)crash("HIGH-SPEED RUNWAY EXCURSION");}}

    private void evaluateRemoteLanding(boolean wasGround){
        if(crashed||wasGround)return;boolean impact=state.onGround||state.altitudeM<=.06;if(!impact)return;
        double sink=Math.max(state.touchdownSinkMps,Math.max(0,-state.verticalSpeedMps)),bank=Math.abs(state.rollDeg),pitch=Math.abs(state.pitchDeg),alignment=Math.abs(wrap180(state.headingDeg-RUNWAY_HDG)),x=Math.abs(runwayCrossTrackM);
        if(state.gearPosition<.80){crash("GEAR NOT DOWN AT TOUCHDOWN");return;}if(sink>5.8){crash(String.format(Locale.US,"HARD LANDING  %.1f m/s",sink));return;}if(bank>14){crash(String.format(Locale.US,"EXCESS BANK  %.0f°",bank));return;}if(pitch>16){crash(String.format(Locale.US,"UNSAFE PITCH  %.0f°",pitch));return;}if(alignment>20){crash(String.format(Locale.US,"RUNWAY MISALIGNMENT  %.0f°",alignment));return;}if(x>33)crash(String.format(Locale.US,"TOUCHDOWN OUTSIDE RUNWAY  %.0f m",x));
    }

    private void crash(String reason){if(crashed)return;crashed=true;crashReason=reason;crashRollTarget=state.rollDeg>=0?34:-34;remoteThrottle=0;remoteBrake=1;runOnUiThread(()->{crashBanner.setText("AIRCRAFT CRASH\n"+reason);crashBanner.setVisibility(View.VISIBLE);resetButton.setVisibility(View.VISIBLE);});}
    private void resetSimulation(){crashed=false;crashReason="";runwayCrossTrackM=0;runwayAlongTrackM=14;freeNavSeeded=false;hangarDeparted=true;mission.reset(state);controls.gearDown=true;localManual=false;localThrottle=.10;localBrake=0;localYawHold=0;localGearDown=true;imuRoll=imuPitch=imuYaw=0;imuCenterPending=true;crashBanner.setVisibility(View.GONE);resetButton.setVisibility(View.GONE);if(connected&&linkArmed){seedFreeNavigation();remoteTakeover=true;remoteThrottle=.08f;remoteBrake=1;remoteGearDown=true;}updateButtons();}

    private String scenePhase(){
        if(crashed)return"CRASH";
        if(!freeNavSeeded)return demoMode?mission.getPhase().name():"FREE_GROUND";
        if(state.onGround)return Math.abs(runwayCrossTrackM)>31?"REMOTE_OFFRUNWAY":"FREE_GROUND";
        if(state.altitudeM<360&&Math.abs(wrap180(state.headingDeg-RUNWAY_HDG))<75)return"APPROACH_REMOTE";
        return"ORBIT_REMOTE";
    }
    private double autoSceneAlong(){if(!demoMode)return 14;switch(mission.getPhase()){case HANGAR_START:return 14;case TAXI_OUT:return 14+mission.getPhaseProgress01()*81;case RUNWAY_HOLD:return 100;case TAKEOFF_ROLL:return 100+mission.getPhaseProgress01()*420;default:return 540;}}
    private double sceneAlong(){return freeNavSeeded?runwayAlongTrackM:autoSceneAlong();}

    private void renderState(){
        String phase=scenePhase();double cross=freeNavSeeded?runwayCrossTrackM:0,along=sceneAlong();
        world.setState(state.altitudeM,state.trueAirspeedMps,state.onGround,phase,state.headingDeg,cross,along,crashed,crashReason);
        jet.setTelemetry((float)state.rollDeg,(float)state.pitchDeg,(float)state.headingDeg,(float)state.throttle,50,0,true);jet.setControlInputs((float)controls.pitch,(float)controls.roll,(float)controls.yaw,(float)controls.throttle);jet.setSimulationState((float)state.gearPosition,(float)state.mainStrutCompression01,(float)state.noseStrutCompression01,(float)state.brake01,state.onGround);jet.setFlightMotion((float)state.trueAirspeedMps,(float)state.verticalSpeedMps,state.onGround);jet.setWheelSpeed((float)(state.onGround?state.trueAirspeedMps:0));
        String mode=crashed?"CRASHED":remoteTakeover&&connected&&linkArmed?"BT REMOTE MASTER":linkArmed?(connected?"LINKED / WAITING V3 DATA":"LINK WAIT / HOLD"):localManual?"LOCAL IMU MANUAL":"DEMO AUTO";
        String imu=localManual?String.format(Locale.US,"   IMU R%+.0f%% P%+.0f%%",imuRoll*100,imuPitch*100):"";
        hud.setText(String.format(Locale.US,"%s   %s%s\nALT %.0f m   VS %.1f m/s   SPD %.0f m/s   HDG %03.0f   ROLL %.0f°   PITCH %.0f°\nTHR %.0f%%   GEAR %.0f%%   BRK %.0f%%   X-TRK %.0f m   %s",mode,phase.replace('_',' '),imu,state.altitudeM,state.verticalSpeedMps,state.trueAirspeedMps,state.headingDeg,state.rollDeg,state.pitchDeg,state.throttle*100,state.gearPosition*100,state.brake01*100,cross,state.onGround?"GROUND":"AIR"));updateButtons();
    }

    private void updateButtons(){if(modeButton==null)return;modeButton.setText(localManual?"OTOMATİK":"MANUEL IMU");linkButton.setText(remoteTakeover&&connected&&linkArmed?"BT MASTER":linkArmed?(connected?"LINKED":"LINK WAIT"):"LINK");brakeButton.setText(localBrake>.5?"BRAKE ●":"BRAKE");gearButton.setText(localGearDown?"GEAR D":"GEAR U");}

    private void requestImuCenter(){
        imuRoll=imuPitch=imuYaw=0;imuCenterPending=true;imuCentered=false;
        if(imuHasSample)centerImuNow(currentDisplayRotation());
    }
    private void centerImuNow(int rotation){zeroRollDeg=rawRollDeg;zeroPitchDeg=rawPitchDeg;zeroYawDeg=rawYawDeg;imuDisplayRotation=rotation;imuCentered=true;imuCenterPending=false;imuRoll=imuPitch=imuYaw=0;}

    @Override public void onSensorChanged(SensorEvent e){
        if(e.sensor!=rotationSensor)return;
        float[] rm=new float[9],screenRm=new float[9],ori=new float[3];
        SensorManager.getRotationMatrixFromVector(rm,e.values);
        int rotation=currentDisplayRotation();
        int x=SensorManager.AXIS_X,y=SensorManager.AXIS_Y;
        if(rotation==Surface.ROTATION_90){x=SensorManager.AXIS_Y;y=SensorManager.AXIS_MINUS_X;}
        else if(rotation==Surface.ROTATION_180){x=SensorManager.AXIS_MINUS_X;y=SensorManager.AXIS_MINUS_Y;}
        else if(rotation==Surface.ROTATION_270){x=SensorManager.AXIS_MINUS_Y;y=SensorManager.AXIS_X;}
        if(!SensorManager.remapCoordinateSystem(rm,x,y,screenRm))return;
        SensorManager.getOrientation(screenRm,ori);
        rawYawDeg=(float)Math.toDegrees(ori[0]);rawPitchDeg=(float)Math.toDegrees(ori[1]);rawRollDeg=(float)Math.toDegrees(ori[2]);imuHasSample=true;

        if(rotation!=imuDisplayRotation&&imuCentered){imuCenterPending=true;imuCentered=false;}
        if(imuCenterPending||!imuCentered){centerImuNow(rotation);return;}

        float dRoll=wrap180f(rawRollDeg-zeroRollDeg);
        float dPitch=wrap180f(rawPitchDeg-zeroPitchDeg);
        float dYaw=wrap180f(rawYawDeg-zeroYawDeg);

        float tr=axisCurve(dRoll,38f,2.0f);
        float tp=axisCurve(dPitch,26f,1.6f);
        float ty=axisCurve(dYaw,52f,3.0f);

        imuRoll=lerp(imuRoll,tr,.24f);
        imuPitch=lerp(imuPitch,tp,.22f);
        imuYaw=lerp(imuYaw,ty,.14f);
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}

    private int currentDisplayRotation(){try{return getWindowManager().getDefaultDisplay().getRotation();}catch(Exception e){return Surface.ROTATION_0;}}
    private static float axisCurve(float deg,float fullScale,float deadZone){
        float a=Math.abs(deg);if(a<=deadZone)return 0;
        float n=clamp((a-deadZone)/Math.max(1f,fullScale-deadZone),0,1);
        float curved=.58f*n+.42f*n*n*n;
        return Math.copySign(curved,deg);
    }

    private void requestBtThenStart(){if(bt==null){Toast.makeText(this,"Bluetooth donanımı yok",Toast.LENGTH_LONG).show();linkArmed=false;updateButtons();return;}if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN},REQ_BT);return;}startServer();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_BT&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED&&linkArmed)startServer();}

    private void startServer(){
        if(!linkArmed||serverLoopRunning||bt==null)return;
        if(!bt.isEnabled()){startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));Toast.makeText(this,"Bluetooth'u aç; LINK WAIT açık kalacak",Toast.LENGTH_LONG).show();return;}
        serverLoopRunning=true;
        io.execute(()->{try{while(running&&linkArmed){try{server=bt.listenUsingRfcommWithServiceRecord("AircraftSimulator3D-v56",SIM_UUID);socket=server.accept();if(!running||!linkArmed)break;connected=true;remoteTakeover=false;writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));BufferedReader r=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));runOnUiThread(()->{updateButtons();Toast.makeText(this,"Bluetooth bağlı — kumanda verisi bekleniyor",Toast.LENGTH_SHORT).show();});String line;while(running&&linkArmed&&(line=r.readLine())!=null)parseRemote(line);}catch(Exception ignored){}finally{connected=false;remoteTakeover=false;closeSocketAndServer();runOnUiThread(this::updateButtons);}}}finally{serverLoopRunning=false;}});
    }

    private void parseRemote(String line){String[] a=line.split(",");if(a.length<10||!"V3".equals(a[0])||!linkArmed)return;try{lastRemoteSeq=Integer.parseInt(a[1]);remoteRoll=clamp(Float.parseFloat(a[3]),-1,1);remotePitch=clamp(Float.parseFloat(a[4]),-1,1);remoteYaw=clamp(Float.parseFloat(a[5]),-1,1);remoteThrottle=clamp(Float.parseFloat(a[6]),0,1);remoteBrake=clamp(Float.parseFloat(a[7]),0,1);remoteGearDown=Integer.parseInt(a[8])!=0;remoteTakeover=true;seedFreeNavigation();}catch(Exception ignored){}}
    private void sendTelemetry(){BufferedWriter w=writer;if(w==null)return;String msg=String.format(Locale.US,"T3,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%.2f,%d,%.2f\n",lastRemoteSeq,state.altitudeM,state.trueAirspeedMps,state.headingDeg,state.rollDeg,state.pitchDeg,state.verticalSpeedMps,state.onGround?1:0,runwayCrossTrackM,crashed?1:0,state.gearPosition);try{synchronized(writeLock){if(writer!=null){writer.write(msg);writer.flush();}}}catch(Exception ignored){}}
    private void cancelLink(){linkArmed=false;remoteTakeover=false;connected=false;closeSocketAndServer();updateButtons();}
    private void closeSocketAndServer(){try{if(socket!=null)socket.close();}catch(Exception ignored){}try{if(server!=null)server.close();}catch(Exception ignored){}socket=null;server=null;writer=null;}

    @Override protected void onResume(){super.onResume();imuCenterPending=true;imuCentered=false;if(rotationSensor!=null)sensors.registerListener(this,rotationSensor,SensorManager.SENSOR_DELAY_GAME);boolean allowed=Build.VERSION.SDK_INT<31||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;if(linkArmed&&allowed&&bt!=null&&bt.isEnabled()&&!serverLoopRunning&&!connected)startServer();}
    @Override protected void onPause(){if(sensors!=null)sensors.unregisterListener(this);super.onPause();}
    @Override protected void onDestroy(){running=false;handler.removeCallbacksAndMessages(null);linkArmed=false;closeSocketAndServer();if(sensors!=null)sensors.unregisterListener(this);io.shutdownNow();super.onDestroy();}

    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(9);b.setMinWidth(0);b.setPadding(dp(2),0,dp(2),0);return b;}
    private Button bottomButton(String s){Button b=button(s);b.setTextColor(Color.WHITE);b.setBackgroundColor(0xff5d5d60);return b;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
    private static float wrap180f(float d){while(d>180)d-=360;while(d<-180)d+=360;return d;}
    private static double wrap180(double d){while(d>180)d-=360;while(d<-180)d+=360;return d;}
}
