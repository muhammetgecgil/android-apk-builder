package com.mg.fixturecockpitsim.sim;
/** Continuous demo loop: post-hangar taxi -> RWY27 -> progressive takeoff -> scenic flight -> stabilized landing -> post-hangar taxi restart. */
public final class AutonomousFlightMission {
 public enum Phase { HANGAR_START,TAXI_OUT,RUNWAY_HOLD,TAKEOFF_ROLL,ROTATE_CLIMB,ORBIT,APPROACH,FLARE,ROLLOUT,TAXI_IN,HANGAR_PARK,COMPLETE }
 public static final double SCENIC_DURATION_SEC=300.0;
 public static final double RUNWAY_HEADING_DEG=270.0;
 private static final double CRUISE_ALTITUDE_M=900.0;
 private Phase phase=Phase.TAXI_OUT;private double phaseTime,orbitTime;
 public void reset(FlightState s){phase=Phase.TAXI_OUT;phaseTime=orbitTime=0;s.timeSec=0;s.altitudeM=0;s.trueAirspeedMps=0;s.verticalSpeedMps=0;s.headingDeg=RUNWAY_HEADING_DEG;s.pitchDeg=s.rollDeg=0;s.throttle=0;s.gearPosition=1;s.brake01=1;s.onGround=true;}
 public Phase getPhase(){return phase;}public double getOrbitTimeSec(){return orbitTime;}public double getPhaseTimeSec(){return phaseTime;}
 public double getPhaseProgress01(){double d;switch(phase){case HANGAR_START:d=5;break;case TAXI_OUT:d=18;break;case RUNWAY_HOLD:d=3;break;case TAKEOFF_ROLL:d=22;break;case ROTATE_CLIMB:d=18;break;case ORBIT:d=300;break;case APPROACH:d=32;break;case FLARE:d=6;break;case ROLLOUT:d=17;break;case TAXI_IN:d=18;break;case HANGAR_PARK:d=6;break;default:d=1;}return clamp(phaseTime/d,0,1);}
 public void update(FlightState s,FlightControls c,double dt){phaseTime+=dt;c.pitch=c.roll=c.yaw=c.brake=0;switch(phase){
  case HANGAR_START:next(Phase.TAXI_OUT);break;
  case TAXI_OUT:c.throttle=.13;c.gearDown=true;groundLock(c,s,.20);if(s.trueAirspeedMps>9)c.brake=.24;if(phaseTime>=18)next(Phase.RUNWAY_HOLD);break;
  case RUNWAY_HOLD:c.throttle=.09;c.brake=1;c.gearDown=true;groundLock(c,s,.24);if(phaseTime>=3)next(Phase.TAKEOFF_ROLL);break;
  case TAKEOFF_ROLL:c.throttle=1;c.gearDown=true;groundLock(c,s,.30);if(s.trueAirspeedMps>=86||phaseTime>=22)next(Phase.ROTATE_CLIMB);break;
  case ROTATE_CLIMB:c.throttle=.96;c.pitch=altitudePitch(s.altitudeM,CRUISE_ALTITUDE_M,.42);c.roll=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG)*.30;c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*.016;c.gearDown=s.altitudeM<55;if(s.altitudeM>=CRUISE_ALTITUDE_M-45)next(Phase.ORBIT);break;
  case ORBIT:orbitTime+=dt;c.gearDown=false;double t=orbitTime,ta=900,th=300,b=.10,tr=.72;if(t<45){ta=980;th=300;b=.10;tr=.76;}else if(t<90){ta=1220;th=325;b=.16;tr=.74;}else if(t<145){ta=760;th=20;b=-.12;tr=.68;}else if(t<205){ta=650;th=65;b=.18;tr=.70;}else if(t<250){ta=840;th=120;b=.14;tr=.72;}else if(t<272){ta=720;th=210;b=-.08;tr=.62;}else{ta=360;th=RUNWAY_HEADING_DEG;b=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG);tr=.46;}c.throttle=tr;c.pitch=altitudePitch(s.altitudeM,ta,.22);c.roll=clamp(b+headingRoll(s.headingDeg,th)*.50,-.34,.34);c.yaw=clamp(headingError(s.headingDeg,th)*.012,-.22,.22);if(orbitTime>=SCENIC_DURATION_SEC)next(Phase.APPROACH);break;
  case APPROACH:{c.gearDown=true;double q=clamp(phaseTime/32.0,0,1),desiredAlt=360.0*(1.0-q)+5.5*q;c.throttle=s.altitudeM>180?.34:s.altitudeM>65?.27:.22;double err=headingError(s.headingDeg,RUNWAY_HEADING_DEG);c.roll=clamp(err/55.0,-.18,.18);c.yaw=clamp(err*.040,-.58,.58);double altErr=desiredAlt-s.altitudeM;c.pitch=clamp(altErr/180.0,-.24,.018);if(s.altitudeM<=6.5||phaseTime>=34)next(Phase.FLARE);break;}
  case FLARE:{c.gearDown=true;c.throttle=.10;double ferr=headingError(s.headingDeg,RUNWAY_HEADING_DEG);c.roll=clamp(ferr/95.0,-.08,.08);c.yaw=clamp(ferr*.055,-.65,.65);c.pitch=s.altitudeM>2.2?-.018:s.altitudeM>.45?.055:.025;if(s.onGround||s.altitudeM<=.05||phaseTime>7)next(Phase.ROLLOUT);break;}
  case ROLLOUT:c.gearDown=true;c.throttle=0;c.brake=.86;groundLock(c,s,.28);if(s.trueAirspeedMps<2&&phaseTime>2)next(Phase.TAXI_IN);break;
  case TAXI_IN:c.gearDown=true;c.throttle=.10;groundLock(c,s,.20);if(s.trueAirspeedMps>8)c.brake=.22;if(phaseTime>=18)next(Phase.HANGAR_PARK);break;
  case HANGAR_PARK:reset(s);break;
  case COMPLETE:c.gearDown=true;c.throttle=0;c.brake=1;groundLock(c,s,.25);break;}c.clamp();}
 private static void groundLock(FlightControls c,FlightState s,double k){double e=headingError(s.headingDeg,RUNWAY_HEADING_DEG);c.yaw=clamp(e*k,-.72,.72);c.roll=0;c.pitch=0;}
 private void next(Phase p){phase=p;phaseTime=0;}
 private static double altitudePitch(double a,double t,double m){return clamp((t-a)/650,-.18,m);}private static double headingRoll(double h,double t){return clamp(headingError(h,t)/55,-.45,.45);}private static double headingError(double h,double t){double d=t-h;while(d>180)d-=360;while(d<-180)d+=360;return d;}private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
}
