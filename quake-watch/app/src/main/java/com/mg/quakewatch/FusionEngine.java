package com.mg.quakewatch;

import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public final class FusionEngine {
    private static final String KP_URL="https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json";

    public static final class Context {
        public final double kp, tidalProxy, magneticUt, infrastructureIndex, fusionIndex;
        public final String text;
        Context(double kp,double tide,double mag,double infra,double fusion,String text){
            this.kp=kp; this.tidalProxy=tide; this.magneticUt=mag; this.infrastructureIndex=infra; this.fusionIndex=fusion; this.text=text;
        }
    }

    public static Context fetch(double seismicScore,double magneticUt) throws Exception {
        double kp=fetchKp();
        double tide=tidalProxy();
        double magAnomaly=magneticContext(magneticUt);
        double infrastructure=clamp100(100.0*(0.72*clamp01(kp/9.0)+0.18*tide+0.10*magAnomaly));
        double fusion=clamp100(0.78*seismicScore + 0.22*infrastructure);

        StringBuilder s=new StringBuilder();
        s.append("ÇOKLU FÜZYON PANELİ\n");
        s.append(String.format(Locale.US,"• Sismik anomali: %.1f/100\n",seismicScore));
        s.append(String.format(Locale.US,"• NOAA gezegensel Kp: %.1f/9\n",kp));
        s.append(String.format(Locale.US,"• Gelgit potansiyeli göstergesi: %.0f/100\n",tide*100.0));
        if(magneticUt>0) s.append(String.format(Locale.US,"• Telefon manyetik alanı: %.1f µT\n",magneticUt));
        else s.append("• Telefon manyetik alanı: sensör verisi bekleniyor\n");
        s.append(String.format(Locale.US,"• Altyapı/çevresel etki endeksi: %.1f/100\n",infrastructure));
        s.append(String.format(Locale.US,"• Birleşik izleme endeksi: %.1f/100\n\n",fusion));

        s.append("OLASI ETKİLER\n");
        if(kp>=7) s.append("• Güçlü jeomanyetik aktivite: GNSS, HF haberleşme, uydu, elektrik şebekesi ve uzun iletken hatlarda bozulma riski artabilir.\n");
        else if(kp>=5) s.append("• Orta-yüksek jeomanyetik aktivite: GNSS/HF doğruluğu ve uzay-hava bağımlı sistemler izlenmeli.\n");
        else s.append("• Jeomanyetik koşullar düşük/orta seviyede.\n");
        if(tide>0.8) s.append("• Ay-Güneş gelgit geometrisi kuvvetli fazda; kıyı/liman operasyonlarında yerel gelgit verisi ayrıca kontrol edilmeli.\n");
        s.append("• Tsunami riski; deprem büyüklüğü, derinliği, mekanizması ve deniz tabanı yer değiştirmesiyle değerlendirilir. Bu endeks tek başına tsunami tahmini değildir.\n");
        s.append("• Sıcaklık, basınç, radon/CO₂/CH₄ gibi gazlar ancak güvenilir yerel sensör ve uzun dönem baz çizgisi varsa bağlam olarak kullanılmalıdır; telefon gaz sensörü yoksa veri üretilmez.\n\n");
        s.append("Bilimsel not: Kp, gelgit, sıcaklık veya telefon manyetometresi deprem için doğrulanmış öncü göstergeler değildir. Deprem sıcak-nokta puanı yalnız sismik katalog modelinden gelir; diğer katmanlar çevresel ve endüstriyel etki bağlamıdır.");
        return new Context(kp,tide,magneticUt,infrastructure,fusion,s.toString());
    }

    private static double fetchKp() throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(KP_URL).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(15000); c.setRequestProperty("User-Agent","QuakeWatch-Fusion/1.2");
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb=new StringBuilder(); String line;
        while((line=br.readLine())!=null) sb.append(line);
        br.close();
        JSONArray a=new JSONArray(sb.toString());
        for(int i=a.length()-1;i>=1;i--){
            JSONArray r=a.optJSONArray(i);
            if(r!=null && r.length()>1){
                try{return Double.parseDouble(r.getString(1));}catch(Exception ignored){}
            }
        }
        return 0;
    }

    private static double tidalProxy(){
        double days=(System.currentTimeMillis()-947182440000L)/86400000.0;
        double phase=(days%29.530588)/29.530588;
        if(phase<0) phase+=1.0;
        return 0.35+0.65*Math.abs(Math.cos(2.0*Math.PI*phase));
    }

    private static double magneticContext(double uT){
        if(uT<=0) return 0;
        if(uT>=25 && uT<=65) return 0.1;
        double d=uT<25?(25-uT):(uT-65);
        return clamp01(d/100.0);
    }

    private static double clamp01(double x){return Math.max(0,Math.min(1,x));}
    private static double clamp100(double x){return Math.max(0,Math.min(100,x));}
    private FusionEngine(){}
}
