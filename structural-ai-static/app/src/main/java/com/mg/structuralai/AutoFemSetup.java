package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;

/** Deterministic engineering setup for mobile static FEM. No AI-generated physics. */
public final class AutoFemSetup {
    public static final class SetupResult {
        public final int fixedNodes, loadedNodes;
        public final double totalLoadN;
        public final String supportDescription, loadDescription;
        SetupResult(int f,int l,double n,String s,String ld){ fixedNodes=f; loadedNodes=l; totalLoadN=n; supportDescription=s; loadDescription=ld; }
    }

    private AutoFemSetup(){}

    /**
     * Cantilever-style automatic setup: minimum-X node plane is fixed and total force is
     * distributed uniformly to the maximum-X node plane. Intended as an explicit default,
     * not as an inferred real-world fixture.
     */
    public static SetupResult applyCantileverZ(StaticFemSolver solver,TetMeshData mesh,double totalForceN){
        if(mesh==null || mesh.nodes.isEmpty()) throw new IllegalArgumentException("Mesh required");
        double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY;
        for(MeshModel.V3 p:mesh.nodes){ minX=Math.min(minX,p.x); maxX=Math.max(maxX,p.x); }
        double span=Math.max(maxX-minX,1e-12);
        double tol=Math.max(span*1e-7,1e-12);
        List<Integer> fixed=new ArrayList<>(), loaded=new ArrayList<>();
        for(int i=0;i<mesh.nodes.size();i++){
            double x=mesh.nodes.get(i).x;
            if(Math.abs(x-minX)<=tol) fixed.add(i);
            if(Math.abs(x-maxX)<=tol) loaded.add(i);
        }
        if(fixed.size()<3) throw new IllegalStateException("Automatic support plane contains fewer than 3 nodes");
        if(loaded.isEmpty()) throw new IllegalStateException("Automatic load plane contains no nodes");
        for(int n:fixed) solver.fixNode(n);
        double each=totalForceN/loaded.size();
        for(int n:loaded) solver.addNodalForce(n,0,0,each);
        return new SetupResult(fixed.size(),loaded.size(),totalForceN,
            "min-X plane fully fixed (Ux=Uy=Uz=0)",
            "max-X plane uniform nodal load, global Z");
    }

    public static double unitScaleToMetres(String unit){
        if(unit==null) return 0.001;
        String u=unit.trim().toLowerCase();
        if(u.equals("m")) return 1.0;
        if(u.equals("cm")) return 0.01;
        if(u.equals("mm")) return 0.001;
        if(u.equals("in")||u.equals("inch")) return 0.0254;
        throw new IllegalArgumentException("Unsupported model unit: "+unit);
    }
}
