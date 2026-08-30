package com.mg.fixturecockpitsim.sim;
/** Continuous demo loop: post-hangar taxi -> progressive takeoff -> varied scenic cruise -> short stabilized landing -> taxi restart. */
public final class AutonomousFlightMission {
 public enum Phase { HANGAR_START,TAXI_OUT,RUNWAY_HOLD,TAKEOFF_ROLL,ROTATE_CLIMB,ORBIT,APPROACH,FLARE,ROLLOUT,TAXI_IN,HANGAR_PARK,COMPLETE }
 public static final double SCENIC_DURATION_SEC=260.0;
 public static final double RUNWAY_HEADING_DEG=270.0;
 private static final double CRUISE_ALTITUDE_M=720.0;
 private Phase phase=Phase.TAXI_OUT;private double phaseTime,orbitTime;
 public void reset(FlightState s){phase=Phase.TAXI_OUT;phaseTime=orbitTime=0;s.timeSec=0;s.altitudeM=0;s.trueAirspeedMps=0;s.verticalSpeedMps=0;s.headingDeg=RUNWAY_HEADING_DEG;s.pitchDeg=s.rollDeg=0;s.throttle=0;s.gearPosition=1;s.brake01=1;s.onGround=true;}
 public Phase getPhase(){return phase;}public double getOrbitTimeSec(){return orbitTime;}public double getPhaseTimeSec(){return phaseTime;}
 public double getPhaseProgress01(){double d;switch(phase){case HANGAR_START:d=5;break;case TAXI_OUT:d=18;break;case RUNWAY_HOLD:d=3;break;case TAKEOFF_ROLL:d=22;break;case ROTATE_CLIMB:d=17;break;case ORBIT:d=260;break;case APPROACH:d=22;break;case FLARE:d=7;break;case ROLLOUT:d=12;break;case TAXI_IN:d=12;break;case HANGAR_PARK:d=4;break;default:d=1;}return clamp(phaseTime/d,0,1);}
 public void update(FlightState s,FlightControls c,double dt){phaseTime+=dt;c.pitch=c.roll=c.yaw=c.brake=0;switch(phase){
  case HANGAR_START:next(Phase.TAXI_OUT);break;
  case TAXI_OUT:c.throttle=.13;c.gearDown=true;groundLock(c,s,.20);if(s.trueAirspeedMps>9)c.brake=.24;if(phaseTime>=18)next(Phase.RUNWAY_HOLD);break;
  case RUNWAY_HOLD:c.throttle=.09;c.brake=1;c.gearDown=true;groundLock(c,s,.24);if(phaseTime>=3)next(Phase.TAKEOFF_ROLL);break;
  case TAKEOFF_ROLL:c.throttle=1;c.gearDown=true;groundLock(c,s,.30);if(s.trueAirspeedMps>=86||phaseTime>=22)next(Phase.ROTATE_CLIMB);break;
  case ROTATE_CLIMB:c.throttle=.94;c.pitch=altitudePitch(s.altitudeM,CRUISE_ALTITUDE_M,.36);c.roll=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG)*.28;c.yaw=headingError(s.headingDeg,RUNWAY_HEADING_DEG)*.015;c.gearDown=s.altitudeM<55;if(s.altitudeM>=CRUISE_ALTITUDE_M-35)next(Phase.ORBIT);break;
  case ORBIT:{
   orbitTime+=dt;c.gearDown=false;double t=orbitTime,ta=760,th=300,b=.08,tr=.70;
   // Scenic cruise stays scenic: altitude changes are modest and no long pre-landing descent is hidden here.
   if(t<38){ta=760;th=305;b=.08;tr=.72;}
   else if(t<76){ta=900;th=340;b=.14;tr=.72;}
   else if(t<114){ta=690;th=25;b=-.10;tr=.66;}
   else if(t<152){ta=820;th=70;b=.16;tr=.70;}
   else if(t<190){ta=740;th=120;b=.12;tr=.69;}
   else if(t<226){ta=880;th=175;b=-.08;tr=.70;}
   else if(t<248){ta=720;th=225;b=.10;tr=.64;}
   else{ta=650;th=RUNWAY_HEADING_DEG;b=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG)*.35;tr=.58;}
   c.throttle=tr;c.pitch=altitudePitch(s.altitudeM,ta,.20);c.roll=clamp(b+headingRoll(s.headingDeg,th)*.46,-.30,.30);c.yaw=clamp(headingError(s.headingDeg,th)*.011,-.20,.20);
   if(orbitTime>=SCENIC_DURATION_SEC)next(Phase.APPROACH);break;}
  case APPROACH:{
   c.gearDown=true;double q=clamp(phaseTime/22.0,0,1),desiredAlt=620.0*(1.0-q)+4.0*q;
   c.throttle=s.altitudeM>220?.31:s.altitudeM>70?.24:.18;
   double err=headingError(s.headingDeg,RUNWAY_HEADING_DEG);c.roll=clamp(err/50.0,-.20,.20);c.yaw=clamp(err*.044,-.60,.60);
   double altErr=desiredAlt-s.altitudeM;c.pitch=clamp(altErr/125.0,-.38,.025);
   if(s.altitudeM<=7.0||phaseTime>=24)next(Phase.FLARE);break;}
  case FLARE:{
   c.gearDown=true;c.throttle=s.altitudeM>2.5?.11:.07;double ferr=headingError(s.headingDeg,RUNWAY_HEADING_DEG);
   c.roll=clamp(ferr/100.0,-.07,.07);c.yaw=clamp(ferr*.055,-.65,.65);
   c.pitch=s.altitudeM>5?-.10:s.altitudeM>2?-.045:s.altitudeM>.45?.045:.018;
   if(s.onGround||s.altitudeM<=.05||phaseTime>8)next(Phase.ROLLOUT);break;}
  case ROLLOUT:c.gearDown=true;c.throttle=0;c.brake=.90;groundLock(c,s,.30);if(s.trueAirspeedMps<2&&phaseTime>1.5)next(Phase.TAXI_IN);break;
  case TAXI_IN:c.gearDown=true;c.throttle=.10;groundLock(c,s,.20);if(s.trueAirspeedMps>8)c.brake=.24;if(phaseTime>=12)next(Phase.HANGAR_PARK);break;
  case HANGAR_PARK:reset(s);break;
  case COMPLETE:c.gearDown=true;c.throttle=0;c.brake=1;groundLock(c,s,.25);break;}c.clamp();}
 private static void groundLock(FlightControls c,FlightState s,double k){double e=headingError(s.headingDeg,RUNWAY_HEADING_DEG);c.yaw=clamp(e*k,-.72,.72);c.roll=0;c.pitch=0;}
 private void next(Phase p){phase=p;phaseTime=0;}
 private static double altitudePitch(double a,double t,double m){return clamp((t-a)/650,-.18,m);}private static double headingRoll(double h,double t){return clamp(headingError(h,t)/55,-.45,.45);}private static double headingError(double h,double t){double d=t-h;while(d>180)d-=360;while(d<-180)d+=360;return d;}private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
}
