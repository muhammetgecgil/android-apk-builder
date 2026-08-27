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
    public static final String EXTRA_DEMO_MODE="demo_mode";

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(40,40,40,40); root.setBackgroundColor(Color.rgb(3,9,13));
        TextView title=new TextView(this); title.setText("AIRCRAFT SIMULATOR • DUAL PHONE"); title.setTextColor(Color.rgb(160,255,190)); title.setTextSize(30); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView sub=new TextView(this); sub.setText("Tek stealth uçak • ayrı kokpit telefonu • ayrı uçak ekranı • Bluetooth IMU"); sub.setTextColor(Color.LTGRAY); sub.setTextSize(15); sub.setGravity(Gravity.CENTER); sub.setPadding(8,12,8,24); root.addView(sub);

        Button demo=new Button(this); demo.setText("DEMO UÇUŞ — OTOMATİK KALK / GEZ / İN"); demo.setAllCaps(false);
        Button pilot=new Button(this); pilot.setText("PİLOT TELEFONU — KOKPİT / KONTROLLER"); pilot.setAllCaps(false);
        Button display=new Button(this); display.setText("UÇAK TELEFONU — 3D UÇAK EKRANI"); display.setAllCaps(false);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(540),dp(74)); lp.setMargins(0,12,0,0); root.addView(demo,lp); root.addView(pilot,lp); root.addView(display,lp);

        TextView hint=new TextView(this); hint.setText("İki telefon ayrı çalışır: bağlantıyı başlatan telefon PILOT olur; bağlantıyı kabul eden telefon UÇAK EKRANI olur. Demo tek telefonda bağımsız çalışır."); hint.setTextColor(Color.GRAY); hint.setTextSize(13); hint.setGravity(Gravity.CENTER); hint.setPadding(10,22,10,0); root.addView(hint);

        demo.setOnClickListener(v->{ Intent i=new Intent(this,Display3DActivity.class); i.putExtra(EXTRA_DEMO_MODE,true); startActivity(i); });
        pilot.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));
        display.setOnClickListener(v->{ Intent i=new Intent(this,Display3DActivity.class); i.putExtra(EXTRA_DEMO_MODE,false); startActivity(i); });
        setContentView(root);
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
