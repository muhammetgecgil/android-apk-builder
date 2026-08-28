package com.mg.structuralai;

import java.util.*;

/** Builds per-body TET meshes, merges the assembly and generates solver-ready contact pairs. */
public final class AssemblyTetContactBuilder {
    public static final class BodyRange {
        public final int body,nodeStart,nodeEnd,tetStart,tetEnd;
        BodyRange(int b,int ns,int ne,int ts,int te){body=b;nodeStart=ns;nodeEnd=ne;tetStart=ts;tetEnd=te;}
    }
    public static final class Result {
        public final TetMeshData mesh; public final List<BodyRange> ranges; public final ContactConstraintSet constraints;
        public final AssemblyContactEngine.Result contacts; public final boolean safeBondedOnly,safeLinearContact; public final String summary;
        Result(TetMeshData m,List<BodyRange> r,ContactConstraintSet c,AssemblyContactEngine.Result a,boolean bonded,boolean linear,String s){mesh=m;ranges=r;constraints=c;contacts=a;safeBondedOnly=bonded;safeLinearContact=linear;summary=s;}
    }
    private AssemblyTetContactBuilder(){}

    public static Result build(MeshModel source,int cells,double scale){
        AssemblyContactEngine.Result ac=AssemblyContactEngine.analyze(source);
        if(ac.components.size()<2)throw new IllegalArgumentException("Assembly contact builder requires multiple disconnected bodies");
        TetMeshData global=new TetMeshData();List<BodyRange> ranges=new ArrayList<>();
        for(AssemblyContactEngine.Component c:ac.components){
            MeshModel body=extractWelded(source,c.triangles);
            SurfaceTopologyReport topo=SurfaceTopologyReport.evaluate(body);
            if(!topo.closedManifold)throw new IllegalStateException("Body "+c.id+" topology blocked after weld: "+topo.summary());
            SmartTetMesher.Result sm=SmartTetMesher.generate(body,Math.max(8,cells),scale);
            if(!sm.quality.pass)throw new IllegalStateException("Body "+c.id+" mesh QA blocked: "+sm.quality.summary());
            int ns=global.nodes.size(),ts=global.tets.size();
            for(MeshModel.V3 p:sm.mesh.nodes)global.addNode(p.x,p.y,p.z);
            for(int[] t:sm.mesh.tets)global.addTet(ns+t[0],ns+t[1],ns+t[2],ns+t[3]);
            ranges.add(new BodyRange(c.id,ns,global.nodes.size(),ts,global.tets.size()));
        }
        global.validate();ContactConstraintSet set=new ContactConstraintSet();boolean unresolved=false;int bondedContacts=0,frictionlessContacts=0;double diagM=source.diagonal()*scale,pairTol=Math.max(diagM*0.006,1e-7),touchTol=Math.max(diagM*1e-5,1e-9);
        for(AssemblyContactEngine.Pair p:ac.pairs){
            if(p.type==AssemblyContactEngine.Type.SEPARATED||p.type==AssemblyContactEngine.Type.NEAR_GAP)continue;
            BodyRange a=find(ranges,p.a),b=find(ranges,p.b);
            if(p.type==AssemblyContactEngine.Type.BONDED_CANDIDATE&&p.confidence>=0.80){int n=pairNearest(global,a,b,pairTol,set,p.confidence,p.normal,ContactConstraintSet.Kind.BONDED_TIE);if(n>0)bondedContacts++;else unresolved=true;}
            else if(p.type==AssemblyContactEngine.Type.FRICTIONLESS_CANDIDATE&&p.confidence>=0.68&&(p.minGap*scale)<=touchTol){int n=pairNearest(global,a,b,pairTol,set,p.confidence,p.normal,ContactConstraintSet.Kind.FRICTIONLESS_NORMAL);if(n>0)frictionlessContacts++;else unresolved=true;}
            else unresolved=true;
        }
        boolean bondedOnly=bondedContacts>0&&frictionlessContacts==0&&!unresolved&&set.bondedCount()>=3;
        boolean linearSafe=(bondedContacts+frictionlessContacts)>0&&!unresolved&&(set.bondedCount()+count(set,ContactConstraintSet.Kind.FRICTIONLESS_NORMAL)>=3);
        String text="bodies="+ranges.size()+" | bondedContacts="+bondedContacts+" | frictionlessContacts="+frictionlessContacts+" | tiedNodePairs="+set.bondedCount()+" | normalPairs="+count(set,ContactConstraintSet.Kind.FRICTIONLESS_NORMAL)+" | touchTol="+touchTol+" m | finiteGapRequiresLoadStepping=true | unresolved="+unresolved+" | safeBondedOnly="+bondedOnly+" | safeLinearContact="+linearSafe;
        return new Result(global,ranges,set,ac,bondedOnly,linearSafe,text);
    }

    private static int pairNearest(TetMeshData m,BodyRange a,BodyRange b,double tol,ContactConstraintSet out,double conf,MeshModel.V3 normal,ContactConstraintSet.Kind kind){
        List<int[]> candidates=new ArrayList<>();for(int i=a.nodeStart;i<a.nodeEnd;i++){MeshModel.V3 p=m.nodes.get(i);int bj=-1;double best=Double.POSITIVE_INFINITY;for(int j=b.nodeStart;j<b.nodeEnd;j++){double d=dist(p,m.nodes.get(j));if(d<best){best=d;bj=j;}}if(bj>=0&&best<=tol)candidates.add(new int[]{i,bj});}
        LinkedHashMap<Long,int[]> unique=new LinkedHashMap<>();for(int[] q:candidates){long k=(((long)q[0])<<32)^(q[1]&0xffffffffL);unique.put(k,q);}int stride=Math.max(1,unique.size()/80),ix=0,n=0;
        for(int[] q:unique.values()){if((ix++%stride)!=0)continue;MeshModel.V3 pa=m.nodes.get(q[0]),pb=m.nodes.get(q[1]);double g=dist(pa,pb);out.add(q[0],q[1],kind,normal,g,conf);n++;if(n>=80)break;}return n;
    }
    private static int count(ContactConstraintSet s,ContactConstraintSet.Kind k){int n=0;for(ContactConstraintSet.Pair p:s.pairs)if(p.kind==k)n++;return n;}
    private static BodyRange find(List<BodyRange> r,int id){for(BodyRange x:r)if(x.body==id)return x;throw new IllegalStateException("Body range missing: "+id);}

    /**
     * STL/OBJ bodies may contain the same geometric corner under different source vertex IDs.
     * Rebuild each body by true geometric-distance welding. No coordinate quantization/hash rounding is used,
     * because non-finite or extreme values could alias unrelated vertices. The tolerance is body-relative and
     * intentionally many orders below any contact tolerance so real gaps cannot be bridged.
     */
    private static MeshModel extractWelded(MeshModel src,List<Integer> tris){
        MeshModel o=new MeshModel();
        if(tris==null||tris.isEmpty())throw new IllegalStateException("Per-body topology repair received no triangles");
        double xmin=Double.POSITIVE_INFINITY,ymin=Double.POSITIVE_INFINITY,zmin=Double.POSITIVE_INFINITY;
        double xmax=Double.NEGATIVE_INFINITY,ymax=Double.NEGATIVE_INFINITY,zmax=Double.NEGATIVE_INFINITY;
        for(int ti:tris){int[] t=src.triangles.get(ti);if(t==null)continue;for(int k=0;k<Math.min(3,t.length);k++){MeshModel.V3 p=src.vertices.get(t[k]);if(!finite(p))throw new IllegalStateException("Non-finite vertex in body topology repair");xmin=Math.min(xmin,p.x);xmax=Math.max(xmax,p.x);ymin=Math.min(ymin,p.y);ymax=Math.max(ymax,p.y);zmin=Math.min(zmin,p.z);zmax=Math.max(zmax,p.z);}}
        double dx=xmax-xmin,dy=ymax-ymin,dz=zmax-zmin,diag=Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(!Double.isFinite(diag)||diag<=0)throw new IllegalStateException("Invalid body span in topology repair");
        double tol=Math.max(diag*1e-9,1e-12),tol2=tol*tol;
        Set<String> faces=new LinkedHashSet<>();
        int input=0,degenerate=0,duplicate=0;
        for(int ti:tris){
            int[] t=src.triangles.get(ti);if(t==null||t.length<3)continue;input++;
            int[] nt=new int[3];
            for(int k=0;k<3;k++)nt[k]=findOrAdd(o,src.vertices.get(t[k]),tol2);
            if(nt[0]==nt[1]||nt[1]==nt[2]||nt[2]==nt[0]){degenerate++;continue;}
            int[] srt={nt[0],nt[1],nt[2]};Arrays.sort(srt);String fk=srt[0]+":"+srt[1]+":"+srt[2];
            if(faces.add(fk))o.triangles.add(nt);else duplicate++;
        }
        if(o.triangles.size()<4)throw new IllegalStateException("Per-body topology repair produced too few triangles | input="+input+" weldedVertices="+o.vertices.size()+" output="+o.triangles.size()+" degenerate="+degenerate+" duplicate="+duplicate+" tol="+tol);
        return o;
    }
    private static int findOrAdd(MeshModel o,MeshModel.V3 p,double tol2){
        for(int i=0;i<o.vertices.size();i++){MeshModel.V3 q=o.vertices.get(i);double x=p.x-q.x,y=p.y-q.y,z=p.z-q.z;if(x*x+y*y+z*z<=tol2)return i;}
        int n=o.vertices.size();o.addVertex(new MeshModel.V3(p.x,p.y,p.z));return n;
    }
    private static boolean finite(MeshModel.V3 p){return p!=null&&Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
    private static double dist(MeshModel.V3 a,MeshModel.V3 b){double x=a.x-b.x,y=a.y-b.y,z=a.z-b.z;return Math.sqrt(x*x+y*y+z*z);}
}
