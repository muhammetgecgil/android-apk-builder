package com.mg.tennistv;

public class TennisIntentEngine {
    public enum Phase { READY, UNIT_TURN, BACKSWING, ACCELERATION, CONTACT, FOLLOW_THROUGH, RECOVERY }
    public enum Intent { UNKNOWN, FOREHAND, BACKHAND, OVERHEAD, VOLLEY, LOB }

    public static class PoseSample {
        public long timeMs;
        public float bodyX, shoulderTurn;
        public float rightWristX, rightWristY, leftWristX, leftWristY;
        public float rightElbowX, rightElbowY, leftElbowX, leftElbowY;
        public float shoulderY, hipY, kneeBend, wristSpeed;
        public boolean valid;
    }

    public static class Decision {
        public final Intent intent;
        public final Phase phase;
        public final float power;
        public final float direction;
        public final float confidence;
        public Decision(Intent i, Phase p, float pow, float dir, float conf){intent=i;phase=p;power=pow;direction=dir;confidence=conf;}
    }

    private volatile PoseSample pose;
    private Phase phase=Phase.READY;
    private Intent pending=Intent.UNKNOWN;
    private long phaseTime=0L;
    private float lastWristX=0f,lastWristY=0f;
    private long lastPoseTime=0L;

    public synchronized void onPose(PoseSample p){
        if(p==null||!p.valid)return;
        if(lastPoseTime>0){
            float dt=Math.max(.016f,(p.timeMs-lastPoseTime)/1000f);
            float dx=p.rightWristX-lastWristX, dy=p.rightWristY-lastWristY;
            p.wristSpeed=(float)Math.sqrt(dx*dx+dy*dy)/dt;
        }
        lastWristX=p.rightWristX;lastWristY=p.rightWristY;lastPoseTime=p.timeMs;pose=p;
        classifyPreparation(p);
    }

    private void classifyPreparation(PoseSample p){
        long now=p.timeMs;
        boolean wristHigh=p.rightWristY < p.shoulderY-.10f || p.leftWristY < p.shoulderY-.10f;
        boolean rightBack=p.rightWristX > .60f;
        boolean leftBack=p.leftWristX < .40f;
        if(phase==Phase.READY && Math.abs(p.shoulderTurn)>.08f){phase=Phase.UNIT_TURN;phaseTime=now;}
        if((phase==Phase.UNIT_TURN||phase==Phase.READY) && (rightBack||leftBack||wristHigh)){
            phase=Phase.BACKSWING;phaseTime=now;
            if(wristHigh)pending=Intent.OVERHEAD;
            else if(rightBack)pending=Intent.FOREHAND;
            else if(leftBack)pending=Intent.BACKHAND;
        }
        if(phase==Phase.BACKSWING && p.wristSpeed>.75f){phase=Phase.ACCELERATION;phaseTime=now;}
        if((phase==Phase.FOLLOW_THROUGH||phase==Phase.CONTACT) && now-phaseTime>260){phase=Phase.RECOVERY;phaseTime=now;}
        if(phase==Phase.RECOVERY && now-phaseTime>420){phase=Phase.READY;pending=Intent.UNKNOWN;phaseTime=now;}
    }

    public synchronized Decision onSwing(float imuPower,float imuDirection,boolean raisedHit){
        long now=System.currentTimeMillis(); PoseSample p=pose;
        float conf=.55f; Intent intent=pending;
        if(raisedHit){intent=Intent.OVERHEAD;conf=.90f;}
        if(p!=null&&p.valid&&now-p.timeMs<350){
            boolean high=(p.rightWristY<p.shoulderY-.08f)||(p.leftWristY<p.shoulderY-.08f);
            if(high&&raisedHit){intent=Intent.OVERHEAD;conf=.96f;}
            else if(intent==Intent.UNKNOWN){
                if(p.rightWristX>.58f){intent=Intent.FOREHAND;conf=.78f;}
                else if(p.leftWristX<.42f){intent=Intent.BACKHAND;conf=.78f;}
                else {intent=Intent.VOLLEY;conf=.62f;}
            } else conf=Math.max(conf,.82f);
            float camDir=clamp((p.bodyX-.5f)*2f,-1f,1f);
            imuDirection=clamp(imuDirection*.72f+camDir*.28f,-1f,1f);
            float motionBoost=clamp(p.wristSpeed*.10f,0f,.28f);
            imuPower=clamp(imuPower*(1f+motionBoost),.6f,2.95f);
        }
        phase=Phase.CONTACT;phaseTime=now;
        return new Decision(intent,phase,imuPower,imuDirection,conf);
    }

    public synchronized Phase getPhase(){return phase;}
    public synchronized Intent getIntent(){return pending;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
