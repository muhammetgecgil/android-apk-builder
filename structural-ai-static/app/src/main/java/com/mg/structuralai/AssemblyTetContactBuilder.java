package com.mg.structuralai;

import java.util.*;

/**
 * Builds separate volumetric meshes for disconnected surface components, merges them into one
 * global solver mesh, and maps high-confidence geometry contact candidates to node-pair ties.
 * Alpha scope: bonded/tied contact only. Frictionless/frictional remain unresolved and blocked.
 */
public final class AssemblyTetContactBuilder {
    public static final class BodyRange {
        public final int body,nodeStart,nodeEnd,tetStart,tetEnd;
        BodyRange(int b,int ns,int ne,int ts,int te){body=b;nodeStart=ns;nodeEnd=ne;tetStart=ts;tetEnd=te;}
    }
    public static final class Result {
        public final TetMeshData mesh; public final List<BodyRange> ranges; public final ContactConstraintSet constraints;
        public final AssemblyContactEngine.Result contacts; public final boolean safeBondedOnly; public final String summary;
        Result(TetMeshData m,List<BodyRange> r,ContactConstraintSet c,AssemblyContactEngine.Result a,boolean ok,String s){mesh=m;ranges=r;constraints=c;contacts=a;safeBondedOnly=ok;summary=s;}
    }
    private AssemblyTetContactBuilder(){}

    public static Result build(MeshModel source,int cells,double scale){
        AssemblyContactEngine.Result ac=AssemblyContactEngine.analyze(source);
        if(ac.components.size()<2)throw new IllegalArgumentException("Assembly contact builder requires multiple disconnected bodies");
        TetMeshData global=new TetMeshData();List<BodyRange> ranges=new ArrayList<>();
        for(AssemblyContactEngine.Component c:ac.components){
            MeshModel body=extract(source,c.triangles);
            SmartTetMesher.Result sm=SmartTetMesher.generate(body,Math.max(8,cells),scale);
            if(!sm.quality.pass)throw new IllegalStateException("Body "+c.id+" mesh QA blocked: "+sm.quality.summary());
            int ns=global.nodes.size(),ts=global.tets.size();
            for(MeshModel.V3 p:sm.mesh.nodes)global.addNode(p.x,p.y,p.z);
            for(int[] t:sm.mesh.tets)global.addTet(ns+t[0],ns+t[1],ns+t[2],ns+t[3]);
            ranges.add(new BodyRange(c.id,ns,global.nodes.size(),ts,global.tets.size()));
        }
        global.validate();
        ContactConstraintSet set=new ContactConstraintSet();
        boolean unresolved=false;int acceptedContacts=0;
        double diagM=source.diagonal()*scale;
        double pairTol=Math.max(diagM*0.006,1e-7);
        for(AssemblyContactEngine.Pair p:ac.pairs){
            if(p.type==AssemblyContactEngine.Type.SEPARATED||p.type==AssemblyContactEngine.Type.NEAR_GAP)continue;
            if(p.type==AssemblyContactEngine.Type.BONDED_CANDIDATE&&p.confidence>=0.80){
                BodyRange a=find(ranges,p.a),b=find(ranges,p.b);
                int n=pairNearest(global,a,b,pairTol,set,p.confidence);
                if(n>0)acceptedContacts++; else unresolved=true;
            }else unresolved=true;
        }
        boolean safe=acceptedContacts>0&&!unresolved&&set.bondedCount()>=3;
        String text="bodies="+ranges.size()+" | bondedContacts="+acceptedContacts+" | tiedNodePairs="+set.bondedCount()+" | unresolved="+unresolved+" | safeBondedOnly="+safe;
        return new Result(global,ranges,set,ac,safe,text);
    }

    private static int pairNearest(TetMeshData m,BodyRange a,BodyRange b,double tol,ContactConstraintSet out,double conf){
        List<int[]> candidates=new ArrayList<>();
        for(int i=a.nodeStart;i<a.nodeEnd;i++){
            MeshModel.V3 p=m.nodes.get(i);int bj=-1;double best=Double.POSITIVE_INFINITY;
            for(int j=b.nodeStart;j<b.nodeEnd;j++){double d=dist(p,m.nodes.get(j));if(d<best){best=d;bj=j;}}
            if(bj>=0&&best<=tol)candidates.add(new int[]{i,bj});
        }
        // deduplicate slave/master pairs and cap density to avoid over-stiffening alpha contact.
        LinkedHashMap<Long,int[]> unique=new LinkedHashMap<>();
        for(int[] q:candidates){long k=(((long)q[0])<<32)^(q[1]&0xffffffffL);unique.put(k,q);}
        int stride=Math.max(1,unique.size()/80),ix=0,n=0;
        for(int[] q:unique.values()){if((ix++%stride)!=0)continue;MeshModel.V3 pa=m.nodes.get(q[0]),pb=m.nodes.get(q[1]);double g=dist(pa,pb);out.add(q[0],q[1],ContactConstraintSet.Kind.BONDED_TIE,unit(pb.x-pa.x,pb.y-pa.y,pb.z-pa.z),g,conf);n++;if(n>=80)break;}
        return n;
    }
    private static BodyRange find(List<BodyRange> r,int id){for(BodyRange x:r)if(x.body==id)return x;throw new IllegalStateException("Body range missing: "+id);}
    private static MeshModel extract(MeshModel src,List<Integer> tris){
        MeshModel o=new MeshModel();Map<Integer,Integer> map=new HashMap<>();
        for(int ti:tris){int[] t=src.triangles.get(ti);int[] nt=new int[3];for(int k=0;k<3;k++){Integer q=map.get(t[k]);if(q==null){q=o.vertices.size();map.put(t[k],q);o.addVertex(src.vertices.get(t[k]));}nt[k]=q;}o.triangles.add(nt);}return o;
    }
    private static MeshModel.V3 unit(double x,double y,double z){double q=Math.sqrt(x*x+y*y+z*z);return q>1e-30?new MeshModel.V3(x/q,y/q,z/q):new MeshModel.V3(0,0,1);}
    private static double dist(MeshModel.V3 a,MeshModel.V3 b){double x=a.x-b.x,y=a.y-b.y,z=a.z-b.z;return Math.sqrt(x*x+y*y+z*z);}
}
