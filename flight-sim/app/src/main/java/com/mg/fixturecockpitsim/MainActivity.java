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
    private static final long TX_PERIOD_NS = 20_000_000L;

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
        startPilot();
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
        // Legacy role selector deliberately kept unreachable. Aircraft display is exclusively Display3DActivity.
        finish();
    }

    private void startPilot() {
        closeConnections(); pilotMode=true; receiverMode=false; centerValid=false; roll=pitch=yaw=0;
        FrameLayout root=new FrameLayout(this); pilotView=new PilotView(this); root.addView(pilotView,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout controls=new LinearLayout(this); controls.setOrientation(LinearLayout.HORIZONTAL); controls.setGravity(Gravity.CENTER); controls.setPadding(8,5,8,5); controls.setBackgroundColor(0xD0000000);
        Button connect=btn("DATA LINK"); Button center=btn("IMU MERKEZLE"); Button minus=btn("THR −"); Button plus=btn("THR +"); Button back=btn("GERİ");
        controls.addView(connect); controls.addView(center); controls.addView(minus); controls.addView(plus); controls.addView(back);
        root.addView(controls,new FrameLayout.LayoutParams(-1,dp(56),Gravity.BOTTOM)); setContentView(root);
        if(rotationSensor!=null) sensors.registerListener(this,rotationSensor,SensorManager.SENSOR_DELAY_GAME);
        connect.setOnClickListener(v->chooseBondedAndConnect());
        center.setOnClickListener(v->{ zeroRoll=rawRoll; zeroPitch=rawPitch; zeroYaw=rawYaw; centerValid=true; roll=pitch=yaw=0; toast("IMU nötr konumu kaydedildi"); });
        minus.setOnClickListener(v->throttle=Math.max(0,throttle-0.05f)); plus.setOnClickListener(v->throttle=Math.min(1,throttle+0.05f)); back.setOnClickListener(v->finish());
    }

    private void startReceiver() {
        // Legacy receiver is intentionally unreachable; the only aircraft renderer is Display3DActivity.
        Intent i=new Intent(this,Display3DActivity.class);
        i.putExtra(LauncherActivity.EXTRA_DEMO_MODE,false);
        startActivity(i);
        finish();
    }

    private boolean btAllowed(){return Build.VERSION.SDK_INT<31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
    private void chooseBondedAndConnect(){
        if(bt==null){toast("Bluetooth donanımı yok");return;} if(!btAllowed()){requestBtIfNeeded();return;} if(!bt.isEnabled()){startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));toast("Bluetooth'u açıp tekrar bağlan");return;}
        Set<BluetoothDevice> set=bt.getBondedDevices(); if(set==null||set.isEmpty()){toast("Eşleştirilmiş cihaz yok");return;}
        ArrayList<BluetoothDevice> devices=new ArrayList<>(set); String[] names=new String[devices.size()];
        for(int i=0;i<devices.size();i++){BluetoothDevice d=devices.get(i);names[i]=(d.getName()==null?"Cihaz":d.getName())+"\n"+d.getAddress();}
        new AlertDialog.Builder(this).setTitle("Yeni uçak ekranı telefonunu seç").setItems(names,(dlg,which)->connectTo(devices.get(which))).show();
    }

    private void connectTo(BluetoothDevice device){
        toast("Bağlanıyor: "+device.getName()); io.execute(()->{try{closeSocketOnly(); BluetoothSocket s=device.createRfcommSocketToServiceRecord(SIM_UUID); s.connect(); socket=s; writer=new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.UTF_8)); reader=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8)); connected=true; runOnUiThread(()->toast("Yeni uçak data link kuruldu")); readAcks();}catch(Exception e){connected=false;runOnUiThread(()->toast("Bağlantı başarısız: "+e.getClass().getSimpleName()));}});
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
    @Override public void onBackPressed(){finish();}

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);} private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));} private static float lerp(float a,float b,float t){return a+(b-a)*t;}
    private static float wrap(float a){while(a>180)a-=360;while(a<-180)a+=360;return a;} private static float lerpAngle(float a,float b,float t){return wrap(a+wrap(b-a)*t);}
    private static float slew(float current,float target,float maxStep){float d=wrap(target-current);return current+clamp(d,-maxStep,maxStep);} private static float adaptiveFilter(float current,float target,float slow,float fast,float threshold){float d=Math.abs(wrap(target-current));return lerpAngle(current,target,d>threshold?fast:slow);}

    private void drawAtmosphere(Canvas c, Paint p, int w, int h, float horizon, long now, float bank){
        LinearGradient sky=new LinearGradient(0,0,0,horizon+80,new int[]{Color.rgb(4,22,48),Color.rgb(33,88,142),Color.rgb(130,185,220),Color.rgb(224,216,184)},null,Shader.TileMode.CLAMP);p.setShader(sky);c.drawRect(0,0,w,h,p);p.setShader(null);
        p.setShader(new RadialGradient(w*0.78f,h*0.18f,Math.min(w,h)*0.22f,new int[]{0xCCFFF7C8,0x55FFF2B0,0x00FFFFFF},null,Shader.TileMode.CLAMP));c.drawCircle(w*0.78f,h*0.18f,Math.min(w,h)*0.24f,p);p.setShader(null);
        c.save();c.rotate(-bank*0.18f,w/2f,h/2f);for(int i=0;i<10;i++){float drift=((now/24f)*(0.2f+throttle*0.5f)+i*171)%(w+420)-210;float y=70+(i*83)%(int)Math.max(110,horizon*0.7f);float s=0.6f+(i%4)*0.18f;drawCloud(c,p,drift,y,115*s,30*s,0x70FFFFFF);}c.restore();
    }

    private void drawCloud(Canvas c, Paint p, float x,float y,float rx,float ry,int color){
        p.setShader(new RadialGradient(x,y,rx,new int[]{color,0x44FFFFFF,0x00FFFFFF},null,Shader.TileMode.CLAMP));c.drawOval(x-rx,y-ry,x+rx,y+ry,p);p.setShader(null);
        p.setColor(0x45FFFFFF);c.drawOval(x-rx*0.55f,y-ry*1.1f,x+rx*0.2f,y+ry*0.35f,p);c.drawOval(x-rx*0.05f,y-ry*0.9f,x+rx*0.7f,y+ry*0.45f,p);
    }

    class PilotView extends View{
        Paint p=new Paint(3); long t0=System.currentTimeMillis(); public PilotView(Context c){super(c);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),h=getHeight();long now=System.currentTimeMillis();float horizon=h*0.48f+pitch*4.7f;
            drawAtmosphere(c,p,w,h,horizon,now,roll);
            c.save();c.rotate(-roll*0.34f,w/2f,h/2f);drawWorld(c,w,h,horizon,now);c.restore();
            drawCanopy(c,w,h);drawHudGlass(c,w,h);drawHud(c,w,h);drawCockpitPanel(c,w,h);postInvalidateDelayed(16);
        }
        private void drawWorld(Canvas c,int w,int h,float horizon,long now){
            LinearGradient ground=new LinearGradient(0,horizon,0,h,new int[]{Color.rgb(74,92,72),Color.rgb(38,52,39),Color.rgb(18,27,22)},null,Shader.TileMode.CLAMP);p.setShader(ground);c.drawRect(-w,horizon,w*2,h*2,p);p.setShader(null);
            p.setColor(0x50D8CFAE);p.setStrokeWidth(2);for(int i=1;i<8;i++){float y=horizon+(h-horizon)*(i*i)/64f;c.drawLine(-w,y,w*2,y,p);}float shift=(now-t0)*0.035f;for(int i=-10;i<20;i++){float x=w/2f+(i*85f-(shift%85));c.drawLine(x,horizon,x+(x-w/2f)*1.8f,h,p);}p.setColor(Color.rgb(62,78,58));Path ridge=new Path();ridge.moveTo(-w,horizon+40);for(int x=-w;x<=w*2;x+=80)ridge.lineTo(x,horizon+28+(float)Math.sin((x+shift)/115.0)*32);ridge.lineTo(w*2,h);ridge.lineTo(-w,h);ridge.close();c.drawPath(ridge,p);
        }
        private void drawCanopy(Canvas c,int w,int h){
            p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(Math.max(18,w*0.022f));p.setColor(Color.rgb(25,30,31));Path frame=new Path();frame.moveTo(w*0.06f,h*0.04f);frame.quadTo(w*0.12f,h*0.20f,w*0.17f,h*0.70f);frame.moveTo(w*0.94f,h*0.04f);frame.quadTo(w*0.88f,h*0.20f,w*0.83f,h*0.70f);frame.moveTo(w*0.50f,0);frame.lineTo(w*0.50f,h*0.18f);c.drawPath(frame,p);p.setStrokeWidth(5);p.setColor(0x557FB1C5);c.drawPath(frame,p);p.setStyle(Paint.Style.FILL);p.setStrokeCap(Paint.Cap.BUTT);
        }
        private void drawHudGlass(Canvas c,int w,int h){
            float left=w*0.31f,right=w*0.69f,top=h*0.10f,bottom=h*0.56f;Path g=new Path();g.moveTo(left,top+25);g.lineTo(left+40,top);g.lineTo(right-40,top);g.lineTo(right,top+25);g.lineTo(right-15,bottom);g.lineTo(left+15,bottom);g.close();p.setShader(new LinearGradient(0,top,0,bottom,0x253C8F84,0x061B3F3D,Shader.TileMode.CLAMP));c.drawPath(g,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(0xAA5A6B64);c.drawPath(g,p);p.setStyle(Paint.Style.FILL);
        }
        private void drawHud(Canvas c,int w,int h){
            int green=Color.rgb(112,255,150);p.setColor(green);p.setStrokeWidth(3);p.setTextSize(18);p.setTextAlign(Paint.Align.CENTER);float cx=w/2f,cy=h*0.33f;
            p.setStyle(Paint.Style.STROKE);c.drawCircle(cx,cy,46,p);c.drawLine(cx-92,cy,cx-26,cy,p);c.drawLine(cx+26,cy,cx+92,cy,p);c.drawLine(cx,cy-17,cx,cy+22,p);for(int d=-30;d<=30;d+=10){if(d==0)continue;float y=cy-d*3.4f;c.drawLine(cx-58,y,cx-20,y,p);c.drawLine(cx+20,y,cx+58,y,p);}p.setStyle(Paint.Style.FILL);
            c.drawText(String.format(Locale.US,"%03.0f",(yaw+360)%360),cx,h*0.145f,p);p.setTextAlign(Paint.Align.LEFT);c.drawText(String.format(Locale.US,"SPD %3.0f",280+throttle*920),w*0.315f,h*0.25f,p);p.setTextAlign(Paint.Align.RIGHT);c.drawText(String.format(Locale.US,"ALT %4.0f",5200+pitch*35),w*0.685f,h*0.25f,p);p.setTextAlign(Paint.Align.LEFT);
        }
        private void drawCockpitPanel(Canvas c,int w,int h){
            float top=h*0.61f;p.setShader(new LinearGradient(0,top,0,h,Color.rgb(35,38,39),Color.rgb(8,10,11),Shader.TileMode.CLAMP));c.drawRoundRect(0,top,w,h,28,28,p);p.setShader(null);p.setColor(Color.rgb(65,69,70));c.drawRect(w*0.02f,top+10,w*0.98f,top+20,p);
            drawMfd(c,w*0.08f,top+36,w*0.34f,h-62,"ENG / FUEL",true);drawMfd(c,w*0.66f,top+36,w*0.92f,h-62,"NAV / LINK",false);drawCenterStack(c,w,h,top);
        }
        private void drawMfd(Canvas c,float l,float t,float r,float b,String title,boolean engine){
            p.setColor(Color.rgb(12,15,16));c.drawRoundRect(l-12,t-12,r+12,b+12,16,16,p);p.setColor(Color.rgb(48,53,54));c.drawRoundRect(l-5,t-5,r+5,b+5,10,10,p);p.setColor(Color.rgb(5,18,15));c.drawRect(l,t,r,b,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(95,255,145));c.drawRect(l,t,r,b,p);p.setStyle(Paint.Style.FILL);p.setTextSize(13);p.setColor(Color.rgb(105,255,150));c.drawText(title,l+8,t+18,p);
            if(engine){c.drawText(String.format(Locale.US,"THR   %3.0f %%",throttle*100),l+12,t+48,p);c.drawText(String.format(Locale.US,"RPM   %3.0f %%",42+throttle*58),l+12,t+70,p);c.drawText("FUEL  74 %",l+12,t+92,p);c.drawText("NOZ   AUTO",l+12,t+114,p);}else{c.drawText(String.format(Locale.US,"HDG   %03.0f",(yaw+360)%360),l+12,t+48,p);c.drawText(String.format(Locale.US,"ROLL  %+04.0f",roll),l+12,t+70,p);c.drawText(String.format(Locale.US,"PITCH %+04.0f",pitch),l+12,t+92,p);c.drawText(connected?"DATALINK  GOOD":"DATALINK  STBY",l+12,t+114,p);}
            for(int i=0;i<5;i++){p.setColor(Color.rgb(75,82,82));c.drawCircle(l+18+i*(r-l-36)/4,b+13,4,p);}
        }
        private void drawCenterStack(Canvas c,int w,int h,float top){
            float l=w*0.39f,r=w*0.61f,t=top+36,b=h-62;p.setColor(Color.rgb(18,21,22));c.drawRoundRect(l,t,r,b,10,10,p);p.setColor(Color.rgb(90,94,90));p.setTextSize(12);p.setTextAlign(Paint.Align.CENTER);c.drawText("MASTER ARM   SAFE",w/2f,t+22,p);c.drawText(connected?"LINK 1   READY":"LINK 1   STBY",w/2f,t+43,p);c.drawText(String.format(Locale.US,"RTT %.0f ms",rttMs),w/2f,t+64,p);p.setColor(Color.rgb(255,170,55));c.drawCircle(w/2f-34,t+88,6,p);p.setColor(connected?Color.rgb(80,255,120):Color.rgb(255,70,55));c.drawCircle(w/2f+34,t+88,6,p);p.setTextAlign(Paint.Align.LEFT);
        }
    }

    class FlightView extends View{
        Paint p=new Paint(3); long t0=System.currentTimeMillis(),lastFrame=t0; float altitude=5200f,simSpeed=520f,verticalSpeed;
        public FlightView(Context c){super(c);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),h=getHeight();long now=System.currentTimeMillis();float dt=clamp((now-lastFrame)/1000f,0.001f,0.05f);lastFrame=now;boolean live=connected&&now-lastPacketMs<1200;
            float targetSpeed=280+throttle*920;simSpeed=lerp(simSpeed,targetSpeed,0.035f);verticalSpeed=(float)(Math.sin(Math.toRadians(pitch))*simSpeed*0.70f);altitude=Math.max(0,altitude+verticalSpeed*dt);float horizon=h*0.54f+pitch*4.4f;
            drawAtmosphere(c,p,w,h,horizon,now,roll);c.save();c.rotate(-roll*0.30f,w/2f,h/2f);drawTerrain(c,w,h,horizon,now);c.restore();drawJet(c,w,h,now);drawHud(c,w,h,live);if(!live)drawLinkLost(c,w,h);postInvalidateDelayed(16);
        }
        private void drawTerrain(Canvas c,int w,int h,float horizon,long now){
            LinearGradient g=new LinearGradient(0,horizon,0,h,new int[]{Color.rgb(91,104,82),Color.rgb(44,64,45),Color.rgb(20,30,24)},null,Shader.TileMode.CLAMP);p.setShader(g);c.drawRect(-w,horizon,w*2,h*2,p);p.setShader(null);float shift=(now-t0)*0.045f*(0.4f+throttle);
            p.setColor(Color.rgb(42,59,44));Path ridge=new Path();ridge.moveTo(-w,horizon+70);for(int x=-w;x<=w*2;x+=70)ridge.lineTo(x,horizon+50+(float)Math.sin((x+shift)/120.0)*26+(float)Math.sin((x-shift)/47.0)*9);ridge.lineTo(w*2,h*2);ridge.lineTo(-w,h*2);ridge.close();c.drawPath(ridge,p);
            p.setColor(0x38D7D2BC);p.setStrokeWidth(2);for(int i=1;i<9;i++){float y=horizon+(h-horizon)*(i*i)/81f;c.drawLine(-w,y,w*2,y,p);}for(int i=-12;i<24;i++){float x=w/2f+(i*90f-(shift%90));c.drawLine(x,horizon,x+(x-w/2f)*2.1f,h,p);}
        }
        private void drawJet(Canvas c,int w,int h,long now){
            float cx=w/2f,cy=h*0.61f+pitch*1.0f;float scale=Math.min(w,h)/610f;c.save();c.translate(cx,cy);c.rotate(roll*0.94f);c.scale(scale,scale);
            p.setShadowLayer(26,0,12,0xAA000000);Path shadow=new Path();shadow.moveTo(0,-150);shadow.lineTo(26,-86);shadow.lineTo(52,-58);shadow.lineTo(140,2);shadow.lineTo(82,24);shadow.lineTo(58,48);shadow.lineTo(38,118);shadow.lineTo(18,102);shadow.lineTo(0,144);shadow.lineTo(-18,102);shadow.lineTo(-38,118);shadow.lineTo(-58,48);shadow.lineTo(-82,24);shadow.lineTo(-140,2);shadow.lineTo(-52,-58);shadow.lineTo(-26,-86);shadow.close();p.setColor(Color.rgb(45,50,54));c.drawPath(shadow,p);p.clearShadowLayer();
            LinearGradient metal=new LinearGradient(-120,-70,120,80,new int[]{Color.rgb(103,112,119),Color.rgb(62,70,77),Color.rgb(35,43,49)},null,Shader.TileMode.CLAMP);p.setShader(metal);Path body=new Path();body.moveTo(0,-154);body.lineTo(28,-88);body.lineTo(55,-58);body.lineTo(142,2);body.lineTo(84,24);body.lineTo(60,47);body.lineTo(40,121);body.lineTo(18,101);body.lineTo(0,147);body.lineTo(-18,101);body.lineTo(-40,121);body.lineTo(-60,47);body.lineTo(-84,24);body.lineTo(-142,2);body.lineTo(-55,-58);body.lineTo(-28,-88);body.close();c.drawPath(body,p);p.setShader(null);
            p.setColor(Color.rgb(28,37,43));Path canopy=new Path();canopy.moveTo(0,-113);canopy.lineTo(18,-76);canopy.lineTo(15,-42);canopy.lineTo(0,-26);canopy.lineTo(-15,-42);canopy.lineTo(-18,-76);canopy.close();c.drawPath(canopy,p);p.setShader(new LinearGradient(-12,-103,14,-36,0xCC5C8294,0x33203B4B,Shader.TileMode.CLAMP));c.drawPath(canopy,p);p.setShader(null);
            p.setColor(Color.rgb(37,43,47));Path leftTail=new Path();leftTail.moveTo(-42,52);leftTail.lineTo(-70,105);leftTail.lineTo(-45,91);leftTail.lineTo(-24,53);leftTail.close();c.drawPath(leftTail,p);Path rightTail=new Path();rightTail.moveTo(42,52);rightTail.lineTo(70,105);rightTail.lineTo(45,91);rightTail.lineTo(24,53);rightTail.close();c.drawPath(rightTail,p);
            p.setColor(Color.rgb(24,27,28));c.drawOval(-35,94,-8,127,p);c.drawOval(8,94,35,127,p);float flame=16+throttle*44+(float)Math.sin(now/48.0)*4;p.setShader(new LinearGradient(0,118,0,118+flame,new int[]{0xFFFFF1A0,0xFFFF9A28,0x55FF3A00},null,Shader.TileMode.CLAMP));c.drawOval(-31,114,-11,114+flame,p);c.drawOval(11,114,31,114+flame,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0x887E8C92);c.drawPath(body,p);p.setStyle(Paint.Style.FILL);c.restore();
        }
        private void drawHud(Canvas c,int w,int h,boolean live){
            int green=Color.rgb(105,255,145);p.setColor(green);p.setStrokeWidth(3);p.setStyle(Paint.Style.STROKE);c.drawCircle(w/2f,h/2f,58,p);c.drawLine(w/2f-118,h/2f,w/2f-28,h/2f,p);c.drawLine(w/2f+28,h/2f,w/2f+118,h/2f,p);p.setStyle(Paint.Style.FILL);p.setTextSize(20);c.drawText(String.format(Locale.US,"SPD %4.0f kt",simSpeed),20,42,p);c.drawText(String.format(Locale.US,"ALT %5.0f ft",altitude),20,70,p);c.drawText(String.format(Locale.US,"V/S %+5.0f",verticalSpeed),20,98,p);p.setTextAlign(Paint.Align.CENTER);c.drawText(String.format(Locale.US,"HDG %03.0f°",(yaw+360)%360),w/2f,32,p);p.setTextAlign(Paint.Align.RIGHT);c.drawText(String.format(Locale.US,"ROLL %+05.1f°",roll),w-20,42,p);c.drawText(String.format(Locale.US,"PITCH %+05.1f°",pitch),w-20,70,p);c.drawText(String.format(Locale.US,"THR %3.0f%%",throttle*100),w-20,98,p);c.drawText(String.format(Locale.US,"LINK %.0f Hz  DROP %d",linkHz,droppedPackets),w-20,h-26,p);p.setTextAlign(Paint.Align.LEFT);if(live){p.setColor(Color.rgb(80,255,120));p.setTextSize(14);c.drawText("IMU DATA LINK • LIVE",20,h-26,p);}drawCompassTape(c,w);
        }
        private void drawCompassTape(Canvas c,int w){p.setTextSize(12);p.setColor(Color.rgb(105,255,145));float hdg=(yaw+360)%360;for(int d=-40;d<=40;d+=10){float x=w/2f+d*5.0f;float val=(hdg+d+360)%360;c.drawLine(x,48,x,59,p);c.drawText(String.format(Locale.US,"%03.0f",val),x-10,74,p);}}
        private void drawLinkLost(Canvas c,int w,int h){p.setColor(0xB0000000);c.drawRect(0,0,w,h,p);p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.rgb(255,190,70));p.setTextSize(32);c.drawText("IMU DATA LINK BEKLENİYOR",w/2f,h/2f-8,p);p.setTextSize(16);c.drawText("Pilot telefonunda DATA LINK → bu telefonu seç",w/2f,h/2f+28,p);p.setTextAlign(Paint.Align.LEFT);}
    }
}
