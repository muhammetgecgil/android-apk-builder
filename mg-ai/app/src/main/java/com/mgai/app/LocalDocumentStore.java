package com.mgai.app;

import android.content.Context;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class LocalDocumentStore {
    private static final String PREFS="mg_local_docs";
    private static final String KEY="docs";
    private static final int MAX_DOCS=20;
    private static final int MAX_CHARS_PER_DOC=120000;
    private LocalDocumentStore(){}

    public static synchronized String importText(Context c, Uri uri, String name) throws Exception {
        InputStream in=c.getContentResolver().openInputStream(uri);
        if(in==null) throw new IllegalArgumentException("document_stream_unavailable");
        StringBuilder sb=new StringBuilder();
        try(BufferedReader br=new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))){
            String line; while((line=br.readLine())!=null && sb.length()<MAX_CHARS_PER_DOC){
                sb.append(line).append('\n');
            }
        }
        String text=sb.toString().trim();
        if(text.length()<10) throw new IllegalArgumentException("document_empty_or_too_short");
        JSONArray arr=load(c);
        JSONObject o=new JSONObject();
        o.put("name",name==null?"belge":name);
        o.put("text",text);
        o.put("ts",System.currentTimeMillis());
        arr.put(o);
        while(arr.length()>MAX_DOCS){JSONArray n=new JSONArray();for(int i=1;i<arr.length();i++)n.put(arr.get(i));arr=n;}
        c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,arr.toString()).apply();
        return name+" • "+text.length()+" karakter";
    }

    public static synchronized String retrieve(Context c,String query,int maxChunks){
        try{
            JSONArray a=load(c); if(a.length()==0)return "";
            Set<String> q=tokens(query); ArrayList<Scored> list=new ArrayList<>();
            for(int i=0;i<a.length();i++){
                JSONObject o=a.getJSONObject(i);String name=o.optString("name","belge");String text=o.optString("text","");
                int chunkSize=1200, overlap=180;
                for(int s=0;s<text.length();s+=Math.max(1,chunkSize-overlap)){
                    int e=Math.min(text.length(),s+chunkSize);String ch=text.substring(s,e);int score=score(q,ch);
                    if(score>0)list.add(new Scored(score,name,ch)); if(e==text.length())break;
                }
            }
            list.sort((x,y)->Integer.compare(y.score,x.score));StringBuilder out=new StringBuilder();
            for(int i=0;i<Math.min(maxChunks,list.size());i++){
                Scored r=list.get(i);out.append("[Belge: ").append(r.name).append("]\n").append(r.text).append("\n\n");
            }
            return out.toString().trim();
        }catch(Exception e){return "";}
    }

    public static synchronized String summary(Context c){
        JSONArray a=load(c); if(a.length()==0)return "Belge yok";StringBuilder sb=new StringBuilder();
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)sb.append("• ").append(o.optString("name","belge")).append('\n');}
        return sb.toString().trim();
    }
    public static synchronized void clear(Context c){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(KEY).apply();}
    private static JSONArray load(Context c){String s=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"[]");try{return new JSONArray(s);}catch(Exception e){return new JSONArray();}}
    private static Set<String> tokens(String s){Set<String> out=new HashSet<>();for(String x:s.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")){if(x.length()>=3)out.add(x);}return out;}
    private static int score(Set<String> q,String text){Set<String> t=tokens(text);int n=0;for(String x:q)if(t.contains(x))n++;return n;}
    private static final class Scored{final int score;final String name;final String text;Scored(int s,String n,String t){score=s;name=n;text=t;}}
}
