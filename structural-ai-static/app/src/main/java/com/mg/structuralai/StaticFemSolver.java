package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;

/** Linear-static 3D FEM pipeline for TET4 meshes. SI units: m, N, Pa. */
public final class StaticFemSolver {
    public static final class Load {
        public final int dof; public final double valueN;
        public Load(int dof,double valueN){ this.dof=dof; this.valueN=valueN; }
    }
    public static final class Result {
        public final double[] displacement;
        public final double[] reactions;
        public final double[] elementVonMisesPa;
        public final double maxDisplacementM;
        public final double maxVonMisesPa;
        public final double forceEquilibriumRelativeError;
        public final SparsePcgSolver.Result linearSolve;
        public final int activeFrictionlessContacts;
        public final int contactIterations;
        Result(double[] u,double[] reactions,double[] vm,double maxU,double maxVm,double eq,SparsePcgSolver.Result ls,int active,int it){
            this.displacement=u; this.reactions=reactions; this.elementVonMisesPa=vm;
            this.maxDisplacementM=maxU; this.maxVonMisesPa=maxVm; this.forceEquilibriumRelativeError=eq; this.linearSolve=ls;
            this.activeFrictionlessContacts=active; this.contactIterations=it;
        }
    }

    private static final class Tie { final int a,b; final double factor; Tie(int a,int b,double f){this.a=a;this.b=b;factor=f;} }
    private static final class NormalConstraint {
        final int a,b; final double nx,ny,nz,factor,gapM; final boolean unilateral;
        NormalConstraint(int a,int b,MeshModel.V3 n,double factor,double gapM,boolean unilateral){
            this.a=a;this.b=b;double q=Math.sqrt(n.x*n.x+n.y*n.y+n.z*n.z);if(q<1e-20)throw new IllegalArgumentException("Contact normal is zero");
            nx=n.x/q;ny=n.y/q;nz=n.z/q;this.factor=Math.max(10.0,Math.min(1e6,factor));this.gapM=Math.max(0.0,gapM);this.unilateral=unilateral;
        }
    }

    private final TetMeshData mesh;
    private final LinearElasticMaterial material;
    private final boolean[] fixed;
    private final double[] force;
    private final List<Tie> bondedTies=new ArrayList<>();
    private final List<NormalConstraint> normalConstraints=new ArrayList<>();

    public StaticFemSolver(TetMeshData mesh, LinearElasticMaterial material){
        mesh.validate(); this.mesh=mesh; this.material=material;
        fixed=new boolean[mesh.dofCount()]; force=new double[mesh.dofCount()];
    }

    public void fixNode(int node){ fixDof(3*node); fixDof(3*node+1); fixDof(3*node+2); }
    public void fixDof(int dof){ checkDof(dof); fixed[dof]=true; }
    public void addNodalForce(int node,double fx,double fy,double fz){ addForce(3*node,fx); addForce(3*node+1,fy); addForce(3*node+2,fz); }
    public void addForce(int dof,double value){ checkDof(dof); force[dof]+=value; }

    /** Adds uA=uB in X/Y/Z through a symmetric penalty tie. */
    public void addBondedTie(int nodeA,int nodeB,double factor){
        checkPair(nodeA,nodeB); bondedTies.add(new Tie(nodeA,nodeB,Math.max(10.0,Math.min(1e6,factor))));
    }
    /** Bilateral normal constraint: no separation and no penetration, tangential motion remains free. */
    public void addNoSeparationNormal(int nodeA,int nodeB,MeshModel.V3 normal,double factor,double gapM){
        checkPair(nodeA,nodeB); normalConstraints.add(new NormalConstraint(nodeA,nodeB,normal,factor,gapM,false));
    }
    /** Unilateral frictionless contact: compression can be carried, opening is released by active-set iteration. */
    public void addFrictionlessNormal(int nodeA,int nodeB,MeshModel.V3 normal,double factor,double gapM){
        checkPair(nodeA,nodeB); normalConstraints.add(new NormalConstraint(nodeA,nodeB,normal,factor,gapM,true));
    }
    public void addContactConstraints(ContactConstraintSet set){
        if(set==null)return;
        for(ContactConstraintSet.Pair p:set.pairs){
            if(p.kind==ContactConstraintSet.Kind.BONDED_TIE)addBondedTie(p.nodeA,p.nodeB,1e4);
            else if(p.kind==ContactConstraintSet.Kind.NO_SEPARATION_NORMAL)addNoSeparationNormal(p.nodeA,p.nodeB,p.normal,5e3,p.gapM);
            else if(p.kind==ContactConstraintSet.Kind.FRICTIONLESS_NORMAL)addFrictionlessNormal(p.nodeA,p.nodeB,p.normal,5e3,p.gapM);
        }
    }

    public Result solve(){
        int unilateralCount=0;for(NormalConstraint c:normalConstraints)if(c.unilateral)unilateralCount++;
        boolean[] active=new boolean[unilateralCount];for(int i=0;i<active.length;i++)active[i]=true;
        double charL=characteristicLength(); double openTol=Math.max(charL*1e-9,1e-12), penetrationTol=Math.max(charL*1e-9,1e-12);
        SparsePcgSolver.Result ls=null;double[] u=null;SparsePcgSolver.Matrix raw=null;int iterations=0;
        for(int contactIt=0;contactIt<Math.max(1,unilateralCount>0?10:1);contactIt++){
            iterations=contactIt+1;raw=assembleGlobal();double diagScale=averageDiagonal(raw);applyBondedTies(raw,diagScale);applyNormalConstraints(raw,diagScale,active);
            double[] rhs=force.clone();SparsePcgSolver.Matrix constrained=copy(raw);for(int i=0;i<mesh.dofCount();i++)if(fixed[i])constrained.applyZeroDirichlet(i,rhs);
            ls=SparsePcgSolver.solve(constrained,rhs,1e-10,Math.max(500,mesh.dofCount()*20));
            if(!ls.converged)throw new IllegalStateException("PCG did not converge: relResidual="+ls.relativeResidual);
            u=ls.x;if(unilateralCount==0)break;
            boolean changed=false;int ui=0;
            for(NormalConstraint c:normalConstraints)if(c.unilateral){
                double du=relativeNormalDisplacement(c,u);double totalGap=c.gapM+du;boolean next=active[ui];
                if(active[ui]&&du>openTol)next=false;
                else if(!active[ui]&&totalGap<-penetrationTol)next=true;
                if(next!=active[ui]){active[ui]=next;changed=true;}ui++;
            }
            if(!changed)break;
        }
        if(u==null||raw==null||ls==null)throw new IllegalStateException("Solver produced no state");

        double[] internal=raw.mul(u),reactions=new double[mesh.dofCount()];for(int i=0;i<reactions.length;i++)reactions[i]=internal[i]-force[i];
        double[] vm=new double[mesh.tets.size()];double maxVm=0;
        for(int e=0;e<mesh.tets.size();e++){int[] t=mesh.tets.get(e);double[] ue=new double[12];for(int a=0;a<4;a++)for(int c=0;c<3;c++)ue[3*a+c]=u[3*t[a]+c];double[] s=Tet4Element.stress(mesh.nodes.get(t[0]),mesh.nodes.get(t[1]),mesh.nodes.get(t[2]),mesh.nodes.get(t[3]),material,ue);vm[e]=Tet4Element.vonMises(s);maxVm=Math.max(maxVm,vm[e]);}
        double maxU=0;for(int n=0;n<mesh.nodes.size();n++){double ux=u[3*n],uy=u[3*n+1],uz=u[3*n+2];maxU=Math.max(maxU,Math.sqrt(ux*ux+uy*uy+uz*uz));}
        double extFx=0,extFy=0,extFz=0,rx=0,ry=0,rz=0;for(int n=0;n<mesh.nodes.size();n++){extFx+=force[3*n];extFy+=force[3*n+1];extFz+=force[3*n+2];if(fixed[3*n]||fixed[3*n+1]||fixed[3*n+2]){rx+=reactions[3*n];ry+=reactions[3*n+1];rz+=reactions[3*n+2];}}
        double imbalance=Math.sqrt((extFx+rx)*(extFx+rx)+(extFy+ry)*(extFy+ry)+(extFz+rz)*(extFz+rz));double applied=Math.max(Math.sqrt(extFx*extFx+extFy*extFy+extFz*extFz),1e-30);double eq=imbalance/applied;
        int activeCount=0;for(boolean a:active)if(a)activeCount++;
        return new Result(u,reactions,vm,maxU,maxVm,eq,ls,activeCount,iterations);
    }

    private SparsePcgSolver.Matrix assembleGlobal(){
        SparsePcgSolver.Matrix K=new SparsePcgSolver.Matrix(mesh.dofCount());
        for(int[] t:mesh.tets){Tet4Element.ElementResult er=Tet4Element.stiffness(mesh.nodes.get(t[0]),mesh.nodes.get(t[1]),mesh.nodes.get(t[2]),mesh.nodes.get(t[3]),material);for(int a=0;a<4;a++)for(int ca=0;ca<3;ca++){int I=3*t[a]+ca,li=3*a+ca;for(int b=0;b<4;b++)for(int cb=0;cb<3;cb++){int J=3*t[b]+cb,lj=3*b+cb;K.add(I,J,er.stiffness[li][lj]);}}}return K;
    }
    private void applyBondedTies(SparsePcgSolver.Matrix K,double diagScale){for(Tie t:bondedTies){double kp=diagScale*t.factor;for(int c=0;c<3;c++){int ia=3*t.a+c,ib=3*t.b+c;K.add(ia,ia,kp);K.add(ib,ib,kp);K.add(ia,ib,-kp);K.add(ib,ia,-kp);}}}
    private void applyNormalConstraints(SparsePcgSolver.Matrix K,double diagScale,boolean[] active){int ui=0;for(NormalConstraint c:normalConstraints){boolean use=!c.unilateral||(ui<active.length&&active[ui]);if(c.unilateral)ui++;if(!use)continue;double kp=diagScale*c.factor;double[] g={-c.nx,-c.ny,-c.nz,c.nx,c.ny,c.nz};int[] d={3*c.a,3*c.a+1,3*c.a+2,3*c.b,3*c.b+1,3*c.b+2};for(int i=0;i<6;i++)for(int j=0;j<6;j++)K.add(d[i],d[j],kp*g[i]*g[j]);}}
    private double relativeNormalDisplacement(NormalConstraint c,double[] u){double ax=u[3*c.a],ay=u[3*c.a+1],az=u[3*c.a+2],bx=u[3*c.b],by=u[3*c.b+1],bz=u[3*c.b+2];return c.nx*(bx-ax)+c.ny*(by-ay)+c.nz*(bz-az);}
    private double averageDiagonal(SparsePcgSolver.Matrix K){double s=0;int n=0;for(int i=0;i<K.n;i++){double d=Math.abs(K.get(i,i));if(d>0){s+=d;n++;}}return n>0?s/n:1.0;}
    private double characteristicLength(){double xmin=Double.POSITIVE_INFINITY,ymin=Double.POSITIVE_INFINITY,zmin=Double.POSITIVE_INFINITY,xmax=Double.NEGATIVE_INFINITY,ymax=Double.NEGATIVE_INFINITY,zmax=Double.NEGATIVE_INFINITY;for(MeshModel.V3 p:mesh.nodes){xmin=Math.min(xmin,p.x);ymin=Math.min(ymin,p.y);zmin=Math.min(zmin,p.z);xmax=Math.max(xmax,p.x);ymax=Math.max(ymax,p.y);zmax=Math.max(zmax,p.z);}double dx=xmax-xmin,dy=ymax-ymin,dz=zmax-zmin;return Math.max(Math.sqrt(dx*dx+dy*dy+dz*dz),1e-9);}
    private static SparsePcgSolver.Matrix copy(SparsePcgSolver.Matrix a){SparsePcgSolver.Matrix b=new SparsePcgSolver.Matrix(a.n);for(int i=0;i<a.n;i++)for(int j=0;j<a.n;j++){double v=a.get(i,j);if(Math.abs(v)>0)b.set(i,j,v);}return b;}
    private void checkDof(int d){if(d<0||d>=mesh.dofCount())throw new IllegalArgumentException("DOF outside system");}
    private void checkPair(int a,int b){if(a<0||a>=mesh.nodes.size()||b<0||b>=mesh.nodes.size()||a==b)throw new IllegalArgumentException("Invalid contact node pair");}
}
