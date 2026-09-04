package com.mg.quakewatch;

import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/** Satellite / space-weather research context. Real-time Kp is fetched from NOAA.
 * InSAR/GNSS channels are explicitly marked unavailable unless a real feed is connected;
 * no synthetic satellite deformation is fabricated.
 */
public final class SatelliteResearchEngine {
    private static final String KP_URL="https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json";
    public static final class Report { public final String text; public final double kp,spaceContext; Report(String t,double k,double s){text=t;kp=k;spaceContext=s;} }

    public static Report fetch() throws Exception {
        double kp=fetchKp(); double tide=tidalProxy();
        double context=100.0*Math.min(1.0,0.78*(kp/9.0)+0.22*tide);
        StringBuilder s=new StringBuilder();
        s.append("SPACE WATCH • UZAY / UYDU ARAŞTIRMA PANELİ\n\n");
        s.append(String.format(Locale.US,"NOAA Kp: %.1f / 9\n",kp));
        s.append(String.format(Locale.US,"Ay-Güneş gelgit geometrisi: %.0f / 100\n",tide*100.0));
        s.append(String.format(Locale.US,"Uzay-çevre bağlam endeksi: %.1f / 100\n\n",context));
        s.append("UYDU KANALLARI\n");
        s.append("• Sentinel-1 InSAR deformasyon: gerçek ürün beslemesi henüz bağlı değil; sahte mm/yıl değeri üretilmez.\n");
        s.append("• GNSS strain/slip-deficit: gerçek istasyon akışı bağlanınca ayrı güven katsayısıyla kullanılacak.\n");
        s.append("• Termal IR / yüzey sıcaklığı: deneysel kanal; tek başına deprem öncüsü kabul edilmez.\n");
        s.append("• İyonosferik TEC: deneysel kanal; ana sismik skora doğrudan karıştırılmaz.\n");
        s.append("• Tsunami: yalnız deniz içi deprem büyüklüğü, derinliği, mekanizması ve deniz tabanı hareketiyle değerlendirilir.\n\n");
        s.append("Bilimsel kural: Uydu/uzay verisi QIE sismik motorundan ayrı tutulur. Gerçek InSAR/GNSS kaynağı olmadan deformasyon tahmini yapılmaz.");
        return new Report(s.toString(),kp,context);
    }

    private static double fetchKp() throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(KP_URL).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","QuakeWatch-Space/2.0");
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder sb=new StringBuilder();String l;while((l=br.readLine())!=null)sb.append(l);br.close();
        JSONArray a=new JSONArray(sb.toString());for(int i=a.length()-1;i>=1;i--){JSONArray r=a.optJSONArray(i);if(r!=null&&r.length()>1){try{return Double.parseDouble(r.getString(1));}catch(Exception ignored){}}}return 0;
    }
    private static double tidalProxy(){double days=(System.currentTimeMillis()-947182440000L)/86400000.0;double phase=(days%29.530588)/29.530588;if(phase<0)phase+=1;return 0.35+0.65*Math.abs(Math.cos(2*Math.PI*phase));}
    private SatelliteResearchEngine(){}
}
