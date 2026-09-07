package com.mg.bionavaviation;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private final FusionEngine fusion = new FusionEngine();
    private final FlightRecorder recorder = new FlightRecorder();
    private BioNavView navView;
    private boolean running = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 12, 16, 12);
        root.setBackgroundColor(0xFF071018);

        TextView title = new TextView(this);
        title.setText("BioNav Aviation v2\nGNSS-Denied Flight Navigation Lab");
        title.setTextColor(0xFFF5F7FA);
        title.setTextSize(21);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView warning = new TextView(this);
        warning.setText("RESEARCH / FLIGHT-TEST AID ONLY — NOT CERTIFIED FOR PRIMARY FLIGHT GUIDANCE");
        warning.setTextColor(0xFFFFC857);
        warning.setTextSize(11);
        warning.setGravity(Gravity.CENTER);
        warning.setPadding(0, 8, 0, 8);
        root.addView(warning);

        navView = new BioNavView(this, fusion, recorder);
        root.addView(navView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout row1 = new LinearLayout(this);
        row1.setGravity(Gravity.CENTER);
        Button zero = new Button(this);
        zero.setText("ZERO");
        zero.setOnClickListener(v -> fusion.reset());
        row1.addView(zero);

        Button align = new Button(this);
        align.setText("BARO ZERO");
        align.setOnClickListener(v -> fusion.calibrateBaroOrigin());
        row1.addView(align);

        Button startStop = new Button(this);
        startStop.setText("PAUSE");
        startStop.setOnClickListener(v -> {
            running = !running;
            startStop.setText(running ? "PAUSE" : "RESUME");
        });
        row1.addView(startStop);
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER);
        Button record = new Button(this);
        record.setText("RECORD");
        record.setOnClickListener(v -> {
            recorder.setRecording(!recorder.isRecording());
            record.setText(recorder.isRecording() ? "STOP REC" : "RECORD");
            navView.invalidate();
        });
        row2.addView(record);

        Button clear = new Button(this);
        clear.setText("CLEAR LOG");
        clear.setOnClickListener(v -> {
            recorder.clear();
            navView.invalidate();
        });
        row2.addView(clear);

        Button save = new Button(this);
        save.setText("SAVE CSV");
        save.setOnClickListener(v -> saveCsv());
        row2.addView(save);
        root.addView(row2);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        register(Sensor.TYPE_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_GAME);
        register(Sensor.TYPE_LINEAR_ACCELERATION, SensorManager.SENSOR_DELAY_GAME);
        register(Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_GAME);
        register(Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_GAME);
        register(Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void register(int type, int delay) {
        Sensor s = sensorManager.getDefaultSensor(type);
        if (s != null) sensorManager.registerListener(this, s, delay);
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!running) return;
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                fusion.updateRotationVector(event.values.clone());
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                fusion.updateLinearAcceleration(event.values[0], event.values[1], event.values[2], event.timestamp);
                break;
            case Sensor.TYPE_GYROSCOPE:
                fusion.updateGyroscope(event.values[0], event.values[1], event.values[2], event.timestamp);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                fusion.updateMagneticField(event.values[0], event.values[1], event.values[2]);
                break;
            case Sensor.TYPE_PRESSURE:
                fusion.updatePressure(event.values[0], event.timestamp);
                break;
        }
        recorder.capture(fusion, SystemClock.elapsedRealtime());
        navView.invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        fusion.markSensorAccuracy(sensor.getType(), accuracy);
    }

    private void saveCsv() {
        if (recorder.size() == 0) {
            Toast.makeText(this, "No recorded flight-test samples", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File dir = new File(getExternalFilesDir(null), "flightlogs");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create log folder");
            File out = new File(dir, "bionav_flightlog_" + System.currentTimeMillis() + ".csv");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(recorder.csv().getBytes(StandardCharsets.UTF_8));
            }
            Toast.makeText(this, "Saved: " + out.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
