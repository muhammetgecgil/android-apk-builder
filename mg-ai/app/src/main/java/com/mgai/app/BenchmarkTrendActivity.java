package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

public class BenchmarkTrendActivity extends Activity {
    private LinearLayout root;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        ScrollView sv=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(24));root.setBackgroundColor(Color.rgb(244,246,248));sv.addView(root);setContentView(sv);render();
    }
    private void render(){
        root.removeAllViews();
        TextView title=new TextView(this);title.setText("Benchmark Trendleri");title.setTextSize(28);title.setTextColor(Color.rgb(20,24,32));root.addView(title);
        TextView trend=card();trend.setText(BenchmarkTrendStore.trendSummary(this));root.addView(trend);
        List<BenchmarkTrendStore.Entry> es=BenchmarkTrendStore.entries(this);
        if(es.isEmpty()){TextView e=card();e.setText("Henüz kayıt yok. Self-Tuning ekranından aktif benchmark çalıştır.");root.addView(e);}else{
            for(int i=0;i<es.size();i++){BenchmarkTrendStore.Entry e=es.get(i);TextView t=card();t.setText((i==0?"EN SON\n":"")+e.summary());root.addView(t);}
        }
        Button clear=new Button(this);clear.setText("Trend Geçmişini Temizle");clear.setAllCaps(false);clear.setOnClickListener(v->{BenchmarkTrendStore.clear(this);render();});root.addView(clear);
    }
    private TextView card(){TextView t=new TextView(this);t.setTextSize(15);t.setTextColor(Color.rgb(35,40,50));t.setPadding(dp(12),dp(12),dp(12),dp(12));t.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(10),0,0);t.setLayoutParams(lp);return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
