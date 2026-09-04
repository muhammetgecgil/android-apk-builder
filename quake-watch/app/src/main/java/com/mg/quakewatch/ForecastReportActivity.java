package com.mg.quakewatch;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ForecastReportActivity extends Activity {
    private TextView report;
    @Override public void onCreate(Bundle b){super.onCreate(b);build();}
    @Override protected void onResume(){super.onResume(); if(report!=null)report.setText(PredictionAudit.report(this));}
    private void build(){
        ScrollView s=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(18),dp(16),dp(28)); root.setBackgroundColor(Color.rgb(7,12,20)); s.addView(root);
        TextView title=t("TAHMİN DENETİM RAPORU",25,Color.WHITE); title.setGravity(Gravity.CENTER); title.setTypeface(null,1); root.addView(title);
        TextView sub=t("Model ne dedi? • Neden dedi? • Sonra ne oldu? • İsabet/kaçırma kaydı",13,Color.rgb(155,176,205)); sub.setGravity(Gravity.CENTER); sub.setPadding(0,dp(5),0,dp(14)); root.addView(sub);
        LinearLayout card=card(); report=t(PredictionAudit.report(this),14,Color.WHITE); card.addView(report); root.addView(card);
        Button refresh=new Button(this); refresh.setText("RAPORU YENİLE"); refresh.setTextColor(Color.WHITE); GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(41,121,255));g.setCornerRadius(dp(14));refresh.setBackground(g);refresh.setOnClickListener(v->report.setText(PredictionAudit.report(this)));root.addView(refresh,new LinearLayout.LayoutParams(-1,dp(50)));
        TextView note=t("Rapor yalnız önceden kaydedilmiş model çıktıları üzerinden hesaplanır. Sonradan sonucu görüp geçmiş tahmini değiştirmez. 'İsabet' tanımı varsayılan olarak risk merkezinin 75 km çevresinde, ilgili zaman penceresi içinde M≥3.5 katalog olayıdır. Bu bilimsel doğrulama metriğidir; kesin deprem tahmini iddiası değildir.",12,Color.rgb(255,191,112));note.setPadding(0,dp(14),0,0);root.addView(note);
        setContentView(s);
    }
    private LinearLayout card(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(14),dp(14),dp(14),dp(14));GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(17,25,39));g.setCornerRadius(dp(18));g.setStroke(dp(1),Color.rgb(38,53,75));v.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(12));v.setLayoutParams(p);return v;}
    private TextView t(String x,int sp,int c){TextView v=new TextView(this);v.setText(x);v.setTextSize(sp);v.setTextColor(c);return v;}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}
}
