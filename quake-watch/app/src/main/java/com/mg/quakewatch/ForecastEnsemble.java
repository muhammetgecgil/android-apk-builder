package com.mg.quakewatch;

/**
 * Experimental operational earthquake forecasting ensemble.
 * Produces RELATIVE activity forecasts, never deterministic earthquake predictions.
 */
public final class ForecastEnsemble {
    public static final class Score {
        public final double etas, hawkes, bTrend, migration, faultGraph, sequence, p24, p7d, p30d, confidence;
        Score(double etas,double hawkes,double bTrend,double migration,double faultGraph,double sequence,double p24,double p7d,double p30d,double confidence){
            this.etas=etas;this.hawkes=hawkes;this.bTrend=bTrend;this.migration=migration;this.faultGraph=faultGraph;this.sequence=sequence;
            this.p24=p24;this.p7d=p7d;this.p30d=p30d;this.confidence=confidence;
        }
    }

    public static Score evaluate(double rateRatio,double etasRaw,double bValue,double migration,double maxMag,int eventCount,double faultKm){
        // Normalize independent expert channels to 0..1. These are relative signals, not calibrated earthquake probabilities.
        double etas = sat(etasRaw/5.0);
        double hawkes = sat(Math.log1p(Math.max(0,rateRatio))/Math.log(4.0));
        double bTrend = sat((1.10-bValue)/0.55);
        double mig = sat(migration);
        double faultGraph = sat(1.0 - Math.min(1.0, Math.max(0,faultKm)/80.0));
        double sequence = sat(0.45*hawkes + 0.25*mig + 0.20*sat((maxMag-2.0)/3.0) + 0.10*sat(eventCount/12.0));

        // Ensemble horizons. Longer horizons lean a little more on persistent/fault-context terms.
        double s24 = 0.32*etas + 0.24*hawkes + 0.16*bTrend + 0.14*mig + 0.08*faultGraph + 0.06*sequence;
        double s7  = 0.25*etas + 0.20*hawkes + 0.16*bTrend + 0.12*mig + 0.15*faultGraph + 0.12*sequence;
        double s30 = 0.16*etas + 0.14*hawkes + 0.14*bTrend + 0.08*mig + 0.25*faultGraph + 0.23*sequence;

        // Map to intuitive 0..100 relative forecast indices; do not label as absolute probability.
        double p24=100*(1-Math.exp(-1.45*s24));
        double p7d=100*(1-Math.exp(-1.65*s7));
        double p30d=100*(1-Math.exp(-1.85*s30));

        // Confidence rises with sample support and agreement between channels; capped to avoid false certainty.
        double mean=(etas+hawkes+bTrend+mig+faultGraph+sequence)/6.0;
        double var=(sq(etas-mean)+sq(hawkes-mean)+sq(bTrend-mean)+sq(mig-mean)+sq(faultGraph-mean)+sq(sequence-mean))/6.0;
        double agreement=1.0-Math.min(1.0,Math.sqrt(var)/0.5);
        double support=sat(eventCount/18.0);
        double confidence=100*Math.min(0.82,0.25+0.42*support+0.33*agreement);
        return new Score(etas,hawkes,bTrend,mig,faultGraph,sequence,p24,p7d,p30d,confidence);
    }

    private static double sat(double x){return Math.max(0,Math.min(1,x));}
    private static double sq(double x){return x*x;}
    private ForecastEnsemble(){}
}
