package com.mg.fixturecockpitsim.sim;

/** Continuous demo loop: hangar -> taxi -> runway -> takeoff -> 5 min flight -> landing -> taxi-in -> hangar. */
public final class AutonomousFlightMission {
    public enum Phase { HANGAR_START, TAXI_OUT, RUNWAY_HOLD, TAKEOFF_ROLL, ROTATE_CLIMB, ORBIT, APPROACH, FLARE, ROLLOUT, TAXI_IN, HANGAR_PARK, COMPLETE }
    public static final double SCENIC_DURATION_SEC=300.0;
    private static final double CRUISE_ALTITUDE_M=900.0, RUNWAY_HEADING_DEG=0.0;
    private Phase phase=Phase.HANGAR_START; private double phaseTime, orbitTime;
    public void reset(FlightState s){phase=Phase.HANGAR_START;phaseTime=orbitTime=0;s.timeSec=0;s.altitudeM=0;s.trueAirspeedMps=0;s.verticalSpeedMps=0;s.headingDeg=RUNWAY_HEADING_DEG;s.pitchDeg=s.rollDeg=0;s.throttle=0;s.gearPosition=1;s.brake01=1;s.onGround=true;}
    public Phase getPhase(){return phase;} public double getOrbitTimeSec(){return orbitTime;} public double getPhaseTimeSec(){return phaseTime;}
    public double getPhaseProgress01(){double duration;switch(phase){case HANGAR_START:duration=4;break;case TAXI_OUT:duration=14;break;case RUNWAY_HOLD:duration=2;break;case TAKEOFF_ROLL:duration=12;break;case ROTATE_CLIMB:duration=16;break;case ORBIT:duration=SCENIC_DURATION_SEC;break;case APPROACH:duration=30;break;case FLARE:duration=8;break;case ROLLOUT:duration=15;break;case TAXI_IN:duration=14;break;case HANGAR_PARK:duration=5;break;default:duration=1;}return clamp(phaseTime/duration,0,1);}
    public void update(FlightState s,FlightControls c,double dt){phaseTime+=dt;c.pitch=c.roll=c.yaw=c.brake=0;
        switch(phase){
            case HANGAR_START:c.throttle=.08;c.brake=1;c.gearDown=true;c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*.05;if(phaseTime>=4)next(Phase.TAXI_OUT);break;
            case TAXI_OUT:c.throttle=.16;c.brake=0;c.gearDown=true;c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*.055;if(s.trueAirspeedMps>12)c.brake=.20;if(phaseTime>=14)next(Phase.RUNWAY_HOLD);break;
            case RUNWAY_HOLD:c.throttle=.10;c.brake=1;c.gearDown=true;c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*.05;if(phaseTime>=2)next(Phase.TAKEOFF_ROLL);break;
            case TAKEOFF_ROLL:c.throttle=1;c.gearDown=true;c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*.045;if(s.trueAirspeedMps>=82||phaseTime>=12)next(Phase.ROTATE_CLIMB);break;
            case ROTATE_CLIMB:c.throttle=.96;c.pitch=altitudePitch(s.altitudeM,CRUISE_ALTITUDE_M,.44);c.roll=headingRoll(s.headingDeg,8);c.gearDown=s.altitudeM<45;if(s.altitudeM>=CRUISE_ALTITUDE_M-45)next(Phase.ORBIT);break;
            case ORBIT:
                orbitTime+=dt;c.gearDown=false;double t=orbitTime,targetAlt=900,targetHdg=25,bank=.10,thr=.72;
                if(t<45){targetAlt=980;targetHdg=28;bank=.10;thr=.76;}else if(t<90){targetAlt=1220;targetHdg=58;bank=.16;thr=.74;}else if(t<145){targetAlt=760;targetHdg=95;bank=-.12;thr=.68;}else if(t<205){targetAlt=650;targetHdg=135;bank=.18;thr=.70;}else if(t<250){targetAlt=840;targetHdg=190;bank=.14;thr=.72;}else if(t<280){targetAlt=1040;targetHdg=245;bank=-.10;thr=.70;}else{targetAlt=520;targetHdg=RUNWAY_HEADING_DEG;bank=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG);thr=.58;}
                c.throttle=thr;c.pitch=altitudePitch(s.altitudeM,targetAlt,.18);c.roll=clamp(bank+headingRoll(s.headingDeg,targetHdg)*.55,-.38,.38);if(orbitTime>=SCENIC_DURATION_SEC)next(Phase.APPROACH);break;
            case APPROACH:c.gearDown=true;c.throttle=s.altitudeM>260?.46:.36;c.roll=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG);c.pitch=s.altitudeM>160?-.16:-.10;if(s.altitudeM<=18)next(Phase.FLARE);break;
            case FLARE:c.gearDown=true;c.throttle=.18;c.pitch=s.altitudeM>3?-.035:s.altitudeM>.60?-.012:-.005;c.roll=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG)*.6;if(s.onGround||s.altitudeM<=.05)next(Phase.ROLLOUT);break;
            case ROLLOUT:c.gearDown=true;c.throttle=0;c.brake=.88;c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*.05;if(s.trueAirspeedMps<2&&phaseTime>2)next(Phase.TAXI_IN);break;
            case TAXI_IN:c.gearDown=true;c.throttle=.13;c.brake=0;c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*.05;if(s.trueAirspeedMps>10)c.brake=.18;if(phaseTime>=14)next(Phase.HANGAR_PARK);break;
            case HANGAR_PARK:c.gearDown=true;c.throttle=.05;c.brake=.72;c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*.05;if(phaseTime>=5){s.altitudeM=0;s.trueAirspeedMps=0;s.verticalSpeedMps=0;s.headingDeg=RUNWAY_HEADING_DEG;s.pitchDeg=s.rollDeg=0;s.throttle=.08;s.gearPosition=1;s.brake01=1;s.onGround=true;phase=Phase.HANGAR_START;phaseTime=0;orbitTime=0;}break;
            case COMPLETE:c.gearDown=true;c.throttle=0;c.brake=1;break;
        }c.clamp();}
    private void next(Phase p){phase=p;phaseTime=0;}
    private static double altitudePitch(double a,double t,double m){return clamp((t-a)/700,-.14,m);}private static double headingRoll(double h,double t){return clamp(headingError(h,t)/55,-.45,.45);}private static double headingError(double h,double t){double d=t-h;while(d>180)d-=360;while(d<-180)d+=360;return d;}private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
}
