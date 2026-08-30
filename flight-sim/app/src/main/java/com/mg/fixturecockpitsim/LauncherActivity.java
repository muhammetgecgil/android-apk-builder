package com.mg.fixturecockpitsim;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** AVM-12.1 launcher for autonomous display, advanced controller and dedicated aircraft screen. */
public final class LauncherActivity extends Activity {
    public static final String EXTRA_DEMO_MODE="demo_mode";

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(36),dp(26),dp(36),dp(26));root.setBackgroundColor(Color.rgb(3,9,13));
        TextView title=new TextView(this);title.setText("AIRCRAFT SIMULATOR 3D • AVM-12.1");title.setTextColor(Color.rgb(157,255,208));title.setTextSize(29);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=new TextView(this);sub.setText("Gerçek zamanlı 3D uçak • serbest taksi/pist kontrolü • ikinci telefondan tam Bluetooth kumanda • iniş emniyet modeli");sub.setTextColor(Color.LTGRAY);sub.setTextSize(14);sub.setGravity(Gravity.CENTER);sub.setPadding(dp(8),dp(10),dp(8),dp(20));root.addView(sub);

        Button demo=button("DEMO UÇUŞ — AUTO BAŞLAT / CONTROLLER BAĞLANINCA TAM DEVRAL");
        Button controller=button("2. TELEFON — ADVANCED FLIGHT CONTROLLER");
        Button display=button("UÇAK TELEFONU — 3D AIRFRAME / BLUETOOTH SERVER");
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(650),dp(72));lp.setMargins(0,dp(10),0,0);root.addView(demo,lp);root.addView(controller,lp);root.addView(display,lp);

        TextView hint=new TextView(this);hint.setText("Kullanım: Uçak telefonunda DEMO veya UÇAK TELEFONU ekranını aç. İkinci telefonda ADVANCED FLIGHT CONTROLLER'ı aç, LINK'e dokun ve uçak telefonunu seç. Bağlantı kurulduğunda pitch/roll/yaw, throttle, wheel-brake ve gear tamamen kumanda telefonuna geçer.");hint.setTextColor(Color.rgb(125,145,151));hint.setTextSize(12);hint.setGravity(Gravity.CENTER);hint.setPadding(dp(12),dp(20),dp(12),0);root.addView(hint);

        demo.setOnClickListener(v->{Intent i=new Intent(this,FlightRuntimeActivity.class);i.putExtra(EXTRA_DEMO_MODE,true);startActivity(i);});
        controller.setOnClickListener(v->startActivity(new Intent(this,AdvancedControllerActivity.class)));
        display.setOnClickListener(v->{Intent i=new Intent(this,FlightRuntimeActivity.class);i.putExtra(EXTRA_DEMO_MODE,false);startActivity(i);});
        setContentView(root);
    }

    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);return b;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
