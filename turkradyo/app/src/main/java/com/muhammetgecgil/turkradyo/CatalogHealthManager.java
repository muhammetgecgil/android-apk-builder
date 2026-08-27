package com.muhammetgecgil.turkradyo;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Background catalog health scanner. Tests a small batch per run so normal playback is never blocked. */
public final class CatalogHealthManager {
    private static final String P="catalog_health_v1", KEY="report", CURSOR="cursor";
    private CatalogHealthManager(){}
    public static void scanAsync(Context c,String queueJson){new Thread(()->scan(c.getApplicationContext(),queueJson),"catalog-health").start();}
    private static void scan(Context c,String queueJson){try{JSONArray q=new JSONArray(queueJson==null?"[]":queueJson);if(q.length()==0)return;SharedPreferences p=c.getSharedPreferences(P,Context.MODE_PRIVATE);int start=p.getInt(CURSOR,0)%q.length(),limit=Math.min(12,q.length());JSONObject report;try{report=new JSONObject(p.getString(KEY,"{}"));}catch(Exception e){report=new JSONObject();}ExecutorService ex=Executors.newFixedThreadPool(3);List<Future<Result>> fs=new ArrayList<>();for(int k=0;k<limit;k++){JSONObject x=q.optJSONObject((start+k)%q.length());if(x==null)continue;String name=x.optString("name",x.optString("title","Radyo")),url=x.optString("url");if(url.isEmpty())continue;fs.add(ex.submit(()->new Result(name,url,probe(url))));}for(Future<Result> f:fs){try{Result r=f.get(5,TimeUnit.SECONDS);JSONObject x=report.optJSONObject(r.name);if(x==null)x=new JSONObject();x.put("url",r.url).put("lastCheck",System.currentTimeMillis()).put("latencyMs",Math.max(0,r.ms)).put("healthy",r.ms>=0);x.put("ok",x.optInt("ok",0)+(r.ms>=0?1:0));x.put("fail",x.optInt("fail",0)+(r.ms<0?1:0));report.put(r.name,x);if(r.ms>=0)StreamFallbackManager.markGood(c,r.name,r.url,r.ms);else StreamFallbackManager.markBad(c,r.name,r.url,60_000L);}catch(Exception ignored){}}ex.shutdownNow();p.edit().putString(KEY,report.toString()).putInt(CURSOR,(start+limit)%q.length()).putLong("lastScan",System.currentTimeMillis()).apply();}catch(Exception ignored){}}
    public static String report(Context c){SharedPreferences p=c.getSharedPreferences(P,Context.MODE_PRIVATE);try{JSONObject o=new JSONObject();JSONObject r=new JSONObject(p.getString(KEY,"{}"));int healthy=0,bad=0;Iterator<String> it=r.keys();while(it.hasNext()){JSONObject x=r.optJSONObject(it.next());if(x!=null&&x.optBoolean("healthy"))healthy++;else bad++;}o.put("healthy",healthy).put("bad",bad).put("checked",healthy+bad).put("lastScan",p.getLong("lastScan",0)).put("stations",r);return o.toString();}catch(Exception e){return"{}";}}
    private static final class Result{String name,url;long ms;Result(String n,String u,long m){name=n;url=u;ms=m;}}
    private static long probe(String raw){HttpURLConnection h=null;InputStream in=null;try{long t=System.currentTimeMillis();h=(HttpURLConnection)new URL(raw).openConnection();h.setConnectTimeout(2200);h.setReadTimeout(3000);h.setInstanceFollowRedirects(true);h.setRequestProperty("User-Agent","TurkRadyo/2.2");h.setRequestProperty("Icy-MetaData","0");int code=h.getResponseCode();if(code<200||code>=400)return-1;in=h.getInputStream();byte[] b=new byte[2048];int total=0,n;while(total<2048&&(n=in.read(b,0,2048-total))>0)total+=n;return total>=128?System.currentTimeMillis()-t:-1;}catch(Exception e){return-1;}finally{try{if(in!=null)in.close();}catch(Exception ignored){}if(h!=null)h.disconnect();}}
}
