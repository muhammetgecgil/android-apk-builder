package com.mg.tennistv;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.*;
import android.bluetooth.*;
import android.view.*;
import android.webkit.*;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accel, gyro, rotation;
    private float gyroMag=0f, tilt=0f;
    private long lastSwing=0L,lastTiltPush=0L;
    private WebView web;
    private volatile boolean pageReady=false;
    private BluetoothAdapter bt;
    private BluetoothSocket btSocket;
    private BluetoothServerSocket serverSocket;
    private volatile boolean controllerMode=false;
    private volatile boolean externalRacketHost=false;
    private static final UUID UUID_GAME=UUID.fromString("6f4f4d30-9c4e-4f99-9e96-1e2ac4c1a501");

    @SuppressLint({"SetJavaScriptEnabled","JavascriptInterface"})
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        web=new WebView(this); web.setBackgroundColor(Color.BLACK); web.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(false); s.setAllowContentAccess(false); s.setLoadWithOverviewMode(true); s.setUseWideViewPort(true); s.setMediaPlaybackRequiresUserGesture(false);
        final WebViewAssetLoader assetLoader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.addJavascriptInterface(new Bridge(),"Android"); web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){return assetLoader.shouldInterceptRequest(request.getUrl());}
            @Override public void onPageFinished(WebView view,String url){pageReady=true;eval("window.androidReady&&window.androidReady()");}
        });
        setContentView(web); web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        accel=sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION); if(accel==null)accel=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro=sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotation=sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR); if(rotation==null)rotation=sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        bt=BluetoothAdapter.getDefaultAdapter(); askPermissions();
    }

    private void askPermissions(){
        ArrayList<String> req=new ArrayList<>();
        if(Build.VERSION.SDK_INT>=31){
            if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)req.add(Manifest.permission.BLUETOOTH_CONNECT);
            if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED)req.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)req.add(Manifest.permission.POST_NOTIFICATIONS);
        if(!req.isEmpty())requestPermissions(req.toArray(new String[0]),7);
    }
    private void registerSensors(){
        if(controllerMode||externalRacketHost)return;
        if(accel!=null)sensorManager.registerListener(this,accel,SensorManager.SENSOR_DELAY_GAME);
        if(gyro!=null)sensorManager.registerListener(this,gyro,SensorManager.SENSOR_DELAY_GAME);
        if(rotation!=null)sensorManager.registerListener(this,rotation,SensorManager.SENSOR_DELAY_GAME);
    }
    @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();registerSensors();}
    @Override protected void onPause(){sensorManager.unregisterListener(this);if(web!=null)web.onPause();super.onPause();}
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}

    @Override public void onSensorChanged(SensorEvent e){
        if(controllerMode||externalRacketHost)return;
        int type=e.sensor.getType();
        if(type==Sensor.TYPE_GYROSCOPE){gyroMag=(float)Math.sqrt(e.values[0]*e.values[0]+e.values[1]*e.values[1]+e.values[2]*e.values[2]);return;}
        if(type==Sensor.TYPE_GAME_ROTATION_VECTOR||type==Sensor.TYPE_ROTATION_VECTOR){
            float[] rm=new float[9],rr=new float[9],ori=new float[3]; SensorManager.getRotationMatrixFromVector(rm,e.values); SensorManager.remapCoordinateSystem(rm,SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X,rr); SensorManager.getOrientation(rr,ori);
            float roll=(float)Math.toDegrees(ori[2]); tilt=clamp(tilt*.82f+clamp(roll/8f,-6f,6f)*.18f,-6f,6f); pushTilt(); return;
        }
        if(e.values.length<3)return;
        if(rotation==null){tilt=clamp(tilt*.86f+(-e.values[0])*.14f,-6f,6f);pushTilt();}
        float a=(float)Math.sqrt(e.values[0]*e.values[0]+e.values[1]*e.values[1]+e.values[2]*e.values[2]); long now=System.currentTimeMillis(); float threshold=type==Sensor.TYPE_LINEAR_ACCELERATION?7.5f:14.5f;
        if(a>threshold&&gyroMag>1.35f&&now-lastSwing>285){lastSwing=now;float power=clamp((a/threshold)*.54f+(gyroMag/4f)*.46f,.68f,2.45f);float direction=clamp(e.values[0]/Math.max(5f,a),-1f,1f);haptic(22);localSwing(power,direction);}
    }
    private void pushTilt(){long n=System.nanoTime();if(n-lastTiltPush<16_000_000L)return;lastTiltPush=n;eval(String.format(Locale.US,"window.setPlayerTilt&&window.setPlayerTilt(%.4f)",tilt));}
    private void localSwing(float p,float d){eval(String.format(Locale.US,"window.nativeSwing&&window.nativeSwing(%.4f,%.4f,false)",p,d));}
    private void remoteSwing(float p,float d){eval(String.format(Locale.US,"window.nativeSwing&&window.nativeSwing(%.4f,%.4f,true)",p,d));}
    private void eval(String js){WebView w=web;if(w==null||!pageReady)return;w.post(()->w.evaluateJavascript(js,null));}
    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private void haptic(int ms){try{Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v==null)return;if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(ms,VibrationEffect.DEFAULT_AMPLITUDE));else v.vibrate(ms);}catch(Exception ignored){}}
    public void calibrate(){tilt=0f;pushTilt();eval("window.calibrationFlash&&window.calibrationFlash()");Toast.makeText(this,"Raket merkezi kalibre edildi",Toast.LENGTH_SHORT).show();}

    public void startAi(){
        controllerMode=false;externalRacketHost=false;stopRacketService();closeBt();sensorManager.unregisterListener(this);registerSensors();
        eval("window.setMode&&window.setMode('ai')");Toast.makeText(this,"Bu telefon: ekran + kendi raketi",Toast.LENGTH_SHORT).show();
    }

    public void startHost(){
        controllerMode=false;externalRacketHost=true;stopRacketService();sensorManager.unregisterListener(this);eval("window.setMode&&window.setMode('ai')");
        if(bt==null){Toast.makeText(this,"Bluetooth desteklenmiyor",Toast.LENGTH_LONG).show();return;}
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){askPermissions();return;}
        closeBt();
        new Thread(()->{
            try{
                serverSocket=bt.listenUsingRfcommWithServiceRecord("MG Tennis TV",UUID_GAME);
                runOnUiThread(()->Toast.makeText(this,"AI + RAKET: raket telefonu bekleniyor…",Toast.LENGTH_LONG).show());
                btSocket=serverSocket.accept();
                runOnUiThread(()->Toast.makeText(this,"Raket telefonu bağlandı • oyun ekranda/TV'de",Toast.LENGTH_LONG).show());
                eval("window.controllerConnected&&window.controllerConnected(true)");
                readRemote(btSocket,true);
            }catch(Exception ex){runOnUiThread(()->Toast.makeText(this,"Raket bağlantısı: "+ex.getMessage(),Toast.LENGTH_LONG).show());}
        },"MG-Tennis-RacketHost").start();
    }

    @SuppressLint("MissingPermission")
    public void startController(){
        controllerMode=true;externalRacketHost=false;sensorManager.unregisterListener(this);eval("window.setMode&&window.setMode('controller')");
        if(bt==null){Toast.makeText(this,"Bluetooth desteklenmiyor",Toast.LENGTH_LONG).show();return;}
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){askPermissions();return;}
        Set<BluetoothDevice> bonded=bt.getBondedDevices();
        if(bonded==null||bonded.isEmpty()){Toast.makeText(this,"Önce iki telefonu Android Bluetooth ayarından eşleştir",Toast.LENGTH_LONG).show();return;}
        ArrayList<BluetoothDevice> devices=new ArrayList<>(bonded);String[] names=new String[devices.size()];
        for(int i=0;i<devices.size();i++){String name=devices.get(i).getName();names[i]=(name==null?"Bluetooth cihazı":name)+"\n"+devices.get(i).getAddress();}
        new AlertDialog.Builder(this).setTitle("Ekran / ana telefonu seç").setItems(names,(d,which)->startRacketService(devices.get(which))).show();
    }

    @SuppressLint("MissingPermission")
    private void startRacketService(BluetoothDevice dev){
        Intent i=new Intent(this,RacketService.class).setAction(RacketService.ACTION_START).putExtra(RacketService.EXTRA_ADDRESS,dev.getAddress());
        if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
        eval("window.controllerConnected&&window.controllerConnected(true)");
        Toast.makeText(this,"RAKET MODU başladı • ekranı kapatabilirsin",Toast.LENGTH_LONG).show();
    }
    private void stopRacketService(){try{stopService(new Intent(this,RacketService.class));}catch(Exception ignored){}}

    private void readRemote(BluetoothSocket s,boolean asLocal)throws IOException{
        BufferedReader br=new BufferedReader(new InputStreamReader(s.getInputStream()));String line;
        while((line=br.readLine())!=null){
            if(line.startsWith("SWING,")){String[] q=line.split(",");if(q.length>=3)try{float p=Float.parseFloat(q[1]),d=Float.parseFloat(q[2]);if(asLocal)localSwing(p,d);else remoteSwing(p,d);}catch(NumberFormatException ignored){}}
            else if(line.startsWith("TILT,")){String[] q=line.split(",");if(q.length>=2)try{float t=Float.parseFloat(q[1]);eval(String.format(Locale.US,"window.setPlayerTilt&&window.setPlayerTilt(%.4f)",t));}catch(NumberFormatException ignored){}}
        }
    }
    private void closeBt(){try{if(btSocket!=null)btSocket.close();}catch(Exception ignored){}try{if(serverSocket!=null)serverSocket.close();}catch(Exception ignored){}btSocket=null;serverSocket=null;}

    public class Bridge{
        @JavascriptInterface public void startAi(){runOnUiThread(MainActivity.this::startAi);}
        @JavascriptInterface public void startHost(){runOnUiThread(MainActivity.this::startHost);}
        @JavascriptInterface public void startController(){runOnUiThread(MainActivity.this::startController);}
        @JavascriptInterface public void calibrate(){runOnUiThread(MainActivity.this::calibrate);}
        @JavascriptInterface public void vibrate(int ms){haptic(Math.max(5,Math.min(ms,80)));}
    }
    @Override public void onBackPressed(){
        if(controllerMode){controllerMode=false;stopRacketService();eval("window.setMode&&window.setMode('ai')");sensorManager.unregisterListener(this);registerSensors();return;}
        if(externalRacketHost){externalRacketHost=false;closeBt();eval("window.setMode&&window.setMode('ai')");sensorManager.unregisterListener(this);registerSensors();return;}
        super.onBackPressed();
    }
    @Override protected void onDestroy(){closeBt();if(web!=null){web.removeJavascriptInterface("Android");web.destroy();web=null;}super.onDestroy();}
}
