package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;

public final class CredibilityGate {
    public static final class GateResult {
        public final boolean mayClaimEngineeringPass;
        public final double combinedConfidence;
        public final List<String> blockers;
        GateResult(boolean pass,double c,List<String> b){mayClaimEngineeringPass=pass;combinedConfidence=c;blockers=b;}
    }

    public GateResult evaluate(List<EngineeringAssumption> assumptions,
                               boolean unitsResolved,
                               boolean solverConverged,
                               boolean equilibriumPassed,
                               boolean meshConverged,
                               boolean singularityReviewed) {
        List<String> blockers=new ArrayList<>();
        if(!unitsResolved) blockers.add("Model birimi çözümlenmedi");
        if(!solverConverged) blockers.add("Solver convergence doğrulanmadı");
        if(!equilibriumPassed) blockers.add("Kuvvet/moment dengesi doğrulanmadı");
        if(!meshConverged) blockers.add("Mesh convergence doğrulanmadı");
        if(!singularityReviewed) blockers.add("Singularity/hotspot incelemesi tamamlanmadı");

        double logSum=0.0; int n=0;
        for(EngineeringAssumption a:assumptions){
            if(a.solverCritical){
                if(a.confidence<0.80) blockers.add(a.kind+" güveni düşük: "+a.value);
                logSum+=Math.log(Math.max(1e-6,a.confidence)); n++;
            }
        }
        double combined=n==0?0.0:Math.exp(logSum/n);
        return new GateResult(blockers.isEmpty(),combined,blockers);
    }
}
