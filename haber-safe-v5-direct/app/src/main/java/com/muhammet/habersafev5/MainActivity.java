package com.muhammet.habersafev5;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(7, 17, 38);
    private static final int TOP = Color.rgb(31, 26, 22);
    private static final int CARD = Color.rgb(29, 48, 83);
    private static final int CARD_BORDER = Color.rgb(43, 67, 109);
    private static final int WHITE = Color.rgb(248, 250, 255);
    private static final int MUTED = Color.rgb(180, 193, 219);
    private static final int GREEN = Color.rgb(82, 214, 163);
    private static final int ORANGE = Color.rgb(244, 178, 74);
    private static final int REQ_NOTIFICATIONS = 77;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final LinkedHashMap<String, String> categories = NewsRepository.categories();
    private final ArrayList<NewsRepository.NewsItem> items = new ArrayList<>();

    private EditText search;
    private ProgressBar progress;
    private TextView status;
    private NewsAdapter adapter;
    private String currentTitle = "";
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(TOP);
        getWindow().setNavigationBarColor(Color.BLACK);
        showHome();
        if (getIntent() != null && getIntent().getBooleanExtra("open_my_news", false)) {
            main.postDelayed(this::loadMyNews, 120);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra("open_my_news", false)) {
            loadMyNews();
        }
    }

    private void showHome() {
        currentTitle = "";
        currentQuery = "";

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = vertical(BG);
        root.setPadding(dp(18), dp(20), dp(18), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("Haber SAFE V5.1", 28, WHITE, true);
        title.setPadding(dp(4), 0, 0, dp(18));
        root.addView(title);

        LinearLayout intro = vertical(Color.rgb(20, 32, 59));
        intro.setPadding(dp(18), dp(14), dp(18), dp(14));
        intro.setBackground(rounded(Color.rgb(20, 32, 59), CARD_BORDER, 18));
        TextView safe = text("DOĞRUDAN HABER + KİŞİSEL GÜNLÜK", 18, GREEN, true);
        TextView help = text("Haber başlıkları RSS kaynağından doğrudan gelir. Uygulama yorumlama, özetleme veya yeniden yazma yapmaz.", 16, MUTED, false);
        help.setPadding(0, dp(10), 0, 0);
        intro.addView(safe);
        intro.addView(help);
        root.addView(intro, matchWrap(dp(14)));

        LinearLayout personal = vertical(Color.rgb(15, 31, 55));
        personal.setPadding(dp(14), dp(14), dp(14), dp(14));
        personal.setBackground(rounded(Color.rgb(15, 31, 55), CARD_BORDER, 18));
        TextView personalTitle = text("KİŞİSEL HABERLER", 16, WHITE, true);
        personal.addView(personalTitle);

        Button myNews = button("BENİM HABERLERİM");
        myNews.setOnClickListener(v -> loadMyNews());
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        p1.setMargins(0, dp(12), 0, dp(8));
        personal.addView(myNews, p1);

        Button reminder = button("GÜNLÜK BİLDİRİM AYARLARI");
        reminder.setOnClickListener(v -> showReminderDialog());
        personal.addView(reminder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        TextView reminderState = text(reminderStateText(), 14, ORANGE, false);
        reminderState.setPadding(dp(2), dp(10), dp(2), 0);
        personal.addView(reminderState);
        root.addView(personal, matchWrap(dp(16)));

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);

        search = new EditText(this);
        search.setTextColor(WHITE);
        search.setHintTextColor(Color.rgb(130, 148, 180));
        search.setHint("Konu ara: Artemis 2, COMAC...");
        search.setTextSize(16);
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setPadding(dp(14), dp(10), dp(14), dp(10));
        search.setBackground(rounded(Color.rgb(16, 29, 54), CARD_BORDER, 14));
        searchRow.addView(search, new LinearLayout.LayoutParams(0, dp(54), 1f));

        Button searchButton = button("ARA");
        LinearLayout.LayoutParams searchButtonLp = new LinearLayout.LayoutParams(dp(92), dp(54));
        searchButtonLp.setMargins(dp(10), 0, 0, 0);
        searchRow.addView(searchButton, searchButtonLp);
        root.addView(searchRow, matchWrap(dp(16)));

        View.OnClickListener doSearch = v -> {
            String q = search.getText().toString().trim();
            if (q.isEmpty()) {
                Toast.makeText(this, "Aramak istediğin konuyu yaz.", Toast.LENGTH_SHORT).show();
                return;
            }
            loadNews(q, "Arama: " + q);
        };
        searchButton.setOnClickListener(doSearch);
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch.onClick(v);
                return true;
            }
            return false;
        });

        List<Map.Entry<String, String>> entries = new ArrayList<>(categories.entrySet());
        for (int i = 0; i < entries.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            Map.Entry<String, String> left = entries.get(i);
            Button b1 = categoryButton(left.getKey());
            b1.setOnClickListener(v -> loadNews(left.getValue(), left.getKey()));
            row.addView(b1, weightedButton(0));

            if (i + 1 < entries.size()) {
                Map.Entry<String, String> right = entries.get(i + 1);
                Button b2 = categoryButton(right.getKey());
                b2.setOnClickListener(v -> loadNews(right.getValue(), right.getKey()));
                row.addView(b2, weightedButton(dp(10)));
            } else {
                View spacer = new View(this);
                row.addView(spacer, weightedButton(dp(10)));
            }
            root.addView(row, matchWrap(dp(10)));
        }

        setContentView(scroll);
    }

    private String reminderStateText() {
        SharedPreferences prefs = getSharedPreferences(NewsReminderReceiver.PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(NewsReminderReceiver.KEY_ENABLED, false)) {
            return "Günlük bildirim kapalı";
        }
        Set<String> selected = prefs.getStringSet(NewsReminderReceiver.KEY_SELECTED, new HashSet<>());
        int hour = prefs.getInt(NewsReminderReceiver.KEY_HOUR, 8);
        int minute = prefs.getInt(NewsReminderReceiver.KEY_MINUTE, 0);
        return String.format(java.util.Locale.getDefault(), "Her gün %02d:%02d • %d alan seçili", hour, minute, selected.size());
    }

    private void loadMyNews() {
        SharedPreferences prefs = getSharedPreferences(NewsReminderReceiver.PREFS, MODE_PRIVATE);
        Set<String> selected = new HashSet<>(prefs.getStringSet(NewsReminderReceiver.KEY_SELECTED, new HashSet<>()));
        if (selected.isEmpty()) {
            Toast.makeText(this, "Önce ilgilendiğin haber alanlarını seç.", Toast.LENGTH_LONG).show();
            showReminderDialog();
            return;
        }
        String query = NewsRepository.combinedQuery(selected);
        loadNews(query, "Benim Haberlerim");
    }

    private void showReminderDialog() {
        SharedPreferences prefs = getSharedPreferences(NewsReminderReceiver.PREFS, MODE_PRIVATE);
        boolean enabledSaved = prefs.getBoolean(NewsReminderReceiver.KEY_ENABLED, false);
        Set<String> selectedSaved = new HashSet<>(prefs.getStringSet(NewsReminderReceiver.KEY_SELECTED, new HashSet<>()));
        int[] time = {
                prefs.getInt(NewsReminderReceiver.KEY_HOUR, 8),
                prefs.getInt(NewsReminderReceiver.KEY_MINUTE, 0)
        };

        ScrollView scroller = new ScrollView(this);
        LinearLayout box = vertical(Color.WHITE);
        box.setPadding(dp(18), dp(12), dp(18), dp(8));
        scroller.addView(box);

        CheckBox enabled = new CheckBox(this);
        enabled.setText("Günlük haber bildirimini aç");
        enabled.setTextSize(17);
        enabled.setChecked(enabledSaved);
        box.addView(enabled);

        Button timeButton = new Button(this);
        timeButton.setAllCaps(false);
        timeButton.setText(String.format(java.util.Locale.getDefault(), "Bildirim saati: %02d:%02d", time[0], time[1]));
        timeButton.setOnClickListener(v -> new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    time[0] = hourOfDay;
                    time[1] = minute;
                    timeButton.setText(String.format(java.util.Locale.getDefault(), "Bildirim saati: %02d:%02d", hourOfDay, minute));
                },
                time[0],
                time[1],
                true
        ).show());
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        timeLp.setMargins(0, dp(8), 0, dp(12));
        box.addView(timeButton, timeLp);

        TextView choose = text("Bildirimde takip edilecek alanları seç:", 15, Color.rgb(30, 40, 58), true);
        choose.setPadding(0, 0, 0, dp(6));
        box.addView(choose);

        LinkedHashMap<String, CheckBox> checks = new LinkedHashMap<>();
        for (String name : categories.keySet()) {
            CheckBox cb = new CheckBox(this);
            cb.setText(name);
            cb.setTextSize(16);
            cb.setChecked(selectedSaved.contains(name));
            cb.setPadding(0, dp(2), 0, dp(2));
            checks.put(name, cb);
            box.addView(cb);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Kişisel Günlük Haber")
                .setView(scroller)
                .setNegativeButton("İptal", null)
                .setPositiveButton("Kaydet", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            HashSet<String> selected = new HashSet<>();
            for (Map.Entry<String, CheckBox> e : checks.entrySet()) {
                if (e.getValue().isChecked()) selected.add(e.getKey());
            }
            if (enabled.isChecked() && selected.isEmpty()) {
                Toast.makeText(this, "Bildirim için en az bir alan seç.", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                    .putBoolean(NewsReminderReceiver.KEY_ENABLED, enabled.isChecked())
                    .putStringSet(NewsReminderReceiver.KEY_SELECTED, selected)
                    .putInt(NewsReminderReceiver.KEY_HOUR, time[0])
                    .putInt(NewsReminderReceiver.KEY_MINUTE, time[1])
                    .apply();

            if (enabled.isChecked()) {
                requestNotificationPermissionIfNeeded();
                NewsReminderReceiver.scheduleNext(this);
                Toast.makeText(this, "Günlük haber bildirimi ayarlandı.", Toast.LENGTH_SHORT).show();
            } else {
                NewsReminderReceiver.cancel(this);
                Toast.makeText(this, "Günlük haber bildirimi kapatıldı.", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
            showHome();
        }));
        dialog.show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    private void loadNews(String query, String title) {
        currentQuery = query;
        currentTitle = title;
        items.clear();
        showResults(title);
        fetchIntoResults(query, title);
    }

    private void showResults(String title) {
        LinearLayout screen = vertical(BG);
        screen.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        Button back = button("‹");
        back.setTextSize(30);
        back.setOnClickListener(v -> showHome());
        top.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));

        TextView heading = text(title, 21, WHITE, true);
        heading.setPadding(dp(12), 0, dp(8), 0);
        top.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button refresh = button("Yenile");
        refresh.setTextSize(14);
        refresh.setOnClickListener(v -> fetchIntoResults(currentQuery, currentTitle));
        top.addView(refresh, new LinearLayout.LayoutParams(dp(82), dp(52)));
        screen.addView(top);

        progress = new ProgressBar(this);
        screen.addView(progress, matchWrap(dp(6)));

        status = text("Haber kaynakları taranıyor…", 15, MUTED, false);
        status.setPadding(dp(4), dp(6), dp(4), dp(10));
        screen.addView(status);

        ListView listView = new ListView(this);
        listView.setDividerHeight(dp(8));
        listView.setDivider(new android.graphics.drawable.ColorDrawable(BG));
        listView.setBackgroundColor(BG);
        listView.setCacheColorHint(BG);
        adapter = new NewsAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            NewsRepository.NewsItem item = items.get(position);
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.link)));
            } catch (Exception e) {
                Toast.makeText(this, "Haber bağlantısı açılamadı.", Toast.LENGTH_SHORT).show();
            }
        });
        screen.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(screen);
    }

    private void fetchIntoResults(String query, String title) {
        if (query == null || query.trim().isEmpty()) return;
        if (!hasNetwork()) {
            progress.setVisibility(View.GONE);
            status.setText("İnternet bağlantısı yok. Bağlantıyı açıp Yenile'ye bas.");
            return;
        }

        progress.setVisibility(View.VISIBLE);
        status.setText("Haberler doğrudan kaynaktan alınıyor…");

        io.execute(() -> {
            try {
                List<NewsRepository.NewsItem> downloaded = NewsRepository.fetchGoogleNews(query, 50);
                main.post(() -> {
                    if (!title.equals(currentTitle)) return;
                    items.clear();
                    items.addAll(downloaded);
                    adapter.notifyDataSetChanged();
                    progress.setVisibility(View.GONE);
                    status.setText(items.isEmpty()
                            ? "Bu başlıkta haber bulunamadı."
                            : items.size() + " haber bulundu. Başlıklar yorumlanmadan gösteriliyor; habere dokununca özgün kaynak açılır.");
                });
            } catch (Exception e) {
                main.post(() -> {
                    if (!title.equals(currentTitle)) return;
                    progress.setVisibility(View.GONE);
                    status.setText("Haberler alınamadı: " + safeMessage(e) + "\nYenile ile tekrar dene.");
                });
            }
        });
    }

    private boolean hasNetwork() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network network = cm.getActiveNetwork();
            return network != null;
        } catch (Exception e) {
            return true;
        }
    }

    private String safeMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.trim().isEmpty()) return "bağlantı hatası";
        if (msg.length() > 90) return msg.substring(0, 90);
        return msg;
    }

    private LinearLayout vertical(int color) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(color);
        return l;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(WHITE);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setPadding(dp(8), dp(4), dp(8), dp(4));
        b.setBackground(rounded(CARD, CARD_BORDER, 14));
        return b;
    }

    private Button categoryButton(String value) {
        Button b = button(value);
        b.setTextSize(15);
        b.setGravity(Gravity.CENTER);
        return b;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private LinearLayout.LayoutParams matchWrap(int bottomMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, bottomMargin);
        return lp;
    }

    private LinearLayout.LayoutParams weightedButton(int leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(58), 1f);
        lp.setMargins(leftMargin, 0, 0, 0);
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    private final class NewsAdapter extends BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            NewsRepository.NewsItem item = items.get(position);
            LinearLayout card = vertical(CARD);
            card.setPadding(dp(15), dp(13), dp(15), dp(13));
            card.setBackground(rounded(CARD, CARD_BORDER, 16));

            TextView headline = text(item.title, 17, WHITE, true);
            headline.setLineSpacing(0, 1.08f);
            card.addView(headline);

            StringBuilder meta = new StringBuilder();
            if (item.source != null && !item.source.isEmpty()) meta.append(item.source);
            if (item.date != null && !item.date.isEmpty()) {
                if (meta.length() > 0) meta.append(" • ");
                meta.append(item.date);
            }
            if (meta.length() > 0) {
                TextView info = text(meta.toString(), 13, MUTED, false);
                info.setPadding(0, dp(9), 0, 0);
                card.addView(info);
            }
            return card;
        }
    }
}
