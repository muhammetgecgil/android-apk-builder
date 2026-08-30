package com.mg.hafizadostum.v4;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ArchiveActivity extends Activity {
    private static final int NAVY = Color.rgb(13,53,86);
    private static final int TEAL = Color.rgb(23,184,151);
    private static final int BG = Color.rgb(245,249,251);
    private long selectedDay = System.currentTimeMillis();
    private CalendarView calendar;
    private LinearLayout entries;
    private TextView dayTitle;
    private TextView monthInfo;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ArchiveStore.importLegacy(this, MemoryStore.getEvents(this));
        render();
    }

    private void render() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(36));
        root.setBackgroundColor(BG);
        sv.addView(root);
        setContentView(sv);

        TextView title = text("6 Aylık Hafıza Arşivi", 28, NAVY, true);
        root.addView(title);
        TextView sub = text("Takvimden bir gün seç. O gün yaptım diye kaydettiğin işler saatleriyle birlikte burada kalır.", 15, Color.DKGRAY, false);
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
        monthInfo.setText("Bu ay arşivlenen toplam yaptım kaydı: " + monthCount);
        entries.removeAllViews();
        JSONArray a = ArchiveStore.forDay(this, selectedDay);
        if (a.length() == 0) {
            LinearLayout empty = card();
            empty.addView(text("Bu tarihte kayıt yok", 18, NAVY, true));
            empty.addView(text("Bu, o gün hiçbir şey yapmadığın anlamına gelmez; yalnızca uygulamada 'yaptım' kaydı oluşturulmamış.", 14, Color.DKGRAY, false));
            entries.addView(empty, full(-2, dp(10)));
            return;
        }
        for (int i = 0; i < a.length(); i++) {
            JSONObject x = a.optJSONObject(i);
            if (x == null) continue;
            LinearLayout row = card();
            row.addView(text("✓ " + x.optString("name", "Kayıt"), 17, NAVY, true));
            row.addView(text(MemoryStore.formatTime(x.optLong("ts")) + " • " + sourceText(x.optString("source")), 14, Color.DKGRAY, false));
            entries.addView(row, full(-2, dp(5)));
        }
    }

    private String sourceText(String s) {
        if ("notification".equals(s)) return "bildirimden kaydedildi";
        if ("legacy".equals(s)) return "önceki sürüm kaydı";
        return "uygulamadan kaydedildi";
    }

    private void shareSelectedDay() {
        JSONArray a = ArchiveStore.forDay(this, selectedDay);
        StringBuilder out = new StringBuilder();
        out.append("Hafıza Dostum • ").append(new SimpleDateFormat("dd MMMM yyyy", new Locale("tr","TR")).format(new Date(selectedDay))).append("\n\n");
        if (a.length() == 0) out.append("Kayıt yok.");
        for (int i = 0; i < a.length(); i++) {
            JSONObject x = a.optJSONObject(i);
            if (x != null) out.append("✓ ").append(MemoryStore.formatTime(x.optLong("ts"))).append(" • ").append(x.optString("name")).append("\n");
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

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(52), 1); p.setMargins(dp(3),dp(4),dp(3),dp(4)); return p;
    }

    private LinearLayout.LayoutParams full(int h, int m) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h == -2 ? LinearLayout.LayoutParams.WRAP_CONTENT : h);
        p.setMargins(0,m,0,m); return p;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
}
