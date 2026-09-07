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
 private TextView live,status;
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);build();refresh();}
 @Override protected void onResume(){super.onResume();refresh();}
 private void build(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
  if(Build.VERSION.SDK_INT>=20){root.setOnApplyWindowInsetsListener((v,i)->{int top=0,bot=0;if(Build.VERSION.SDK_INT>=30){top=i.getInsets(WindowInsets.Type.statusBars()).top;bot=i.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{top=i.getSystemWindowInsetTop();bot=i.getSystemWindowInsetBottom();}v.setPadding(0,top,0,bot);return i;});root.requestApplyInsets();}
  TextView h=t("QUAKE WATCH • AŞAMA 8",25,T);h.setTypeface(Typeface.DEFAULT_BOLD);h.setPadding(dp(16),dp(14),dp(16),dp(3));root.addView(h);
  TextView s=t("CALIBRATION LAB • AFAD TÜRKİYE KATALOĞU • MAGNITUDE TIERS",11,C);s.setPadding(dp(16),0,dp(16),dp(5));root.addView(s);
  status=t("● AFAD Türkiye kataloğu ve passport kayıtları analiz ediliyor",11,G);status.setPadding(dp(16),0,dp(16),dp(9));root.addView(status);
  ScrollView sv=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),0,dp(12),dp(16));sv.addView(body);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
  card(body,"TÜRKİYE RESMİ KATALOG KANALI","Türkiye için AFAD Event Web Service birincil doğrulama kataloğudur. Son 7 gün Türkiye ve yakın çevre olayları çekilir. AFAD erişilemezse mevcut USGS Türkiye kutusu yedek katalog olarak kullanılır.",G);
  card(body,"KAÇIRILAN DEPREM ANALİZİ","Katalogda M≥3 / M≥4 / M≥5 olaylar ayrı incelenir. Önceden açılmış tahmin penceresi olayı kapsamıyorsa false-negative adayı olarak işaretlenir.",R);
  card(body,"KONUM + ZAMAN HATASI","25/50/75/100 km eşiklerinde mekânsal isabet; 0–6s, 6–24s, 1–3g ve 3–7g bantlarında zaman hatası ayrı raporlanır.",C);
  card(body,"KALİBRASYON EĞRİSİ","0–20 / 20–40 / 40–60 / 60–80 / 80–100 tahmin bantları gözlenen olay frekansıyla karşılaştırılır. Ham skor geçmişte değiştirilmez.",V);
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(13),dp(14),dp(13));box.setBackgroundColor(P);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.setMargins(0,0,0,dp(9));body.addView(box,bp);TextView bh=t("CANLI STAGE 8 METRİKLERİ",17,T);bh.setTypeface(Typeface.DEFAULT_BOLD);box.addView(bh);live=t("Hesaplanıyor...",12,T);live.setPadding(0,dp(8),0,0);box.addView(live);
  TextView n=t("Bilimsel sınır: AFAD/USGS katalogları gözlenen depremleri sağlar. Bunların modele dahil edilmesi kısa vadeli kesin deprem yeri/zamanı tahmininin mümkün olduğunu göstermez.",11,M);n.setPadding(dp(5),dp(8),dp(5),dp(6));body.addView(n);
  LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(7),dp(6),dp(7),dp(9));nav.setBackgroundColor(Color.rgb(9,16,27));Button refresh=btn("YENİLE",G),a=btn("AŞAMA 7",Y),r=btn("DENETİM",C),m=btn("RADAR",V);nav.addView(refresh,lp());nav.addView(a,lp());nav.addView(r,lp());nav.addView(m,lp());root.addView(nav);refresh.setOnClickListener(v->refresh());a.setOnClickListener(v->startActivity(new Intent(this,Stage7Activity.class)));r.setOnClickListener(v->startActivity(new Intent(this,ForecastReportActivity.class)));m.setOnClickListener(v->startActivity(new Intent(this,PremiumResearchActivity.class)));setContentView(root);
 }
 private void refresh(){
  if(status!=null)status.setText("● AFAD Türkiye kataloğu indiriliyor • geçmiş passport kayıtları korunuyor");
  new Thread(()->{
   try{
    AfadTurkeyCatalog.Report afad=AfadTurkeyCatalog.fetchLast7Days();
    String x=CalibrationMetrics.report(this,afad.eventsJson);
    runOnUiThread(()->{live.setText("Kaynak: AFAD • "+afad.count+" olay\n\n"+x);status.setText("● AFAD aktif • "+afad.count+" Türkiye olayı • Stage 8 güncel");});
   }catch(Exception afadErr){
    try{
     TurkeyAnalyzer.Report rep=TurkeyAnalyzer.fetchAndAnalyze();
     String x=CalibrationMetrics.report(this,rep.eventsJson);
     runOnUiThread(()->{live.setText("Kaynak: USGS Türkiye yedek katalog • "+rep.eventCount+" olay\n\n"+x);status.setText("● AFAD alınamadı • USGS yedek aktif • "+rep.eventCount+" olay");});
    }catch(Exception e){
     String x=CalibrationMetrics.report(this,"[]");
     runOnUiThread(()->{live.setText(x);status.setText("● AFAD ve USGS katalogları alınamadı • yerel passport metrikleri");});
    }
   }
  }).start();
 }
 private void card(LinearLayout p,String h,String s,int c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(14),dp(13),dp(14),dp(13));x.setBackgroundColor(P);LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-1,-2);q.setMargins(0,0,0,dp(9));p.addView(x,q);TextView a=t(h,17,T);a.setTypeface(Typeface.DEFAULT_BOLD);x.addView(a);TextView b=t(s,12,M);b.setPadding(0,dp(6),0,0);x.addView(b);TextView z=t("● STAGE 8",10,c);z.setPadding(0,dp(7),0,0);x.addView(z);}
 private TextView t(String s,int z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setLineSpacing(0,1.12f);return v;}private Button btn(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(c);b.setTextSize(10);b.setBackgroundColor(P);return b;}private LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(0,dp(52),1);}private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
}
