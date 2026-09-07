package com.mg.quakewatch;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Rolling out-of-sample style research backtest over the last 30 days.
 *  It never claims deterministic prediction. The model uses only data before each test day.
 */
public final class BacktestEngine {
    private static final String API="https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=now-30days&minlatitude=34&maxlatitude=43&minlongitude=25&maxlongitude=46&orderby=time&limit=20000";
    static final class E { double lat,lon,mag; long t; E(double a,double o,double m,long tt){lat=a;lon=o;mag=m;t=tt;} }
    public static final class Result {
        public final String text; public final double hitRate, baselineHitRate, brier;
        Result(String t,double h,double b,double br){text=t;hitRate=h;baselineHitRate=b;brier=br;}
    }

    public static Result run() throws Exception {
        List<E> es=fetch(); if(es.size()<10) return new Result("Backtest için yeterli katalog verisi yok.",0,0,0);
        long now=System.currentTimeMillis(); long day=86400000L;
        int tests=0,hits=0,baseHits=0; double brierSum=0;
        // Test the latest 14 complete/near-complete daily windows. Each forecast sees only the preceding 7 days.
        for(int d=13;d>=0;d--){
            long testStart=now-(d+1L)*day, testEnd=testStart+day, trainStart=testStart-7L*day;
            Map<String,Integer> train=new HashMap<>(); Map<String,Integer> recent=new HashMap<>();
            for(E e:es){
                if(e.t>=trainStart&&e.t<testStart){String k=cell(e);train.put(k,train.getOrDefault(k,0)+1); if(e.t>=testStart-day)recent.put(k,recent.getOrDefault(k,0)+1);}
            }
            String best=null; double bestScore=-1;
            for(String k:train.keySet()){
                double n7=train.get(k),n1=recent.getOrDefault(k,0);
                double score=(n1+0.5)/((n7/7.0)+0.5); if(score>bestScore){bestScore=score;best=k;}
            }
            // Background baseline = historically busiest training cell.
            String base=null; int bc=-1; for(Map.Entry<String,Integer>x:train.entrySet())if(x.getValue()>bc){bc=x.getValue();base=x.getKey();}
            boolean hit=false,bhit=false; double obs=0;
            for(E e:es){if(e.t>=testStart&&e.t<testEnd&&e.mag>=3.0){String k=cell(e);if(k.equals(best))hit=true;if(k.equals(base))bhit=true;}}
            if(hit)hits++; if(bhit)baseHits++; tests++;
            double p=Math.min(0.85,Math.max(0.05,(bestScore-0.5)/3.0)); obs=hit?1:0; brierSum+=(p-obs)*(p-obs);
        }
        double hr=tests==0?0:100.0*hits/tests, bhr=tests==0?0:100.0*baseHits/tests, br=tests==0?0:brierSum/tests;
        StringBuilder s=new StringBuilder();
        s.append("ROLLING BACKTEST • SON 30 GÜN\n");
        s.append("Her test günü yalnız önceki 7 gün kullanıldı; hedef sonraki 24 saatte aynı 0.5° hücrede M≥3 olay görülmesiydi.\n\n");
        s.append(String.format(Locale.US,"QIE-hızlanma hit-rate: %.1f%%\n",hr));
        s.append(String.format(Locale.US,"Basit arka-plan baseline: %.1f%%\n",bhr));
        s.append(String.format(Locale.US,"Brier skoru (düşük daha iyi): %.3f\n\n",br));
        if(hr>bhr+5)s.append("Bu kısa pencerede hızlanma modeli baseline'dan daha iyi görünüyor; daha uzun ve bağımsız test gerekir.\n");
        else s.append("Bu kısa pencerede model baseline'a karşı belirgin üstünlük göstermiyor; ağırlıkların artırılması bilimsel olarak gerekçeli değil.\n");
        s.append("Not: 30 günlük mobil backtest yalnız hızlı sağlık kontrolüdür; gerçek model doğrulaması yıllarca veri ve bağımsız prospective test gerektirir.");
        return new Result(s.toString(),hr,bhr,br);
    }

    private static String cell(E e){int a=(int)Math.floor((e.lat-34)/0.5),b=(int)Math.floor((e.lon-25)/0.5);return a+":"+b;}
    private static List<E> fetch() throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection(); c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","QuakeWatch-Backtest/2.0");
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder sb=new StringBuilder();String l;while((l=br.readLine())!=null)sb.append(l);br.close();
        JSONArray f=new JSONObject(sb.toString()).getJSONArray("features");List<E> out=new ArrayList<>();
        for(int i=0;i<f.length();i++){JSONObject x=f.getJSONObject(i),p=x.getJSONObject("properties");JSONArray co=x.getJSONObject("geometry").getJSONArray("coordinates");double m=p.isNull("mag")?0:p.getDouble("mag");out.add(new E(co.getDouble(1),co.getDouble(0),m,p.getLong("time")));}
        return out;
    }
    private BacktestEngine(){}
}
