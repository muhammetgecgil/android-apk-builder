package com.mg.structuralai;

import java.util.*;

/** Splits a tessellated model into disconnected surface bodies using quantized shared edges. */
public final class AssemblyBodyDecomposer {
    public static final class Body {
        public final int id;
        public final List<Integer> triangleIndices;
        public final double minX,minY,minZ,maxX,maxY,maxZ;
        Body(int id,List<Integer> tris,double[] b){this.id=id;triangleIndices=tris;minX=b[0];minY=b[1];minZ=b[2];maxX=b[3];maxY=b[4];maxZ=b[5];}
        public double dx(){return Math.max(0,maxX-minX);} public double dy(){return Math.max(0,maxY-minY);} public double dz(){return Math.max(0,maxZ-minZ);}
        public double diagonal(){return Math.sqrt(dx()*dx()+dy()*dy()+dz()*dz());}
        public String summary(){return "body="+id+" triangles="+triangleIndices.size()+" bbox=["+minX+","+minY+","+minZ+"]..["+maxX+","+maxY+","+maxZ+"]";}
    }
    public static final class Result {
        public final List<Body> bodies;
        public final double quantization;
        Result(List<Body> b,double q){bodies=b;quantization=q;}
        public boolean isAssembly(){return bodies.size()>1;}
    }
    private AssemblyBodyDecomposer(){}

    public static Result decompose(MeshModel m){
        int nt=m.triangles.size();
        if(nt==0)return new Result(Collections.<Body>emptyList(),1e-9);
        double q=Math.max(m.diagonal()*1e-8,1e-9);
        Map<Edge,List<Integer>> owners=new HashMap<>();
        for(int ti=0;ti<nt;ti++){
            int[] t=m.triangles.get(ti);
            Key a=key(m.vertices.get(t[0]),q),b=key(m.vertices.get(t[1]),q),c=key(m.vertices.get(t[2]),q);
            add(owners,new Edge(a,b),ti);add(owners,new Edge(b,c),ti);add(owners,new Edge(c,a),ti);
        }
        List<List<Integer>> adj=new ArrayList<>(nt);for(int i=0;i<nt;i++)adj.add(new ArrayList<Integer>());
        for(List<Integer> os:owners.values())if(os.size()>1)for(int i=0;i<os.size();i++)for(int j=i+1;j<os.size();j++){adj.get(os.get(i)).add(os.get(j));adj.get(os.get(j)).add(os.get(i));}
        boolean[] seen=new boolean[nt];List<Body> bodies=new ArrayList<>();
        for(int seed=0;seed<nt;seed++)if(!seen[seed]){
            ArrayDeque<Integer> dq=new ArrayDeque<>();dq.add(seed);seen[seed]=true;List<Integer> tris=new ArrayList<>();
            double[] bb={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
            while(!dq.isEmpty()){
                int ti=dq.removeFirst();tris.add(ti);int[] t=m.triangles.get(ti);
                for(int vi:t){MeshModel.V3 p=m.vertices.get(vi);bb[0]=Math.min(bb[0],p.x);bb[1]=Math.min(bb[1],p.y);bb[2]=Math.min(bb[2],p.z);bb[3]=Math.max(bb[3],p.x);bb[4]=Math.max(bb[4],p.y);bb[5]=Math.max(bb[5],p.z);}
                for(int nb:adj.get(ti))if(!seen[nb]){seen[nb]=true;dq.addLast(nb);}
            }
            bodies.add(new Body(bodies.size(),tris,bb));
        }
        Collections.sort(bodies,(a,b)->Integer.compare(b.triangleIndices.size(),a.triangleIndices.size()));
        List<Body> renum=new ArrayList<>();for(int i=0;i<bodies.size();i++){Body x=bodies.get(i);renum.add(new Body(i,x.triangleIndices,new double[]{x.minX,x.minY,x.minZ,x.maxX,x.maxY,x.maxZ}));}
        return new Result(renum,q);
    }
    private static void add(Map<Edge,List<Integer>> m,Edge e,int t){m.computeIfAbsent(e,k->new ArrayList<Integer>()).add(t);}
    private static Key key(MeshModel.V3 p,double q){return new Key(Math.round(p.x/q),Math.round(p.y/q),Math.round(p.z/q));}
    private static final class Key implements Comparable<Key>{final long x,y,z;Key(long a,long b,long c){x=a;y=b;z=c;}public int compareTo(Key o){int c=Long.compare(x,o.x);if(c!=0)return c;c=Long.compare(y,o.y);return c!=0?c:Long.compare(z,o.z);}public int hashCode(){return Objects.hash(x,y,z);}public boolean equals(Object o){if(!(o instanceof Key))return false;Key k=(Key)o;return x==k.x&&y==k.y&&z==k.z;}}
    private static final class Edge{final Key a,b;Edge(Key x,Key y){if(x.compareTo(y)<=0){a=x;b=y;}else{a=y;b=x;}}public int hashCode(){return 31*a.hashCode()+b.hashCode();}public boolean equals(Object o){if(!(o instanceof Edge))return false;Edge e=(Edge)o;return a.equals(e.a)&&b.equals(e.b);}}
}
