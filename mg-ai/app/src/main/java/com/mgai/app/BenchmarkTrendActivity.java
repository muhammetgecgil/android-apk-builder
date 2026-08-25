package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class BenchmarkTrendActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        ScrollView sv=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(24));root.setBackgroundColor(Color.rgb(244,246,248));sv.addView(root);
        TextView title=new TextView(this);title.setText("Benchmark Trendleri");title.setTextSize(28);title.setTextColor(Color.rgb(20,24,32));root.addView(title);
        TextView sub=new TextView(this);sub.setText("Son 20 aktif benchmark koşusunu tarih, model, profil, hız, gecikme ve sıcaklık açısından karşılaştırır.");sub.setTextSize(14);sub.setPadding(0,dp(4),0,dp(14));root.addView(sub);
        TextView insight=card(root);insight.setText("TREND YORUMU\n"+SelfTuningManager.trendInsight(this));
        TextView table=card(root);table.setText(SelfTuningManager.trendSummary(this));
        TextView note=new TextView(this);note.setText("Yorum en eski ve en yeni benchmarkı karşılaştırır. Model değişirse model adı tabloda ayrıca görünür; bu nedenle performans farkı cihaz, sıcaklık ve model değişiminden birlikte etkilenebilir.");note.setTextSize(13);note.setPadding(0,dp(14),0,0);root.addView(note);
        setContentView(sv);
    }
    private TextView card(LinearLayout root){TextView t=new TextView(this);t.setTextSize(15);t.setTextColor(Color.rgb(35,40,50));t.setPadding(dp(12),dp(12),dp(12),dp(12));t.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(10),0,0);root.addView(t,lp);return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
