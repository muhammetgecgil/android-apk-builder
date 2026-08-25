package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;

/**
 * Autonomous first-pass planner. It never invents a real service load.
 * Geometry is used to choose a stable unit-load capacity study and candidate support/load regions.
 */
public final class AutonomousAnalysisPlanner {
    public static final class Plan {
        public final List<MeshModel.V3> supports=new ArrayList<>();
        public final List<MeshModel.V3> loads=new ArrayList<>();
        public double fx,fy,fz;
        public double unitScaleM;
        public LinearElasticMaterial material;
        public String geometryClass, supportReason, loadReason, materialReason, unitReason;
        public double supportConfidence, loadConfidence, materialConfidence, unitConfidence;
        public boolean parametricLoad=true;
    }
    private AutonomousAnalysisPlanner(){}

    public static Plan infer(MeshModel m){
        if(m==null||m.vertices.isEmpty()) throw new IllegalArgumentException("Model required");
        double minX=1e300,minY=1e300,minZ=1e300,maxX=-1e300,maxY=-1e300,maxZ=-1e300;
        for(MeshModel.V3 p:m.vertices){minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);minZ=Math.min(minZ,p.z);maxX=Math.max(maxX,p.x);maxY=Math.max(maxY,p.y);maxZ=Math.max(maxZ,p.z);}
        double[] d={maxX-minX,maxY-minY,maxZ-minZ}; int major=0;if(d[1]>d[major])major=1;if(d[2]>d[major])major=2;
        double longest=d[major], mid=Math.max(Math.min(d[0],d[1]),Math.min(Math.max(d[0],d[1]),d[2]));
        Plan p=new Plan();
        p.geometryClass=longest>3.0*Math.max(mid,1e-12)?"SLENDER / BEAM-LIKE":"GENERAL 3D SOLID";
        // STL/OBJ have no reliable units. Choose a scale only for the parametric unit-load study and expose low confidence.
        double maxDim=Math.max(d[0],Math.max(d[1],d[2]));
        p.unitScaleM=maxDim>2.0?0.001:1.0;
        p.unitReason="STL/OBJ unit metadata absent; scale inferred from dimensional magnitude for unit-load study";p.unitConfidence=0.35;
        // Material cannot be recovered from bare tessellation. Use normalized steel-like E only to obtain a displacement-per-load coefficient.
        p.material=new LinearElasticMaterial("Normalized isotropic reference",210e9,0.30,7850,355e6);
        p.materialReason="Bare geometry contains no material evidence; reference isotropic material used, not claimed as actual material";p.materialConfidence=0.0;
        double lo=major==0?minX:major==1?minY:minZ, hi=major==0?maxX:major==1?maxY:maxZ;
        double tol=Math.max((hi-lo)*0.02,1e-9);
        for(MeshModel.V3 v:m.vertices){double q=major==0?v.x:major==1?v.y:v.z;if(Math.abs(q-lo)<=tol)p.supports.add(v);if(Math.abs(q-hi)<=tol)p.loads.add(v);}
        p.supportReason="Candidate mounting region = extreme end of dominant geometric axis";p.supportConfidence=p.geometryClass.startsWith("SLENDER")?0.70:0.40;
        p.loadReason="Opposite extreme end used for 1 N influence/capacity study; not asserted as real service load";p.loadConfidence=p.geometryClass.startsWith("SLENDER")?0.65:0.35;
        // Apply 1 N transverse to dominant axis, choosing the smaller transverse dimension direction.
        int transverse;if(major==0)transverse=d[1]<=d[2]?1:2;else if(major==1)transverse=d[0]<=d[2]?0:2;else transverse=d[0]<=d[1]?0:1;
        if(transverse==0)p.fx=-1.0;else if(transverse==1)p.fy=-1.0;else p.fz=-1.0;
        if(p.supports.isEmpty()||p.loads.isEmpty())throw new IllegalStateException("Autonomous regions could not be inferred");
        return p;
    }
}
