package com.mg.structuralai;

import java.util.Arrays;

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
        double[][] B = strainDisplacement(a,b,c,d);
        double[][] m = {
            {1,a.x,a.y,a.z}, {1,b.x,b.y,b.z}, {1,c.x,c.y,c.z}, {1,d.x,d.y,d.z}
        };
        double volume = Math.abs(det4(m)) / 6.0;
        if (volume <= 1e-18) throw new IllegalArgumentException("Degenerate tetrahedron");
        double[][] D = material.elasticity3D();
        double[][] DB = mul(D, B);
        double[][] BT = transpose(B);
        double[][] K = mul(BT, DB);
        for (int i=0;i<12;i++) for (int j=0;j<12;j++) K[i][j] *= volume;
        return new ElementResult(volume, K);
    }

    /** Engineering strain vector: ex, ey, ez, gamma_xy, gamma_yz, gamma_xz. */
    public static double[] strain(MeshModel.V3 a, MeshModel.V3 b, MeshModel.V3 c, MeshModel.V3 d,double[] u12) {
        if (u12.length != 12) throw new IllegalArgumentException("TET4 displacement vector must have 12 entries");
        return mul(strainDisplacement(a,b,c,d),u12);
    }

    public static double[] stress(MeshModel.V3 a, MeshModel.V3 b, MeshModel.V3 c, MeshModel.V3 d,
                                  LinearElasticMaterial material, double[] u12) {
        return mul(material.elasticity3D(), strain(a,b,c,d,u12));
    }

    public static double vonMises(double[] s) {
        double sx=s[0], sy=s[1], sz=s[2], txy=s[3], tyz=s[4], txz=s[5];
        return Math.sqrt(0.5*((sx-sy)*(sx-sy)+(sy-sz)*(sy-sz)+(sz-sx)*(sz-sx))
            + 3.0*(txy*txy+tyz*tyz+txz*txz));
    }

    /** Principal values of a symmetric tensor stored as xx,yy,zz,xy,yz,xz. For engineering strain input, shear terms are halved first. */
    public static double[] principalValues(double[] v, boolean engineeringStrain) {
        if(v==null||v.length<6)throw new IllegalArgumentException("Tensor requires 6 components");
        double xy=v[3]*(engineeringStrain?0.5:1.0), yz=v[4]*(engineeringStrain?0.5:1.0), xz=v[5]*(engineeringStrain?0.5:1.0);
        double[][] a={{v[0],xy,xz},{xy,v[1],yz},{xz,yz,v[2]}};
        // Jacobi eigen-solver for real symmetric 3x3 tensor.
        for(int it=0;it<24;it++){
            int p=0,q=1;double m=Math.abs(a[0][1]);
            if(Math.abs(a[0][2])>m){m=Math.abs(a[0][2]);p=0;q=2;}
            if(Math.abs(a[1][2])>m){m=Math.abs(a[1][2]);p=1;q=2;}
            if(m<1e-14*Math.max(1.0,Math.max(Math.abs(a[0][0]),Math.max(Math.abs(a[1][1]),Math.abs(a[2][2])))))break;
            double phi=0.5*Math.atan2(2*a[p][q],a[q][q]-a[p][p]),c=Math.cos(phi),s=Math.sin(phi);
            double app=c*c*a[p][p]-2*s*c*a[p][q]+s*s*a[q][q];
            double aqq=s*s*a[p][p]+2*s*c*a[p][q]+c*c*a[q][q];
            for(int r=0;r<3;r++)if(r!=p&&r!=q){double arp=a[r][p],arq=a[r][q];a[r][p]=a[p][r]=c*arp-s*arq;a[r][q]=a[q][r]=s*arp+c*arq;}
            a[p][p]=app;a[q][q]=aqq;a[p][q]=a[q][p]=0;
        }
        double[] out={a[0][0],a[1][1],a[2][2]};Arrays.sort(out);
        return new double[]{out[2],out[1],out[0]};
    }

    private static double[][] strainDisplacement(MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c,MeshModel.V3 d){
        double[][] m={{1,a.x,a.y,a.z},{1,b.x,b.y,b.z},{1,c.x,c.y,c.z},{1,d.x,d.y,d.z}};
        double[][] inv=invert4(m);double[][] B=new double[6][12];
        for(int i=0;i<4;i++){
            double bx=inv[1][i],by=inv[2][i],bz=inv[3][i];int k=3*i;
            B[0][k]=bx;B[1][k+1]=by;B[2][k+2]=bz;
            B[3][k]=by;B[3][k+1]=bx;
            B[4][k+1]=bz;B[4][k+2]=by;
            B[5][k]=bz;B[5][k+2]=bx;
        }
        return B;
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
