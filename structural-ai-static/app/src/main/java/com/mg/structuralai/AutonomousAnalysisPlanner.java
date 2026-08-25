package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;

/** Autonomous first-pass planner. Never invents a real service load or material. */
public final class AutonomousAnalysisPlanner {
    public static final class Plan {
        public final List<MeshModel.V3> supports=new ArrayList<>();
        public final List<MeshModel.V3> loads=new ArrayList<>();
        public double fx,fy,fz,unitScaleM;
        public LinearElasticMaterial material;
        public String geometryClass,supportReason,loadReason,materialReason,unitReason,featureSummary;
        public double supportConfidence,loadConfidence,materialConfidence,unitConfidence;
        public boolean parametricLoad=true;
    }
    private AutonomousAnalysisPlanner(){}

    public static Plan infer(MeshModel m){
        if(m==null||m.vertices.isEmpty())throw new IllegalArgumentException("Model required");
        double minX=1e300,minY=1e300,minZ=1e300,maxX=-1e300,maxY=-1e300,maxZ=-1e300;
        for(MeshModel.V3 q:m.vertices){minX=Math.min(minX,q.x);minY=Math.min(minY,q.y);minZ=Math.min(minZ,q.z);maxX=Math.max(maxX,q.x);maxY=Math.max(maxY,q.y);maxZ=Math.max(maxZ,q.z);}
        double[] d={maxX-minX,maxY-minY,maxZ-minZ};int major=0;if(d[1]>d[major])major=1;if(d[2]>d[major])major=2;
        double longest=d[major],mid=Math.max(Math.min(d[0],d[1]),Math.min(Math.max(d[0],d[1]),d[2]));
        Plan p=new Plan();p.geometryClass=longest>3*Math.max(mid,1e-12)?"SLENDER / BEAM-LIKE":"GENERAL 3D SOLID";
        double maxDim=Math.max(d[0],Math.max(d[1],d[2]));p.unitScaleM=maxDim>2?0.001:1.0;p.unitReason="No reliable STL/OBJ unit metadata; dimensional-magnitude inference used only for parametric study";p.unitConfidence=0.35;
        p.material=new LinearElasticMaterial("Normalized isotropic reference",210e9,0.30,7850,355e6);p.materialReason="No material evidence in bare tessellation; reference material is normalization only";p.materialConfidence=0.0;

        GeometryFeatureDetector.FeatureSet fs=GeometryFeatureDetector.detect(m);p.featureSummary=fs.summary();
        if(!fs.flangeCandidates.isEmpty()&&fs.flangeConfidence>=0.45){
            p.supports.addAll(fs.flangeCandidates);p.supportReason="Broad flange/mount-like planar geometry detected";p.supportConfidence=fs.flangeConfidence;
        }else if(!fs.planarMountCandidates.isEmpty()&&fs.planarConfidence>=0.45){
            p.supports.addAll(fs.planarMountCandidates);p.supportReason="Planar mounting-face candidates detected near model boundary";p.supportConfidence=fs.planarConfidence;
        }

        double lo=major==0?minX:major==1?minY:minZ,hi=major==0?maxX:major==1?maxY:maxZ,tol=Math.max((hi-lo)*0.02,1e-9);
        List<MeshModel.V3> lowEnd=new ArrayList<>(),highEnd=new ArrayList<>();
        for(MeshModel.V3 v:m.vertices){double q=major==0?v.x:major==1?v.y:v.z;if(Math.abs(q-lo)<=tol)lowEnd.add(v);if(Math.abs(q-hi)<=tol)highEnd.add(v);}
        if(p.supports.isEmpty()){p.supports.addAll(lowEnd);p.supportReason="Fallback candidate mounting region = extreme end of dominant geometric axis";p.supportConfidence=p.geometryClass.startsWith("SLENDER")?0.70:0.40;}
        // load region chosen away from support centroid, defaulting to opposite major-axis end
        MeshModel.V3 sc=centroid(p.supports);double dl=dist(sc,centroid(lowEnd)),dh=dist(sc,centroid(highEnd));p.loads.addAll(dh>=dl?highEnd:lowEnd);
        p.loadReason="Region farthest from inferred support used for 1 N influence/capacity study; not asserted as service load";p.loadConfidence=p.geometryClass.startsWith("SLENDER")?0.68:0.42;
        if(!fs.circularHoleCandidates.isEmpty())p.supportReason+="; circular/hole-like evidence present (support confidence modifier only)";

        int transverse;if(major==0)transverse=d[1]<=d[2]?1:2;else if(major==1)transverse=d[0]<=d[2]?0:2;else transverse=d[0]<=d[1]?0:1;
        if(transverse==0)p.fx=-1;else if(transverse==1)p.fy=-1;else p.fz=-1;
        if(p.supports.isEmpty()||p.loads.isEmpty())throw new IllegalStateException("Autonomous regions could not be inferred");return p;
    }
    private static MeshModel.V3 centroid(List<MeshModel.V3> a){if(a==null||a.isEmpty())return new MeshModel.V3(0,0,0);double x=0,y=0,z=0;for(MeshModel.V3 v:a){x+=v.x;y+=v.y;z+=v.z;}double n=a.size();return new MeshModel.V3(x/n,y/n,z/n);}
    private static double dist(MeshModel.V3 a,MeshModel.V3 b){double x=a.x-b.x,y=a.y-b.y,z=a.z-b.z;return Math.sqrt(x*x+y*y+z*z);}
}
