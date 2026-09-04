package com.mg.quakewatch;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public final class CalibrationMetrics {
    private static final String PREF="quake_prediction_audit", KEY="records";
    private static final long H24=24L*3600_000L;

    public static String report(Context c,String eventsJson){
        try{
            PredictionAudit.verifyAgainstCatalog(c,eventsJson);
            JSONArray rec=new JSONArray(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"[]"));
            JSONArray ev=new JSONArray(eventsJson);
            if(rec.length()==0)return "Henüz yeterli Prediction Passport kaydı yok. Önce tahminlerin zaman içinde birikmesi gerekiyor.";
            int closed=0;int[] spatial=new int[4];int[] timeBins=new int[4];double[] radii={25,50,75,100};
            int[] calN=new int[5],calY=new int[5];double[] calP=new double[5];
            int[] tierEvents=new int[3],tierCovered=new int[3];double[] tiers={3,4,5};
            long earliest=Long.MAX_VALUE;
            for(int i=0;i<rec.length();i++)earliest=Math.min(earliest,rec.getJSONObject(i).optLong("created",Long.MAX_VALUE));
            for(int i=0;i<rec.length();i++){
                JSONObject r=rec.getJSONObject(i);long created=r.optLong("created",0);if(created<=0)continue;
                String st=r.optString("status24","OPEN");if("OPEN".equals(st))continue;closed++;
                double lat=r.optDouble("lat"),lon=r.optDouble("lon"),p=Math.max(0,Math.min(100,r.optDouble("q24",0)));
                JSONObject best=null;double bestD=1e9;long bestT=0;
                for(int j=0;j<ev.length();j++){JSONObject e=ev.getJSONObject(j);long t=e.optLong("time",0);if(t<=created||t>created+H24||e.optDouble("mag",-9)<3.5)continue;double d=hav(lat,lon,e.optDouble("lat"),e.optDouble("lon"));if(d<bestD){bestD=d;best=e;bestT=t;}}
                boolean y=best!=null&&bestD<=75;
                int bin=Math.min(4,(int)(p/20));calN[bin]++;calP[bin]+=p;calY[bin]+=y?1:0;
                if(best!=null){for(int k=0;k<radii.length;k++)if(bestD<=radii[k])spatial[k]++;double h=(bestT-created)/3600000.0;if(h<=6)timeBins[0]++;else if(h<=24)timeBins[1]++;else if(h<=72)timeBins[2]++;else timeBins[3]++;}
            }
            // Missed-event coverage: every catalog event after earliest stored forecast is checked against a prior <=24h passport.
            for(int j=0;j<ev.length();j++){
                JSONObject e=ev.getJSONObject(j);long t=e.optLong("time",0);double m=e.optDouble("mag",-9);if(t<earliest)continue;
                for(int k=0;k<tiers.length;k++)if(m>=tiers[k]){tierEvents[k]++;boolean covered=false;for(int i=0;i<rec.length();i++){JSONObject r=rec.getJSONObject(i);long cr=r.optLong("created",0);if(cr<=0||cr>=t||t-cr>H24||r.optDouble("q24",0)<60)continue;double d=hav(r.optDouble("lat"),r.optDouble("lon"),e.optDouble("lat"),e.optDouble("lon"));if(d<=75){covered=true;break;}}if(covered)tierCovered[k]++;}
            }
            StringBuilder s=new StringBuilder();
            s.append("STAGE 8 • CANLI KALİBRASYON RAPORU\n\n");
            s.append("Kapalı 24s passport: ").append(closed).append("\n");
            if(closed>0){s.append("Mekânsal isabet merdiveni (kapalı passport):\n");for(int k=0;k<4;k++)s.append(String.format(Locale.US,"  ≤%.0f km: %d / %d (%.1f%%)\n",radii[k],spatial[k],closed,100.0*spatial[k]/closed));
                s.append("Zaman bantları (ilk M≥3.5 aday olay):\n").append("  0–6s: ").append(timeBins[0]).append(" • 6–24s: ").append(timeBins[1]).append(" • 1–3g: ").append(timeBins[2]).append(" • >3g: ").append(timeBins[3]).append("\n");}
            s.append("\nMagnitude-tier kapsama (QIE24≥60, ≤75 km, önceki 24s):\n");for(int k=0;k<3;k++){int n=tierEvents[k],h=tierCovered[k];s.append(String.format(Locale.US,"  M≥%.0f: %d/%d",tiers[k],h,n));if(n>0)s.append(String.format(Locale.US," (%.1f%%)",100.0*h/n));s.append("\n");}
            s.append("\nKalibrasyon bantları (tahmin ort. → gözlenen frekans):\n");for(int b=0;b<5;b++){if(calN[b]==0)continue;s.append(String.format(Locale.US,"  %d–%d: %.1f → %.1f%%  (n=%d)\n",b*20,(b+1)*20,calP[b]/calN[b],100.0*calY[b]/calN[b],calN[b]));}
            if(closed<30)s.append("\nKalibrasyon kapısı: örnek sayısı düşük. Otomatik skor düzeltmesi uygulanmıyor.");else s.append("\nKalibrasyon kapısı: yeterli örnek oluşmaya başladı; ham skor korunarak ayrı kalibre skor üretilebilir.");
            s.append("\n\nBu rapor model doğrulamasıdır; kesin deprem tahmini değildir.");
            return s.toString();
        }catch(Exception e){return "Stage 8 metrik hatası: "+e.getMessage();}
    }
    private static double hav(double a,double o,double b,double p){double R=6371,dLat=Math.toRadians(b-a),dLon=Math.toRadians(p-o);double q=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(b))*Math.sin(dLon/2)*Math.sin(dLon/2);return 2*R*Math.asin(Math.sqrt(q));}
    private CalibrationMetrics(){}
}
