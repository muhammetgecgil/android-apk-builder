package com.mg.structuralai;

import java.util.*;

/** Geometry-only contact candidate detector for disconnected tessellated bodies. */
public final class AssemblyContactEngine {
    public enum Type { BONDED_CANDIDATE, FRICTIONLESS_CANDIDATE, NEAR_GAP, SEPARATED, UNRESOLVED }
    public static final class Component {
        public final int id; public final List<Integer> triangles;
        public final double minX,minY,minZ,maxX,maxY,maxZ;
        Component(int id,List<Integer> t,double a,double b,double c,double d,double e,double f){this.id=id;triangles=t;minX=a;minY=b;minZ=c;maxX=d;maxY=e;maxZ=f;}
        MeshModel.V3 center(){return new MeshModel.V3((minX+maxX)/2,(minY+maxY)/2,(minZ+maxZ)/2);}
    }
    public static final class Pair {
        public final int a,b; public final Type type; public final double minGap,normalOpposition,confidence;
        public final int closeSamples; public final MeshModel.V3 normal;
        Pair(int a,int b,Type t,double g,double n,double c,int s,MeshModel.V3 normal){this.a=a;this.b=b;type=t;minGap=g;normalOpposition=n;confidence=c;closeSamples=s;this.normal=normal;}
    }
    public static final class Result {
        public final List<Component> components; public final List<Pair> pairs; public final boolean assemblyLike;
        Result(List<Component> c,List<Pair> p){components=c;pairs=p;assemblyLike=c.size()>1;}
        public String summary(double scaleM){StringBuilder sb=new StringBuilder();sb.append("components=").append(components.size()).append(" | assemblyLike=").append(assemblyLike);for(Pair p:pairs){sb.append("\nC").append(p.a).append("-C").append(p.b).append(": ").append(p.type).append(" | gap=").append(String.format(Locale.US,"%.6g",p.minGap*scaleM*1000)).append(" mm").append(" | opposingNormals=").append(String.format(Locale.US,"%.0f%%",p.normalOpposition*100)).append(" | conf=").append(String.format(Locale.US,"%.0f%%",p.confidence*100)).append(" | n=[").append(String.format(Locale.US,"%.2f,%.2f,%.2f",p.normal.x,p.normal.y,p.normal.z)).append("] | closeSamples=").append(p.closeSamples);}return sb.toString();}
    }
    private AssemblyContactEngine(){}

    public static Result analyze(MeshModel m){List<Component> comps=split(m);List<Pair> pairs=new ArrayList<>();double diag=Math.max(m.diagonal(),1e-9),closeTol=diag*0.003,nearTol=diag*0.02;for(int i=0;i<comps.size();i++)for(int j=i+1;j<comps.size();j++)pairs.add(classify(m,comps.get(i),comps.get(j),closeTol,nearTol));return new Result(comps,pairs);}

    /**
     * Split by geometrically shared edges rather than raw vertex IDs. STL/OBJ commonly duplicate
     * vertex records per triangle, so ID-based adjacency incorrectly turns one closed body into
     * many one-triangle components. The quantization is tiny relative to model size and is used
     * only for topology identity; it must not bridge physical contact gaps.
     */
    private static List<Component> split(MeshModel m){
        int nt=m.triangles.size();
        if(nt==0)return Collections.emptyList();
        double q=Math.max(m.diagonal()*1e-9,1e-10);
        Map<GEdge,List<Integer>> owners=new HashMap<>();
        for(int ti=0;ti<nt;ti++){
            int[] t=m.triangles.get(ti);if(t==null||t.length<3)continue;
            GKey a=gkey(m.vertices.get(t[0]),q),b=gkey(m.vertices.get(t[1]),q),c=gkey(m.vertices.get(t[2]),q);
            addOwner(owners,new GEdge(a,b),ti);addOwner(owners,new GEdge(b,c),ti);addOwner(owners,new GEdge(c,a),ti);
        }
        List<List<Integer>> adj=new ArrayList<>();for(int i=0;i<nt;i++)adj.add(new ArrayList<>());
        for(List<Integer> ids:owners.values())if(ids.size()>1)for(int i=0;i<ids.size();i++)for(int j=i+1;j<ids.size();j++){int a=ids.get(i),b=ids.get(j);adj.get(a).add(b);adj.get(b).add(a);}
        boolean[] seen=new boolean[nt];List<Component> out=new ArrayList<>();
        for(int seed=0;seed<nt;seed++)if(!seen[seed]){
            ArrayDeque<Integer> dq=new ArrayDeque<>();dq.add(seed);seen[seed]=true;List<Integer> ids=new ArrayList<>();
            double x0=Double.POSITIVE_INFINITY,y0=Double.POSITIVE_INFINITY,z0=Double.POSITIVE_INFINITY,x1=Double.NEGATIVE_INFINITY,y1=Double.NEGATIVE_INFINITY,z1=Double.NEGATIVE_INFINITY;
            while(!dq.isEmpty()){
                int ti=dq.removeFirst();ids.add(ti);int[] tri=m.triangles.get(ti);
                for(int v:tri){MeshModel.V3 p=m.vertices.get(v);x0=Math.min(x0,p.x);y0=Math.min(y0,p.y);z0=Math.min(z0,p.z);x1=Math.max(x1,p.x);y1=Math.max(y1,p.y);z1=Math.max(z1,p.z);}
                for(int nb:adj.get(ti))if(!seen[nb]){seen[nb]=true;dq.addLast(nb);}
            }
            out.add(new Component(out.size(),ids,x0,y0,z0,x1,y1,z1));
        }
        Collections.sort(out,(a,b)->Integer.compare(b.triangles.size(),a.triangles.size()));
        List<Component> renum=new ArrayList<>();for(int i=0;i<out.size();i++){Component c=out.get(i);renum.add(new Component(i,c.triangles,c.minX,c.minY,c.minZ,c.maxX,c.maxY,c.maxZ));}
        return renum;
    }
    private static void addOwner(Map<GEdge,List<Integer>> m,GEdge e,int ti){m.computeIfAbsent(e,k->new ArrayList<>()).add(ti);}
    private static GKey gkey(MeshModel.V3 p,double q){return new GKey(Math.round(p.x/q),Math.round(p.y/q),Math.round(p.z/q));}
    private static final class GKey implements Comparable<GKey>{final long x,y,z;GKey(long x,long y,long z){this.x=x;this.y=y;this.z=z;}public int compareTo(GKey o){int c=Long.compare(x,o.x);if(c!=0)return c;c=Long.compare(y,o.y);return c!=0?c:Long.compare(z,o.z);}public int hashCode(){return Objects.hash(x,y,z);}public boolean equals(Object o){if(!(o instanceof GKey))return false;GKey k=(GKey)o;return x==k.x&&y==k.y&&z==k.z;}}
    private static final class GEdge{final GKey a,b;GEdge(GKey x,GKey y){if(x.compareTo(y)<=0){a=x;b=y;}else{a=y;b=x;}}public int hashCode(){return 31*a.hashCode()+b.hashCode();}public boolean equals(Object o){if(!(o instanceof GEdge))return false;GEdge e=(GEdge)o;return a.equals(e.a)&&b.equals(e.b);}}

    private static Pair classify(MeshModel m,Component a,Component b,double closeTol,double nearTol){
        double bg=boxGap(a,b);MeshModel.V3 centerDir=unit(sub(b.center(),a.center()));if(bg>nearTol*2)return new Pair(a.id,b.id,Type.SEPARATED,bg,0,0.98,0,centerDir);
        double best=Double.POSITIVE_INFINITY,oppSum=0;int close=0,oppN=0;MeshModel.V3 bestNormal=centerDir;double bestOpp=-1;
        int sa=Math.max(1,a.triangles.size()/120),sb=Math.max(1,b.triangles.size()/120);
        for(int ia=0;ia<a.triangles.size();ia+=sa){int ta=a.triangles.get(ia);MeshModel.V3 ca=centroid(m,ta),na=normal(m,ta);for(int ib=0;ib<b.triangles.size();ib+=sb){int tb=b.triangles.get(ib);MeshModel.V3 cb=centroid(m,tb),nb=normal(m,tb);double d=dist(ca,cb),opp=Math.max(0,-dot(na,nb));if(d<best){best=d;bestNormal=orient(na,centerDir);}if(d<=nearTol){oppSum+=opp;oppN++;if(d<=closeTol&&opp>0.65){close++;if(opp>bestOpp){bestOpp=opp;bestNormal=orient(na,centerDir);}}}}}
        double opp=oppN>0?oppSum/oppN:0;Type type;double conf;if(best<=closeTol&&opp>=0.80&&close>=2){type=Type.BONDED_CANDIDATE;conf=Math.min(0.95,0.55+0.35*opp+0.02*Math.min(close,5));}else if(best<=closeTol&&opp>=0.55){type=Type.FRICTIONLESS_CANDIDATE;conf=Math.min(0.88,0.45+0.35*opp);}else if(best<=nearTol){type=Type.NEAR_GAP;conf=0.55+0.25*Math.max(0,1-best/nearTol);}else{type=Type.SEPARATED;conf=0.90;}return new Pair(a.id,b.id,type,best,opp,conf,close,unit(bestNormal));
    }
    private static MeshModel.V3 orient(MeshModel.V3 n,MeshModel.V3 toward){return dot(n,toward)>=0?n:new MeshModel.V3(-n.x,-n.y,-n.z);}
    private static double boxGap(Component a,Component b){double dx=Math.max(0,Math.max(a.minX-b.maxX,b.minX-a.maxX)),dy=Math.max(0,Math.max(a.minY-b.maxY,b.minY-a.maxY)),dz=Math.max(0,Math.max(a.minZ-b.maxZ,b.minZ-a.maxZ));return Math.sqrt(dx*dx+dy*dy+dz*dz);}
    private static MeshModel.V3 centroid(MeshModel m,int ti){int[] t=m.triangles.get(ti);MeshModel.V3 a=m.vertices.get(t[0]),b=m.vertices.get(t[1]),c=m.vertices.get(t[2]);return new MeshModel.V3((a.x+b.x+c.x)/3,(a.y+b.y+c.y)/3,(a.z+b.z+c.z)/3);}
    private static MeshModel.V3 normal(MeshModel m,int ti){int[] t=m.triangles.get(ti);MeshModel.V3 a=m.vertices.get(t[0]),b=m.vertices.get(t[1]),c=m.vertices.get(t[2]);double ux=b.x-a.x,uy=b.y-a.y,uz=b.z-a.z,vx=c.x-a.x,vy=c.y-a.y,vz=c.z-a.z;return unit(new MeshModel.V3(uy*vz-uz*vy,uz*vx-ux*vz,ux*vy-uy*vx));}
    private static MeshModel.V3 sub(MeshModel.V3 a,MeshModel.V3 b){return new MeshModel.V3(a.x-b.x,a.y-b.y,a.z-b.z);}private static MeshModel.V3 unit(MeshModel.V3 a){double q=Math.sqrt(dot(a,a));return q>1e-30?new MeshModel.V3(a.x/q,a.y/q,a.z/q):new MeshModel.V3(1,0,0);}private static double dot(MeshModel.V3 a,MeshModel.V3 b){return a.x*b.x+a.y*b.y+a.z*b.z;}private static double dist(MeshModel.V3 a,MeshModel.V3 b){double x=a.x-b.x,y=a.y-b.y,z=a.z-b.z;return Math.sqrt(x*x+y*y+z*z);}
}
