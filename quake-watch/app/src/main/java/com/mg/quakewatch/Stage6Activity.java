package com.mg.quakewatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public class Stage6Activity extends Activity {
  private final int BG=Color.rgb(5,9,16),PANEL=Color.rgb(12,21,35),TEXT=Color.rgb(239,245,255),MUTED=Color.rgb(151,170,194),CYAN=Color.rgb(82,202,255),GREEN=Color.rgb(74,226,162),GOLD=Color.rgb(255,202,101),RED=Color.rgb(255,83,104);
  private TextView live,health,ablation;
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);build();refresh();}
  private void build(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
    if(Build.VERSION.SDK_INT>=20){root.setOnApplyWindowInsetsListener((v,in)->{int t=0,bt=0;if(Build.VERSION.SDK_INT>=30){t=in.getInsets(WindowInsets.Type.statusBars()).top;bt=in.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{t=in.getSystemWindowInsetTop();bt=in.getSystemWindowInsetBottom();}v.setPadding(0,t,0,bt);return in;});root.requestApplyInsets();}
    TextView title=txt("QUAKE WATCH • AŞAMA 6",25,TEXT);title.setTypeface(Typeface.DEFAULT_BOLD);title.setPadding(dp(16),dp(15),dp(16),dp(4));root.addView(title);
    TextView sub=txt("SPACE WATCH 2.0 • EARTH-SYSTEM FUSION LAB",12,CYAN);sub.setPadding(dp(16),0,dp(16),dp(12));root.addView(sub);
    ScrollView sv=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),0,dp(12),dp(16));sv.addView(body);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

    live=card(body,"CANLI SPACE WATCH","NOAA Kp verisi alınıyor...","LIVE",GREEN);
    health=card(body,"VERİ SAĞLIĞI","Kaynak durumları hazırlanıyor...","DATA HEALTH",CYAN);
    card(body,"UYDU / JEODEZİ","Sentinel-1 InSAR deformasyon zaman serisi\nGNSS hız / strain alanı\nUydu ölçüm güveni ve veri yaşı","BAĞLANTI KATMANI",CYAN);
    card(body,"UZAY HAVASI","NOAA Kp / jeomanyetik bağlam\nİyonosferik TEC araştırma kanalı\nGüneş aktivitesi bağlamı","DENEYSEL",GOLD);
    card(body,"YER-SİSTEM FÜZYONU","Gelgit fazı • atmosfer basıncı • sıcaklık\nTermal anomaliler • manyetik alan • gaz/radon araştırma kanalı","ANA QIE'DEN AYRI",GREEN);
    ablation=card(body,"KANAL ABLATION / BACKTEST","Sismik ana model korunur. Bir çevresel kanal ancak geçmiş kör testte Brier/log-loss ve yanlış alarm açısından ek fayda gösterirse deneysel ağırlık adayı olur. Şu anda çevresel kanallar için doğrulanmış artı ağırlık = 0.","BACKTEST KAPISI",RED);
    card(body,"FUSION SCORE","Sismik model = ana kanal\nGNSS/InSAR = gerçek veri bağlanırsa bağımsız deformasyon kanalı\nKp/TEC/termal/gelgit = araştırma bağlamı\nEksik/eski veri güveni otomatik düşürür.","ŞEFFAF AĞIRLIK",CYAN);
    TextView note=txt("Bilimsel sınır: Bu sistem kesin yer-saat-büyüklük deprem tahmini yaptığını iddia etmez. Korelasyon nedensellik değildir; deneysel çevresel anomaliler tek başına alarm üretmez.",12,MUTED);note.setPadding(dp(12),dp(12),dp(12),dp(14));body.addView(note);

    LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(8),dp(7),dp(8),dp(9));Button refresh=button("YENİLE",GREEN),prev=button("AŞAMA 5",GOLD),radar=button("RADAR",CYAN);nav.addView(refresh,new LinearLayout.LayoutParams(0,dp(52),1));nav.addView(prev,new LinearLayout.LayoutParams(0,dp(52),1));nav.addView(radar,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(nav);
    refresh.setOnClickListener(v->refresh());prev.setOnClickListener(v->startActivity(new Intent(this,Stage5Activity.class)));radar.setOnClickListener(v->startActivity(new Intent(this,PremiumResearchActivity.class)));setContentView(root);
  }
  private void refresh(){
    live.setText("NOAA Kp verisi alınıyor...");health.setText("Kaynaklar kontrol ediliyor...");
    new Thread(()->{try{SatelliteResearchEngine.Report r=SatelliteResearchEngine.fetch();runOnUiThread(()->{
      live.setText(String.format(Locale.US,"NOAA planetary Kp: %.1f / 9\nUzay-çevre bağlam endeksi: %.1f / 100\nBu endeks deprem olasılığı değildir.",r.kp,r.spaceContext));
      health.setText("USGS deprem kataloğu: BAĞLI\nNOAA Kp: BAĞLI\nSentinel-1 InSAR ürün beslemesi: BAĞLI DEĞİL\nGNSS strain ağı: BAĞLI DEĞİL\nTEC: BAĞLI DEĞİL\nTermal IR: BAĞLI DEĞİL\n\nKural: bağlı olmayan kaynak için sahte değer üretilmez.");
    });}catch(Exception e){runOnUiThread(()->{live.setText("NOAA Kp alınamadı: "+e.getMessage());health.setText("USGS deprem kataloğu: uygulama analiz motorunda\nNOAA Kp: HATA / ERİŞİLEMİYOR\nInSAR/GNSS/TEC/Termal: BAĞLI DEĞİL\n\nEksik veri güveni yükseltmez.");});}}).start();
  }
  private TextView card(LinearLayout p,String h,String s,String badge,int c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(14),dp(13),dp(14),dp(13));x.setBackgroundColor(PANEL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(9));p.addView(x,lp);TextView a=txt(h,17,TEXT);a.setTypeface(Typeface.DEFAULT_BOLD);x.addView(a);TextView b=txt(badge,10,c);b.setPadding(0,dp(4),0,dp(7));x.addView(b);TextView v=txt(s,13,MUTED);x.addView(v);return v;}
  private TextView txt(String s,int z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setLineSpacing(0,1.12f);return v;}
  private Button button(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(c);b.setTextSize(11);b.setBackgroundColor(PANEL);return b;}
  private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
}
