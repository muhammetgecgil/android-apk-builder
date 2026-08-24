package com.mg.whiffletreeaero;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import android.text.InputType;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText loadKn, pressureBar, safety, cylKn, pads, ratioA, ratioB;
    private TextView result, scenarioInfo;

    private static final String[] SCENARIOS = {
        "1. Full Wing Limit Load","2. Wing Ultimate Load","3. Wing Bending + Torsion","4. Wingtip Load",
        "5. Aileron Hinge Moment","6. Flap Static Load","7. Elevator Load","8. Horizontal Tail Load",
        "9. Vertical Tail Side Load","10. Rudder Hinge Moment","11. Fuselage Global Bending","12. Fuselage Torsion",
        "13. Landing Gear Attachment","14. Engine Mount 6-DOF","15. Pylon Static Load","16. Seat/Floor Inertia Load",
        "17. External Tank Lug Load","18. Payload Attachment","19. Composite Panel Buckling","20. Wing Fatigue Spectrum"
    };

    private static final String[] INFO = {
        "Distributed aerodynamic lift → multi-level whiffletree → bonded/bolted load pads → wing strong points.",
        "Ultimate static load. Use calibrated load cells, stroke margin and independent reaction-structure check.",
        "Split front/rear spar load lines to reproduce both root bending moment and torsion.",
        "Concentrated wingtip load. Check local reinforcement and actuator lateral-load sensitivity.",
        "Opposed force couple around hinge line. Verify hinge fittings and local bearing stress.",
        "Distributed flap pressure represented by several pads/straps and a whiffletree.",
        "Tail bending plus hinge moment. Use spherical joints to tolerate rotation during deflection.",
        "Distributed tail load using multiple channels or a balanced whiffletree.",
        "Horizontal actuator/reaction arrangement for fin side-load testing.",
        "Bidirectional actuator pair to reproduce rudder hinge torque.",
        "Multiple frames loaded to reproduce global fuselage bending and shear distribution.",
        "Opposed actuator couples around fuselage frames to create torsion.",
        "Multi-axis concentrated load at gear fittings. High local reaction and fixture stiffness required.",
        "Fx/Fy/Fz and Mx/My/Mz reproduction. Usually requires a multi-axis reaction frame.",
        "Pylon shear + bending; verify lug/bolt load paths and eccentricity.",
        "Inertia-equivalent floor load. Check seat rails, fittings and local floor panel response.",
        "Twin-actuator or fixture arrangement for lug separation/inertia loads.",
        "Multi-axis payload interface load with clevis, spherical bearing and calibrated load cell.",
        "Compression/shear boundary loading; alignment and anti-buckling fixture parasitics are critical.",
        "Servo-hydraulic cyclic spectrum. Include fatigue spectrum, phase control and thermal monitoring."
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sc = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(30));
        root.setBackgroundColor(Color.rgb(245,247,250));
        sc.addView(root);

        TextView title = text("WHIFFLETREE AERO", 26, true, Color.rgb(17,24,39));
        root.addView(title);
        TextView sub = text("Aircraft Structural Test Load Distribution & Actuator Sizing", 14, false, Color.DKGRAY);
        sub.setPadding(0,0,0,dp(14)); root.addView(sub);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SCENARIOS);
        spinner.setAdapter(ad); root.addView(spinner, lp());
        scenarioInfo = card("Select a test case to see the recommended physical load path."); root.addView(scenarioInfo, lp());
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id){ scenarioInfo.setText(INFO[pos]); }
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });

        root.addView(section("INPUTS"));
        loadKn = field(root,"Target total test load [kN]","240");
        pressureBar = field(root,"Hydraulic pressure [bar]","210");
        safety = field(root,"Sizing factor [-]","1.50");
        cylKn = field(root,"Available cylinder nominal force [kN]","100");
        pads = field(root,"Total load pads","8");
        ratioA = field(root,"Whiffletree branch A share [%]","50");
        ratioB = field(root,"Whiffletree branch B share [%]","50");

        Button calc = new Button(this); calc.setText("CALCULATE RIG"); calc.setTextSize(17); root.addView(calc, lp());
        result = card("Results will appear here."); result.setTextSize(15); root.addView(result, lp());
        calc.setOnClickListener(v -> calculate());

        root.addView(section("PHYSICAL LOAD TRAIN"));
        root.addView(card("Hydraulic actuator → rod-end / spherical bearing → clevis → load cell → threaded link → whiffletree beam → secondary links → load pads → aircraft structure → reaction fixture / strong floor"), lp());
        root.addView(section("ENGINEERING CHECKS"));
        root.addView(card("Verify separately before hardware release: actuator stroke, rod buckling, fixture stiffness, clevis/pin bearing & shear, spherical-bearing misalignment, load-cell overload margin, pad local stress, adhesive/fastener capability, hardpoint loads, stability, fatigue spectrum, hydraulic energy and fail-safe behavior."), lp());

        setContentView(sc);
    }

    private void calculate(){
        try {
            double F = d(loadKn), pbar = d(pressureBar), sf = d(safety), cyl = d(cylKn);
            int nPads = Math.max(1,(int)Math.round(d(pads)));
            double a = d(ratioA), b = d(ratioB);
            if(F<=0||pbar<=0||sf<=0||cyl<=0||a<=0||b<=0) throw new Exception();
            double design = F*sf;
            int nCyl = (int)Math.ceil(design/cyl);
            double forcePerCylinder = design/nCyl;
            double pPa = pbar*100000.0;
            double area = forcePerCylinder*1000.0/pPa;
            double boreMm = Math.sqrt(4.0*area/Math.PI)*1000.0;
            double padNom = F/nPads;
            double padDesign = design/nPads;
            double sum = a+b;
            double fracA=a/sum, fracB=b/sum;
            double armRatio = fracB/fracA; // L_A/L_B from FA*LA=FB*LB
            double reserve = nCyl*cyl-design;
            String risk = reserve < 0.1*design ? "LOW CAPACITY RESERVE" : "Capacity reserve acceptable for preliminary sizing";
            String s = String.format(Locale.US,
                "TARGET LOAD: %.1f kN\nDESIGN LOAD: %.1f kN\n\nACTUATORS\nRecommended minimum count: %d\nDesign force / actuator: %.1f kN\nAvailable nominal total: %.1f kN\nReserve: %.1f kN\nPreliminary equivalent bore @ %.0f bar: %.1f mm\n\nLOAD PADS\nPad count: %d\nNominal load / pad: %.1f kN\nDesign load / pad: %.1f kN\n\nWHIFFLETREE FIRST SPLIT\nBranch A: %.1f%% → %.1f kN design\nBranch B: %.1f%% → %.1f kN design\nRequired lever-arm relation LA/LB = %.3f\n(FA·LA = FB·LB)\n\nSTATUS\n%s\n\nNOTE: Preliminary sizing only. Final rig design requires structural verification of every load-train component, test article allowable loads, reaction frame and failure containment.",
                F,design,nCyl,forcePerCylinder,nCyl*cyl,reserve,pbar,boreMm,nPads,padNom,padDesign,100*fracA,design*fracA,100*fracB,design*fracB,armRatio,risk);
            result.setText(s);
        } catch(Exception e){ result.setText("Check all inputs. Values must be positive numbers."); }
    }

    private double d(EditText e){ return Double.parseDouble(e.getText().toString().trim().replace(',','.')); }
    private EditText field(LinearLayout root,String label,String val){
        TextView l=text(label,14,true,Color.rgb(55,65,81)); l.setPadding(0,dp(8),0,dp(3)); root.addView(l);
        EditText e=new EditText(this); e.setText(val); e.setTextSize(17); e.setSingleLine(true); e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); root.addView(e,lp()); return e;
    }
    private TextView section(String s){ TextView t=text(s,18,true,Color.rgb(17,24,39)); t.setPadding(0,dp(18),0,dp(6)); return t; }
    private TextView card(String s){ TextView t=text(s,14,false,Color.rgb(31,41,55)); t.setPadding(dp(14),dp(14),dp(14),dp(14)); t.setBackgroundColor(Color.WHITE); return t; }
    private TextView text(String s,int sp,boolean bold,int c){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(c); if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD); return t; }
    private LinearLayout.LayoutParams lp(){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(4),0,dp(8)); return p; }
    private int dp(int x){ return (int)(x*getResources().getDisplayMetrics().density+0.5f); }
}
