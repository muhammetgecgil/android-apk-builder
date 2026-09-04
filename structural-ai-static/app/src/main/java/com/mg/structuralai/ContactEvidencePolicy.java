package com.mg.structuralai;

import java.util.Locale;

/** Engineering-evidence policy for explicit contact definitions. Never invents friction or contact type. */
public final class ContactEvidencePolicy {
    public enum Mode { BONDED, FRICTIONLESS, NO_SEPARATION, FRICTIONAL }

    public static final class Definition {
        public final Mode mode;
        public final Double frictionCoefficient;
        public final String provenance;
        public final double initialGapM;
        public final double confidence;

        public Definition(Mode mode, Double mu, String provenance, double initialGapM, double confidence) {
            if (mode == null) throw new IllegalArgumentException("Contact mode is required");
            if (!Double.isFinite(initialGapM) || initialGapM < 0) throw new IllegalArgumentException("Initial gap must be finite and >=0");
            if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) throw new IllegalArgumentException("Contact confidence must be within [0,1]");
            if (mu != null && (!Double.isFinite(mu) || mu < 0 || mu > 2.0)) throw new IllegalArgumentException("Friction coefficient must be within [0,2]");
            this.mode = mode;
            this.frictionCoefficient = mu;
            this.provenance = provenance == null ? "" : provenance.trim();
            this.initialGapM = initialGapM;
            this.confidence = confidence;
        }
    }

    public static final class Assessment {
        public final boolean numericallyAdmissible;
        public final boolean designEvidenceReady;
        public final String reason;
        Assessment(boolean n, boolean d, String r){numericallyAdmissible=n;designEvidenceReady=d;reason=r;}
        public String summary(){return "CONTACT EVIDENCE | numerical="+numericallyAdmissible+" | designEvidence="+designEvidenceReady+" | "+reason;}
    }

    private ContactEvidencePolicy(){}

    public static Assessment assess(Definition d){
        if (d.mode == Mode.FRICTIONAL) {
            if (d.frictionCoefficient == null) return new Assessment(false,false,"FRICTIONAL contact blocked: explicit coefficient mu is missing");
            if (d.provenance.isEmpty()) return new Assessment(false,false,"FRICTIONAL contact blocked: friction coefficient provenance is missing");
            if (d.confidence < 0.80) return new Assessment(false,false,"FRICTIONAL contact blocked: geometry/contact confidence <80%");
            return new Assessment(false,false,String.format(Locale.US,"FRICTIONAL evidence present (mu=%.4g) but nonlinear Coulomb solver is not yet release-qualified; safe block",d.frictionCoefficient));
        }
        if (d.mode == Mode.BONDED) {
            if (d.confidence < 0.80) return new Assessment(false,false,"BONDED contact confidence <80%; explicit user confirmation required");
            return new Assessment(true,!d.provenance.isEmpty(),d.provenance.isEmpty()?"BONDED numerical solve allowed; design release needs contact provenance":"BONDED contact evidence ready");
        }
        if (d.mode == Mode.FRICTIONLESS || d.mode == Mode.NO_SEPARATION) {
            if (d.confidence < 0.70) return new Assessment(false,false,"Contact confidence <70%; unresolved contact must not be solved");
            return new Assessment(true,!d.provenance.isEmpty(),d.provenance.isEmpty()?"Numerical contact solve allowed; design release needs contact provenance":"Contact evidence ready");
        }
        return new Assessment(false,false,"Unsupported contact definition");
    }
}
