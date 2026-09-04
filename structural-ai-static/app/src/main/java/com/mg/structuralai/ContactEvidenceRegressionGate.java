package com.mg.structuralai;

/** Regression for contact-evidence safety policy. */
public final class ContactEvidenceRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private static volatile Result cached;
    private ContactEvidenceRegressionGate(){}

    public static Result run(){
        Result c=cached;if(c!=null)return c;
        try{
            ContactEvidencePolicy.Assessment bonded=ContactEvidencePolicy.assess(
                new ContactEvidencePolicy.Definition(ContactEvidencePolicy.Mode.BONDED,null,"drawing/weld callout",0.0,0.95));
            ContactEvidencePolicy.Assessment frictionless=ContactEvidencePolicy.assess(
                new ContactEvidencePolicy.Definition(ContactEvidencePolicy.Mode.FRICTIONLESS,null,"explicit analyst contact",0.0,0.90));
            ContactEvidencePolicy.Assessment noSep=ContactEvidencePolicy.assess(
                new ContactEvidencePolicy.Definition(ContactEvidencePolicy.Mode.NO_SEPARATION,null,"explicit analyst contact",0.0,0.90));
            ContactEvidencePolicy.Assessment missingMu=ContactEvidencePolicy.assess(
                new ContactEvidencePolicy.Definition(ContactEvidencePolicy.Mode.FRICTIONAL,null,"test source",0.0,0.95));
            ContactEvidencePolicy.Assessment frictionEvidence=ContactEvidencePolicy.assess(
                new ContactEvidencePolicy.Definition(ContactEvidencePolicy.Mode.FRICTIONAL,0.25,"validated tribology input",0.0,0.95));
            boolean pass=bonded.numericallyAdmissible&&bonded.designEvidenceReady&&frictionless.numericallyAdmissible&&noSep.numericallyAdmissible&&!missingMu.numericallyAdmissible&&!frictionEvidence.numericallyAdmissible;
            String s="CONTACT EVIDENCE REGRESSION "+(pass?"PASS":"FAIL")+
                " | bondedReady="+bonded.designEvidenceReady+
                " | frictionlessNumerical="+frictionless.numericallyAdmissible+
                " | noSepNumerical="+noSep.numericallyAdmissible+
                " | missingMuBlocked="+(!missingMu.numericallyAdmissible)+
                " | frictionalReleaseBlocked="+(!frictionEvidence.numericallyAdmissible);
            return cached=new Result(pass,s);
        }catch(Throwable t){return cached=new Result(false,"CONTACT EVIDENCE REGRESSION ERROR: "+t.getMessage());}
    }
}
