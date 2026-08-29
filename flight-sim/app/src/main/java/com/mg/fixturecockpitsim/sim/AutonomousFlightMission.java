package com.mg.fixturecockpitsim.sim;
/** Continuous demo loop: hangar -> taxi -> runway -> takeoff -> 5 min flight -> stabilized final -> landing -> hangar. */
public final class AutonomousFlightMission {
 public enum Phase { HANGAR_START,TAXI_OUT,RUNWAY_HOLD,TAKEOFF_ROLL,ROTATE_CLIMB,ORBIT,APPROACH,FLARE,ROLLOUT,TAXI_IN,HANGAR_PARK,COMPLETE }
 public static final double SCENIC_DURATION_SEC=300.0; private static final double CRUISE_ALTITUDE_M=900.0,RUNWAY_HEADING_DEG=0.0;
 private Phase phase=Phase.HANGAR_START;private double phaseTime,orbitTime;
 public void reset(FlightState s){phase=Phase.HANGAR_START;phaseTime=orbitTime=0;s.timeSec=0;s.altitudeM=0;s.trueAirspeedMps=0;s.verticalSpeedMps=0;s.headingDeg=RUNWAY_HEADING_DEG;s.pitchDeg=s.rollDeg=0;s.throttle=0;s.gearPosition=1;s.brake01=1;s.onGround=true;}
 public Phase getPhase(){return phase;}public double getOrbitTimeSec(){return orbitTime;}public double getPhaseTimeSec(){return phaseTime;}
 public double getPhaseProgress01(){double d;switch(phase){case HANGAR_START:d=4;break;case TAXI_OUT:d=14;break;case RUNWAY_HOLD:d=2;break;case TAKEOFF_ROLL:d=12;break;case ROTATE_CLIMB:d=16;break;case ORBIT:d=300;break;case APPROACH:d=42;break;case FLARE:d=8;break;case ROLLOUT:d=15;break;case TAXI_IN:d=14;break;case HANGAR_PARK:d=5;break;default:d=1;}return clamp(phaseTime/d,0,1);}
 public void update(FlightState s,FlightControls c,double dt){phaseTime+=dt;c.pitch=c.roll=c.yaw=c.brake=0;switch(phase){
 case HANGAR_START:c.throttle=.08;c.brake=1;c.gearDown=true;align(c,s,.05);if(phaseTime>=4)next(Phase.TAXI_OUT);break;
 case TAXI_OUT:c.throttle=.16;c.gearDown=true;align(c,s,.055);if(s.trueAirspeedMps>12)c.brake=.20;if(phaseTime>=14)next(Phase.RUNWAY_HOLD);break;
 case RUNWAY_HOLD:c.throttle=.10;c.brake=1;c.gearDown=true;align(c,s,.06);if(phaseTime>=2)next(Phase.TAKEOFF_ROLL);break;
 case TAKEOFF_ROLL:c.throttle=1;c.gearDown=true;align(c,s,.06);if(s.trueAirspeedMps>=82||phaseTime>=12)next(Phase.ROTATE_CLIMB);break;
 case ROTATE_CLIMB:c.throttle=.96;c.pitch=altitudePitch(s.altitudeM,CRUISE_ALTITUDE_M,.44);c.roll=headingRoll(s.headingDeg,8);c.gearDown=s.altitudeM<45;if(s.altitudeM>=CRUISE_ALTITUDE_M-45)next(Phase.ORBIT);break;
 case ORBIT:orbitTime+=dt;c.gearDown=false;double t=orbitTime,ta=900,th=25,b=.10,tr=.72;if(t<45){ta=980;th=28;b=.10;tr=.76;}else if(t<90){ta=1220;th=58;b=.16;tr=.74;}else if(t<145){ta=760;th=95;b=-.12;tr=.68;}else if(t<205){ta=650;th=135;b=.18;tr=.70;}else if(t<250){ta=840;th=190;b=.14;tr=.72;}else if(t<275){ta=900;th=245;b=-.10;tr=.70;}else{ta=430;th=RUNWAY_HEADING_DEG;b=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG);tr=.54;}c.throttle=tr;c.pitch=altitudePitch(s.altitudeM,ta,.18);c.roll=clamp(b+headingRoll(s.headingDeg,th)*.55,-.38,.38);if(orbitTime>=SCENIC_DURATION_SEC)next(Phase.APPROACH);break;
 case APPROACH:c.gearDown=true;double q=clamp(phaseTime/42.0,0,1),desiredAlt=430.0*(1.0-q)+18.0*q;c.throttle=s.altitudeM>180?.42:.32;c.roll=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG)*1.15;c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*.018;c.pitch=clamp((desiredAlt-s.altitudeM)/520.0,-.14,.035);if(s.altitudeM<=18||phaseTime>=42)next(Phase.FLARE);break;
 case FLARE:c.gearDown=true;c.throttle=.16;c.pitch=s.altitudeM>3?-.025:s.altitudeM>.6?-.008:0;c.roll=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG)*.7;c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*.012;if(s.onGround||s.altitudeM<=.05)next(Phase.ROLLOUT);break;
 case ROLLOUT:c.gearDown=true;c.throttle=0;c.brake=.88;align(c,s,.06);if(s.trueAirspeedMps<2&&phaseTime>2)next(Phase.TAXI_IN);break;
 case TAXI_IN:c.gearDown=true;c.throttle=.13;align(c,s,.055);if(s.trueAirspeedMps>10)c.brake=.18;if(phaseTime>=14)next(Phase.HANGAR_PARK);break;
 case HANGAR_PARK:c.gearDown=true;c.throttle=.05;c.brake=.72;align(c,s,.055);if(phaseTime>=5){reset(s);}break;
 case COMPLETE:c.gearDown=true;c.throttle=0;c.brake=1;break;}c.clamp();}
 private static void align(FlightControls c,FlightState s,double k){c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*k;}
 private void next(Phase p){phase=p;phaseTime=0;}private static double altitudePitch(double a,double t,double m){return clamp((t-a)/700,-.14,m);}private static double headingRoll(double h,double t){return clamp(headingError(h,t)/55,-.45,.45);}private static double headingError(double h,double t){double d=t-h;while(d>180)d-=360;while(d<-180)d+=360;return d;}private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
}
