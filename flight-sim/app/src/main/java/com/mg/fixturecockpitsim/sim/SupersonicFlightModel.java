package com.mg.fixturecockpitsim.sim;

/**
 * Pure-Java compressibility / propulsion schedule used by the realtime flight model.
 * It is intentionally lightweight, but all Mach-dependent systems share this one source
 * of truth: speed of sound, wave drag, transonic buffet, shock strength and target speed.
 */
public final class SupersonicFlightModel {
    public static final class Output {
        public double speedOfSoundMps;
        public double mach;
        public double afterburner01;
        public double waveDrag01;
        public double transonicBuffet01;
        public double shockStrength01;
        public double targetSpeedMps;
        public double speedResponsePerSec;
    }

    public Output evaluate(double altitudeM,double speedMps,double throttle,
                           double gearPosition,double speedBrake01,double leadingEdgeFlap01){
        Output o=new Output();
        double th=clamp(throttle,0,1),sp=Math.max(0,speedMps);
        double gear=clamp(gearPosition,0,1),brake=clamp(speedBrake01,0,1),lef=clamp(leadingEdgeFlap01,0,1);
        o.speedOfSoundMps=FighterSoundModel.speedOfSoundMps(altitudeM);
        o.mach=sp/Math.max(1.0,o.speedOfSoundMps);
        o.afterburner01=smooth(.80,.97,th);

        // Wave drag rises rapidly in the high-subsonic region, peaks around Mach 1,
        // then reduces as the flow settles into the supersonic regime.
        double rise=smooth(.78,1.00,o.mach);
        double fall=1.0-smooth(1.08,1.48,o.mach);
        o.waveDrag01=clamp(rise*fall,0,1);

        // Buffet is deliberately narrower than wave drag and strongest immediately
        // around the transonic shock movement region.
        double d=Math.abs(o.mach-.985);
        o.transonicBuffet01=d<.145?clamp(1.0-d/.145,0,1):0.0;
        o.shockStrength01=smooth(1.00,1.28,o.mach);

        // Dry power stays around the high-subsonic/low-transonic region. Afterburner
        // adds the energy required to push through the drag rise and sustain M>1.
        double dryTarget=62.0+th*300.0;
        double altitudeAssist=clamp(altitudeM/15000.0,0,1)*55.0;
        double abTarget=o.afterburner01*(300.0+altitudeAssist);
        double penalties=gear*72.0+brake*105.0+lef*14.0+o.waveDrag01*(42.0-18.0*o.afterburner01);
        o.targetSpeedMps=Math.max(45.0,dryTarget+abTarget-penalties);

        // Response is intentionally slow through the drag rise and recovers once
        // established supersonic; afterburner adds thrust response.
        o.speedResponsePerSec=.32+.34*o.afterburner01-.16*o.waveDrag01+.10*smooth(1.20,1.55,o.mach);
        o.speedResponsePerSec=clamp(o.speedResponsePerSec,.18,.82);
        return o;
    }

    public static boolean crossedMachOne(double previousMach,double currentMach){
        return previousMach<1.0&&currentMach>=1.0;
    }

    private static double smooth(double a,double b,double x){
        if(b<=a)return x>=b?1:0;
        double t=clamp((x-a)/(b-a),0,1);
        return t*t*(3.0-2.0*t);
    }

    private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
}
