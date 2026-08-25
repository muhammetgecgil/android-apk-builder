package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.text.*;
import android.widget.*;
import java.util.*;

public class V601Activity extends V60Activity {
    TextView hardeningBanner, fingerprintCard, exceptionCard;
    String lastError="None";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout p=new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(8),dp(8),dp(8),dp(8));
        p.setBackground(bg(Color.rgb(4,20,32),14));
        p.addView(tx("v6.0.1 RELEASE HARDENING",18,true,Color.WHITE));
        p.addView(tx("Auto gate • fingerprint integrity • explicit calculation faults",9,false,Color.rgb(180,210,230)));
        hardeningBanner=card("Hardening checks starting...",Color.rgb(17,46,66));p.addView(hardeningBanner,lp());
        fingerprintCard=card("Project fingerprint state...",Color.rgb(20,51,72));p.addView(fingerprintCard,lp());
        exceptionCard=card("No calculation exception recorded.",Color.rgb(20,51,72));p.addView(exceptionCard,lp());
        Button rerun=new Button(this);rerun.setText("RE-RUN HARDENED RELEASE GATE");rerun.setOnClickListener(v->refreshHardening(true));p.addView(rerun,lp());
        root.addView(p,Math.min(2,root.getChildCount()),lp());

        TextWatcher tw=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int d){}public void onTextChanged(CharSequence s,int a,int b,int c){refreshHardening(false);}public void afterTextChanged(Editable e){}};
        for(EditText e:new EditText[]{F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf,padW,padL})e.addTextChangedListener(tw);
        refreshHardening(false);
    }

    boolean savedFingerprintMatches(){
        if(prefs==null)return true;
        String saved=prefs.getString("savedFingerprint","");
        return saved.length()==0 || saved.equals(fingerprint());
    }

    @Override void saveProject(){
        super.saveProject();
        try{
            if(prefs!=null && runValidation(false)){
                prefs.edit().putString("savedFingerprint",fingerprint()).apply();
                fingerprintCard.setText("SAVED PROJECT FINGERPRINT: "+fingerprint()+"\nSaved and active engineering states match.");
            }
        }catch(Exception e){recordError("Save/fingerprint",e);}
        refreshHardening(false);
    }

    @Override void loadProject(){
        try{
            super.loadProject();
            refreshHardening(false);
        }catch(Exception e){recordError("Project load",e);}
    }

    void recordError(String where,Exception e){
        lastError=where+": "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());
        if(exceptionCard!=null)exceptionCard.setText("CALCULATION / STATE EXCEPTION\n"+lastError+"\nReview the related input or reload the last saved project before relying on results.");
    }

    boolean safeRegression(){
        try{return regressionPass();}catch(Exception e){recordError("Regression suite",e);return false;}
    }
    boolean safeVerification(){
        try{return currentVerificationPass();}catch(Exception e){recordError("Independent verification",e);return false;}
    }
    boolean safeProject(){
        try{return projectIntegrityPass()&&savedFingerprintMatches();}catch(Exception e){recordError("Project integrity",e);return false;}
    }

    boolean refreshHardening(boolean toast){
        boolean input=false,reg=false,verify=false,project=false,visual=false;
        try{input=runValidation(false);}catch(Exception e){recordError("Input validation",e);}
        reg=safeRegression(); verify=safeVerification(); project=safeProject();
        try{visual=workspace!=null&&workspace.c!=null;}catch(Exception e){recordError("Visual sync",e);}
        boolean all=input&&reg&&verify&&project&&visual;
        String saved=prefs==null?"":prefs.getString("savedFingerprint","");
        String active=fingerprint();
        hardeningBanner.setText((all?"HARDENED READY":"HARDENING BLOCKED")+
                "\nValidation "+yn(input)+" • Regression "+yn(reg)+" • Verification "+yn(verify)+" • Project "+yn(project)+" • Visual "+yn(visual));
        fingerprintCard.setText("ACTIVE PROJECT: "+active+"\nSAVED PROJECT: "+(saved.length()==0?"not recorded":saved)+
                "\n"+(saved.length()==0?"Save the project to establish a persistence fingerprint.":(saved.equals(active)?"MATCH — saved and active states are aligned.":"MISMATCH — active inputs changed after the last save.")));
        if(lastError.equals("None"))exceptionCard.setText("NO CALCULATION EXCEPTIONS\nAll hardening checks executed without a trapped runtime calculation/state error.");
        if(toast)Toast.makeText(this,all?"v6.0.1 hardened gate PASS":"v6.0.1 gate BLOCKED",Toast.LENGTH_SHORT).show();
        return all;
    }

    String yn(boolean x){return x?"PASS":"FAIL";}

    @Override boolean runStableGate(boolean toast){
        if(hardeningBanner==null)return super.runStableGate(toast);
        return refreshHardening(toast);
    }

    @Override void calculate(){
        try{
            if(!refreshHardening(false)){
                result.setText("CALCULATION BLOCKED — v6.0.1 HARDENING GATE\nValidation, regression, independent verification, project fingerprint integrity and visual synchronization must all pass.");
                Toast.makeText(this,"Hardened release gate blocked",Toast.LENGTH_SHORT).show();
                return;
            }
            super.calculate();
            refreshHardening(false);
        }catch(Exception e){
            recordError("Calculate",e);
            result.setText("CALCULATION EXCEPTION\n"+lastError+"\nNo stable engineering result has been released.");
        }
    }
}
