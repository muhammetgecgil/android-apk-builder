package com.mg.structuralai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Conservative tessellation feature detector for autonomous support/load reasoning. */
public final class GeometryFeatureDetector {
    public static final class FeatureSet {
        public final List<MeshModel.V3> planarMountCandidates=new ArrayList<>();
        public final List<MeshModel.V3> circularHoleCandidates=new ArrayList<>();
        public final List<MeshModel.V3> flangeCandidates=new ArrayList<>();
        public double planarConfidence,holeConfidence,flangeConfidence;
        public String summary(){return "planes="+planarMountCandidates.size()+", holes="+circularHoleCandidates.size()+", flanges="+flangeCandidates.size();}
    }
    private static final class PlaneAcc {double sx,sy,sz,area;int count;}
    private GeometryFeatureDetector(){}

    public static FeatureSet detect(MeshModel m){
        FeatureSet f=new FeatureSet();
        if(m==null||m.triangles.isEmpty())return f;
        double[] bb=bounds(m); double dx=bb[3]-bb[0],dy=bb[4]-bb[1],dz=bb[5]-bb[2]; double diag=Math.sqrt(dx*dx+dy*dy+dz*dz);
        double tol=Math.max(diag*1e-6,1e-12), totalArea=0;
        Map<String,PlaneAcc> planes=new LinkedHashMap<>();
        for(int[] t:m.triangles){
            if(t.length<3)continue;
            MeshModel.V3 a=m.vertices.get(t[0]),b=m.vertices.get(t[1]),c=m.vertices.get(t[2]);
            double ar=area(a,b,c); if(ar<=1e-20)continue; totalArea+=ar;
            double[] n=normal(a,b,c); int axis=dominantAxis(n);
            if(Math.abs(n[axis])<0.995)continue;
            double coord=(coord(a,axis)+coord(b,axis)+coord(c,axis))/3.0;
            long q=Math.round(coord/tol);
            String key=axis+":"+q;
            PlaneAcc p=planes.get(key);if(p==null){p=new PlaneAcc();planes.put(key,p);}
            MeshModel.V3 cent=new MeshModel.V3((a.x+b.x+c.x)/3.0,(a.y+b.y+c.y)/3.0,(a.z+b.z+c.z)/3.0);
            p.sx+=cent.x*ar;p.sy+=cent.y*ar;p.sz+=cent.z*ar;p.area+=ar;p.count++;
        }
        for(PlaneAcc p:planes.values()){
            if(p.area<=diag*diag*1e-8)continue;
            MeshModel.V3 c=new MeshModel.V3(p.sx/p.area,p.sy/p.area,p.sz/p.area);
            f.planarMountCandidates.add(c);
            double frac=p.area/Math.max(totalArea,1e-30);
            // Conservative flange evidence: a distinct planar region of intermediate surface-area share.
            if(frac>=0.08 && frac<=0.25 && planes.size()>6)f.flangeCandidates.add(c);
        }
        // Tessellated STL/OBJ vertices alone are not reliable evidence of a through-hole.
        // Hole detection stays intentionally empty until a closed circular boundary/cylindrical patch is proven.
        f.planarConfidence=Math.min(0.90,0.35+0.06*f.planarMountCandidates.size());
        f.flangeConfidence=f.flangeCandidates.isEmpty()?0.0:Math.min(0.75,0.30+0.05*f.flangeCandidates.size());
        f.holeConfidence=0.0;
        return f;
    }

    private static int dominantAxis(double[] n){int a=0;if(Math.abs(n[1])>Math.abs(n[a]))a=1;if(Math.abs(n[2])>Math.abs(n[a]))a=2;return a;}
    private static double coord(MeshModel.V3 p,int a){return a==0?p.x:a==1?p.y:p.z;}
    private static double[] bounds(MeshModel m){double x0=1e99,y0=1e99,z0=1e99,x1=-1e99,y1=-1e99,z1=-1e99;for(MeshModel.V3 p:m.vertices){x0=Math.min(x0,p.x);y0=Math.min(y0,p.y);z0=Math.min(z0,p.z);x1=Math.max(x1,p.x);y1=Math.max(y1,p.y);z1=Math.max(z1,p.z);}return new double[]{x0,y0,z0,x1,y1,z1};}
    private static double area(MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c){double[] n=cross(a,b,c);return 0.5*Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);}
    private static double[] normal(MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c){double[] n=cross(a,b,c);double d=Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);if(d<1e-20)return new double[]{0,0,0};return new double[]{n[0]/d,n[1]/d,n[2]/d};}
    private static double[] cross(MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c){double ux=b.x-a.x,uy=b.y-a.y,uz=b.z-a.z,vx=c.x-a.x,vy=c.y-a.y,vz=c.z-a.z;return new double[]{uy*vz-uz*vy,uz*vx-ux*vz,ux*vy-uy*vx};}
}
