package com.mg.fixturecockpitsim.sim;

/** Shared deterministic state for the long-form Türkiye cinematic flight. */
public final class CinematicJourneyState {
    public static final int RUNWAY=0, TOROS=1, AEGEAN=2, PATARA=3, KARAPINAR=4,
            CLOUD_SEA=5, STORM=6, SNOW=7, MOONLIT=8, RETURN=9;

    private static volatile double airborneSec;
    private static volatile double progress01;
    private static volatile int stage=RUNWAY;
    private static volatile String flightPhase="RUNWAY_HOLD";

    private CinematicJourneyState(){}

    public static synchronized void reset(){airborneSec=0;progress01=0;stage=RUNWAY;flightPhase="RUNWAY_HOLD";}

    public static synchronized void update(double speedMps,double dt,boolean onGround,String phase){
        dt=clamp(dt,0.001,0.08);flightPhase=phase==null?"":phase;
        if(flightPhase.contains("APPROACH")||flightPhase.contains("FLARE")||flightPhase.contains("ROLLOUT")||flightPhase.contains("TAXI_IN")){
            stage=RETURN;progress01=Math.max(progress01,.96);return;
        }
        if(onGround){
            if(flightPhase.contains("RUNWAY_HOLD")||flightPhase.contains("TAKEOFF_ROLL")||speedMps<3){
                airborneSec=0;progress01=0;stage=RUNWAY;
            }
            return;
        }
        airborneSec+=dt;
        progress01=clamp(airborneSec/112.0,0,1);
        if(airborneSec<13)stage=TOROS;
        else if(airborneSec<27)stage=AEGEAN;
        else if(airborneSec<39)stage=PATARA;
        else if(airborneSec<51)stage=KARAPINAR;
        else if(airborneSec<66)stage=CLOUD_SEA;
        else if(airborneSec<79)stage=STORM;
        else if(airborneSec<90)stage=SNOW;
        else if(airborneSec<107)stage=MOONLIT;
        else stage=RETURN;
    }

    public static int getStage(){return stage;}
    public static double getAirborneSec(){return airborneSec;}
    public static double getProgress01(){return progress01;}
    public static String getFlightPhase(){return flightPhase;}

    public static float getStageBlend01(){
        double t=airborneSec,a=0,b=1;
        switch(stage){
            case TOROS:a=0;b=13;break; case AEGEAN:a=13;b=27;break; case PATARA:a=27;b=39;break;
            case KARAPINAR:a=39;b=51;break; case CLOUD_SEA:a=51;b=66;break; case STORM:a=66;b=79;break;
            case SNOW:a=79;b=90;break; case MOONLIT:a=90;b=107;break; case RETURN:a=107;b=112;break;
            default:return 0f;
        }
        return (float)clamp((t-a)/(b-a),0,1);
    }

    public static int terrainKind(){
        switch(stage){case TOROS:return 0;case AEGEAN:return 1;case PATARA:return 2;case KARAPINAR:return 3;case MOONLIT:return 4;default:return 0;}
    }
    public static float sea01(){return stage==AEGEAN?1f:stage==MOONLIT?.95f:stage==RETURN?.35f:0f;}
    public static float desert01(){return stage==PATARA?1f:stage==KARAPINAR?.92f:0f;}
    public static float cloudSea01(){return stage==CLOUD_SEA?1f:stage==STORM?.35f:0f;}
    public static float storm01(){return stage==STORM?1f:0f;}
    public static float snow01(){return stage==SNOW?1f:0f;}
    public static float night01(){return stage==MOONLIT?1f:stage==RETURN?.45f:0f;}

    public static String stageName(){
        switch(stage){
            case TOROS:return "TOROS 3D GEÇİŞİ";case AEGEAN:return "EGE KIYISI";case PATARA:return "PATARA KUMULLARI";
            case KARAPINAR:return "KARAPINAR / İÇ ANADOLU";case CLOUD_SEA:return "BULUT DENİZİ";case STORM:return "FIRTINA ÇEKİRDEĞİ";
            case SNOW:return "KAR HATTI";case MOONLIT:return "AY IŞIĞINDA DENİZ";case RETURN:return "ÜSSE DÖNÜŞ";default:return "PIST / KALKIŞ";
        }
    }

    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
