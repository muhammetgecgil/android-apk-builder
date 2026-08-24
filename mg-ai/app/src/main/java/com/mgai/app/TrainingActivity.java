package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;

public class TrainingActivity extends Activity {
    private EditText endpoint, model, dataset;
    private TextView out;
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(24),dp(20),dp(20)); root.setBackgroundColor(Color.rgb(244,246,248));
        TextView title=new TextView(this); title.setText("Training / LoRA-SFT"); title.setTextSize(26); title.setTypeface(null,android.graphics.Typeface.BOLD); root.addView(title);
        TextView info=new TextView(this); info.setText("Training yalnız doğrulanmış dataset + offline evaluation + promotion gate ile çalışır. GPU yoksa job blocked kalır; online weight update yoktur."); info.setPadding(0,dp(12),0,dp(16)); root.addView(info);
        endpoint=input("Training API endpoint"); root.addView(endpoint);
        model=input("Base model (örn. Qwen3.5-4B)"); model.setText("Qwen3.5-4B"); root.addView(model);
        dataset=input("Dataset ID"); dataset.setText("experience-buffer-clean-v1"); root.addView(dataset);
        Button health=button("Training Server Durumu"); root.addView(health);
        Button create=button("LoRA/SFT Training Job Oluştur"); root.addView(create);
        Button jobs=button("Training Job'larını Gör"); root.addView(jobs);
        Button ckpt=button("Checkpoint Registry"); root.addView(ckpt);
        out=new TextView(this); out.setText("Hazır."); out.setPadding(0,dp(14),0,0); root.addView(out);
        health.setOnClickListener(v->{String b=base();if(b!=null)TrainingClient.health(b,cb());});
        create.setOnClickListener(v->{String b=base();if(b!=null)TrainingClient.createJob(b,model.getText().toString().trim(),dataset.getText().toString().trim(),cb());});
        jobs.setOnClickListener(v->{String b=base();if(b!=null)TrainingClient.listJobs(b,cb());});
        ckpt.setOnClickListener(v->{String b=base();if(b!=null)TrainingClient.checkpoints(b,cb());});
        setContentView(root);
    }
    private String base(){String b=endpoint.getText().toString().trim();if(b.isEmpty()){out.setText("Endpoint gir.");return null;}return b;}
    private TrainingClient.Callback cb(){return new TrainingClient.Callback(){public void onSuccess(String v){runOnUiThread(()->out.setText(v));}public void onError(String e){runOnUiThread(()->out.setText("Hata: "+e));}};}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);return e;}
    private Button button(String t){Button b=new Button(this);b.setText(t);b.setAllCaps(false);return b;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
