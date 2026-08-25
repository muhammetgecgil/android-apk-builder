package com.mg.quakewatch;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/** Stage 8 validation metrics. Reads immutable forecast records and compares them to catalog events. */
public final class Stage8MetricsEngine {
    private static final String PREF="quake_prediction_audit", KEY="records";
    private static final double[] RADII={25,50,75,100};
    private static final double[] MAGS={3,4,5};
    private static final long D7=7L*24L*3600_000L;

    public static String report(Context c,String eventsJson){
        try{
            JSONArray rec=new JSONArray(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"[]"));
            JSONArray ev=new JSONArray(eventsJson==null?"[]":eventsJson);
            long now=System.currentTimeMillis(), since=now-D7;
            StringBuilder out=new StringBuilder("STAGE 8 • VALIDATION MATRIX\n\n");

            out.append("MAGNITUDE TIER / FALSE-NEGATIVE\n");
            for(double minMag:MAGS){
                int total=0,covered=0;
                for(int j=0;j<ev.length();j++){
                    JSONObject e=ev.optJSONObject(j); if(e==null)continue;
                    long t=e.optLong("time",0); double mag=e.optDouble("mag",-99);
                    if(t<since||t>now||mag<minMag)continue; total++;
                    if(coveredByForecast(rec,e,75,t))covered++;
                }
                int missed=Math.max(0,total-covered);
                out.append(String.format(Locale.US,"M≥%.0f: katalog %d • kapsanan %d • kaçırılan %d",minMag,total,covered,missed));
                if(total>0)out.append(String.format(Locale.US," • recall %.1f%%",100.0*covered/total));
                out.append("\n");
            }

            out.append("\nKONUM HATA MERDİVENİ (sonuçlanmış HIT kayıtları)\n");
            int hitN=0; double sumKm=0,sumHours=0; int timeN=0;
            int[] rc=new int[RADII.length]; int[] tb=new int[4];
            for(int i=0;i<rec.length();i++){
                JSONObject r=rec.optJSONObject(i);if(r==null||!"HIT".equals(r.optString("status24")))continue;
                double km=r.optDouble("hitKm24",Double.NaN); if(Double.isNaN(km))continue;
                hitN++;sumKm+=km;for(int k=0;k<RADII.length;k++)if(km<=RADII[k])rc[k]++;
                long ht=r.optLong("hitTime24",0),ct=r.optLong("created",0);if(ht>ct){double hrs=(ht-ct)/3600000.0;sumHours+=hrs;timeN++;if(hrs<=6)tb[0]++;else if(hrs<=24)tb[1]++;else if(hrs<=72)tb[2]++;else tb[3]++;}
            }
            for(int k=0;k<RADII.length;k++)out.append(String.format(Locale.US,"≤%.0f km: %d/%d",RADII[k],rc[k],hitN)).append(hitN>0?String.format(Locale.US," (%.1f%%)\n",100.0*rc[k]/hitN):"\n");
            if(hitN>0)out.append(String.format(Locale.US,"Ortalama konum hatası: %.1f km\n",sumKm/hitN));

            out.append("\nZAMAN HATASI\n");
            out.append("0–6 saat: ").append(tb[0]).append(" • 6–24 saat: ").append(tb[1]).append(" • 1–3 gün: ").append(tb[2]).append(" • 3–7 gün: ").append(tb[3]).append("\n");
            if(timeN>0)out.append(String.format(Locale.US,"Ortalama ilk-isabet süresi: %.1f saat\n",sumHours/timeN));

            int[] n=new int[5], y=new int[5];
            for(int i=0;i<rec.length();i++){
                JSONObject r=rec.optJSONObject(i);if(r==null)continue;String st=r.optString("status24","OPEN");if("OPEN".equals(st))continue;
                double p=Math.max(0,Math.min(100,r.optDouble("q24",r.optDouble("score",0))));int bin=Math.min(4,(int)(p/20.0));n[bin]++;if("HIT".equals(st))y[bin]++;
            }
            out.append("\nKALİBRASYON EĞRİSİ\n");
            for(int b=0;b<5;b++){int lo=b*20,hi=b==4?100:(b+1)*20;out.append(lo).append("–").append(hi).append(": n=").append(n[b]);if(n[b]>0)out.append(String.format(Locale.US," • gözlenen %.1f%%",100.0*y[b]/n[b]));out.append("\n");}
            int closed=0;for(int v:n)closed+=v;
            out.append("Kalibrasyon kapısı: ").append(closed>=50?"ÖRNEK SAYISI YETERLİ — kalibrasyon adayı hesaplanabilir":"BEKLEMEDE — en az 50 kapanmış 24s tahmin hedefi").append(" (n=").append(closed).append(")\n");

            out.append("\nSKOR MUHASEBESİ\n");
            if(rec.length()>=2){JSONObject a=rec.optJSONObject(rec.length()-2),b=rec.optJSONObject(rec.length()-1);if(a!=null&&b!=null){double d=b.optDouble("q24",0)-a.optDouble("q24",0);out.append(String.format(Locale.US,"Son iki passport farkı: %+.1f puan\n",d));out.append(String.format(Locale.US,"Aktivite oranı Δ %+.2f • b Δ %+.2f • ETAS Δ %+.2f • göç Δ %+.1f%%\n",b.optDouble("rate",0)-a.optDouble("rate",0),b.optDouble("b",1)-a.optDouble("b",1),b.optDouble("etas",0)-a.optDouble("etas",0),100*(b.optDouble("migration",0)-a.optDouble("migration",0))));}}
            else out.append("Skor değişimi için en az iki passport gerekli.\n");

            out.append("\nBu metrikler model doğrulaması içindir; kesin kısa vadeli deprem tahmini iddiası değildir.");
            return out.toString();
        }catch(Exception e){return "Stage 8 metrikleri hesaplanamadı: "+e.getMessage();}
    }

    private static boolean coveredByForecast(JSONArray rec,JSONObject e,double kmLimit,long eventTime){
        double lat=e.optDouble("lat",999),lon=e.optDouble("lon",999);
        for(int i=0;i<rec.length();i++){
            JSONObject r=rec.optJSONObject(i);if(r==null)continue;long created=r.optLong("created",0),deadline=r.optLong("deadline7",created+D7);
            if(created<=0||eventTime<=created||eventTime>deadline)continue;
            if(haversine(r.optDouble("lat",999),r.optDouble("lon",999),lat,lon)<=kmLimit)return true;
        }
        return false;
    }
    private static double haversine(double a,double o,double b,double p){double R=6371,dLat=Math.toRadians(b-a),dLon=Math.toRadians(p-o),q=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(b))*Math.sin(dLon/2)*Math.sin(dLon/2);return 2*R*Math.asin(Math.sqrt(q));}
    private Stage8MetricsEngine(){}
}
