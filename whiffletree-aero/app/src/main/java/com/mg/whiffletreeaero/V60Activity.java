package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import java.util.*;

public class V60Activity extends V59Activity {
    TextView stableBanner, gateCard, releaseCard;
    Button stableGateButton;
    boolean lastRegressionOk=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout p=new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(8),dp(8),dp(8),dp(8));
        p.setBackground(bg(Color.rgb(4,22,34),14));
        p.addView(tx("v6.0 STABLE ENGINEERING RELEASE",20,true,Color.WHITE));
        p.addView(tx("Release gate • deterministic benchmarks • project integrity • concept-design lock",9,false,Color.rgb(180,210,230)));
        stableBanner=card("Stable release gate evaluating...",Color.rgb(17,46,66));p.addView(stableBanner,lp());
        stableGateButton=new Button(this);stableGateButton.setText("RUN FULL RELEASE GATE");stableGateButton.setOnClickListener(v->runStableGate(true));p.addView(stableGateButton,lp());
        gateCard=card("Release gate details...",Color.rgb(20,51,72));p.addView(gateCard,lp());
        releaseCard=card("Release basis...",Color.rgb(15,43,58));p.addView(releaseCard,lp());
        root.addView(p,Math.min(1,root.getChildCount()),lp());
        runStableGate(false);
    }

    boolean regressionPass(){
        int pass=0,total=0;
        for(Bench b:suite()){total++;BR r=solveBench(b);if(r.pass)pass++;}
        return total>0&&pass==total;
    }

    boolean currentVerificationPass(){
        try{
            Calc c=compute(false);
            double sf0=0,sm=0,sa=0;
            for(int i=0;i<c.n;i++){sf0+=c.fi[i];sm+=c.fi[i]*c.x[i];}
            for(double v:c.actLoads)sa+=v;
            double ef=100*Math.abs(sf0-c.targetF)/Math.max(1,c.targetF);
            double em=100*Math.abs(Math.abs(sm)-Math.abs(c.targetM))/Math.max(1,Math.abs(c.targetM));
            double ea=100*Math.abs(sa-c.designF)/Math.max(1,c.designF);
            return ef<=1.0&&em<=2.0&&ea<=2.0;
        }catch(Exception e){return false;}
    }

    boolean projectIntegrityPass(){
        try{
            Calc c=compute(false);ensureHardpoints(c);
            if(hpXv==null||hpYv==null||hpZv==null)return false;
            if(hpXv.length!=c.n||hpYv.length!=c.n||hpZv.length!=c.n)return false;
            for(int i=0;i<c.n;i++)if(!Double.isFinite(hpXv[i])||!Double.isFinite(hpYv[i])||!Double.isFinite(hpZv[i]))return false;
            return fingerprint()!=null&&fingerprint().length()==8;
        }catch(Exception e){return false;}
    }

    boolean runStableGate(boolean toast){
        boolean inputOk=runValidation(false);
        lastRegressionOk=regressionPass();
        boolean verifyOk=currentVerificationPass();
        boolean projectOk=projectIntegrityPass();
        boolean visualOk=workspace!=null&&workspace.c!=null;
        boolean all=inputOk&&lastRegressionOk&&verifyOk&&projectOk&&visualOk;

        stableBanner.setText((all?"STABLE / READY — CONCEPT ENGINEERING RELEASE":"RELEASE BLOCKED — REVIEW REQUIRED")+
            "\nProject "+fingerprint()+" • v6.0 calculation state");
        gateCard.setText(
            (inputOk?"PASS":"FAIL")+"  Input & engineering validation\n"+
            (lastRegressionOk?"PASS":"FAIL")+"  24-case deterministic regression suite\n"+
            (verifyOk?"PASS":"FAIL")+"  Independent force/moment/actuator closure\n"+
            (projectOk?"PASS":"FAIL")+"  Project & hardpoint integrity\n"+
            (visualOk?"PASS":"FAIL")+"  EFT visual workspace synchronized\n\n"+
            (all?"All release gates passed. Sizing outputs are internally consistent at concept-design level.":"One or more gates failed. Stable status is withheld until corrected."));
        releaseCard.setText(
            "v6.0 RELEASE BASIS\n"+
            "• EFT-centered visual workflow\n"+
            "• guarded calculations and validation gates\n"+
            "• saved project state + hardpoint integrity\n"+
            "• 24 deterministic regression benchmarks\n"+
            "• independent equilibrium verification\n"+
            "• common active-state basis for visual/BOM/report/validation\n\n"+
            "Scope: preliminary/concept test-rig engineering. STABLE does not mean flight/airworthiness approval. Final rig release still requires approved loads, real material allowables, supplier ratings, structural/FEM verification, drawing/CAD hardpoints and formal test-safety review.");
        if(toast)Toast.makeText(this,all?"v6.0 release gate PASS":"Release gate BLOCKED",Toast.LENGTH_SHORT).show();
        return all;
    }

    @Override void calculate(){
        if(!runStableGate(false)){
            result.setText("CALCULATION / RELEASE BLOCKED\nThe v6.0 stable gate is not fully satisfied. Review validation, regression, closure and project-integrity results.");
            Toast.makeText(this,"v6.0 release gate blocked",Toast.LENGTH_SHORT).show();
            return;
        }
        super.calculate();
        runStableGate(false);
    }
}
