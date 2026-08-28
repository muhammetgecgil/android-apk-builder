package com.mg.structuralai;

import java.util.Locale;

/** Analytical regression for tensor post-processing and material-evidence-gated FoS. */
public final class ProfessionalResultsRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private ProfessionalResultsRegressionGate(){}

    public static Result run(){
        try{
            double E=200e9,nu=0.30,ex=1.0e-3,ey=-nu*ex,ez=-nu*ex;
            LinearElasticMaterial ref=new LinearElasticMaterial("uniaxial reference",E,nu,7800,400e6,500e6,"reference only",LinearElasticMaterial.EvidenceLevel.REFERENCE);
            MeshModel.V3 a=new MeshModel.V3(0,0,0),b=new MeshModel.V3(1,0,0),c=new MeshModel.V3(0,1,0),d=new MeshModel.V3(0,0,1);
            double[] u={0,0,0, ex,0,0, 0,ey,0, 0,0,ez};
            double[] strain=Tet4Element.strain(a,b,c,d,u),stress=Tet4Element.stress(a,b,c,d,ref,u);
            double[] pe=Tet4Element.principalValues(strain,true),ps=Tet4Element.principalValues(stress,false);double vm=Tet4Element.vonMises(stress);
            boolean strainOk=rel(strain[0],ex)<1e-10&&rel(strain[1],ey)<1e-10&&rel(strain[2],ez)<1e-10&&pe[0]>=pe[1]&&pe[1]>=pe[2];
            boolean stressOk=rel(ps[0],E*ex)<1e-8&&Math.abs(ps[1])<1e-3&&Math.abs(ps[2])<1e-3&&rel(vm,E*ex)<1e-8&&ps[0]>=ps[1]&&ps[1]>=ps[2];
            LinearElasticMaterial certified=new LinearElasticMaterial("certified",E,nu,7800,400e6,500e6,"regression certificate",LinearElasticMaterial.EvidenceLevel.USER_CERTIFIED);
            boolean evidenceOk=!ref.designReleaseEligible()&&certified.designReleaseEligible();double fos=certified.yieldPa/vm;boolean fosOk=Math.abs(fos-2.0)<1e-6;
            boolean pass=strainOk&&stressOk&&evidenceOk&&fosOk;
            return new Result(pass,String.format(Locale.US,"PRO RESULTS REGRESSION %s | e1=%.6g e2=%.6g e3=%.6g | s1=%.6g MPa s2=%.3g s3=%.3g | VM=%.6g MPa | evidence=%s | FoS=%.6g",pass?"PASS":"FAIL",pe[0],pe[1],pe[2],ps[0]/1e6,ps[1]/1e6,ps[2]/1e6,vm/1e6,evidenceOk,fos));
        }catch(Exception e){return new Result(false,"PRO RESULTS REGRESSION EXCEPTION: "+e.getMessage());}
    }
    private static double rel(double a,double b){return Math.abs(a-b)/Math.max(Math.max(Math.abs(a),Math.abs(b)),1e-30);}
}
