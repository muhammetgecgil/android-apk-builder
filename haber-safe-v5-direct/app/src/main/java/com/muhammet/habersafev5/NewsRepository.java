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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    public static List<NewsItem> fetchGoogleNews(String query, int limit) throws Exception {
        String q = query + " when:7d";
        String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8.name());
        String urlText = "https://news.google.com/rss/search?q=" + encoded + "&hl=tr&gl=TR&ceid=TR:tr";

        HttpURLConnection conn = (HttpURLConnection) new URL(urlText).openConnection();
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(16000);
        conn.setRequestProperty("User-Agent", "HaberSAFEV5/5.1.0 Android");
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
        return decoded.replace('\u00A0', ' ').trim();
    }

    private static String formatDate(String rssDate) {
        if (rssDate == null || rssDate.trim().isEmpty()) return "";
        String[] patterns = {"EEE, dd MMM yyyy HH:mm:ss z", "EEE, dd MMM yyyy HH:mm:ss Z"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(pattern, Locale.US);
                Date date = in.parse(rssDate.trim());
                if (date != null) {
                    SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy • HH:mm", new Locale("tr", "TR"));
                    return out.format(date);
                }
            } catch (Exception ignored) {}
        }
        return rssDate.trim();
    }

    public static final class NewsItem {
        public final String title;
        public final String link;
        public final String source;
        public final String date;

        public NewsItem(String title, String link, String source, String date) {
            this.title = title;
            this.link = link;
            this.source = source;
            this.date = date;
        }

        @Override public String toString() { return title; }
    }
}
