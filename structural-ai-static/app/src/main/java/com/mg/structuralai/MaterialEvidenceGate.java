package com.mg.structuralai;

import java.util.*;

/** Verifies material data integrity and enforces separation of reference data from design-release evidence. */
public final class MaterialEvidenceGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private static volatile Result cached;
    private MaterialEvidenceGate(){}

    public static Result run(){
        Result c=cached;if(c!=null)return c;
        try{
            StringBuilder sb=new StringBuilder();boolean ok=true;int count=0;
            for(MaterialLibrary.Entry e:MaterialLibrary.all()){
                count++;LinearElasticMaterial m=e.material;
                boolean elastic=m.youngPa>=10e9&&m.youngPa<=300e9&&m.poisson>0&&m.poisson<0.49&&m.densityKgM3>=1000&&m.densityKgM3<=20000;
                boolean strength=m.hasStrengthEvidence()&&m.ultimatePa>=m.yieldPa;
                boolean referenceBlocked=!m.designReleaseEligible();
                boolean row=elastic&&strength&&referenceBlocked;ok&=row;
                sb.append('\n').append(e.id).append(": ").append(row?"PASS":"FAIL").append(" | ").append(e.summary()).append(" | designReleaseBlocked=").append(referenceBlocked);
            }
            LinearElasticMaterial certified=MaterialLibrary.userCertified("QA_CERTIFIED",70e9,0.33,2700,250e6,300e6,"QA-CERT-001");
            boolean certifiedPass=certified.designReleaseEligible();ok&=certifiedPass;
            boolean missingProvBlocked=false;try{MaterialLibrary.userCertified("BAD",70e9,0.33,2700,250e6,300e6,"");}catch(IllegalArgumentException ex){missingProvBlocked=true;}ok&=missingProvBlocked;
            String summary="MATERIAL EVIDENCE GATE "+(ok?"PASS":"FAIL")+" | entries="+count+" | certifiedPath="+certifiedPass+" | missingProvenanceBlocked="+missingProvBlocked+sb;
            return cached=new Result(ok,summary);
        }catch(Throwable t){return cached=new Result(false,"MATERIAL EVIDENCE GATE ERROR: "+t.getMessage());}
    }
}
