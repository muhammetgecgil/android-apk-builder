package com.mg.structuralai;

import java.util.Locale;

/**
 * Fast, visualization-only volumetric mesh preview. This mesh is NEVER used as
 * the authoritative solve mesh. The autonomous solver still runs its own
 * convergence/adaptive mesh chain afterwards.
 */
public final class PreSolveMeshPreviewEngine {
    public static final class Result {
        public final TetMeshData mesh;
        public final boolean available;
        public final String summary;
        Result(TetMeshData m, boolean a, String s){mesh=m;available=a;summary=s;}
    }

    private PreSolveMeshPreviewEngine(){}

    public static Result build(MeshModel model){
        if(model==null||model.vertices==null||model.vertices.isEmpty())
            return new Result(null,false,"PRE-SOLVE PREVIEW BLOCKED: model unavailable");
        try{
            SurfaceTopologyReport topo=SurfaceTopologyReport.evaluate(model);
            if(!topo.closedManifold)
                return new Result(null,false,"PRE-SOLVE PREVIEW BLOCKED: "+topo.summary());
            AutonomousAnalysisPlanner.Plan p=AutonomousAnalysisPlanner.infer(model);
            // Deliberately modest density: interactive preview only. The real solve
            // uses convergence/adaptive meshes and may be much finer.
            VoxelTetMesher.Result r=VoxelTetMesher.generate(model,8,p.unitScaleM);
            if(r==null||r.mesh==null||r.mesh.tets==null||r.mesh.tets.isEmpty())
                return new Result(null,false,"PRE-SOLVE PREVIEW BLOCKED: volumetric mesh unavailable");
            String q=r.quality==null?"QA unavailable":r.quality.summary();
            return new Result(r.mesh,true,String.format(Locale.US,
                    "PRE-SOLVE PREVIEW ONLY | nodes=%d | TET4=%d | %s | NOT USED FOR FINAL SOLVE",
                    r.mesh.nodes.size(),r.mesh.tets.size(),q));
        }catch(Throwable t){
            return new Result(null,false,"PRE-SOLVE PREVIEW BLOCKED: "+t.getMessage());
        }
    }
}
