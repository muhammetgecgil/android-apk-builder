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

public class MainActivity extends Activity implements SensorEventListener {
    private static final UUID SIM_UUID = UUID.fromString("6d9b6c72-4d47-4d8e-9b58-b5e7465b4a22");
    private static final int REQ_BT = 44;

    private BluetoothAdapter bt;
    private BluetoothSocket socket;
    private BluetoothServerSocket serverSocket;
    private BufferedWriter writer;
    private final ExecutorService io = Executors.newCachedThreadPool();
    private SensorManager sensors;
    private Sensor rotationSensor;
    private FlightView flightView;
    private PilotView pilotView;

    private volatile boolean pilotMode = false;
    private volatile boolean receiverMode = false;
    private volatile boolean connected = false;
    private volatile float roll = 0, pitch = 0, yaw = 0;
    private volatile float zeroRoll = 0, zeroPitch = 0, zeroYaw = 0;
    private volatile float throttle = 0.62f;
    private volatile long lastPacketMs = 0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        bt = BluetoothAdapter.getDefaultAdapter();
        sensors = (SensorManager)getSystemService(SENSOR_SERVICE);
        rotationSensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        requestBtIfNeeded();
        showRoleScreen();
    }

    private void requestBtIfNeeded() {
        if (Build.VERSION.SDK_INT >= 31 && (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, REQ_BT);
        }
    }

    private TextView title(String s, int size) {
        TextView t = new TextView(this); t.setText(s); t.setTextColor(Color.rgb(175,255,205)); t.setTextSize(size); t.setGravity(Gravity.CENTER); t.setPadding(16,16,16,16); return t;
    }

    private Button btn(String s) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(18); b.setPadding(18,16,18,16); return b;
    }

    private void showRoleScreen() {
        closeConnections();
        pilotMode = receiverMode = false;
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(40,40,40,40); root.setBackgroundColor(Color.rgb(4,10,14));
        TextView h = title("FIXTURE COCKPIT SIM", 31); root.addView(h, new LinearLayout.LayoutParams(-1,-2));
        TextView sub = title("Aynı APK • 2 telefon • Bluetooth IMU uçuş simülasyonu", 16); sub.setTextColor(Color.LTGRAY); root.addView(sub);
        Button pilot = btn("PİLOT / KOKPİT TELEFONU"); Button screen = btn("UÇAK EKRANI / F-22 GÖRSELİ");
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(420), dp(76)); p.setMargins(0,18,0,0); root.addView(pilot,p); root.addView(screen,p);
        TextView hint = title("Önce iki telefonu Android Bluetooth ayarlarından eşleştir. Uçak ekranını aç, sonra pilot telefonundan bağlan.", 14); hint.setTextColor(Color.GRAY); root.addView(hint);
        pilot.setOnClickListener(v -> startPilot()); screen.setOnClickListener(v -> startReceiver()); setContentView(root);
    }

    private void startPilot() {
        pilotMode = true; receiverMode = false;
        FrameLayout root = new FrameLayout(this); pilotView = new PilotView(this); root.addView(pilotView, new FrameLayout.LayoutParams(-1,-1));
        LinearLayout controls = new LinearLayout(this); controls.setOrientation(LinearLayout.HORIZONTAL); controls.setGravity(Gravity.CENTER); controls.setPadding(12,8,12,8); controls.setBackgroundColor(0x66000000);
        Button connect = btn("Bluetooth Bağlan"); Button center = btn("Merkezle"); Button minus = btn("Gaz -"); Button plus = btn("Gaz +"); Button back = btn("Mod Seçimi");
        controls.addView(connect); controls.addView(center); controls.addView(minus); controls.addView(plus); controls.addView(back);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, dp(64), Gravity.BOTTOM); root.addView(controls,cp); setContentView(root);
        if(rotationSensor!=null) sensors.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        connect.setOnClickListener(v -> chooseBondedAndConnect());
        center.setOnClickListener(v -> { zeroRoll += roll; zeroPitch += pitch; zeroYaw += yaw; roll=pitch=yaw=0; });
        minus.setOnClickListener(v -> throttle=Math.max(0, throttle-0.08f)); plus.setOnClickListener(v -> throttle=Math.min(1, throttle+0.08f)); back.setOnClickListener(v -> showRoleScreen());
    }

    private void startReceiver() {
        receiverMode = true; pilotMode = false;
        FrameLayout root = new FrameLayout(this); flightView = new FlightView(this); root.addView(flightView,new FrameLayout.LayoutParams(-1,-1));
        Button back = btn("Mod Seçimi"); FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(150),dp(54),Gravity.TOP|Gravity.RIGHT); bp.setMargins(0,12,12,0); root.addView(back,bp); back.setOnClickListener(v -> showRoleScreen()); setContentView(root);
        startServer();
    }

    private boolean btAllowed(){ return Build.VERSION.SDK_INT < 31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED; }

    private void chooseBondedAndConnect() {
        if(bt==null){ toast("Bu telefonda Bluetooth yok."); return; }
        if(!btAllowed()){ requestBtIfNeeded(); return; }
        if(!bt.isEnabled()){ startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)); toast("Bluetooth'u açıp tekrar Bağlan'a bas."); return; }
        Set<BluetoothDevice> set = bt.getBondedDevices();
        if(set==null || set.isEmpty()){ toast("Eşleştirilmiş telefon bulunamadı. Önce Android Bluetooth ayarlarından iki telefonu eşleştir."); return; }
        ArrayList<BluetoothDevice> devices=new ArrayList<>(set); String[] names=new String[devices.size()];
        for(int i=0;i<devices.size();i++){ BluetoothDevice d=devices.get(i); names[i]=(d.getName()==null?"Cihaz":d.getName())+"\n"+d.getAddress(); }
        new AlertDialog.Builder(this).setTitle("Uçak ekranı telefonunu seç").setItems(names,(d,w)->connectTo(devices.get(w))).show();
    }

    private void connectTo(BluetoothDevice device) {
        toast("Bağlanıyor: "+device.getName());
        io.execute(() -> {
            try {
                closeSocketOnly();
                BluetoothSocket s=device.createRfcommSocketToServiceRecord(SIM_UUID); s.connect(); socket=s; writer=new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8)); connected=true;
                runOnUiThread(()->toast("Bluetooth bağlı"));
            } catch(Exception e){ connected=false; runOnUiThread(()->toast("Bağlantı kurulamadı: "+e.getClass().getSimpleName())); }
        });
    }

    private void startServer() {
        if(bt==null){ toast("Bu telefonda Bluetooth yok."); return; }
        if(!btAllowed()){ requestBtIfNeeded(); return; }
        if(!bt.isEnabled()){ startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)); }
        io.execute(() -> {
            while(receiverMode){
                try {
                    serverSocket=bt.listenUsingRfcommWithServiceRecord("FixtureCockpitSim", SIM_UUID);
                    runOnUiThread(()->toast("Uçak ekranı hazır — pilot bağlantısı bekleniyor"));
                    BluetoothSocket s=serverSocket.accept(); socket=s; connected=true; lastPacketMs=System.currentTimeMillis();
                    readPackets(s.getInputStream());
                } catch(Exception e){ connected=false; }
                finally { try{ if(serverSocket!=null)serverSocket.close(); }catch(Exception ignored){} }
            }
        });
    }

    private void readPackets(InputStream in) throws IOException {
        BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8)); String line;
        while(receiverMode && (line=r.readLine())!=null){
            String[] a=line.split(","); if(a.length<4)continue;
            try{
                float nr=Float.parseFloat(a[0]), np=Float.parseFloat(a[1]), ny=Float.parseFloat(a[2]), nt=Float.parseFloat(a[3]);
                roll=lerp(roll,nr,0.32f); pitch=lerp(pitch,np,0.32f); yaw=lerpAngle(yaw,ny,0.24f); throttle=lerp(throttle,nt,0.22f); lastPacketMs=System.currentTimeMillis();
                if(flightView!=null) flightView.postInvalidateOnAnimation();
            }catch(Exception ignored){}
        }
        connected=false;
    }

    @Override public void onSensorChanged(SensorEvent e) {
        if(!pilotMode || e.sensor.getType()!=Sensor.TYPE_ROTATION_VECTOR)return;
        float[] rm=new float[9], ori=new float[3]; SensorManager.getRotationMatrixFromVector(rm,e.values); SensorManager.getOrientation(rm,ori);
        float rawYaw=(float)Math.toDegrees(ori[0]); float rawPitch=(float)Math.toDegrees(ori[1]); float rawRoll=(float)Math.toDegrees(ori[2]);
        float targetRoll=clamp(rawRoll-zeroRoll,-85,85); float targetPitch=clamp(-(rawPitch-zeroPitch),-50,50); float targetYaw=wrap(rawYaw-zeroYaw);
        roll=lerp(roll,targetRoll,0.25f); pitch=lerp(pitch,targetPitch,0.25f); yaw=lerpAngle(yaw,targetYaw,0.18f);
        if(pilotView!=null) pilotView.postInvalidateOnAnimation();
        sendPacket();
    }
    @Override public void onAccuracyChanged(Sensor s,int a){}

    private void sendPacket(){
        BufferedWriter w=writer; if(!connected||w==null)return; final String msg=String.format(Locale.US,"%.2f,%.2f,%.2f,%.3f\n",roll,pitch,yaw,throttle);
        io.execute(()->{ try{ synchronized(MainActivity.this){ if(writer!=null){ writer.write(msg); writer.flush(); } } }catch(Exception e){connected=false;} });
    }

    private void closeSocketOnly(){ try{if(socket!=null)socket.close();}catch(Exception ignored){} socket=null; writer=null; connected=false; }
    private void closeConnections(){ sensors.unregisterListener(this); closeSocketOnly(); try{if(serverSocket!=null)serverSocket.close();}catch(Exception ignored){} serverSocket=null; }
    @Override protected void onDestroy(){ closeConnections(); io.shutdownNow(); super.onDestroy(); }
    @Override public void onBackPressed(){ showRoleScreen(); }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);} private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
    private static float wrap(float a){while(a>180)a-=360; while(a<-180)a+=360; return a;}
    private static float lerpAngle(float a,float b,float t){return wrap(a+wrap(b-a)*t);}

    class PilotView extends View {
        Paint p=new Paint(3); public PilotView(Context c){super(c); p.setTypeface(Typeface.create("monospace",Typeface.BOLD));}
        protected void onDraw(Canvas c){ super.onDraw(c); int w=getWidth(),h=getHeight();
            c.drawColor(Color.rgb(4,12,17)); float cy=h/2f + pitch*5.5f;
            p.setColor(Color.rgb(18,68,92)); c.save(); c.rotate(-roll,w/2f,h/2f); c.drawRect(-w,cy-1000,w*2,cy,p); p.setColor(Color.rgb(74,50,27)); c.drawRect(-w,cy,w*2,cy+1000,p); p.setStrokeWidth(5); p.setColor(Color.WHITE); c.drawLine(-w,cy,w*2,cy,p); c.restore();
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4); p.setColor(Color.rgb(120,255,170)); c.drawCircle(w/2f,h/2f,110,p); c.drawLine(w/2f-150,h/2f,w/2f-36,h/2f,p); c.drawLine(w/2f+36,h/2f,w/2f+150,h/2f,p); c.drawLine(w/2f,h/2f-22,w/2f,h/2f+42,p); p.setStyle(Paint.Style.FILL);
            p.setTextSize(28); c.drawText(String.format(Locale.US,"ROLL %+05.1f°",roll),28,44,p); c.drawText(String.format(Locale.US,"PITCH %+05.1f°",pitch),28,78,p); c.drawText(String.format(Locale.US,"HDG %03.0f°",(yaw+360)%360),28,112,p);
            p.setColor(connected?Color.rgb(95,255,130):Color.rgb(255,90,70)); c.drawText(connected?"LINK ● BAĞLI":"LINK ○ BAĞLI DEĞİL",w-330,44,p);
            p.setColor(Color.rgb(120,255,170)); c.drawText(String.format(Locale.US,"THR %3.0f%%",throttle*100),w-230,82,p);
        }
    }

    class FlightView extends View {
        Paint p=new Paint(3); Path jet=new Path(); long t0=System.currentTimeMillis();
        public FlightView(Context c){ super(c); p.setTypeface(Typeface.create("monospace",Typeface.BOLD)); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }
        protected void onDraw(Canvas c){ super.onDraw(c); int w=getWidth(),h=getHeight(); long now=System.currentTimeMillis(); boolean live=connected && now-lastPacketMs<1500;
            float speed=220+throttle*980; float altitude=4200 + pitch*42 + (float)Math.sin((now-t0)/1800.0)*12;
            LinearGradient sky=new LinearGradient(0,0,0,h,Color.rgb(7,25,44),Color.rgb(40,92,120),Shader.TileMode.CLAMP); p.setShader(sky); c.drawRect(0,0,w,h,p); p.setShader(null);
            drawStars(c,w,h,now);
            float horizon=h*0.55f + pitch*3.2f; c.save(); c.rotate(-roll*0.35f,w/2f,h/2f); p.setColor(Color.rgb(26,40,37)); c.drawRect(-w,horizon,w*2,h*2,p); p.setColor(Color.rgb(80,115,98)); p.setStrokeWidth(3); c.drawLine(-w,horizon,w*2,horizon,p); c.restore();
            drawJet(c,w,h,roll,pitch,yaw,now);
            drawHud(c,w,h,speed,altitude,live,now);
            if(!live){p.setColor(0xAA000000);c.drawRect(0,0,w,h,p);p.setColor(Color.rgb(255,185,70));p.setTextSize(34);p.setTextAlign(Paint.Align.CENTER);c.drawText("BLUETOOTH IMU BAĞLANTISI BEKLENİYOR",w/2f,h/2f,p);p.setTextSize(18);c.drawText("Pilot telefonunda Bluetooth Bağlan → bu telefonu seç",w/2f,h/2f+42,p);p.setTextAlign(Paint.Align.LEFT);}
            postInvalidateDelayed(33);
        }
        private void drawStars(Canvas c,int w,int h,long now){p.setColor(0x55FFFFFF); for(int i=0;i<34;i++){float x=(i*173)%w; float y=(i*71+(now/70f)*(0.25f+throttle))%(h*0.48f); c.drawCircle(x,y,1.2f+(i%3)*.45f,p);}}
        private void drawJet(Canvas c,int w,int h,float r,float pit,float y,long now){
            c.save(); float cx=w/2f, cy=h*0.56f + pit*1.8f; c.translate(cx,cy); c.rotate(-r*0.9f); float bankScale=1f-0.22f*Math.abs(r)/85f; c.scale(1.05f,bankScale);
            jet.reset(); jet.moveTo(0,-150); jet.lineTo(24,-72); jet.lineTo(112,-30); jet.lineTo(78,0); jet.lineTo(155,52); jet.lineTo(72,40); jet.lineTo(50,92); jet.lineTo(20,74); jet.lineTo(0,120); jet.lineTo(-20,74); jet.lineTo(-50,92); jet.lineTo(-72,40); jet.lineTo(-155,52); jet.lineTo(-78,0); jet.lineTo(-112,-30); jet.lineTo(-24,-72); jet.close();
            p.setShadowLayer(20,0,8,0x99000000); p.setColor(Color.rgb(92,103,108)); c.drawPath(jet,p); p.clearShadowLayer(); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(Color.rgb(175,188,188)); c.drawPath(jet,p); p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(255,112,35)); float flame=22+throttle*36+(float)Math.sin(now/42.0)*5; c.drawOval(-38,72,-16,72+flame,p); c.drawOval(16,72,38,72+flame,p); p.setColor(Color.rgb(255,218,110)); c.drawOval(-32,72,-21,76+flame*.65f,p); c.drawOval(21,72,32,76+flame*.65f,p);
            p.setColor(Color.rgb(45,58,62)); c.drawRect(-11,-104,11,-12,p); c.restore();
        }
        private void drawHud(Canvas c,int w,int h,float speed,float alt,boolean live,long now){
            int g=Color.rgb(117,255,163); p.setColor(g); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); c.drawRect(18,18,w-18,h-18,p); c.drawCircle(w/2f,h/2f,38,p); c.drawLine(w/2f-70,h/2f,w/2f-18,h/2f,p); c.drawLine(w/2f+18,h/2f,w/2f+70,h/2f,p); p.setStyle(Paint.Style.FILL);
            p.setTextSize(26); c.drawText(String.format(Locale.US,"SPD %4.0f kt",speed),40,58,p); c.drawText(String.format(Locale.US,"ALT %6.0f ft",alt),w-250,58,p); c.drawText(String.format(Locale.US,"HDG %03.0f",(yaw+360)%360),w/2f-72,52,p);
            p.setTextSize(18); c.drawText(String.format(Locale.US,"ROLL %+05.1f",roll),40,88,p); c.drawText(String.format(Locale.US,"PITCH %+05.1f",pitch),40,112,p); c.drawText(String.format(Locale.US,"THR %3.0f%%",throttle*100),w-200,88,p);
            p.setColor(live?g:Color.rgb(255,180,60)); c.drawText(live?"IMU LINK: LIVE":"IMU LINK: LOST",w-200,112,p);
            p.setColor(g); p.setStyle(Paint.Style.STROKE); for(int i=-3;i<=3;i++){float yy=h/2f+i*42+pitch*3; float len=i==0?92:55; c.drawLine(w/2f-len,yy,w/2f-20,yy,p); c.drawLine(w/2f+20,yy,w/2f+len,yy,p);} p.setStyle(Paint.Style.FILL);
            float stickX=w/2f + clamp(roll/85f,-1,1)*160; float stickY=h/2f - clamp(pitch/50f,-1,1)*110; p.setStyle(Paint.Style.STROKE); c.drawCircle(stickX,stickY,15,p); p.setStyle(Paint.Style.FILL);
        }
    }
}
