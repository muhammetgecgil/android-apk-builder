package com.muhammet.habersafev5;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
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

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final LinkedHashMap<String, String> categories = new LinkedHashMap<>();
    private final ArrayList<NewsItem> items = new ArrayList<>();

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
        initCategories();
        showHome();
    }

    private void initCategories() {
        categories.put("Gündem", "gündem Türkiye son dakika");
        categories.put("Türkiye", "Türkiye haberleri");
        categories.put("Dünya", "dünya uluslararası haberler");
        categories.put("Bilim", "bilim araştırma keşif");
        categories.put("Yapay Zeka", "yapay zeka artificial intelligence AI");
        categories.put("Robotik", "robotik robot humanoid otonom");
        categories.put("Uzay", "uzay NASA ESA SpaceX astronomi");
        categories.put("Havacılık", "havacılık uçak aviation aerospace");
        categories.put("Savunma", "savunma sanayi defense");
        categories.put("Savaş", "savaş çatışma güvenlik dünya");
        categories.put("Arkeoloji", "arkeoloji kazı antik keşif");
        categories.put("Tarih", "tarih tarihi keşif araştırma");
        categories.put("Sanat", "sanat sergi müze sanatçı");
        categories.put("Kültür", "kültür edebiyat sinema festival");
        categories.put("Enerji", "enerji elektrik nükleer yenilenebilir");
        categories.put("Otomotiv", "otomotiv otomobil araç sektörü");
        categories.put("Elektrikli Araç", "elektrikli araç EV batarya şarj");
        categories.put("Ekonomi", "ekonomi piyasa enflasyon finans");
        categories.put("Sağlık", "sağlık tıp araştırma sağlık haberleri");
        categories.put("Çevre İklim", "çevre iklim değişikliği doğa");
        categories.put("Eğitim", "eğitim okul üniversite bilim eğitim");
        categories.put("Siber Güvenlik", "siber güvenlik cybersecurity veri ihlali");
        categories.put("Mühendislik", "mühendislik teknoloji proje engineering");
        categories.put("Teknoloji", "teknoloji teknoloji şirketleri yeni ürün");
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

        TextView title = text("Haber SAFE V5", 28, WHITE, true);
        title.setPadding(dp(4), 0, 0, dp(18));
        root.addView(title);

        LinearLayout intro = vertical(Color.rgb(20, 32, 59));
        intro.setPadding(dp(18), dp(14), dp(18), dp(14));
        intro.setBackground(rounded(Color.rgb(20, 32, 59), CARD_BORDER, 18));
        TextView safe = text("DOĞRUDAN HABER MODU", 18, GREEN, true);
        TextView help = text("Bir kategoriye bas veya konu ara. Uygulama haber başlıklarını RSS kaynağından olduğu gibi getirir; yorumlama, özetleme ve yeniden yazma yapmaz.", 16, MUTED, false);
        help.setPadding(0, dp(10), 0, 0);
        intro.addView(safe);
        intro.addView(help);
        root.addView(intro, matchWrap(dp(14)));

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
        adapter = new NewsAdapter(this, items);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            NewsItem item = items.get(position);
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
                List<NewsItem> downloaded = fetchGoogleNews(query);
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

    private List<NewsItem> fetchGoogleNews(String query) throws Exception {
        String q = query + " when:7d";
        String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8.name());
        String urlText = "https://news.google.com/rss/search?q=" + encoded + "&hl=tr&gl=TR&ceid=TR:tr";

        HttpURLConnection conn = (HttpURLConnection) new URL(urlText).openConnection();
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(16000);
        conn.setRequestProperty("User-Agent", "HaberSAFEV5/5.0.1 Android");
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            conn.disconnect();
            throw new Exception("Sunucu yanıtı " + code);
        }

        ArrayList<NewsItem> out = new ArrayList<>();
        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(in, "UTF-8");

            boolean insideItem = false;
            String itemTitle = "";
            String link = "";
            String pubDate = "";
            String source = "";

            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT && out.size() < 50) {
                if (event == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    if ("item".equalsIgnoreCase(tag)) {
                        insideItem = true;
                        itemTitle = "";
                        link = "";
                        pubDate = "";
                        source = "";
                    } else if (insideItem) {
                        if ("title".equalsIgnoreCase(tag)) itemTitle = readText(parser);
                        else if ("link".equalsIgnoreCase(tag)) link = readText(parser);
                        else if ("pubDate".equalsIgnoreCase(tag)) pubDate = readText(parser);
                        else if ("source".equalsIgnoreCase(tag)) source = readText(parser);
                    }
                } else if (event == XmlPullParser.END_TAG && "item".equalsIgnoreCase(parser.getName())) {
                    insideItem = false;
                    String directTitle = clean(itemTitle);
                    String directSource = clean(source);
                    if (!directTitle.isEmpty() && link != null && link.startsWith("http")) {
                        out.add(new NewsItem(directTitle, link.trim(), directSource, formatDate(pubDate)));
                    }
                }
                event = parser.next();
            }
        } finally {
            conn.disconnect();
        }
        return out;
    }

    private String readText(XmlPullParser parser) {
        try {
            return parser.nextText();
        } catch (Exception e) {
            return "";
        }
    }

    private String clean(String value) {
        if (value == null) return "";
        String decoded = Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString();
        return decoded.replace('\u00A0', ' ').trim();
    }

    private String formatDate(String rssDate) {
        if (rssDate == null || rssDate.trim().isEmpty()) return "";
        String[] patterns = {"EEE, dd MMM yyyy HH:mm:ss z", "EEE, dd MMM yyyy HH:mm:ss Z"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(pattern, Locale.US);
                Date date = in.parse(rssDate.trim());
                if (date != null) {
                    SimpleDateFormat out = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("tr", "TR"));
                    out.setTimeZone(TimeZone.getDefault());
                    return out.format(date);
                }
            } catch (Exception ignored) {
            }
        }
        return rssDate;
    }

    private boolean hasNetwork() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            Network active = cm.getActiveNetwork();
            return active != null;
        } catch (Exception e) {
            return true;
        }
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) return "bağlantı hatası";
        return message.length() > 100 ? message.substring(0, 100) : message;
    }

    @Override
    public void onBackPressed() {
        if (!currentTitle.isEmpty()) showHome();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private LinearLayout vertical(int color) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(color);
        return layout;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(WHITE);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(rounded(CARD, CARD_BORDER, 14));
        return button;
    }

    private Button categoryButton(String label) {
        Button button = button(label);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setTextSize(17);
        button.setPadding(dp(16), 0, dp(12), 0);
        return button;
    }

    private LinearLayout.LayoutParams weightedButton(int leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(64), 1f);
        lp.setMargins(leftMargin, 0, 0, 0);
        return lp;
    }

    private LinearLayout.LayoutParams matchWrap(int bottomMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, bottomMargin);
        return lp;
    }

    private android.graphics.drawable.GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class NewsItem {
        final String title;
        final String link;
        final String source;
        final String date;

        NewsItem(String title, String link, String source, String date) {
            this.title = title;
            this.link = link;
            this.source = source;
            this.date = date;
        }
    }

    private class NewsAdapter extends ArrayAdapter<NewsItem> {
        NewsAdapter(Context context, List<NewsItem> data) {
            super(context, android.R.layout.simple_list_item_1, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            if (convertView == null) {
                LinearLayout card = vertical(CARD);
                card.setPadding(dp(16), dp(14), dp(16), dp(14));
                card.setBackground(rounded(CARD, CARD_BORDER, 15));

                TextView titleView = text("", 18, WHITE, true);
                titleView.setLineSpacing(0, 1.08f);
                card.addView(titleView);

                TextView metaView = text("", 14, GREEN, true);
                metaView.setPadding(0, dp(9), 0, 0);
                card.addView(metaView);

                TextView hint = text("Özgün kaynağı açmak için dokun", 13, MUTED, false);
                hint.setPadding(0, dp(5), 0, 0);
                card.addView(hint);

                holder = new Holder(titleView, metaView);
                card.setTag(holder);
                convertView = card;
            } else {
                holder = (Holder) convertView.getTag();
            }

            NewsItem item = getItem(position);
            if (item != null) {
                holder.title.setText(item.title);
                String meta = item.source;
                if (!item.date.isEmpty()) meta = meta.isEmpty() ? item.date : meta + "  •  " + item.date;
                holder.meta.setText(meta.isEmpty() ? "Güncel haber" : meta);
            }
            return convertView;
        }

        class Holder {
            final TextView title;
            final TextView meta;
            Holder(TextView title, TextView meta) {
                this.title = title;
                this.meta = meta;
            }
        }
    }
}
