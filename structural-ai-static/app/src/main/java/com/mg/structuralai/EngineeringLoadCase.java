package com.mg.structuralai;

import java.util.*;

/** Explicit, auditable boundary-condition/load model. No physical quantity is invented. */
public final class EngineeringLoadCase {
    public enum SupportType { FIXED, PINNED, ROLLER_X, ROLLER_Y, ROLLER_Z, SYMMETRY_X, SYMMETRY_Y, SYMMETRY_Z }
    public enum LoadType { NODAL_FORCE, PRESSURE, GRAVITY }

    public static final class Support {
        public final SupportType type;
        public final Set<Integer> nodes;
        public Support(SupportType type,Collection<Integer> nodes){this.type=Objects.requireNonNull(type);this.nodes=Collections.unmodifiableSet(new LinkedHashSet<>(nodes));}
    }
    public static final class Load {
        public final LoadType type;
        public final Set<Integer> nodes;
        public final double x,y,z,pressurePa;
        public Load(LoadType type,Collection<Integer> nodes,double x,double y,double z,double pressurePa){this.type=Objects.requireNonNull(type);this.nodes=Collections.unmodifiableSet(new LinkedHashSet<>(nodes));this.x=x;this.y=y;this.z=z;this.pressurePa=pressurePa;}
        public static Load force(Collection<Integer> nodes,double fx,double fy,double fz){return new Load(LoadType.NODAL_FORCE,nodes,fx,fy,fz,0);}
        public static Load gravity(double gx,double gy,double gz){return new Load(LoadType.GRAVITY,Collections.emptySet(),gx,gy,gz,0);}
    }

    public final String name;
    public final List<Support> supports=new ArrayList<>();
    public final List<Load> loads=new ArrayList<>();
    public EngineeringLoadCase(String name){this.name=(name==null||name.trim().isEmpty())?"Load Case":name.trim();}
    public EngineeringLoadCase addSupport(SupportType t,Collection<Integer> nodes){supports.add(new Support(t,nodes));return this;}
    public EngineeringLoadCase addLoad(Load l){loads.add(l);return this;}

    public Validation validate(TetMeshData mesh,LinearElasticMaterial material){
        if(mesh==null||mesh.nodes.isEmpty())return new Validation(false,"Mesh missing");
        boolean[] constrained=new boolean[mesh.dofCount()];
        int constrainedDofs=0; Set<Integer> supportNodes=new HashSet<>();
        for(Support s:supports){
            if(s.nodes.isEmpty())return new Validation(false,"Support region is empty: "+s.type);
            for(int n:s.nodes){if(n<0||n>=mesh.nodes.size())return new Validation(false,"Support node outside mesh: "+n);supportNodes.add(n);constrainedDofs+=mark(constrained,n,s.type);}
        }
        if(constrainedDofs<3)return new Validation(false,"Insufficient constraints: only "+constrainedDofs+" constrained DOF");
        double fx=0,fy=0,fz=0; int loadedNodes=0; boolean hasPhysicalLoad=false;
        for(Load l:loads){
            if(l.type==LoadType.NODAL_FORCE){if(l.nodes.isEmpty())return new Validation(false,"Force load region is empty");if(!finite(l.x,l.y,l.z))return new Validation(false,"Non-finite force");for(int n:l.nodes){if(n<0||n>=mesh.nodes.size())return new Validation(false,"Load node outside mesh: "+n);if(!supportNodes.contains(n))loadedNodes++;}fx+=l.x;fy+=l.y;fz+=l.z;hasPhysicalLoad|=(Math.abs(l.x)+Math.abs(l.y)+Math.abs(l.z))>0;}
            else if(l.type==LoadType.GRAVITY){if(material==null||!(material.densityKgM3>0))return new Validation(false,"Gravity requires positive material density");if(!finite(l.x,l.y,l.z))return new Validation(false,"Non-finite gravity vector");hasPhysicalLoad|=(Math.abs(l.x)+Math.abs(l.y)+Math.abs(l.z))>0;}
            else if(l.type==LoadType.PRESSURE){if(!(Double.isFinite(l.pressurePa)&&Math.abs(l.pressurePa)>0))return new Validation(false,"Pressure must be finite and non-zero");hasPhysicalLoad=true;}
        }
        if(!hasPhysicalLoad)return new Validation(false,"No explicit physical load defined");
        if(loadedNodes==0 && loads.stream().anyMatch(l->l.type==LoadType.NODAL_FORCE))return new Validation(false,"All force nodes overlap supports");
        return new Validation(true,"BOUNDARY/LOAD CASE PASS | supports="+supports.size()+" | constrainedDOF="+constrainedDofs+" | loads="+loads.size()+" | resultantForce=("+fx+","+fy+","+fz+") N");
    }

    public void apply(StaticFemSolver solver,TetMeshData mesh,LinearElasticMaterial material){
        Validation v=validate(mesh,material); if(!v.pass)throw new IllegalStateException(v.summary);
        for(Support s:supports)for(int n:s.nodes)applySupport(solver,n,s.type);
        for(Load l:loads){
            if(l.type==LoadType.NODAL_FORCE){double inv=1.0/l.nodes.size();for(int n:l.nodes)solver.addNodalForce(n,l.x*inv,l.y*inv,l.z*inv);}
            else if(l.type==LoadType.GRAVITY)applyGravity(solver,mesh,material.densityKgM3,l.x,l.y,l.z);
            else throw new IllegalStateException("Pressure requires face-area evidence; node-only load case cannot apply pressure safely");
        }
    }

    public static final class Validation { public final boolean pass; public final String summary; Validation(boolean p,String s){pass=p;summary=s;} }
    private static boolean finite(double...a){for(double x:a)if(!Double.isFinite(x))return false;return true;}
    private static int mark(boolean[] c,int n,SupportType t){int k=0;for(int d:dofs(n,t))if(!c[d]){c[d]=true;k++;}return k;}
    private static int[] dofs(int n,SupportType t){int x=3*n,y=x+1,z=x+2;switch(t){case FIXED:return new int[]{x,y,z};case PINNED:return new int[]{x,y,z};case ROLLER_X:case SYMMETRY_X:return new int[]{x};case ROLLER_Y:case SYMMETRY_Y:return new int[]{y};case ROLLER_Z:case SYMMETRY_Z:return new int[]{z};default:throw new IllegalStateException();}}
    private static void applySupport(StaticFemSolver s,int n,SupportType t){for(int d:dofs(n,t))s.fixDof(d);}
    private static void applyGravity(StaticFemSolver s,TetMeshData m,double rho,double gx,double gy,double gz){for(int[] t:m.tets){MeshModel.V3 a=m.nodes.get(t[0]),b=m.nodes.get(t[1]),c=m.nodes.get(t[2]),d=m.nodes.get(t[3]);double v=Math.abs((b.x-a.x)*((c.y-a.y)*(d.z-a.z)-(c.z-a.z)*(d.y-a.y))-(b.y-a.y)*((c.x-a.x)*(d.z-a.z)-(c.z-a.z)*(d.x-a.x))+(b.z-a.z)*((c.x-a.x)*(d.y-a.y)-(c.y-a.y)*(d.x-a.x)))/6.0;double mass=rho*v/4.0;for(int n:t)s.addNodalForce(n,mass*gx,mass*gy,mass*gz);}}
}
