package com.mg.quakewatch;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/** Persists model forecasts and later scores them against subsequently observed earthquakes. */
public final class PredictionAudit {
    private static final String PREF="quake_prediction_audit";
    private static final String KEY="records";
    private static final double HIT_KM=75.0;
    private static final double MIN_MAG=3.5;
    private static final long H24=24L*3600_000L;
    private static final long D7=7L*24L*3600_000L;

    public static void recordTurkeyForecast(Context c, TurkeyAnalyzer.Report r){
        try{
            SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
            JSONArray all=new JSONArray(p.getString(KEY,"[]"));
            JSONArray hot=new JSONArray(r.hotspotsJson);
            long now=System.currentTimeMillis();
            int n=Math.min(5,hot.length());
            for(int i=0;i<n;i++){
                JSONObject h=hot.getJSONObject(i);
                JSONObject x=new JSONObject();
                x.put("id",now+"-"+i); x.put("created",now); x.put("lat",h.getDouble("lat")); x.put("lon",h.getDouble("lon"));
                x.put("score",h.optDouble("score",0)); x.put("q24",h.optDouble("q24",h.optDouble("score",0))); x.put("q7",h.optDouble("q7",h.optDouble("score",0)));
                x.put("confidence",h.optDouble("confidence",0)); x.put("rate",h.optDouble("rate",0)); x.put("b",h.optDouble("b",1)); x.put("etas",h.optDouble("etas",0)); x.put("migration",h.optDouble("migration",0));
                x.put("deadline24",now+H24); x.put("deadline7",now+D7); x.put("status24","OPEN"); x.put("status7","OPEN");
                all.put(x);
            }
            all=trim(all,250);
            p.edit().putString(KEY,all.toString()).apply();
        }catch(Exception ignored){}
    }

    public static void verifyAgainstCatalog(Context c, String eventsJson){
        try{
            SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
            JSONArray rec=new JSONArray(p.getString(KEY,"[]")); JSONArray ev=new JSONArray(eventsJson); long now=System.currentTimeMillis();
            for(int i=0;i<rec.length();i++){
                JSONObject r=rec.getJSONObject(i); long created=r.getLong("created"); double lat=r.getDouble("lat"),lon=r.getDouble("lon");
                JSONObject best=null; double bestMag=-99,bestKm=1e9;
                for(int j=0;j<ev.length();j++){
                    JSONObject e=ev.getJSONObject(j); long t=e.optLong("time",0); if(t<=created)continue; double mag=e.optDouble("mag",-99); if(mag<MIN_MAG)continue;
                    double km=haversine(lat,lon,e.getDouble("lat"),e.getDouble("lon")); if(km<=HIT_KM && (mag>bestMag || (mag==bestMag&&km<bestKm))){best=e;bestMag=mag;bestKm=km;}
                }
                if("OPEN".equals(r.optString("status24"))){
                    if(best!=null && best.optLong("time",0)<=r.getLong("deadline24")){r.put("status24","HIT");attach(r,"24",best,bestKm);}
                    else if(now>r.getLong("deadline24")) r.put("status24","MISS");
                }
                if("OPEN".equals(r.optString("status7"))){
                    if(best!=null && best.optLong("time",0)<=r.getLong("deadline7")){r.put("status7","HIT");attach(r,"7",best,bestKm);}
                    else if(now>r.getLong("deadline7")) r.put("status7","MISS");
                }
            }
            p.edit().putString(KEY,rec.toString()).apply();
        }catch(Exception ignored){}
    }

    public static String report(Context c){
        try{
            JSONArray a=new JSONArray(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"[]"));
            int h24=0,m24=0,o24=0,h7=0,m7=0,o7=0; double brier24=0; int bN=0;
            StringBuilder s=new StringBuilder("TAHMİN DENETİM RAPORU\n\n");
            for(int i=0;i<a.length();i++){
                JSONObject r=a.getJSONObject(i); String s24=r.optString("status24","OPEN"),s7=r.optString("status7","OPEN");
                if("HIT".equals(s24))h24++; else if("MISS".equals(s24))m24++; else o24++;
                if("HIT".equals(s7))h7++; else if("MISS".equals(s7))m7++; else o7++;
                if(!"OPEN".equals(s24)){double p=Math.max(0,Math.min(1,r.optDouble("q24",0)/100.0));double y="HIT".equals(s24)?1:0;brier24+=(p-y)*(p-y);bN++;}
            }
            s.append("24 saat: isabet ").append(h24).append(" • kaçırma ").append(m24).append(" • açık ").append(o24).append("\n");
            s.append("7 gün: isabet ").append(h7).append(" • kaçırma ").append(m7).append(" • açık ").append(o7).append("\n");
            if(h24+m24>0)s.append(String.format(Locale.US,"24s hit-rate: %.1f%%\n",100.0*h24/(h24+m24)));
            if(bN>0)s.append(String.format(Locale.US,"24s Brier skoru: %.3f (düşük daha iyi)\n",brier24/bN));
            s.append("\nSONUÇLANAN SON KAYITLAR\n");
            int shown=0;
            for(int i=a.length()-1;i>=0&&shown<8;i--){JSONObject r=a.getJSONObject(i);String st=r.optString("status24","OPEN");if("OPEN".equals(st))continue;shown++;
                s.append(shown).append(") ").append(String.format(Locale.US,"%.3f, %.3f",r.getDouble("lat"),r.getDouble("lon")))
                 .append(" • risk ").append(String.format(Locale.US,"%.1f",r.optDouble("q24",r.optDouble("score",0)))).append("/100 • ").append("HIT".equals(st)?"İSABET":"İSABET YOK").append("\n")
                 .append("   Gerekçe: oran=").append(String.format(Locale.US,"%.2fx",r.optDouble("rate",0))).append(", b≈").append(String.format(Locale.US,"%.2f",r.optDouble("b",1))).append(", ETAS=").append(String.format(Locale.US,"%.2f",r.optDouble("etas",0))).append(", göç=").append(String.format(Locale.US,"%.0f%%",100*r.optDouble("migration",0))).append(", güven=").append(String.format(Locale.US,"%.0f%%",r.optDouble("confidence",0))).append("\n");
                if("HIT".equals(st))s.append("   Sonradan gözlenen: M").append(String.format(Locale.US,"%.1f",r.optDouble("hitMag24",0))).append(" • yaklaşık ").append(String.format(Locale.US,"%.0f km",r.optDouble("hitKm24",0))).append(" • ").append(r.optString("hitPlace24","" )).append("\n");
            }
            s.append("\nKriter: 24 saat/7 gün penceresinde risk merkezinin 75 km çevresinde M≥3.5 katalog olayı. Bu eşik performans denetimi içindir; deprem tahmininin bilimsel olarak kanıtlandığı anlamına gelmez.");
            return s.toString();
        }catch(Exception e){return "Tahmin raporu henüz oluşturulamadı: "+e.getMessage();}
    }

    public static boolean hasNewHit(Context c){try{JSONArray a=new JSONArray(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"[]"));for(int i=Math.max(0,a.length()-20);i<a.length();i++){JSONObject r=a.getJSONObject(i);if("HIT".equals(r.optString("status24"))&&!r.optBoolean("notified",false)){r.put("notified",true);c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply();return true;}}}catch(Exception ignored){}return false;}

    private static void attach(JSONObject r,String h,JSONObject e,double km)throws Exception{r.put("hitMag"+h,e.optDouble("mag",0));r.put("hitKm"+h,km);r.put("hitPlace"+h,e.optString("place",""));r.put("hitTime"+h,e.optLong("time",0));}
    private static JSONArray trim(JSONArray a,int max)throws Exception{JSONArray b=new JSONArray();int start=Math.max(0,a.length()-max);for(int i=start;i<a.length();i++)b.put(a.get(i));return b;}
    private static double haversine(double a,double o,double b,double p){double R=6371.0,dLat=Math.toRadians(b-a),dLon=Math.toRadians(p-o);double q=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(b))*Math.sin(dLon/2)*Math.sin(dLon/2);return 2*R*Math.asin(Math.sqrt(q));}
    private PredictionAudit(){}
}
