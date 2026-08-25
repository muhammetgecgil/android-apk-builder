package com.mg.bionavaviation;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private final FusionEngine fusion = new FusionEngine();
    private BioNavView navView;
    private TextView warning;
    private boolean running = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 16, 20, 16);
        root.setBackgroundColor(0xFF08111A);

        TextView title = new TextView(this);
        title.setText("BioNav Aviation\nGNSS-Denied Navigation Lab");
        title.setTextColor(0xFFF5F7FA);
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        warning = new TextView(this);
        warning.setText("RESEARCH / DEMO ONLY — NOT FOR FLIGHT GUIDANCE OR SAFETY-CRITICAL NAVIGATION");
        warning.setTextColor(0xFFFFC857);
        warning.setTextSize(12);
        warning.setGravity(Gravity.CENTER);
        warning.setPadding(0, 12, 0, 12);
        root.addView(warning);

        navView = new BioNavView(this, fusion);
        root.addView(navView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        Button reset = new Button(this);
        reset.setText("ZERO / ORIGIN");
        reset.setOnClickListener(v -> fusion.reset());
        controls.addView(reset);

        Button startStop = new Button(this);
        startStop.setText("PAUSE");
        startStop.setOnClickListener(v -> {
            running = !running;
            startStop.setText(running ? "PAUSE" : "RESUME");
        });
        controls.addView(startStop);
        root.addView(controls);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        register(Sensor.TYPE_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_GAME);
        register(Sensor.TYPE_LINEAR_ACCELERATION, SensorManager.SENSOR_DELAY_GAME);
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
            case Sensor.TYPE_PRESSURE:
                fusion.updatePressure(event.values[0]);
                break;
        }
        navView.invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Accuracy is reflected indirectly by source availability in this V1 demo.
    }
}
