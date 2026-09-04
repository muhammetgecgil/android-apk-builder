package com.mg.structuralai;

public final class LinearElasticMaterial {
    public enum EvidenceLevel { UNKNOWN, REFERENCE, STANDARD_TRACEABLE, USER_CERTIFIED }
    public final String name;
    public final double youngPa;
    public final double poisson;
    public final double densityKgM3;
    public final double yieldPa;
    public final double ultimatePa;
    public final String provenance;
    public final EvidenceLevel evidenceLevel;

    public LinearElasticMaterial(String name,double youngPa,double poisson,double densityKgM3,double yieldPa){
        this(name,youngPa,poisson,densityKgM3,yieldPa,Double.NaN,"legacy/reference material; design allowables not certified",EvidenceLevel.REFERENCE);
    }

    public LinearElasticMaterial(String name,double youngPa,double poisson,double densityKgM3,double yieldPa,double ultimatePa,String provenance,EvidenceLevel evidenceLevel) {
        if (youngPa <= 0.0 || !Double.isFinite(youngPa)) throw new IllegalArgumentException("E must be positive");
        if (poisson <= -1.0 || poisson >= 0.5 || !Double.isFinite(poisson)) throw new IllegalArgumentException("Poisson ratio outside stable isotropic range");
        if (densityKgM3 <= 0.0 || !Double.isFinite(densityKgM3)) throw new IllegalArgumentException("Density must be positive");
        if (Double.isFinite(yieldPa) && yieldPa <= 0) throw new IllegalArgumentException("Yield must be positive when supplied");
        if (Double.isFinite(ultimatePa) && ultimatePa <= 0) throw new IllegalArgumentException("Ultimate must be positive when supplied");
        this.name = name;
        this.youngPa = youngPa;
        this.poisson = poisson;
        this.densityKgM3 = densityKgM3;
        this.yieldPa = yieldPa;
        this.ultimatePa = ultimatePa;
        this.provenance = provenance==null?"":provenance;
        this.evidenceLevel = evidenceLevel==null?EvidenceLevel.UNKNOWN:evidenceLevel;
    }

    public boolean hasStrengthEvidence(){return Double.isFinite(yieldPa)&&yieldPa>0&&Double.isFinite(ultimatePa)&&ultimatePa>=yieldPa;}
    public boolean designReleaseEligible(){return hasStrengthEvidence()&&(evidenceLevel==EvidenceLevel.STANDARD_TRACEABLE||evidenceLevel==EvidenceLevel.USER_CERTIFIED)&&!provenance.trim().isEmpty();}

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
