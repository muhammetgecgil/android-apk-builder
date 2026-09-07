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

public class Stage7Activity extends Activity {
    private final int BG=Color.rgb(5,9,16), PANEL=Color.rgb(13,22,36), TEXT=Color.rgb(239,245,255), MUTED=Color.rgb(151,170,194), CYAN=Color.rgb(82,202,255), GREEN=Color.rgb(74,226,162), GOLD=Color.rgb(255,202,101), RED=Color.rgb(255,83,104), PURPLE=Color.rgb(171,129,255);
    private TextView report, status;

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);build();refresh();}
    @Override protected void onResume(){super.onResume();refresh();}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=20){root.setOnApplyWindowInsetsListener((v,in)->{int t=0,bt=0;if(Build.VERSION.SDK_INT>=30){t=in.getInsets(WindowInsets.Type.statusBars()).top;bt=in.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{t=in.getSystemWindowInsetTop();bt=in.getSystemWindowInsetBottom();}v.setPadding(0,t,0,bt);return in;});root.requestApplyInsets();}
        TextView title=t("QUAKE WATCH • AŞAMA 7",25,TEXT);title.setTypeface(Typeface.DEFAULT_BOLD);title.setPadding(dp(16),dp(14),dp(16),dp(3));root.addView(title);
        TextView sub=t("OPERATIONAL FORECAST AUDIT • PREDICTION PASSPORT 2.0",11,CYAN);sub.setPadding(dp(16),0,dp(16),dp(7));root.addView(sub);
        status=t("● Tahmin denetimi hazırlanıyor",12,GREEN);status.setPadding(dp(16),0,dp(16),dp(10));root.addView(status);

        ScrollView sv=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),0,dp(12),dp(16));sv.addView(body);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        card(body,"PREDICTION PASSPORT 2.0","Her tahmin zaman, koordinat, 24s/7g skor, güven, ETAS, b-değeri, aktivite oranı ve göç metriğiyle önceden kaydedilir. Sonuç görüldükten sonra geçmiş kayıt değiştirilmez.",CYAN);
        card(body,"OTOMATİK KAPANIŞ","24 saat ve 7 günlük pencereler katalogla karşılaştırılır. Kriter sağlanırsa İSABET, süre dolar ve olay yoksa İSABET YOK olarak kapanır.",GREEN);
        card(body,"YANLIŞ ALARM MATRİSİ","İsabet, kaçırma, açık tahmin, hit-rate ve Brier skoru birlikte raporlanır. Yalnız başarılı tahminleri gösteren seçici raporlama yapılmaz.",GOLD);
        card(body,"NEDEN FİKRİ DEĞİŞTİ?","Yeni skorun gerekçeleri aktivite oranı, b-değeri, ETAS, göç ve güven parametreleriyle gösterilir. Amaç skor değişimini kullanıcıya denetlenebilir hale getirmektir.",PURPLE);
        card(body,"KAÇIRILAN OLAY ANALİZİ","Bir tahmin penceresi kapandığında modelin risk verdiği fakat kriteri karşılayan olay oluşmayan kayıtlar ayrı tutulur. Sonraki model kalibrasyonunda yanlış alarm maliyetine dahil edilir.",RED);

        LinearLayout audit=new LinearLayout(this);audit.setOrientation(LinearLayout.VERTICAL);audit.setPadding(dp(14),dp(13),dp(14),dp(13));audit.setBackgroundColor(PANEL);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.setMargins(0,0,0,dp(10));body.addView(audit,ap);
        TextView ah=t("CANLI DENETİM ÖZETİ",17,TEXT);ah.setTypeface(Typeface.DEFAULT_BOLD);audit.addView(ah);report=t("Rapor hazırlanıyor...",13,TEXT);report.setPadding(0,dp(9),0,0);audit.addView(report);
        TextView note=t("Bilimsel not: Bu performans denetimi operasyonel olasılık/araştırma tahminlerini değerlendirir; güvenilir kısa vadeli kesin deprem tahmini iddiası değildir.",11,MUTED);note.setPadding(dp(4),dp(8),dp(4),dp(6));body.addView(note);

        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(7),dp(6),dp(7),dp(9));nav.setBackgroundColor(Color.rgb(9,16,27));
        Button refresh=btn("YENİLE",GREEN), full=btn("RAPOR",CYAN), s6=btn("AŞAMA 6",GOLD), radar=btn("RADAR",PURPLE);
        nav.addView(refresh,lp());nav.addView(full,lp());nav.addView(s6,lp());nav.addView(radar,lp());root.addView(nav);
        refresh.setOnClickListener(v->refresh());
        full.setOnClickListener(v->startActivity(new Intent(this,ForecastReportActivity.class)));
        s6.setOnClickListener(v->startActivity(new Intent(this,Stage6Activity.class)));
        radar.setOnClickListener(v->startActivity(new Intent(this,PremiumResearchActivity.class)));
        setContentView(root);
    }

    private void refresh(){if(report!=null)report.setText(PredictionAudit.report(this));if(status!=null)status.setText("● Kayıtlar güncel • değiştirilemez geçmiş denetimi aktif");}
    private void card(LinearLayout p,String h,String s,int c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(14),dp(13),dp(14),dp(13));x.setBackgroundColor(PANEL);LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-1,-2);q.setMargins(0,0,0,dp(9));p.addView(x,q);TextView a=t(h,17,TEXT);a.setTypeface(Typeface.DEFAULT_BOLD);x.addView(a);TextView b=t(s,12,MUTED);b.setPadding(0,dp(6),0,0);x.addView(b);TextView tag=t("● STAGE 7",10,c);tag.setPadding(0,dp(7),0,0);x.addView(tag);}
    private TextView t(String s,int sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setLineSpacing(0,1.12f);return v;}
    private Button btn(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(c);b.setTextSize(10);b.setBackgroundColor(PANEL);return b;}
    private LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(0,dp(52),1);}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
}
