package com.mg.trainingassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String PREFS = "training_assistant";
    private SharedPreferences prefs;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(44, 60, 44, 54);
        root.setBackgroundColor(Color.rgb(16, 20, 24));
        scroll.addView(root);

        TextView title = text("Eğitim Asistanı 1.1", 28, Color.WHITE);
        title.setPadding(0, 0, 0, 12);
        root.addView(title);

        TextView subtitle = text("Takılmaya dayanıklı, kontrollü eğitim gezinme yardımcısı", 16, Color.rgb(160, 205, 255));
        subtitle.setPadding(0, 0, 0, 24);
        root.addView(subtitle);

        TextView info = text(
                "İçeriği sen izlerken görünür İleri / Devam / Sonraki düğmelerini bulur. Düğme ekranın altındaysa kontrollü kaydırıp tekrar arar. Sınav, quiz, değerlendirme, soru/cevap ve onay ekranlarında otomatik ilerlemeyi durdurur.",
                17, Color.rgb(210, 216, 222));
        info.setPadding(0, 0, 0, 28);
        root.addView(info);

        status = text("", 18, Color.WHITE);
        status.setPadding(0, 0, 0, 26);
        root.addView(status);

        Button access = button("1. Erişilebilirlik iznini aç");
        access.setOnClickListener(v -> showDisclosureThenOpenSettings());
        root.addView(access);

        Button start = button("2. Otomatik ilerlemeyi başlat");
        start.setOnClickListener(v -> {
            if (!prefs.getBoolean("consent", false)) {
                showDisclosureThenOpenSettings();
                return;
            }
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

        Button privacy = button("Gizlilik ve kullanım açıklaması");
        privacy.setOnClickListener(v -> showPrivacy());
        root.addView(privacy);

        TextView note = text(
                "Güvenli kullanım: Uygulama eğitim süresini atlatmaz, videoyu izlenmiş gibi göstermez, sınav/quiz cevaplamaz ve sistem ayarlarında otomatik tıklama yapmaz. Erişilebilirlik verisi cihaz üzerinde anlık olarak yalnızca gezinme düğmesini bulmak için kullanılır.",
                14, Color.rgb(165, 174, 183));
        note.setPadding(0, 26, 0, 0);
        root.addView(note);

        setContentView(scroll);
        refresh();
    }

    private void showDisclosureThenOpenSettings() {
        new AlertDialog.Builder(this)
                .setTitle("Erişilebilirlik API açıklaması")
                .setMessage("Eğitim Asistanı, sen etkinleştirdiğinde ekrandaki görünür metin ve düğme bilgilerini erişilebilirlik hizmeti üzerinden okuyarak yalnızca İleri/Devam/Sonraki gibi gezinme kontrollerini bulur ve tıklar. Bu bilgi cihaz dışına gönderilmez, reklam/analiz için kullanılmaz ve saklanmaz. Sistem ayarları, izin ekranları, sınav/quiz/değerlendirme ve onay adımları otomatik tıklanmaz. Devam ederek bu işlev için erişilebilirlik kullanımını açıkça kabul etmiş olursun.")
                .setNegativeButton("İptal", null)
                .setPositiveButton("Kabul ediyorum", (d, w) -> {
                    prefs.edit().putBoolean("consent", true).apply();
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                })
                .show();
    }

    private void showPrivacy() {
        new AlertDialog.Builder(this)
                .setTitle("Gizlilik ve veri kullanımı")
                .setMessage("Erişilebilirlik üzerinden okunan ekran içeriği yalnızca cihaz üzerinde, o anda gezinme düğmesini tespit etmek ve kullanıcı tarafından açılmış otomasyonu yürütmek için işlenir. Uygulama bu içeriği sunucuya göndermez, satmaz, paylaşmaz veya kalıcı olarak kaydetmez. Otomasyon kullanıcı tarafından her zaman durdurulabilir ve erişilebilirlik izni Android Ayarları'ndan kapatılabilir.")
                .setPositiveButton("Tamam", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs != null) refresh();
    }

    private void refresh() {
        boolean running = prefs.getBoolean("running", false);
        boolean consent = prefs.getBoolean("consent", false);
        String state = running ? "AKTİF" : "DURDURULDU";
        status.setText("Durum: " + state + (consent ? "  •  izin açıklaması kabul edildi" : "  •  izin açıklaması bekleniyor"));
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
