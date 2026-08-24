package com.mg.quakewatch;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QuakeAnalyzer {
    private static final String API = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=now-1days&minmagnitude=0&orderby=time&limit=20000";
    private static final DecimalFormat DF = new DecimalFormat("0.00");

    static final class Event {
        final double lat, lon, depth, mag;
        final long time;
        final String place;
        Event(double lat,double lon,double depth,double mag,long time,String place){this.lat=lat;this.lon=lon;this.depth=depth;this.mag=mag;this.time=time;this.place=place;}
    }

    static final class Cell {
        final int a,b;
        final List<Event> events = new ArrayList<>();
        double score,bValue,rateRatio,etas;
        Cell(int a,int b){this.a=a;this.b=b;}
        double lat(){return a*2.0+1.0;}
        double lon(){return b*2.0+1.0;}
        String name(){return String.format(Locale.US,"%.1f°, %.1f°", lat(), lon());}
    }

    public static final class Report {
        public final int eventCount;
        public final double maxScore;
        public final String text;
        public final String hotspotsJson;
        Report(int n,double m,String t,String h){eventCount=n;maxScore=m;text=t;hotspotsJson=h;}
    }

    public static Report fetchAndAnalyze() throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(API).openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(25000); c.setRequestProperty("User-Agent","QuakeWatch-Android/1.1");
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while((line=br.readLine())!=null) sb.append(line);
        br.close();
        JSONObject root = new JSONObject(sb.toString());
        JSONArray f = root.getJSONArray("features");
        List<Event> all = new ArrayList<>();
        Map<String,Cell> cells = new HashMap<>();
        for(int i=0;i<f.length();i++){
            JSONObject x=f.getJSONObject(i), p=x.getJSONObject("properties");
            JSONArray co=x.getJSONObject("geometry").getJSONArray("coordinates");
            double mag = p.isNull("mag") ? 0 : p.getDouble("mag");
            Event e=new Event(co.getDouble(1),co.getDouble(0),co.optDouble(2,0),mag,p.getLong("time"),p.optString("place","Bilinmeyen"));
            all.add(e);
            int la=(int)Math.floor((e.lat+90.0)/2.0)-45;
            int lo=(int)Math.floor((e.lon+180.0)/2.0)-90;
            String k=la+":"+lo;
            Cell cell=cells.get(k); if(cell==null){cell=new Cell(la,lo);cells.put(k,cell);} cell.events.add(e);
        }
        long now=System.currentTimeMillis();
        List<Cell> ranked=new ArrayList<>();
        for(Cell cell:cells.values()){
            int n1=0,n6=0,n24=cell.events.size(); double maxM=0,sumM=0; int nComp=0;
            for(Event e:cell.events){
                long age=now-e.time;
                if(age<=3600_000L)n1++;
                if(age<=6*3600_000L)n6++;
                if(e.mag>=1.5){sumM+=e.mag;nComp++;}
                if(e.mag>maxM)maxM=e.mag;
            }
            double expected6=Math.max(0.5,n24/4.0);
            cell.rateRatio=(n6+0.5)/(expected6+0.5);
            if(nComp>=4){ double mean=sumM/nComp; cell.bValue=0.4342944819/Math.max(0.05,mean-1.45); } else cell.bValue=1.0;
            double et=0;
            for(Event e:cell.events){
                double hours=Math.max(0.05,(now-e.time)/3600000.0);
                double productivity=Math.exp(Math.min(8.0,1.25*Math.max(0,e.mag-2.5)));
                et += productivity/Math.pow(hours+0.2,1.05);
            }
            cell.etas=Math.log1p(et);
            double burst=Math.log1p(n1*2.0+n6);
            double bSignal=Math.max(0,1.1-cell.bValue);
            double magSignal=Math.max(0,maxM-3.5);
            double raw=1.7*Math.log1p(cell.rateRatio)+0.9*cell.etas+0.7*burst+0.8*bSignal+0.55*magSignal;
            cell.score=100.0*(1.0-Math.exp(-raw/7.0));
            if(n24>=2)ranked.add(cell);
        }
        Collections.sort(ranked,Comparator.comparingDouble((Cell x)->x.score).reversed());
        StringBuilder out=new StringBuilder();
        out.append("OLASILIKSAL SICAK NOKTA ANALİZİ • SON 24 SAAT\n");
        out.append("Katalog olayı: ").append(all.size()).append("\n");
        out.append("Puan = kısa dönem aktivite artışı + ETAS-benzeri tetiklenme yoğunluğu + b-değeri sinyali + büyüklük katkısı.\n\n");
        JSONArray hot = new JSONArray();
        int mapLim=Math.min(100,ranked.size());
        for(int i=0;i<mapLim;i++){
            Cell z=ranked.get(i);
            JSONObject o=new JSONObject();
            o.put("lat",z.lat()); o.put("lon",z.lon()); o.put("score",z.score());
            o.put("count",z.events.size()); o.put("rate",z.rateRatio); o.put("b",z.bValue); o.put("etas",z.etas);
            hot.put(o);
        }
        int lim=Math.min(12,ranked.size());
        for(int i=0;i<lim;i++){
            Cell z=ranked.get(i);
            Event last=z.events.get(0); for(Event e:z.events) if(e.time>last.time) last=e;
            out.append(i+1).append(") Bölge ").append(z.name())
               .append("  • olasılıksal aktivite ").append(DF.format(z.score)).append("/100\n")
               .append("   24s olay=").append(z.events.size())
               .append("  oran=").append(DF.format(z.rateRatio)).append("x")
               .append("  b≈").append(DF.format(z.bValue))
               .append("  ETAS=").append(DF.format(z.etas)).append("\n")
               .append("   son: M").append(DF.format(last.mag)).append(" • ").append(last.place).append("\n\n");
        }
        out.append("YORUM\n");
        if(lim>0){
            double s=ranked.get(0).score;
            if(s>=80) out.append("Çok güçlü bir sismik kümelenme/anomali var. Haritada kırmızı gösterilir; yeni büyük deprem garantisi değildir.\n");
            else if(s>=60) out.append("Bazı bölgelerde belirgin kısa dönem aktivite artışı var; haritada turuncu/kırmızı görünür.\n");
            else out.append("Modelin eşik üstü güçlü anomalisi görünmüyor.\n");
        }
        out.append("\nBilimsel sınır: Bu harita deterministik deprem tahmini değildir. Kesin tarih, saat, büyüklük ve konum veremez; göreli kısa dönem sismik aktivite/anomali gösterir.");
        return new Report(all.size(),lim==0?0:ranked.get(0).score,out.toString(),hot.toString());
    }
}
