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
        int calibration=0;
        int hotFrames=0;
        long started=System.currentTimeMillis();
        try{
            while(running&&record!=null){
                int n=record.read(buf,0,buf.length);
                if(n<=0)continue;
                double sum=0;
                for(int i=0;i<n;i++){double v=buf[i];sum+=v*v;}
                double rms=Math.sqrt(sum/Math.max(1,n));
                if(calibration<12){baseline=(baseline*calibration+rms)/(calibration+1);calibration++;continue;}
                baseline=baseline*0.985+rms*0.015;
                double threshold=Math.max(1100.0,baseline*2.35);
                if(rms>threshold)hotFrames++; else hotFrames=Math.max(0,hotFrames-1);
                if(System.currentTimeMillis()-started>300&&hotFrames>=3){
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
}
