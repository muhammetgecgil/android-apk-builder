package com.mg.structuralai;

import java.util.HashMap;
import java.util.Map;

/** Minimal symmetric sparse matrix + Jacobi-preconditioned conjugate gradient solver. */
public final class SparsePcgSolver {
    public static final class Matrix {
        public final int n;
        @SuppressWarnings("unchecked")
        private final Map<Integer,Double>[] rows;
        public Matrix(int n) {
            this.n=n;
            rows=new Map[n];
            for(int i=0;i<n;i++) rows[i]=new HashMap<>();
        }
        public void add(int r,int c,double v){
            if(Math.abs(v)<1e-30) return;
            rows[r].put(c, rows[r].getOrDefault(c,0.0)+v);
        }
        public double get(int r,int c){ return rows[r].getOrDefault(c,0.0); }
        public void set(int r,int c,double v){ if(Math.abs(v)<1e-30) rows[r].remove(c); else rows[r].put(c,v); }
        public double[] mul(double[] x){
            double[] y=new double[n];
            for(int r=0;r<n;r++) for(Map.Entry<Integer,Double> e:rows[r].entrySet()) y[r]+=e.getValue()*x[e.getKey()];
            return y;
        }
        public void applyZeroDirichlet(int dof, double[] rhs){
            for(int r=0;r<n;r++) if(r!=dof) rows[r].remove(dof);
            rows[dof].clear(); rows[dof].put(dof,1.0); rhs[dof]=0.0;
        }
        /** Sparse structural copy; avoids the former O(n^2) scan when refining mobile meshes. */
        public Matrix copy(){
            Matrix b=new Matrix(n);
            for(int r=0;r<n;r++) b.rows[r].putAll(rows[r]);
            return b;
        }
        public int nonZeros(){ int c=0; for(Map<Integer,Double> r:rows) c+=r.size(); return c; }
    }

    public static final class Result {
        public final double[] x;
        public final int iterations;
        public final double relativeResidual;
        public final boolean converged;
        Result(double[] x,int iterations,double relativeResidual,boolean converged){
            this.x=x; this.iterations=iterations; this.relativeResidual=relativeResidual; this.converged=converged;
        }
    }

    private SparsePcgSolver(){}

    public static Result solve(Matrix A,double[] b,double tol,int maxIter){
        int n=A.n; if(b.length!=n) throw new IllegalArgumentException("RHS size mismatch");
        double[] x=new double[n], r=b.clone(), z=new double[n], p=new double[n];
        double normB=Math.max(norm(b),1e-30);
        for(int i=0;i<n;i++){
            double d=A.get(i,i);
            if(Math.abs(d)<1e-30) throw new IllegalStateException("Zero diagonal / rigid body mode candidate at DOF "+i);
            z[i]=r[i]/d; p[i]=z[i];
        }
        double rz=dot(r,z);
        double rel=norm(r)/normB;
        if(rel<=tol) return new Result(x,0,rel,true);
        for(int it=1;it<=maxIter;it++){
            double[] Ap=A.mul(p);
            double den=dot(p,Ap);
            if(!(den>0.0)) throw new IllegalStateException("Matrix is not positive definite; check constraints/material/mesh");
            double alpha=rz/den;
            for(int i=0;i<n;i++){ x[i]+=alpha*p[i]; r[i]-=alpha*Ap[i]; }
            rel=norm(r)/normB;
            if(rel<=tol) return new Result(x,it,rel,true);
            for(int i=0;i<n;i++) z[i]=r[i]/A.get(i,i);
            double rzNew=dot(r,z), beta=rzNew/rz;
            for(int i=0;i<n;i++) p[i]=z[i]+beta*p[i];
            rz=rzNew;
        }
        return new Result(x,maxIter,rel,false);
    }

    private static double dot(double[] a,double[] b){ double s=0; for(int i=0;i<a.length;i++) s+=a[i]*b[i]; return s; }
    private static double norm(double[] a){ return Math.sqrt(Math.max(dot(a,a),0.0)); }
}
