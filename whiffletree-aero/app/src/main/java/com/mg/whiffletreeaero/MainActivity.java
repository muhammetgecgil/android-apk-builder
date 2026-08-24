package com.mg.whiffletreeaero;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import android.text.InputType;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText loadKn, pressureBar, safety, cylKn, pads, ratioA, ratioB;
    private TextView result, scenarioInfo, problemText, solutionText, stepDetail;
    private RigView rigView;
    private int selectedScenario = 0;
    private int selectedStep = 0;

    private static final String[] SCENARIOS = {
        "1. Full Wing Limit Load","2. Wing Ultimate Load","3. Wing Bending + Torsion","4. Wingtip Load",
        "5. Aileron Hinge Moment","6. Flap Static Load","7. Elevator Load","8. Horizontal Tail Load",
        "9. Vertical Tail Side Load","10. Rudder Hinge Moment","11. Fuselage Global Bending","12. Fuselage Torsion",
        "13. Landing Gear Attachment","14. Engine Mount 6-DOF","15. Pylon Static Load","16. Seat/Floor Inertia Load",
        "17. External Tank Lug Load","18. Payload Attachment","19. Composite Panel Buckling","20. Wing Fatigue Spectrum"
    };

    private static final String[] INFO = {
        "Distributed aerodynamic lift → multi-level whiffletree → load pads → wing strong points.",
        "Ultimate static load: capacity, stroke and reaction structure become dominant design checks.",
        "Two load lines reproduce root bending plus torsion: front spar and rear spar are controlled separately.",
        "Concentrated wingtip load: local reinforcement and actuator alignment are critical.",
        "A force couple around the hinge line creates hinge moment without adding unwanted net force.",
        "Distributed flap pressure is represented by several pads/straps and a whiffletree.",
        "Tail bending + hinge moment with spherical joints to tolerate rotation during deflection.",
        "Distributed horizontal-tail load with balanced branches and monitored reaction loads.",
        "Horizontal actuator arrangement creates vertical-tail side load while protecting the root fixture.",
        "Bidirectional force couple reproduces rudder hinge torque.",
        "Several fuselage frames are loaded together to reproduce global bending and shear.",
        "Opposed actuator couples around fuselage frames create torsion.",
        "Multi-axis concentrated loading at landing-gear fittings requires high-stiffness local fixtures.",
        "Fx/Fy/Fz and Mx/My/Mz are reproduced with a multi-axis reaction frame and coordinated channels.",
        "Pylon shear + bending: lug/bolt load paths and eccentricity are the main local concerns.",
        "Inertia-equivalent floor loading checks seat rails, fittings and floor-panel response.",
        "Twin-channel load application reproduces external-store lug/separation loads.",
        "Multi-axis payload interface uses clevises, spherical bearings and calibrated load cells.",
        "Compression/shear boundary loading: alignment and fixture parasitics can dominate the result.",
        "Servo-hydraulic cyclic spectrum: phase control, fatigue damage and temperature are monitored."
    };

    private static final String[] PROBLEMS = {
        "Problem: aerodynamic load is continuous, but the laboratory has only a limited number of actuators. The rig must reproduce total force and root bending moment without locally overloading the wing skin.",
        "Problem: the wing must reach ultimate load while actuator capacity, stroke, pad stress and strong-floor reactions stay inside allowable limits.",
        "Problem: the same rig must create lift-induced bending and aerodynamic torsion simultaneously, so front and rear spar load lines cannot be treated as one force.",
        "Problem: a high concentrated tip load creates a large root moment and can introduce local crushing or actuator side load if the line of action changes.",
        "Problem: create a pure hinge moment with minimal unwanted force at the control-surface attachment.",
        "Problem: convert distributed flap pressure into discrete laboratory loads without distorting the flap locally.",
        "Problem: reproduce tail bending and hinge load while the surface rotates and deflects under load.",
        "Problem: distribute tail aerodynamic load while maintaining correct root moment and avoiding fixture parasitic loads.",
        "Problem: apply side load to the fin while keeping actuator alignment valid through structural deflection.",
        "Problem: generate rudder torque in both directions and verify hinge/reaction fitting loads.",
        "Problem: reproduce global fuselage bending using discrete frame loads while controlling local frame distortion.",
        "Problem: generate fuselage torsion without adding unintended global translation or bending.",
        "Problem: reproduce concentrated multi-axis landing-load components at a small fitting region.",
        "Problem: simultaneously reproduce three forces and three moments at the engine interface.",
        "Problem: apply pylon shear and bending through real attachment lugs without creating unrealistic eccentricity.",
        "Problem: represent occupant/equipment inertia loads through seat rails and floor attachments.",
        "Problem: reproduce store/tank interface forces at two lugs while controlling load sharing.",
        "Problem: reproduce payload-interface forces and moments while allowing small angular misalignment.",
        "Problem: introduce compression/shear uniformly enough that the specimen buckles because of the intended load, not fixture misalignment.",
        "Problem: repeat thousands of load cycles with controlled amplitude, phase and fail-safe limits."
    };

    private static final String[] SOLUTIONS = {
        "Solution: discretize the aerodynamic load into zones, assign a target force to each pad, then synthesize those forces with a multi-level whiffletree. Verify ΣF and Σ(F·x) against the target load case.",
        "Solution: size actuator count from design load, then separately verify stroke, rod buckling, pad allowable, whiffletree beam stress and reaction-frame capacity before release.",
        "Solution: create separate front- and rear-spar branches. Their force difference and spacing generate torque while their sum generates lift/bending.",
        "Solution: use a reinforced load saddle/pad and spherical connection so the actuator remains primarily axial as the wing bends.",
        "Solution: use two opposed forces separated by a known lever arm: M = F·d. Balance the pair so net translational force is near zero.",
        "Solution: use several padded straps/load pads connected to a balanced whiffletree; tune branch ratios to match pressure-resultant locations.",
        "Solution: introduce spherical joints/rod ends in the load train and maintain adequate actuator stroke for expected rotation.",
        "Solution: split the tail into load zones and solve branch forces so both total force and root moment match the aerodynamic target.",
        "Solution: use a horizontal load train with spherical joints and a stiff root reaction frame; monitor out-of-plane reaction.",
        "Solution: use two controlled load lines around the hinge axis and calculate the couple from force × spacing.",
        "Solution: distribute load across selected fuselage frames, then solve frame forces from target shear and bending envelopes.",
        "Solution: use opposed actuator pairs around the cross-section; equal and opposite forces form a torsional couple.",
        "Solution: use a multi-axis fixture/load frame with local reinforcement and measured load cells at the attachment interface.",
        "Solution: coordinate multiple actuators around the mount and solve the 6-DOF equilibrium matrix before commanding loads.",
        "Solution: align load introduction through the actual lug geometry; include pin bearing, shear and fixture eccentricity checks.",
        "Solution: apply inertia-equivalent forces through representative rail/floor interfaces with realistic contact geometry.",
        "Solution: use twin actuator/load-cell channels and tune sharing to the required lug-force ratio.",
        "Solution: use clevis + spherical bearing + load cell chains so small angular changes do not become parasitic bending.",
        "Solution: use guided platen/edge fixtures, alignment measurement and low-friction interfaces; quantify fixture-induced bending.",
        "Solution: use servo-hydraulic closed-loop channels, calibrated load cells, abort limits and synchronized phase control."
    };

    private static final String[] STEP_TITLES = {"1 PROBLEM","2 PHYSICS","3 LOAD SPLIT","4 HARDWARE","5 REACTION","6 VERIFY"};
    private static final String[] STEP_DETAILS = {
        "Define what the aircraft structure must feel: total force, moment, torque, direction, application region and allowable local contact pressure.",
        "Convert the aerodynamic/inertial load into force and moment equilibrium. Key checks are ΣF, ΣM and the correct line of action.",
        "Discretize the continuous load into pad forces. A whiffletree uses lever equilibrium FA·LA = FB·LB to create the required branch ratio.",
        "Build the load train: actuator → spherical bearing/rod end → clevis → load cell → whiffletree beam → links → load pads.",
        "Close the load path into a strongback/strong floor. Every applied force creates an equal reaction that the fixture and anchors must carry.",
        "Before test: check actuator capacity/stroke, pin & clevis shear/bearing, rod buckling, beam stress, pad local stress, load-cell range, fixture stiffness and fail-safe behavior."
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sc = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(30));
        root.setBackgroundColor(Color.rgb(239,243,248));
        sc.addView(root);

        root.addView(text("WHIFFLETREE AERO", 27, true, Color.rgb(15,23,42)));
        TextView sub = text("Visual Aircraft Structural Test Rig Designer", 14, false, Color.rgb(71,85,105));
        sub.setPadding(0,0,0,dp(12)); root.addView(sub);

        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SCENARIOS));
        root.addView(spinner, lp());
        scenarioInfo = card(INFO[0], Color.WHITE); root.addView(scenarioInfo, lp());

        problemText = card(PROBLEMS[0], Color.rgb(255,247,237)); root.addView(problemText, lp());
        solutionText = card(SOLUTIONS[0], Color.rgb(240,253,244)); root.addView(solutionText, lp());

        root.addView(section("VISUAL LOAD PATH"));
        rigView = new RigView();
        root.addView(rigView, new LinearLayout.LayoutParams(-1, dp(330)));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        LinearLayout steps = new LinearLayout(this); steps.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=0;i<STEP_TITLES.length;i++){
            final int idx=i;
            Button btt=new Button(this); btt.setText(STEP_TITLES[i]); btt.setTextSize(12); btt.setAllCaps(false);
            btt.setOnClickListener(v->{ selectedStep=idx; stepDetail.setText(STEP_DETAILS[idx]); rigView.setStep(idx); });
            steps.addView(btt,new LinearLayout.LayoutParams(dp(118),dp(48)));
        }
        hsv.addView(steps); root.addView(hsv,lp());
        stepDetail = card(STEP_DETAILS[0], Color.WHITE); root.addView(stepDetail,lp());

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id){
                selectedScenario=pos; scenarioInfo.setText(INFO[pos]); problemText.setText(PROBLEMS[pos]); solutionText.setText(SOLUTIONS[pos]); rigView.setScenario(pos);
            }
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });

        root.addView(section("CALCULATION INPUTS"));
        loadKn = field(root,"Target total test load [kN]","240");
        pressureBar = field(root,"Hydraulic pressure [bar]","210");
        safety = field(root,"Sizing factor [-]","1.50");
        cylKn = field(root,"Available cylinder nominal force [kN]","100");
        pads = field(root,"Total load pads","8");
        ratioA = field(root,"Whiffletree branch A share [%]","50");
        ratioB = field(root,"Whiffletree branch B share [%]","50");

        Button calc = new Button(this); calc.setText("CALCULATE + UPDATE VISUAL"); calc.setTextSize(16); root.addView(calc, lp());
        result = card("Enter the test load and press CALCULATE. The diagram will update with actuator and pad forces.", Color.WHITE);
        result.setTextSize(15); root.addView(result, lp());
        calc.setOnClickListener(v -> calculate());

        root.addView(section("HOW TO READ THE RIG"));
        root.addView(card("Blue arrows = applied aircraft loads. Orange circles = load pads. Dark beams = whiffletree members. Green block = actuator/load-cell chain. Red base = reaction structure / strong floor. The load path must be continuous from actuator to aircraft and back into the reaction structure.", Color.WHITE), lp());
        root.addView(section("PRE-TEST ENGINEERING GATES"));
        root.addView(card("1) Match target ΣF and ΣM.  2) Check cylinder force + stroke.  3) Check rod buckling and spherical-bearing angle.  4) Check pins/clevises and whiffletree beams.  5) Check pad local stress.  6) Check strong-floor/fixture reactions.  7) Add load-cell limits, abort logic and physical containment. Final hardware release requires project-specific structural substantiation.", Color.WHITE), lp());
        setContentView(sc);
    }

    private void calculate(){
        try {
            double F=d(loadKn), pbar=d(pressureBar), sf=d(safety), cyl=d(cylKn);
            int nPads=Math.max(1,(int)Math.round(d(pads)));
            double a=d(ratioA), b=d(ratioB);
            if(F<=0||pbar<=0||sf<=0||cyl<=0||a<=0||b<=0) throw new Exception();
            double design=F*sf;
            int nCyl=(int)Math.ceil(design/cyl);
            double forcePerCylinder=design/nCyl;
            double pPa=pbar*100000.0;
            double area=forcePerCylinder*1000.0/pPa;
            double boreMm=Math.sqrt(4.0*area/Math.PI)*1000.0;
            double padNom=F/nPads, padDesign=design/nPads;
            double sum=a+b, fracA=a/sum, fracB=b/sum;
            double armRatio=fracB/fracA;
            double reserve=nCyl*cyl-design;
            String risk=reserve < 0.1*design ? "WARNING: cylinder reserve < 10% of design load" : "Preliminary cylinder capacity reserve > 10%";
            result.setText(String.format(Locale.US,
                "LOAD CASE\nTarget %.1f kN  |  Design %.1f kN\n\nACTUATOR SOLUTION\nMinimum count: %d\nDesign force / actuator: %.1f kN\nEquivalent piston bore @ %.0f bar: %.1f mm\nTotal nominal reserve: %.1f kN\n\nPAD SOLUTION\nPads: %d\nNominal force / pad: %.1f kN\nDesign force / pad: %.1f kN\n\nWHIFFLETREE FIRST SPLIT\nA = %.1f%% (%.1f kN)\nB = %.1f%% (%.1f kN)\nLever relation LA/LB = %.3f\n\n%s\n\nInterpretation: the actuator force is not applied directly to one wing point. The whiffletree divides it into controlled pad forces so the structure sees the intended distributed load.",
                F,design,nCyl,forcePerCylinder,pbar,boreMm,reserve,nPads,padNom,padDesign,100*fracA,design*fracA,100*fracB,design*fracB,armRatio,risk));
            rigView.setValues(F,design,nCyl,nPads,padDesign,fracA,fracB);
        } catch(Exception e){ result.setText("Check all inputs. Values must be positive numbers."); }
    }

    private class RigView extends View {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); Paint txt=new Paint(Paint.ANTI_ALIAS_FLAG);
        int scenario=0, step=0, nCyl=1, nPads=8; double target=240,design=360,padF=45,fa=.5,fb=.5;
        RigView(){ super(MainActivity.this); setBackgroundColor(Color.WHITE); txt.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD)); }
        void setScenario(int s){scenario=s; invalidate();}
        void setStep(int s){step=s; invalidate();}
        void setValues(double t,double d,int nc,int np,double pf,double a,double b){target=t;design=d;nCyl=nc;nPads=np;padF=pf;fa=a;fb=b;invalidate();}
        void line(Canvas c,float x1,float y1,float x2,float y2,int color,float w){p.setColor(color);p.setStrokeWidth(w);p.setStyle(Paint.Style.STROKE);c.drawLine(x1,y1,x2,y2,p);}
        void box(Canvas c,float l,float t,float r,float b,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,r,b,10,10,p);}
        void label(Canvas c,String s,float x,float y,int color,float size){txt.setColor(color);txt.setTextSize(size);c.drawText(s,x,y,txt);}
        void arrow(Canvas c,float x,float y1,float y2,int color){line(c,x,y1,x,y2,color,5); p.setColor(color);p.setStyle(Paint.Style.FILL);Path q=new Path();q.moveTo(x,y2);q.lineTo(x-9,y2-14);q.lineTo(x+9,y2-14);q.close();c.drawPath(q,p);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth();float h=getHeight();int blue=Color.rgb(37,99,235),orange=Color.rgb(234,88,12),dark=Color.rgb(51,65,85),green=Color.rgb(22,163,74),red=Color.rgb(185,28,28),muted=Color.rgb(100,116,139);
            label(c,"Scenario "+(scenario+1)+"  •  Step "+(step+1),14,24,muted,13*getResources().getDisplayMetrics().scaledDensity);
            boolean torsion=(scenario==2||scenario==4||scenario==9||scenario==11||scenario==13);
            float wingY=95; p.setColor(Color.rgb(226,232,240));p.setStyle(Paint.Style.FILL);Path wing=new Path();wing.moveTo(22,wingY);wing.lineTo(w-22,wingY+15);wing.lineTo(w-55,wingY+58);wing.lineTo(22,wingY+45);wing.close();c.drawPath(wing,p);
            label(c,torsion?"TEST ARTICLE / TWO LOAD LINES":"TEST ARTICLE / DISTRIBUTED LOAD",28,wingY+35,Color.rgb(30,41,59),12*getResources().getDisplayMetrics().scaledDensity);
            int shown=Math.min(Math.max(nPads,2),8);for(int i=0;i<shown;i++){float x=35+i*(w-70)/(shown-1);arrow(c,x,45,wingY-3,blue);p.setColor(orange);c.drawCircle(x,wingY+50,7,p);}
            label(c,String.format(Locale.US,"Target %.0f kN",target),w-120,55,blue,11*getResources().getDisplayMetrics().scaledDensity);
            float beam1=190; line(c,55,beam1,w-55,beam1,dark,7); for(int i=0;i<shown;i++){float x=35+i*(w-70)/(shown-1);line(c,x,wingY+56,x,beam1,dark,2);}
            label(c,String.format(Locale.US,"Pads ≈ %.1f kN design each",padF),22,beam1+25,orange,11*getResources().getDisplayMetrics().scaledDensity);
            float mid=w/2f; float beam2=235;line(c,95,beam2,w-95,beam2,dark,8);line(c,w*.28f,beam1,w*.34f,beam2,dark,3);line(c,w*.72f,beam1,w*.66f,beam2,dark,3);
            label(c,String.format(Locale.US,"A %.0f%%",fa*100),78,beam2-10,dark,11*getResources().getDisplayMetrics().scaledDensity);label(c,String.format(Locale.US,"B %.0f%%",fb*100),w-145,beam2-10,dark,11*getResources().getDisplayMetrics().scaledDensity);
            line(c,mid,beam2,mid,267,dark,4);box(c,mid-48,267,mid+48,304,green);label(c,"ACTUATOR",mid-38,289,Color.WHITE,11*getResources().getDisplayMetrics().scaledDensity);
            label(c,String.format(Locale.US,"%d cyl • design %.0f kN total",nCyl,design),18,316,green,11*getResources().getDisplayMetrics().scaledDensity);
            box(c,15,h-20,w-15,h-5,red);label(c,"REACTION / STRONG FLOOR",w/2-82,h-8,Color.WHITE,10*getResources().getDisplayMetrics().scaledDensity);
            if(step==0){p.setColor(Color.argb(45,37,99,235));p.setStyle(Paint.Style.FILL);c.drawRect(10,35,w-10,wingY+65,p);} else if(step==2){p.setColor(Color.argb(45,234,88,12));c.drawRect(10,wingY+42,w-10,beam2+12,p);} else if(step==3){p.setColor(Color.argb(45,22,163,74));c.drawRect(mid-60,255,mid+60,310,p);} else if(step==4){p.setColor(Color.argb(40,185,28,28));c.drawRect(8,h-34,w-8,h,p);} 
        }
    }

    private double d(EditText e){ return Double.parseDouble(e.getText().toString().trim().replace(',','.')); }
    private EditText field(LinearLayout root,String label,String val){TextView l=text(label,14,true,Color.rgb(51,65,85));l.setPadding(0,dp(7),0,dp(2));root.addView(l);EditText e=new EditText(this);e.setText(val);e.setTextSize(17);e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);root.addView(e,lp());return e;}
    private TextView section(String s){TextView t=text(s,18,true,Color.rgb(15,23,42));t.setPadding(0,dp(18),0,dp(6));return t;}
    private TextView card(String s,int bg){TextView t=text(s,14,false,Color.rgb(30,41,59));t.setPadding(dp(14),dp(14),dp(14),dp(14));GradientDrawable g=new GradientDrawable();g.setColor(bg);g.setCornerRadius(dp(12));g.setStroke(dp(1),Color.rgb(226,232,240));t.setBackground(g);return t;}
    private TextView text(String s,int sp,boolean bold,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private LinearLayout.LayoutParams lp(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(8));return p;}
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
}
