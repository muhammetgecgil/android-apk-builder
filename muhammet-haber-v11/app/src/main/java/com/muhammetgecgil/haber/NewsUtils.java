package com.muhammetgecgil.haber;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NewsUtils {
    private NewsUtils() {}

    public static class Article {
        public String title = "";
        public String url = "";
        public String source = "";
        public String date = "";
    }

    public static List<Article> fetch(String query, int max) throws Exception {
        Exception first = null;
        try {
            String q = URLEncoder.encode(query + " when:7d", "UTF-8");
            String url = "https://news.google.com/rss/search?q=" + q + "&hl=tr&gl=TR&ceid=TR:tr";
            List<Article> r = parseRss(url, max);
            if (!r.isEmpty()) return r;
        } catch (Exception e) { first = e; }

        try {
            String q = URLEncoder.encode(query, "UTF-8");
            String url = "https://www.bing.com/news/search?q=" + q + "&format=rss&mkt=tr-TR";
            List<Article> r = parseRss(url, max);
            if (!r.isEmpty()) return r;
        } catch (Exception e) {
            if (first == null) first = e;
        }

        if (first != null) throw first;
        return new ArrayList<>();
    }

    private static List<Article> parseRss(String address, int max) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(15000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) MuhammetHaber/1.1");
        c.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*");
        int code = c.getResponseCode();
        if (code < 200 || code >= 400) throw new Exception("Haber servisi HTTP " + code);

        List<Article> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (InputStream in = c.getInputStream()) {
            XmlPullParser p = Xml.newPullParser();
            p.setInput(in, "UTF-8");
            int event = p.getEventType();
            Article cur = null;
            String tag = "";
            while (event != XmlPullParser.END_DOCUMENT && out.size() < max) {
                if (event == XmlPullParser.START_TAG) {
                    tag = p.getName();
                    if ("item".equalsIgnoreCase(tag)) cur = new Article();
                } else if (event == XmlPullParser.TEXT && cur != null) {
                    String text = p.getText() == null ? "" : p.getText().trim();
                    if (!text.isEmpty()) {
                        if ("title".equalsIgnoreCase(tag)) cur.title += text;
                        else if ("link".equalsIgnoreCase(tag)) cur.url += text;
                        else if ("pubDate".equalsIgnoreCase(tag)) cur.date += text;
                        else if ("source".equalsIgnoreCase(tag)) cur.source += text;
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    String end = p.getName();
                    if ("item".equalsIgnoreCase(end) && cur != null) {
                        cur.title = clean(cur.title);
                        cur.url = clean(cur.url);
                        cur.source = clean(cur.source);
                        cur.date = clean(cur.date);
                        if (!cur.title.isEmpty() && !cur.url.isEmpty()) {
                            String key = cur.title.toLowerCase(new Locale("tr", "TR")).replaceAll("[^a-z0-9çğıöşü ]", "");
                            if (seen.add(key)) out.add(cur);
                        }
                        cur = null;
                    }
                    tag = "";
                }
                event = p.next();
            }
        } finally { c.disconnect(); }
        return out;
    }

    public static String clean(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }
}
