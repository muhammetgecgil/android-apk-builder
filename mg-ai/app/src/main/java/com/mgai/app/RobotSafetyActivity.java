package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RobotSafetyActivity extends Activity {
    private EditText endpoint; private TextView out;
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(20));root.setBackgroundColor(Color.rgb(244,246,248));
        TextView title=new TextView(this);title.setText("Robotics Safety Supervisor");title.setTextSize(25);title.setTypeface(null,android.graphics.Typeface.BOLD);root.addView(title);
        TextView info=new TextView(this);info.setText("Deterministik ve LLM'den bağımsız. Bu ekran yalnız durum/audit okur; üretim safety limitlerini telefondan değiştirmez.");info.setTextSize(14);info.setPadding(0,dp(10),0,dp(16));root.addView(info);
        endpoint=new EditText(this);endpoint.setHint("Safety API endpoint (örn. http://192.168.1.10:8096)");endpoint.setSingleLine(true);root.addView(endpoint);
        Button health=button("Safety Durumunu Doğrula");root.addView(health);
        Button audit=button("Safety Audit Kaydını Göster");root.addView(audit);
        out=new TextView(this);out.setText("Safety config bilinmiyor. Config yoksa servis REJECT davranır.");out.setPadding(0,dp(14),0,0);root.addView(out);
        health.setOnClickListener(v->{String b=base();if(b!=null)RobotSafetyClient.health(b,cb());});
        audit.setOnClickListener(v->{String b=base();if(b!=null)RobotSafetyClient.audit(b,cb());});
        setContentView(root);
    }
    private String base(){String b=endpoint.getText().toString().trim();if(b.isEmpty()){out.setText("Endpoint gir.");return null;}out.setText("Safety servisi sorgulanıyor...");return b;}
    private RobotSafetyClient.Callback cb(){return new RobotSafetyClient.Callback(){public void onSuccess(String v){runOnUiThread(()->out.setText(v));}public void onError(String e){runOnUiThread(()->out.setText("Hata: "+e));}};}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
