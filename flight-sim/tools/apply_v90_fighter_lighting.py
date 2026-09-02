from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RUNTIME=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/FlightRuntimeActivity.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v90 fighter-lighting patch anchor missing: {label}')
    return text.replace(old,new,1)

r=RUNTIME.read_text()

r=rep(r,
'import com.mg.fixturecockpitsim.sim.FlightState;\n',
'import com.mg.fixturecockpitsim.sim.FlightState;\nimport com.mg.fixturecockpitsim.sim.FighterLightingSystem;\n',
'lighting import')

r=rep(r,
'    private final FlightSoundEngine sound=new FlightSoundEngine();\n',
'    private final FlightSoundEngine sound=new FlightSoundEngine();\n    private final FighterLightingSystem lighting=new FighterLightingSystem();\n',
'lighting controller')

r=rep(r,
'    private Jet3DView jet;\n    private TextView hud,crashBanner;\n    private LinearLayout bottomPanel;\n    private Button resetButton,modeButton,linkButton,brakeButton,gearButton;\n',
'    private Jet3DView jet;\n    private FighterLightingOverlayView lightingOverlay;\n    private TextView hud,crashBanner;\n    private LinearLayout bottomPanel,lightingPanel;\n    private Button resetButton,modeButton,linkButton,brakeButton,gearButton,lightButton;\n    private Button navLightButton,strobeLightButton,beaconLightButton,landingLightButton,taxiLightButton,formationLightButton,floodLightButton,hudLightButton;\n',
'lighting view fields')

r=rep(r,
'        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);\n        root.addView(world,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));\n',
'        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);lightingOverlay=new FighterLightingOverlayView(this,lighting);\n        root.addView(world,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(lightingOverlay,new FrameLayout.LayoutParams(-1,-1));\n',
'lighting overlay layering')

r=rep(r,
'        buildBottomPanel(root);setContentView(root);\n',
'        buildLightingPanel(root);buildBottomPanel(root);setContentView(root);\n',
'lighting panel creation')

r=rep(r,
'        modeButton=bottomButton("MANUEL IMU");linkButton=bottomButton("LINK");Button center=bottomButton("IMU 0");Button yawL=bottomButton("YAW ◀");Button yawR=bottomButton("YAW ▶");Button thrM=bottomButton("THR −");Button thrP=bottomButton("THR +");brakeButton=bottomButton("BRAKE");gearButton=bottomButton("GEAR D");Button cam=bottomButton("CAM");\n        Button[] all={modeButton,linkButton,center,yawL,yawR,thrM,thrP,brakeButton,gearButton,cam};for(Button b:all)bottomPanel.addView(b,new LinearLayout.LayoutParams(0,-1,1f));\n',
'        modeButton=bottomButton("MANUEL IMU");linkButton=bottomButton("LINK");Button center=bottomButton("IMU 0");Button yawL=bottomButton("YAW ◀");Button yawR=bottomButton("YAW ▶");Button thrM=bottomButton("THR −");Button thrP=bottomButton("THR +");brakeButton=bottomButton("BRAKE");gearButton=bottomButton("GEAR D");Button cam=bottomButton("CAM");lightButton=bottomButton("LGT");\n        Button[] all={modeButton,linkButton,center,yawL,yawR,thrM,thrP,brakeButton,gearButton,cam,lightButton};for(Button b:all)bottomPanel.addView(b,new LinearLayout.LayoutParams(0,-1,1f));\n',
'bottom light button')

r=rep(r,
'        gearButton.setOnClickListener(v->{if(requireLocalManual()){localGearDown=!localGearDown;updateButtons();}});cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%4;jet.setCameraMode(cameraMode);});updateButtons();\n    }\n\n    private void hold(Button b,Runnable press,Runnable release)',
'        gearButton.setOnClickListener(v->{if(requireLocalManual()){localGearDown=!localGearDown;updateButtons();}});cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%4;jet.setCameraMode(cameraMode);});lightButton.setOnClickListener(v->{lightingPanel.setVisibility(lightingPanel.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);updateLightingButtons();});updateButtons();\n    }\n\n    private void buildLightingPanel(FrameLayout root){\n        lightingPanel=new LinearLayout(this);lightingPanel.setOrientation(LinearLayout.HORIZONTAL);lightingPanel.setGravity(Gravity.CENTER);lightingPanel.setPadding(dp(3),dp(3),dp(3),dp(3));lightingPanel.setBackgroundColor(0xe6101519);\n        navLightButton=bottomButton("NAV");strobeLightButton=bottomButton("STRB");beaconLightButton=bottomButton("BCN");landingLightButton=bottomButton("LAND");taxiLightButton=bottomButton("TAXI");formationLightButton=bottomButton("FORM");floodLightButton=bottomButton("FLOOD");hudLightButton=bottomButton("HUD");\n        Button[] lights={navLightButton,strobeLightButton,beaconLightButton,landingLightButton,taxiLightButton,formationLightButton,floodLightButton,hudLightButton};for(Button b:lights)lightingPanel.addView(b,new LinearLayout.LayoutParams(0,-1,1f));\n        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,dp(50),Gravity.BOTTOM);lp.setMargins(dp(4),0,dp(4),dp(64));root.addView(lightingPanel,lp);lightingPanel.setVisibility(View.GONE);\n        navLightButton.setOnClickListener(v->{lighting.navigation=!lighting.navigation;updateLightingButtons();});\n        strobeLightButton.setOnClickListener(v->{lighting.strobe=!lighting.strobe;updateLightingButtons();});\n        beaconLightButton.setOnClickListener(v->{lighting.beacon=!lighting.beacon;updateLightingButtons();});\n        landingLightButton.setOnClickListener(v->{lighting.landing=!lighting.landing;updateLightingButtons();});\n        taxiLightButton.setOnClickListener(v->{lighting.taxi=!lighting.taxi;updateLightingButtons();});\n        formationLightButton.setOnClickListener(v->{lighting.formation=!lighting.formation;updateLightingButtons();});\n        floodLightButton.setOnClickListener(v->{lighting.cycleFlood();applyLightingPresentation();updateLightingButtons();});\n        hudLightButton.setOnClickListener(v->{lighting.cycleHud();applyLightingPresentation();updateLightingButtons();});\n        updateLightingButtons();\n    }\n\n    private void updateLightingButtons(){\n        if(navLightButton==null)return;\n        navLightButton.setText(lighting.navigation?"NAV ●":"NAV");strobeLightButton.setText(lighting.strobe?"STRB ●":"STRB");beaconLightButton.setText(lighting.beacon?"BCN ●":"BCN");landingLightButton.setText(lighting.landing?"LAND ●":"LAND");taxiLightButton.setText(lighting.taxi?"TAXI ●":"TAXI");formationLightButton.setText(lighting.formation?"FORM ●":"FORM");\n        floodLightButton.setText(String.format(Locale.US,"FLOOD %.0f%%",lighting.floodBrightness()*100));hudLightButton.setText(String.format(Locale.US,"HUD %.0f%%",lighting.hudBrightness()*100));\n        if(lightButton!=null)lightButton.setText((lighting.navigation||lighting.strobe||lighting.beacon||lighting.landing||lighting.taxi||lighting.formation||lighting.floodBrightness()>0)?"LGT ●":"LGT");\n    }\n\n    private void applyLightingPresentation(){\n        if(hud!=null){int a=(int)(255*lighting.hudBrightness());hud.setTextColor(lighting.getHudStep()==0?Color.argb(a,255,255,255):Color.argb(a,135,255,180));}\n        int flood=(int)(lighting.floodBrightness()*70);if(bottomPanel!=null)bottomPanel.setBackgroundColor(Color.argb(220,8+flood/2,13+flood/3,16+flood/4));if(lightingPanel!=null)lightingPanel.setBackgroundColor(Color.argb(232,10+flood/2,21+flood/3,25+flood/4));\n    }\n\n    private void hold(Button b,Runnable press,Runnable release)',
'lighting panel and switches')

# After v89, this render line includes the real fighter control-surface state.
old_render='        jet.setTelemetry((float)state.rollDeg,(float)state.pitchDeg,(float)state.headingDeg,(float)state.throttle,50,0,true);jet.setControlInputs((float)controls.pitch,(float)controls.roll,(float)controls.yaw,(float)controls.throttle);jet.setFighterSurfaceState((float)state.leftStabilatorDeg,(float)state.rightStabilatorDeg,(float)state.leftRudderDeg,(float)state.rightRudderDeg,(float)state.leftFlaperonDeg,(float)state.rightFlaperonDeg,(float)state.leftLeadingEdgeFlapDeg,(float)state.rightLeadingEdgeFlapDeg,(float)state.speedBrakeDeg);jet.setSimulationState((float)state.gearPosition,(float)state.mainStrutCompression01,(float)state.noseStrutCompression01,(float)state.brake01,state.onGround);jet.setFlightMotion((float)state.trueAirspeedMps,(float)state.verticalSpeedMps,state.onGround);jet.setWheelSpeed((float)(state.onGround?state.trueAirspeedMps:0));\n'
new_render=old_render+'        lightingOverlay.setAircraftState(cameraMode,state.onGround,(float)state.gearPosition,(float)state.trueAirspeedMps);applyLightingPresentation();\n'
r=rep(r,old_render,new_render,'lighting render state')

RUNTIME.write_text(r)

g=GRADLE.read_text()
g=rep(g,'        versionCode 89\n','        versionCode 90\n','version code')
g=rep(g,"        versionName '26.7-avm19.0-fighter-control-surfaces'\n","        versionName '26.8-avm20.0-fighter-lighting'\n",'version name')
GRADLE.write_text(g)

print('v90 fighter lighting applied: independent nav/strobe/beacon/landing/taxi/formation lights, cockpit flood levels and HUD night brightness')
