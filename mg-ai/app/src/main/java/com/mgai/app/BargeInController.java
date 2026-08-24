package com.mgai.app;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;

public final class BargeInController {
    private static final int SAMPLE_RATE=16000;
    private static volatile boolean running=false;
    private static AudioRecord record;
    private static AcousticEchoCanceler aec;
    private static NoiseSuppressor ns;
    private static Thread worker;
    private static volatile double lastBaseline=0;
    private static volatile double lastRms=0;
    private static volatile double lastThreshold=0;
    private static volatile int triggerCount=0;
    private static volatile int rejectedSpikes=0;

    private BargeInController(){}

    public static synchronized void start(Context context,Runnable onUserVoice){
        stop();
        if(context==null||onUserVoice==null||!ContinuousDialogManager.enabled(context))return;
        try{
            int min=AudioRecord.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            int size=Math.max(min,2048);
            record=new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size);
            if(record.getState()!=AudioRecord.STATE_INITIALIZED){release();return;}
            int session=record.getAudioSessionId();
            if(AcousticEchoCanceler.isAvailable()){try{aec=AcousticEchoCanceler.create(session);if(aec!=null)aec.setEnabled(true);}catch(Throwable ignored){}}
            if(NoiseSuppressor.isAvailable()){try{ns=NoiseSuppressor.create(session);if(ns!=null)ns.setEnabled(true);}catch(Throwable ignored){}}
            running=true;
            record.startRecording();
            worker=new Thread(()->monitor(size,onUserVoice),"mg-ai-barge-in");
            worker.start();
        }catch(Throwable ignored){stop();}
    }

    private static void monitor(int bufferSize,Runnable onUserVoice){
        short[] buf=new short[Math.max(512,bufferSize/2)];
        double baseline=0;
        int calibrationFrames=0;
        int hotFrames=0;
        int coolFrames=0;
        long started=System.currentTimeMillis();
        try{
            while(running&&record!=null){
                int n=record.read(buf,0,buf.length);
                if(n<=0)continue;
                double sum=0;
                int zeroCross=0;
                short prev=0;
                for(int i=0;i<n;i++){
                    short s=buf[i];
                    double v=s;sum+=v*v;
                    if(i>0&&((s>=0&&prev<0)||(s<0&&prev>=0)))zeroCross++;
                    prev=s;
                }
                double rms=Math.sqrt(sum/Math.max(1,n));
                double zcr=zeroCross/(double)Math.max(1,n);
                lastRms=rms;

                if(calibrationFrames<20){
                    baseline=(baseline*calibrationFrames+rms)/(calibrationFrames+1);
                    calibrationFrames++;
                    lastBaseline=baseline;
                    continue;
                }

                double speechLike=(zcr>0.015&&zcr<0.28)?1.0:0.0;
                double adaptAlpha=(rms<baseline*1.35)?0.025:0.004;
                baseline=baseline*(1.0-adaptAlpha)+rms*adaptAlpha;
                baseline=Math.max(120.0,Math.min(12000.0,baseline));
                double threshold=Math.max(950.0,baseline*2.05+180.0);
                lastBaseline=baseline;
                lastThreshold=threshold;

                boolean hot=rms>threshold&&speechLike>0.5;
                if(hot){hotFrames++;coolFrames=0;}
                else {coolFrames++;if(coolFrames>=2)hotFrames=Math.max(0,hotFrames-1);}

                if(rms>threshold*2.8&&hotFrames<2)rejectedSpikes++;

                if(System.currentTimeMillis()-started>450&&hotFrames>=4){
                    triggerCount++;
                    running=false;
                    try{onUserVoice.run();}catch(Throwable ignored){}
                    break;
                }
            }
        }catch(Throwable ignored){}finally{release();}
    }

    public static synchronized void stop(){
        running=false;
        try{if(record!=null)record.stop();}catch(Throwable ignored){}
        release();
    }

    private static synchronized void release(){
        try{if(aec!=null)aec.release();}catch(Throwable ignored){}
        try{if(ns!=null)ns.release();}catch(Throwable ignored){}
        try{if(record!=null)record.release();}catch(Throwable ignored){}
        aec=null;ns=null;record=null;worker=null;
    }

    public static boolean isRunning(){return running;}
    public static boolean aecActive(){try{return aec!=null&&aec.getEnabled();}catch(Throwable t){return false;}}
    public static boolean noiseSuppressorActive(){try{return ns!=null&&ns.getEnabled();}catch(Throwable t){return false;}}
    public static String diagnostics(){return "baseline="+Math.round(lastBaseline)+" rms="+Math.round(lastRms)+" threshold="+Math.round(lastThreshold)+" triggers="+triggerCount+" rejectedSpikes="+rejectedSpikes+" aec="+aecActive()+" ns="+noiseSuppressorActive();}
}
