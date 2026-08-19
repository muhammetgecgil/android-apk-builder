package com.mg.fixturecockpitsim;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class LauncherActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(40,40,40,40); root.setBackgroundColor(Color.rgb(3,9,13));
        TextView title=new TextView(this); title.setText("FIXTURE COCKPIT SIM • 3D"); title.setTextColor(Color.rgb(160,255,190)); title.setTextSize(30); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView sub=new TextView(this); sub.setText("Tek APK • Bluetooth IMU • ileri seviye 3D savaş uçağı"); sub.setTextColor(Color.LTGRAY); sub.setTextSize(15); sub.setGravity(Gravity.CENTER); sub.setPadding(8,12,8,24); root.addView(sub);
        Button pilot=new Button(this); pilot.setText("PİLOT / KOKPİT"); pilot.setAllCaps(false);
        Button display=new Button(this); display.setText("UÇAK EKRANI / 3D F-22 BENZERİ"); display.setAllCaps(false);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(470),dp(74)); lp.setMargins(0,12,0,0); root.addView(pilot,lp); root.addView(display,lp);
        TextView hint=new TextView(this); hint.setText("Uçak ekranında ekrana dokunarak kamera açısını değiştirebilirsin."); hint.setTextColor(Color.GRAY); hint.setTextSize(13); hint.setGravity(Gravity.CENTER); hint.setPadding(10,22,10,0); root.addView(hint);
        pilot.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));
        display.setOnClickListener(v->startActivity(new Intent(this,Display3DActivity.class)));
        setContentView(root);
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
