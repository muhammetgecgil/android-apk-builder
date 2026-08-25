package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.*;
import java.util.*;

public class V57Activity extends V56Activity {
    TextView maturityInfo, validationInfo, projectInfo;
    SharedPreferences prefs;
    final String PREFS="wt_aero_project_v57";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);

        LinearLayout panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8),dp(8),dp(8),dp(8));
        panel.setBackground(bg(Color.rgb(7,26,39),14));
        panel.addView(tx("v5.7 MATURITY CORE",18,true,Color.WHITE));
        panel.addView(tx("Validate • Save • Restore • Navigate • Recalculate",9,false,Color.rgb(180,210,230)));

        maturityInfo=card("Maturity checks preparing...",Color.rgb(17,46,66));
        panel.addView(maturityInfo,lp());

        LinearLayout nav1=new LinearLayout(this);nav1.setOrientation(LinearLayout.HORIZONTAL);
        nav1.addView(nav("EFT",0),new LinearLayout.LayoutParams(0,dp(46),1));
        nav1.addView(nav("TREE",1),new LinearLayout.LayoutParams(0,dp(46),1));
        nav1.addView(nav("MECH",2),new LinearLayout.LayoutParams(0,dp(46),1));
        nav1.addView(nav("HYD",5),new LinearLayout.LayoutParams(0,dp(46),1));
        panel.addView(nav1);
        LinearLayout nav2=new LinearLayout(this);nav2.setOrientation(LinearLayout.HORIZONTAL);
        nav2.addView(nav("BOM",6),new LinearLayout.LayoutParams(0,dp(46),1));
        nav2.addView(nav("VALIDATE",7),new LinearLayout.LayoutParams(0,dp(46),1));
        nav2.addView(nav("RISK",8),new LinearLayout.LayoutParams(0,dp(46),1));
        nav2.addView(nav("REVIEW",9),new LinearLayout.LayoutParams(0,dp(46),1));
        panel.addView(nav2);

        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button validate=new Button(this);validate.setText("VALIDATE");validate.setOnClickListener(v->runValidation(true));
        Button save=new Button(this);save.setText("SAVE PROJECT");save.setOnClickListener(v->saveProject());
        Button load=new Button(this);load.setText("LOAD PROJECT");load.setOnClickListener(v->loadProject());
        actions.addView(validate,new LinearLayout.LayoutParams(0,dp(50),1));
        actions.addView(save,new LinearLayout.LayoutParams(0,dp(50),1));
        actions.addView(load,new LinearLayout.LayoutParams(0,dp(50),1));
        panel.addView(actions);

        validationInfo=card("No validation run yet.",Color.rgb(20,51,72));panel.addView(validationInfo,lp());
        projectInfo=card("Project state is local to this device.",Color.rgb(20,51,72));panel.addView(projectInfo,lp());
        root.addView(panel,37,lp());
        runValidation(false);
    }

    Button nav(String label,int view){
        Button b=new Button(this);b.setText(label);b.setOnClickListener(v->{viewMode.setSelection(view);scrollTop();});return b;
    }
    void scrollTop(){try{View par=(View)root.getParent();if(par instanceof ScrollView)((ScrollView)par).smoothScrollTo(0,0);}catch(Exception ignored){}}

    static class VR{boolean ok;String msg;VR(boolean o,String m){ok=o;msg=m;}}
    ArrayList<VR> validateInputs(){
        ArrayList<VR> r=new ArrayList<>();
        double len=d(L),dia=d(D),force=d(F),mom=d(M),ns=d(stations),nl=d(layers),na=d(actCount),cap=d(actCapacity),pr=d(pressure),factor=d(sf),pw=d(padW),pl=d(padL);
        r.add(new VR(len>=0.5&&len<=30,"Tank length 0.5…30 m"));
        r.add(new VR(dia>=0.1&&dia<=5,"Tank diameter 0.1…5 m"));
        r.add(new VR(force>0&&force<=5000,"Target force >0 and ≤5000 kN"));
        r.add(new VR(Math.abs(mom)<=50000,"Target moment |M| ≤50000 kN·m"));
        r.add(new VR(ns>=4&&ns<=16&&Math.rint(ns)==ns,"Station count integer 4…16"));
        r.add(new VR(nl>=1&&nl<=3&&Math.rint(nl)==nl,"Whiffletree layers integer 1…3"));
        r.add(new VR(na>=1&&na<=8&&Math.rint(na)==na,"Actuator count integer 1…8"));
        r.add(new VR(cap>0,"Actuator capacity >0 kN"));
        r.add(new VR(pr>=20&&pr<=700,"Hydraulic pressure 20…700 bar"));
        r.add(new VR(factor>=1&&factor<=3,"Sizing factor 1.0…3.0"));
        r.add(new VR(pw>=20&&pl>=20,"Pad dimensions ≥20 mm"));
        try{
            Calc c=compute(false);
            r.add(new VR(Math.abs(c.sumF-c.targetF)/Math.max(1,c.targetF)<=0.01,"ΣF closure ≤1%"));
            r.add(new VR(Math.abs(c.mErr)<=2,"ΣM closure ≤2%"));
            r.add(new VR(max(c.actLoads)<=Math.max(1,cap),"Actuator group load ≤ nominal capacity"));
            r.add(new VR(c.linkAngle<=12,"Link angle ≤12° screening limit"));
            r.add(new VR(c.strokeReq<=Math.max(120,d(deflection)*2.5),"Stroke envelope internally consistent"));
        }catch(Exception e){r.add(new VR(false,"Calculation core could not evaluate inputs"));}
        return r;
    }

    boolean runValidation(boolean toast){
        ArrayList<VR> a=validateInputs();int pass=0;StringBuilder sb=new StringBuilder();
        for(VR q:a){if(q.ok)pass++;sb.append(q.ok?"PASS  ":"FAIL  ").append(q.msg).append("\n");}
        boolean ok=pass==a.size();
        validationInfo.setText((ok?"INPUT / CALCULATION GATE: PASS":"INPUT / CALCULATION GATE: BLOCKED")+"\n"+pass+" / "+a.size()+" checks passed\n\n"+sb.toString().trim());
        maturityInfo.setText(ok?"ENGINEERING STATE: READY FOR CONCEPT CALCULATION\nInputs and basic equilibrium gates are valid. Detailed release checks still required.":"ENGINEERING STATE: NOT READY\nFix FAIL items before relying on sizing or optimization outputs.");
        if(toast)Toast.makeText(this,ok?"Validation PASS":"Validation blocked — review FAIL items",Toast.LENGTH_SHORT).show();
        return ok;
    }

    void put(SharedPreferences.Editor e,String k,EditText v){e.putString(k,v.getText().toString());}
    void set(EditText v,String k){if(prefs.contains(k))v.setText(prefs.getString(k,v.getText().toString()));}
    String arr(double[] a){if(a==null)return "";StringBuilder s=new StringBuilder();for(int i=0;i<a.length;i++){if(i>0)s.append(',');s.append(String.format(Locale.US,"%.3f",a[i]));}return s.toString();}
    double[] parseArr(String s,int n){String[] p=s.split(",");if(p.length!=n)return null;double[] a=new double[n];for(int i=0;i<n;i++)a[i]=Double.parseDouble(p[i]);return a;}

    void saveProject(){
        if(!runValidation(false)){projectInfo.setText("SAVE BLOCKED: project has invalid engineering inputs. Resolve validation failures first.");return;}
        try{
            Calc c=compute(false);ensureHardpoints(c);
            SharedPreferences.Editor e=prefs.edit();
            put(e,"F",F);put(e,"M",M);put(e,"L",L);put(e,"D",D);put(e,"stations",stations);put(e,"layers",layers);put(e,"actCount",actCount);put(e,"actCapacity",actCapacity);put(e,"pressure",pressure);put(e,"sf",sf);put(e,"padW",padW);put(e,"padL",padL);put(e,"deflection",deflection);put(e,"linkLength",linkLength);put(e,"beamSpan",beamSpan);put(e,"beamH",beamH);put(e,"beamB",beamB);put(e,"pinAllow",pinAllow);put(e,"bearingAllow",bearingAllow);
            e.putInt("loadCase",loadCase.getSelectedItemPosition());e.putInt("qProfile",qProfile.getSelectedItemPosition());e.putInt("loadPct",loadPct);e.putInt("layerFilter",layerFilter);e.putBoolean("expert",expertMode.isChecked());e.putString("hpX",arr(hpXv));e.putString("hpY",arr(hpYv));e.putString("hpZ",arr(hpZv));e.apply();
            projectInfo.setText("PROJECT SAVED\nCore loads, geometry, topology, sizing inputs, visual state and station hardpoints stored locally.");
        }catch(Exception ex){projectInfo.setText("Project save failed: "+ex.getMessage());}
    }

    void loadProject(){
        if(!prefs.contains("F")){projectInfo.setText("No saved v5.7 project found on this device.");return;}
        try{
            set(F,"F");set(M,"M");set(L,"L");set(D,"D");set(stations,"stations");set(layers,"layers");set(actCount,"actCount");set(actCapacity,"actCapacity");set(pressure,"pressure");set(sf,"sf");set(padW,"padW");set(padL,"padL");set(deflection,"deflection");set(linkLength,"linkLength");set(beamSpan,"beamSpan");set(beamH,"beamH");set(beamB,"beamB");set(pinAllow,"pinAllow");set(bearingAllow,"bearingAllow");
            loadCase.setSelection(prefs.getInt("loadCase",0));qProfile.setSelection(prefs.getInt("qProfile",0));loadPct=prefs.getInt("loadPct",100);loadSeek.setProgress(loadPct);layerFilter=prefs.getInt("layerFilter",0);layerSeek.setProgress(layerFilter);expertMode.setChecked(prefs.getBoolean("expert",false));
            Calc c=compute(false);ensureHardpoints(c);double[] x=parseArr(prefs.getString("hpX",""),c.n),y=parseArr(prefs.getString("hpY",""),c.n),z=parseArr(prefs.getString("hpZ",""),c.n);if(x!=null&&y!=null&&z!=null){hpXv=x;hpYv=y;hpZv=z;updateHpFields();}
            refreshWorkspace();runValidation(false);projectInfo.setText("PROJECT RESTORED\nSaved inputs, visual state and compatible hardpoints loaded and recalculated.");
        }catch(Exception ex){projectInfo.setText("Project restore failed: saved state is incompatible or damaged.");}
    }
}
