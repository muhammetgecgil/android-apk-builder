package com.mg.fixturecockpitsim;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.Random;

/** AVM-12.7 real-time procedural aircraft sound: twin-jet rumble/whine, stronger airflow and runway tyre/brake noise. */
public final class FlightSoundEngine {
    private static final int SR=24000, N=480;
    private final Random rng=new Random(73);
    private volatile boolean running;
    private volatile float throttle,speed,gear=1f,brake;
    private volatile boolean ground=true;
    private Thread thread; private AudioTrack track;
    private double phLow,phMid,phHigh; private float windLP;

    public void start(){
        if(running)return; running=true;
        int min=AudioTrack.getMinBufferSize(SR,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);
        track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(SR).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(Math.max(min,N*8)).setTransferMode(AudioTrack.MODE_STREAM).build();
        track.play(); thread=new Thread(this::loop,"FlightSound");thread.start();
    }
    public void update(double t,double v,double g,double b,boolean wow){throttle=cl((float)t,0,1);speed=Math.max(0,(float)v);gear=cl((float)g,0,1);brake=cl((float)b,0,1);ground=wow;}
    private void loop(){
        short[] out=new short[N*2];
        while(running){
            float th=throttle,sp=speed,gd=gear,br=brake;boolean wow=ground;
            double f0=40+th*82,f1=175+th*560,f2=720+th*1580;
            float eng=.16f+.38f*th;
            float wind=cl((sp-10)/185f,0,.56f);
            float tyre=wow?cl(sp/95f,0,.24f):0;
            for(int i=0;i<N;i++){
                phLow+=6.283185307*f0/SR;phMid+=6.283185307*f1/SR;phHigh+=6.283185307*f2/SR;
                float noise=rng.nextFloat()*2-1;windLP+=.065f*(noise-windLP);
                float jet=(float)(Math.sin(phLow)*.58+Math.sin(phMid)*.21+Math.sin(phHigh)*.08)*eng;
                float air=(noise*.60f+windLP*.40f)*wind*(1+.32f*gd);
                float wheel=tyre*((rng.nextFloat()*2-1)*.55f+(float)Math.sin(phLow*2.7)*.22f);
                float brakeSqueal=wow&&br>.25f&&sp>5?(float)Math.sin(phHigh*.43)*.040f*br:0;
                float s=cl(jet+air+wheel+brakeSqueal,-.92f,.92f);short q=(short)(s*32767);out[i*2]=q;out[i*2+1]=q;
            }
            track.write(out,0,out.length,AudioTrack.WRITE_BLOCKING);
        }
    }
    public void stop(){running=false;if(thread!=null){try{thread.join(250);}catch(InterruptedException ignored){}}if(track!=null){try{track.pause();track.flush();track.stop();}catch(Exception ignored){}track.release();track=null;}}
    private static float cl(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
