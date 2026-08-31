package com.mg.hafizadostum.v4;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ArchiveActivity extends Activity {
    private static final int NAVY = Color.rgb(13,53,86);
    private static final int RED = Color.rgb(190,55,68);
    private static final int BG = Color.rgb(245,249,251);
    private long selectedDay = System.currentTimeMillis();
    private CalendarView calendar;
    private LinearLayout entries;
    private TextView dayTitle;
    private TextView monthInfo;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        UiUtil.prepareWindow(this);
        ArchiveStore.importLegacy(this, MemoryStore.getEvents(this));
        render();
    }

    private void render() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        sv.addView(root);
        setContentView(sv);
        UiUtil.applyInsets(root, 16, 18, 16, 36);

        TextView title = text("6 Aylık Hafıza Arşivi", 28, NAVY, true);
        root.addView(title);
        TextView sub = text("Takvimden bir gün seç. Kayıtlarını gör, geçmişe not ekle veya istemediğin kaydı silebilirsin. Bu arşiv bütün profil ve mesleklerde aynıdır.", 15, Color.DKGRAY, false);
        sub.setPadding(0, dp(4), 0, dp(10)); root.addView(sub);

        calendar = new CalendarView(this);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinDate(ArchiveStore.minDate());
        calendar.setMaxDate(System.currentTimeMillis());
        calendar.setDate(selectedDay, false, true);
        root.addView(calendar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(330)));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        Button prev = button("‹ Önceki gün");
        Button today = button("Bugün");
        Button next = button("Sonraki gün ›");
        nav.addView(prev, weight()); nav.addView(today, weight()); nav.addView(next, weight());
        root.addView(nav);

        dayTitle = text("", 22, NAVY, true);
        dayTitle.setPadding(0, dp(18), 0, dp(4)); root.addView(dayTitle);
        monthInfo = text("", 14, Color.DKGRAY, false); root.addView(monthInfo);

        LinearLayout manage = new LinearLayout(this);
        manage.setOrientation(LinearLayout.HORIZONTAL);
        Button addManual = button("＋ Bu güne kayıt ekle");
        Button clearDay = dangerButton("🗑 Günü temizle");
        manage.addView(addManual, weightWide());
        manage.addView(clearDay, weightNarrow());
        root.addView(manage);

        entries = new LinearLayout(this);
        entries.setOrientation(LinearLayout.VERTICAL);
        root.addView(entries);

        Button share = button("📤 Bu günü paylaş");
        share.setOnClickListener(v -> shareSelectedDay());
        root.addView(share, full(dp(54), dp(14)));

        Button back = button("← Hafıza Dostum'a dön");
        back.setOnClickListener(v -> finish());
        root.addView(back, full(dp(54), dp(5)));

        calendar.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth, 12, 0, 0); c.set(Calendar.MILLISECOND, 0);
            selectedDay = c.getTimeInMillis();
            updateDay();
        });
        prev.setOnClickListener(v -> moveDay(-1));
        next.setOnClickListener(v -> moveDay(1));
        today.setOnClickListener(v -> {
            selectedDay = System.currentTimeMillis();
            calendar.setDate(selectedDay, true, true);
            updateDay();
        });
        addManual.setOnClickListener(v -> addManualEntry());
        clearDay.setOnClickListener(v -> confirmClearDay());
        updateDay();
    }

    private void moveDay(int amount) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(selectedDay); c.add(Calendar.DAY_OF_YEAR, amount);
        long target = c.getTimeInMillis();
        if (target < ArchiveStore.minDate() || target > System.currentTimeMillis()) return;
        selectedDay = target;
        calendar.setDate(selectedDay, true, true);
        updateDay();
    }

    private void updateDay() {
        String d = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("tr","TR")).format(new Date(selectedDay));
        dayTitle.setText(d);
        int monthCount = ArchiveStore.countMonth(this, selectedDay);
        monthInfo.setText("Bu ay arşivlenen toplam kayıt: " + monthCount + " • Tek tek silme ve manuel ekleme açık");
        entries.removeAllViews();
        JSONArray a = ArchiveStore.forDay(this, selectedDay);
        if (a.length() == 0) {
            LinearLayout empty = card();
            empty.addView(text("Bu tarihte kayıt yok", 18, NAVY, true));
            empty.addView(text("İstersen yukarıdaki 'Bu güne kayıt ekle' ile sonradan hatırladığın bir olayı veya işi arşive ekleyebilirsin.", 14, Color.DKGRAY, false));
            entries.addView(empty, full(-2, dp(10)));
            return;
        }
        for (int i = 0; i < a.length(); i++) {
            JSONObject x = a.optJSONObject(i);
            if (x == null) continue;
            LinearLayout row = card();
            row.addView(text(("manual".equals(x.optString("source")) ? "📝 " : "✓ ") + x.optString("name", "Kayıt"), 17, NAVY, true));
            row.addView(text(MemoryStore.formatTime(x.optLong("ts")) + " • " + sourceText(x.optString("source")), 14, Color.DKGRAY, false));
            Button del = dangerButton("Bu kaydı sil");
            del.setOnClickListener(v -> confirmDeleteEntry(x));
            row.addView(del, full(dp(48), dp(7)));
            entries.addView(row, full(-2, dp(5)));
        }
    }

    private void addManualEntry() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), 0);
        EditText name = new EditText(this);
        name.setHint("Örn. Doktor randevusuna gittim / faturayı ödedim");
        name.setTextSize(16);
        form.addView(name);

        Calendar base = Calendar.getInstance();
        base.setTimeInMillis(selectedDay);
        Calendar now = Calendar.getInstance();
        boolean today = base.get(Calendar.YEAR) == now.get(Calendar.YEAR) && base.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
        final int[] hm = {today ? now.get(Calendar.HOUR_OF_DAY) : 12, today ? now.get(Calendar.MINUTE) : 0};
        Button time = button(timeLabel(hm[0], hm[1]));
        time.setOnClickListener(v -> new TimePickerDialog(this, (view, h, m) -> {
            hm[0] = h; hm[1] = m; time.setText(timeLabel(h, m));
        }, hm[0], hm[1], true).show());
        form.addView(time, full(dp(52), dp(8)));

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Geçmişe kayıt ekle")
                .setMessage("Bu kayıt seçtiğin tarihin 6 aylık arşivine eklenir. Bir rutin oluşturmaz ve yeni bildirim başlatmaz.")
                .setView(form)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Arşive ekle", null)
                .create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if (n.length() < 2) { name.setError("Bir kayıt açıklaması yaz"); return; }
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selectedDay);
            c.set(Calendar.HOUR_OF_DAY, hm[0]); c.set(Calendar.MINUTE, hm[1]); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
            long ts = c.getTimeInMillis();
            if (ts > System.currentTimeMillis() + 60_000L) {
                Toast.makeText(this, "Gelecekteki bir saate arşiv kaydı eklenemez.", Toast.LENGTH_LONG).show();
                return;
            }
            ArchiveStore.recordManual(this, n, ts);
            dlg.dismiss();
            updateDay();
            Toast.makeText(this, "Arşiv kaydı eklendi.", Toast.LENGTH_SHORT).show();
        }));
        dlg.show();
    }

    private void confirmDeleteEntry(JSONObject x) {
        String name = x.optString("name", "Kayıt");
        String time = MemoryStore.formatTime(x.optLong("ts"));
        new AlertDialog.Builder(this)
                .setTitle("Arşiv kaydını sil?")
                .setMessage(time + " • " + name + "\n\nBu işlem yalnız bu kayıt için geçerlidir. İlgili rutin silinmez.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Sil", (d, w) -> {
                    syncCurrentTaskIfNeeded(x);
                    if (ArchiveStore.deleteById(this, x.optString("id"))) {
                        Toast.makeText(this, "Kayıt arşivden silindi.", Toast.LENGTH_SHORT).show();
                        updateDay();
                    }
                }).show();
    }

    private void confirmClearDay() {
        JSONArray day = ArchiveStore.forDay(this, selectedDay);
        if (day.length() == 0) {
            Toast.makeText(this, "Bu günde silinecek kayıt yok.", Toast.LENGTH_SHORT).show();
            return;
        }
        String label = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr","TR")).format(new Date(selectedDay));
        new AlertDialog.Builder(this)
                .setTitle("Bu günün arşivini temizle?")
                .setMessage(label + " tarihindeki " + day.length() + " kayıt silinecek. Rutinlerin kendisi silinmez.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Tümünü sil", (d, w) -> {
                    for (int i = day.length() - 1; i >= 0; i--) {
                        JSONObject x = day.optJSONObject(i);
                        if (x != null) syncCurrentTaskIfNeeded(x);
                    }
                    int deleted = ArchiveStore.deleteDay(this, selectedDay);
                    Toast.makeText(this, deleted + " kayıt silindi.", Toast.LENGTH_LONG).show();
                    updateDay();
                }).show();
    }

    private void syncCurrentTaskIfNeeded(JSONObject x) {
        if (x == null || "manual".equals(x.optString("source"))) return;
        String taskId = x.optString("taskId", "");
        if (taskId.isEmpty() || taskId.startsWith("manual_")) return;
        JSONObject task = MemoryStore.findTaskById(this, taskId);
        long ts = x.optLong("ts", 0L);
        if (task != null && task.optLong("lastDone", 0L) == ts) {
            MemoryStore.undoLatest(this, taskId);
        }
    }

    private String timeLabel(int h, int m) {
        return String.format(Locale.getDefault(), "Saat: %02d:%02d", h, m);
    }

    private String sourceText(String s) {
        if ("notification".equals(s)) return "bildirimden kaydedildi";
        if ("legacy".equals(s)) return "önceki sürüm kaydı";
        if ("backup".equals(s)) return "yedekten geri yüklendi";
        if ("manual".equals(s)) return "elle arşive eklendi";
        return "uygulamadan kaydedildi";
    }

    private void shareSelectedDay() {
        JSONArray a = ArchiveStore.forDay(this, selectedDay);
        StringBuilder out = new StringBuilder();
        out.append("Hafıza Dostum • ").append(new SimpleDateFormat("dd MMMM yyyy", new Locale("tr","TR")).format(new Date(selectedDay))).append("\n\n");
        if (a.length() == 0) out.append("Kayıt yok.");
        for (int i = 0; i < a.length(); i++) {
            JSONObject x = a.optJSONObject(i);
            if (x != null) out.append("• ").append(MemoryStore.formatTime(x.optLong("ts"))).append(" • ").append(x.optString("name")).append("\n");
        }
        Intent s = new Intent(Intent.ACTION_SEND).setType("text/plain");
        s.putExtra(Intent.EXTRA_TEXT, out.toString());
        startActivity(Intent.createChooser(s, "Gün arşivini paylaş"));
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(14),dp(13),dp(14),dp(13));
        GradientDrawable g = new GradientDrawable(); g.setColor(Color.WHITE); g.setCornerRadius(dp(18)); g.setStroke(dp(1), Color.rgb(211,225,231)); l.setBackground(g); return l;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setLineSpacing(0,1.08f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }

    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setTextSize(14); b.setTextColor(NAVY); b.setAllCaps(false); b.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(); g.setColor(Color.WHITE); g.setCornerRadius(dp(14)); g.setStroke(dp(1), Color.rgb(205,221,228)); b.setBackground(g); return b;
    }

    private Button dangerButton(String s) {
        Button b = new Button(this); b.setText(s); b.setTextSize(13); b.setTextColor(RED); b.setAllCaps(false); b.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(); g.setColor(Color.WHITE); g.setCornerRadius(dp(14)); g.setStroke(dp(1), Color.rgb(235,190,196)); b.setBackground(g); return b;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(52), 1); p.setMargins(dp(3),dp(4),dp(3),dp(4)); return p;
    }

    private LinearLayout.LayoutParams weightWide() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(54), 1.35f); p.setMargins(dp(3),dp(8),dp(3),dp(6)); return p;
    }

    private LinearLayout.LayoutParams weightNarrow() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(54), .75f); p.setMargins(dp(3),dp(8),dp(3),dp(6)); return p;
    }

    private LinearLayout.LayoutParams full(int h, int m) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h == -2 ? LinearLayout.LayoutParams.WRAP_CONTENT : h);
        p.setMargins(0,m,0,m); return p;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
}
