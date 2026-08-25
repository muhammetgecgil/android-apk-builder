package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;

/** Solver-ready volumetric mesh. Coordinates are SI metres. */
public final class TetMeshData {
    public final List<MeshModel.V3> nodes = new ArrayList<>();
    public final List<int[]> tets = new ArrayList<>();

    public int addNode(double x, double y, double z) {
        nodes.add(new MeshModel.V3(x, y, z));
        return nodes.size() - 1;
    }

    public void addTet(int n0, int n1, int n2, int n3) {
        int n = nodes.size();
        int[] e = {n0,n1,n2,n3};
        for (int i : e) if (i < 0 || i >= n) throw new IllegalArgumentException("Tet node index outside mesh");
        tets.add(e);
    }

    public int dofCount() { return nodes.size() * 3; }

    public void validate() {
        if (nodes.size() < 4) throw new IllegalStateException("At least four nodes required");
        if (tets.isEmpty()) throw new IllegalStateException("No tetrahedra in volume mesh");
        for (int[] t : tets) {
            if (t.length != 4) throw new IllegalStateException("Only TET4 supported in this solver stage");
            MeshModel.V3 a=nodes.get(t[0]), b=nodes.get(t[1]), c=nodes.get(t[2]), d=nodes.get(t[3]);
            double v6 = (b.x-a.x)*((c.y-a.y)*(d.z-a.z)-(c.z-a.z)*(d.y-a.y))
                      - (b.y-a.y)*((c.x-a.x)*(d.z-a.z)-(c.z-a.z)*(d.x-a.x))
                      + (b.z-a.z)*((c.x-a.x)*(d.y-a.y)-(c.y-a.y)*(d.x-a.x));
            if (Math.abs(v6) <= 1e-18) throw new IllegalStateException("Degenerate tetrahedron detected");
        }
    }
}
