package com.mg.structuralai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** v0.9 engineering loads: multiple picked patches, surface-pressure mapping and consistent gravity body load. */
public final class AdvancedFemLoads {
    public static final class Result {
        public int fixedNodes, loadedNodes, pressureTriangles;
        public double patchRadiusM, resultantFx, resultantFy, resultantFz, gravityMassKg;
    }
    private AdvancedFemLoads(){}

    public static Result apply(StaticFemSolver solver,TetMeshData mesh,MeshModel surface,
                               List<MeshModel.V3> supports,List<MeshModel.V3> loads,double unitScale,
                               double fx,double fy,double fz,double pressurePa,boolean gravity,
                               double densityKgM3){
        if(supports==null||supports.isEmpty()) throw new IllegalArgumentException("En az bir mesnet yüzey noktası seçilmeli");
        Result r=new Result();
        double diag=diag(mesh); r.patchRadiusM=Math.max(diag*0.075,1e-9);
        Set<Integer> fixed=new HashSet<>();
        for(MeshModel.V3 p:supports) fixed.addAll(nearOrNearest(mesh,si(p,unitScale),r.patchRadiusM,4));
        if(fixed.size()<3) throw new IllegalStateException("Mesnet patch rijit cisim hareketini kısıtlamak için çok küçük");
        for(int n:fixed) solver.fixNode(n); r.fixedNodes=fixed.size();

        Set<Integer> loaded=new HashSet<>();
        if(loads!=null) for(MeshModel.V3 p:loads) loaded.addAll(nearOrNearest(mesh,si(p,unitScale),r.patchRadiusM,4));
        if((Math.abs(fx)+Math.abs(fy)+Math.abs(fz))>0){
            if(loaded.isEmpty()) throw new IllegalArgumentException("Kuvvet için en az bir yük yüzeyi seçilmeli");
            double inv=1.0/loaded.size(); for(int n:loaded) solver.addNodalForce(n,fx*inv,fy*inv,fz*inv);
            r.resultantFx+=fx;r.resultantFy+=fy;r.resultantFz+=fz;
        }

        if(Math.abs(pressurePa)>0){
            if(loads==null||loads.isEmpty()) throw new IllegalArgumentException("Basınç için yüzey patch seçilmeli");
            applyPressure(solver,mesh,surface,loads,unitScale,r.patchRadiusM,pressurePa,loaded,r);
        }
        r.loadedNodes=loaded.size();

        if(gravity){ applyGravity(solver,mesh,densityKgM3,r); }
        return r;
    }

    private static void applyPressure(StaticFemSolver solver,TetMeshData mesh,MeshModel s,List<MeshModel.V3> picks,
                                      double scale,double radiusM,double pressurePa,Set<Integer> loaded,Result out){
        double radiusModel=radiusM/scale;
        for(int[] t:s.triangles){
            if(t.length<3) continue; MeshModel.V3 a=s.vertices.get(t[0]),b=s.vertices.get(t[1]),c=s.vertices.get(t[2]);
            MeshModel.V3 cent=new MeshModel.V3((a.x+b.x+c.x)/3,(a.y+b.y+c.y)/3,(a.z+b.z+c.z)/3);
            boolean chosen=false; for(MeshModel.V3 p:picks) if(dist(cent,p)<=radiusModel){chosen=true;break;} if(!chosen) continue;
            double ux=b.x-a.x,uy=b.y-a.y,uz=b.z-a.z,vx=c.x-a.x,vy=c.y-a.y,vz=c.z-a.z;
            double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
            double mag=Math.sqrt(nx*nx+ny*ny+nz*nz); if(mag<=1e-20) continue;
            double areaM2=0.5*mag*scale*scale;
            double f=-pressurePa*areaM2; // compressive, opposite outward triangle normal
            double fx=f*nx/mag,fy=f*ny/mag,fz=f*nz/mag;
            List<Integer> ns=nearOrNearest(mesh,si(cent,scale),radiusM*0.65,3); double inv=1.0/ns.size();
            for(int n:ns){solver.addNodalForce(n,fx*inv,fy*inv,fz*inv);loaded.add(n);}
            out.resultantFx+=fx;out.resultantFy+=fy;out.resultantFz+=fz;out.pressureTriangles++;
        }
        if(out.pressureTriangles==0) throw new IllegalStateException("Seçilen patch üzerinde basınç uygulanabilir yüzey üçgeni bulunamadı");
    }

    private static void applyGravity(StaticFemSolver solver,TetMeshData mesh,double rho,Result out){
        double total=0;
        for(int[] t:mesh.tets){
            MeshModel.V3 a=mesh.nodes.get(t[0]),b=mesh.nodes.get(t[1]),c=mesh.nodes.get(t[2]),d=mesh.nodes.get(t[3]);
            double v=Math.abs(volume6(a,b,c,d))/6.0,m=rho*v,totalF=-m*9.80665,share=totalF/4.0;
            for(int n:t) solver.addNodalForce(n,0,0,share); total+=m;
        }
        out.gravityMassKg=total; out.resultantFz+=-total*9.80665;
    }

    private static double volume6(MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c,MeshModel.V3 d){return (b.x-a.x)*((c.y-a.y)*(d.z-a.z)-(c.z-a.z)*(d.y-a.y))-(b.y-a.y)*((c.x-a.x)*(d.z-a.z)-(c.z-a.z)*(d.x-a.x))+(b.z-a.z)*((c.x-a.x)*(d.y-a.y)-(c.y-a.y)*(d.x-a.x));}
    private static MeshModel.V3 si(MeshModel.V3 p,double s){return new MeshModel.V3(p.x*s,p.y*s,p.z*s);}
    private static double diag(TetMeshData m){double x0=1e99,y0=1e99,z0=1e99,x1=-1e99,y1=-1e99,z1=-1e99;for(MeshModel.V3 n:m.nodes){x0=Math.min(x0,n.x);x1=Math.max(x1,n.x);y0=Math.min(y0,n.y);y1=Math.max(y1,n.y);z0=Math.min(z0,n.z);z1=Math.max(z1,n.z);}return Math.sqrt(sq(x1-x0)+sq(y1-y0)+sq(z1-z0));}
    private static List<Integer> nearOrNearest(TetMeshData m,MeshModel.V3 p,double r,int fallback){List<Integer> o=new ArrayList<>();double r2=r*r;for(int i=0;i<m.nodes.size();i++){MeshModel.V3 n=m.nodes.get(i);if(sq(n.x-p.x)+sq(n.y-p.y)+sq(n.z-p.z)<=r2)o.add(i);}if(!o.isEmpty())return o;boolean[] used=new boolean[m.nodes.size()];for(int k=0;k<Math.min(fallback,m.nodes.size());k++){int bi=-1;double bd=1e300;for(int i=0;i<m.nodes.size();i++)if(!used[i]){MeshModel.V3 n=m.nodes.get(i);double d=sq(n.x-p.x)+sq(n.y-p.y)+sq(n.z-p.z);if(d<bd){bd=d;bi=i;}}if(bi>=0){used[bi]=true;o.add(bi);}}return o;}
    private static double dist(MeshModel.V3 a,MeshModel.V3 b){return Math.sqrt(sq(a.x-b.x)+sq(a.y-b.y)+sq(a.z-b.z));}
    private static double sq(double x){return x*x;}
}
