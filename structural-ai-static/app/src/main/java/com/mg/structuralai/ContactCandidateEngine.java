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
            if(bp>touchTol){st=State.INTERFERENCE_SUSPECTED;conf=0.86;}
            else if(sr.minGap<=touchTol&&sr.opposition>=0.45){st=State.TOUCHING_OR_COINCIDENT;conf=clamp(0.72+0.24*sr.opposition);}
            else if(sr.minGap<=nearTol){st=State.NEAR_CONTACT;conf=clamp(0.62+0.25*sr.opposition);}
            else if(bg<=touchTol){st=State.NEAR_CONTACT;conf=0.62;}
            else if(bg<=nearTol*3.0){st=State.FINITE_GAP;conf=0.78;}
            else {st=State.FAR;conf=0.95;}
            double evidenceGap=Double.isFinite(sr.minGap)?sr.minGap:bg;
            out.add(new Pair(a.id,b.id,st,bg,bp,evidenceGap,sr.opposition,conf,sr.count));
        }
        Collections.sort(out,(x,y)->Double.compare(x.sampledGap,y.sampledGap));
        return new Result(out,dec.bodies.size());
    }

    private static final class SampleResult{double minGap=Double.POSITIVE_INFINITY,opposition=0;int count=0;}
    private static SampleResult sample(MeshModel m,AssemblyBodyDecomposer.Body a,AssemblyBodyDecomposer.Body b,int maxPerBody){
        SampleResult r=new SampleResult();
        List<Integer> aa=sampleIndices(a.triangleIndices,maxPerBody),bb=sampleIndices(b.triangleIndices,maxPerBody);
        for(int ia:aa){Tri ta=tri(m,ia);for(int ib:bb){
            Tri tb=tri(m,ib);double d=triangleDistance(ta,tb);double opp=Math.max(0,-dot(ta.n,tb.n));r.count++;
            if(!Double.isFinite(r.minGap)){r.minGap=d;r.opposition=opp;continue;}
            double eps=Math.max(1e-12,Math.max(Math.abs(d),Math.abs(r.minGap))*1e-7);
            if(d<r.minGap-eps){r.minGap=d;r.opposition=opp;}
            else if(Math.abs(d-r.minGap)<=eps){r.opposition=Math.max(r.opposition,opp);}
        }}
        return r;
    }
    private static List<Integer> sampleIndices(List<Integer> src,int max){if(src.size()<=max)return src;List<Integer> o=new ArrayList<>();double step=(double)src.size()/max;for(int i=0;i<max;i++)o.add(src.get(Math.min(src.size()-1,(int)Math.floor(i*step))));return o;}
    private static final class Tri{final MeshModel.V3 a,b,c,n;Tri(MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c,MeshModel.V3 n){this.a=a;this.b=b;this.c=c;this.n=n;}}
    private static Tri tri(MeshModel m,int ti){int[] t=m.triangles.get(ti);MeshModel.V3 a=m.vertices.get(t[0]),b=m.vertices.get(t[1]),c=m.vertices.get(t[2]);MeshModel.V3 ab=sub(b,a),ac=sub(c,a),nn=cross(ab,ac);double q=Math.sqrt(dot(nn,nn));if(q<1e-30)q=1;return new Tri(a,b,c,scale(nn,1.0/q));}
    private static double triangleDistance(Tri x,Tri y){double d=Double.POSITIVE_INFINITY;d=Math.min(d,pointTriangleDistance(x.a,y));d=Math.min(d,pointTriangleDistance(x.b,y));d=Math.min(d,pointTriangleDistance(x.c,y));d=Math.min(d,pointTriangleDistance(y.a,x));d=Math.min(d,pointTriangleDistance(y.b,x));d=Math.min(d,pointTriangleDistance(y.c,x));MeshModel.V3[][] ex={{x.a,x.b},{x.b,x.c},{x.c,x.a}},ey={{y.a,y.b},{y.b,y.c},{y.c,y.a}};for(MeshModel.V3[] a:ex)for(MeshModel.V3[] b:ey)d=Math.min(d,segmentSegmentDistance(a[0],a[1],b[0],b[1]));return d;}
    private static double pointTriangleDistance(MeshModel.V3 p,Tri t){MeshModel.V3 ab=sub(t.b,t.a),ac=sub(t.c,t.a),ap=sub(p,t.a);double d1=dot(ab,ap),d2=dot(ac,ap);if(d1<=0&&d2<=0)return dist(p,t.a);MeshModel.V3 bp=sub(p,t.b);double d3=dot(ab,bp),d4=dot(ac,bp);if(d3>=0&&d4<=d3)return dist(p,t.b);double vc=d1*d4-d3*d2;if(vc<=0&&d1>=0&&d3<=0){double v=d1/(d1-d3);return dist(p,add(t.a,scale(ab,v)));}MeshModel.V3 cp=sub(p,t.c);double d5=dot(ab,cp),d6=dot(ac,cp);if(d6>=0&&d5<=d6)return dist(p,t.c);double vb=d5*d2-d1*d6;if(vb<=0&&d2>=0&&d6<=0){double w=d2/(d2-d6);return dist(p,add(t.a,scale(ac,w)));}double va=d3*d6-d5*d4;if(va<=0&&(d4-d3)>=0&&(d5-d6)>=0){double w=(d4-d3)/((d4-d3)+(d5-d6));return dist(p,add(t.b,scale(sub(t.c,t.b),w)));}double denom=1.0/(va+vb+vc),v=vb*denom,w=vc*denom;return dist(p,add(t.a,add(scale(ab,v),scale(ac,w))));}
    private static double segmentSegmentDistance(MeshModel.V3 p1,MeshModel.V3 q1,MeshModel.V3 p2,MeshModel.V3 q2){MeshModel.V3 d1=sub(q1,p1),d2=sub(q2,p2),r=sub(p1,p2);double a=dot(d1,d1),e=dot(d2,d2),f=dot(d2,r),s,t;if(a<=1e-30&&e<=1e-30)return dist(p1,p2);if(a<=1e-30){s=0;t=clamp01(f/e);}else{double c=dot(d1,r);if(e<=1e-30){t=0;s=clamp01(-c/a);}else{double b=dot(d1,d2),den=a*e-b*b;s=Math.abs(den)>1e-30?clamp01((b*f-c*e)/den):0;t=(b*s+f)/e;if(t<0){t=0;s=clamp01(-c/a);}else if(t>1){t=1;s=clamp01((b-c)/a);}}}return dist(add(p1,scale(d1,s)),add(p2,scale(d2,t)));}
    private static double bboxGap(AssemblyBodyDecomposer.Body a,AssemblyBodyDecomposer.Body b){double gx=axisGap(a.minX,a.maxX,b.minX,b.maxX),gy=axisGap(a.minY,a.maxY,b.minY,b.maxY),gz=axisGap(a.minZ,a.maxZ,b.minZ,b.maxZ);return Math.sqrt(gx*gx+gy*gy+gz*gz);}
    private static double bboxPenetration(AssemblyBodyDecomposer.Body a,AssemblyBodyDecomposer.Body b){double ox=axisOverlap(a.minX,a.maxX,b.minX,b.maxX),oy=axisOverlap(a.minY,a.maxY,b.minY,b.maxY),oz=axisOverlap(a.minZ,a.maxZ,b.minZ,b.maxZ);return ox>0&&oy>0&&oz>0?Math.min(ox,Math.min(oy,oz)):0;}
    private static double axisGap(double a0,double a1,double b0,double b1){if(a1<b0)return b0-a1;if(b1<a0)return a0-b1;return 0;}
    private static double axisOverlap(double a0,double a1,double b0,double b1){return Math.max(0,Math.min(a1,b1)-Math.max(a0,b0));}
    private static MeshModel.V3 sub(MeshModel.V3 a,MeshModel.V3 b){return new MeshModel.V3(a.x-b.x,a.y-b.y,a.z-b.z);}private static MeshModel.V3 add(MeshModel.V3 a,MeshModel.V3 b){return new MeshModel.V3(a.x+b.x,a.y+b.y,a.z+b.z);}private static MeshModel.V3 scale(MeshModel.V3 a,double s){return new MeshModel.V3(a.x*s,a.y*s,a.z*s);}private static MeshModel.V3 cross(MeshModel.V3 a,MeshModel.V3 b){return new MeshModel.V3(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x);}private static double dist(MeshModel.V3 a,MeshModel.V3 b){double x=a.x-b.x,y=a.y-b.y,z=a.z-b.z;return Math.sqrt(x*x+y*y+z*z);}private static double dot(MeshModel.V3 a,MeshModel.V3 b){return a.x*b.x+a.y*b.y+a.z*b.z;}private static double clamp(double x){return Math.max(0,Math.min(0.99,x));}private static double clamp01(double x){return Math.max(0,Math.min(1,x));}
}
