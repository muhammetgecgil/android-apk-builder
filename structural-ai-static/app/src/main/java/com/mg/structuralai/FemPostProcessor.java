package com.mg.structuralai;

/** Deterministic TET4 post-processing from the solved displacement field. */
public final class FemPostProcessor {
    public final double[][] stress6Pa;
    public final double[][] strain6;
    public final double[][] principalStressPa;
    public final double[][] principalStrain;
    public final double[] safetyFactorYield;
    public final boolean safetyFactorAuthoritative;

    private FemPostProcessor(int n,boolean sfAuth){
        stress6Pa=new double[n][6];strain6=new double[n][6];principalStressPa=new double[n][3];principalStrain=new double[n][3];safetyFactorYield=new double[n];safetyFactorAuthoritative=sfAuth;
    }

    public static FemPostProcessor evaluate(TetMeshData mesh,StaticFemSolver.Result r,LinearElasticMaterial mat){
        if(mesh==null||r==null)throw new IllegalArgumentException("Mesh/result required");
        if(mat==null)throw new IllegalStateException("Material provenance unavailable for stress post-processing");
        FemPostProcessor p=new FemPostProcessor(mesh.tets.size(),mat.designReleaseEligible());
        for(int e=0;e<mesh.tets.size();e++){
            int[] t=mesh.tets.get(e);double[] ue=new double[12];
            for(int a=0;a<4;a++)for(int c=0;c<3;c++)ue[3*a+c]=r.displacement[3*t[a]+c];
            double[] eps=Tet4Element.strain(mesh.nodes.get(t[0]),mesh.nodes.get(t[1]),mesh.nodes.get(t[2]),mesh.nodes.get(t[3]),ue);
            double[] sig=Tet4Element.stress(mesh.nodes.get(t[0]),mesh.nodes.get(t[1]),mesh.nodes.get(t[2]),mesh.nodes.get(t[3]),mat,ue);
            System.arraycopy(eps,0,p.strain6[e],0,6);System.arraycopy(sig,0,p.stress6Pa[e],0,6);
            double[] ps=Tet4Element.principalValues(sig,false),pe=Tet4Element.principalValues(eps,true);
            System.arraycopy(ps,0,p.principalStressPa[e],0,3);System.arraycopy(pe,0,p.principalStrain[e],0,3);
            double vm=Tet4Element.vonMises(sig);
            p.safetyFactorYield[e]=Double.isFinite(mat.yieldPa)&&mat.yieldPa>0&&vm>1e-20?mat.yieldPa/vm:Double.NaN;
        }
        return p;
    }
}
