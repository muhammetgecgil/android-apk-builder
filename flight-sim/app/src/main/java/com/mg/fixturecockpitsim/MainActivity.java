package com.mg.fixturecockpitsim;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity implements SensorEventListener {
    private static final UUID SIM_UUID=UUID.fromString("6d9b6c72-4d47-4d8e-9b58-b5e7465b4a22");
    private static final long TX_PERIOD_NS=20_000_000L;
    private static final int REQ_BT=44;

    private BluetoothAdapter bt; private BluetoothSocket socket; private BluetoothServerSocket server;
    private BufferedWriter writer; private BufferedReader reader;
    private final ExecutorService io=Executors.newCachedThreadPool(); private final Object writeLock=new Object();
    private final AtomicInteger seq=new AtomicInteger(); private final ConcurrentHashMap<Integer,Long> txTimes=new ConcurrentHashMap<>();
    private SensorManager sensors; private Sensor rotationSensor;
    private PilotView pilotView; private AircraftDisplayView aircraftDisplay;
    private volatile boolean pilotMode,receiverMode,connected;
    private volatile float roll,pitch,yaw,throttle=.68f,rawRoll,rawPitch,rawYaw,zeroRoll,zeroPitch,zeroYaw,rttMs,linkHz;
    private volatile boolean centered; private volatile long lastTxNs,previousRxMs; private volatile int lastRxSeq,drops;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        bt=BluetoothAdapter.getDefaultAdapter();sensors=(SensorManager)getSystemService(SENSOR_SERVICE);rotationSensor=sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);if(rotationSensor==null)rotationSensor=sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        requestBt();showRoleScreen();
    }

    private void requestBt(){if(Build.VERSION.SDK_INT>=31&&(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED))requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN},REQ_BT);}
    private boolean btAllowed(){return Build.VERSION.SDK_INT<31||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);return b;}
    private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(125,255,165));t.setGravity(Gravity.CENTER);t.setPadding(12,10,12,10);return t;}

    private void showRoleScreen(){
        closeConnections();pilotMode=receiverMode=false;aircraftDisplay=null;
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(42,28,42,28);root.setBackgroundColor(Color.rgb(2,8,13));
        root.addView(label("FIXTURE COCKPIT SIM • 3D PRO V4",30));TextView sub=label("Tek APK • iki telefon • Bluetooth IMU • 3D stealth fighter • offline demo",15);sub.setTextColor(Color.LTGRAY);root.addView(sub);
        Button p=button("PİLOT / KOKPİT TELEFONU");Button d=button("UÇAK + KOKPİT EKRANI");LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(500),dp(70));lp.setMargins(0,14,0,0);root.addView(p,lp);root.addView(d,lp);
        TextView info=label("Uçak tarafı Bluetooth olmadan DEMO ile çalışır. Bluetooth bağlanınca gerçek IMU otomatik devralır.",14);info.setTextColor(Color.GRAY);root.addView(info);
        p.setOnClickListener(v->startPilot());d.setOnClickListener(v->startReceiver());setContentView(root);
    }

    private void startPilot(){
        closeConnections();pilotMode=true;receiverMode=false;centered=false;roll=pitch=yaw=0;
        FrameLayout root=new FrameLayout(this);pilotView=new PilotView(this);root.addView(pilotView,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER);bar.setBackgroundColor(0xD8000000);Button conn=button("DATA LINK");Button center=button("IMU MERKEZLE");Button minus=button("THR −");Button plus=button("THR +");Button back=button("MOD");bar.addView(conn);bar.addView(center);bar.addView(minus);bar.addView(plus);bar.addView(back);root.addView(bar,new FrameLayout.LayoutParams(-1,dp(56),Gravity.BOTTOM));setContentView(root);
        if(rotationSensor!=null)sensors.registerListener(this,rotationSensor,SensorManager.SENSOR_DELAY_GAME);
        conn.setOnClickListener(v->chooseDevice());center.setOnClickListener(v->{zeroRoll=rawRoll;zeroPitch=rawPitch;zeroYaw=rawYaw;centered=true;roll=pitch=yaw=0;toast("IMU merkezlendi");});minus.setOnClickListener(v->throttle=Math.max(0,throttle-.05f));plus.setOnClickListener(v->throttle=Math.min(1,throttle+.05f));back.setOnClickListener(v->showRoleScreen());
    }

    private void startReceiver(){
        closeConnections();receiverMode=true;pilotMode=false;lastRxSeq=drops=0;previousRxMs=0;
        FrameLayout root=new FrameLayout(this);aircraftDisplay=new AircraftDisplayView(this);root.addView(aircraftDisplay,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);bar.setBackgroundColor(0x66000000);Button demo=button("DEMO ON/OFF");Button back=button("MOD");bar.addView(demo);bar.addView(back);FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-2,dp(50),Gravity.TOP|Gravity.RIGHT);bp.setMargins(0,8,8,0);root.addView(bar,bp);setContentView(root);
        demo.setOnClickListener(v->{if(aircraftDisplay!=null){boolean next=!aircraftDisplay.isDemoActive();aircraftDisplay.setDemoEnabled(next);toast(next?"Demo modu açık":"Demo modu kapalı");}});back.setOnClickListener(v->showRoleScreen());
        if(bt!=null&&btAllowed()&&bt.isEnabled())startServer();else toast("Bluetooth yok/açık değil — DEMO FLIGHT çalışıyor");
    }

    private void chooseDevice(){
        if(bt==null){toast("Bluetooth donanımı yok");return;}if(!btAllowed()){requestBt();return;}if(!bt.isEnabled()){startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));toast("Bluetooth'u açıp tekrar DATA LINK'e bas");return;}
        Set<BluetoothDevice> set=bt.getBondedDevices();if(set==null||set.isEmpty()){toast("Önce iki telefonu Android Bluetooth ayarından eşleştir");return;}ArrayList<BluetoothDevice> list=new ArrayList<>(set);String[] names=new String[list.size()];for(int i=0;i<list.size();i++){BluetoothDevice d=list.get(i);names[i]=(d.getName()==null?"Cihaz":d.getName())+"\n"+d.getAddress();}
        new AlertDialog.Builder(this).setTitle("Uçak ekranı telefonunu seç").setItems(names,(x,i)->connectTo(list.get(i))).show();
    }

    private void connectTo(BluetoothDevice d){
        toast("Bağlanıyor...");io.execute(()->{try{closeSocketOnly();BluetoothSocket s=d.createRfcommSocketToServiceRecord(SIM_UUID);s.connect();socket=s;writer=new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.UTF_8));reader=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8));connected=true;runOnUiThread(()->toast("Bluetooth IMU link kuruldu"));readAcks();}catch(Exception e){connected=false;runOnUiThread(()->toast("Bağlantı başarısız: "+e.getClass().getSimpleName()));}});
    }

    private void startServer(){
        io.execute(()->{while(receiverMode){try{server=bt.listenUsingRfcommWithServiceRecord("FixtureCockpit3D",SIM_UUID);runOnUiThread(()->toast("DEMO aktif • pilot bağlantısı bekleniyor"));BluetoothSocket s=server.accept();socket=s;writer=new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.UTF_8));connected=true;readPackets(s.getInputStream());}catch(Exception ignored){connected=false;}finally{try{if(server!=null)server.close();}catch(Exception ignored){}}}});
    }

    private void readPackets(InputStream in)throws IOException{
        BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));String line;while(receiverMode&&(line=r.readLine())!=null){String[] a=line.split(",");if(a.length<7||!"V2".equals(a[0]))continue;try{int n=Integer.parseInt(a[1]);float nr=Float.parseFloat(a[3]),np=Float.parseFloat(a[4]),ny=Float.parseFloat(a[5]),nt=Float.parseFloat(a[6]);long now=System.currentTimeMillis();if(lastRxSeq>0&&n>lastRxSeq+1)drops+=n-lastRxSeq-1;lastRxSeq=n;if(previousRxMs>0){float dt=Math.max(1,now-previousRxMs);linkHz=lerp(linkHz,1000f/dt,.15f);}previousRxMs=now;roll=slew(roll,nr,8f);pitch=slew(pitch,np,5f);yaw=lerpAngle(yaw,ny,.24f);throttle=lerp(throttle,nt,.2f);sendAck(n);AircraftDisplayView av=aircraftDisplay;if(av!=null)runOnUiThread(()->av.setTelemetry(roll,pitch,yaw,throttle,linkHz,drops));}catch(Exception ignored){}}connected=false;
    }

    private void readAcks()throws IOException{String line;while(pilotMode&&connected&&reader!=null&&(line=reader.readLine())!=null){if(!line.startsWith("A,"))continue;try{int n=Integer.parseInt(line.substring(2).trim());Long t=txTimes.remove(n);if(t!=null)rttMs=(System.nanoTime()-t)/1_000_000f;}catch(Exception ignored){}}connected=false;}
    private void sendAck(int n){try{synchronized(writeLock){if(writer!=null){writer.write("A,"+n+"\n");writer.flush();}}}catch(Exception ignored){}}

    @Override public void onSensorChanged(SensorEvent e){
        if(!pilotMode||e.sensor!=rotationSensor)return;float[] rm=new float[9],o=new float[3];SensorManager.getRotationMatrixFromVector(rm,e.values);SensorManager.getOrientation(rm,o);rawYaw=(float)Math.toDegrees(o[0]);rawPitch=(float)Math.toDegrees(o[1]);rawRoll=(float)Math.toDegrees(o[2]);if(!centered){zeroRoll=rawRoll;zeroPitch=rawPitch;zeroYaw=rawYaw;centered=true;}float tr=clamp(wrap(rawRoll-zeroRoll),-85,85),tp=clamp(-wrap(rawPitch-zeroPitch),-50,50),ty=wrap(rawYaw-zeroYaw);roll=adaptive(roll,tr,.16f,.38f,20);pitch=adaptive(pitch,tp,.14f,.34f,15);yaw=lerpAngle(yaw,ty,.17f);if(pilotView!=null)pilotView.postInvalidateOnAnimation();sendPacket();
    }
    @Override public void onAccuracyChanged(Sensor s,int a){}
    private void sendPacket(){long now=System.nanoTime();if(now-lastTxNs<TX_PERIOD_NS)return;lastTxNs=now;if(!connected||writer==null)return;int n=seq.incrementAndGet();txTimes.put(n,now);String msg=String.format(Locale.US,"V2,%d,%d,%.2f,%.2f,%.2f,%.3f\n",n,now,roll,pitch,yaw,throttle);io.execute(()->{try{synchronized(writeLock){if(writer!=null){writer.write(msg);writer.flush();}}}catch(Exception e){connected=false;}});}

    private void closeSocketOnly(){try{if(socket!=null)socket.close();}catch(Exception ignored){}socket=null;writer=null;reader=null;connected=false;}
    private void closeConnections(){if(sensors!=null)sensors.unregisterListener(this);closeSocketOnly();try{if(server!=null)server.close();}catch(Exception ignored){}server=null;}
    @Override protected void onDestroy(){closeConnections();io.shutdownNow();super.onDestroy();}
    @Override public void onBackPressed(){showRoleScreen();}

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}private static float lerp(float a,float b,float t){return a+(b-a)*t;}private static float wrap(float a){while(a>180)a-=360;while(a<-180)a+=360;return a;}private static float lerpAngle(float a,float b,float t){return wrap(a+wrap(b-a)*t);}private static float slew(float a,float b,float s){float d=wrap(b-a);return a+clamp(d,-s,s);}private static float adaptive(float a,float b,float slow,float fast,float th){return lerpAngle(a,b,Math.abs(wrap(b-a))>th?fast:slow);}

    private final class PilotView extends View{
        private final Paint p=new Paint(3);PilotView(Context c){super(c);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));}
        @Override protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();float cy=h*.43f+pitch*5f;c.drawColor(Color.rgb(7,18,28));c.save();c.rotate(-roll,w/2f,cy);p.setColor(Color.rgb(52,119,170));c.drawRect(-w,-h,w*2,cy,p);p.setColor(Color.rgb(72,79,52));c.drawRect(-w,cy,w*2,h*2,p);p.setColor(Color.WHITE);p.setStrokeWidth(4);c.drawLine(-w,cy,w*2,cy,p);c.restore();
            p.setColor(Color.rgb(105,255,145));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawCircle(w/2f,h*.38f,52,p);c.drawLine(w/2f-120,h*.38f,w/2f-30,h*.38f,p);c.drawLine(w/2f+30,h*.38f,w/2f+120,h*.38f,p);p.setStyle(Paint.Style.FILL);p.setTextSize(20);c.drawText(String.format(Locale.US,"ROLL %+05.1f°",roll),24,42,p);c.drawText(String.format(Locale.US,"PITCH %+05.1f°",pitch),24,70,p);c.drawText(String.format(Locale.US,"HDG %03.0f°",(yaw+360)%360),24,98,p);p.setTextAlign(Paint.Align.RIGHT);c.drawText(String.format(Locale.US,"THR %3.0f%%",throttle*100),w-24,42,p);c.drawText(connected?String.format(Locale.US,"LINK LIVE  RTT %.0f ms",rttMs):"LINK STANDBY",w-24,70,p);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(17);c.drawText("PİLOT / IMU KOKPİT",w/2f,34,p);p.setTextAlign(Paint.Align.LEFT);postInvalidateDelayed(16);
        }
    }
}
