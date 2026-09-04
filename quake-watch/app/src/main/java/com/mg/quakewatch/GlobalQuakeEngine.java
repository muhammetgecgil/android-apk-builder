package com.mg.quakewatch;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Global seismic catalog + transparent anomaly/forecast context.
 * Source: USGS GeoJSON 7-day feed.
 * IMPORTANT: this is not an exact earthquake prediction engine. It ranks spatial cells
 * by recent seismicity, acceleration, magnitude and recency to create a testable
 * probabilistic/anomaly forecast that can later be backtested.
 */
public final class GlobalQuakeEngine {
    private static final String URL7="https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_week.geojson";
    private static final double CELL=5.0;

    public static final class Report {
        public final String eventsJson, hotspotsJson, summary;
        public final int eventCount, count24;
        public final long fetchedAt;
        Report(String e,String h,String s,int n,int n24,long t){eventsJson=e;hotspotsJson=h;summary=s;eventCount=n;count24=n24;fetchedAt=t;}
    }

    private static final class Cell {
        final int iy,ix; int n7=0,n24=0; double maxMag=-9,sumMag=0; long latest=0;
        Cell(int y,int x){iy=y;ix=x;}
        double lat(){return -90+(iy+.5)*CELL;}
        double lon(){return -180+(ix+.5)*CELL;}
    }

    public static Report fetch() throws Exception {
        String raw=get(URL7);
        JSONObject root=new JSONObject(raw); JSONArray f=root.getJSONArray("features");
        JSONArray events=new JSONArray(); Map<String,Cell> cells=new HashMap<>();
        long now=System.currentTimeMillis(), cut24=now-24L*3600_000L; int n24=0;
        for(int i=0;i<f.length();i++){
            JSONObject feat=f.optJSONObject(i); if(feat==null)continue;
            JSONObject p=feat.optJSONObject("properties"), g=feat.optJSONObject("geometry"); if(p==null||g==null)continue;
            JSONArray c=g.optJSONArray("coordinates"); if(c==null||c.length()<3)continue;
            double lon=c.optDouble(0,999),lat=c.optDouble(1,999),dep=Math.max(0,c.optDouble(2,0)); if(Math.abs(lat)>90||Math.abs(lon)>180)continue;
            double mag=p.isNull("mag")?0:p.optDouble("mag",0); long time=p.optLong("time",0); String place=p.optString("place","");
            JSONObject e=new JSONObject();e.put("lat",lat);e.put("lon",lon);e.put("depth",dep);e.put("mag",mag);e.put("time",time);e.put("place",place);events.put(e);
            if(time>=cut24)n24++;
            int iy=(int)Math.floor((lat+90)/CELL);int ix=(int)Math.floor((lon+180)/CELL);iy=Math.max(0,Math.min(35,iy));ix=Math.max(0,Math.min(71,ix));String key=iy+":"+ix;
            Cell z=cells.get(key);if(z==null){z=new Cell(iy,ix);cells.put(key,z);}z.n7++;if(time>=cut24)z.n24++;z.maxMag=Math.max(z.maxMag,mag);z.sumMag+=Math.max(0,mag);z.latest=Math.max(z.latest,time);
        }
        ArrayList<JSONObject> ranked=new ArrayList<>();
        for(Cell z:cells.values()){
            double baseline=Math.max(.35,z.n7/7.0);double accel=z.n24/baseline;
            double recencyH=z.latest>0?(now-z.latest)/3600000.0:168;double recency=Math.exp(-Math.max(0,recencyH)/36.0);
            double magTerm=Math.max(0,z.maxMag-2.5);
            double score=14*Math.log1p(z.n7)+18*Math.log1p(z.n24)+16*Math.log1p(Math.max(0,accel-1))+9*magTerm+12*recency;
            score=Math.max(0,Math.min(100,score));
            double confidence=Math.min(92,28+5*Math.sqrt(z.n7)+Math.min(25,z.n24*2));
            JSONObject o=new JSONObject();o.put("lat",z.lat());o.put("lon",z.lon());o.put("score",score);o.put("confidence",confidence);o.put("n24",z.n24);o.put("n7",z.n7);o.put("rate",accel);o.put("maxMag",z.maxMag);o.put("latest",z.latest);ranked.add(o);
        }
        Collections.sort(ranked,new Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){return Double.compare(b.optDouble("score"),a.optDouble("score"));}});
        JSONArray hot=new JSONArray();for(int i=0;i<Math.min(20,ranked.size());i++)hot.put(ranked.get(i));
        double top=hot.length()>0?hot.getJSONObject(0).optDouble("score",0):0;
        String summary=String.format(Locale.US,"USGS 7 gün: %d olay • son 24s: %d • en yüksek anomali/forecast skoru: %.1f/100",events.length(),n24,top);
        return new Report(events.toString(),hot.toString(),summary,events.length(),n24,now);
    }

    private static String get(String u)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","QuakeWatch-Global/3.1");c.setRequestProperty("Accept","application/geo+json,application/json");
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder sb=new StringBuilder();String l;while((l=br.readLine())!=null)sb.append(l);br.close();return sb.toString();
    }
    private GlobalQuakeEngine(){}
}
