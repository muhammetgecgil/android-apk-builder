package com.mg.tennistv;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
    private Tennis3DView game;
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

        FrameLayout root = new FrameLayout(this);
        game = new Tennis3DView(this);
        root.addView(game, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.HORIZONTAL);
        menu.setPadding(dp(10), dp(8), dp(10), dp(8));
        menu.setBackgroundColor(Color.argb(150, 8, 12, 18));
        menu.addView(button("AI", v -> startAi()));
        menu.addView(button("2 TEL HOST", v -> startHost()));
        menu.addView(button("KONTROLCU", v -> startController()));
        menu.addView(button("KALIBRE", v -> calibrate()));
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(-2, dp(56), Gravity.TOP | Gravity.LEFT);
        mp.setMargins(dp(8), dp(6), 0, 0);
        root.addView(menu, mp);

        TextView badge = new TextView(this);
        badge.setText("MG TENNIS 3D PRO");
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(15);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundColor(Color.argb(135, 0, 0, 0));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(170), dp(38), Gravity.TOP | Gravity.RIGHT);
        bp.setMargins(0, dp(8), dp(10), 0);
        root.addView(badge, bp);

        setContentView(root);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (accel == null) accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        bt = BluetoothAdapter.getDefaultAdapter();
        askBluetoothPermission();
    }

    private Button button(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(11);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.argb(190, 28, 34, 42));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(112), -1);
        p.setMargins(0, 0, dp(5), 0);
        b.setLayoutParams(p);
        return b;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    private void askBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 7);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        game.onResume();
        if (accel != null) sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        if (gyro != null) sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override protected void onPause() {
        sensorManager.unregisterListener(this);
        game.onPause();
        super.onPause();
    }

    @Override public void onAccuracyChanged(Sensor s, int a) {}

    @Override public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroMag = (float)Math.sqrt(e.values[0]*e.values[0] + e.values[1]*e.values[1] + e.values[2]*e.values[2]);
            return;
        }
        if (e.values.length < 3) return;
        tilt = clamp(tilt * 0.86f + (-e.values[0]) * 0.14f, -6f, 6f);
        game.setPlayerTilt(tilt);
        float a = (float)Math.sqrt(e.values[0]*e.values[0] + e.values[1]*e.values[1] + e.values[2]*e.values[2]);
        long now = System.currentTimeMillis();
        float threshold = e.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION ? 8.0f : 15.0f;
        if (a > threshold && gyroMag > 1.5f && now - lastSwing > 300) {
            lastSwing = now;
            float power = clamp((a/threshold)*0.56f + (gyroMag/4f)*0.44f, 0.72f, 2.35f);
            float direction = clamp(e.values[0] / Math.max(5f, a), -1f, 1f);
            if (controllerMode) sendSwing(power, direction);
            else game.localSwing(power, direction);
        }
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    public void calibrate() {
        tilt = 0f;
        game.setPlayerTilt(0f);
        Toast.makeText(this, "Raket merkezi kalibre edildi", Toast.LENGTH_SHORT).show();
    }

    public void startAi() {
        controllerMode = false;
        closeBt();
        game.setMode(Tennis3DView.MODE_AI);
        Toast.makeText(this, "3D maç: Telefon vs AI", Toast.LENGTH_SHORT).show();
    }

    public void startHost() {
        controllerMode = false;
        game.setMode(Tennis3DView.MODE_HOST);
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
        game.setMode(Tennis3DView.MODE_CONTROLLER);
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
}
