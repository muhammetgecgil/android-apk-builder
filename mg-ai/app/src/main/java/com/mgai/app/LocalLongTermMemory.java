package com.mgai.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LocalLongTermMemory {
    private static final String PREFS="mg_long_term_memory";
    private static final String KEY_ITEMS="items";
    private static final String KEY_SUMMARY="summary";
    private static final int MAX_ITEMS=120;
    private LocalLongTermMemory(){}

    public static synchronized boolean maybeRememberUserMessage(Context c,String text){
        String t=text==null?"":text.trim(); if(t.length()<8)return false;
        String l=t.toLowerCase(new Locale("tr","TR"));
        boolean explicit=l.startsWith("hatırla:")||l.startsWith("bunu hatırla")||l.startsWith("unutma:");
        boolean durable=containsAny(l,"benim adım","adım ","hedefim","tercihim","seviyorum","sevmiyorum","projem","projemiz","çalışıyorum","kullanıyorum","istiyorum","ben ","benim ","her zaman","asla");
        if(!explicit&&!durable)return false;
        String clean=t.replaceFirst("(?i)^hatırla\\s*:\\s*","").replaceFirst("(?i)^unutma\\s*:\\s*","").trim();
        add(c,"user_fact",clean, explicit?1.0:0.72); return true;
    }

    public static synchronized void addProjectNote(Context c,String text){ add(c,"project",text,0.9); }

    private static void add(Context c,String type,String text,double importance){
        if(text==null||text.trim().isEmpty())return;
        try{
            JSONArray arr=load(c); String norm=normalize(text);
            for(int i=0;i<arr.length();i++) if(normalize(arr.getJSONObject(i).optString("text")).equals(norm)) return;
            JSONObject o=new JSONObject(); o.put("type",type);o.put("text",text.trim());o.put("importance",importance);o.put("ts",System.currentTimeMillis());arr.put(o);
            while(arr.length()>MAX_ITEMS){JSONArray n=new JSONArray();for(int i=1;i<arr.length();i++)n.put(arr.get(i));arr=n;}
            prefs(c).edit().putString(KEY_ITEMS,arr.toString()).apply();
            rebuildSummary(c,arr);
        }catch(Exception ignored){}
    }

    public static synchronized String relevantContext(Context c,String query,int maxItems){
        try{
            JSONArray arr=load(c); Set<String> q=terms(query); List<Scored> list=new ArrayList<>();
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.getJSONObject(i);String text=o.optString("text",""); if(text.isEmpty())continue;
                Set<String> m=terms(text); int overlap=0; for(String s:q)if(m.contains(s))overlap++;
                double score=o.optDouble("importance",0.5)+(overlap*0.55)+(i/(double)Math.max(1,arr.length())*0.15);
                if(overlap>0 || q.isEmpty()) list.add(new Scored(text,score));
            }
            list.sort(Comparator.comparingDouble((Scored x)->x.score).reversed());
            StringBuilder sb=new StringBuilder(); int n=Math.min(maxItems,list.size());
            for(int i=0;i<n;i++) sb.append("- ").append(list.get(i).text).append('\n');
            if(sb.length()==0){String s=summary(c);if(!s.isEmpty())sb.append(s);}
            return sb.toString();
        }catch(Exception e){return "";}
    }

    public static synchronized String allText(Context c){
        try{JSONArray a=load(c);StringBuilder sb=new StringBuilder();for(int i=0;i<a.length();i++)sb.append("• ").append(a.getJSONObject(i).optString("text")).append('\n');return sb.toString();}catch(Exception e){return "";}
    }
    public static synchronized String summary(Context c){return prefs(c).getString(KEY_SUMMARY,"");}
    public static synchronized int count(Context c){return load(c).length();}
    public static synchronized void clear(Context c){prefs(c).edit().clear().apply();}

    private static void rebuildSummary(Context c,JSONArray arr){
        try{StringBuilder sb=new StringBuilder();int start=Math.max(0,arr.length()-12);for(int i=start;i<arr.length();i++)sb.append("- ").append(arr.getJSONObject(i).optString("text")).append('\n');prefs(c).edit().putString(KEY_SUMMARY,sb.toString()).apply();}catch(Exception ignored){}
    }
    private static JSONArray load(Context c){String s=prefs(c).getString(KEY_ITEMS,"[]");try{return new JSONArray(s);}catch(Exception e){return new JSONArray();}}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private static String normalize(String s){return s.toLowerCase(new Locale("tr","TR")).replaceAll("[^a-z0-9çğıöşü ]"," ").replaceAll("\\s+"," ").trim();}
    private static boolean containsAny(String s,String...xs){for(String x:xs)if(s.contains(x))return true;return false;}
    private static Set<String> terms(String s){Set<String> r=new HashSet<>();for(String x:normalize(s).split(" "))if(x.length()>=4&&!STOP.contains(x))r.add(x);return r;}
    private static final Set<String> STOP=new HashSet<>(Arrays.asList("bunu","şunu","olan","olarak","için","gibi","daha","sonra","şimdi","benim","senin","burada","orada","hangi","neden","nasıl","ama","veya","ile"));
    private static final class Scored{final String text;final double score;Scored(String t,double s){text=t;score=s;}}
}
