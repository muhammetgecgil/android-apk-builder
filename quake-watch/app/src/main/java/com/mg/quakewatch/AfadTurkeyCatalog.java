package com.mg.quakewatch;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Official Turkey earthquake catalog adapter using AFAD Event Web Service.
 * Normalizes AFAD records into Quake Watch's common event JSON shape.
 */
public final class AfadTurkeyCatalog {
    private static final String BASE="https://deprem.afad.gov.tr/apiv2/event/filter";
    public static final class Report { public final String eventsJson, source; public final int count; Report(String j,int n,String s){eventsJson=j;count=n;source=s;} }

    public static Report fetchLast7Days() throws Exception {
        Calendar cal=Calendar.getInstance(TimeZone.getTimeZone("UTC")); Date end=cal.getTime(); cal.add(Calendar.DAY_OF_YEAR,-7); Date start=cal.getTime();
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));
        String q="start="+enc(f.format(start))+"&end="+enc(f.format(end))+"&minlat=34&maxlat=43&minlon=25&maxlon=46&orderby=timedesc&limit=20000&format=json";
        HttpURLConnection c=(HttpURLConnection)new URL(BASE+"?"+q).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","QuakeWatch-AFAD/3.0.1");c.setRequestProperty("Accept","application/json");
        int code=c.getResponseCode();if(code<200||code>=300)throw new Exception("AFAD HTTP "+code);
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);br.close();
        JSONArray src=parseArray(sb.toString());JSONArray out=new JSONArray();
        for(int i=0;i<src.length();i++){
            JSONObject a=src.optJSONObject(i);if(a==null)continue;
            double lat=num(a,"latitude","lat"),lon=num(a,"longitude","lon"),dep=num(a,"depth","depthKm"),mag=num(a,"magnitude","mag");
            if(lat<34||lat>43||lon<25||lon>46)continue;
            long time=parseTime(str(a,"date","time","originTime"));
            String place=str(a,"location","place","province");
            JSONObject o=new JSONObject();o.put("lat",lat);o.put("lon",lon);o.put("mag",mag);o.put("depth",dep);o.put("time",time);o.put("place",place);o.put("source","AFAD");o.put("eventId",str(a,"eventID","eventId","id"));
            FaultModel.Nearest nf=FaultModel.nearest(lat,lon);o.put("fault",nf.name);o.put("faultSystem",nf.system);o.put("faultType",nf.type);o.put("faultKm",nf.km);out.put(o);
        }
        return new Report(out.toString(),out.length(),"AFAD");
    }

    private static JSONArray parseArray(String s)throws Exception{String t=s.trim();if(t.startsWith("["))return new JSONArray(t);JSONObject o=new JSONObject(t);JSONArray a=o.optJSONArray("result");if(a==null)a=o.optJSONArray("data");if(a==null)a=o.optJSONArray("events");if(a==null)throw new Exception("AFAD JSON formatı tanınmadı");return a;}
    private static double num(JSONObject o,String...k){for(String x:k){Object v=o.opt(x);if(v==null||v==JSONObject.NULL)continue;try{return v instanceof Number?((Number)v).doubleValue():Double.parseDouble(String.valueOf(v).replace(',','.'));}catch(Exception ignored){}}return 0;}
    private static String str(JSONObject o,String...k){for(String x:k){String v=o.optString(x,"");if(v!=null&&!v.isEmpty()&&!"null".equalsIgnoreCase(v))return v;}return "";}
    private static long parseTime(String s){if(s==null||s.isEmpty())return 0;String[] p={"yyyy-MM-dd'T'HH:mm:ss.SSS","yyyy-MM-dd'T'HH:mm:ss","yyyy-MM-dd HH:mm:ss"};for(String x:p){try{SimpleDateFormat f=new SimpleDateFormat(x,Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.parse(s).getTime();}catch(Exception ignored){}}try{return Long.parseLong(s);}catch(Exception ignored){}return 0;}
    private static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    private AfadTurkeyCatalog(){}
}
