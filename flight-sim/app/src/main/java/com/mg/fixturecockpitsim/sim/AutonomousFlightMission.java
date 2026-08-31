package com.mg.fixturecockpitsim.sim;

import com.mg.fixturecockpitsim.CinematicEnvironmentView;

/**
 * AVM-14.3 demo mission: runway departure -> cloud-deck climb -> above-cloud cruise ->
 * fighter-style dive -> sea-skimming run -> high-power pull-up -> scenic cruise -> landing.
 */
public final class AutonomousFlightMission {
    public enum Phase {
        HANGAR_START,TAXI_OUT,RUNWAY_HOLD,TAKEOFF_ROLL,ROTATE_CLIMB,
        CLOUD_CLIMB,ABOVE_CLOUD_CRUISE,DIVE_TO_SEA_SKIM,SEA_SKIM_RUN,PULL_UP_CLIMB,
        ORBIT,APPROACH,FLARE,ROLLOUT,TAXI_IN,HANGAR_PARK,COMPLETE
    }

    public static final double SCENIC_DURATION_SEC=105.0;
    public static final double RUNWAY_HEADING_DEG=270.0;
    private static final double CLOUD_TOP_ALT_M=1450.0;
    private static final double SEA_SKIM_ALT_M=24.0;
    private static final double POST_PULL_ALT_M=900.0;

    private Phase phase=Phase.TAXI_OUT;
    private double phaseTime,orbitTime;

    public void reset(FlightState s){
        phase=Phase.TAXI_OUT;phaseTime=orbitTime=0;
        s.timeSec=0;s.altitudeM=0;s.trueAirspeedMps=0;s.verticalSpeedMps=0;
        s.headingDeg=RUNWAY_HEADING_DEG;s.pitchDeg=s.rollDeg=0;s.throttle=0;
        s.gearPosition=1;s.brake01=1;s.onGround=true;
        CinematicEnvironmentView.setFlightScene(phase.name(),0,0,0,s.headingDeg,0);
    }

    public Phase getPhase(){return phase;}
    public double getOrbitTimeSec(){return orbitTime;}
    public double getPhaseTimeSec(){return phaseTime;}

    public double getPhaseProgress01(){
        double d;
        switch(phase){
            case HANGAR_START:d=5;break;
            case TAXI_OUT:d=18;break;
            case RUNWAY_HOLD:d=3;break;
            case TAKEOFF_ROLL:d=22;break;
            case ROTATE_CLIMB:d=26;break;
            case CLOUD_CLIMB:d=28;break;
            case ABOVE_CLOUD_CRUISE:d=34;break;
            case DIVE_TO_SEA_SKIM:d=30;break;
            case SEA_SKIM_RUN:d=34;break;
            case PULL_UP_CLIMB:d=22;break;
            case ORBIT:d=SCENIC_DURATION_SEC;break;
            case APPROACH:d=22;break;
            case FLARE:d=7;break;
            case ROLLOUT:d=12;break;
            case TAXI_IN:d=12;break;
            case HANGAR_PARK:d=4;break;
            default:d=1;
        }
        return clamp(phaseTime/d,0,1);
    }

    public void update(FlightState s,FlightControls c,double dt){
        // DEMO invariant: zero speed means the autonomous aircraft is landed.
        if(s.trueAirspeedMps<=.05){s.trueAirspeedMps=0;s.altitudeM=0;s.verticalSpeedMps=0;s.onGround=true;}
        phaseTime+=dt;c.pitch=c.roll=c.yaw=c.brake=0;

        switch(phase){
            case HANGAR_START:
                next(Phase.TAXI_OUT);break;

            case TAXI_OUT:
                c.throttle=.13;c.gearDown=true;groundLock(c,s,.20);
                if(s.trueAirspeedMps>9)c.brake=.24;
                if(phaseTime>=18)next(Phase.RUNWAY_HOLD);break;

            case RUNWAY_HOLD:
                c.throttle=.09;c.brake=1;c.gearDown=true;groundLock(c,s,.24);
                if(phaseTime>=3)next(Phase.TAKEOFF_ROLL);break;

            case TAKEOFF_ROLL:
                c.throttle=1;c.gearDown=true;groundLock(c,s,.30);
                if(s.trueAirspeedMps>=86||phaseTime>=22)next(Phase.ROTATE_CLIMB);break;

            case ROTATE_CLIMB:{
                c.throttle=.96;c.gearDown=s.altitudeM<55;
                c.pitch=altitudePitch(s.altitudeM,720,.34);
                c.roll=headingRoll(s.headingDeg,288)*.34;
                c.yaw=headingError(s.headingDeg,288)*.015;
                if(s.altitudeM>=680||phaseTime>=26)next(Phase.CLOUD_CLIMB);break;
            }

            case CLOUD_CLIMB:{
                c.gearDown=false;c.throttle=.96;
                double targetHdg=300+8*Math.sin(phaseTime*.08);
                c.pitch=clamp((CLOUD_TOP_ALT_M-s.altitudeM)/900.0,.08,.34);
                c.roll=clamp(headingRoll(s.headingDeg,targetHdg)*.52,-.25,.25);
                c.yaw=clamp(headingError(s.headingDeg,targetHdg)*.012,-.18,.18);
                if(s.altitudeM>=1375||phaseTime>=28)next(Phase.ABOVE_CLOUD_CRUISE);break;
            }

            case ABOVE_CLOUD_CRUISE:{
                c.gearDown=false;c.throttle=.78;
                double targetHdg=318+14*Math.sin(phaseTime*.055);
                c.pitch=clamp((CLOUD_TOP_ALT_M-s.altitudeM)/700.0,-.12,.16);
                c.roll=clamp(.08*Math.sin(phaseTime*.28)+headingRoll(s.headingDeg,targetHdg)*.42,-.23,.23);
                c.yaw=clamp(headingError(s.headingDeg,targetHdg)*.010,-.16,.16);
                if(phaseTime>=34)next(Phase.DIVE_TO_SEA_SKIM);break;
            }

            case DIVE_TO_SEA_SKIM:{
                c.gearDown=false;
                double q=clamp(phaseTime/30.0,0,1);
                double ease=(1-Math.pow(1-q,1.45));
                double desired=CLOUD_TOP_ALT_M*(1-ease)+SEA_SKIM_ALT_M*ease;
                double targetHdg=225;
                c.throttle=q<.55?.72:.88;
                c.pitch=clamp((desired-s.altitudeM)/320.0,-.48,.10);
                // Begin the recovery before reaching the water so the maneuver flows into level flight.
                if(s.altitudeM<150)c.pitch=Math.max(c.pitch,-.11);
                if(s.altitudeM<70)c.pitch=Math.max(c.pitch,.015);
                c.roll=clamp(headingRoll(s.headingDeg,targetHdg)*.60,-.32,.32);
                c.yaw=clamp(headingError(s.headingDeg,targetHdg)*.013,-.22,.22);
                if((s.altitudeM<=48&&phaseTime>18)||phaseTime>=30)next(Phase.SEA_SKIM_RUN);break;
            }

            case SEA_SKIM_RUN:{
                c.gearDown=false;c.throttle=.90;
                double targetAlt=SEA_SKIM_ALT_M+4.5*Math.sin(phaseTime*.34);
                double targetHdg=225+17*Math.sin(phaseTime*.095);
                c.pitch=clamp((targetAlt-s.altitudeM)/115.0,-.10,.13);
                c.roll=clamp(.11*Math.sin(phaseTime*.42)+headingRoll(s.headingDeg,targetHdg)*.45,-.27,.27);
                c.yaw=clamp(headingError(s.headingDeg,targetHdg)*.011,-.18,.18);
                if(phaseTime>=34)next(Phase.PULL_UP_CLIMB);break;
            }

            case PULL_UP_CLIMB:{
                c.gearDown=false;c.throttle=1.0;
                double q=clamp(phaseTime/22.0,0,1);
                double smooth=q*q*(3-2*q);
                double desired=SEA_SKIM_ALT_M+(POST_PULL_ALT_M-SEA_SKIM_ALT_M)*smooth;
                double targetHdg=255;
                c.pitch=clamp((desired-s.altitudeM)/350.0,.12,.48);
                c.roll=clamp(headingRoll(s.headingDeg,targetHdg)*.44,-.24,.24);
                c.yaw=clamp(headingError(s.headingDeg,targetHdg)*.010,-.16,.16);
                if(s.altitudeM>=840||phaseTime>=22)next(Phase.ORBIT);break;
            }

            case ORBIT:{
                orbitTime+=dt;c.gearDown=false;
                double t=orbitTime,ta=820,th=285,b=.08,tr=.70;
                if(t<22){ta=900;th=285;b=.08;tr=.72;}
                else if(t<44){ta=760;th=335;b=.14;tr=.69;}
                else if(t<66){ta=870;th=35;b=-.10;tr=.70;}
                else if(t<84){ta=720;th=100;b=.12;tr=.67;}
                else{ta=650;th=RUNWAY_HEADING_DEG;b=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG)*.35;tr=.58;}
                c.throttle=tr;c.pitch=altitudePitch(s.altitudeM,ta,.20);
                c.roll=clamp(b+headingRoll(s.headingDeg,th)*.46,-.30,.30);
                c.yaw=clamp(headingError(s.headingDeg,th)*.011,-.20,.20);
                if(orbitTime>=SCENIC_DURATION_SEC)next(Phase.APPROACH);break;
            }

            case APPROACH:{
                c.gearDown=true;double q=clamp(phaseTime/22.0,0,1),desiredAlt=620.0*(1.0-q)+4.0*q;
                c.throttle=s.altitudeM>220?.31:s.altitudeM>70?.24:.18;
                double err=headingError(s.headingDeg,RUNWAY_HEADING_DEG);
                c.roll=clamp(err/50.0,-.20,.20);c.yaw=clamp(err*.044,-.60,.60);
                double altErr=desiredAlt-s.altitudeM;c.pitch=clamp(altErr/125.0,-.38,.025);
                if(s.altitudeM<=7.0||phaseTime>=24)next(Phase.FLARE);break;
            }

            case FLARE:{
                c.gearDown=true;c.throttle=s.altitudeM>2.5?.11:.07;
                double ferr=headingError(s.headingDeg,RUNWAY_HEADING_DEG);
                c.roll=clamp(ferr/100.0,-.07,.07);c.yaw=clamp(ferr*.055,-.65,.65);
                c.pitch=s.altitudeM>5?-.10:s.altitudeM>2?-.045:s.altitudeM>.45?.045:.018;
                if(s.onGround||s.altitudeM<=.05||phaseTime>8)next(Phase.ROLLOUT);break;
            }

            case ROLLOUT:
                c.gearDown=true;c.throttle=0;c.brake=.90;groundLock(c,s,.30);
                if(s.trueAirspeedMps<2&&phaseTime>1.5)next(Phase.TAXI_IN);break;

            case TAXI_IN:
                c.gearDown=true;c.throttle=.10;groundLock(c,s,.20);
                if(s.trueAirspeedMps>8)c.brake=.24;
                if(phaseTime>=12)next(Phase.HANGAR_PARK);break;

            case HANGAR_PARK:
                reset(s);break;

            case COMPLETE:
                c.gearDown=true;c.throttle=0;c.brake=1;groundLock(c,s,.25);break;
        }
        c.clamp();
        CinematicEnvironmentView.setFlightScene(phase.name(),s.altitudeM,s.trueAirspeedMps,s.pitchDeg,s.headingDeg,phaseTime);
    }

    private static void groundLock(FlightControls c,FlightState s,double k){double e=headingError(s.headingDeg,RUNWAY_HEADING_DEG);c.yaw=clamp(e*k,-.72,.72);c.roll=0;c.pitch=0;}
    private void next(Phase p){phase=p;phaseTime=0;}
    private static double altitudePitch(double a,double t,double m){return clamp((t-a)/650,-.18,m);}
    private static double headingRoll(double h,double t){return clamp(headingError(h,t)/55,-.45,.45);}
    private static double headingError(double h,double t){double d=t-h;while(d>180)d-=360;while(d<-180)d+=360;return d;}
    private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
}
