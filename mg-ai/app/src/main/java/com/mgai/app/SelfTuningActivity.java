package com.mgai.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SelfTuningActivity extends Activity {
    private TextView status,table,reason,history,benchmark,modelBenchmark;
    private Button benchmarkBtn,modelBenchmarkBtn;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        ScrollView sv=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(24));root.setBackgroundColor(Color.rgb(244,246,248));sv.addView(root);
        TextView title=new TextView(this);title.setText("Self-Tuning & Benchmark");title.setTextSize(28);title.setTextColor(Color.rgb(20,24,32));root.addView(title);
        TextView sub=new TextView(this);sub.setText("Gerçek kullanım verilerini öğrenir; profil ve kurulu GGUF modellerini aynı cihazda karşılaştırır.");sub.setTextSize(14);sub.setPadding(0,dp(4),0,dp(14));root.addView(sub);
        status=card(root);table=card(root);reason=card(root);history=card(root);benchmark=card(root);modelBenchmark=card(root);
        Button refresh=new Button(this);refresh.setText("Verileri Yenile");refresh.setAllCaps(false);refresh.setOnClickListener(v->render());root.addView(refresh);
        benchmarkBtn=new Button(this);benchmarkBtn.setText("Aktif Profil Benchmark Başlat");benchmarkBtn.setAllCaps(false);benchmarkBtn.setOnClickListener(v->startBenchmark());root.addView(benchmarkBtn);
        modelBenchmarkBtn=new Button(this);modelBenchmarkBtn.setText("Kurulu Modelleri Karşılaştır");modelBenchmarkBtn.setAllCaps(false);modelBenchmarkBtn.setOnClickListener(v->startModelBenchmark());root.addView(modelBenchmarkBtn);
        Button compare=new Button(this);compare.setText("Profil Karşılaştırmasını Gör");compare.setAllCaps(false);compare.setOnClickListener(v->startActivity(new Intent(this,BenchmarkComparisonActivity.class)));root.addView(compare);
        Button trends=new Button(this);trends.setText("Benchmark Trendlerini Gör");trends.setAllCaps(false);trends.setOnClickListener(v->startActivity(new Intent(this,BenchmarkTrendActivity.class)));root.addView(trends);
        Button reset=new Button(this);reset.setText("Öğrenmeyi Sıfırla / Yeni Öğrenme Başlat");reset.setAllCaps(false);reset.setOnClickListener(v->{SelfTuningManager.reset(this);render();});root.addView(reset);
        TextView note=new TextView(this);note.setText("Benchmarklar yalnız sen başlattığında çalışır. 43°C ve üzeri sıcaklıkta termal güvenlik testi durdurur. Model karşılaştırması için models klasöründe en az iki GGUF gerekir.");note.setTextSize(13);note.setPadding(0,dp(14),0,0);root.addView(note);
        setContentView(sv);render();
    }
    private void startBenchmark(){
        benchmarkBtn.setEnabled(false);benchmark.setText("AKTİF PROFİL BENCHMARK\nBaşlatılıyor…");
        ActiveBenchmarkRunner.run(this,new ActiveBenchmarkRunner.Listener(){
            @Override public void onProgress(String text){runOnUiThread(()->benchmark.setText("AKTİF PROFİL BENCHMARK\n"+text));}
            @Override public void onComplete(String report){runOnUiThread(()->{benchmarkBtn.setEnabled(true);benchmark.setText(report);render();});}
            @Override public void onError(String message){runOnUiThread(()->{benchmarkBtn.setEnabled(true);benchmark.setText("AKTİF BENCHMARK HATASI\n"+message);});}
        });
    }
    private void startModelBenchmark(){
        modelBenchmarkBtn.setEnabled(false);modelBenchmark.setText("MODEL-VS-MODEL\nBaşlatılıyor…");
        ModelVsModelBenchmarkRunner.run(this,new ModelVsModelBenchmarkRunner.Listener(){
            @Override public void onProgress(String text){runOnUiThread(()->modelBenchmark.setText("MODEL-VS-MODEL\n"+text));}
            @Override public void onComplete(String report){runOnUiThread(()->{modelBenchmarkBtn.setEnabled(true);modelBenchmark.setText(report);});}
            @Override public void onError(String message){runOnUiThread(()->{modelBenchmarkBtn.setEnabled(true);modelBenchmark.setText("MODEL-VS-MODEL HATASI\n"+message);});}
        });
    }
    private TextView card(LinearLayout root){TextView t=new TextView(this);t.setTextSize(15);t.setTextColor(Color.rgb(35,40,50));t.setPadding(dp(12),dp(12),dp(12),dp(12));t.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(10),0,0);root.addView(t,lp);return t;}
    private void render(){
        AdaptivePerformanceManager.Profile p=AdaptivePerformanceManager.choose(this);
        status.setText("AKTİF DURUM\n"+SelfTuningManager.summary(this)+"\nAdaptif profil: "+p.summary()+"\nAktif profil süresi: "+(AdaptivePerformanceManager.activeAgeMs()/1000)+" sn\n"+SelfTuningManager.trendInsight(this));
        table.setText("PROFİL PERFORMANS TABLOSU\n"+SelfTuningManager.profileTable(this));
        reason.setText("NEDEN BU PROFİL?\n"+SelfTuningManager.selectionReason(this));
        history.setText(AdaptivePerformanceManager.historySummary());
        String r=SelfTuningManager.benchmarkReport(this);benchmark.setText(r.isEmpty()?"AKTİF PROFİL BENCHMARK\nHenüz çalıştırılmadı.":r);
        modelBenchmark.setText("KURULU GGUF MODELLER\n"+LocalModelManager.installedModelsSummary(this)+"\n\nModel-versus-model sonucu burada gösterilecek.");
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
