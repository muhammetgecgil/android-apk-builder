package com.mg.fixturecockpitsim;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import com.mg.fixturecockpitsim.sim.FighterSoundModel;

import java.util.Random;

/**
 * AVM-21.0 procedural twin-engine fighter audio.
 * One central engine synthesizes intake, fan/turbine whine, exhaust/afterburner,
 * airflow/transonic buffet, hydraulic motion, tyre/runway and brake layers.
 */
public final class FlightSoundEngine {
    private static final int SR=48000, N=960;
    private static final double TAU=Math.PI*2.0;
    private final Random rng=new Random(73);
    private final FighterSoundModel model=new FighterSoundModel();

    private volatile boolean running;
    private volatile float throttle,speed,altitude,gear=1f,brake;
    private volatile boolean ground=true,cockpitView;
    private volatile float stabL,stabR,rudderL,rudderR,flapL,flapR,lefL,lefR,speedBrake;
    private float prevGear=1f,prevStabL,prevStabR,prevRudderL,prevRudderR,prevFlapL,prevFlapR,prevLefL,prevLefR,prevSpeedBrake;
    private volatile float gearMotion,surfaceMotion;
    private volatile long lastUpdateNs;

    private Thread thread;
    private AudioTrack track;
    private double phCoreL,phCoreR,phFanL,phFanR,phTurbL,phTurbR,phHyd,phTyre,phBrake;
    private float spool=.08f,windLP,exhaustLP,cockpitLP;
    private float touchdownEnv,gearClunkEnv;
    private boolean previousGround=true;

    public void start(){
        if(running)return;
        int min=AudioTrack.getMinBufferSize(SR,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);
        if(min<=0)return;
        try{
            track=new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(SR).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                    .setBufferSizeInBytes(Math.max(min,N*8)).setTransferMode(AudioTrack.MODE_STREAM).build();
            track.play();running=true;thread=new Thread(this::loop,"FighterAudio");thread.start();
        }catch(Exception e){running=false;if(track!=null){try{track.release();}catch(Exception ignored){}track=null;}}
    }

    /** Backward-compatible update. */
    public void update(double t,double v,double g,double b,boolean wow){
        update(t,v,0,g,b,wow,0,0,0,0,0,0,0,0,0,false);
    }

    public void update(double t,double v,double alt,double g,double b,boolean wow,
                       double sl,double sr,double rl,double rr,double fl,double fr,double lel,double ler,double spdBrk,
                       boolean internalView){
        throttle=cl((float)t,0,1);speed=Math.max(0,(float)v);altitude=Math.max(0,(float)alt);gear=cl((float)g,0,1);brake=cl((float)b,0,1);ground=wow;cockpitView=internalView;
        stabL=(float)sl;stabR=(float)sr;rudderL=(float)rl;rudderR=(float)rr;flapL=(float)fl;flapR=(float)fr;lefL=(float)lel;lefR=(float)ler;speedBrake=(float)spdBrk;
        long now=System.nanoTime();float dt=lastUpdateNs==0?.02f:cl((now-lastUpdateNs)/1_000_000_000f,.005f,.08f);lastUpdateNs=now;
        float gm=cl(Math.abs(gear-prevGear)/(dt*.95f),0,1);
        float sm=(Math.abs(stabL-prevStabL)+Math.abs(stabR-prevStabR))/50f+
                (Math.abs(rudderL-prevRudderL)+Math.abs(rudderR-prevRudderR))/60f+
                (Math.abs(flapL-prevFlapL)+Math.abs(flapR-prevFlapR))/50f+
                (Math.abs(lefL-prevLefL)+Math.abs(lefR-prevLefR))/55f+Math.abs(speedBrake-prevSpeedBrake)/55f;
        sm=cl(sm/Math.max(.005f,dt)*.020f,0,1);
        gearMotion+=.28f*(gm-gearMotion);surfaceMotion+=.18f*(sm-surfaceMotion);
        if(Math.abs(gear-prevGear)>.002f&&((gear<.03f)||(gear>.97f)))gearClunkEnv=1f;
        prevGear=gear;prevStabL=stabL;prevStabR=stabR;prevRudderL=rudderL;prevRudderR=rudderR;prevFlapL=flapL;prevFlapR=flapR;prevLefL=lefL;prevLefR=lefR;prevSpeedBrake=speedBrake;
    }

    private void loop(){
        short[] out=new short[N*2];
        while(running){
            float th=throttle,sp=speed,alt=altitude,gd=gear,br=brake,gm=gearMotion,sm=surfaceMotion;boolean wow=ground,internal=cockpitView;
            float targetSpool=.06f+.94f*th;float spoolK=targetSpool>spool?.030f:.016f;spool+=spoolK*(targetSpool-spool);
            FighterSoundModel.Mix mx=model.evaluate(spool,sp,alt,gd,br,wow,gm,sm);
            if(!previousGround&&wow&&sp>18)touchdownEnv=cl(sp/95f,.25f,1f);previousGround=wow;

            double coreHz=43+spool*96, fanHz=235+spool*920, turbHz=930+spool*2750;
            for(int i=0;i<N;i++){
                phCoreL=wrap(phCoreL+TAU*(coreHz*.992)/SR);phCoreR=wrap(phCoreR+TAU*(coreHz*1.008)/SR);
                phFanL=wrap(phFanL+TAU*(fanHz*.996)/SR);phFanR=wrap(phFanR+TAU*(fanHz*1.004)/SR);
                phTurbL=wrap(phTurbL+TAU*(turbHz*.998)/SR);phTurbR=wrap(phTurbR+TAU*(turbHz*1.002)/SR);
                phHyd=wrap(phHyd+TAU*(190+gm*75+sm*120)/SR);phTyre=wrap(phTyre+TAU*(52+sp*4.6)/SR);phBrake=wrap(phBrake+TAU*(1450+sp*7.5)/SR);

                float noise=rng.nextFloat()*2f-1f;
                windLP+=.035f*(noise-windLP);exhaustLP+=.090f*(noise-exhaustLP);
                float highNoise=noise-windLP;
                float lowNoise=exhaustLP;

                float coreL=(float)(Math.sin(phCoreL)*.62+Math.sin(phCoreL*.51)*.22)*.24f;
                float coreR=(float)(Math.sin(phCoreR)*.62+Math.sin(phCoreR*.51)*.22)*.24f;
                float fanL=(float)(Math.sin(phFanL)+.27*Math.sin(phFanL*2.01))*((float)mx.fan);
                float fanR=(float)(Math.sin(phFanR)+.27*Math.sin(phFanR*1.99))*((float)mx.fan);
                float turbineL=(float)(Math.sin(phTurbL)+.22*Math.sin(phTurbL*1.51))*((float)mx.turbine);
                float turbineR=(float)(Math.sin(phTurbR)+.22*Math.sin(phTurbR*1.49))*((float)mx.turbine);
                float intake=(highNoise*.62f+windLP*.18f)*(float)mx.intake;
                float exhaust=(lowNoise*.72f+noise*.20f)*(float)mx.exhaust+(coreL+coreR)*.36f;
                float ab=(noise*.57f+lowNoise*.82f+(float)Math.sin(phCoreL*.37)*.22f)*(float)mx.afterburner;
                if(mx.afterburner>.10&&rng.nextFloat()>.995f)ab+=(rng.nextBoolean()?1f:-1f)*(float)mx.afterburner*.28f;
                float wind=(highNoise*.70f+windLP*.48f)*(float)mx.wind;
                float trans=(noise*.52f+lowNoise*.38f)*(float)mx.transonic;
                float tyre=(noise*.45f+(float)Math.sin(phTyre)*.18f)*(float)mx.tyre;
                float brakeTone=(float)(Math.sin(phBrake)*(0.65+.35*Math.sin(phBrake*.041)))*((float)mx.brake);
                float hydraulic=(float)(Math.sin(phHyd)+.31*Math.sin(phHyd*2.02))*((float)(mx.gearHydraulic+mx.surfaceHydraulic));
                float touchdown=(noise*.34f+(float)Math.sin(phCoreL*.18)*.66f)*touchdownEnv*.34f;
                float clunk=(float)Math.sin(phCoreL*.28)*gearClunkEnv*.16f;

                float common=intake+exhaust+ab+wind+trans+tyre+brakeTone+hydraulic+touchdown+clunk;
                float left=common+coreL+fanL*.88f+turbineL*.78f+fanR*.18f+turbineR*.12f;
                float right=common+coreR+fanR*.88f+turbineR*.78f+fanL*.18f+turbineL*.12f;

                if(internal){
                    float mono=(left+right)*.5f;cockpitLP+=.075f*(mono-cockpitLP);left=cockpitLP*.78f+left*.13f;right=cockpitLP*.78f+right*.13f;
                }
                float limiter=.74f;left=(float)Math.tanh(left*limiter);right=(float)Math.tanh(right*limiter);
                out[i*2]=(short)(cl(left,-.96f,.96f)*32767);out[i*2+1]=(short)(cl(right,-.96f,.96f)*32767);
                touchdownEnv*=.99935f;gearClunkEnv*=.9986f;
            }
            AudioTrack tr=track;if(tr!=null){try{tr.write(out,0,out.length,AudioTrack.WRITE_BLOCKING);}catch(Exception ignored){}}
        }
    }

    public void stop(){
        running=false;if(thread!=null){try{thread.join(350);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}thread=null;}
        if(track!=null){try{track.pause();track.flush();track.stop();}catch(Exception ignored){}try{track.release();}catch(Exception ignored){}track=null;}
    }

    private static double wrap(double p){return p>TAU? p-TAU : p;}
    private static float cl(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
