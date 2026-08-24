package com.mgai.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DashboardActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22),dp(28),dp(22),dp(22)); root.setBackgroundColor(Color.rgb(244,246,248));
        TextView title=new TextView(this); title.setText("MG-AI"); title.setTextSize(34); title.setTypeface(null,android.graphics.Typeface.BOLD); title.setTextColor(Color.rgb(20,24,32)); root.addView(title);
        TextView sub=new TextView(this); sub.setText("v0.13 • Training + LoRA/SFT + Robot World Model + Safety Supervisor"); sub.setTextSize(14); sub.setTextColor(Color.rgb(90,97,110)); sub.setPadding(0,dp(4),0,dp(22)); root.addView(sub);
        TextView summary=new TextView(this); summary.setText("Gereksinim entegrasyon durumu\n"+CapabilityRegistry.summary()); summary.setTextSize(16); summary.setTextColor(Color.rgb(35,40,50)); summary.setPadding(dp(14),dp(14),dp(14),dp(14)); summary.setBackgroundColor(Color.WHITE); root.addView(summary,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        add(root,"MG-AI Sohbet",MainActivity.class);
        add(root,"MG-Core Durum Testi",CoreHealthActivity.class);
        add(root,"Internet Research Engine",ResearchActivity.class);
        add(root,"Hafıza & Bilgi",MemoryActivity.class);
        add(root,"Derin Muhakeme",ReasoningActivity.class);
        add(root,"Tools + Agents",ToolsActivity.class);
        add(root,"Öğrenme & Gelişim",LearningActivity.class);
        add(root,"Training / LoRA-SFT",TrainingActivity.class);
        add(root,"Görsel, Ses & Sensör Analizi",MultimodalActivity.class);
        add(root,"Robot Perception & World Model",RobotWorldActivity.class);
        add(root,"Robotics Safety Supervisor",RobotSafetyActivity.class);
        add(root,"Yetenekler & Gereksinimler",CapabilitiesActivity.class);
        TextView rule=new TextView(this); rule.setText("Training yalnız doğrulanmış veri + offline eval + promotion gate ile yapılır. Robot runtime sırasında weight update yoktur. Fiziksel eylem hâlâ devre dışı."); rule.setTextSize(13); rule.setTextColor(Color.rgb(95,102,116)); rule.setPadding(0,dp(22),0,0); root.addView(rule);
        setContentView(root);
    }
    private void add(LinearLayout root,String text,Class<?> cls){Button b=button(text);b.setOnClickListener(v->startActivity(new Intent(this,cls)));root.addView(b);}
    private Button button(String text){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(16);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(16),0,0);b.setLayoutParams(lp);return b;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
