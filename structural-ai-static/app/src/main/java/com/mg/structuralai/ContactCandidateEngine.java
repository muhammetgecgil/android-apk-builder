package com.mg.structuralai;

import java.util.*;

/** Geometry-only first-pass contact candidate detector for disconnected tessellated bodies. */
public final class ContactCandidateEngine {
    public enum State { TOUCHING_OR_COINCIDENT, NEAR_CONTACT, FINITE_GAP, INTERFERENCE_SUSPECTED, FAR }
    public static final class Pair {
        public final int bodyA,bodyB;
        public final State state;
        public final double bboxGap, bboxPenetration, sampledGap, normalOpposition, confidence;
        public final int sampledPairs;
        Pair(int a,int b,State s,double bg,double bp,double sg,double no,double c,int n){bodyA=a;bodyB=b;state=s;bboxGap=bg;bboxPenetration=bp;sampledGap=sg;normalOpposition=no;confidence=c;sampledPairs=n;}
        public String summary(){return "body "+bodyA+" ↔ "+bodyB+" | "+state+" | bboxGap="+bboxGap+" | bboxPenetration="+bboxPenetration+" | sampledGap="+sampledGap+" | opposedNormals="+normalOpposition+" | confidence="+confidence;}
    }
    public static final class Result {
        public final List<Pair> pairs;
        public final int bodies;
        Result(List<Pair> p,int b){pairs=p;bodies=b;}
        public int activeCandidates(){int n=0;for(Pair p:pairs)if(p.state!=State.FAR)n++;return n;}
    }
    private ContactCandidateEngine(){}

    public static Result analyze(MeshModel m,AssemblyBodyDecomposer.Result dec){
        if(dec==null)dec=AssemblyBodyDecomposer.decompose(m);
        List<Pair> out=new ArrayList<>();
        double modelDiag=Math.max(m.diagonal(),1e-9);
        double touchTol=Math.max(modelDiag*2e-4,dec.quantization*8.0);
        double nearTol=Math.max(modelDiag*0.01,touchTol*8.0);
        for(int i=0;i<dec.bodies.size();i++)for(int j=i+1;j<dec.bodies.size();j++){
            AssemblyBodyDecomposer.Body a=dec.bodies.get(i),b=dec.bodies.get(j);
            double bg=bboxGap(a,b),bp=bboxPenetration(a,b);
            if(bg>nearTol*3.0){out.add(new Pair(a.id,b.id,State.FAR,bg,0,bg,0,0.98,0));continue;}
            SampleResult sr=sample(m,a,b,96);
            State st;
            double conf;
            // Positive overlap on all three AABB axes is not proof of exact solid penetration for arbitrary
            // concave parts, but it is sufficient evidence to prevent an automatic release-qualified contact solve.
            // Require penetration above the coincidence tolerance and a sampled surface separation above that
            // tolerance so a merely touching common face is not mislabeled as interference.
            if(bp>touchTol&&sr.minGap>touchTol){st=State.INTERFERENCE_SUSPECTED;conf=0.80;}
            else if(sr.minGap<=touchTol&&sr.opposition>=0.55){st=State.TOUCHING_OR_COINCIDENT;conf=clamp(0.70+0.25*sr.opposition);}
            else if(bg<=touchTol&&sr.minGap>nearTol){st=State.INTERFERENCE_SUSPECTED;conf=0.60;}
            else if(sr.minGap<=nearTol){st=State.NEAR_CONTACT;conf=clamp(0.55+0.30*sr.opposition);}
            else if(bg<=nearTol*3.0){st=State.FINITE_GAP;conf=0.75;}
            else {st=State.FAR;conf=0.95;}
            out.add(new Pair(a.id,b.id,st,bg,bp,sr.minGap,sr.opposition,conf,sr.count));
        }
        Collections.sort(out,(x,y)->Double.compare(x.sampledGap,y.sampledGap));
        return new Result(out,dec.bodies.size());
    }

    private static final class SampleResult{double minGap=Double.POSITIVE_INFINITY,opposition=0;int count=0;}
    private static SampleResult sample(MeshModel m,AssemblyBodyDecomposer.Body a,AssemblyBodyDecomposer.Body b,int maxPerBody){
        SampleResult r=new SampleResult();
        List<Integer> aa=sampleIndices(a.triangleIndices,maxPerBody),bb=sampleIndices(b.triangleIndices,maxPerBody);
        double bestOpp=0;
        for(int ia:aa){Tri ta=tri(m,ia);for(int ib:bb){Tri tb=tri(m,ib);double d=dist(ta.c,tb.c);r.count++;if(d<r.minGap){r.minGap=d;bestOpp=Math.max(0,-dot(ta.n,tb.n));}}}
        r.opposition=bestOpp;
        return r;
    }
    private static List<Integer> sampleIndices(List<Integer> src,int max){if(src.size()<=max)return src;List<Integer> o=new ArrayList<>();double step=(double)src.size()/max;for(int i=0;i<max;i++)o.add(src.get(Math.min(src.size()-1,(int)Math.floor(i*step))));return o;}
    private static final class Tri{final MeshModel.V3 c,n;Tri(MeshModel.V3 c,MeshModel.V3 n){this.c=c;this.n=n;}}
    private static Tri tri(MeshModel m,int ti){int[] t=m.triangles.get(ti);MeshModel.V3 a=m.vertices.get(t[0]),b=m.vertices.get(t[1]),c=m.vertices.get(t[2]);MeshModel.V3 ce=new MeshModel.V3((a.x+b.x+c.x)/3.0,(a.y+b.y+c.y)/3.0,(a.z+b.z+c.z)/3.0);double ux=b.x-a.x,uy=b.y-a.y,uz=b.z-a.z,vx=c.x-a.x,vy=c.y-a.y,vz=c.z-a.z;double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;double q=Math.sqrt(nx*nx+ny*ny+nz*nz);if(q<1e-30)q=1;return new Tri(ce,new MeshModel.V3(nx/q,ny/q,nz/q));}
    private static double bboxGap(AssemblyBodyDecomposer.Body a,AssemblyBodyDecomposer.Body b){double gx=axisGap(a.minX,a.maxX,b.minX,b.maxX),gy=axisGap(a.minY,a.maxY,b.minY,b.maxY),gz=axisGap(a.minZ,a.maxZ,b.minZ,b.maxZ);return Math.sqrt(gx*gx+gy*gy+gz*gz);}
    private static double bboxPenetration(AssemblyBodyDecomposer.Body a,AssemblyBodyDecomposer.Body b){double ox=axisOverlap(a.minX,a.maxX,b.minX,b.maxX),oy=axisOverlap(a.minY,a.maxY,b.minY,b.maxY),oz=axisOverlap(a.minZ,a.maxZ,b.minZ,b.maxZ);return ox>0&&oy>0&&oz>0?Math.min(ox,Math.min(oy,oz)):0;}
    private static double axisGap(double a0,double a1,double b0,double b1){if(a1<b0)return b0-a1;if(b1<a0)return a0-b1;return 0;}
    private static double axisOverlap(double a0,double a1,double b0,double b1){return Math.max(0,Math.min(a1,b1)-Math.max(a0,b0));}
    private static double dist(MeshModel.V3 a,MeshModel.V3 b){double x=a.x-b.x,y=a.y-b.y,z=a.z-b.z;return Math.sqrt(x*x+y*y+z*z);}
    private static double dot(MeshModel.V3 a,MeshModel.V3 b){return a.x*b.x+a.y*b.y+a.z*b.z;}
    private static double clamp(double x){return Math.max(0,Math.min(0.99,x));}
}
