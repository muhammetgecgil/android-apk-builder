package com.mg.quakewatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Stage1Activity extends Activity {
    private LinearLayout body; private TextView status;
    private static final int BG=Color.rgb(5,9,16),CARD=Color.rgb(16,25,40),TEXT=Color.rgb(238,244,255),MUTED=Color.rgb(143,165,194),CYAN=Color.rgb(82,202,255),GREEN=Color.rgb(74,226,162),GOLD=Color.rgb(255,202,101);
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(Color.rgb(8,14,24));build();run();}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,in)->{int top=0,bot=0;if(android.os.Build.VERSION.SDK_INT>=30){top=in.getInsets(WindowInsets.Type.statusBars()).top;bot=in.getInsets(WindowInsets.Type.navigationBars()).bottom;}v.setPadding(0,top,0,bot);return in;});root.requestApplyInsets();
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(16),dp(18),dp(16),dp(14));head.setBackground(grad(Color.rgb(12,22,38),Color.rgb(8,14,25)));
        TextView title=t("QUAKE WATCH • AŞAMA 1",24,TEXT);title.setTypeface(Typeface.DEFAULT_BOLD);head.addView(title);head.addView(t("Change-point • Sequence Intelligence • Segment Cards • Prediction Passport • Data Health",11,MUTED));
        status=t("● Analiz hazırlanıyor",12,GREEN);status.setPadding(0,dp(10),0,0);head.addView(status);root.addView(head);
        ScrollView sv=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),dp(10),dp(12),dp(18));sv.addView(body);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setPadding(dp(8),dp(8),dp(8),dp(10));nav.setBackgroundColor(Color.rgb(10,17,28));
        Button premium=btn("PREMIUM RADAR",CYAN);Button refresh=btn("YENİ ANALİZ",GREEN);nav.addView(premium,new LinearLayout.LayoutParams(0,dp(54),1));nav.addView(refresh,new LinearLayout.LayoutParams(0,dp(54),1));root.addView(nav);
        premium.setOnClickListener(v->startActivity(new Intent(this,PremiumResearchActivity.class)));refresh.setOnClickListener(v->run());setContentView(root);
    }
    private void run(){status.setText("● Türkiye kataloğu + Aşama 1 motoru çalışıyor...");body.removeAllViews();body.addView(card("AŞAMA 1 ÇEKİRDEK","Veri indiriliyor ve değişim noktaları hesaplanıyor...",CYAN));new Thread(()->{try{TurkeyAnalyzer.Report tr=TurkeyAnalyzer.fetchAndAnalyze();Stage1ResearchEngine.Result r=Stage1ResearchEngine.analyze(this,tr);PredictionAudit.verifyAgainstCatalog(this,tr.eventsJson);PredictionAudit.recordTurkeyForecast(this,tr);runOnUiThread(()->{body.removeAllViews();body.addView(card("GENEL DURUM",r.summary,GOLD));body.addView(card("CHANGE-POINT",r.changePoint,CYAN));body.addView(card("SEQUENCE INTELLIGENCE",r.sequence,GREEN));body.addView(card("SEGMENT DURUMU",r.segments,Color.rgb(255,131,102)));body.addView(card("PREDICTION PASSPORT",r.passport,Color.rgb(171,129,255)));body.addView(card("NEDEN FİKRİM DEĞİŞTİ?",r.whyChanged,Color.rgb(112,190,255)));body.addView(card("DATA HEALTH",r.health,r.healthScore>=70?GREEN:GOLD));status.setText("● Aşama 1 tamam • veri sağlığı "+Math.round(r.healthScore)+"/100");});}catch(Exception e){runOnUiThread(()->{body.removeAllViews();body.addView(card("ANALİZ HATASI",e.getClass().getSimpleName()+": "+e.getMessage(),Color.rgb(255,83,104)));status.setText("● Veri/analiz hatası");});}}).start();}
    private LinearLayout card(String title,String text,int accent){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(14),dp(14),dp(14));GradientDrawable g=new GradientDrawable();g.setColor(CARD);g.setCornerRadius(dp(18));g.setStroke(dp(1),Color.argb(150,Color.red(accent),Color.green(accent),Color.blue(accent)));c.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));c.setLayoutParams(p);TextView h=t(title,12,accent);h.setTypeface(Typeface.DEFAULT_BOLD);c.addView(h);TextView x=t(text,13,TEXT);x.setPadding(0,dp(8),0,0);x.setLineSpacing(dp(3),1f);c.addView(x);return c;}
    private Button btn(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(Color.rgb(5,10,18));b.setTextSize(11);b.setTypeface(Typeface.DEFAULT_BOLD);b.setAllCaps(false);GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(15));b.setBackground(g);return b;}
    private TextView t(String s,int sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);return v;}
    private GradientDrawable grad(int a,int b){return new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}
}
