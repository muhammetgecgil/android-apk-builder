package com.mg.structuralai;

public final class EngineeringAssumption {
    public enum Kind { UNIT, MATERIAL, SUPPORT, LOAD, CONTACT, IDEALIZATION, MESH }

    public final Kind kind;
    public final String value;
    public final String evidence;
    public final double confidence;
    public final boolean solverCritical;

    public EngineeringAssumption(Kind kind, String value, String evidence, double confidence, boolean solverCritical) {
        this.kind = kind;
        this.value = value;
        this.evidence = evidence;
        this.confidence = clamp(confidence);
        this.solverCritical = solverCritical;
    }

    public boolean needsReview() { return solverCritical && confidence < 0.80; }

    private static double clamp(double x) { return Math.max(0.0, Math.min(1.0, x)); }
}
