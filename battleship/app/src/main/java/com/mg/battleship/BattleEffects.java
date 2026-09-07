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
    private static final int SR = 22050;
    private static final Random R = new Random();
    private BattleEffects() {}

    static void hit(Context c) {
        vibrate(c, 90);
        new Thread(() -> playExplosion(), "battle-hit").start();
    }

    static void miss(Context c) {
        vibrate(c, 28);
        new Thread(() -> playSplash(), "battle-splash").start();
    }

    private static void vibrate(Context c, long ms) {
        try {
            Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, 150));
            else v.vibrate(ms);
        } catch (Exception ignored) {}
    }

    private static void playExplosion() {
        int n = (int)(SR * 0.58);
        short[] pcm = new short[n];
        double phase = 0;
        double lp = 0;
        for (int i=0;i<n;i++) {
            double t = i/(double)SR;
            double env = Math.exp(-5.3*t);
            double noise = R.nextDouble()*2.0-1.0;
            lp = lp*0.84 + noise*0.16;
            phase += 2.0*Math.PI*(78.0 + 18.0*Math.exp(-8*t))/SR;
            double boom = Math.sin(phase)*0.72 + lp*0.85;
            if (t < 0.045) boom += noise*(1.0-t/0.045)*0.75;
            pcm[i]=(short)(Math.max(-1,Math.min(1,boom*env))*30000);
        }
        play(pcm);
    }

    private static void playSplash() {
        int n=(int)(SR*0.42);
        short[] pcm=new short[n];
        double lp=0, phase=0;
        for(int i=0;i<n;i++){
            double t=i/(double)SR;
            double env=Math.exp(-7.2*t);
            double noise=R.nextDouble()*2.0-1.0;
            lp=lp*0.58+noise*0.42;
            double f=520.0-330.0*Math.min(1,t/0.32);
            phase += 2*Math.PI*f/SR;
            double plop=Math.sin(phase)*0.32 + lp*0.78;
            if(t<0.018) plop += noise*0.9;
            pcm[i]=(short)(Math.max(-1,Math.min(1,plop*env))*25500);
        }
        play(pcm);
    }

    private static void play(short[] pcm) {
        try {
            AudioAttributes aa=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            AudioFormat af=new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
            AudioTrack at=new AudioTrack.Builder().setAudioAttributes(aa).setAudioFormat(af).setBufferSizeInBytes(pcm.length*2).setTransferMode(AudioTrack.MODE_STATIC).build();
            at.write(pcm,0,pcm.length); at.play();
            try { Thread.sleep((pcm.length*1000L)/SR + 60); } catch (InterruptedException ignored) {}
            at.stop(); at.release();
        } catch(Exception ignored) {}
    }
}
