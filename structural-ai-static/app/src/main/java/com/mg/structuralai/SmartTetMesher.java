package com.mg.structuralai;

/** Selects the safer mesh automatically. Boundary-snapped TET is preferred only when it improves
 * conformity without violating quality. Otherwise the original conforming voxel topology is kept. */
public final class SmartTetMesher {
    public static final class Result {
        public final TetMeshData mesh; public final MeshQualityReport quality; public final BoundaryConformityReport conformity;
        public final boolean snapped; public final String decision;
        Result(TetMeshData m,MeshQualityReport q,BoundaryConformityReport c,boolean s,String d){mesh=m;quality=q;conformity=c;snapped=s;decision=d;}
    }
    private SmartTetMesher(){}

    public static Result generate(MeshModel surface,int cells,double scale){
        VoxelTetMesher.Result base=VoxelTetMesher.generate(surface,cells,scale);
        BoundaryConformityReport baseC=BoundaryConformityReport.evaluate(base.mesh,surface,scale,base.cellSizeModelUnits);
        try{
            BoundarySnapTetMesher.Result s=BoundarySnapTetMesher.generate(surface,cells,scale);
            boolean improved=s.quality.pass && s.after.meanDistanceM<=baseC.meanDistanceM*0.98 && s.after.maxDistanceM<=baseC.maxDistanceM*1.05;
            if(improved)return new Result(s.mesh,s.quality,s.after,true,"BOUNDARY_SNAP accepted: conformity improved while TET quality gate remained PASS.");
        }catch(RuntimeException ignored){ }
        return new Result(base.mesh,base.quality,baseC,false,"VOXEL fallback retained: snap did not safely improve conformity.");
    }
}
