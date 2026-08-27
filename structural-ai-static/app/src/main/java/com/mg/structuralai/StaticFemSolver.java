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
        Result(double[] u,double[] reactions,double[] vm,double maxU,double maxVm,double eq,SparsePcgSolver.Result ls){
            this.displacement=u; this.reactions=reactions; this.elementVonMisesPa=vm;
            this.maxDisplacementM=maxU; this.maxVonMisesPa=maxVm; this.forceEquilibriumRelativeError=eq; this.linearSolve=ls;
        }
    }

    private static final class Tie { final int a,b; final double factor; Tie(int a,int b,double f){this.a=a;this.b=b;factor=f;} }
    private static final class NormalTie {
        final int a,b; final double nx,ny,nz,factor;
        NormalTie(int a,int b,MeshModel.V3 n,double f){this.a=a;this.b=b;double q=Math.sqrt(n.x*n.x+n.y*n.y+n.z*n.z);if(q<=1e-30)throw new IllegalArgumentException("Contact normal is zero");nx=n.x/q;ny=n.y/q;nz=n.z/q;factor=f;}
    }
    private final TetMeshData mesh;
    private final LinearElasticMaterial material;
    private final boolean[] fixed;
    private final double[] force;
    private final List<Tie> bondedTies=new ArrayList<>();
    private final List<NormalTie> normalTies=new ArrayList<>();

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
        validatePair(nodeA,nodeB); bondedTies.add(new Tie(nodeA,nodeB,clampFactor(factor)));
    }

    /** Bilateral normal-only constraint: n dot (uB-uA)=0; tangential relative motion remains free. */
    public void addNoSeparationNormal(int nodeA,int nodeB,MeshModel.V3 normal,double factor){
        validatePair(nodeA,nodeB); normalTies.add(new NormalTie(nodeA,nodeB,normal,clampFactor(factor)));
    }

    public void addContactConstraints(ContactConstraintSet set){
        if(set==null)return;
        for(ContactConstraintSet.Pair p:set.pairs){
            if(p.kind==ContactConstraintSet.Kind.BONDED_TIE)addBondedTie(p.nodeA,p.nodeB,1e4);
            else if(p.kind==ContactConstraintSet.Kind.NO_SEPARATION_NORMAL)addNoSeparationNormal(p.nodeA,p.nodeB,p.normal,1e4);
            // FRICTIONLESS_NORMAL is unilateral and intentionally handled by the active-set solver, not by this bilateral path.
        }
    }

    public Result solve(){
        int ndof=mesh.dofCount();
        SparsePcgSolver.Matrix raw=assembleGlobal();
        double diagScale=meanPositiveDiagonal(raw);
        applyBondedTies(raw,diagScale);
        applyNormalTies(raw,diagScale);
        double[] rhs=force.clone();
        SparsePcgSolver.Matrix constrained=copy(raw);
        for(int i=0;i<ndof;i++) if(fixed[i]) constrained.applyZeroDirichlet(i,rhs);

        SparsePcgSolver.Result ls=SparsePcgSolver.solve(constrained,rhs,1e-10,Math.max(500,ndof*20));
        if(!ls.converged) throw new IllegalStateException("PCG did not converge: relResidual="+ls.relativeResidual);
        double[] u=ls.x;

        double[] internal=raw.mul(u);
        double[] reactions=new double[ndof];
        for(int i=0;i<ndof;i++) reactions[i]=internal[i]-force[i];

        double[] vm=new double[mesh.tets.size()]; double maxVm=0;
        for(int e=0;e<mesh.tets.size();e++){
            int[] t=mesh.tets.get(e); double[] ue=new double[12];
            for(int a=0;a<4;a++) for(int c=0;c<3;c++) ue[3*a+c]=u[3*t[a]+c];
            double[] s=Tet4Element.stress(mesh.nodes.get(t[0]),mesh.nodes.get(t[1]),mesh.nodes.get(t[2]),mesh.nodes.get(t[3]),material,ue);
            vm[e]=Tet4Element.vonMises(s); maxVm=Math.max(maxVm,vm[e]);
        }
        double maxU=0;
        for(int n=0;n<mesh.nodes.size();n++){
            double ux=u[3*n],uy=u[3*n+1],uz=u[3*n+2]; maxU=Math.max(maxU,Math.sqrt(ux*ux+uy*uy+uz*uz));
        }

        double extFx=0,extFy=0,extFz=0,rx=0,ry=0,rz=0;
        for(int n=0;n<mesh.nodes.size();n++){
            extFx+=force[3*n]; extFy+=force[3*n+1]; extFz+=force[3*n+2];
            if(fixed[3*n]||fixed[3*n+1]||fixed[3*n+2]){rx+=reactions[3*n]; ry+=reactions[3*n+1]; rz+=reactions[3*n+2];}
        }
        double imbalance=Math.sqrt((extFx+rx)*(extFx+rx)+(extFy+ry)*(extFy+ry)+(extFz+rz)*(extFz+rz));
        double applied=Math.max(Math.sqrt(extFx*extFx+extFy*extFy+extFz*extFz),1e-30);
        return new Result(u,reactions,vm,maxU,maxVm,imbalance/applied,ls);
    }

    private SparsePcgSolver.Matrix assembleGlobal(){
        int ndof=mesh.dofCount(); SparsePcgSolver.Matrix K=new SparsePcgSolver.Matrix(ndof);
        for(int[] t:mesh.tets){
            Tet4Element.ElementResult er=Tet4Element.stiffness(mesh.nodes.get(t[0]),mesh.nodes.get(t[1]),mesh.nodes.get(t[2]),mesh.nodes.get(t[3]),material);
            for(int a=0;a<4;a++) for(int ca=0;ca<3;ca++){
                int I=3*t[a]+ca, li=3*a+ca;
                for(int b=0;b<4;b++) for(int cb=0;cb<3;cb++){int J=3*t[b]+cb, lj=3*b+cb;K.add(I,J,er.stiffness[li][lj]);}
            }
        }
        return K;
    }

    private void applyBondedTies(SparsePcgSolver.Matrix K,double diagScale){
        for(Tie t:bondedTies){double kp=diagScale*t.factor;for(int c=0;c<3;c++){int ia=3*t.a+c,ib=3*t.b+c;K.add(ia,ia,kp);K.add(ib,ib,kp);K.add(ia,ib,-kp);K.add(ib,ia,-kp);}}
    }

    /** Adds kp * (n dot (uB-uA))^2 / 2. This constrains only the normal relative displacement. */
    private void applyNormalTies(SparsePcgSolver.Matrix K,double diagScale){
        for(NormalTie t:normalTies){
            double kp=diagScale*t.factor; double[] n={t.nx,t.ny,t.nz};
            for(int i=0;i<3;i++)for(int j=0;j<3;j++){
                double v=kp*n[i]*n[j]; int ai=3*t.a+i,aj=3*t.a+j,bi=3*t.b+i,bj=3*t.b+j;
                K.add(ai,aj,v); K.add(bi,bj,v); K.add(ai,bj,-v); K.add(bi,aj,-v);
            }
        }
    }

    private static double meanPositiveDiagonal(SparsePcgSolver.Matrix K){double s=0;int n=0;for(int i=0;i<K.n;i++){double d=Math.abs(K.get(i,i));if(d>0){s+=d;n++;}}return n>0?s/n:1.0;}
    private static double clampFactor(double f){return Math.max(10.0,Math.min(1e6,f));}
    private void validatePair(int a,int b){if(a<0||a>=mesh.nodes.size()||b<0||b>=mesh.nodes.size()||a==b)throw new IllegalArgumentException("Invalid contact node pair");}
    private static SparsePcgSolver.Matrix copy(SparsePcgSolver.Matrix a){SparsePcgSolver.Matrix b=new SparsePcgSolver.Matrix(a.n);for(int i=0;i<a.n;i++)for(int j=0;j<a.n;j++){double v=a.get(i,j);if(Math.abs(v)>0)b.set(i,j,v);}return b;}
    private void checkDof(int d){if(d<0||d>=mesh.dofCount())throw new IllegalArgumentException("DOF outside system");}
}
