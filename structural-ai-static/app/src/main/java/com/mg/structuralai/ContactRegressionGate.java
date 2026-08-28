package com.mg.structuralai;

/** End-to-end synthetic regression for bonded and frictionless contact behavior. */
public final class ContactRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private ContactRegressionGate(){}

    public static Result run(){
        try{
            MeshModel touching=twoBlocks(0.0);AssemblyContactEngine.Result a=AssemblyContactEngine.analyze(touching);
            if(a.components.size()!=2)return new Result(false,"CONTACT REGRESSION FAIL: components="+a.components.size());
            boolean bonded=false;for(AssemblyContactEngine.Pair p:a.pairs)if(p.type==AssemblyContactEngine.Type.BONDED_CANDIDATE)bonded=true;
            if(!bonded)return new Result(false,"CONTACT REGRESSION FAIL: no bonded candidate | "+a.summary(0.001));
            AssemblyTetContactBuilder.Result asm=AssemblyTetContactBuilder.build(touching,8,0.001);
            if(asm.constraints.bondedCount()<3)return new Result(false,"CONTACT REGRESSION FAIL: tiedNodePairs="+asm.constraints.bondedCount()+" | "+asm.summary);
            LinearElasticMaterial mat=new LinearElasticMaterial("ContactRegressionAl",70e9,0.33,2700.0,250e6);

            Bounds b=bounds(asm.mesh);double tol=Math.max((b.xmax-b.xmin)*0.04,1e-8);
            StaticFemSolver bondedSolver=new StaticFemSolver(asm.mesh,mat);bondedSolver.addContactConstraints(asm.constraints);
            for(int i=0;i<asm.mesh.nodes.size();i++){MeshModel.V3 p=asm.mesh.nodes.get(i);if(p.x<=b.xmin+tol)bondedSolver.fixNode(i);if(p.x>=b.xmax-tol)bondedSolver.addNodalForce(i,-1.0,0,0);}
            StaticFemSolver.Result fb=bondedSolver.solve();boolean bondedNumerical=fb.linearSolve.converged&&fb.maxDisplacementM>0&&fb.forceEquilibriumRelativeError<1e-4;

            ContactConstraintSet friction=new ContactConstraintSet();int fpairs=0;for(ContactConstraintSet.Pair p:asm.constraints.pairs)if(p.kind==ContactConstraintSet.Kind.BONDED_TIE){friction.add(p.nodeA,p.nodeB,ContactConstraintSet.Kind.FRICTIONLESS_NORMAL,p.normal,p.gapM,p.confidence);fpairs++;}
            if(fpairs<3)return new Result(false,"CONTACT REGRESSION FAIL: frictionless pairs="+fpairs);

            StaticFemSolver compress=new StaticFemSolver(asm.mesh,mat);compress.addContactConstraints(friction);stabilizeTangential(compress,asm.mesh,b,tol,false);
            for(int i=0;i<asm.mesh.nodes.size();i++){MeshModel.V3 p=asm.mesh.nodes.get(i);if(p.x>=b.xmax-tol)compress.addNodalForce(i,-1.0,0,0);}
            StaticFemSolver.Result fc=compress.solve();boolean compressionOk=fc.linearSolve.converged&&fc.activeFrictionlessContacts>0&&fc.forceEquilibriumRelativeError<1e-4;

            StaticFemSolver opening=new StaticFemSolver(asm.mesh,mat);opening.addContactConstraints(friction);
            // Once contact opens, the right component becomes mechanically independent. Use a minimal 3-2-1
            // reference stabilization on its far face: node1 XYZ, node2 YZ, node3 Z. This removes all six rigid-body
            // modes without tying the interface or forcing the contact itself to remain closed.
            stabilizeTangential(opening,asm.mesh,b,tol,true);
            int interfaceLoads=0;double fpair=1.0/Math.max(1,fpairs);
            for(ContactConstraintSet.Pair p:friction.pairs){
                double q=Math.sqrt(p.normal.x*p.normal.x+p.normal.y*p.normal.y+p.normal.z*p.normal.z);
                double nx=p.normal.x/q,ny=p.normal.y/q,nz=p.normal.z/q;
                opening.addNodalForce(p.nodeB,fpair*nx,fpair*ny,fpair*nz);interfaceLoads++;
            }
            StaticFemSolver.Result fo=opening.solve();
            boolean released=fo.activeFrictionlessContacts<fpairs;
            boolean openingOk=fo.linearSolve.converged&&released&&fo.contactIterations>=2;

            MeshModel separated=twoBlocks(20.0);AssemblyContactEngine.Result sep=AssemblyContactEngine.analyze(separated);boolean separatedOk=sep.pairs.size()==1&&sep.pairs.get(0).type==AssemblyContactEngine.Type.SEPARATED;
            boolean ok=bondedNumerical&&compressionOk&&openingOk&&separatedOk;
            String txt="CONTACT REGRESSION "+(ok?"PASS":"FAIL")+" | bondedTies="+asm.constraints.bondedCount()+" | bondedEq="+fb.forceEquilibriumRelativeError+" | frictionPairs="+fpairs+" | compressionActive="+fc.activeFrictionlessContacts+" | compressionEq="+fc.forceEquilibriumRelativeError+" | openingActive="+fo.activeFrictionlessContacts+" | openingReleased="+released+" | openingIterations="+fo.contactIterations+" | separatedCheck="+separatedOk;
            return new Result(ok,txt);
        }catch(Exception e){return new Result(false,"CONTACT REGRESSION EXCEPTION: "+e.getMessage());}
    }

    private static void stabilizeTangential(StaticFemSolver s,TetMeshData m,Bounds b,double tol,boolean full321){
        int first=-1,second=-1,third=-1;
        for(int i=0;i<m.nodes.size();i++){
            MeshModel.V3 p=m.nodes.get(i);
            if(p.x<=b.xmin+tol)s.fixNode(i);
            if(p.x>=b.xmax-tol){
                if(first<0)first=i;
                else if(second<0&&geometricallyDistinct(m.nodes.get(first),p))second=i;
                else if(third<0&&second>=0&&geometricallyDistinct(m.nodes.get(first),p)&&geometricallyDistinct(m.nodes.get(second),p))third=i;
            }
        }
        if(first>=0){s.fixDof(3*first+1);s.fixDof(3*first+2);if(full321)s.fixDof(3*first);}
        if(second>=0){s.fixDof(3*second+2);if(full321)s.fixDof(3*second+1);}
        if(full321&&third>=0)s.fixDof(3*third+2);
        if(full321&&(first<0||second<0||third<0))throw new IllegalStateException("3-2-1 contact stabilization could not find three independent far-face nodes");
    }
    private static boolean geometricallyDistinct(MeshModel.V3 a,MeshModel.V3 b){double dx=a.x-b.x,dy=a.y-b.y,dz=a.z-b.z;return dx*dx+dy*dy+dz*dz>1e-24;}
    private static final class Bounds{double xmin=1e99,xmax=-1e99;}private static Bounds bounds(TetMeshData m){Bounds b=new Bounds();for(MeshModel.V3 p:m.nodes){b.xmin=Math.min(b.xmin,p.x);b.xmax=Math.max(b.xmax,p.x);}return b;}
    private static MeshModel twoBlocks(double gap){MeshModel m=new MeshModel();addBox(m,0,10,0,10,0,10);addBox(m,10+gap,20+gap,0,10,0,10);return m;}
    private static void addBox(MeshModel m,double x0,double x1,double y0,double y1,double z0,double z1){int o=m.vertices.size();double[][] p={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};for(double[] q:p)m.addVertex(new MeshModel.V3(q[0],q[1],q[2]));int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};for(int[] t:f)m.triangles.add(new int[]{o+t[0],o+t[1],o+t[2]});}
}
