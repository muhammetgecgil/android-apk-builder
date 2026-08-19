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
    private static final UUID SIM_UUID = UUID.fromString("6d9b6c72-4d47-4d8e-9b58-b5e7465b4a22");
    private static final int REQ_BT = 44;
    private static final long TX_PERIOD_NS = 20_000_000L; // 50 Hz

    private BluetoothAdapter bt;
    private BluetoothSocket socket;
    private BluetoothServerSocket serverSocket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final ExecutorService io = Executors.newCachedThreadPool();
    private final Object writeLock = new Object();
    private final AtomicInteger sequence = new AtomicInteger();
    private final ConcurrentHashMap<Integer, Long> txTimes = new ConcurrentHashMap<>();

    private SensorManager sensors;
    private Sensor rotationSensor;
    private FlightView flightView;
    private PilotView pilotView;

    private volatile boolean pilotMode, receiverMode, connected;
    private volatile float roll, pitch, yaw, throttle = 0.62f;
    private volatile float rawRoll, rawPitch, rawYaw;
    private volatile float zeroRoll, zeroPitch, zeroYaw;
    private volatile boolean centerValid;
    private volatile long lastPacketMs, lastTxNs;
    private volatile int lastRxSeq, droppedPackets;
    private volatile float rttMs, linkHz;
    private volatile long previousRxMs;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        bt = BluetoothAdapter.getDefaultAdapter();
        sensors = (SensorManager)getSystemService(SENSOR_SERVICE);
        rotationSensor = sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if (rotationSensor == null) rotationSensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        requestBtIfNeeded();
        showRoleScreen();
    }

    private void requestBtIfNeeded() {
        if (Build.VERSION.SDK_INT >= 31 && (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, REQ_BT);
        }
    }

    private TextView title(String s, int size) {
        TextView t = new TextView(this); t.setText(s); t.setTextColor(Color.rgb(160,255,190)); t.setTextSize(size); t.setGravity(Gravity.CENTER); t.setPadding(16,12,16,12); return t;
    }
    private Button btn(String s) { Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(16); b.setPadding(12,8,12,8); return b; }

    private void showRoleScreen() {
        closeConnections(); pilotMode=receiverMode=false;
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(46,36,46,36); root.setBackgroundColor(Color.rgb(3,9,13));
        root.addView(title("FIXTURE COCKPIT SIM • PRO",32));
        TextView sub=title("Tek APK • Çift telefon • Bluetooth IMU uçuş simülatörü",16); sub.setTextColor(Color.LTGRAY); root.addView(sub);
        Button pilot=btn("PİLOT / FİKSTÜR KOKPİT"); Button display=btn("UÇAK EKRANI / F-22 GÖRSELİ");
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(440),dp(72)); lp.setMargins(0,16,0,0); root.addView(pilot,lp); root.addView(display,lp);
        TextView info=title("1) Telefonları Android Bluetooth ayarlarından eşleştir  2) Uçak ekranını aç  3) Pilot telefonundan Bağlan",14); info.setTextColor(Color.GRAY); root.addView(info);
        pilot.setOnClickListener(v->startPilot()); display.setOnClickListener(v->startReceiver()); setContentView(root);
    }

    private void startPilot() {
        closeConnections(); pilotMode=true; receiverMode=false; centerValid=false; roll=pitch=yaw=0;
        FrameLayout root=new FrameLayout(this); pilotView=new PilotView(this); root.addView(pilotView,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout controls=new LinearLayout(this); controls.setOrientation(LinearLayout.HORIZONTAL); controls.setGravity(Gravity.CENTER); controls.setPadding(8,6,8,6); controls.setBackgroundColor(0x99000000);
        Button connect=btn("BLUETOOTH BAĞLAN"); Button center=btn("IMU MERKEZLE"); Button minus=btn("GAZ −"); Button plus=btn("GAZ +"); Button back=btn("MOD SEÇİMİ");
        controls.addView(connect); controls.addView(center); controls.addView(minus); controls.addView(plus); controls.addView(back);
        root.addView(controls,new FrameLayout.LayoutParams(-1,dp(58),Gravity.BOTTOM)); setContentView(root);
        if(rotationSensor!=null) sensors.registerListener(this,rotationSensor,SensorManager.SENSOR_DELAY_GAME);
        connect.setOnClickListener(v->chooseBondedAndConnect());
        center.setOnClickListener(v->{ zeroRoll=rawRoll; zeroPitch=rawPitch; zeroYaw=rawYaw; centerValid=true; roll=pitch=yaw=0; toast("IMU nötr konumu kaydedildi"); });
        minus.setOnClickListener(v->throttle=Math.max(0,throttle-0.05f)); plus.setOnClickListener(v->throttle=Math.min(1,throttle+0.05f)); back.setOnClickListener(v->showRoleScreen());
    }

    private void startReceiver() {
        closeConnections(); receiverMode=true; pilotMode=false; lastPacketMs=0; lastRxSeq=0; droppedPackets=0;
        FrameLayout root=new FrameLayout(this); flightView=new FlightView(this); root.addView(flightView,new FrameLayout.LayoutParams(-1,-1));
        Button back=btn("MOD SEÇİMİ"); FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(145),dp(50),Gravity.TOP|Gravity.RIGHT); bp.setMargins(0,10,10,0); root.addView(back,bp); back.setOnClickListener(v->showRoleScreen()); setContentView(root); startServer();
    }

    private boolean btAllowed(){return Build.VERSION.SDK_INT<31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
    private void chooseBondedAndConnect(){
        if(bt==null){toast("Bluetooth donanımı yok");return;} if(!btAllowed()){requestBtIfNeeded();return;} if(!bt.isEnabled()){startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));toast("Bluetooth'u açıp tekrar bağlan");return;}
        Set<BluetoothDevice> set=bt.getBondedDevices(); if(set==null||set.isEmpty()){toast("Eşleştirilmiş cihaz yok");return;}
        ArrayList<BluetoothDevice> devices=new ArrayList<>(set); String[] names=new String[devices.size()];
        for(int i=0;i<devices.size();i++){BluetoothDevice d=devices.get(i);names[i]=(d.getName()==null?"Cihaz":d.getName())+"\n"+d.getAddress();}
        new AlertDialog.Builder(this).setTitle("Uçak ekranı telefonunu seç").setItems(names,(dlg,which)->connectTo(devices.get(which))).show();
    }

    private void connectTo(BluetoothDevice device){
        toast("Bağlanıyor: "+device.getName()); io.execute(()->{try{closeSocketOnly(); BluetoothSocket s=device.createRfcommSocketToServiceRecord(SIM_UUID); s.connect(); socket=s; writer=new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.UTF_8)); reader=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8)); connected=true; runOnUiThread(()->toast("Bluetooth bağlantısı kuruldu")); readAcks();}catch(Exception e){connected=false;runOnUiThread(()->toast("Bağlantı başarısız: "+e.getClass().getSimpleName()));}});
    }

    private void readAcks() throws IOException {
        String line; while(pilotMode && connected && reader!=null && (line=reader.readLine())!=null){ if(!line.startsWith("A,"))continue; try{int seq=Integer.parseInt(line.substring(2).trim()); Long t=txTimes.remove(seq); if(t!=null) rttMs=(float)((System.nanoTime()-t)/1_000_000.0); if(txTimes.size()>120) txTimes.clear();}catch(Exception ignored){} }
        connected=false;
    }

    private void startServer(){
        if(bt==null){toast("Bluetooth donanımı yok");return;} if(!btAllowed()){requestBtIfNeeded();return;} if(!bt.isEnabled()) startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        io.execute(()->{while(receiverMode){try{serverSocket=bt.listenUsingRfcommWithServiceRecord("FixtureCockpitSimPro",SIM_UUID); runOnUiThread(()->toast("Uçak ekranı hazır — pilot bekleniyor")); BluetoothSocket s=serverSocket.accept(); socket=s; writer=new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.UTF_8)); connected=true; readPackets(s.getInputStream());}catch(Exception e){connected=false;}finally{try{if(serverSocket!=null)serverSocket.close();}catch(Exception ignored){}}}});
    }

    private void readPackets(InputStream in)throws IOException{
        BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8)); String line;
        while(receiverMode&&(line=r.readLine())!=null){String[] a=line.split(","); if(a.length<7||!"V2".equals(a[0]))continue; try{
            int seq=Integer.parseInt(a[1]); float nr=Float.parseFloat(a[3]),np=Float.parseFloat(a[4]),ny=Float.parseFloat(a[5]),nt=Float.parseFloat(a[6]); long now=System.currentTimeMillis();
            if(lastRxSeq>0 && seq>lastRxSeq+1)droppedPackets+=seq-lastRxSeq-1; lastRxSeq=seq;
            if(previousRxMs>0){float dt=Math.max(1,now-previousRxMs); linkHz=lerp(linkHz,1000f/dt,0.15f);} previousRxMs=now;
            roll=slew(roll,nr,7.5f); pitch=slew(pitch,np,5.0f); yaw=lerpAngle(yaw,ny,0.24f); throttle=lerp(throttle,nt,0.20f); lastPacketMs=now;
            sendAck(seq); if(flightView!=null)flightView.postInvalidateOnAnimation();
        }catch(Exception ignored){}}
        connected=false;
    }
    private void sendAck(int seq){BufferedWriter w=writer;if(w==null)return;try{synchronized(writeLock){w.write("A,"+seq+"\n");w.flush();}}catch(Exception ignored){} }

    @Override public void onSensorChanged(SensorEvent e){
        if(!pilotMode || e.sensor!=rotationSensor)return; float[] rm=new float[9],ori=new float[3]; SensorManager.getRotationMatrixFromVector(rm,e.values); SensorManager.getOrientation(rm,ori);
        rawYaw=(float)Math.toDegrees(ori[0]); rawPitch=(float)Math.toDegrees(ori[1]); rawRoll=(float)Math.toDegrees(ori[2]);
        if(!centerValid){zeroRoll=rawRoll;zeroPitch=rawPitch;zeroYaw=rawYaw;centerValid=true;}
        float tr=clamp(wrap(rawRoll-zeroRoll),-85,85); float tp=clamp(-wrap(rawPitch-zeroPitch),-50,50); float ty=wrap(rawYaw-zeroYaw);
        roll=adaptiveFilter(roll,tr,0.16f,0.38f,20f); pitch=adaptiveFilter(pitch,tp,0.14f,0.34f,15f); yaw=lerpAngle(yaw,ty,0.17f);
        if(pilotView!=null)pilotView.postInvalidateOnAnimation(); sendPacket();
    }
    @Override public void onAccuracyChanged(Sensor s,int accuracy){}

    private void sendPacket(){long now=System.nanoTime(); if(now-lastTxNs<TX_PERIOD_NS)return; lastTxNs=now; BufferedWriter w=writer;if(!connected||w==null)return; int seq=sequence.incrementAndGet(); txTimes.put(seq,now); String msg=String.format(Locale.US,"V2,%d,%d,%.2f,%.2f,%.2f,%.3f\n",seq,now,roll,pitch,yaw,throttle); io.execute(()->{try{synchronized(writeLock){if(writer!=null){writer.write(msg);writer.flush();}}}catch(Exception e){connected=false;}});}

    private void closeSocketOnly(){try{if(socket!=null)socket.close();}catch(Exception ignored){} socket=null;writer=null;reader=null;connected=false;}
    private void closeConnections(){if(sensors!=null)sensors.unregisterListener(this);closeSocketOnly();try{if(serverSocket!=null)serverSocket.close();}catch(Exception ignored){}serverSocket=null;}
    @Override protected void onDestroy(){closeConnections();io.shutdownNow();super.onDestroy();}
    @Override public void onBackPressed(){showRoleScreen();}

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);} private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));} private static float lerp(float a,float b,float t){return a+(b-a)*t;}
    private static float wrap(float a){while(a>180)a-=360;while(a<-180)a+=360;return a;} private static float lerpAngle(float a,float b,float t){return wrap(a+wrap(b-a)*t);}
    private static float slew(float current,float target,float maxStep){float d=wrap(target-current);return current+clamp(d,-maxStep,maxStep);} private static float adaptiveFilter(float current,float target,float slow,float fast,float threshold){float d=Math.abs(wrap(target-current));return lerpAngle(current,target,d>threshold?fast:slow);}

    class PilotView extends View{
        Paint p=new Paint(3); public PilotView(Context c){super(c);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));}
        @Override protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),h=getHeight();c.drawColor(Color.rgb(4,12,17));float cy=h/2f+pitch*5.2f;
            c.save();c.rotate(-roll,w/2f,h/2f);p.setColor(Color.rgb(18,67,96));c.drawRect(-w,cy-1400,w*2,cy,p);p.setColor(Color.rgb(78,52,30));c.drawRect(-w,cy,w*2,cy+1400,p);p.setColor(Color.WHITE);p.setStrokeWidth(4);c.drawLine(-w,cy,w*2,cy,p);drawPitchLadder(c,w,cy);c.restore();
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(Color.rgb(120,255,170));c.drawCircle(w/2f,h/2f,105,p);c.drawLine(w/2f-145,h/2f,w/2f-36,h/2f,p);c.drawLine(w/2f+36,h/2f,w/2f+145,h/2f,p);c.drawLine(w/2f,h/2f-18,w/2f,h/2f+38,p);p.setStyle(Paint.Style.FILL);
            p.setTextSize(25);p.setTextAlign(Paint.Align.LEFT);c.drawText(String.format(Locale.US,"ROLL %+05.1f°",roll),24,38,p);c.drawText(String.format(Locale.US,"PITCH %+05.1f°",pitch),24,70,p);c.drawText(String.format(Locale.US,"HDG %03.0f°",(yaw+360)%360),24,102,p);
            p.setTextAlign(Paint.Align.RIGHT);p.setColor(connected?Color.rgb(90,255,130):Color.rgb(255,90,70));c.drawText(connected?"DATA LINK ●":"DATA LINK ○",w-24,38,p);p.setColor(Color.rgb(120,255,170));c.drawText(String.format(Locale.US,"RTT %.0f ms",rttMs),w-24,70,p);c.drawText(String.format(Locale.US,"THR %3.0f%%",throttle*100),w-24,102,p);p.setTextAlign(Paint.Align.LEFT);postInvalidateDelayed(33);
        }
        private void drawPitchLadder(Canvas c,int w,float cy){p.setTextSize(14);p.setStrokeWidth(2);for(int d=-30;d<=30;d+=10){if(d==0)continue;float y=cy-d*5.2f;float len=(d%20==0)?95:65;p.setColor(0xCCFFFFFF);c.drawLine(w/2f-len,y,w/2f-20,y,p);c.drawLine(w/2f+20,y,w/2f+len,y,p);c.drawText(Integer.toString(Math.abs(d)),w/2f+len+8,y+5,p);}}
    }

    class FlightView extends View{
        Paint p=new Paint(3); long t0=System.currentTimeMillis(),lastFrame=t0; float altitude=5200f,simSpeed=520f,verticalSpeed;
        public FlightView(Context c){super(c);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),h=getHeight();long now=System.currentTimeMillis();float dt=clamp((now-lastFrame)/1000f,0.001f,0.05f);lastFrame=now;boolean live=connected&&now-lastPacketMs<1200;
            float targetSpeed=280+throttle*920;simSpeed=lerp(simSpeed,targetSpeed,0.035f);verticalSpeed=(float)(Math.sin(Math.toRadians(pitch))*simSpeed*0.70f);altitude=Math.max(0,altitude+verticalSpeed*dt);
            LinearGradient sky=new LinearGradient(0,0,0,h,Color.rgb(3,18,36),Color.rgb(55,118,155),Shader.TileMode.CLAMP);p.setShader(sky);c.drawRect(0,0,w,h,p);p.setShader(null);drawMovingClouds(c,w,h,now);
            float horizon=h*0.55f+pitch*4.0f;c.save();c.rotate(-roll*0.42f,w/2f,h/2f);p.setColor(Color.rgb(31,52,43));c.drawRect(-w,horizon,w*2,h*2,p);p.setColor(Color.rgb(116,142,124));p.setStrokeWidth(3);c.drawLine(-w,horizon,w*2,horizon,p);drawTerrain(c,w,horizon,now);c.restore();
            drawJet(c,w,h,now);drawHud(c,w,h,simSpeed,altitude,verticalSpeed,live,now);if(!live)drawLinkLost(c,w,h);postInvalidateDelayed(16);
        }
        private void drawMovingClouds(Canvas c,int w,int h,long now){p.setColor(0x28FFFFFF);float drift=((now-t0)*0.02f*(0.4f+throttle))%w;for(int i=0;i<7;i++){float x=(i*223+drift)% (w+220)-110;float y=55+(i*67)%(int)(h*0.34f);c.drawOval(x-80,y-18,x+80,y+18,p);}}
        private void drawTerrain(Canvas c,int w,float horizon,long now){p.setColor(Color.rgb(22,43,35));Path m=new Path();m.moveTo(-w,horizon+90);for(int x=-w;x<=w*2;x+=90){float y=horizon+65+(float)Math.sin((x+(now-t0)*0.02)/150.0)*32;m.lineTo(x,y);}m.lineTo(w*2,getHeight()*2);m.lineTo(-w,getHeight()*2);m.close();c.drawPath(m,p);}
        private void drawJet(Canvas c,int w,int h,long now){float cx=w/2f,cy=h*0.62f+pitch*1.3f;float scale=Math.min(w,h)/620f;c.save();c.translate(cx,cy);c.rotate(roll*0.92f);c.scale(scale,scale);
            p.setShadowLayer(20,0,8,0x88000000);p.setColor(Color.rgb(72,82,90));Path body=new Path();body.moveTo(0,-130);body.lineTo(28,-58);body.lineTo(118,12);body.lineTo(55,26);body.lineTo(30,104);body.lineTo(9,84);body.lineTo(0,126);body.lineTo(-9,84);body.lineTo(-30,104);body.lineTo(-55,26);body.lineTo(-118,12);body.lineTo(-28,-58);body.close();c.drawPath(body,p);p.clearShadowLayer();
            p.setColor(Color.rgb(38,47,54));Path canopy=new Path();canopy.moveTo(0,-88);canopy.lineTo(16,-48);canopy.lineTo(0,-18);canopy.lineTo(-16,-48);canopy.close();c.drawPath(canopy,p);p.setColor(Color.rgb(115,125,130));p.setStrokeWidth(3);c.drawLine(-90,12,90,12,p);
            float flame=18+throttle*38+(float)Math.sin(now/55.0)*4;p.setColor(Color.rgb(255,150,45));c.drawOval(-25,93,-7,93+flame,p);c.drawOval(7,93,25,93+flame,p);p.setColor(Color.rgb(255,230,120));c.drawOval(-20,95,-12,95+flame*.55f,p);c.drawOval(12,95,20,95+flame*.55f,p);c.restore();}
        private void drawHud(Canvas c,int w,int h,float speed,float alt,float vs,boolean live,long now){p.setColor(Color.rgb(110,255,150));p.setStrokeWidth(3);p.setStyle(Paint.Style.STROKE);c.drawCircle(w/2f,h/2f,62,p);c.drawLine(w/2f-120,h/2f,w/2f-30,h/2f,p);c.drawLine(w/2f+30,h/2f,w/2f+120,h/2f,p);p.setStyle(Paint.Style.FILL);p.setTextSize(22);
            c.drawText(String.format(Locale.US,"SPD %4.0f kt",speed),22,42,p);c.drawText(String.format(Locale.US,"ALT %5.0f ft",alt),22,72,p);c.drawText(String.format(Locale.US,"V/S %+5.0f",vs),22,102,p);c.drawText(String.format(Locale.US,"HDG %03.0f°",(yaw+360)%360),w/2f-60,34,p);
            p.setTextAlign(Paint.Align.RIGHT);c.drawText(String.format(Locale.US,"ROLL %+05.1f°",roll),w-22,42,p);c.drawText(String.format(Locale.US,"PITCH %+05.1f°",pitch),w-22,72,p);c.drawText(String.format(Locale.US,"THR %3.0f%%",throttle*100),w-22,102,p);c.drawText(String.format(Locale.US,"LINK %.0f Hz  DROP %d",linkHz,droppedPackets),w-22,h-28,p);p.setTextAlign(Paint.Align.LEFT);
            drawCompassTape(c,w);drawBankScale(c,w,h);if(live){p.setColor(Color.rgb(90,255,130));p.setTextSize(14);c.drawText("IMU DATA LINK • LIVE",20,h-28,p);}
        }
        private void drawCompassTape(Canvas c,int w){p.setTextSize(13);p.setColor(Color.rgb(110,255,150));float hdg=(yaw+360)%360;for(int d=-40;d<=40;d+=10){float x=w/2f+d*5.2f;float val=(hdg+d+360)%360;c.drawLine(x,50,x,62,p);c.drawText(String.format(Locale.US,"%03.0f",val),x-11,78,p);}}
        private void drawBankScale(Canvas c,int w,int h){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(110,255,150));RectF r=new RectF(w/2f-145,h/2f-145,w/2f+145,h/2f+145);c.drawArc(r,205,130,false,p);p.setStyle(Paint.Style.FILL);}
        private void drawLinkLost(Canvas c,int w,int h){p.setColor(0xB0000000);c.drawRect(0,0,w,h,p);p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.rgb(255,185,70));p.setTextSize(34);c.drawText("IMU DATA LINK BEKLENİYOR",w/2f,h/2f-8,p);p.setTextSize(17);c.drawText("Pilot telefonunda BLUETOOTH BAĞLAN → bu telefonu seç",w/2f,h/2f+30,p);p.setTextAlign(Paint.Align.LEFT);}
    }
}
