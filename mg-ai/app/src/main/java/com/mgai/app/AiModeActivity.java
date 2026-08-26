package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class AiModeActivity extends Activity {
    private TextView status;
    @Override protected void onCreate(Bundle b){super.onCreate(b);ScrollView sv=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(24));root.setBackgroundColor(Color.rgb(244,246,248));sv.addView(root);
        TextView title=new TextView(this);title.setText("AI Çalışma Modu");title.setTextSize(28);title.setTextColor(Color.rgb(20,24,32));root.addView(title);
        TextView sub=new TextView(this);sub.setText("Telefonun hangi hedefe göre model ve çalışma profilini seçmesini istediğini belirle.");sub.setTextSize(14);sub.setPadding(0,dp(6),0,dp(14));root.addView(sub);
        status=new TextView(this);status.setTextSize(16);status.setTextColor(Color.rgb(35,40,50));status.setPadding(dp(12),dp(12),dp(12),dp(12));status.setBackgroundColor(Color.WHITE);root.addView(status);
        add(root,"⚡ Hız",AiModeManager.Mode.SPEED);add(root,"⚖ Dengeli",AiModeManager.Mode.BALANCED);add(root,"★ Kalite",AiModeManager.Mode.QUALITY);add(root,"❄ Serin",AiModeManager.Mode.COOL);add(root,"🧠 Uzun Muhakeme",AiModeManager.Mode.DEEP_REASONING);
        setContentView(sv);render();}
    private void add(LinearLayout root,String text,AiModeManager.Mode m){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setOnClickListener(v->{AiModeManager.set(this,m);render();});root.addView(b);}
    private void render(){status.setText("AKTİF MOD\n"+AiModeManager.summary(this));}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
