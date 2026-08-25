package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;

/** Lightweight tessellation feature detector for autonomous support/load reasoning. */
public final class GeometryFeatureDetector {
    public static final class FeatureSet {
        public final List<MeshModel.V3> planarMountCandidates=new ArrayList<>();
        public final List<MeshModel.V3> circularHoleCandidates=new ArrayList<>();
        public final List<MeshModel.V3> flangeCandidates=new ArrayList<>();
        public double planarConfidence,holeConfidence,flangeConfidence;
        public String summary(){return "planes="+planarMountCandidates.size()+", holes="+circularHoleCandidates.size()+", flanges="+flangeCandidates.size();}
    }
    private GeometryFeatureDetector(){}

    public static FeatureSet detect(MeshModel m){
        FeatureSet f=new FeatureSet();
        if(m==null||m.triangles.isEmpty())return f;
        double[] bb=bounds(m); double dx=bb[3]-bb[0],dy=bb[4]-bb[1],dz=bb[5]-bb[2]; double diag=Math.sqrt(dx*dx+dy*dy+dz*dz);
        for(int[] t:m.triangles){if(t.length<3)continue;MeshModel.V3 a=m.vertices.get(t[0]),b=m.vertices.get(t[1]),c=m.vertices.get(t[2]);
            MeshModel.V3 cent=new MeshModel.V3((a.x+b.x+c.x)/3.0,(a.y+b.y+c.y)/3.0,(a.z+b.z+c.z)/3.0);
            double[] n=normal(a,b,c); double area=area(a,b,c);
            double edgeFrac=Math.min(Math.min(Math.abs(cent.x-bb[0]),Math.abs(cent.x-bb[3])),Math.min(Math.min(Math.abs(cent.y-bb[1]),Math.abs(cent.y-bb[4])),Math.min(Math.abs(cent.z-bb[2]),Math.abs(cent.z-bb[5]))))/Math.max(diag,1e-12);
            if(area>diag*diag*0.001 && (Math.abs(n[0])>0.95||Math.abs(n[1])>0.95||Math.abs(n[2])>0.95) && edgeFrac<0.08)f.planarMountCandidates.add(cent);
            // coarse flange proxy: broad planar triangle near model extremity
            if(area>diag*diag*0.003 && edgeFrac<0.12)f.flangeCandidates.add(cent);
        }
        // crude hole proxy from vertex rings: local points with similar radius around axis-aligned centroid bands
        MeshModel.V3 ctr=new MeshModel.V3((bb[0]+bb[3])/2,(bb[1]+bb[4])/2,(bb[2]+bb[5])/2);
        for(MeshModel.V3 v:m.vertices){double rx=Math.hypot(v.y-ctr.y,v.z-ctr.z),ry=Math.hypot(v.x-ctr.x,v.z-ctr.z),rz=Math.hypot(v.x-ctr.x,v.y-ctr.y);double r=Math.min(rx,Math.min(ry,rz));if(r>diag*0.02&&r<diag*0.20)f.circularHoleCandidates.add(v);}
        f.planarConfidence=Math.min(0.85,0.25+0.03*f.planarMountCandidates.size());
        f.flangeConfidence=Math.min(0.80,0.20+0.04*f.flangeCandidates.size());
        f.holeConfidence=Math.min(0.55,0.10+0.002*f.circularHoleCandidates.size());
        return f;
    }

    private static double[] bounds(MeshModel m){double x0=1e99,y0=1e99,z0=1e99,x1=-1e99,y1=-1e99,z1=-1e99;for(MeshModel.V3 p:m.vertices){x0=Math.min(x0,p.x);y0=Math.min(y0,p.y);z0=Math.min(z0,p.z);x1=Math.max(x1,p.x);y1=Math.max(y1,p.y);z1=Math.max(z1,p.z);}return new double[]{x0,y0,z0,x1,y1,z1};}
    private static double area(MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c){double[] n=cross(a,b,c);return 0.5*Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);}
    private static double[] normal(MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c){double[] n=cross(a,b,c);double d=Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);if(d<1e-20)return new double[]{0,0,0};return new double[]{n[0]/d,n[1]/d,n[2]/d};}
    private static double[] cross(MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c){double ux=b.x-a.x,uy=b.y-a.y,uz=b.z-a.z,vx=c.x-a.x,vy=c.y-a.y,vz=c.z-a.z;return new double[]{uy*vz-uz*vy,uz*vx-ux*vz,ux*vy-uy*vx};}
}
