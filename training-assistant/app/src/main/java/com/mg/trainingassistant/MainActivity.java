package com.mg.trainingassistant;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("training_assistant", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(44, 60, 44, 44);
        root.setBackgroundColor(Color.rgb(16, 20, 24));

        TextView title = text("Eğitim Asistanı", 28, Color.WHITE);
        title.setPadding(0, 0, 0, 16);
        root.addView(title);

        TextView info = text("İçeriği sen izlerken İleri / Devam / Sonraki düğmelerini otomatik bulur. Sınav, quiz, değerlendirme ve onay ekranlarında durur.", 17, Color.rgb(210, 216, 222));
        info.setPadding(0, 0, 0, 28);
        root.addView(info);

        status = text("", 18, Color.WHITE);
        status.setPadding(0, 0, 0, 28);
        root.addView(status);

        Button access = button("1. Erişilebilirlik iznini aç");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(access);

        Button start = button("2. Otomatik ilerlemeyi başlat");
        start.setOnClickListener(v -> {
            prefs.edit().putBoolean("running", true).apply();
            refresh();
        });
        root.addView(start);

        Button stop = button("Otomatik ilerlemeyi durdur");
        stop.setOnClickListener(v -> {
            prefs.edit().putBoolean("running", false).apply();
            refresh();
        });
        root.addView(stop);

        TextView note = text("Not: Bu uygulama eğitim süresini atlatmaz, videoyu izlenmiş gibi göstermez ve sınav/quiz cevaplamaz. Yalnızca görünür gezinme düğmelerine yardımcı olur.", 14, Color.rgb(165, 174, 183));
        note.setPadding(0, 30, 0, 0);
        root.addView(note);

        setContentView(root);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs != null) refresh();
    }

    private void refresh() {
        boolean running = prefs.getBoolean("running", false);
        status.setText(running ? "Durum: AKTİF" : "Durum: DURDURULDU");
        status.setTextColor(running ? Color.rgb(94, 234, 138) : Color.rgb(255, 184, 108));
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.START);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 18);
        b.setLayoutParams(lp);
        return b;
    }
}
