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

public class Stage8Activity extends Activity {
 private final int BG=Color.rgb(4,8,15),P=Color.rgb(13,22,36),T=Color.rgb(240,246,255),M=Color.rgb(151,170,194),C=Color.rgb(82,202,255),G=Color.rgb(74,226,162),Y=Color.rgb(255,202,101),R=Color.rgb(255,83,104),V=Color.rgb(171,129,255);
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);build();}
 private void build(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
  if(Build.VERSION.SDK_INT>=20){root.setOnApplyWindowInsetsListener((v,i)->{int top=0,bot=0;if(Build.VERSION.SDK_INT>=30){top=i.getInsets(WindowInsets.Type.statusBars()).top;bot=i.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{top=i.getSystemWindowInsetTop();bot=i.getSystemWindowInsetBottom();}v.setPadding(0,top,0,bot);return i;});root.requestApplyInsets();}
  TextView h=t("QUAKE WATCH • AŞAMA 8",25,T);h.setTypeface(Typeface.DEFAULT_BOLD);h.setPadding(dp(16),dp(14),dp(16),dp(3));root.addView(h);
  TextView s=t("CALIBRATION LAB • ERROR GEOMETRY • MAGNITUDE TIERS",11,C);s.setPadding(dp(16),0,dp(16),dp(10));root.addView(s);
  ScrollView sv=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),0,dp(12),dp(16));sv.addView(body);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
  card(body,"KAÇIRILAN DEPREM ANALİZİ","Katalogda M≥3 / M≥4 / M≥5 olaylar ayrı incelenir. Önceden açılmış tahmin penceresi olayı kapsamıyorsa false-negative adayı olarak işaretlenir. Böylece yalnız yanlış alarmlar değil kaçırılan gerçek olaylar da ölçülür.",R);
  card(body,"KONUM HATA MERDİVENİ","25 km • 50 km • 75 km • 100 km eşiklerinde ayrı isabet oranları. Tek bir 75 km kriterine bağlı kalmadan modelin mekânsal keskinliği raporlanır.",C);
  card(body,"ZAMAN HATASI","Tahmin oluşturma zamanı ile ilk kriter olayı arasındaki saat farkı tutulur. 0–6s, 6–24s, 1–3g ve 3–7g bantları ayrı değerlendirilir.",G);
  card(body,"MAGNITUDE TIER SCORE","M≥3, M≥4 ve M≥5 için ayrı hit-rate, false-alarm ve örnek sayısı. Büyük olaylarda az örnek olduğunda sistem yüksek güven iddiasında bulunmaz.",Y);
  card(body,"KALİBRASYON EĞRİSİ","0–20 / 20–40 / 40–60 / 60–80 / 80–100 tahmin bantları gözlenen olay frekansıyla karşılaştırılır. 70/100 denilen olaylar yaklaşık %70 gerçekleşmiyorsa skor kalibrasyonsuz olarak işaretlenir.",V);
  card(body,"OTOMATİK KALİBRASYON KAPISI","Kalibrasyon yalnız yeterli kapalı örnek sayısı olduğunda önerilir. Ham model skoru korunur; kalibre skor ayrı alan olarak gösterilir. Böylece model geçmişi geriye dönük yeniden yazılmaz.",C);
  card(body,"SKOR DEĞİŞİM MUHASEBESİ","Dün 42 → bugün 68 gibi değişimler; aktivite, ETAS, b-değeri, göç, veri sağlığı ve varsa doğrulanmış deformasyon katkılarına ayrılır. Her katkı artı/eksi puan olarak açıklanabilir.",G);
  TextView n=t("Bilimsel sınır: Bu metrikler model doğrulama araçlarıdır. Kısa vadeli kesin deprem yeri/zamanı tahmininin güvenilir biçimde mümkün olduğunu göstermez.",11,M);n.setPadding(dp(5),dp(8),dp(5),dp(6));body.addView(n);
  LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(7),dp(6),dp(7),dp(9));nav.setBackgroundColor(Color.rgb(9,16,27));Button a=btn("AŞAMA 7",Y),r=btn("DENETİM",C),m=btn("RADAR",V);nav.addView(a,lp());nav.addView(r,lp());nav.addView(m,lp());root.addView(nav);a.setOnClickListener(v->startActivity(new Intent(this,Stage7Activity.class)));r.setOnClickListener(v->startActivity(new Intent(this,ForecastReportActivity.class)));m.setOnClickListener(v->startActivity(new Intent(this,PremiumResearchActivity.class)));setContentView(root);
 }
 private void card(LinearLayout p,String h,String s,int c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(14),dp(13),dp(14),dp(13));x.setBackgroundColor(P);LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-1,-2);q.setMargins(0,0,0,dp(9));p.addView(x,q);TextView a=t(h,17,T);a.setTypeface(Typeface.DEFAULT_BOLD);x.addView(a);TextView b=t(s,12,M);b.setPadding(0,dp(6),0,0);x.addView(b);TextView z=t("● STAGE 8",10,c);z.setPadding(0,dp(7),0,0);x.addView(z);}
 private TextView t(String s,int z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setLineSpacing(0,1.12f);return v;}private Button btn(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(c);b.setTextSize(10);b.setBackgroundColor(P);return b;}private LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(0,dp(52),1);}private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
}
