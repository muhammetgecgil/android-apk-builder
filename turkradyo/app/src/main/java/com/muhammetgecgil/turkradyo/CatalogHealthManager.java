package com.muhammetgecgil.turkradyo;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Full-catalog background validator + conservative self-healing.
 * Scans in small batches to avoid disturbing foreground playback. */
public final class CatalogHealthManager {
    private static final String P="catalog_health_v3", KEY="report", CURSOR="cursor";
    private static final int BATCH=12, THREADS=3;
    private static final AtomicBoolean FULL_SWEEP_RUNNING=new AtomicBoolean(false);
    private CatalogHealthManager(){}

    public static void scanAsync(Context c,String queueJson){new Thread(()->scan(c.getApplicationContext(),queueJson),"catalog-health").start();}

    /** Starts one controlled end-to-end sweep. Duplicate requests are ignored. */
    public static void fullSweepAsync(Context c,String queueJson){
        final Context app=c.getApplicationContext();
        if(!FULL_SWEEP_RUNNING.compareAndSet(false,true))return;
        new Thread(()->{
            try{
                JSONArray q=new JSONArray(queueJson==null?"[]":queueJson); if(q.length()==0)return;
                SharedPreferences p=app.getSharedPreferences(P,Context.MODE_PRIVATE);
                p.edit().putBoolean("fullSweepRunning",true).putLong("fullSweepRequestedAt",System.currentTimeMillis()).apply();
                long startCycle=p.getLong("cycle",1); int safety=(int)Math.ceil(q.length()/(double)BATCH)+2;
                for(int i=0;i<safety;i++){
                    scan(app,queueJson);
                    long nowCycle=p.getLong("cycle",1);
                    if(nowCycle>startCycle)break;
                    try{Thread.sleep(1400);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
                }
            }catch(Exception ignored){}
            finally{app.getSharedPreferences(P,Context.MODE_PRIVATE).edit().putBoolean("fullSweepRunning",false).apply();FULL_SWEEP_RUNNING.set(false);}
        },"catalog-full-sweep").start();
    }

    /** Auto-sweep at most once per 24h, normally after the catalog is loaded. */
    public static void autoSweepIfDue(Context c,String queueJson){
        SharedPreferences p=c.getSharedPreferences(P,Context.MODE_PRIVATE); long last=p.getLong("lastFullSweep",0),now=System.currentTimeMillis();
        if(last==0||now-last>=24*60*60_000L)fullSweepAsync(c,queueJson);
    }

    private static void scan(Context c,String queueJson){
        try{
            JSONArray q=new JSONArray(queueJson==null?"[]":queueJson); if(q.length()==0)return;
            SharedPreferences p=c.getSharedPreferences(P,Context.MODE_PRIVATE);
            int start=p.getInt(CURSOR,0)%q.length(),limit=Math.min(BATCH,q.length());
            long cycle=p.getLong("cycle",1),cycleStarted=p.getLong("cycleStarted",0); if(cycleStarted==0)cycleStarted=System.currentTimeMillis();
            JSONObject report; try{report=new JSONObject(p.getString(KEY,"{}"));}catch(Exception e){report=new JSONObject();}
            ExecutorService ex=Executors.newFixedThreadPool(THREADS); List<Future<Result>> fs=new ArrayList<>();
            for(int k=0;k<limit;k++){
                JSONObject x=q.optJSONObject((start+k)%q.length()); if(x==null)continue;
                String name=x.optString("name",x.optString("title","Radyo")),url=x.optString("url"); if(url.isEmpty())continue;
                final int catalogIndex=(start+k)%q.length();
                fs.add(ex.submit(()->new Result(name,url,catalogIndex,probe(url))));
            }
            for(Future<Result> f:fs){
                try{
                    Result r=f.get(6,TimeUnit.SECONDS); JSONObject x=report.optJSONObject(r.name); if(x==null)x=new JSONObject();
                    int ok=x.optInt("ok",0),fail=x.optInt("fail",0),consecutiveFail=x.optInt("consecutiveFail",0);
                    if(r.ms>=0){ok++;consecutiveFail=0;}else{fail++;consecutiveFail++;}
                    int samples=Math.max(1,ok+fail); int successRate=Math.round(ok*100f/samples);
                    int latencyScore=r.ms<0?0:r.ms<=700?100:r.ms<=1500?90:r.ms<=2500?75:r.ms<=4000?55:35;
                    int quality=Math.max(0,Math.min(100,Math.round(successRate*.70f+latencyScore*.30f-consecutiveFail*8f)));
                    x.put("url",r.url).put("catalogIndex",r.index).put("lastCheck",System.currentTimeMillis()).put("latencyMs",Math.max(0,r.ms)).put("healthy",r.ms>=0)
                     .put("ok",ok).put("fail",fail).put("consecutiveFail",consecutiveFail).put("successRate",successRate).put("qualityScore",quality).put("cycle",cycle);
                    report.put(r.name,x);
                    if(r.ms>=0)StreamFallbackManager.markGood(c,r.name,r.url,r.ms);
                    else {StreamFallbackManager.markBad(c,r.name,r.url,consecutiveFail>=2?30*60_000L:60_000L); if(consecutiveFail>=2)scheduleRepair(c,r.name,r.url);}
                }catch(Exception ignored){}
            }
            ex.shutdownNow();
            int next=(start+limit)%q.length(); boolean cycleDone=(start+limit)>=q.length();
            SharedPreferences.Editor ed=p.edit().putString(KEY,report.toString()).putInt(CURSOR,next).putLong("lastScan",System.currentTimeMillis()).putInt("catalogSize",q.length()).putLong("cycleStarted",cycleStarted);
            if(cycleDone){ed.putLong("lastFullSweep",System.currentTimeMillis()).putLong("lastFullSweepDurationMs",System.currentTimeMillis()-cycleStarted).putLong("cycle",cycle+1).putLong("cycleStarted",System.currentTimeMillis());}
            ed.apply();
        }catch(Exception ignored){}
    }

    private static void scheduleRepair(Context c,String name,String primary){
        SharedPreferences p=c.getSharedPreferences(P,Context.MODE_PRIVATE); long now=System.currentTimeMillis();
        try{
            JSONObject report=new JSONObject(p.getString(KEY,"{}")),x=report.optJSONObject(name); if(x==null)x=new JSONObject();
            long last=x.optLong("lastRepairTry",0); if(now-last<30*60_000L)return;
            x.put("lastRepairTry",now).put("repairAttempts",x.optInt("repairAttempts",0)+1); report.put(name,x); p.edit().putString(KEY,report.toString()).apply();
        }catch(Exception ignored){}
        StreamFallbackManager.discoverBestAsync(c,name,primary,u->{
            try{
                SharedPreferences sp=c.getSharedPreferences(P,Context.MODE_PRIVATE); JSONObject report=new JSONObject(sp.getString(KEY,"{}")),x=report.optJSONObject(name); if(x==null)x=new JSONObject();
                if(u!=null&&!u.isEmpty()&&!u.equals(primary)){
                    x.put("repaired",true).put("repairedUrl",u).put("repairedAt",System.currentTimeMillis()).put("repairSuccess",x.optInt("repairSuccess",0)+1).put("healthy",true).put("consecutiveFail",0);
                    StreamFallbackManager.markGood(c,name,u,0);
                }else{x.put("repaired",false).put("lastRepairFail",System.currentTimeMillis());}
                report.put(name,x); sp.edit().putString(KEY,report.toString()).apply();
            }catch(Exception ignored){}
        });
    }

    public static String report(Context c){
        SharedPreferences p=c.getSharedPreferences(P,Context.MODE_PRIVATE);
        try{
            JSONObject o=new JSONObject(),r=new JSONObject(p.getString(KEY,"{}")); int healthy=0,bad=0,repaired=0,pending=0,excellent=0,weak=0; long qSum=0; Iterator<String> it=r.keys();
            while(it.hasNext()){
                JSONObject x=r.optJSONObject(it.next()); if(x==null)continue;
                if(x.optBoolean("healthy"))healthy++; else bad++;
                if(x.optBoolean("repaired"))repaired++; else if(!x.optBoolean("healthy"))pending++;
                int qs=x.optInt("qualityScore",0); qSum+=qs; if(qs>=90)excellent++; if(qs>0&&qs<60)weak++;
            }
            int checked=healthy+bad,total=p.getInt("catalogSize",0),coverage=total>0?Math.min(100,Math.round(checked*100f/total)):0,avg=checked>0?Math.round(qSum/(float)checked):0;
            long lastFull=p.getLong("lastFullSweep",0); boolean verified=total>0&&coverage>=100&&lastFull>0;
            o.put("healthy",healthy).put("bad",bad).put("checked",checked).put("catalogSize",total).put("coveragePct",coverage).put("averageQuality",avg).put("excellent",excellent).put("weak",weak)
             .put("repaired",repaired).put("repairPending",pending).put("lastScan",p.getLong("lastScan",0)).put("lastFullSweep",lastFull).put("lastFullSweepDurationMs",p.getLong("lastFullSweepDurationMs",0)).put("cycle",p.getLong("cycle",1))
             .put("fullSweepRunning",p.getBoolean("fullSweepRunning",false)||FULL_SWEEP_RUNNING.get()).put("verified",verified).put("stations",r);
            return o.toString();
        }catch(Exception e){return"{}";}
    }

    private static final class Result{String name,url;int index;long ms;Result(String n,String u,int i,long m){name=n;url=u;index=i;ms=m;}}
    private static long probe(String raw){
        HttpURLConnection h=null; InputStream in=null;
        try{
            long t=System.currentTimeMillis(); h=(HttpURLConnection)new URL(raw).openConnection(); h.setConnectTimeout(2200); h.setReadTimeout(3000); h.setInstanceFollowRedirects(true); h.setRequestProperty("User-Agent","TurkRadyo/2.3"); h.setRequestProperty("Icy-MetaData","0");
            int code=h.getResponseCode(); if(code<200||code>=400)return-1; in=h.getInputStream(); byte[] b=new byte[2048]; int total=0,n; while(total<2048&&(n=in.read(b,0,2048-total))>0)total+=n; return total>=128?System.currentTimeMillis()-t:-1;
        }catch(Exception e){return-1;}finally{try{if(in!=null)in.close();}catch(Exception ignored){}if(h!=null)h.disconnect();}
    }
}
