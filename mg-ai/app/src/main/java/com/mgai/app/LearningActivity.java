package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LearningActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22),dp(28),dp(22),dp(22));
        root.setBackgroundColor(Color.rgb(244,246,248));
        TextView t=new TextView(this); t.setText("Öğrenme & Gelişim"); t.setTextSize(30); t.setTextColor(Color.rgb(20,24,32)); root.addView(t);
        TextView s=new TextView(this);
        s.setText("Experience Buffer • Failure Memory • Teacher karşılaştırma • Eğitim adayı kalite kapısı • Offline promotion/reject\n\nGüvenlik: runtime sırasında online weight update YOK. Robot çalışırken weight update YOK. İnternet içeriği doğrudan model ağırlığına yazılamaz. Yeni sürüm yalnız offline değerlendirme ve regresyon kontrolünden sonra aday olabilir.");
        s.setTextSize(16); s.setPadding(0,dp(18),0,0); root.addView(s);
        setContentView(root);
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
