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

public class Stage6Activity extends Activity {
  private final int BG=Color.rgb(5,9,16),PANEL=Color.rgb(12,21,35),TEXT=Color.rgb(239,245,255),MUTED=Color.rgb(151,170,194),CYAN=Color.rgb(82,202,255),GREEN=Color.rgb(74,226,162),GOLD=Color.rgb(255,202,101),RED=Color.rgb(255,83,104);
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);build();}
  private void build(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
    if(Build.VERSION.SDK_INT>=20){root.setOnApplyWindowInsetsListener((v,in)->{int t=0,bt=0;if(Build.VERSION.SDK_INT>=30){t=in.getInsets(WindowInsets.Type.statusBars()).top;bt=in.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{t=in.getSystemWindowInsetTop();bt=in.getSystemWindowInsetBottom();}v.setPadding(0,t,0,bt);return in;});root.requestApplyInsets();}
    TextView title=txt("QUAKE WATCH • AŞAMA 6",25,TEXT);title.setTypeface(Typeface.DEFAULT_BOLD);title.setPadding(dp(16),dp(15),dp(16),dp(4));root.addView(title);
    TextView sub=txt("SPACE WATCH 2.0 • EARTH-SYSTEM FUSION LAB",12,CYAN);sub.setPadding(dp(16),0,dp(16),dp(12));root.addView(sub);
    ScrollView sv=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),0,dp(12),dp(16));sv.addView(body);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
    card(body,"UYDU / JEODEZİ","Sentinel-1 InSAR deformasyon zaman serisi\nGNSS hız / strain alanı\nUydu ölçüm güveni ve veri yaşı","BAĞLANTI KATMANI",CYAN);
    card(body,"UZAY HAVASI","NOAA Kp / jeomanyetik bağlam\nİyonosferik TEC araştırma kanalı\nGüneş aktivitesi bağlamı","DENEYSEL",GOLD);
    card(body,"YER-SİSTEM FÜZYONU","Gelgit fazı • atmosfer basıncı • sıcaklık\nTermal anomaliler • manyetik alan • gaz/radon araştırma kanalı","ANA QIE'DEN AYRI",GREEN);
    card(body,"BİLİMSEL GÜVEN KAPISI","Her kanal geçmiş katalogda kör backtest edilir. Katkı göstermeyen kanal tahmin skoruna ağırlık vermez. Korelasyon deprem öncüsü kabul edilmez.","BACKTEST ZORUNLU",RED);
    card(body,"FUSION SCORE","Sismik model = ana kanal\nGNSS/InSAR = bağımsız deformasyon kanalı\nDeneysel çevresel kanallar = yalnız araştırma göstergesi\nEksik/eski veri güveni otomatik düşürür.","ŞEFFAF AĞIRLIK",CYAN);
    TextView note=txt("Not: Bu ekran depremi kesin olarak önceden bildirdiğini iddia etmez. Kısa vadeli güvenilir deprem tahmini bilimsel olarak kurulmuş değildir; amaç hipotezleri ölçülebilir ve geriye dönük test edilebilir tutmaktır.",12,MUTED);note.setPadding(dp(12),dp(12),dp(12),dp(14));body.addView(note);
    LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(8),dp(7),dp(8),dp(9));Button prev=button("AŞAMA 5",GOLD),radar=button("RADAR",CYAN);nav.addView(prev,new LinearLayout.LayoutParams(0,dp(52),1));nav.addView(radar,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(nav);prev.setOnClickListener(v->startActivity(new Intent(this,Stage5Activity.class)));radar.setOnClickListener(v->startActivity(new Intent(this,PremiumResearchActivity.class)));setContentView(root);
  }
  private void card(LinearLayout p,String h,String s,String badge,int c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(14),dp(13),dp(14),dp(13));x.setBackgroundColor(PANEL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(9));p.addView(x,lp);TextView a=txt(h,17,TEXT);a.setTypeface(Typeface.DEFAULT_BOLD);x.addView(a);TextView b=txt(badge,10,c);b.setPadding(0,dp(4),0,dp(7));x.addView(b);x.addView(txt(s,13,MUTED));}
  private TextView txt(String s,int z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setLineSpacing(0,1.12f);return v;}
  private Button button(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(c);b.setTextSize(11);b.setBackgroundColor(PANEL);return b;}
  private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
}
