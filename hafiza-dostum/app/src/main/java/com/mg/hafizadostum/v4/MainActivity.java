package com.mg.hafizadostum.v4;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int VOICE_REQ = 991;
    private static final int C_NAVY = Color.rgb(13, 53, 86);
    private static final int C_TEAL = Color.rgb(23, 184, 151);
    private static final int C_BG = Color.rgb(245, 249, 251);
    private static final int C_RED = Color.rgb(195, 57, 70);
    private LinearLayout content;
    private TextToSpeech tts;
    private boolean simpleMode;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        MemoryStore.ensureDefaults(this);
        simpleMode = uiPrefs().getBoolean("simple", false);
        initTts();
        askNotificationPermission();
        ReminderScheduler.scheduleAll(this);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) render();
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("tr", "TR"));
        });
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 51);
        }
    }

    private SharedPreferences uiPrefs() {
        return getSharedPreferences("hafiza_dostum_ui", MODE_PRIVATE);
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(40));
        content.setBackgroundColor(C_BG);
        scroll.addView(content, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        TextView title = text("Hafıza Dostum", simpleMode ? 34 : 30, C_NAVY, true);
        content.addView(title);
        TextView sub = text("İkinci hafızan • yaptığını kanıtla, unuttuğunu yakala", simpleMode ? 18 : 15, Color.DKGRAY, false);
        sub.setPadding(0, 0, 0, dp(14));
        content.addView(sub);

        addSmartCard();
        addProgress();
        addQuickActions();
        addModeActions();
        addSection("BUGÜN");
        addTasks();
        Button add = primary("＋ Yeni rutin ekle");
        add.setOnClickListener(v -> openTaskEditor(null));
        content.addView(add, margin(dp(8)));

        if (!simpleMode) {
            addSection("SON KAYITLAR");
            addHistory();
            addSupportActions();
        }
    }

    private void addSmartCard() {
        LinearLayout box = card(Color.WHITE, C_TEAL);
        TextView label = text("ŞİMDİ NE ÖNEMLİ?", 13, C_TEAL, true);
        box.addView(label);
        TextView smart = text(MemoryStore.smartNow(this), simpleMode ? 25 : 21, C_NAVY, true);
        smart.setPadding(0, dp(8), 0, dp(10));
        box.addView(smart);
        Button b = primary("Bana sıradakini söyle");
        b.setOnClickListener(v -> {
            String a = MemoryStore.smartNow(this);
            showAnswer("Şimdi ne yapmalıyım?", a);
        });
        box.addView(b);
        content.addView(box, margin(dp(10)));
    }

    private void addProgress() {
        int[] p = MemoryStore.progressToday(this);
        LinearLayout row = card(Color.rgb(232, 247, 243), Color.rgb(194, 233, 223));
        TextView t = text("Bugün  " + p[0] + " / " + p[1] + " tamamlandı", simpleMode ? 20 : 16, C_NAVY, true);
        row.addView(t);
        content.addView(row, margin(dp(6)));
    }

    private void addQuickActions() {
        LinearLayout r1 = row();
        Button did = secondary("✓ Yaptım mı?");
        Button obj = secondary("📍 Eşyam nerede?");
        r1.addView(did, weight()); r1.addView(obj, weight());
        did.setOnClickListener(v -> askDidI());
        obj.setOnClickListener(v -> objectMemory());
        content.addView(r1);

        LinearLayout r2 = row();
        Button exit = secondary("🚪 Evden çıkış");
        Button voice = secondary("🎤 Sesle sor");
        r2.addView(exit, weight()); r2.addView(voice, weight());
        exit.setOnClickListener(v -> exitChecklist());
        voice.setOnClickListener(v -> startVoice());
        content.addView(r2);
    }

    private void addModeActions() {
        LinearLayout r = row();
        Button mom = small("👩‍👧 Anne modu");
        Button busy = small("⚡ Yoğun gün");
        Button simple = small(simpleMode ? "↩ Normal görünüm" : "👓 Sade mod");
        r.addView(mom, weight()); r.addView(busy, weight()); r.addView(simple, weight());
        mom.setOnClickListener(v -> enableMomMode());
        busy.setOnClickListener(v -> enableBusyMode());
        simple.setOnClickListener(v -> {
            simpleMode = !simpleMode;
            uiPrefs().edit().putBoolean("simple", simpleMode).apply();
            render();
        });
        content.addView(r);
    }

    private void addTasks() {
        JSONArray a = MemoryStore.getTasks(this);
        int shown = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject t = a.optJSONObject(i);
            if (t == null || !t.optBoolean("active", true) || !MemoryStore.scheduledToday(t)) continue;
            addTaskCard(t);
            shown++;
        }
        if (shown == 0) content.addView(text("Bugün için planlı rutin yok.", 16, Color.GRAY, false));
    }

    private void addTaskCard(JSONObject t) {
        int border = t.optBoolean("critical") && !MemoryStore.doneToday(t) ? C_RED : Color.rgb(218, 228, 233);
        LinearLayout box = card(Color.WHITE, border);
        TextView n = text(t.optString("name"), simpleMode ? 24 : 19, C_NAVY, true);
        box.addView(n);
        TextView st = text(MemoryStore.taskStatus(t), simpleMode ? 18 : 14,
                MemoryStore.doneToday(t) ? Color.rgb(20, 125, 84) : (t.optBoolean("critical") ? C_RED : Color.DKGRAY), false);
        st.setPadding(0, dp(5), 0, dp(9));
        box.addView(st);

        LinearLayout r = row();
        Button done = primary(MemoryStore.doneToday(t) ? "✓ TEKRAR KAYDET" : "✓ YAPTIM");
        Button last = secondary("Son kayıt");
        r.addView(done, new LinearLayout.LayoutParams(0, dp(simpleMode ? 60 : 52), 1.35f));
        r.addView(last, new LinearLayout.LayoutParams(0, dp(simpleMode ? 60 : 52), 0.85f));
        box.addView(r);
        done.setOnClickListener(v -> markTask(t));
        last.setOnClickListener(v -> showLast(t));
        box.setOnLongClickListener(v -> { taskMenu(t); return true; });
        n.setOnClickListener(v -> openTaskEditor(t));
        content.addView(box, margin(dp(7)));
    }

    private void markTask(JSONObject t) {
        long last = t.optLong("lastDone", 0L);
        long diff = System.currentTimeMillis() - last;
        if (t.optBoolean("critical") && last > 0 && diff < 30 * 60_000L) {
            new AlertDialog.Builder(this)
                    .setTitle("Az önce kaydedilmiş")
                    .setMessage(t.optString("name") + " en son " + MemoryStore.formatTime(last) + "'de yapıldı olarak kaydedildi.\n\nİkinci kez kaydetmek istiyor musun?")
                    .setNegativeButton("Hayır", null)
                    .setPositiveButton("Evet, tekrar yaptım", (d, w) -> doMark(t))
                    .show();
            return;
        }
        doMark(t);
    }

    private void doMark(JSONObject t) {
        MemoryStore.markDone(this, t.optString("id"), "app");
        JSONObject fresh = MemoryStore.findTaskById(this, t.optString("id"));
        ReminderScheduler.scheduleTask(this, fresh);
        String msg = t.optString("name") + " kaydedildi. Saat " + MemoryStore.formatTime(System.currentTimeMillis());
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        speak(msg);
        render();
    }

    private void showLast(JSONObject t) {
        JSONObject fresh = MemoryStore.findTaskById(this, t.optString("id"));
        long ts = fresh == null ? 0L : fresh.optLong("lastDone", 0L);
        String m = ts <= 0 ? "Henüz kayıt yok." : "En son:\n" + MemoryStore.formatDateTime(ts) + "\n\n" + MemoryStore.answerDidI(this, t.optString("name"));
        new AlertDialog.Builder(this).setTitle(t.optString("name")).setMessage(m)
                .setNegativeButton("Kapat", null)
                .setNeutralButton(ts > 0 ? "Son kaydı geri al" : "", (d, w) -> {
                    if (ts > 0) { MemoryStore.undoLatest(this, t.optString("id")); render(); }
                }).show();
    }

    private void taskMenu(JSONObject t) {
        String[] items = {"Düzenle", "Son kaydı geri al", "Rutini sil"};
        new AlertDialog.Builder(this).setTitle(t.optString("name")).setItems(items, (d, which) -> {
            if (which == 0) openTaskEditor(t);
            else if (which == 1) { MemoryStore.undoLatest(this, t.optString("id")); render(); }
            else new AlertDialog.Builder(this).setTitle("Rutini sil?").setMessage(t.optString("name"))
                        .setNegativeButton("Vazgeç", null)
                        .setPositiveButton("Sil", (x, y) -> { MemoryStore.removeTask(this, t.optString("id")); render(); }).show();
        }).show();
    }

    private void openTaskEditor(JSONObject existing) {
        LinearLayout v = form();
        EditText name = input("Rutin adı • ör. Akşam ilacımı aldım");
        if (existing != null) name.setText(existing.optString("name"));
        v.addView(name);

        int[] hm = {existing == null ? 9 : existing.optInt("hour", 9), existing == null ? 0 : existing.optInt("minute", 0)};
        Button time = secondary(timeLabel(hm));
        time.setOnClickListener(x -> new TimePickerDialog(this, (TimePicker view, int h, int m) -> {
            hm[0] = h; hm[1] = m; time.setText(timeLabel(hm));
        }, hm[0], hm[1], true).show());
        v.addView(time, margin(dp(5)));

        CheckBox critical = new CheckBox(this);
        critical.setText("Kritik görev • yanlış tekrarları özellikle uyar");
        critical.setTextSize(simpleMode ? 18 : 15);
        critical.setChecked(existing != null && existing.optBoolean("critical"));
        v.addView(critical);

        boolean[] days = new boolean[7];
        String ds = existing == null ? "1234567" : existing.optString("days", "1234567");
        for (int i = 0; i < 7; i++) days[i] = ds.contains(String.valueOf(i + 1));
        Button dayBtn = secondary("Günler: " + dayText(days));
        dayBtn.setOnClickListener(x -> chooseDays(days, dayBtn));
        v.addView(dayBtn, margin(dp(5)));

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Yeni rutin" : "Rutini düzenle")
                .setView(v)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Kaydet", null)
                .create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(y -> {
            String n = name.getText().toString().trim();
            if (n.length() < 2) { name.setError("Bir ad yaz"); return; }
            StringBuilder dayStr = new StringBuilder();
            for (int i = 0; i < 7; i++) if (days[i]) dayStr.append(i + 1);
            if (dayStr.length() == 0) { Toast.makeText(this, "En az bir gün seç", Toast.LENGTH_SHORT).show(); return; }
            JSONArray a = MemoryStore.getTasks(this);
            if (existing == null) {
                a.put(MemoryStore.task(MemoryStore.newId(), n, hm[0], hm[1], critical.isChecked(), dayStr.toString(), "ozel"));
            } else {
                for (int i = 0; i < a.length(); i++) {
                    JSONObject t = a.optJSONObject(i);
                    if (t != null && existing.optString("id").equals(t.optString("id"))) {
                        try {
                            t.put("name", n); t.put("hour", hm[0]); t.put("minute", hm[1]);
                            t.put("critical", critical.isChecked()); t.put("days", dayStr.toString());
                        } catch (JSONException ignored) {}
                    }
                }
            }
            MemoryStore.saveTasks(this, a);
            ReminderScheduler.scheduleAll(this);
            dlg.dismiss(); render();
        }));
        dlg.show();
    }

    private void chooseDays(boolean[] days, Button target) {
        String[] labels = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};
        boolean[] work = days.clone();
        new AlertDialog.Builder(this).setTitle("Hangi günler?")
                .setMultiChoiceItems(labels, work, (d, which, checked) -> work[which] = checked)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Tamam", (d, w) -> {
                    System.arraycopy(work, 0, days, 0, 7);
                    target.setText("Günler: " + dayText(days));
                }).show();
    }

    private String dayText(boolean[] d) {
        boolean all = true, weekdays = true;
        for (boolean b : d) all &= b;
        for (int i = 0; i < 5; i++) weekdays &= d[i];
        if (all) return "Her gün";
        if (weekdays && !d[5] && !d[6]) return "Hafta içi";
        String[] s = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 7; i++) if (d[i]) { if (b.length() > 0) b.append(" "); b.append(s[i]); }
        return b.length() == 0 ? "Seçilmedi" : b.toString();
    }

    private String timeLabel(int[] hm) {
        return String.format(Locale.getDefault(), "Hatırlatma saati: %02d:%02d", hm[0], hm[1]);
    }

    private void askDidI() {
        final EditText q = input("Örn: Ocağı kapattım mı?");
        new AlertDialog.Builder(this).setTitle("Yaptım mı?").setView(q).setNegativeButton("Kapat", null)
                .setPositiveButton("Sor", (d, w) -> showAnswer("Hafıza cevabı", MemoryStore.answerDidI(this, q.getText().toString()))).show();
    }

    private void startVoice() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Sor: Ocağı kapattım mı? / Şimdi ne yapmalıyım?");
        try { startActivityForResult(i, VOICE_REQ); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "Telefonda ses tanıma hizmeti bulunamadı.", Toast.LENGTH_LONG).show(); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQ && resultCode == RESULT_OK && data != null) {
            ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (r != null && !r.isEmpty()) {
                String q = r.get(0);
                showAnswer("“" + q + "”", MemoryStore.answerDidI(this, q));
            }
        }
    }

    private void objectMemory() {
        LinearLayout f = form();
        EditText item = input("Eşya • ör. gözlük");
        EditText where = input("Nereye koydum? • ör. komodinin üstü");
        f.addView(item); f.addView(where);
        TextView hint = text("Konumu yazıp KAYDET'e bas. Sadece eşya adını yazıp ARA'ya basarsan son yerini söyler.", 13, Color.DKGRAY, false);
        f.addView(hint);
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle("Eşya hafızası").setView(f)
                .setNegativeButton("Kapat", null).setNeutralButton("ARA", null).setPositiveButton("KAYDET", null).create();
        dlg.setOnShowListener(x -> {
            dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                String q = item.getText().toString().trim();
                if (q.isEmpty()) { item.setError("Eşya adını yaz"); return; }
                showAnswer("Eşya hafızası", MemoryStore.findObject(this, q));
            });
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String i = item.getText().toString().trim(), w = where.getText().toString().trim();
                if (i.isEmpty() || w.isEmpty()) { Toast.makeText(this, "Eşya ve yerini yaz", Toast.LENGTH_SHORT).show(); return; }
                MemoryStore.rememberObject(this, i, w);
                speak(i + " için yer kaydedildi: " + w);
                Toast.makeText(this, "Kaydedildi • " + i + " → " + w, Toast.LENGTH_LONG).show();
                dlg.dismiss();
            });
        });
        dlg.show();
    }

    private void exitChecklist() {
        String[] labels = {"📱 Telefon yanımda", "🔑 Anahtar yanımda", "💳 Cüzdan / kart yanımda", "🔥 Ocak kapalı", "🚪 Kapı kilitli", "🎒 Çanta / gerekli eşya yanımda"};
        LinearLayout v = form();
        CheckBox[] checks = new CheckBox[labels.length];
        for (int i = 0; i < labels.length; i++) {
            checks[i] = new CheckBox(this); checks[i].setText(labels[i]); checks[i].setTextSize(simpleMode ? 20 : 16);
            checks[i].setPadding(0, dp(5), 0, dp(5)); v.addView(checks[i]);
        }
        new AlertDialog.Builder(this).setTitle("Evden çıkış güvence kontrolü").setView(v)
                .setNegativeButton("Kapat", null)
                .setPositiveButton("Kontrolü bitir", (d, w) -> {
                    StringBuilder miss = new StringBuilder();
                    for (int i = 0; i < checks.length; i++) if (!checks[i].isChecked()) miss.append("• ").append(labels[i]).append("\n");
                    if (miss.length() == 0) showAnswer("Hazırsın ✓", "Tüm çıkış kontrolü tamamlandı. Şimdi kafanda tekrar tekrar kontrol etmene gerek yok.");
                    else showAnswer("Henüz eksik", "Çıkmadan önce şunlara bak:\n" + miss);
                }).show();
    }

    private void enableMomMode() {
        MemoryStore.addIfMissing(this, MemoryStore.task(MemoryStore.newId(), "🎒 Çocuğun çantasını kontrol ettim", 7, 25, false, "1234567", "anne"));
        MemoryStore.addIfMissing(this, MemoryStore.task(MemoryStore.newId(), "🥤 Çocuğun suyunu hazırladım", 7, 30, false, "1234567", "anne"));
        MemoryStore.addIfMissing(this, MemoryStore.task(MemoryStore.newId(), "🧥 Hava / giysi kontrolü yaptım", 7, 35, false, "1234567", "anne"));
        ReminderScheduler.scheduleAll(this);
        Toast.makeText(this, "Anne modu rutinleri eklendi. İsim ve saatlerini değiştirebilirsin.", Toast.LENGTH_LONG).show();
        render();
    }

    private void enableBusyMode() {
        MemoryStore.addIfMissing(this, MemoryStore.task(MemoryStore.newId(), "📝 Günün 3 önemli işini belirledim", 8, 15, false, "12345", "is"));
        MemoryStore.addIfMissing(this, MemoryStore.task(MemoryStore.newId(), "🔋 Telefon / cihaz şarjını kontrol ettim", 18, 30, false, "1234567", "is"));
        MemoryStore.addIfMissing(this, MemoryStore.task(MemoryStore.newId(), "🪪 Kimlik • kart • çanta hazır", 7, 55, false, "12345", "is"));
        ReminderScheduler.scheduleAll(this);
        Toast.makeText(this, "Yoğun gün rutinleri eklendi.", Toast.LENGTH_LONG).show();
        render();
    }

    private void addHistory() {
        JSONArray e = MemoryStore.getEvents(this);
        if (e.length() == 0) {
            content.addView(text("Henüz yaptım kaydı yok.", 15, Color.GRAY, false)); return;
        }
        int start = Math.max(0, e.length() - 8);
        for (int i = e.length() - 1; i >= start; i--) {
            JSONObject x = e.optJSONObject(i);
            if (x == null) continue;
            LinearLayout row = card(Color.WHITE, Color.rgb(226, 233, 237));
            row.addView(text("✓ " + x.optString("name"), 15, C_NAVY, true));
            row.addView(text(MemoryStore.formatDateTime(x.optLong("ts")), 13, Color.DKGRAY, false));
            content.addView(row, margin(dp(4)));
        }
    }

    private void addSupportActions() {
        LinearLayout r = row();
        Button share = secondary("📤 Gün özetini paylaş");
        Button trusted = secondary("☎ Güvendiğim kişi");
        r.addView(share, weight()); r.addView(trusted, weight());
        share.setOnClickListener(v -> shareDay());
        trusted.setOnClickListener(v -> trustedPerson());
        content.addView(r);
        TextView privacy = text("🔒 Kayıtlar cihazda tutulur. Uygulama ilaç dozu kararı vermez; yalnızca senin yaptım kayıtlarını ve rutinlerini hatırlar.", 12, Color.GRAY, false);
        privacy.setPadding(dp(4), dp(16), dp(4), 0);
        content.addView(privacy);
    }

    private void shareDay() {
        Intent s = new Intent(Intent.ACTION_SEND).setType("text/plain");
        s.putExtra(Intent.EXTRA_TEXT, MemoryStore.dailyShareText(this));
        startActivity(Intent.createChooser(s, "Gün özetini paylaş"));
    }

    private void trustedPerson() {
        LinearLayout f = form();
        EditText name = input("Yakının adı");
        EditText phone = input("Telefon numarası");
        name.setText(uiPrefs().getString("trusted_name", ""));
        phone.setText(uiPrefs().getString("trusted_phone", ""));
        phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        f.addView(name); f.addView(phone);
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle("Güvendiğim kişi").setView(f)
                .setNegativeButton("Kapat", null).setNeutralButton("ARA", null).setPositiveButton("Kaydet", null).create();
        dlg.setOnShowListener(x -> {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                uiPrefs().edit().putString("trusted_name", name.getText().toString().trim()).putString("trusted_phone", phone.getText().toString().trim()).apply();
                Toast.makeText(this, "Yakın kişi kaydedildi.", Toast.LENGTH_SHORT).show(); dlg.dismiss();
            });
            dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                String p = phone.getText().toString().trim();
                if (p.isEmpty()) p = uiPrefs().getString("trusted_phone", "");
                if (p.isEmpty()) { Toast.makeText(this, "Önce telefon numarası yaz", Toast.LENGTH_SHORT).show(); return; }
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(p))));
            });
        });
        dlg.show();
    }

    private void showAnswer(String title, String answer) {
        speak(answer.replace("\n", ". "));
        new AlertDialog.Builder(this).setTitle(title).setMessage(answer).setPositiveButton("Tamam", null).show();
    }

    private void speak(String s) {
        if (tts != null) tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "hafiza");
    }

    private void addSection(String s) {
        TextView t = text(s, 13, Color.rgb(92, 112, 124), true);
        t.setPadding(dp(2), dp(18), 0, dp(7)); content.addView(t);
    }

    private LinearLayout form() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(20), dp(8), dp(20), 0); return l;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setTextSize(simpleMode ? 19 : 16); e.setSingleLine(false); e.setPadding(dp(10), dp(12), dp(10), dp(12)); return e;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }

    private LinearLayout card(int fill, int border) {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(14), dp(13), dp(14), dp(13));
        GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(18)); g.setStroke(dp(border == C_RED ? 2 : 1), border); l.setBackground(g); return l;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setLineSpacing(0, 1.08f); if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }

    private Button primary(String s) {
        Button b = new Button(this); b.setText(s); b.setTextSize(simpleMode ? 18 : 15); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setMinHeight(dp(50));
        GradientDrawable g = new GradientDrawable(); g.setColor(C_TEAL); g.setCornerRadius(dp(14)); b.setBackground(g); return b;
    }

    private Button secondary(String s) {
        Button b = new Button(this); b.setText(s); b.setTextSize(simpleMode ? 17 : 14); b.setTextColor(C_NAVY); b.setAllCaps(false); b.setMinHeight(dp(50));
        GradientDrawable g = new GradientDrawable(); g.setColor(Color.WHITE); g.setCornerRadius(dp(14)); g.setStroke(dp(1), Color.rgb(209, 222, 228)); b.setBackground(g); return b;
    }

    private Button small(String s) {
        Button b = secondary(s); b.setTextSize(simpleMode ? 15 : 12); b.setMinHeight(dp(46)); return b;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(simpleMode ? 58 : 52), 1); p.setMargins(dp(4), dp(4), dp(4), dp(4)); return p;
    }

    private LinearLayout.LayoutParams margin(int m) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); p.setMargins(0, m, 0, m); return p;
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
