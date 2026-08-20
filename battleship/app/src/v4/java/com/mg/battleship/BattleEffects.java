package com.mg.battleship;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import java.util.Random;

final class BattleEffects {
    private static final int SR=44100;
    private static final Random R=new Random();
    private BattleEffects(){}

    static void hit(Context c){
        vibrate(c,110,210);
        new Thread(BattleEffects::playExplosion,"battle-explosion").start();
    }

    static void miss(Context c){
        vibrate(c,24,65);
        new Thread(BattleEffects::playSplash,"battle-water").start();
    }

    private static void vibrate(Context c,long ms,int amp){
        try{
            Vibrator v=(Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
            if(v==null)return;
            if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(ms,amp));
            else v.vibrate(ms);
        }catch(Exception ignored){}
    }

    // Tok ve derin patlama: namlu/ates sesi yok. Dusuk frekansli basinc dalgasi,
    // govde yankisi ve sonradan gelen kisa metalik kirilma katmanlari kullanilir.
    private static void playExplosion(){
        int n=(int)(SR*1.05);
        short[] pcm=new short[n];
        double low=0,veryLow=0,body=0;
        for(int i=0;i<n;i++){
            double t=i/(double)SR;
            double noise=R.nextDouble()*2-1;
            low=low*0.92+noise*0.08;
            veryLow=veryLow*0.985+noise*0.015;
            body=body*0.73+noise*0.27;
            double pressure=Math.exp(-4.0*t)*(veryLow*1.35+low*0.75);
            double rumble=Math.exp(-2.8*t)*Math.sin(2*Math.PI*(42+9*Math.exp(-3*t))*t)*0.48;
            double metal=0;
            if(t>0.055&&t<0.24){
                double mt=t-0.055;
                metal=(Math.sin(2*Math.PI*310*mt)+0.55*Math.sin(2*Math.PI*487*mt))*Math.exp(-13*mt)*0.16;
            }
            double tail=Math.exp(-5.8*t)*body*0.45;
            double s=pressure+rumble+metal+tail;
            if(t<0.010)s*=t/0.010; // keskin silah klikini engelle
            pcm[i]=(short)(Math.max(-1,Math.min(1,s))*29200);
        }
        play(pcm);
    }

    // Suya agir cisim dusmesi: once yumusak 'plop', sonra genis bantli sicrama,
    // kabarcik ve su damlacigi kuyrugu. Silah veya mekanik ates katmani yok.
    private static void playSplash(){
        int n=(int)(SR*0.95);
        short[] pcm=new short[n];
        double low=0,mid=0,high=0;
        for(int i=0;i<n;i++){
            double t=i/(double)SR;
            double noise=R.nextDouble()*2-1;
            low=low*0.94+noise*0.06;
            mid=mid*0.66+noise*0.34;
            high=noise-mid;
            double impact=Math.exp(-18*t)*(low*1.15+mid*0.18);
            double splashEnv=(1-Math.exp(-28*t))*Math.exp(-4.8*t);
            double splash=splashEnv*(mid*0.72+high*0.38);
            double bubbles=0;
            if(t>0.10){
                double bt=t-0.10;
                bubbles=(Math.sin(2*Math.PI*118*bt)+0.45*Math.sin(2*Math.PI*173*bt))*Math.exp(-5.8*bt)*0.15;
            }
            double droplets=0;
            if(t>0.15&&R.nextDouble()<0.0025){ droplets=(R.nextDouble()*2-1)*0.55*Math.exp(-3.2*t); }
            double s=impact+splash+bubbles+droplets;
            if(t<0.018)s*=t/0.018;
            pcm[i]=(short)(Math.max(-1,Math.min(1,s))*24500);
        }
        play(pcm);
    }

    private static void play(short[] pcm){
        try{
            AudioAttributes aa=new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            AudioFormat af=new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SR)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
            AudioTrack at=new AudioTrack.Builder()
                    .setAudioAttributes(aa)
                    .setAudioFormat(af)
                    .setBufferSizeInBytes(pcm.length*2)
                    .setTransferMode(AudioTrack.MODE_STATIC).build();
            at.write(pcm,0,pcm.length);
            at.play();
            try{Thread.sleep(pcm.length*1000L/SR+80);}catch(InterruptedException ignored){}
            at.stop();
            at.release();
        }catch(Exception ignored){}
    }
}
