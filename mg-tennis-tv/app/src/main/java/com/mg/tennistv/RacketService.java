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
    public static final String ACTION_START = "com.mg.tennistv.RACKET_START";
    public static final String ACTION_STOP = "com.mg.tennistv.RACKET_STOP";
    public static final String EXTRA_ADDRESS = "device_address";
    private static final String CHANNEL = "mg_tennis_racket";
    private static final int NOTIF_ID = 731;
    private static final UUID UUID_GAME = UUID.fromString("6f4f4d30-9c4e-4f99-9e96-1e2ac4c1a501");

    private SensorManager sm;
    private Sensor accel, gyro, rotation;
    private float gyroMag=0f, tilt=0f, neutralPitch=Float.NaN, pitchDeg=0f;
    private long lastSwing=0L, lastTilt=0L, raisedUntil=0L;
    private BluetoothSocket socket;
    private PrintWriter out;
    private PowerManager.WakeLock wakeLock;
    private volatile boolean running=false;

    @Override public void onCreate(){
        super.onCreate();createChannel();sm=(SensorManager)getSystemService(SENSOR_SERVICE);
        accel=sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);if(accel==null)accel=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro=sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotation=sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);if(rotation==null)rotation=sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);if(pm!=null){wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MGTennis:Racket");wakeLock.setReferenceCounted(false);}
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&ACTION_STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        startForeground(NOTIF_ID,notification("Ana telefona bağlanıyor…"));
        String address=intent==null?null:intent.getStringExtra(EXTRA_ADDRESS);if(address==null||address.isEmpty()){stopSelf();return START_NOT_STICKY;}
        if(wakeLock!=null&&!wakeLock.isHeld())wakeLock.acquire(8*60*60*1000L);registerSensors();connect(address);return START_REDELIVER_INTENT;
    }

    private void registerSensors(){if(accel!=null)sm.registerListener(this,accel,SensorManager.SENSOR_DELAY_GAME);if(gyro!=null)sm.registerListener(this,gyro,SensorManager.SENSOR_DELAY_GAME);if(rotation!=null)sm.registerListener(this,rotation,SensorManager.SENSOR_DELAY_GAME);}

    @SuppressLint("MissingPermission")
    private void connect(String address){new Thread(()->{closeSocket();try{BluetoothAdapter bt=BluetoothAdapter.getDefaultAdapter();if(bt==null)throw new IOException("Bluetooth yok");if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Bluetooth izni yok");if(bt.isDiscovering())bt.cancelDiscovery();BluetoothDevice dev=bt.getRemoteDevice(address);socket=dev.createRfcommSocketToServiceRecord(UUID_GAME);socket.connect();out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);running=true;notifyState("Raket bağlı • ekran kapanabilir");while(running&&socket!=null&&socket.isConnected())SystemClock.sleep(1000);}catch(Exception e){running=false;notifyState("Bağlantı koptu • uygulamayı açıp yeniden bağla");}},"MG-Racket-Bluetooth").start();}

    @Override public void onSensorChanged(SensorEvent e){
        if(!running)return;int type=e.sensor.getType();
        if(type==Sensor.TYPE_GYROSCOPE){gyroMag=(float)Math.sqrt(e.values[0]*e.values[0]+e.values[1]*e.values[1]+e.values[2]*e.values[2]);return;}
        if(type==Sensor.TYPE_GAME_ROTATION_VECTOR||type==Sensor.TYPE_ROTATION_VECTOR){
            float[] rm=new float[9],rr=new float[9],ori=new float[3];SensorManager.getRotationMatrixFromVector(rm,e.values);SensorManager.remapCoordinateSystem(rm,SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X,rr);SensorManager.getOrientation(rr,ori);
            float roll=(float)Math.toDegrees(ori[2]);pitchDeg=(float)Math.toDegrees(ori[1]);if(Float.isNaN(neutralPitch))neutralPitch=pitchDeg;
            float dp=pitchDeg-neutralPitch;if(Math.abs(dp)>32f)raisedUntil=System.currentTimeMillis()+1100L;
            tilt=clamp(tilt*.82f+clamp(roll/8f,-6f,6f)*.18f,-6f,6f);long n=System.nanoTime();if(n-lastTilt>33_000_000L){lastTilt=n;send(String.format(Locale.US,"TILT,%.4f",tilt));}return;
        }
        if(e.values.length<3)return;
        float a=(float)Math.sqrt(e.values[0]*e.values[0]+e.values[1]*e.values[1]+e.values[2]*e.values[2]);long now=System.currentTimeMillis();float threshold=type==Sensor.TYPE_LINEAR_ACCELERATION?7.5f:14.5f;
        float direction=clamp(e.values[0]/Math.max(5f,a),-1f,1f);
        boolean raisedForward=now<raisedUntil&&a>threshold*1.12f&&gyroMag>1.45f&&Math.abs(e.values[2])>threshold*.28f&&now-lastSwing>320;
        if(raisedForward){
            lastSwing=now;raisedUntil=0L;float power=clamp((a/threshold)*.58f+(gyroMag/4f)*.42f,.95f,2.65f);send(String.format(Locale.US,"RAISE_HIT,%.4f,%.4f",power,direction));vibrate(34);return;
        }
        if(a>threshold&&gyroMag>1.35f&&now-lastSwing>285){lastSwing=now;float power=clamp((a/threshold)*.54f+(gyroMag/4f)*.46f,.68f,2.45f);send(String.format(Locale.US,"SWING,%.4f,%.4f",power,direction));vibrate(22);}
    }

    private synchronized void send(String line){PrintWriter w=out;if(w!=null){w.println(line);if(w.checkError())running=false;}}
    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private void vibrate(int ms){try{Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v==null)return;if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(ms,VibrationEffect.DEFAULT_AMPLITUDE));else v.vibrate(ms);}catch(Exception ignored){}}
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"MG Tennis Raket",NotificationManager.IMPORTANCE_LOW);c.setDescription("Raket telefonu sensör ve Bluetooth bağlantısı");NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.createNotificationChannel(c);}}
    private Notification notification(String text){Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Intent stop=new Intent(this,RacketService.class).setAction(ACTION_STOP);PendingIntent ps=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("MG Tennis • Raket Modu").setContentText(text).setOngoing(true).setContentIntent(pi).addAction(android.R.drawable.ic_menu_close_clear_cancel,"Durdur",ps).build();}
    private void notifyState(String text){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(nm!=null)nm.notify(NOTIF_ID,notification(text));}
    private void closeSocket(){running=false;try{if(socket!=null)socket.close();}catch(Exception ignored){}socket=null;out=null;}
    @Override public void onDestroy(){sm.unregisterListener(this);closeSocket();if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
