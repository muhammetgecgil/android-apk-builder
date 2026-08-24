package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RobotWorldActivity extends Activity {
    private EditText endpoint;
    private TextView out;
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(24),dp(20),dp(20)); root.setBackgroundColor(Color.rgb(244,246,248));
        TextView title=new TextView(this); title.setText("Robot Perception & World Model"); title.setTextSize(25); title.setTypeface(null,android.graphics.Typeface.BOLD); root.addView(title);
        TextView info=new TextView(this); info.setText("Perception-only prototip. Frame tree, robot state ve world snapshot vardır. Motor/servo/tork/hız komut endpoint'i YOKTUR."); info.setTextSize(14); info.setPadding(0,dp(10),0,dp(16)); root.addView(info);
        endpoint=new EditText(this); endpoint.setHint("Robot World API endpoint (örn. http://192.168.1.10:8095)"); endpoint.setSingleLine(true); root.addView(endpoint);
        Button health=button("Robot World Durum / Safety Boundary"); root.addView(health);
        Button frame=button("Android Sensor Frame Kaydet"); root.addView(frame);
        Button state=button("Perception-Only Robot State Gönder"); root.addView(state);
        Button snapshot=button("World Snapshot Getir"); root.addView(snapshot);
        out=new TextView(this); out.setText("Hazır. Fiziksel eylem devre dışı."); out.setPadding(0,dp(14),0,0); root.addView(out);
        health.setOnClickListener(v->{String b=base();if(b!=null)RobotWorldClient.health(b,cb());});
        frame.setOnClickListener(v->{String b=base();if(b!=null)RobotWorldClient.sendPrototypeFrames(b,System.currentTimeMillis(),cb());});
        state.setOnClickListener(v->{String b=base();if(b!=null)RobotWorldClient.sendPerceptionOnlyState(b,System.currentTimeMillis(),cb());});
        snapshot.setOnClickListener(v->{String b=base();if(b!=null)RobotWorldClient.snapshot(b,cb());});
        setContentView(root);
    }
    private String base(){String b=endpoint.getText().toString().trim();if(b.isEmpty()){out.setText("Endpoint gir.");return null;}out.setText("İstek gönderiliyor...");return b;}
    private RobotWorldClient.Callback cb(){return new RobotWorldClient.Callback(){public void onSuccess(String v){runOnUiThread(()->out.setText(v));}public void onError(String e){runOnUiThread(()->out.setText("Hata: "+e));}};}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
