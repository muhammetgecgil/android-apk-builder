package com.mg.fixturecockpitsim.sim;
/** Continuous demo loop: hangar -> straight taxi -> RWY27 -> takeoff -> scenic flight -> stabilized landing -> hangar. */
public final class AutonomousFlightMission {
 public enum Phase { HANGAR_START,TAXI_OUT,RUNWAY_HOLD,TAKEOFF_ROLL,ROTATE_CLIMB,ORBIT,APPROACH,FLARE,ROLLOUT,TAXI_IN,HANGAR_PARK,COMPLETE }
 public static final double SCENIC_DURATION_SEC=300.0;
 public static final double RUNWAY_HEADING_DEG=270.0;
 private static final double CRUISE_ALTITUDE_M=900.0;
 private Phase phase=Phase.HANGAR_START;private double phaseTime,orbitTime;
 public void reset(FlightState s){phase=Phase.HANGAR_START;phaseTime=orbitTime=0;s.timeSec=0;s.altitudeM=0;s.trueAirspeedMps=0;s.verticalSpeedMps=0;s.headingDeg=RUNWAY_HEADING_DEG;s.pitchDeg=s.rollDeg=0;s.throttle=0;s.gearPosition=1;s.brake01=1;s.onGround=true;}
 public Phase getPhase(){return phase;}public double getOrbitTimeSec(){return orbitTime;}public double getPhaseTimeSec(){return phaseTime;}
 public double getPhaseProgress01(){double d;switch(phase){case HANGAR_START:d=5;break;case TAXI_OUT:d=18;break;case RUNWAY_HOLD:d=3;break;case TAKEOFF_ROLL:d=14;break;case ROTATE_CLIMB:d=18;break;case ORBIT:d=300;break;case APPROACH:d=48;break;case FLARE:d=9;break;case ROLLOUT:d=17;break;case TAXI_IN:d=18;break;case HANGAR_PARK:d=6;break;default:d=1;}return clamp(phaseTime/d,0,1);}
 public void update(FlightState s,FlightControls c,double dt){phaseTime+=dt;c.pitch=c.roll=c.yaw=c.brake=0;switch(phase){
  case HANGAR_START:c.throttle=.055;c.brake=phaseTime<1.8?1:.35;c.gearDown=true;groundLock(c,s,.18);if(phaseTime>=5)next(Phase.TAXI_OUT);break;
  case TAXI_OUT:c.throttle=.13;c.gearDown=true;groundLock(c,s,.20);if(s.trueAirspeedMps>9)c.brake=.24;if(phaseTime>=18)next(Phase.RUNWAY_HOLD);break;
  case RUNWAY_HOLD:c.throttle=.09;c.brake=1;c.gearDown=true;groundLock(c,s,.24);if(phaseTime>=3)next(Phase.TAKEOFF_ROLL);break;
  case TAKEOFF_ROLL:c.throttle=1;c.gearDown=true;groundLock(c,s,.28);if(s.trueAirspeedMps>=86||phaseTime>=14)next(Phase.ROTATE_CLIMB);break;
  case ROTATE_CLIMB:c.throttle=.96;c.pitch=altitudePitch(s.altitudeM,CRUISE_ALTITUDE_M,.42);c.roll=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG)*.38;c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*.012;c.gearDown=s.altitudeM<55;if(s.altitudeM>=CRUISE_ALTITUDE_M-45)next(Phase.ORBIT);break;
  case ORBIT:orbitTime+=dt;c.gearDown=false;double t=orbitTime,ta=900,th=300,b=.10,tr=.72;if(t<45){ta=980;th=300;b=.10;tr=.76;}else if(t<90){ta=1220;th=325;b=.16;tr=.74;}else if(t<145){ta=760;th=20;b=-.12;tr=.68;}else if(t<205){ta=650;th=65;b=.18;tr=.70;}else if(t<250){ta=840;th=120;b=.14;tr=.72;}else if(t<275){ta=900;th=185;b=-.10;tr=.70;}else{ta=470;th=RUNWAY_HEADING_DEG;b=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG);tr=.54;}c.throttle=tr;c.pitch=altitudePitch(s.altitudeM,ta,.18);c.roll=clamp(b+headingRoll(s.headingDeg,th)*.55,-.38,.38);if(orbitTime>=SCENIC_DURATION_SEC)next(Phase.APPROACH);break;
  case APPROACH:c.gearDown=true;double q=clamp(phaseTime/48.0,0,1),desiredAlt=470.0*(1.0-q)+15.0*q;c.throttle=s.altitudeM>220?.43:s.altitudeM>80?.35:.28;double err=headingError(s.headingDeg,RUNWAY_HEADING_DEG);c.roll=clamp(err/38.0,-.28,.28);c.yaw=clamp(err*.026,-.34,.34);c.pitch=clamp((desiredAlt-s.altitudeM)/460.0,-.13,.025);if(s.altitudeM<=15||phaseTime>=48)next(Phase.FLARE);break;
  case FLARE:c.gearDown=true;c.throttle=.14;double ferr=headingError(s.headingDeg,RUNWAY_HEADING_DEG);c.roll=clamp(ferr/65.0,-.12,.12);c.yaw=clamp(ferr*.018,-.18,.18);c.pitch=s.altitudeM>3?-.02:s.altitudeM>.5?-.004:0;if(s.onGround||s.altitudeM<=.05)next(Phase.ROLLOUT);break;
  case ROLLOUT:c.gearDown=true;c.throttle=0;c.brake=.86;groundLock(c,s,.24);if(s.trueAirspeedMps<2&&phaseTime>2)next(Phase.TAXI_IN);break;
  case TAXI_IN:c.gearDown=true;c.throttle=.10;groundLock(c,s,.20);if(s.trueAirspeedMps>8)c.brake=.22;if(phaseTime>=18)next(Phase.HANGAR_PARK);break;
  case HANGAR_PARK:c.gearDown=true;c.throttle=.035;c.brake=.78;groundLock(c,s,.22);if(phaseTime>=6)reset(s);break;
  case COMPLETE:c.gearDown=true;c.throttle=0;c.brake=1;groundLock(c,s,.25);break;}c.clamp();}
 private static void groundLock(FlightControls c,FlightState s,double k){double e=headingError(s.headingDeg,RUNWAY_HEADING_DEG);c.yaw=clamp(e*k,-.65,.65);c.roll=0;c.pitch=0;/* Demo ground phases deliberately stay wings-level and nose-on-centerline. */}
 private void next(Phase p){phase=p;phaseTime=0;}
 private static double altitudePitch(double a,double t,double m){return clamp((t-a)/700,-.14,m);}private static double headingRoll(double h,double t){return clamp(headingError(h,t)/55,-.45,.45);}private static double headingError(double h,double t){double d=t-h;while(d>180)d-=360;while(d<-180)d+=360;return d;}private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
}
