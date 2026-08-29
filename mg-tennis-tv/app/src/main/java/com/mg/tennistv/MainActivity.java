package com.mg.tennistv;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.*;
import android.hardware.display.DisplayManager;
import android.provider.Settings;
import android.bluetooth.*;
import android.view.*;
import android.webkit.*;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accel, gyro, rotation;
    private float gyroMag=0f,gx=0f,gy=0f,gz=0f,tilt=0f,soloNoise=0.35f;
    private int soloNoiseSamples=0;
    private long lastSwing=0L,lastTiltPush=0L;
    private WebView web;
    private volatile boolean pageReady=false;
    private BluetoothAdapter bt;
    private BluetoothSocket btSocket;
    private BluetoothServerSocket serverSocket;
    private volatile PrintWriter hostOut;
    private volatile boolean controllerMode=false;
    private volatile boolean externalRacketHost=false;
    private volatile boolean visionEnabled=false;
    private volatile boolean pingRunning=false;
    private volatile long latencyMs=0L;
    private PoseIntentTracker poseTracker;
    private final TennisIntentEngine intentEngine=new TennisIntentEngine();
    private volatile long poseTime=0L;
    private DisplayManager displayManager;
    private Button castButton;
    private static final UUID UUID_GAME=UUID.fromString("6f4f4d30-9c4e-4f99-9e96-1e2ac4c1a501");

    private final DisplayManager.DisplayListener displayListener=new DisplayManager.DisplayListener(){
        @Override public void onDisplayAdded(int displayId){updateCastButton();}
        @Override public void onDisplayRemoved(int displayId){updateCastButton();}
        @Override public void onDisplayChanged(int displayId){updateCastButton();}
    };

    @SuppressLint({"SetJavaScriptEnabled","JavascriptInterface"})
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        web=new WebView(this);web.setBackgroundColor(Color.BLACK);web.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setLoadWithOverviewMode(true);s.setUseWideViewPort(true);s.setMediaPlaybackRequiresUserGesture(false);
        final WebViewAssetLoader assetLoader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.addJavascriptInterface(new Bridge(),"Android");web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){@Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){return assetLoader.shouldInterceptRequest(request.getUrl());}@Override public void onPageFinished(WebView view,String url){pageReady=true;eval("window.androidReady&&window.androidReady()");}});
        FrameLayout root=new FrameLayout(this);root.addView(web,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
        castButton=new Button(this);castButton.setText("TV'YE AKTAR");castButton.setTextColor(Color.WHITE);castButton.setTextSize(11f);castButton.setAllCaps(false);castButton.setBackgroundColor(Color.argb(210,12,20,28));castButton.setPadding(18,4,18,4);castButton.setOnClickListener(v->openCastSettings());
        FrameLayout.LayoutParams castLp=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,52);castLp.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;castLp.bottomMargin=14;root.addView(castButton,castLp);setContentView(root);
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        displayManager=(DisplayManager)getSystemService(DISPLAY_SERVICE);if(displayManager!=null)displayManager.registerDisplayListener(displayListener,null);updateCastButton();
        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);accel=sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);if(accel==null)accel=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);gyro=sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);rotation=sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);if(rotation==null)rotation=sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        bt=BluetoothAdapter.getDefaultAdapter();poseTracker=new PoseIntentTracker(this,p->{intentEngine.onPose(p);poseTime=p.timeMs;pushIntentStatus();});askPermissions();
    }

    private void openCastSettings(){Toast.makeText(this,"LG TV'de Screen Share / Ekran Paylaşımı açık olmalı",Toast.LENGTH_LONG).show();try{startActivity(new Intent(Settings.ACTION_CAST_SETTINGS));}catch(Exception first){try{startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));}catch(Exception second){Toast.makeText(this,"Kablosuz ekran ayarı bu telefonda açılamadı",Toast.LENGTH_LONG).show();}}}
    private boolean hasPresentationDisplay(){if(displayManager==null)return false;try{return displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).length>0;}catch(Exception ignored){return false;}}
    private void updateCastButton(){if(castButton==null)return;castButton.post(()->{boolean connected=hasPresentationDisplay();castButton.setText(connected?"TV BAĞLI ✓":"TV'YE AKTAR");castButton.setAlpha(connected?0.88f:1f);eval("window.tvConnectionChanged&&window.tvConnectionChanged("+connected+")");});}
    private void askPermissions(){ArrayList<String> req=new ArrayList<>();if(visionEnabled&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)req.add(Manifest.permission.CAMERA);if(Build.VERSION.SDK_INT>=31){if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)req.add(Manifest.permission.BLUETOOTH_CONNECT);if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED)req.add(Manifest.permission.BLUETOOTH_SCAN);}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)req.add(Manifest.permission.POST_NOTIFICATIONS);if(!req.isEmpty())requestPermissions(req.toArray(new String[0]),7);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==7&&externalRacketHost&&visionEnabled)startVision();}

    private void registerSensors(){if(controllerMode||externalRacketHost)return;if(accel!=null)sensorManager.registerListener(this,accel,SensorManager.SENSOR_DELAY_GAME);if(gyro!=null)sensorManager.registerListener(this,gyro,SensorManager.SENSOR_DELAY_GAME);if(rotation!=null)sensorManager.registerListener(this,rotation,SensorManager.SENSOR_DELAY_GAME);}
    @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();registerSensors();if(externalRacketHost&&visionEnabled)startVision();updateCastButton();}
    @Override protected void onPause(){sensorManager.unregisterListener(this);if(poseTracker!=null)poseTracker.stop();if(web!=null)web.onPause();super.onPause();}
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}

    private int displayRotation(){try{Display d=getWindowManager().getDefaultDisplay();return d==null?Surface.ROTATION_0:d.getRotation();}catch(Exception e){return Surface.ROTATION_0;}}
    private float screenX(float x,float y){int r=displayRotation();if(r==Surface.ROTATION_90)return -y;if(r==Surface.ROTATION_270)return y;return r==Surface.ROTATION_180?-x:x;}
    private float screenY(float x,float y){int r=displayRotation();if(r==Surface.ROTATION_90)return x;if(r==Surface.ROTATION_270)return -x;return r==Surface.ROTATION_180?-y:y;}

    @Override public void onSensorChanged(SensorEvent e){
        if(controllerMode||externalRacketHost)return;
        int type=e.sensor.getType();
        if(type==Sensor.TYPE_GYROSCOPE){gx=e.values[0];gy=e.values[1];gz=e.values[2];gyroMag=(float)Math.sqrt(gx*gx+gy*gy+gz*gz);return;}
        if(type==Sensor.TYPE_GAME_ROTATION_VECTOR||type==Sensor.TYPE_ROTATION_VECTOR){float[] rm=new float[9],rr=new float[9],ori=new float[3];SensorManager.getRotationMatrixFromVector(rm,e.values);SensorManager.remapCoordinateSystem(rm,SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X,rr);SensorManager.getOrientation(rr,ori);float roll=(float)Math.toDegrees(ori[2]);tilt=clamp(tilt*.86f+clamp(roll/10f,-6f,6f)*.14f,-6f,6f);pushTilt();return;}
        if(e.values.length<3)return;
        float ax=e.values[0],ay=e.values[1],az=e.values[2];
        if(rotation==null){tilt=clamp(tilt*.90f+(-screenX(ax,ay))*.10f,-6f,6f);pushTilt();}
        float a=(float)Math.sqrt(ax*ax+ay*ay+az*az);
        boolean linear=type==Sensor.TYPE_LINEAR_ACCELERATION;
        if(linear&&a<3.2f){soloNoise=soloNoise*.965f+a*.035f;soloNoiseSamples++;}
        float adaptive=linear?clamp(Math.max(3.8f,soloNoise*2.45f+1.35f),3.8f,6.1f):12.2f;
        float sx=screenX(ax,ay),sy=screenY(ax,ay),sgx=screenX(gx,gy);
        float lateral=clamp(sx/Math.max(3.6f,a),-1f,1f);
        float gyroDir=clamp(sgx/Math.max(.75f,gyroMag),-1f,1f);
        float direction=clamp(lateral*.76f+gyroDir*.24f,-1f,1f);if(Math.abs(direction)<.09f)direction=0f;
        float thrust=Math.max(Math.abs(sy),Math.abs(az)*.58f);
        long now=System.currentTimeMillis();
        boolean twoHandImpulse=a>adaptive&&(gyroMag>.48f||thrust>adaptive*.58f);
        if(twoHandImpulse&&now-lastSwing>255){
            lastSwing=now;
            float power=clamp((a/adaptive)*.64f+(gyroMag/3.2f)*.20f+(thrust/adaptive)*.16f,.78f,2.55f);
            TennisIntentEngine.Decision d=intentEngine.onSwing(power,direction,false);
            haptic(24);showSoloDecision(d);
            localSwingDetailed(d.power,d.direction,Math.abs(direction)<.13f?"CENTER":direction>0?"FOREHAND":"BACKHAND");
        }
    }

    private void startVision(){if(visionEnabled&&checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED&&poseTracker!=null)poseTracker.start();}
    private void stopVision(){if(poseTracker!=null)poseTracker.stop();}
    private void pushIntentStatus(){if(System.currentTimeMillis()-poseTime>500)return;TennisIntentEngine.Phase p=intentEngine.getPhase();TennisIntentEngine.Intent i=intentEngine.getIntent();eval("window.tennisIntent&&window.tennisIntent('"+p.name()+"','"+i.name()+"')");}
    private void showDecision(TennisIntentEngine.Decision d){String label=d.intent==TennisIntentEngine.Intent.UNKNOWN?"VURUŞ":d.intent.name();eval("document.getElementById('status').textContent='"+label+" • güven %"+Math.round(d.confidence*100f)+" • kamera+IMU';");}
    private void showSoloDecision(TennisIntentEngine.Decision d){String label=d.intent==TennisIntentEngine.Intent.UNKNOWN?"VURUŞ":d.intent.name();eval("document.getElementById('status').textContent='TEK TELEFON • "+label+" • VURUŞ ALGILANDI';window.MGPC1&&(MGPC1.soloNativeProfile='PASS');");}
    private void pushTilt(){long n=System.nanoTime();if(n-lastTiltPush<16_000_000L)return;lastTiltPush=n;eval(String.format(Locale.US,"window.setPlayerTilt&&window.setPlayerTilt(%.4f)",tilt));}
    private void localSwingDetailed(float p,float d,String stroke){eval(String.format(Locale.US,"window.localSwingV61?window.localSwingV61(%.4f,%.4f,'%s'):window.nativeSwing&&window.nativeSwing(%.4f,%.4f,false)",p,d,stroke,p,d));}
    private void remoteSwingDetailed(float p,float d,String stroke,String spin,float spinValue){eval(String.format(Locale.US,"window.remoteSwingV39&&window.remoteSwingV39(%.4f,%.4f,'%s','%s',%.2f)",p,d,stroke.replace("'",""),spin.replace("'",""),spinValue));}
    private void eval(String js){WebView w=web;if(w==null||!pageReady)return;w.post(()->w.evaluateJavascript(js,null));}
    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private void haptic(int ms){try{Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v==null)return;if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(ms,VibrationEffect.DEFAULT_AMPLITUDE));else v.vibrate(ms);}catch(Exception ignored){}}
    public void calibrate(){tilt=0f;soloNoise=.35f;soloNoiseSamples=0;pushTilt();eval("window.calibrationFlash&&window.calibrationFlash()");Toast.makeText(this,"Tek telefon ekran tutuşu kalibre edildi",Toast.LENGTH_SHORT).show();}
    public void startAi(){controllerMode=false;externalRacketHost=false;visionEnabled=false;stopVision();stopRacketService();closeBt();sensorManager.unregisterListener(this);soloNoise=.35f;soloNoiseSamples=0;registerSensors();eval("window.setMode&&window.setMode('ai');window.MGPC1&&(MGPC1.sensorProfile='SOLO_TWO_HAND_SCREEN')");Toast.makeText(this,"Tek telefon: iki elle ekran tutuşu sensör profili",Toast.LENGTH_SHORT).show();}

    public void startHost(){startHost(true);}
    public void startHostNoCamera(){startHost(false);}
    private void startHost(boolean useCamera){controllerMode=false;externalRacketHost=true;visionEnabled=useCamera;stopRacketService();sensorManager.unregisterListener(this);stopVision();eval("window.setMode&&window.setMode('ai');window.MGPC1&&(MGPC1.tvCamera='"+(useCamera?"CAMERA":"NO_CAMERA")+"')");if(useCamera){if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){askPermissions();}else startVision();}if(bt==null){Toast.makeText(this,"Bluetooth desteklenmiyor",Toast.LENGTH_LONG).show();return;}if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){askPermissions();return;}closeBt();new Thread(()->{try{serverSocket=bt.listenUsingRfcommWithServiceRecord("MG Tennis TV",UUID_GAME);runOnUiThread(()->Toast.makeText(this,useCamera?"TV + kamera • raket telefonu bekleniyor…":"TV • kamera kapalı • raket telefonu bekleniyor…",Toast.LENGTH_LONG).show());btSocket=serverSocket.accept();hostOut=new PrintWriter(new OutputStreamWriter(btSocket.getOutputStream()),true);eval("window.racketLinkState&&window.racketLinkState('CONNECTED',0)");startPingLoop();readRemote(btSocket);}catch(Exception ex){eval("window.racketLinkState&&window.racketLinkState('DISCONNECTED',0)");runOnUiThread(()->Toast.makeText(this,"Raket bağlantısı: "+ex.getMessage(),Toast.LENGTH_LONG).show());}},"MG-Tennis-RacketHost").start();}

    private void startPingLoop(){pingRunning=true;new Thread(()->{while(pingRunning&&btSocket!=null&&btSocket.isConnected()){try{PrintWriter w=hostOut;if(w!=null){long t=System.currentTimeMillis();w.println("PING,"+t);if(w.checkError())break;}SystemClock.sleep(1000);}catch(Exception e){break;}}},"MG-Tennis-Ping").start();}

    @SuppressLint("MissingPermission") public void startController(){controllerMode=true;externalRacketHost=false;visionEnabled=false;stopVision();sensorManager.unregisterListener(this);eval("window.setMode&&window.setMode('controller');window.MGPC1&&(MGPC1.sensorProfile='RACKET_HORIZONTAL')");if(bt==null){Toast.makeText(this,"Bluetooth desteklenmiyor",Toast.LENGTH_LONG).show();return;}if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){askPermissions();return;}Set<BluetoothDevice> bonded=bt.getBondedDevices();if(bonded==null||bonded.isEmpty()){Toast.makeText(this,"Önce iki telefonu Android Bluetooth ayarından eşleştir",Toast.LENGTH_LONG).show();return;}ArrayList<BluetoothDevice> devices=new ArrayList<>(bonded);String[] names=new String[devices.size()];for(int i=0;i<devices.size();i++){String name=devices.get(i).getName();names[i]=(name==null?"Bluetooth cihazı":name)+"\n"+devices.get(i).getAddress();}new AlertDialog.Builder(this).setTitle("Ekran / ana telefonu seç").setItems(names,(d,which)->startRacketService(devices.get(which))).show();}
    @SuppressLint("MissingPermission") private void startRacketService(BluetoothDevice dev){Intent i=new Intent(this,RacketService.class).setAction(RacketService.ACTION_START).putExtra(RacketService.EXTRA_ADDRESS,dev.getAddress());if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);Toast.makeText(this,"RAKET MODU • telefonu yatay raket gibi tut",Toast.LENGTH_LONG).show();}
    private void stopRacketService(){try{stopService(new Intent(this,RacketService.class));}catch(Exception ignored){}}

    private void readRemote(BluetoothSocket s)throws IOException{BufferedReader br=new BufferedReader(new InputStreamReader(s.getInputStream()));String line;while((line=br.readLine())!=null){if(line.startsWith("PONG,")){String[] q=line.split(",");if(q.length>1)try{long sent=Long.parseLong(q[1]);latencyMs=Math.max(0,System.currentTimeMillis()-sent);eval("window.racketLinkState&&window.racketLinkState('CONNECTED',"+latencyMs+")");}catch(Exception ignored){}continue;}if(line.startsWith("SWING,")||line.startsWith("RAISE_HIT,")){String[] q=line.split(",");if(q.length>=3)try{boolean raised=line.startsWith("RAISE_HIT,");float p=Float.parseFloat(q[1]),d=Float.parseFloat(q[2]);TennisIntentEngine.Decision dec=intentEngine.onSwing(p,d,raised);showDecision(dec);String stroke=q.length>=4?q[3]:(d>.15f?"FOREHAND":d<-.15f?"BACKHAND":"CENTER");String spin=q.length>=5?q[4]:"TOPSPIN";float spinValue=q.length>=6?Float.parseFloat(q[5]):0f;remoteSwingDetailed(dec.power,dec.direction,stroke,spin,spinValue);}catch(Exception ignored){}}else if(line.startsWith("TILT,")){String[] q=line.split(",");if(q.length>=2)try{float t=Float.parseFloat(q[1]);eval(String.format(Locale.US,"window.setPlayerTilt&&window.setPlayerTilt(%.4f)",t));}catch(NumberFormatException ignored){}}}pingRunning=false;eval("window.racketLinkState&&window.racketLinkState('DISCONNECTED',0)");}
    private void closeBt(){pingRunning=false;hostOut=null;try{if(btSocket!=null)btSocket.close();}catch(Exception ignored){}try{if(serverSocket!=null)serverSocket.close();}catch(Exception ignored){}btSocket=null;serverSocket=null;latencyMs=0;}

    public class Bridge{
        @JavascriptInterface public void startAi(){runOnUiThread(MainActivity.this::startAi);}
        @JavascriptInterface public void startHost(){runOnUiThread(MainActivity.this::startHost);}
        @JavascriptInterface public void startHostCamera(){runOnUiThread(MainActivity.this::startHost);}
        @JavascriptInterface public void startHostNoCamera(){runOnUiThread(MainActivity.this::startHostNoCamera);}
        @JavascriptInterface public void startController(){runOnUiThread(MainActivity.this::startController);}
        @JavascriptInterface public void calibrate(){runOnUiThread(MainActivity.this::calibrate);}
        @JavascriptInterface public void vibrate(int ms){haptic(Math.max(5,Math.min(ms,80)));}
        @JavascriptInterface public void openCastSettings(){runOnUiThread(MainActivity.this::openCastSettings);}
        @JavascriptInterface public boolean isTvConnected(){return hasPresentationDisplay();}
        @JavascriptInterface public long getRacketLatencyMs(){return latencyMs;}
        @JavascriptInterface public boolean hasGyroscope(){return gyro!=null;}
        @JavascriptInterface public boolean hasAccelerometer(){return accel!=null;}
        @JavascriptInterface public boolean hasCameraPermission(){return checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED;}
        @JavascriptInterface public void stopVision(){runOnUiThread(MainActivity.this::stopVision);}
    }
    @Override public void onBackPressed(){if(controllerMode){controllerMode=false;stopRacketService();eval("window.setMode&&window.setMode('ai')");sensorManager.unregisterListener(this);registerSensors();return;}if(externalRacketHost){externalRacketHost=false;visionEnabled=false;stopVision();closeBt();eval("window.setMode&&window.setMode('ai')");sensorManager.unregisterListener(this);registerSensors();return;}super.onBackPressed();}
    @Override protected void onDestroy(){if(displayManager!=null)try{displayManager.unregisterDisplayListener(displayListener);}catch(Exception ignored){}stopVision();closeBt();if(web!=null){web.removeJavascriptInterface("Android");web.destroy();web=null;}super.onDestroy();}
}
