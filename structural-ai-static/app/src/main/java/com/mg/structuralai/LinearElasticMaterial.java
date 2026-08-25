package com.mg.structuralai;

public final class LinearElasticMaterial {
    public final String name;
    public final double youngPa;
    public final double poisson;
    public final double densityKgM3;
    public final double yieldPa;

    public LinearElasticMaterial(String name, double youngPa, double poisson, double densityKgM3, double yieldPa) {
        if (youngPa <= 0.0) throw new IllegalArgumentException("E must be positive");
        if (poisson <= -1.0 || poisson >= 0.5) throw new IllegalArgumentException("Poisson ratio outside stable isotropic range");
        if (densityKgM3 <= 0.0) throw new IllegalArgumentException("Density must be positive");
        this.name = name;
        this.youngPa = youngPa;
        this.poisson = poisson;
        this.densityKgM3 = densityKgM3;
        this.yieldPa = yieldPa;
    }

    public double[][] elasticity3D() {
        double e = youngPa, nu = poisson;
        double c = e / ((1.0 + nu) * (1.0 - 2.0 * nu));
        double l = nu * c;
        double g2 = (1.0 - 2.0 * nu) * c / 2.0;
        double a = (1.0 - nu) * c;
        return new double[][]{
            {a,l,l,0,0,0}, {l,a,l,0,0,0}, {l,l,a,0,0,0},
            {0,0,0,g2,0,0}, {0,0,0,0,g2,0}, {0,0,0,0,0,g2}
        };
    }
}
