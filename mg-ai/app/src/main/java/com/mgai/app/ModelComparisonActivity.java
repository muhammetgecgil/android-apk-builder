package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ModelComparisonActivity extends Activity {
    private TextView models,result;
    private Button run;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        ScrollView sv=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(24));root.setBackgroundColor(Color.rgb(244,246,248));sv.addView(root);
        TextView title=new TextView(this);title.setText("En İyi Yerel Modeli Bul");title.setTextSize(28);title.setTextColor(Color.rgb(20,24,32));root.addView(title);
        TextView sub=new TextView(this);sub.setText("Kurulu GGUF modellerini aynı telefon, aynı prompt ve aynı profil ile karşılaştırır. Kazanan performans modeli otomatik aktif edilir.");sub.setTextSize(14);sub.setTextColor(Color.DKGRAY);sub.setPadding(0,dp(6),0,dp(14));root.addView(sub);
        models=card(root);result=card(root);
        run=new Button(this);run.setText("En İyi Modeli Bul ve Aktif Et");run.setAllCaps(false);run.setOnClickListener(v->start());root.addView(run);
        Button refresh=new Button(this);refresh.setText("Kurulu Modelleri Yenile");refresh.setAllCaps(false);refresh.setOnClickListener(v->render());root.addView(refresh);
        TextView note=new TextView(this);note.setText("Termal güvenlik: 43°C ve üzerindeyse test durur. Sonuç hız/TTFT/ısı/yükleme dengesi içindir; modelin bilgi veya muhakeme kalitesini tek başına ölçmez.");note.setTextSize(13);note.setPadding(0,dp(14),0,0);root.addView(note);
        setContentView(sv);render();
    }

    private void render(){
        models.setText("KURULU GGUF MODELLER\n"+LocalModelManager.installedModelsSummary(this)+"\n\nAktif: "+LocalModelManager.activeModelName(this));
        String r=ModelVsModelBenchmarkRunner.lastReport(this);result.setText(r.isEmpty()?"SON KARŞILAŞTIRMA\nHenüz çalıştırılmadı.":r);
    }

    private void start(){
        run.setEnabled(false);result.setText("MODEL KARŞILAŞTIRMA\nBaşlatılıyor…");
        ModelVsModelBenchmarkRunner.run(this,new ModelVsModelBenchmarkRunner.Listener(){
            @Override public void onProgress(String text){runOnUiThread(()->result.setText("MODEL KARŞILAŞTIRMA\n"+text));}
            @Override public void onComplete(String report){runOnUiThread(()->{run.setEnabled(true);result.setText(report);render();});}
            @Override public void onError(String message){runOnUiThread(()->{run.setEnabled(true);result.setText("MODEL KARŞILAŞTIRMA HATASI\n"+message);});}
        });
    }

    private TextView card(LinearLayout root){TextView t=new TextView(this);t.setTextSize(15);t.setTextColor(Color.rgb(35,40,50));t.setPadding(dp(12),dp(12),dp(12),dp(12));t.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(10),0,dp(10));root.addView(t,lp);return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
