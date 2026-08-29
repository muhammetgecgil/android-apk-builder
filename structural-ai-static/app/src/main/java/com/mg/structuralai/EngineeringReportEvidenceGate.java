package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;

/** P1 report contract: an engineering report must carry the full traceable evidence chain. */
public final class EngineeringReportEvidenceGate {
    public static final class Result {
        public final boolean pass;
        public final String summary;
        Result(boolean p,String s){pass=p;summary=s;}
    }

    private static final String[] REQUIRED={
        "MODEL",
        "Format:",
        "Unit scale:",
        "BOUNDARY CONDITIONS",
        "Support patches:",
        "Resultant load:",
        "MATERIAL EVIDENCE",
        "MESH / CONVERGENCE",
        "NUMERICAL QA",
        "force equilibrium",
        "residual",
        "RESULTS",
        "Safety factor"
    };

    private EngineeringReportEvidenceGate(){}

    public static Result validate(String report){
        String r=report==null?"":report;
        List<String> missing=new ArrayList<>();
        for(String k:REQUIRED)if(!r.contains(k))missing.add(k);
        boolean pass=missing.isEmpty();
        return new Result(pass,"ENGINEERING REPORT EVIDENCE "+(pass?"PASS":"FAIL")+" | required="+REQUIRED.length+" | missing="+missing);
    }

    public static Result run(){
        String complete="MODEL\npart.step\nFormat: STEP\nUnit scale: 0.001 m/model-unit\n"+
            "BOUNDARY CONDITIONS\nSupport patches: 2\nResultant load: [0,0,-1000] N\n"+
            "MATERIAL EVIDENCE\nUSER_CERTIFIED source=EN10025\n"+
            "MESH / CONVERGENCE\n8/12/16 converged=true\n"+
            "NUMERICAL QA\nforce equilibrium error=1e-8\nresidual=1e-10\n"+
            "RESULTS\nUmax=0.2 mm\nVM=120 MPa\nSafety factor=2.95";
        Result ok=validate(complete);
        Result bad=validate("MODEL\nRESULTS\nSafety factor=BLOCKED");
        boolean pass=ok.pass&&!bad.pass;
        return new Result(pass,"ENGINEERING REPORT EVIDENCE REGRESSION "+(pass?"PASS":"FAIL")+" | completeAccepted="+ok.pass+" | incompleteBlocked="+(!bad.pass)+" | "+bad.summary);
    }
}
