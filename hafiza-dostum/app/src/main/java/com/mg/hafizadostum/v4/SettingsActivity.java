package com.mg.hafizadostum.v4;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends Activity {
    private static final int EXPORT_REQ = 701;
    private static final int IMPORT_REQ = 702;
    private static final int NAVY = Color.rgb(13,53,86);
    private static final int TEAL = Color.rgb(23,184,151);
    private static final int BG = Color.rgb(245,249,251);

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        UiUtil.prepareWindow(this);
        render();
    }

    private void render() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        sv.addView(root);
        setContentView(sv);
        UiUtil.applyInsets(root, 18, 18, 18, 28);

        root.addView(text("Ayarlar", 30, NAVY, true));
        TextView sub = text("Profil, veri güvenliği ve uygulama kontrolleri", 15, Color.DKGRAY, false);
        sub.setPadding(0, dp(4), 0, dp(14)); root.addView(sub);

        root.addView(section("KİŞİSELLEŞTİRME"));
        root.addView(action("👤 Profil ayarları", "Meslek, anne/baba ve diğer yaşam rollerini değiştir", v -> {
            Intent i = new Intent(this, ProfileActivity.class); i.setAction(ProfileActivity.ACTION_EDIT); startActivity(i);
        }));
        root.addView(action("🗓 6 aylık arşiv", "Geçmiş günlerde ne yaptığını tarih ve saatle gör", v -> startActivity(new Intent(this, ArchiveActivity.class))));

        root.addView(section("YEDEKLEME"));
        root.addView(action("⬆ Yedeği dışa aktar", "Rutin, profil, eşya hafızası ve 6 aylık arşivi tek JSON dosyasına kaydet", v -> exportBackup()));
        root.addView(action("⬇ Yedeği geri yükle", "Daha önce oluşturduğun Hafıza Dostum yedeğini geri getir", v -> importBackup()));
        TextView local = text("Yedek dosyası yalnızca sen seçtiğinde oluşturulur. Uygulama kendi başına internete veri göndermez.", 13, Color.GRAY, false);
        local.setPadding(dp(4), dp(4), dp(4), dp(10)); root.addView(local);

        root.addView(section("BİLDİRİMLER VE ERİŞİLEBİLİRLİK"));
        root.addView(action("🔔 Android bildirim ayarları", "Bildirim izni, ses, titreşim ve kanal tercihlerini yönet", v -> openNotificationSettings()));
        boolean simple = getSharedPreferences("hafiza_dostum_ui", MODE_PRIVATE).getBoolean("simple", false);
        root.addView(action(simple ? "↩ Normal görünüm" : "👓 Sade / büyük yazı", simple ? "Standart arayüze dön" : "Daha büyük yazı ve daha az görsel kalabalık kullan", v -> {
            boolean old = getSharedPreferences("hafiza_dostum_ui", MODE_PRIVATE).getBoolean("simple", false);
            getSharedPreferences("hafiza_dostum_ui", MODE_PRIVATE).edit().putBoolean("simple", !old).apply();
            render();
        }));

        root.addView(section("GİZLİLİK VE GÜVENLİK"));
        root.addView(action("🔒 Gizlilik ve sağlık bilgisi", "Verilerin nasıl saklandığını ve uygulamanın sınırlarını oku", v -> startActivity(new Intent(this, PrivacyActivity.class))));
        root.addView(action("🗑 Tüm yerel verilerimi sil", "Profil, rutin, eşya hafızası ve arşiv dahil cihazdaki tüm uygulama verilerini temizle", v -> confirmDelete()));

        root.addView(section("HAKKINDA"));
        LinearLayout info = card();
        info.addView(text("Hafıza Dostum 5.0", 18, NAVY, true));
        info.addView(text("Android 16 / API 36 hedefli • çevrimdışı çekirdek • 6 aylık SQLite arşiv • manuel yedekleme", 14, Color.DKGRAY, false));
        root.addView(info, margin(dp(6)));

        Button close = primary("Ana ekrana dön");
        close.setOnClickListener(v -> finish());
        root.addView(close, margin(dp(14)));
    }

    private void exportBackup() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        String d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        i.putExtra(Intent.EXTRA_TITLE, "HafizaDostum-Yedek-" + d + ".json");
        startActivityForResult(i, EXPORT_REQ);
    }

    private void importBackup() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        startActivityForResult(i, IMPORT_REQ);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == EXPORT_REQ) {
                String json = BackupStore.exportJson(this);
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os == null) throw new IllegalStateException("Dosya açılamadı");
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                Toast.makeText(this, "Yedek dosyası oluşturuldu.", Toast.LENGTH_LONG).show();
            } else if (requestCode == IMPORT_REQ) {
                String json;
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    if (is == null) throw new IllegalStateException("Dosya açılamadı");
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192]; int n;
                    while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                    json = bos.toString("UTF-8");
                }
                new AlertDialog.Builder(this).setTitle("Yedeği geri yükle?")
                        .setMessage("Mevcut profil ve rutin verileri yedekteki bilgilerle değiştirilecek. Bu işlemden önce istersen mevcut yedeğini dışa aktar.")
                        .setNegativeButton("Vazgeç", null)
                        .setPositiveButton("Geri yükle", (d,w) -> {
                            try {
                                BackupStore.importJson(this, json);
                                Toast.makeText(this, "Yedek geri yüklendi.", Toast.LENGTH_LONG).show();
                                Intent home = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(home); finish();
                            } catch (Exception e) {
                                new AlertDialog.Builder(this).setTitle("Yedek açılamadı").setMessage(e.getMessage()).setPositiveButton("Tamam", null).show();
                            }
                        }).show();
            }
        } catch (Exception e) {
            new AlertDialog.Builder(this).setTitle("İşlem tamamlanamadı").setMessage(e.getMessage()).setPositiveButton("Tamam", null).show();
        }
    }

    private void openNotificationSettings() {
        Intent i;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        }
        startActivity(i);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this).setTitle("Tüm yerel verileri sil?")
                .setMessage("Profil, rutinler, yaptım kayıtları, eşya hafızası, güvenilen kişi ve 6 aylık arşiv bu cihazdan silinecek. Bu işlem geri alınamaz. Önce yedek oluşturabilirsin.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("TÜMÜNÜ SİL", (d,w) -> {
                    BackupStore.clearAll(this);
                    Toast.makeText(this, "Yerel veriler silindi.", Toast.LENGTH_LONG).show();
                    Intent i = new Intent(this, ProfileActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i); finish();
                }).show();
    }

    private LinearLayout action(String title, String desc, android.view.View.OnClickListener listener) {
        LinearLayout c = card();
        TextView t = text(title, 17, NAVY, true);
        TextView d = text(desc, 13, Color.DKGRAY, false); d.setPadding(0, dp(4), 0, 0);
        c.addView(t); c.addView(d); c.setClickable(true); c.setFocusable(true); c.setOnClickListener(listener);
        c.setContentDescription(title + ". " + desc);
        return c;
    }

    private TextView section(String s) { TextView t = text(s, 13, Color.rgb(72,102,118), true); t.setPadding(0, dp(16), 0, dp(6)); return t; }
    private LinearLayout card() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(15),dp(14),dp(15),dp(14)); GradientDrawable g = new GradientDrawable(); g.setColor(Color.WHITE); g.setCornerRadius(dp(16)); g.setStroke(dp(1), Color.rgb(210,225,231)); l.setBackground(g); l.setMinimumHeight(dp(58)); return l; }
    private TextView text(String s, int sp, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setLineSpacing(0,1.08f); if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t; }
    private Button primary(String s) { Button b = new Button(this); b.setText(s); b.setTextSize(16); b.setTextColor(Color.WHITE); b.setAllCaps(false); GradientDrawable g = new GradientDrawable(); g.setColor(TEAL); g.setCornerRadius(dp(14)); b.setBackground(g); b.setMinHeight(dp(54)); return b; }
    private LinearLayout.LayoutParams margin(int m) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); p.setMargins(0,m,0,m); return p; }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
}
