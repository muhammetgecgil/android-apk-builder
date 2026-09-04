package com.muhammet.habersafev5;

import android.text.Html;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class NewsRepository {
    private NewsRepository() {}

    public static LinkedHashMap<String, String> categories() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("Gündem", "gündem Türkiye son dakika");
        map.put("Türkiye", "Türkiye haberleri");
        map.put("Dünya", "dünya uluslararası haberler");
        map.put("Bilim", "bilim araştırma keşif");
        map.put("Yapay Zeka", "yapay zeka artificial intelligence AI");
        map.put("Robotik", "robotik robot humanoid otonom");
        map.put("Uzay", "uzay NASA ESA SpaceX astronomi");
        map.put("Havacılık", "havacılık uçak aviation aerospace");
        map.put("Savunma", "savunma sanayi defense");
        map.put("Savaş", "savaş çatışma güvenlik dünya");
        map.put("Arkeoloji", "arkeoloji kazı antik keşif");
        map.put("Tarih", "tarih tarihi keşif araştırma");
        map.put("Sanat", "sanat sergi müze sanatçı");
        map.put("Kültür", "kültür edebiyat sinema festival");
        map.put("Enerji", "enerji elektrik nükleer yenilenebilir");
        map.put("Otomotiv", "otomotiv otomobil araç sektörü");
        map.put("Elektrikli Araç", "elektrikli araç EV batarya şarj");
        map.put("Ekonomi", "ekonomi piyasa enflasyon finans");
        map.put("Sağlık", "sağlık tıp araştırma sağlık haberleri");
        map.put("Çevre İklim", "çevre iklim değişikliği doğa");
        map.put("Eğitim", "eğitim okul üniversite bilim eğitim");
        map.put("Siber Güvenlik", "siber güvenlik cybersecurity veri ihlali");
        map.put("Mühendislik", "mühendislik teknoloji proje engineering");
        map.put("Teknoloji", "teknoloji teknoloji şirketleri yeni ürün");
        return map;
    }

    public static String combinedQuery(Set<String> selectedNames) {
        LinkedHashMap<String, String> map = categories();
        StringBuilder out = new StringBuilder();
        for (String name : map.keySet()) {
            if (!selectedNames.contains(name)) continue;
            if (out.length() > 0) out.append(" OR ");
            out.append('(').append(map.get(name)).append(')');
        }
        return out.toString();
    }

    public static String selectionCacheKey(Set<String> selectedNames) {
        ArrayList<String> sorted = new ArrayList<>(selectedNames);
        Collections.sort(sorted);
        return "my-news:" + android.text.TextUtils.join("|", sorted);
    }

    public static List<NewsItem> fetchSelectedCategories(Set<String> selectedNames, int perCategory, int maxTotal) throws Exception {
        LinkedHashMap<String, String> map = categories();
        ArrayList<String> queries = new ArrayList<>();
        for (String name : map.keySet()) {
            if (selectedNames.contains(name)) queries.add(map.get(name));
        }
        if (queries.isEmpty()) return new ArrayList<>();

        int threads = Math.min(4, queries.size());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        ArrayList<Future<List<NewsItem>>> futures = new ArrayList<>();
        for (String q : queries) {
            futures.add(pool.submit(new Callable<List<NewsItem>>() {
                @Override public List<NewsItem> call() {
                    try {
                        return fetchGoogleNews(q, perCategory);
                    } catch (Exception ignored) {
                        return new ArrayList<>();
                    }
                }
            }));
        }

        ArrayList<NewsItem> merged = new ArrayList<>();
        for (Future<List<NewsItem>> f : futures) {
            try { merged.addAll(f.get()); } catch (Exception ignored) {}
        }
        pool.shutdownNow();

        if (merged.isEmpty()) throw new Exception("Seçili alanlardan haber alınamadı");

        merged.sort((a, b) -> Long.compare(b.publishedAt, a.publishedAt));
        ArrayList<NewsItem> deduped = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (NewsItem item : merged) {
            String key = (item.title + "|" + item.source).toLowerCase(Locale.ROOT);
            if (seen.add(key)) deduped.add(item);
            if (deduped.size() >= maxTotal) break;
        }
        return deduped;
    }

    public static List<NewsItem> fetchGoogleNews(String query, int limit) throws Exception {
        String q = query + " when:7d";
        String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8.name());
        String urlText = "https://news.google.com/rss/search?q=" + encoded + "&hl=tr&gl=TR&ceid=TR:tr";

        HttpURLConnection conn = (HttpURLConnection) new URL(urlText).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(14000);
        conn.setRequestProperty("User-Agent", "HaberSAFE/6.0.0 Android");
        conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml");
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            conn.disconnect();
            throw new Exception("Sunucu yanıtı " + code);
        }

        ArrayList<NewsItem> out = new ArrayList<>();
        HashSet<String> dedupe = new HashSet<>();
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
            while (event != XmlPullParser.END_DOCUMENT && out.size() < limit) {
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
                    String directLink = link == null ? "" : link.trim();
                    if (!directTitle.isEmpty() && directLink.startsWith("http")) {
                        String key = (directTitle + "|" + directSource).toLowerCase(Locale.ROOT);
                        if (dedupe.add(key)) {
                            ParsedDate parsed = parseDate(pubDate);
                            out.add(new NewsItem(directTitle, directLink, directSource, parsed.text, parsed.epoch));
                        }
                    }
                }
                event = parser.next();
            }
        } finally {
            conn.disconnect();
        }
        return out;
    }

    private static String readText(XmlPullParser parser) {
        try {
            return parser.nextText();
        } catch (Exception e) {
            return "";
        }
    }

    private static String clean(String value) {
        if (value == null) return "";
        String decoded = Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString();
        return decoded.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static ParsedDate parseDate(String rssDate) {
        if (rssDate == null || rssDate.trim().isEmpty()) return new ParsedDate("", 0L);
        String[] patterns = {"EEE, dd MMM yyyy HH:mm:ss z", "EEE, dd MMM yyyy HH:mm:ss Z"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(pattern, Locale.US);
                Date date = in.parse(rssDate.trim());
                if (date != null) {
                    SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy • HH:mm", new Locale("tr", "TR"));
                    return new ParsedDate(out.format(date), date.getTime());
                }
            } catch (Exception ignored) {}
        }
        return new ParsedDate(rssDate.trim(), 0L);
    }

    private static final class ParsedDate {
        final String text;
        final long epoch;
        ParsedDate(String text, long epoch) { this.text = text; this.epoch = epoch; }
    }

    public static final class NewsItem {
        public final String title;
        public final String link;
        public final String source;
        public final String date;
        public final long publishedAt;

        public NewsItem(String title, String link, String source, String date) {
            this(title, link, source, date, 0L);
        }

        public NewsItem(String title, String link, String source, String date, long publishedAt) {
            this.title = title == null ? "" : title;
            this.link = link == null ? "" : link;
            this.source = source == null ? "" : source;
            this.date = date == null ? "" : date;
            this.publishedAt = publishedAt;
        }

        @Override public String toString() { return title; }
    }
}
