package com.mg.fixturecockpitsim;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.mg.fixturecockpitsim.sim.AutonomousFlightMission;
import com.mg.fixturecockpitsim.sim.FlightControls;
import com.mg.fixturecockpitsim.sim.FlightDynamicsEngine;
import com.mg.fixturecockpitsim.sim.FlightState;

/**
 * AVM-17.1 third screen: pilot/cockpit presentation of the same autonomous demo flight.
 * It deliberately shares the production world, weather, 3-D aircraft and flight dynamics classes.
 */
public final class PilotCockpitActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final FlightDynamicsEngine dynamics=new FlightDynamicsEngine();
    private final FlightState state=new FlightState();
    private final FlightControls controls=new FlightControls();
    private final AutonomousFlightMission mission=new AutonomousFlightMission();

    private AirfieldWorldView world;
    private WeatherEffectsView weather;
    private Jet3DView jet;
    private PilotCockpitOverlayView cockpit;
    private boolean running;
    private long lastNs;
    private int cameraMode=Jet3DView.CAMERA_CHASE;

    private final Runnable loop=new Runnable(){@Override public void run(){
        if(!running)return;
        long now=System.nanoTime();
        double dt=lastNs==0?.02:Math.min(.05,Math.max(.005,(now-lastNs)/1e9));lastNs=now;
        mission.update(state,controls,dt);
        dynamics.step(state,controls,dt);
        render();
        handler.postDelayed(this,20);
    }};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mission.reset(state);controls.gearDown=true;

        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(2,7,10));
        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);cockpit=new PilotCockpitOverlayView(this);
        root.addView(world,new FrameLayout.LayoutParams(-1,-1));
        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));
        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));
        root.addView(cockpit,new FrameLayout.LayoutParams(-1,-1));

        Button back=smallButton("GERİ");
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(76),dp(42),Gravity.TOP|Gravity.RIGHT);bp.setMargins(0,dp(9),dp(10),0);root.addView(back,bp);back.setOnClickListener(v->finish());
        Button cam=smallButton("CAM");
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(72),dp(42),Gravity.TOP|Gravity.RIGHT);cp.setMargins(0,dp(58),dp(10),0);root.addView(cam,cp);cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%4;jet.setCameraMode(cameraMode);});

        setContentView(root);jet.setCameraMode(cameraMode);render();
    }

    @Override protected void onResume(){super.onResume();running=true;lastNs=0;if(jet!=null)jet.onResume();handler.removeCallbacks(loop);handler.post(loop);}
    @Override protected void onPause(){running=false;handler.removeCallbacks(loop);if(jet!=null)jet.onPause();super.onPause();}
    @Override protected void onDestroy(){running=false;handler.removeCallbacksAndMessages(null);super.onDestroy();}

    private void render(){
        String phase=mission.getPhase().name();double along=autoSceneAlong();
        // Pilot demo owns the centerline: the autonomous landing cannot touch down off the runway.
        world.setState(state.altitudeM,state.trueAirspeedMps,state.onGround,phase,state.headingDeg,state.pitchDeg,0,along,false,"");
        jet.setTelemetry((float)state.rollDeg,(float)state.pitchDeg,(float)state.headingDeg,(float)state.throttle,50,0,true);
        jet.setControlInputs((float)controls.pitch,(float)controls.roll,(float)controls.yaw,(float)controls.throttle);
        jet.setSimulationState((float)state.gearPosition,(float)state.mainStrutCompression01,(float)state.noseStrutCompression01,(float)state.brake01,state.onGround);
        jet.setFlightMotion((float)state.trueAirspeedMps,(float)state.verticalSpeedMps,state.onGround);
        jet.setWheelSpeed((float)(state.onGround?state.trueAirspeedMps:0));
        cockpit.setState(state.altitudeM,state.trueAirspeedMps,state.headingDeg,state.rollDeg,state.pitchDeg,state.verticalSpeedMps,
                state.throttle,state.gearPosition,state.brake01,state.onGround,phase,weather.getModeLabel(),state.timeSec);
    }

    private double autoSceneAlong(){
        switch(mission.getPhase()){
            case HANGAR_START:return 14;
            case TAXI_OUT:return 14+mission.getPhaseProgress01()*81;
            case RUNWAY_HOLD:return 100;
            case TAKEOFF_ROLL:return 100+mission.getPhaseProgress01()*420;
            default:return 540;
        }
    }

    private Button smallButton(String text){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(11);b.setTextColor(Color.WHITE);b.setBackgroundColor(0xaa263238);return b;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
