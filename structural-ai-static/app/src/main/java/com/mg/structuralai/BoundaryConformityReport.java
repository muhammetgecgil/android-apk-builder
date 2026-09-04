package com.mg.structuralai;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

/** Measures how closely boundary faces of a volume mesh follow the source triangle surface. */
public final class BoundaryConformityReport {
    public final int boundaryFaces;
    public final double meanDistanceM,maxDistanceM,rmsDistanceM;
    public final double meanRelativeToCell,maxRelativeToCell;
    public final boolean productionReady;
    private BoundaryConformityReport(int n,double mean,double max,double rms,double mr,double xr,boolean ok){boundaryFaces=n;meanDistanceM=mean;maxDistanceM=max;rmsDistanceM=rms;meanRelativeToCell=mr;maxRelativeToCell=xr;productionReady=ok;}

    public static BoundaryConformityReport evaluate(TetMeshData mesh,MeshModel surface,double scale,double cellSizeModelUnits){
        Map<Face,Integer> count=new HashMap<>();
        for(int[] t:mesh.tets){add(count,t[0],t[1],t[2]);add(count,t[0],t[1],t[3]);add(count,t[0],t[2],t[3]);add(count,t[1],t[2],t[3]);}
        int n=0;double sum=0,sum2=0,max=0;
        for(Map.Entry<Face,Integer> e:count.entrySet())if(e.getValue()==1){
            Face f=e.getKey();MeshModel.V3 a=mesh.nodes.get(f.a),b=mesh.nodes.get(f.b),c=mesh.nodes.get(f.c);
            MeshModel.V3 q=new MeshModel.V3((a.x+b.x+c.x)/3.0,(a.y+b.y+c.y)/3.0,(a.z+b.z+c.z)/3.0);
            double d=nearestSurfaceDistance(q,surface,scale);n++;sum+=d;sum2+=d*d;max=Math.max(max,d);
        }
        if(n==0)return new BoundaryConformityReport(0,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,false);
        double mean=sum/n,rms=Math.sqrt(sum2/n),h=cellSizeModelUnits*scale;
        double mr=mean/Math.max(h,1e-30),xr=max/Math.max(h,1e-30);
        // Production target, intentionally strict. Current voxel mesher is expected to be advisory until replaced/snapped.
        boolean ok=mr<=0.10&&xr<=0.35;
        return new BoundaryConformityReport(n,mean,max,rms,mr,xr,ok);
    }

    public String summary(){return String.format(Locale.US,"boundaryFaces=%d | meanDist=%.6g mm | maxDist=%.6g mm | mean/h=%.3f | max/h=%.3f | conformalGate=%s",boundaryFaces,meanDistanceM*1000,maxDistanceM*1000,meanRelativeToCell,maxRelativeToCell,productionReady?"PASS":"ADVISORY/FAIL");}

    private static void add(Map<Face,Integer> m,int a,int b,int c){Face f=new Face(a,b,c);Integer n=m.get(f);m.put(f,n==null?1:n+1);}
    private static final class Face{final int a,b,c;Face(int x,int y,int z){int p=x,q=y,r=z;if(p>q){int t=p;p=q;q=t;}if(q>r){int t=q;q=r;r=t;}if(p>q){int t=p;p=q;q=t;}a=p;b=q;c=r;}@Override public int hashCode(){return (a*73856093)^(b*19349663)^(c*83492791);}@Override public boolean equals(Object o){if(!(o instanceof Face))return false;Face f=(Face)o;return a==f.a&&b==f.b&&c==f.c;}}

    private static double nearestSurfaceDistance(MeshModel.V3 p,MeshModel s,double scale){double best=Double.POSITIVE_INFINITY;for(int[] t:s.triangles){if(t.length<3)continue;MeshModel.V3 A=sc(s.vertices.get(t[0]),scale),B=sc(s.vertices.get(t[1]),scale),C=sc(s.vertices.get(t[2]),scale);best=Math.min(best,pointTriangleDistance(p,A,B,C));}return best;}
    private static MeshModel.V3 sc(MeshModel.V3 p,double s){return new MeshModel.V3(p.x*s,p.y*s,p.z*s);}

    // Closest-point distance from Real-Time Collision Detection, Christer Ericson.
    private static double pointTriangleDistance(MeshModel.V3 p,MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c){
        double[] ab=v(b,a),ac=v(c,a),ap=v(p,a);double d1=dot(ab,ap),d2=dot(ac,ap);if(d1<=0&&d2<=0)return len(ap);
        double[] bp=v(p,b);double d3=dot(ab,bp),d4=dot(ac,bp);if(d3>=0&&d4<=d3)return len(bp);
        double vc=d1*d4-d3*d2;if(vc<=0&&d1>=0&&d3<=0){double u=d1/(d1-d3);return len(v(p,new MeshModel.V3(a.x+u*ab[0],a.y+u*ab[1],a.z+u*ab[2])));}
        double[] cp=v(p,c);double d5=dot(ab,cp),d6=dot(ac,cp);if(d6>=0&&d5<=d6)return len(cp);
        double vb=d5*d2-d1*d6;if(vb<=0&&d2>=0&&d6<=0){double w=d2/(d2-d6);return len(v(p,new MeshModel.V3(a.x+w*ac[0],a.y+w*ac[1],a.z+w*ac[2])));}
        double va=d3*d6-d5*d4;if(va<=0&&(d4-d3)>=0&&(d5-d6)>=0){double w=(d4-d3)/((d4-d3)+(d5-d6));return len(v(p,new MeshModel.V3(b.x+w*(c.x-b.x),b.y+w*(c.y-b.y),b.z+w*(c.z-b.z))));}
        double denom=1.0/(va+vb+vc),u=vb*denom,w=vc*denom;MeshModel.V3 q=new MeshModel.V3(a.x+ab[0]*u+ac[0]*w,a.y+ab[1]*u+ac[1]*w,a.z+ab[2]*u+ac[2]*w);return len(v(p,q));
    }
    private static double[] v(MeshModel.V3 a,MeshModel.V3 b){return new double[]{a.x-b.x,a.y-b.y,a.z-b.z};}
    private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    private static double len(double[] a){return Math.sqrt(dot(a,a));}
}
