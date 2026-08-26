package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Self-test for section-profile recognition and section-property math. */
public final class ProfileRegressionGate {
    public static final class Result {
        public final boolean pass; public final String summary;
        Result(boolean p,String s){pass=p;summary=s;}
    }
    private ProfileRegressionGate(){}

    public static Result run(){
        StringBuilder sb=new StringBuilder(); boolean ok=true;
        ok &= check(OpenSectionTopologyClassifier.Type.I_SECTION, loop(I()), sb);
        ok &= check(OpenSectionTopologyClassifier.Type.C_SECTION, loop(C()), sb);
        ok &= check(OpenSectionTopologyClassifier.Type.T_SECTION, loop(T()), sb);
        ok &= check(OpenSectionTopologyClassifier.Type.L_SECTION, loop(L()), sb);

        CrossSectionAnalyzer.Result rect=section(loop(new double[][]{{0,0},{20,0},{20,10},{0,10},{0,0}}));
        SectionProperties.Result sp=SectionProperties.compute(rect);
        boolean props=sp.valid && rel(sp.area,200.0)<1e-9 && rel(sp.cx,10.0)<1e-9 && rel(sp.cy,5.0)<1e-9;
        ok &= props;
        sb.append(String.format(Locale.US,"RECT properties: %s | A=%.6g | C=(%.6g,%.6g)\n",props?"PASS":"FAIL",sp.area,sp.cx,sp.cy));
        return new Result(ok,(ok?"PROFILE REGRESSION PASS":"PROFILE REGRESSION FAIL")+"\n"+sb);
    }

    private static boolean check(OpenSectionTopologyClassifier.Type expected,CrossSectionAnalyzer.Loop l,StringBuilder sb){
        OpenSectionTopologyClassifier.Result r=OpenSectionTopologyClassifier.classify(section(l));
        boolean pass=r.type==expected && r.confidence>=0.80;
        sb.append(String.format(Locale.US,"%s: %s | got=%s | conf=%.3f | score=%.3f | margin=%.3f\n",expected,pass?"PASS":"FAIL",r.type,r.confidence,r.score,r.margin));
        return pass;
    }
    private static CrossSectionAnalyzer.Result section(CrossSectionAnalyzer.Loop l){List<CrossSectionAnalyzer.Loop> a=new ArrayList<>();a.add(l);return new CrossSectionAnalyzer.Result(0,a,true,"synthetic regression section");}
    private static CrossSectionAnalyzer.Loop loop(double[][] xy){List<double[]> p=new ArrayList<>();for(double[] q:xy)p.add(new double[]{q[0],q[1]});double a=0,per=0;for(int i=0;i<p.size()-1;i++){double[] u=p.get(i),v=p.get(i+1);a+=u[0]*v[1]-v[0]*u[1];per+=Math.hypot(v[0]-u[0],v[1]-u[1]);}return new CrossSectionAnalyzer.Loop(p,0.5*a,per);}
    private static double rel(double a,double b){return Math.abs(a-b)/Math.max(Math.max(Math.abs(a),Math.abs(b)),1e-30);}

    private static double[][] I(){return new double[][]{{0,0},{1,0},{1,.18},{.59,.18},{.59,.82},{1,.82},{1,1},{0,1},{0,.82},{.41,.82},{.41,.18},{0,.18},{0,0}};}
    private static double[][] C(){return new double[][]{{0,0},{1,0},{1,.18},{.18,.18},{.18,.82},{1,.82},{1,1},{0,1},{0,0}};}
    private static double[][] T(){return new double[][]{{0,0},{1,0},{1,.18},{.59,.18},{.59,1},{.41,1},{.41,.18},{0,.18},{0,0}};}
    private static double[][] L(){return new double[][]{{0,0},{.18,0},{.18,.82},{1,.82},{1,1},{0,1},{0,0}};}
}
