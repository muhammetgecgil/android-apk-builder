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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 17, 38);
    private static final int TOP = Color.rgb(15, 25, 47);
    private static final int CARD = Color.rgb(25, 43, 76);
    private static final int CARD_READ = Color.rgb(20, 34, 61);
    private static final int CARD_BORDER = Color.rgb(52, 78, 120);
    private static final int WHITE = Color.rgb(248, 250, 255);
    private static final int MUTED = Color.rgb(178, 192, 218);
    private static final int GREEN = Color.rgb(82, 214, 163);
    private static final int ORANGE = Color.rgb(244, 178, 74);
    private static final int BLUE = Color.rgb(96, 165, 250);
    private static final int REQ_NOTIFICATIONS = 77;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final LinkedHashMap<String, String> categories = NewsRepository.categories();
    private final ArrayList<NewsRepository.NewsItem> items = new ArrayList<>();
    private final HashSet<String> currentSelected = new HashSet<>();

    private EditText search;
    private ProgressBar progress;
    private TextView status;
    private NewsAdapter adapter;
    private String currentTitle = "";
    private String currentQuery = "";
    private String currentCacheKey = "";
    private boolean savedMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(TOP);
        getWindow().setNavigationBarColor(Color.BLACK);
        showHome();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("open_my_news", false)) {
            main.postDelayed(this::loadMyNews, 150);
        }
    }

    private void showHome() {
        savedMode = false;
        currentTitle = "";
        currentQuery = "";
        currentCacheKey = "";
        currentSelected.clear();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = vertical(BG);
        root.setPadding(dp(18), dp(20), dp(18), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Haber SAFE", 30, WHITE, true);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView version = text("V6.0", 13, GREEN, true);
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(12), dp(7), dp(12), dp(7));
        version.setBackground(rounded(Color.rgb(18, 54, 55), Color.rgb(46, 109, 96), 18));
        titleRow.addView(version);
        root.addView(titleRow);

        TextView subtitle = text("Doğrudan kaynak • kişisel akış • çevrimdışı önbellek", 14, MUTED, false);
        subtitle.setPadding(dp(2), dp(5), 0, dp(16));
        root.addView(subtitle);

        LinearLayout intro = vertical(Color.rgb(18, 32, 60));
        intro.setPadding(dp(16), dp(14), dp(16), dp(14));
        intro.setBackground(rounded(Color.rgb(18, 32, 60), CARD_BORDER, 18));
        intro.addView(text("YORUMSUZ HABER", 17, GREEN, true));
        TextView help = text("Başlık, kaynak ve tarih RSS akışından doğrudan gösterilir. Yapay zekâ özeti, yorumlama, haber birleştirme veya yeniden yazma yoktur.", 15, MUTED, false);
        help.setPadding(0, dp(8), 0, 0);
        intro.addView(help);
        root.addView(intro, matchWrap(dp(14)));

        LinearLayout personal = vertical(Color.rgb(14, 29, 53));
        personal.setPadding(dp(14), dp(14), dp(14), dp(14));
        personal.setBackground(rounded(Color.rgb(14, 29, 53), CARD_BORDER, 18));
        personal.addView(text("KİŞİSEL MERKEZ", 16, WHITE, true));

        Button myNews = button("BENİM HABERLERİM");
        myNews.setOnClickListener(v -> loadMyNews());
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        p1.setMargins(0, dp(12), 0, dp(8));
        personal.addView(myNews, p1);

        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        Button saved = button("KAYDEDİLENLER");
        saved.setTextSize(13);
        saved.setOnClickListener(v -> loadSaved());
        quickRow.addView(saved, weightedButton(0));
        Button reminder = button("BİLDİRİM / ALANLAR");
        reminder.setTextSize(13);
        reminder.setOnClickListener(v -> showReminderDialog());
        quickRow.addView(reminder, weightedButton(dp(8)));
        personal.addView(quickRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        TextView reminderState = text(reminderStateText(), 14, ORANGE, false);
        reminderState.setPadding(dp(2), dp(10), dp(2), 0);
        personal.addView(reminderState);
        root.addView(personal, matchWrap(dp(16)));

        root.addView(sectionTitle("HABER ARA"));
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        search = new EditText(this);
        search.setTextColor(WHITE);
        search.setHintTextColor(Color.rgb(130, 148, 180));
        search.setHint("Konu ara: COMAC, Artemis, batarya...");
        search.setTextSize(16);
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setPadding(dp(14), dp(10), dp(14), dp(10));
        search.setBackground(rounded(Color.rgb(16, 29, 54), CARD_BORDER, 14));
        searchRow.addView(search, new LinearLayout.LayoutParams(0, dp(54), 1f));
        Button searchButton = button("ARA");
        LinearLayout.LayoutParams searchButtonLp = new LinearLayout.LayoutParams(dp(86), dp(54));
        searchButtonLp.setMargins(dp(8), 0, 0, 0);
        searchRow.addView(searchButton, searchButtonLp);
        root.addView(searchRow, matchWrap(dp(14)));

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

        root.addView(sectionTitle("KATEGORİLER"));
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
                row.addView(b2, weightedButton(dp(8)));
            } else {
                View spacer = new View(this);
                row.addView(spacer, weightedButton(dp(8)));
            }
            root.addView(row, matchWrap(dp(8)));
        }

        Button settings = button("AYARLAR • GİZLİLİK • BAKIM");
        settings.setOnClickListener(v -> showSettings());
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        slp.setMargins(0, dp(18), 0, 0);
        root.addView(settings, slp);

        TextView privacy = text("Hesap gerekmez • Analitik yok • Seçimler ve kayıtlar yalnızca cihazda tutulur", 13, MUTED, false);
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(dp(8), dp(12), dp(8), 0);
        root.addView(privacy);

        setContentView(scroll);
    }

    private TextView sectionTitle(String s) {
        TextView t = text(s, 14, MUTED, true);
        t.setPadding(dp(2), dp(6), 0, dp(9));
        return t;
    }

    private String reminderStateText() {
        SharedPreferences prefs = getSharedPreferences(NewsReminderReceiver.PREFS, MODE_PRIVATE);
        Set<String> selected = prefs.getStringSet(NewsReminderReceiver.KEY_SELECTED, new HashSet<>());
        boolean enabled = prefs.getBoolean(NewsReminderReceiver.KEY_ENABLED, false);
        if (!enabled) {
            return selected.isEmpty() ? "Günlük bildirim kapalı • İlgi alanı seçilmedi" : "Günlük bildirim kapalı • " + selected.size() + " ilgi alanı kayıtlı";
        }
        int hour = prefs.getInt(NewsReminderReceiver.KEY_HOUR, 8);
        int minute = prefs.getInt(NewsReminderReceiver.KEY_MINUTE, 0);
        return String.format(Locale.getDefault(), "Her gün %02d:%02d • %d alan seçili", hour, minute, selected.size());
    }

    private void loadMyNews() {
        SharedPreferences prefs = getSharedPreferences(NewsReminderReceiver.PREFS, MODE_PRIVATE);
        Set<String> selected = new HashSet<>(prefs.getStringSet(NewsReminderReceiver.KEY_SELECTED, new HashSet<>()));
        if (selected.isEmpty()) {
            Toast.makeText(this, "Önce ilgi alanlarını seç.", Toast.LENGTH_LONG).show();
            showReminderDialog();
            return;
        }
        savedMode = false;
        currentSelected.clear();
        currentSelected.addAll(selected);
        currentQuery = "";
        currentTitle = "Benim Haberlerim";
        currentCacheKey = NewsRepository.selectionCacheKey(currentSelected);
        items.clear();
        showResults(currentTitle);
        fetchIntoResults();
    }

    private void loadNews(String query, String title) {
        savedMode = false;
        currentSelected.clear();
        currentQuery = query;
        currentTitle = title;
        currentCacheKey = "query:" + query;
        items.clear();
        showResults(title);
        fetchIntoResults();
    }

    private void loadSaved() {
        savedMode = true;
        currentSelected.clear();
        currentQuery = "";
        currentCacheKey = "";
        currentTitle = "Kaydedilenler";
        items.clear();
        items.addAll(AppStore.getFavorites(this));
        showResults(currentTitle);
        progress.setVisibility(View.GONE);
        status.setText(items.isEmpty() ? "Henüz kaydedilmiş haber yok. Bir habere uzun basıp Kaydet'i seçebilirsin." : items.size() + " haber cihazda kayıtlı.");
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
        heading.setPadding(dp(10), 0, dp(8), 0);
        top.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button refresh = button(savedMode ? "Yenile" : "Yenile");
        refresh.setTextSize(13);
        refresh.setOnClickListener(v -> {
            if (savedMode) loadSaved(); else fetchIntoResults();
        });
        top.addView(refresh, new LinearLayout.LayoutParams(dp(78), dp(52)));
        screen.addView(top);

        progress = new ProgressBar(this);
        screen.addView(progress, matchWrap(dp(5)));
        status = text(savedMode ? "Kayıtlar yükleniyor…" : "Haberler doğrudan kaynaktan alınıyor…", 14, MUTED, false);
        status.setPadding(dp(4), dp(6), dp(4), dp(10));
        screen.addView(status);

        ListView listView = new ListView(this);
        listView.setDividerHeight(dp(8));
        listView.setDivider(new android.graphics.drawable.ColorDrawable(BG));
        listView.setBackgroundColor(BG);
        listView.setCacheColorHint(BG);
        adapter = new NewsAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> openNews(items.get(position)));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showItemActions(items.get(position));
            return true;
        });
        screen.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(screen);
    }

    private void fetchIntoResults() {
        if (savedMode) return;
        if (currentCacheKey.isEmpty()) return;
        String expectedKey = currentCacheKey;
        progress.setVisibility(View.VISIBLE);
        status.setText("Haberler doğrudan kaynaktan alınıyor…");

        if (!hasNetwork()) {
            showCachedOrError(expectedKey, "İnternet yok");
            return;
        }

        HashSet<String> selectedSnapshot = new HashSet<>(currentSelected);
        String querySnapshot = currentQuery;
        io.execute(() -> {
            try {
                List<NewsRepository.NewsItem> downloaded = selectedSnapshot.isEmpty()
                        ? NewsRepository.fetchGoogleNews(querySnapshot, 60)
                        : NewsRepository.fetchSelectedCategories(selectedSnapshot, 8, 60);
                AppStore.saveCache(this, expectedKey, downloaded);
                main.post(() -> {
                    if (!expectedKey.equals(currentCacheKey) || savedMode) return;
                    items.clear();
                    items.addAll(downloaded);
                    adapter.notifyDataSetChanged();
                    progress.setVisibility(View.GONE);
                    status.setText(downloaded.isEmpty()
                            ? "Bu başlıkta haber bulunamadı."
                            : downloaded.size() + " haber • " + nowText() + " • Başlıklar yorumlanmadan gösteriliyor");
                });
            } catch (Exception e) {
                main.post(() -> {
                    if (!expectedKey.equals(currentCacheKey) || savedMode) return;
                    showCachedOrError(expectedKey, "Canlı veri alınamadı: " + safeMessage(e));
                });
            }
        });
    }

    private void showCachedOrError(String key, String reason) {
        List<NewsRepository.NewsItem> cached = AppStore.loadCache(this, key);
        progress.setVisibility(View.GONE);
        items.clear();
        items.addAll(cached);
        if (adapter != null) adapter.notifyDataSetChanged();
        if (cached.isEmpty()) {
            status.setText(reason + ". Önbellekte de haber yok. Bağlantıyı kontrol edip Yenile'ye bas.");
        } else {
            long t = AppStore.cacheTime(this, key);
            status.setText(reason + " • " + cached.size() + " haber çevrimdışı önbellekten" + (t > 0 ? " • " + formatTime(t) : ""));
        }
    }

    private void openNews(NewsRepository.NewsItem item) {
        AppStore.setRead(this, item.link, true);
        if (adapter != null) adapter.notifyDataSetChanged();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.link)));
        } catch (Exception e) {
            Toast.makeText(this, "Haber bağlantısı açılamadı.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showItemActions(NewsRepository.NewsItem item) {
        boolean favorite = AppStore.isFavorite(this, item.link);
        boolean read = AppStore.isRead(this, item.link);
        String[] actions = {
                favorite ? "Kaydı kaldır" : "Haberi kaydet",
                "Paylaş",
                read ? "Okunmadı işaretle" : "Okundu işaretle",
                "Haberi aç"
        };
        new AlertDialog.Builder(this)
                .setTitle(item.source.isEmpty() ? "Haber işlemleri" : item.source)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        boolean saved = AppStore.toggleFavorite(this, item);
                        Toast.makeText(this, saved ? "Haber kaydedildi." : "Kayıt kaldırıldı.", Toast.LENGTH_SHORT).show();
                        if (savedMode) loadSaved(); else if (adapter != null) adapter.notifyDataSetChanged();
                    } else if (which == 1) {
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("text/plain");
                        share.putExtra(Intent.EXTRA_TEXT, item.title + "\n" + item.link);
                        startActivity(Intent.createChooser(share, "Haberi paylaş"));
                    } else if (which == 2) {
                        AppStore.setRead(this, item.link, !read);
                        if (adapter != null) adapter.notifyDataSetChanged();
                    } else if (which == 3) {
                        openNews(item);
                    }
                })
                .show();
    }

    private void showReminderDialog() {
        SharedPreferences prefs = getSharedPreferences(NewsReminderReceiver.PREFS, MODE_PRIVATE);
        boolean enabledSaved = prefs.getBoolean(NewsReminderReceiver.KEY_ENABLED, false);
        boolean onlyNewSaved = prefs.getBoolean(NewsReminderReceiver.KEY_ONLY_NEW, false);
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

        CheckBox onlyNew = new CheckBox(this);
        onlyNew.setText("Sadece yeni haber varsa bildir");
        onlyNew.setTextSize(15);
        onlyNew.setChecked(onlyNewSaved);
        box.addView(onlyNew);

        Button timeButton = new Button(this);
        timeButton.setAllCaps(false);
        timeButton.setText(String.format(Locale.getDefault(), "Bildirim saati: %02d:%02d", time[0], time[1]));
        timeButton.setOnClickListener(v -> new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    time[0] = hourOfDay;
                    time[1] = minute;
                    timeButton.setText(String.format(Locale.getDefault(), "Bildirim saati: %02d:%02d", hourOfDay, minute));
                }, time[0], time[1], true).show());
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        timeLp.setMargins(0, dp(8), 0, dp(10));
        box.addView(timeButton, timeLp);

        TextView choose = text("Benim Haberlerim ve günlük bildirim için alanları seç:", 15, Color.rgb(30, 40, 58), true);
        choose.setPadding(0, dp(4), 0, dp(6));
        box.addView(choose);

        LinkedHashMap<String, CheckBox> checks = new LinkedHashMap<>();
        for (String name : categories.keySet()) {
            CheckBox cb = new CheckBox(this);
            cb.setText(name);
            cb.setTextSize(16);
            cb.setChecked(selectedSaved.contains(name));
            checks.put(name, cb);
            box.addView(cb);
        }

        LinearLayout selectionRow = new LinearLayout(this);
        selectionRow.setOrientation(LinearLayout.HORIZONTAL);
        Button all = new Button(this);
        all.setText("Tümünü seç");
        all.setAllCaps(false);
        all.setOnClickListener(v -> { for (CheckBox cb : checks.values()) cb.setChecked(true); });
        selectionRow.addView(all, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button none = new Button(this);
        none.setText("Temizle");
        none.setAllCaps(false);
        none.setOnClickListener(v -> { for (CheckBox cb : checks.values()) cb.setChecked(false); });
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        nlp.setMargins(dp(8), 0, 0, 0);
        selectionRow.addView(none, nlp);
        box.addView(selectionRow, matchWrap(dp(8)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("İlgi Alanları ve Günlük Haber")
                .setView(scroller)
                .setNegativeButton("İptal", null)
                .setPositiveButton("Kaydet", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            HashSet<String> selected = new HashSet<>();
            for (Map.Entry<String, CheckBox> e : checks.entrySet()) if (e.getValue().isChecked()) selected.add(e.getKey());
            if (enabled.isChecked() && selected.isEmpty()) {
                Toast.makeText(this, "Bildirim için en az bir alan seç.", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit()
                    .putBoolean(NewsReminderReceiver.KEY_ENABLED, enabled.isChecked())
                    .putBoolean(NewsReminderReceiver.KEY_ONLY_NEW, onlyNew.isChecked())
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
                Toast.makeText(this, "İlgi alanları kaydedildi; günlük bildirim kapalı.", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
            showHome();
        }));
        dialog.show();
    }

    private void showSettings() {
        String[] options = {
                "Bildirim ve ilgi alanları",
                "Bildirim testi gönder",
                "Önbelleği temizle",
                "Okundu işaretlerini sıfırla",
                "Hakkında ve gizlilik"
        };
        new AlertDialog.Builder(this)
                .setTitle("Haber SAFE Ayarları")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        showReminderDialog();
                    } else if (which == 1) {
                        SharedPreferences p = getSharedPreferences(NewsReminderReceiver.PREFS, MODE_PRIVATE);
                        Set<String> selected = p.getStringSet(NewsReminderReceiver.KEY_SELECTED, new HashSet<>());
                        if (selected.isEmpty()) {
                            Toast.makeText(this, "Önce ilgi alanlarını seç.", Toast.LENGTH_LONG).show();
                            showReminderDialog();
                        } else {
                            requestNotificationPermissionIfNeeded();
                            NewsReminderReceiver.sendNow(this);
                            Toast.makeText(this, "Test bildirimi hazırlanıyor…", Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 2) {
                        AppStore.clearCache(this);
                        Toast.makeText(this, "Haber önbelleği temizlendi.", Toast.LENGTH_SHORT).show();
                    } else if (which == 3) {
                        AppStore.clearRead(this);
                        Toast.makeText(this, "Okundu işaretleri sıfırlandı.", Toast.LENGTH_SHORT).show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Haber SAFE 6.0")
                                .setMessage("Doğrudan haber okuyucu ve kişisel günlük haber uygulaması. Başlıklar ve kaynak bilgileri RSS akışından gelir; yapay zekâ ile yorumlanmaz veya yeniden yazılmaz.\n\nKişisel hesap, reklam SDK'sı veya analitik bulunmaz. İlgi alanları, okundu işaretleri, önbellek ve kaydedilen haberler cihazda tutulur.\n\nBir habere dokun: aç. Uzun bas: kaydet, paylaş veya okundu durumunu değiştir.")
                                .setPositiveButton("Tamam", null)
                                .show();
                    }
                }).show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bildirim izni verilmedi. Haberleri uygulama içinden okumaya devam edebilirsin.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasNetwork() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network active = cm.getActiveNetwork();
            return active != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String nowText() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }

    private String formatTime(long millis) {
        return new SimpleDateFormat("dd MMM HH:mm", new Locale("tr", "TR")).format(new Date(millis));
    }

    private String safeMessage(Exception e) {
        String s = e == null ? null : e.getMessage();
        return s == null || s.trim().isEmpty() ? "bağlantı hatası" : s.trim();
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
        t.setLineSpacing(0f, 1.08f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(WHITE);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setPadding(dp(8), dp(5), dp(8), dp(5));
        b.setBackground(rounded(Color.rgb(29, 55, 91), Color.rgb(55, 88, 136), 14));
        return b;
    }

    private Button categoryButton(String value) {
        Button b = button(value);
        b.setTextSize(14);
        b.setMinHeight(dp(52));
        b.setBackground(rounded(Color.rgb(24, 43, 76), CARD_BORDER, 14));
        return b;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private LinearLayout.LayoutParams matchWrap(int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, topMargin, 0, 0);
        return lp;
    }

    private LinearLayout.LayoutParams weightedButton(int leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(54), 1f);
        lp.setMargins(leftMargin, 0, 0, 0);
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class NewsAdapter extends BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            NewsRepository.NewsItem item = items.get(position);
            boolean read = AppStore.isRead(MainActivity.this, item.link);
            boolean favorite = AppStore.isFavorite(MainActivity.this, item.link);

            LinearLayout card = vertical(read ? CARD_READ : CARD);
            card.setPadding(dp(15), dp(13), dp(15), dp(13));
            card.setBackground(rounded(read ? CARD_READ : CARD, CARD_BORDER, 16));

            TextView headline = text(item.title, 17, read ? MUTED : WHITE, true);
            headline.setMaxLines(5);
            card.addView(headline);

            StringBuilder meta = new StringBuilder();
            if (!item.source.isEmpty()) meta.append(item.source);
            if (!item.date.isEmpty()) {
                if (meta.length() > 0) meta.append(" • ");
                meta.append(item.date);
            }
            if (meta.length() > 0) {
                TextView source = text(meta.toString(), 13, MUTED, false);
                source.setPadding(0, dp(8), 0, 0);
                card.addView(source);
            }

            String flags = favorite && read ? "★ KAYITLI  •  OKUNDU" : favorite ? "★ KAYITLI" : read ? "OKUNDU" : "YENİ / OKUNMADI";
            TextView state = text(flags, 12, favorite ? ORANGE : (read ? MUTED : BLUE), true);
            state.setPadding(0, dp(8), 0, 0);
            card.addView(state);
            return card;
        }
    }
}
