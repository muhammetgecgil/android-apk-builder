package com.mg.tennistv;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.os.*;
import androidx.core.app.NotificationCompat;
import java.io.*;
import java.util.*;

public class RacketService extends Service implements SensorEventListener {
    public static final String ACTION_START="com.mg.tennistv.RACKET_START";
    public static final String ACTION_STOP="com.mg.tennistv.RACKET_STOP";
    public static final String ACTION_CALIBRATE="com.mg.tennistv.RACKET_CALIBRATE";
    public static final String ACTION_STATE="com.mg.tennistv.RACKET_STATE";
    public static final String EXTRA_ADDRESS="device_address";
    public static final String EXTRA_STATE="state";
    private static final String CHANNEL="mg_tennis_racket";
    private static final int NOTIF_ID=731;
    private static final UUID UUID_GAME=UUID.fromString("6f4f4d30-9c4e-4f99-9e96-1e2ac4c1a501");

    private SensorManager sm; private Sensor accel,gyro,rotation;
    private float gyroMag=0f,gx=0f,gy=0f,gz=0f,tilt=0f,neutralPitch=Float.NaN,pitchDeg=0f;
    private long lastSwing=0L,lastTilt=0L,raisedUntil=0L,calibrateUntil=0L; private float calibPitchSum=0f; private int calibCount=0;
    private BluetoothSocket socket; private PrintWriter out; private BufferedReader in; private PowerManager.WakeLock wakeLock;
    private volatile boolean running=false,stopping=false; private String address;

    @Override public void onCreate(){super.onCreate();createChannel();sm=(SensorManager)getSystemService(SENSOR_SERVICE);accel=sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);if(accel==null)accel=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);gyro=sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);rotation=sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);if(rotation==null)rotation=sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);if(pm!=null){wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MGTennis:Racket");wakeLock.setReferenceCounted(false);}}

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&ACTION_STOP.equals(intent.getAction())){stopping=true;stopSelf();return START_NOT_STICKY;}
        if(intent!=null&&ACTION_CALIBRATE.equals(intent.getAction())){beginCalibration();return START_STICKY;}
        startForeground(NOTIF_ID,notification("Ana telefona bağlanıyor…"));address=intent==null?null:intent.getStringExtra(EXTRA_ADDRESS);if(address==null||address.isEmpty()){stopSelf();return START_NOT_STICKY;}stopping=false;if(wakeLock!=null&&!wakeLock.isHeld())wakeLock.acquire(8*60*60*1000L);registerSensors();beginCalibration();connectLoop();return START_REDELIVER_INTENT;
    }
    private void registerSensors(){sm.unregisterListener(this);if(accel!=null)sm.registerListener(this,accel,SensorManager.SENSOR_DELAY_GAME);if(gyro!=null)sm.registerListener(this,gyro,SensorManager.SENSOR_DELAY_GAME);if(rotation!=null)sm.registerListener(this,rotation,SensorManager.SENSOR_DELAY_GAME);}
    private void beginCalibration(){neutralPitch=Float.NaN;calibPitchSum=0f;calibCount=0;calibrateUntil=System.currentTimeMillis()+1800L;notifyState("Raket kalibrasyonu • telefonu doğal tut");broadcast("CALIBRATING");}

    @SuppressLint("MissingPermission") private void connectLoop(){new Thread(()->{int attempt=0;while(!stopping){closeSocket();try{BluetoothAdapter bt=BluetoothAdapter.getDefaultAdapter();if(bt==null)throw new IOException("Bluetooth yok");if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Bluetooth izni yok");if(bt.isDiscovering())bt.cancelDiscovery();BluetoothDevice dev=bt.getRemoteDevice(address);socket=dev.createRfcommSocketToServiceRecord(UUID_GAME);broadcast("CONNECTING");socket.connect();out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);in=new BufferedReader(new InputStreamReader(socket.getInputStream()));running=true;attempt=0;notifyState("Raket bağlı • otomatik yeniden bağlantı aktif");broadcast("CONNECTED");readHost();}catch(Exception e){running=false;broadcast("DISCONNECTED");if(stopping)break;attempt++;long wait=Math.min(8000L,800L*(1L<<Math.min(attempt,3)));notifyState("Bağlantı koptu • yeniden bağlanıyor…");SystemClock.sleep(wait);}}},"MG-Racket-Bluetooth").start();}
    private void readHost() throws IOException {String line;while(running&&!stopping&&in!=null&&(line=in.readLine())!=null){if(line.startsWith("PING,")){String[] q=line.split(",");if(q.length>1)send("PONG,"+q[1]);}else if(line.equals("CALIBRATE"))beginCalibration();}running=false;}

    @Override public void onSensorChanged(SensorEvent e){if(!running&&System.currentTimeMillis()>calibrateUntil)return;int type=e.sensor.getType();if(type==Sensor.TYPE_GYROSCOPE){gx=e.values[0];gy=e.values[1];gz=e.values[2];gyroMag=(float)Math.sqrt(gx*gx+gy*gy+gz*gz);return;}if(type==Sensor.TYPE_GAME_ROTATION_VECTOR||type==Sensor.TYPE_ROTATION_VECTOR){float[] rm=new float[9],rr=new float[9],ori=new float[3];SensorManager.getRotationMatrixFromVector(rm,e.values);SensorManager.remapCoordinateSystem(rm,SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X,rr);SensorManager.getOrientation(rr,ori);float roll=(float)Math.toDegrees(ori[2]);pitchDeg=(float)Math.toDegrees(ori[1]);long now=System.currentTimeMillis();if(now<calibrateUntil){calibPitchSum+=pitchDeg;calibCount++;if(calibCount>8)neutralPitch=calibPitchSum/calibCount;}else if(Float.isNaN(neutralPitch)){neutralPitch=pitchDeg;broadcast("CALIBRATED");}float dp=pitchDeg-neutralPitch;if(Math.abs(dp)>32f)raisedUntil=now+1100L;tilt=clamp(tilt*.82f+clamp(roll/8f,-6f,6f)*.18f,-6f,6f);long n=System.nanoTime();if(running&&n-lastTilt>33_000_000L){lastTilt=n;send(String.format(Locale.US,"TILT,%.4f",tilt));}return;}if(!running||e.values.length<3)return;float a=(float)Math.sqrt(e.values[0]*e.values[0]+e.values[1]*e.values[1]+e.values[2]*e.values[2]);long now=System.currentTimeMillis();float threshold=type==Sensor.TYPE_LINEAR_ACCELERATION?7.5f:14.5f;float direction=clamp(e.values[0]/Math.max(5f,a),-1f,1f);boolean raisedForward=now<raisedUntil&&a>threshold*1.12f&&gyroMag>1.45f&&Math.abs(e.values[2])>threshold*.28f&&now-lastSwing>320;if(raisedForward){lastSwing=now;raisedUntil=0L;emitSwing(a,threshold,direction,true);return;}if(a>threshold&&gyroMag>1.35f&&now-lastSwing>285){lastSwing=now;emitSwing(a,threshold,direction,false);}}
    private void emitSwing(float a,float threshold,float direction,boolean raised){float power=clamp((a/threshold)*(raised?.58f:.54f)+(gyroMag/4f)*(raised?.42f:.46f),raised?.95f:.68f,raised?2.65f:2.45f);String stroke=Math.abs(direction)<.15f?"CENTER":(direction>0?"FOREHAND":"BACKHAND");float spin=clamp((Math.abs(gz)*.62f+Math.abs(gx)*.38f)*6f,0f,30f);String spinType=Math.abs(gx)>Math.abs(gz)*1.25f?"SLICE":"TOPSPIN";send(String.format(Locale.US,"%s,%.4f,%.4f,%s,%s,%.2f",raised?"RAISE_HIT":"SWING",power,direction,stroke,spinType,spin));vibrate(raised?34:22);}

    private synchronized void send(String line){PrintWriter w=out;if(w!=null){w.println(line);if(w.checkError())running=false;}}
    private void broadcast(String state){Intent i=new Intent(ACTION_STATE).setPackage(getPackageName()).putExtra(EXTRA_STATE,state);sendBroadcast(i);}
    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private void vibrate(int ms){try{Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v==null)return;if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(ms,VibrationEffect.DEFAULT_AMPLITUDE));else v.vibrate(ms);}catch(Exception ignored){}}
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"MG Tennis Raket",NotificationManager.IMPORTANCE_LOW);c.setDescription("Raket telefonu sensör ve Bluetooth bağlantısı");NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.createNotificationChannel(c);}}
    private Notification notification(String text){Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Intent stop=new Intent(this,RacketService.class).setAction(ACTION_STOP);PendingIntent ps=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Intent cal=new Intent(this,RacketService.class).setAction(ACTION_CALIBRATE);PendingIntent pc=PendingIntent.getService(this,2,cal,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("MG Tennis • Raket Modu").setContentText(text).setOngoing(true).setContentIntent(pi).addAction(android.R.drawable.ic_menu_compass,"Kalibre",pc).addAction(android.R.drawable.ic_menu_close_clear_cancel,"Durdur",ps).build();}
    private void notifyState(String text){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(nm!=null)nm.notify(NOTIF_ID,notification(text));}
    private void closeSocket(){running=false;try{if(socket!=null)socket.close();}catch(Exception ignored){}socket=null;out=null;in=null;}
    @Override public void onDestroy(){stopping=true;sm.unregisterListener(this);closeSocket();broadcast("STOPPED");if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
