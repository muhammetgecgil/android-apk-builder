package com.mg.structuralai;

/** First real FEM kernel: constant-strain 4-node tetrahedron, 3 DOF/node. */
public final class Tet4Element {
    private Tet4Element() {}

    public static final class ElementResult {
        public final double volume;
        public final double[][] stiffness; // 12x12
        public ElementResult(double volume, double[][] stiffness) {
            this.volume = volume;
            this.stiffness = stiffness;
        }
    }

    public static ElementResult stiffness(MeshModel.V3 a, MeshModel.V3 b, MeshModel.V3 c, MeshModel.V3 d,
                                          LinearElasticMaterial material) {
        double[][] m = {
            {1,a.x,a.y,a.z}, {1,b.x,b.y,b.z}, {1,c.x,c.y,c.z}, {1,d.x,d.y,d.z}
        };
        double det = det4(m);
        double volume = Math.abs(det) / 6.0;
        if (volume <= 1e-18) throw new IllegalArgumentException("Degenerate tetrahedron");

        double[][] inv = invert4(m);
        // Ni = ai + bi*x + ci*y + di*z. Coefficients are columns of inv(M).
        double[][] B = new double[6][12];
        for (int i=0;i<4;i++) {
            double bx = inv[1][i], by = inv[2][i], bz = inv[3][i];
            int k = 3*i;
            B[0][k] = bx;
            B[1][k+1] = by;
            B[2][k+2] = bz;
            B[3][k] = by;   B[3][k+1] = bx;
            B[4][k+1] = bz; B[4][k+2] = by;
            B[5][k] = bz;   B[5][k+2] = bx;
        }

        double[][] D = material.elasticity3D();
        double[][] DB = mul(D, B);
        double[][] BT = transpose(B);
        double[][] K = mul(BT, DB);
        for (int i=0;i<12;i++) for (int j=0;j<12;j++) K[i][j] *= volume;
        return new ElementResult(volume, K);
    }

    public static double[] stress(MeshModel.V3 a, MeshModel.V3 b, MeshModel.V3 c, MeshModel.V3 d,
                                  LinearElasticMaterial material, double[] u12) {
        if (u12.length != 12) throw new IllegalArgumentException("TET4 displacement vector must have 12 entries");
        double[][] m = {
            {1,a.x,a.y,a.z}, {1,b.x,b.y,b.z}, {1,c.x,c.y,c.z}, {1,d.x,d.y,d.z}
        };
        double[][] inv = invert4(m);
        double[][] B = new double[6][12];
        for (int i=0;i<4;i++) {
            double bx=inv[1][i], by=inv[2][i], bz=inv[3][i]; int k=3*i;
            B[0][k]=bx; B[1][k+1]=by; B[2][k+2]=bz;
            B[3][k]=by; B[3][k+1]=bx;
            B[4][k+1]=bz; B[4][k+2]=by;
            B[5][k]=bz; B[5][k+2]=bx;
        }
        double[] strain = mul(B, u12);
        return mul(material.elasticity3D(), strain);
    }

    public static double vonMises(double[] s) {
        double sx=s[0], sy=s[1], sz=s[2], txy=s[3], tyz=s[4], txz=s[5];
        return Math.sqrt(0.5*((sx-sy)*(sx-sy)+(sy-sz)*(sy-sz)+(sz-sx)*(sz-sx))
            + 3.0*(txy*txy+tyz*tyz+txz*txz));
    }

    private static double[][] transpose(double[][] a){
        double[][] t=new double[a[0].length][a.length];
        for(int i=0;i<a.length;i++) for(int j=0;j<a[0].length;j++) t[j][i]=a[i][j];
        return t;
    }
    private static double[][] mul(double[][] a,double[][] b){
        double[][] r=new double[a.length][b[0].length];
        for(int i=0;i<a.length;i++) for(int k=0;k<b.length;k++) for(int j=0;j<b[0].length;j++) r[i][j]+=a[i][k]*b[k][j];
        return r;
    }
    private static double[] mul(double[][] a,double[] x){
        double[] r=new double[a.length];
        for(int i=0;i<a.length;i++) for(int j=0;j<x.length;j++) r[i]+=a[i][j]*x[j];
        return r;
    }
    private static double det4(double[][] a){
        double[][] m=copy(a); double det=1.0;
        for(int i=0;i<4;i++){
            int p=i; for(int r=i+1;r<4;r++) if(Math.abs(m[r][i])>Math.abs(m[p][i])) p=r;
            if(Math.abs(m[p][i])<1e-30) return 0.0;
            if(p!=i){ double[] tmp=m[i];m[i]=m[p];m[p]=tmp;det=-det; }
            double piv=m[i][i]; det*=piv;
            for(int r=i+1;r<4;r++){ double f=m[r][i]/piv; for(int c=i+1;c<4;c++) m[r][c]-=f*m[i][c]; }
        }
        return det;
    }
    private static double[][] invert4(double[][] a){
        double[][] aug=new double[4][8];
        for(int i=0;i<4;i++){ for(int j=0;j<4;j++) aug[i][j]=a[i][j]; aug[i][4+i]=1.0; }
        for(int i=0;i<4;i++){
            int p=i; for(int r=i+1;r<4;r++) if(Math.abs(aug[r][i])>Math.abs(aug[p][i])) p=r;
            if(Math.abs(aug[p][i])<1e-30) throw new IllegalArgumentException("Singular tetra geometry matrix");
            if(p!=i){ double[] tmp=aug[i];aug[i]=aug[p];aug[p]=tmp; }
            double piv=aug[i][i]; for(int c=0;c<8;c++) aug[i][c]/=piv;
            for(int r=0;r<4;r++) if(r!=i){ double f=aug[r][i]; for(int c=0;c<8;c++) aug[r][c]-=f*aug[i][c]; }
        }
        double[][] inv=new double[4][4];
        for(int i=0;i<4;i++) for(int j=0;j<4;j++) inv[i][j]=aug[i][4+j];
        return inv;
    }
    private static double[][] copy(double[][] a){
        double[][] b=new double[a.length][a[0].length];
        for(int i=0;i<a.length;i++) System.arraycopy(a[i],0,b[i],0,a[i].length);
        return b;
    }
}
