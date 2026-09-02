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

/** Launcher plus mission/scenario selection screen. */
public final class LauncherActivity extends Activity {
    public static final String EXTRA_DEMO_MODE="demo_mode";
    public static final String EXTRA_SCENARIO_ID="scenario_id";
    public static final String EXTRA_SCENARIO_NAME="scenario_name";

    public static final int SCENARIO_FULL_DEMO=1;
    public static final int SCENARIO_NIGHT_LANDING=2;
    public static final int SCENARIO_LOW_VIS_RAIN=3;
    public static final int SCENARIO_MOUNTAIN_PASS=4;
    public static final int SCENARIO_SEA_LOW_LEVEL=5;
    public static final int SCENARIO_EMERGENCY_RETURN=6;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        showMainMenu();
    }

    private void showMainMenu(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(10),dp(24),dp(10));root.setBackgroundColor(Color.rgb(3,9,13));
        TextView title=new TextView(this);title.setText("AIRCRAFT SIMULATOR 3D");title.setTextColor(Color.rgb(157,255,208));title.setTextSize(23);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=new TextView(this);sub.setText("Uçuş ekranı • Pilot / cockpit ekranı • İkinci telefon kumandası");sub.setTextColor(Color.LTGRAY);sub.setTextSize(12);sub.setGravity(Gravity.CENTER);sub.setPadding(dp(6),dp(4),dp(6),dp(7));root.addView(sub);

        Button flight=button("1. UÇUŞ EKRANI — AUTO / IMU MANUEL / LINK");
        Button pilot=button("2. PİLOT / COCKPIT — HUD + MFD + SİSTEMLER");
        Button controller=button("3. İKİNCİ TELEFON — ADVANCED FLIGHT CONTROLLER");
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(680),dp(51));lp.setMargins(0,dp(6),0,0);root.addView(flight,lp);root.addView(pilot,lp);root.addView(controller,lp);

        TextView hint=new TextView(this);hint.setText("Uçuş veya cockpit seçildiğinde görev/senaryo ekranı açılır. Seçilen görev aynı uçuş motoruna senaryo kimliğiyle aktarılır.");hint.setTextColor(Color.rgb(125,145,151));hint.setTextSize(9);hint.setGravity(Gravity.CENTER);hint.setPadding(dp(10),dp(8),dp(10),0);root.addView(hint);

        flight.setOnClickListener(v->showScenarioSelect(false));
        pilot.setOnClickListener(v->showScenarioSelect(true));
        controller.setOnClickListener(v->startActivity(new Intent(this,AdvancedControllerActivity.class)));
        setContentView(root);
    }

    private void showScenarioSelect(boolean cockpitTarget){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(24),dp(7),dp(24),dp(7));root.setBackgroundColor(Color.rgb(2,7,10));

        TextView title=new TextView(this);title.setText("GÖREV / SCENARIO SELECT");title.setTextColor(Color.rgb(132,255,180));title.setTextSize(24);title.setGravity(Gravity.CENTER);root.addView(title,new LinearLayout.LayoutParams(-1,dp(42)));
        TextView sub=new TextView(this);sub.setText("Her görev aynı uçuş motorunu kullanır; hava, başlangıç fazı, ATC ve pist koşulları göreve göre ayarlanır.");sub.setTextColor(Color.rgb(190,194,197));sub.setTextSize(11);sub.setGravity(Gravity.CENTER);root.addView(sub,new LinearLayout.LayoutParams(-1,dp(34)));

        addScenario(root,cockpitTarget,SCENARIO_FULL_DEMO,"1. FULL DEMO  —  Tam görev: kalkış • bulut • deniz • orbit • yaklaşma • iniş");
        addScenario(root,cockpitTarget,SCENARIO_NIGHT_LANDING,"2. NIGHT LANDING  —  RWY 27 gece yaklaşması • tam pist ışıkları • PAPI/HUD");
        addScenario(root,cockpitTarget,SCENARIO_LOW_VIS_RAIN,"3. LOW VIS / RAIN APPROACH  —  Yağmur + sis • düşük RVR • ıslak pist • ILS/PAPI");
        addScenario(root,cockpitTarget,SCENARIO_MOUNTAIN_PASS,"4. MOUNTAIN PASS  —  Dağ rotası • yüksek hızlı alçak/orta irtifa manevrası");
        addScenario(root,cockpitTarget,SCENARIO_SEA_LOW_LEVEL,"5. SEA LOW LEVEL  —  Deniz üstü alçak irtifa • yüksek hız • pull-up");
        addScenario(root,cockpitTarget,SCENARIO_EMERGENCY_RETURN,"6. EMERGENCY RETURN  —  Kısa acil geri dönüş • pist yakalama • öncelikli ATC");

        Button back=button("GERİ");
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(390),dp(47));bp.setMargins(0,dp(8),0,0);root.addView(back,bp);back.setOnClickListener(v->showMainMenu());
        setContentView(root);
    }

    private void addScenario(LinearLayout root,boolean cockpitTarget,int id,String label){
        Button b=scenarioButton(label);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(59));lp.setMargins(0,dp(5),0,0);root.addView(b,lp);b.setOnClickListener(v->launchScenario(cockpitTarget,id,label));
    }

    private void launchScenario(boolean cockpitTarget,int id,String label){
        Intent i=new Intent(this,cockpitTarget?PilotCockpitActivity.class:FlightRuntimeActivity.class);
        i.putExtra(EXTRA_DEMO_MODE,true);
        i.putExtra(EXTRA_SCENARIO_ID,id);
        i.putExtra(EXTRA_SCENARIO_NAME,label);
        startActivity(i);
    }

    @Override public void onBackPressed(){showMainMenu();}

    private Button scenarioButton(String s){Button b=button(s);b.setTextSize(12);b.setTextColor(Color.WHITE);b.setBackgroundColor(Color.rgb(96,97,99));b.setGravity(Gravity.CENTER);return b;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(12);return b;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
