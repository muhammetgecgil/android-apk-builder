package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;

public final class MeshModel {
    public static final class V3 {
        public final double x, y, z;
        public V3(double x, double y, double z) { this.x=x; this.y=y; this.z=z; }
    }

    public final List<V3> vertices = new ArrayList<>();
    public final List<int[]> triangles = new ArrayList<>();
    public double minX=Double.POSITIVE_INFINITY,minY=Double.POSITIVE_INFINITY,minZ=Double.POSITIVE_INFINITY;
    public double maxX=Double.NEGATIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY,maxZ=Double.NEGATIVE_INFINITY;

    public void addVertex(V3 v){
        vertices.add(v);
        minX=Math.min(minX,v.x); minY=Math.min(minY,v.y); minZ=Math.min(minZ,v.z);
        maxX=Math.max(maxX,v.x); maxY=Math.max(maxY,v.y); maxZ=Math.max(maxZ,v.z);
    }

    public double dx(){ return Math.max(1e-9,maxX-minX); }
    public double dy(){ return Math.max(1e-9,maxY-minY); }
    public double dz(){ return Math.max(1e-9,maxZ-minZ); }
    public double diagonal(){ return Math.sqrt(dx()*dx()+dy()*dy()+dz()*dz()); }
}
