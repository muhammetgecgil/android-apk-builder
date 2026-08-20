package com.mg.tennistv;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.*;
import android.bluetooth.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accel, gyro;
    private float gyroMag = 0f, tilt = 0f;
    private long lastSwing = 0L;
    private GameView game;
    private BluetoothAdapter bt;
    private BluetoothSocket btSocket;
    private BluetoothServerSocket serverSocket;
    private PrintWriter btOut;
    private volatile boolean controllerMode = false;
    private static final UUID UUID_GAME = UUID.fromString("6f4f4d30-9c4e-4f99-9e96-1e2ac4c1a501");

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        game = new GameView(this);
        setContentView(game);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (accel == null) accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        bt = BluetoothAdapter.getDefaultAdapter();
        askBluetoothPermission();
    }

    private void askBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 7);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (accel != null) sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        if (gyro != null) sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override public void onAccuracyChanged(Sensor s, int a) {}

    @Override public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroMag = (float)Math.sqrt(e.values[0]*e.values[0] + e.values[1]*e.values[1] + e.values[2]*e.values[2]);
            return;
        }
        if (e.values.length < 3) return;
        tilt = clamp(tilt * 0.85f + (-e.values[0]) * 0.15f, -6f, 6f);
        game.playerTilt = tilt;
        float a = (float)Math.sqrt(e.values[0]*e.values[0] + e.values[1]*e.values[1] + e.values[2]*e.values[2]);
        long now = System.currentTimeMillis();
        float threshold = e.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION ? 8.0f : 15.0f;
        if (a > threshold && gyroMag > 1.5f && now - lastSwing > 320) {
            lastSwing = now;
            float power = clamp((a/threshold)*0.55f + (gyroMag/4f)*0.45f, 0.7f, 2.3f);
            float direction = clamp(e.values[0] / Math.max(5f, a), -1f, 1f);
            if (controllerMode) sendSwing(power, direction);
            else game.localSwing(power, direction);
        }
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    public void calibrate() {
        game.playerTilt = 0f;
        tilt = 0f;
        Toast.makeText(this, "Raket merkezi kalibre edildi", Toast.LENGTH_SHORT).show();
    }

    public void startAi() {
        controllerMode = false;
        closeBt();
        game.setMode(GameView.MODE_AI);
        Toast.makeText(this, "Telefon vs AI", Toast.LENGTH_SHORT).show();
    }

    public void startHost() {
        controllerMode = false;
        game.setMode(GameView.MODE_HOST);
        if (bt == null) { Toast.makeText(this, "Bluetooth desteklenmiyor", Toast.LENGTH_LONG).show(); return; }
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { askBluetoothPermission(); return; }
        new Thread(() -> {
            try {
                serverSocket = bt.listenUsingRfcommWithServiceRecord("MG Tennis TV", UUID_GAME);
                runOnUiThread(() -> Toast.makeText(this, "Rakip telefon bekleniyor...", Toast.LENGTH_LONG).show());
                btSocket = serverSocket.accept();
                runOnUiThread(() -> Toast.makeText(this, "Rakip bağlandı", Toast.LENGTH_SHORT).show());
                readRemote(btSocket);
            } catch (Exception ex) {
                runOnUiThread(() -> Toast.makeText(this, "Host bağlantısı: " + ex.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    public void startController() {
        controllerMode = true;
        game.setMode(GameView.MODE_CONTROLLER);
        if (bt == null) { Toast.makeText(this, "Bluetooth desteklenmiyor", Toast.LENGTH_LONG).show(); return; }
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { askBluetoothPermission(); return; }
        Set<BluetoothDevice> bonded = bt.getBondedDevices();
        if (bonded == null || bonded.isEmpty()) {
            Toast.makeText(this, "Önce iki telefonu Android Bluetooth ayarından eşleştir", Toast.LENGTH_LONG).show();
            return;
        }
        final ArrayList<BluetoothDevice> devices = new ArrayList<>(bonded);
        String[] names = new String[devices.size()];
        for (int i=0;i<devices.size();i++) names[i] = devices.get(i).getName() + "\n" + devices.get(i).getAddress();
        new AlertDialog.Builder(this).setTitle("Ana telefonu seç").setItems(names, (d, which) -> connectController(devices.get(which))).show();
    }

    private void connectController(BluetoothDevice dev) {
        new Thread(() -> {
            try {
                if (bt.isDiscovering()) bt.cancelDiscovery();
                btSocket = dev.createRfcommSocketToServiceRecord(UUID_GAME);
                btSocket.connect();
                btOut = new PrintWriter(new OutputStreamWriter(btSocket.getOutputStream()), true);
                runOnUiThread(() -> Toast.makeText(this, "Kontrolcü bağlandı", Toast.LENGTH_SHORT).show());
            } catch (Exception ex) {
                runOnUiThread(() -> Toast.makeText(this, "Bağlantı olmadı: " + ex.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void sendSwing(float p, float d) {
        PrintWriter out = btOut;
        if (out != null) out.println("SWING," + p + "," + d);
        game.flashSwing(p);
    }

    private void readRemote(BluetoothSocket s) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("SWING,")) {
                String[] q = line.split(",");
                if (q.length >= 3) {
                    final float p = Float.parseFloat(q[1]);
                    final float d = Float.parseFloat(q[2]);
                    runOnUiThread(() -> game.remoteSwing(p, d));
                }
            }
        }
    }

    private void closeBt() {
        try { if (btSocket != null) btSocket.close(); } catch(Exception ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch(Exception ignored) {}
        btSocket = null; serverSocket = null; btOut = null;
    }

    @Override protected void onDestroy() { closeBt(); super.onDestroy(); }

    public static class GameView extends View {
        static final int MODE_AI=0, MODE_HOST=1, MODE_CONTROLLER=2;
        final MainActivity a;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rnd = new Random();
        int mode = MODE_AI;
        float bx=.5f, by=.62f, vx=.17f, vy=-.26f;
        float playerX=.5f, oppX=.5f, playerTilt=0f;
        int me=0, them=0;
        long last=System.nanoTime(), swingFlash=0;
        float pendingPower=0, pendingDir=0, remotePower=0, remoteDir=0;
        RectF aiBtn = new RectF(), hostBtn = new RectF(), ctrlBtn = new RectF(), calBtn = new RectF();

        GameView(MainActivity a) { super(a); this.a=a; p.setTypeface(Typeface.create("sans", Typeface.BOLD)); setBackgroundColor(Color.rgb(9,55,45)); }
        void setMode(int m) { mode=m; resetBall(true); invalidate(); }
        void localSwing(float power, float dir) { pendingPower=power; pendingDir=dir; swingFlash=System.currentTimeMillis(); }
        void remoteSwing(float power, float dir) { remotePower=power; remoteDir=dir; swingFlash=System.currentTimeMillis(); }
        void flashSwing(float power) { pendingPower=power; swingFlash=System.currentTimeMillis(); invalidate(); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            long now=System.nanoTime(); float dt=Math.min(.032f,(now-last)/1_000_000_000f); last=now;
            if (mode != MODE_CONTROLLER) update(dt);
            drawCourt(c);
            if (mode == MODE_CONTROLLER) drawController(c); else drawGame(c);
            postInvalidateOnAnimation();
        }

        void update(float dt) {
            playerX = clamp(.5f + playerTilt/10f, .12f, .88f);
            if (mode==MODE_AI) oppX += (bx-oppX)*Math.min(1f,dt*3.8f);
            bx += vx*dt; by += vy*dt;
            if (bx<.06f) { bx=.06f; vx=Math.abs(vx); }
            if (bx>.94f) { bx=.94f; vx=-Math.abs(vx); }

            if (by > .82f && vy>0) {
                if (Math.abs(bx-playerX) < .23f && pendingPower>0) {
                    vy = -.28f * pendingPower; vx += pendingDir*.18f; pendingPower=0;
                } else if (by>1.03f) { them++; resetBall(false); }
            }
            if (by < .18f && vy<0) {
                if (mode==MODE_AI) {
                    if (Math.abs(bx-oppX)<.27f) { vy=Math.abs(vy)*.96f; vx += (rnd.nextFloat()-.5f)*.13f; }
                    else if (by<-.05f) { me++; resetBall(true); }
                } else {
                    if (remotePower>0 && Math.abs(bx-oppX)<.34f) {
                        vy=.28f*remotePower; vx += remoteDir*.18f; remotePower=0;
                    } else if (by<-.05f) { me++; resetBall(true); }
                }
            }
            if (Math.abs(vy)<.16f) vy += Math.signum(vy==0?1:vy)*.02f;
        }

        void resetBall(boolean towardOpponent) {
            bx=.5f; by=.55f; vx=(rnd.nextFloat()-.5f)*.18f; vy=towardOpponent?-.27f:.27f;
            pendingPower=remotePower=0;
        }

        void drawCourt(Canvas c) {
            int w=getWidth(), h=getHeight();
            p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(21,111,84)); c.drawRect(0,0,w,h,p);
            float top=h*.12f, bottom=h*.94f, cx=w*.5f;
            Path court=new Path(); court.moveTo(w*.29f,top); court.lineTo(w*.71f,top); court.lineTo(w*.93f,bottom); court.lineTo(w*.07f,bottom); court.close();
            p.setColor(Color.rgb(34,139,92)); c.drawPath(court,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(2,w/500f)); p.setColor(Color.WHITE); c.drawPath(court,p);
            c.drawLine(cx,top,cx,bottom,p); c.drawLine(w*.18f,h*.55f,w*.82f,h*.55f,p);
            c.drawLine(w*.12f,h*.72f,w*.88f,h*.72f,p); c.drawLine(w*.23f,h*.36f,w*.77f,h*.36f,p);
            p.setStyle(Paint.Style.FILL); p.setColor(Color.argb(190,20,20,20)); c.drawRect(0,0,w,h*.10f,p);
            float bw=w*.115f, gap=w*.012f, y=h*.017f;
            aiBtn.set(w*.02f,y,w*.02f+bw,h*.085f); hostBtn.set(aiBtn.right+gap,y,aiBtn.right+gap+bw,h*.085f); ctrlBtn.set(hostBtn.right+gap,y,hostBtn.right+gap+bw,h*.085f); calBtn.set(ctrlBtn.right+gap,y,ctrlBtn.right+gap+bw,h*.085f);
            drawButton(c,aiBtn,"AI"); drawButton(c,hostBtn,"2 TEL HOST"); drawButton(c,ctrlBtn,"KONTROLCU"); drawButton(c,calBtn,"KALIBRE");
        }

        void drawGame(Canvas c) {
            int w=getWidth(), h=getHeight();
            float px=w*(.10f+.80f*playerX), py=h*.88f;
            float ox=w*(.30f+.40f*oppX), oy=h*.19f;
            p.setColor(Color.rgb(245,245,245)); c.drawCircle(px,py,w*.018f,p); c.drawCircle(ox,oy,w*.011f,p);
            float sx=w*(.12f+.76f*bx), sy=h*(.13f+.78f*by);
            p.setColor(Color.rgb(230,255,40)); c.drawCircle(sx,sy,w*.0095f,p);
            p.setTextAlign(Paint.Align.CENTER); p.setTextSize(h*.052f); p.setColor(Color.WHITE); c.drawText(me+"  -  "+them,w*.78f,h*.065f,p);
            p.setTextSize(h*.028f); String m=mode==MODE_AI?"AI RAKIP":"2 TELEFON"; c.drawText(m,w*.91f,h*.06f,p);
            if (System.currentTimeMillis()-swingFlash<180) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(w*.007f); p.setColor(Color.YELLOW); c.drawCircle(px,py,w*.05f,p); p.setStyle(Paint.Style.FILL); }
        }

        void drawController(Canvas c) {
            int w=getWidth(), h=getHeight();
            p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.WHITE); p.setTextSize(h*.075f); c.drawText("BLUETOOTH RAKET KONTROLCUSU",w*.5f,h*.36f,p);
            p.setTextSize(h*.045f); c.drawText("Telefonu raket gibi salla",w*.5f,h*.46f,p);
            c.drawText("Vurus gucu ivme + jiroskop ile hesaplanir",w*.5f,h*.53f,p);
            p.setTextSize(h*.032f); c.drawText("Once Android Bluetooth ayarindan iki telefonu eslestir",w*.5f,h*.63f,p);
            if (System.currentTimeMillis()-swingFlash<180) { p.setColor(Color.YELLOW); p.setTextSize(h*.08f); c.drawText("VURUS!",w*.5f,h*.78f,p); }
        }

        void drawButton(Canvas c, RectF r, String s) {
            p.setColor(Color.argb(210,40,40,40)); c.drawRoundRect(r,12,12,p);
            p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(getHeight()*.025f); c.drawText(s,r.centerX(),r.centerY()+getHeight()*.009f,p);
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            if (e.getAction()!=MotionEvent.ACTION_UP) return true;
            float x=e.getX(), y=e.getY();
            if (aiBtn.contains(x,y)) a.startAi();
            else if (hostBtn.contains(x,y)) a.startHost();
            else if (ctrlBtn.contains(x,y)) a.startController();
            else if (calBtn.contains(x,y)) a.calibrate();
            return true;
        }
    }
}
