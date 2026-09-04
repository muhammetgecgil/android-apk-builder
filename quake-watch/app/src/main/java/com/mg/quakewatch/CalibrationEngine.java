package com.mg.quakewatch;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public final class CalibrationEngine {
    private static final String PREF="quake_prediction_audit", KEY="records";

    public static String report(Context c){
        try{
            JSONArray a=new JSONArray(c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"[]"));
            int closed=0,hits=0,miss=0; double kmSum=0,timeSum=0;int kmN=0,tN=0;
            int[] kmHit=new int[4], kmTot=new int[4]; double[] kmThr={25,50,75,100};
            int[] calN=new int[5],calY=new int[5];
            int[] timeBins=new int[4];
            int[] magHit=new int[3],magTot=new int[3]; double[] magThr={3,4,5};
            StringBuilder detail=new StringBuilder(); int shown=0;
            for(int i=0;i<a.length();i++){
                JSONObject r=a.getJSONObject(i); String st=r.optString("status24","OPEN"); if("OPEN".equals(st))continue;
                closed++; boolean hit="HIT".equals(st); if(hit)hits++; else miss++;
                double p=Math.max(0,Math.min(100,r.optDouble("q24",r.optDouble("score",0)))); int cb=Math.min(4,(int)(p/20.0)); calN[cb]++; if(hit)calY[cb]++;
                for(int k=0;k<4;k++){kmTot[k]++; if(hit && r.optDouble("hitKm24",1e9)<=kmThr[k])kmHit[k]++;}
                if(hit){double km=r.optDouble("hitKm24",Double.NaN); if(!Double.isNaN(km)){kmSum+=km;kmN++;}
                    long ct=r.optLong("created",0),ht=r.optLong("hitTime24",0); if(ct>0&&ht>ct){double hrs=(ht-ct)/3600000.0;timeSum+=hrs;tN++; if(hrs<=6)timeBins[0]++; else if(hrs<=24)timeBins[1]++; else if(hrs<=72)timeBins[2]++; else timeBins[3]++;}
                    double hm=r.optDouble("hitMag24",0); for(int m=0;m<3;m++){magTot[m]++;if(hm>=magThr[m])magHit[m]++;}
                }
            }
            for(int i=a.length()-1;i>=0&&shown<6;i--){JSONObject r=a.getJSONObject(i);String st=r.optString("status24","OPEN");if("OPEN".equals(st))continue;shown++;detail.append("\n").append(shown).append(") ").append("HIT".equals(st)?"İSABET":"YANLIŞ ALARM").append(" • Q24=").append(String.format(Locale.US,"%.0f",r.optDouble("q24",0))).append(" • ").append(String.format(Locale.US,"%.3f, %.3f",r.optDouble("lat",0),r.optDouble("lon",0)));if("HIT".equals(st))detail.append(" • M").append(String.format(Locale.US,"%.1f",r.optDouble("hitMag24",0))).append(" • ").append(String.format(Locale.US,"%.0f km",r.optDouble("hitKm24",0)));
            }
            StringBuilder s=new StringBuilder();
            s.append("STAGE 8 • KALİBRASYON / HATA RAPORU\n\n");
            s.append("Kapalı tahmin: ").append(closed).append(" • isabet ").append(hits).append(" • yanlış alarm ").append(miss).append("\n");
            if(closed>0)s.append(String.format(Locale.US,"24s hit-rate: %.1f%%\n",100.0*hits/closed));
            if(kmN>0)s.append(String.format(Locale.US,"Ortalama konum hatası: %.1f km\n",kmSum/kmN));
            if(tN>0)s.append(String.format(Locale.US,"Ortalama zaman hatası: %.1f saat\n",timeSum/tN));
            s.append("\nKONUM EŞİKLERİ\n");for(int k=0;k<4;k++)s.append((int)kmThr[k]).append(" km: ").append(kmHit[k]).append("/").append(kmTot[k]).append("\n");
            s.append("\nZAMAN BANTLARI (isabetler)\n0–6s: ").append(timeBins[0]).append(" • 6–24s: ").append(timeBins[1]).append(" • 1–3g: ").append(timeBins[2]).append(" • 3–7g: ").append(timeBins[3]).append("\n");
            s.append("\nMAGNITUDE TIER (gözlenen isabet olayı)\n");for(int m=0;m<3;m++)s.append("M≥").append((int)magThr[m]).append(": ").append(magHit[m]).append("/").append(magTot[m]).append("\n");
            s.append("\nKALİBRASYON BANTLARI\n");for(int b=0;b<5;b++){int lo=b*20,hi=b==4?100:(b+1)*20;if(calN[b]>0)s.append(lo).append("–").append(hi).append(": tahmin n=").append(calN[b]).append(" • gözlenen=").append(String.format(Locale.US,"%.0f%%",100.0*calY[b]/calN[b])).append("\n");else s.append(lo).append("–").append(hi).append(": veri yok\n");}
            s.append("\nSON KAPALI KAYITLAR").append(detail);
            s.append("\n\nNot: Bu rapor model doğrulaması içindir; kesin kısa vadeli deprem tahmini iddiası değildir.");
            return s.toString();
        }catch(Exception e){return "Kalibrasyon raporu oluşturulamadı: "+e.getMessage();}
    }
    private CalibrationEngine(){}
}
