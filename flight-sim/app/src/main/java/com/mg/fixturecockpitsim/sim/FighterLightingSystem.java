package com.mg.fixturecockpitsim.sim;

/**
 * Generic modern fighter lighting controller.
 * Keeps exterior lights independent while providing realistic pulse timing
 * and brightness levels for cockpit flood/HUD night operation.
 */
public final class FighterLightingSystem {
    public boolean navigation = true;
    public boolean strobe = true;
    public boolean beacon = true;
    public boolean landing = false;
    public boolean taxi = false;
    public boolean formation = false;

    // 0=off, 1=low, 2=medium, 3=high.
    private int floodStep = 0;
    // 0=100%, 1=70%, 2=45%, 3=25% for night adaptation.
    private int hudStep = 0;

    public int getFloodStep(){ return floodStep; }
    public int getHudStep(){ return hudStep; }

    public double floodBrightness(){
        switch(floodStep){
            case 1:return .28;
            case 2:return .58;
            case 3:return 1.0;
            default:return 0.0;
        }
    }

    public double hudBrightness(){
        switch(hudStep){
            case 1:return .70;
            case 2:return .45;
            case 3:return .25;
            default:return 1.0;
        }
    }

    public void cycleFlood(){ floodStep=(floodStep+1)%4; }
    public void cycleHud(){ hudStep=(hudStep+1)%4; }

    public double strobeIntensity(long timeMs){
        if(!strobe)return 0.0;
        long p=((timeMs%1200)+1200)%1200;
        return (p<65 || (p>=165 && p<230)) ? 1.0 : 0.0;
    }

    public double beaconIntensity(long timeMs){
        if(!beacon)return 0.0;
        long p=((timeMs%1050)+1050)%1050;
        if(p<90)return 1.0-p/90.0*.35;
        if(p<190)return .65*(1.0-(p-90)/100.0);
        return 0.0;
    }

    /** Generic gear-mounted fighter landing light behavior. */
    public double landingIntensity(double gearPosition){
        if(!landing)return 0.0;
        return clamp((gearPosition-.18)/.62,0,1);
    }

    /** Taxi light is useful only with gear extended and aircraft on ground. */
    public double taxiIntensity(double gearPosition,boolean onGround){
        if(!taxi || !onGround)return 0.0;
        return clamp((gearPosition-.25)/.55,0,1);
    }

    public static double clamp(double v,double lo,double hi){ return Math.max(lo,Math.min(hi,v)); }
}
