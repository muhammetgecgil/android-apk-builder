package com.mg.trainingassistant;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

public final class CaptureStore {
    private static final String PREFS = "training_assistant";
    private static final String KEY = "captured_training_text";
    private CaptureStore() {}

    public static synchronized void append(Context c, String source, String text) {
        if (text == null) return;
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() < 8) return;
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String old = p.getString(KEY, "");
        String probe = text.length() > 180 ? text.substring(0, 180) : text;
        if (old.contains(probe)) return;
        long sec = System.currentTimeMillis() / 1000L;
        String line = "\n[" + source + " @" + sec + "] " + text;
        String merged = old + line;
        if (merged.length() > 450000) merged = merged.substring(merged.length() - 450000);
        p.edit().putString(KEY, merged).apply();
    }

    public static String get(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "");
    }

    public static void clear(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply();
    }

    public static String summarize(Context c, int pages) {
        String raw = get(c).replaceAll("\\[[^\\]]+\\]", " ").replaceAll("\\s+", " ").trim();
        if (raw.isEmpty()) return "Henüz eğitim içeriği toplanmadı.";
        String[] parts = raw.split("(?<=[.!?])\\s+");
        Map<String,Integer> freq = new HashMap<>();
        for (String s : parts) for (String w : words(s)) if (w.length() > 4) freq.put(w, freq.getOrDefault(w,0)+1);
        List<Scored> scored = new ArrayList<>();
        for (int i=0;i<parts.length;i++) {
            String s=parts[i].trim(); if (s.length()<25) continue;
            double score=0; for(String w:words(s)) score += freq.getOrDefault(w,0);
            score /= Math.max(8, words(s).size());
            scored.add(new Scored(i,s,score));
        }
        int targetChars = pages<=1 ? 4200 : pages<=5 ? 21000 : 42000;
        scored.sort((a,b)->Double.compare(b.score,a.score));
        Set<Integer> picked=new HashSet<>(); int chars=0;
        for(Scored s:scored){ if(chars>=targetChars) break; picked.add(s.index); chars+=s.text.length()+2; }
        StringBuilder out=new StringBuilder();
        out.append("EĞİTİM ÖZETİ – ").append(pages).append(" SAYFA HEDEFİ\n\n");
        for(int i=0;i<parts.length;i++) if(picked.contains(i)) out.append("• ").append(parts[i].trim()).append("\n\n");
        if(out.length()<700) out.append(raw.substring(0, Math.min(raw.length(), targetChars)));
        return out.toString();
    }

    private static List<String> words(String s){
        String n=s.toLowerCase(new Locale("tr","TR")).replaceAll("[^\\p{L}\\p{N} ]"," ");
        return Arrays.asList(n.split("\\s+"));
    }
    private static final class Scored { final int index; final String text; final double score; Scored(int i,String t,double s){index=i;text=t;score=s;} }
}
