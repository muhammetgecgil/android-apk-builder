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

/** AVM-17.1 launcher: flight, pilot cockpit and second-phone controller screens. */
public final class LauncherActivity extends Activity {
    public static final String EXTRA_DEMO_MODE="demo_mode";

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(34),dp(20),dp(34),dp(20));root.setBackgroundColor(Color.rgb(3,9,13));
        TextView title=new TextView(this);title.setText("AIRCRAFT SIMULATOR 3D • AVM-17.1");title.setTextColor(Color.rgb(157,255,208));title.setTextSize(27);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=new TextView(this);sub.setText("Uçuş ekranı • Pilot / cockpit ekranı • İkinci telefon kumandası");sub.setTextColor(Color.LTGRAY);sub.setTextSize(14);sub.setGravity(Gravity.CENTER);sub.setPadding(dp(8),dp(8),dp(8),dp(14));root.addView(sub);

        Button flight=button("1. UÇUŞ EKRANI — AUTO / IMU MANUEL / LINK");
        Button pilot=button("2. PİLOT / COCKPIT — DEMO + HUD + MFD");
        Button controller=button("3. İKİNCİ TELEFON — ADVANCED FLIGHT CONTROLLER");
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(690),dp(66));lp.setMargins(0,dp(9),0,0);root.addView(flight,lp);root.addView(pilot,lp);root.addView(controller,lp);

        TextView hint=new TextView(this);hint.setText("PİLOT / COCKPIT ekranı, AUTO demonun aynı uçak ve çevre motorunu kullanır. İlk cockpit sürümünde HUD, iki MFD, UFCP, motor/yakıt/gear göstergeleri hazırdır; sonraki sürümlerde radar, navigasyon ve sistem sayfaları bu ekranlara bağlanabilir.");hint.setTextColor(Color.rgb(125,145,151));hint.setTextSize(11);hint.setGravity(Gravity.CENTER);hint.setPadding(dp(12),dp(16),dp(12),0);root.addView(hint);

        flight.setOnClickListener(v->{Intent i=new Intent(this,FlightRuntimeActivity.class);i.putExtra(EXTRA_DEMO_MODE,true);startActivity(i);});
        pilot.setOnClickListener(v->startActivity(new Intent(this,PilotCockpitActivity.class)));
        controller.setOnClickListener(v->startActivity(new Intent(this,AdvancedControllerActivity.class)));
        setContentView(root);
    }

    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(14);return b;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
