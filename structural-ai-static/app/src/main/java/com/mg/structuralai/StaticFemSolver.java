package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;

/** Linear-static 3D FEM pipeline for TET4 meshes. SI units: m, N, Pa. */
public final class StaticFemSolver {
    public static final class Load { public final int dof; public final double valueN; public Load(int dof,double valueN){this.dof=dof;this.valueN=valueN;} }
    public static final class Result {
        public final double[] displacement,reactions,elementVonMisesPa; public final double maxDisplacementM,maxVonMisesPa,forceEquilibriumRelativeError; public final SparsePcgSolver.Result linearSolve; public final int activeFrictionlessContacts,contactIterations;
        Result(double[] u,double[] r,double[] vm,double mu,double mv,double eq,SparsePcgSolver.Result ls,int ac,int it){displacement=u;reactions=r;elementVonMisesPa=vm;maxDisplacementM=mu;maxVonMisesPa=mv;forceEquilibriumRelativeError=eq;linearSolve=ls;activeFrictionlessContacts=ac;contactIterations=it;}
    }
    private static final class Tie { final int a,b; final double factor; Tie(int a,int b,double f){this.a=a;this.b=b;factor=f;} }
    private static final class NormalConstraint {
        final int a,b; final double nx,ny,nz,factor,gapM; final boolean unilateral;
        NormalConstraint(int a,int b,MeshModel.V3 n,double factor,double gapM,boolean unilateral){this.a=a;this.b=b;double q=Math.sqrt(n.x*n.x+n.y*n.y+n.z*n.z);if(q<1e-20)throw new IllegalArgumentException("Contact normal is zero");nx=n.x/q;ny=n.y/q;nz=n.z/q;this.factor=Math.max(10.0,Math.min(1e6,factor));this.gapM=Math.max(0.0,gapM);this.unilateral=unilateral;}
    }
    private final TetMeshData mesh; private final LinearElasticMaterial material; private final boolean[] fixed; private final double[] force; private final List<Tie> bondedTies=new ArrayList<>(); private final List<NormalConstraint> normalConstraints=new ArrayList<>();
    public StaticFemSolver(TetMeshData mesh,LinearElasticMaterial material){mesh.validate();this.mesh=mesh;this.material=material;fixed=new boolean[mesh.dofCount()];force=new double[mesh.dofCount()];}
    public void fixNode(int node){fixDof(3*node);fixDof(3*node+1);fixDof(3*node+2);} public void fixDof(int dof){checkDof(dof);fixed[dof]=true;} public void addNodalForce(int node,double fx,double fy,double fz){addForce(3*node,fx);addForce(3*node+1,fy);addForce(3*node+2,fz);} public void addForce(int dof,double value){checkDof(dof);force[dof]+=value;}
    public void addBondedTie(int a,int b,double f){checkPair(a,b);bondedTies.add(new Tie(a,b,Math.max(10.0,Math.min(1e6,f))));}
    public void addNoSeparationNormal(int a,int b,MeshModel.V3 n,double f,double g){checkPair(a,b);normalConstraints.add(new NormalConstraint(a,b,n,f,g,false));}
    public void addFrictionlessNormal(int a,int b,MeshModel.V3 n,double f,double g){checkPair(a,b);normalConstraints.add(new NormalConstraint(a,b,n,f,g,true));}
    public void addContactConstraints(ContactConstraintSet set){if(set==null)return;for(ContactConstraintSet.Pair p:set.pairs){if(p.kind==ContactConstraintSet.Kind.BONDED_TIE)addBondedTie(p.nodeA,p.nodeB,1e4);else if(p.kind==ContactConstraintSet.Kind.NO_SEPARATION_NORMAL)addNoSeparationNormal(p.nodeA,p.nodeB,p.normal,5e3,p.gapM);else if(p.kind==ContactConstraintSet.Kind.FRICTIONLESS_NORMAL)addFrictionlessNormal(p.nodeA,p.nodeB,p.normal,5e3,p.gapM);}}

    public Result solve(){
        int unilateralCount=0;for(NormalConstraint c:normalConstraints)if(c.unilateral)unilateralCount++;
        boolean[] active=new boolean[unilateralCount]; int ai=0; double charL=characteristicLength();
        double penetrationTol=Math.max(charL*1e-9,1e-12); double openTol=Math.max(charL*1e-13,1e-15);
        for(NormalConstraint c:normalConstraints)if(c.unilateral)active[ai++]=c.gapM<=penetrationTol;
        SparsePcgSolver.Result ls=null;double[] u=null;SparsePcgSolver.Matrix raw=null;int iterations=0;
        for(int contactIt=0;contactIt<Math.max(1,unilateralCount>0?12:1);contactIt++){
            iterations=contactIt+1;raw=assembleGlobal();double diagScale=averageDiagonal(raw);applyBondedTies(raw,diagScale);applyNormalConstraints(raw,diagScale,active);
            double[] rhs=force.clone();SparsePcgSolver.Matrix constrained=raw.copy();for(int i=0;i<mesh.dofCount();i++)if(fixed[i])constrained.applyZeroDirichlet(i,rhs);
            ls=SparsePcgSolver.solve(constrained,rhs,1e-10,Math.max(500,mesh.dofCount()*20));if(!ls.converged)throw new IllegalStateException("PCG did not converge: relResidual="+ls.relativeResidual);u=ls.x;if(unilateralCount==0)break;
            boolean changed=false;int ui=0;for(NormalConstraint c:normalConstraints)if(c.unilateral){double du=relativeNormalDisplacement(c,u),totalGap=c.gapM+du;boolean next=active[ui];if(active[ui]&&du>openTol)next=false;else if(!active[ui]&&totalGap<-penetrationTol)next=true;if(next!=active[ui]){active[ui]=next;changed=true;}ui++;}if(!changed)break;
        }
        if(u==null||raw==null||ls==null)throw new IllegalStateException("Solver produced no state");
        double[] internal=raw.mul(u),reactions=new double[mesh.dofCount()];for(int i=0;i<reactions.length;i++)reactions[i]=internal[i]-force[i];
        double[] vm=new double[mesh.tets.size()];double maxVm=0;for(int e=0;e<mesh.tets.size();e++){int[] t=mesh.tets.get(e);double[] ue=new double[12];for(int a=0;a<4;a++)for(int c=0;c<3;c++)ue[3*a+c]=u[3*t[a]+c];double[] s=Tet4Element.stress(mesh.nodes.get(t[0]),mesh.nodes.get(t[1]),mesh.nodes.get(t[2]),mesh.nodes.get(t[3]),material,ue);vm[e]=Tet4Element.vonMises(s);maxVm=Math.max(maxVm,vm[e]);}
        double maxU=0;for(int n=0;n<mesh.nodes.size();n++){double ux=u[3*n],uy=u[3*n+1],uz=u[3*n+2];maxU=Math.max(maxU,Math.sqrt(ux*ux+uy*uy+uz*uz));}
        double extFx=0,extFy=0,extFz=0,rx=0,ry=0,rz=0;for(int n=0;n<mesh.nodes.size();n++){extFx+=force[3*n];extFy+=force[3*n+1];extFz+=force[3*n+2];if(fixed[3*n]||fixed[3*n+1]||fixed[3*n+2]){rx+=reactions[3*n];ry+=reactions[3*n+1];rz+=reactions[3*n+2];}}
        double imbalance=Math.sqrt((extFx+rx)*(extFx+rx)+(extFy+ry)*(extFy+ry)+(extFz+rz)*(extFz+rz)),applied=Math.max(Math.sqrt(extFx*extFx+extFy*extFy+extFz*extFz),1e-30),eq=imbalance/applied;int activeCount=0;for(boolean a:active)if(a)activeCount++;return new Result(u,reactions,vm,maxU,maxVm,eq,ls,activeCount,iterations);
    }
    private SparsePcgSolver.Matrix assembleGlobal(){SparsePcgSolver.Matrix K=new SparsePcgSolver.Matrix(mesh.dofCount());for(int[] t:mesh.tets){Tet4Element.ElementResult er=Tet4Element.stiffness(mesh.nodes.get(t[0]),mesh.nodes.get(t[1]),mesh.nodes.get(t[2]),mesh.nodes.get(t[3]),material);for(int a=0;a<4;a++)for(int ca=0;ca<3;ca++){int I=3*t[a]+ca,li=3*a+ca;for(int b=0;b<4;b++)for(int cb=0;cb<3;cb++){int J=3*t[b]+cb,lj=3*b+cb;K.add(I,J,er.stiffness[li][lj]);}}}return K;}
    private void applyBondedTies(SparsePcgSolver.Matrix K,double d){for(Tie t:bondedTies){double kp=d*t.factor;for(int c=0;c<3;c++){int ia=3*t.a+c,ib=3*t.b+c;K.add(ia,ia,kp);K.add(ib,ib,kp);K.add(ia,ib,-kp);K.add(ib,ia,-kp);}}}
    private void applyNormalConstraints(SparsePcgSolver.Matrix K,double d,boolean[] active){int ui=0;for(NormalConstraint c:normalConstraints){boolean use=!c.unilateral||(ui<active.length&&active[ui]);if(c.unilateral)ui++;if(!use)continue;double kp=d*c.factor;double[] g={-c.nx,-c.ny,-c.nz,c.nx,c.ny,c.nz};int[] q={3*c.a,3*c.a+1,3*c.a+2,3*c.b,3*c.b+1,3*c.b+2};for(int i=0;i<6;i++)for(int j=0;j<6;j++)K.add(q[i],q[j],kp*g[i]*g[j]);}}
    private double relativeNormalDisplacement(NormalConstraint c,double[] u){return c.nx*(u[3*c.b]-u[3*c.a])+c.ny*(u[3*c.b+1]-u[3*c.a+1])+c.nz*(u[3*c.b+2]-u[3*c.a+2]);}
    private double averageDiagonal(SparsePcgSolver.Matrix K){double s=0;int n=0;for(int i=0;i<K.n;i++){double d=Math.abs(K.get(i,i));if(d>0){s+=d;n++;}}return n>0?s/n:1.0;}
    private double characteristicLength(){double xmin=1e99,ymin=1e99,zmin=1e99,xmax=-1e99,ymax=-1e99,zmax=-1e99;for(MeshModel.V3 p:mesh.nodes){xmin=Math.min(xmin,p.x);ymin=Math.min(ymin,p.y);zmin=Math.min(zmin,p.z);xmax=Math.max(xmax,p.x);ymax=Math.max(ymax,p.y);zmax=Math.max(zmax,p.z);}double dx=xmax-xmin,dy=ymax-ymin,dz=zmax-zmin;return Math.max(Math.sqrt(dx*dx+dy*dy+dz*dz),1e-9);}
    private void checkDof(int d){if(d<0||d>=mesh.dofCount())throw new IllegalArgumentException("DOF outside system");}private void checkPair(int a,int b){if(a<0||a>=mesh.nodes.size()||b<0||b>=mesh.nodes.size()||a==b)throw new IllegalArgumentException("Invalid contact node pair");}
}
