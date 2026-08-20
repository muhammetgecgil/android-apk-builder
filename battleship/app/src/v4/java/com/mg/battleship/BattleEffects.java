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
    private static final int SR=22050; private static final Random R=new Random(); private BattleEffects(){}
    static void hit(Context c){vibrate(c,90);new Thread(BattleEffects::playExplosion,"battle-hit").start();}
    static void miss(Context c){vibrate(c,28);new Thread(BattleEffects::playSplash,"battle-splash").start();}
    private static void vibrate(Context c,long ms){try{Vibrator v=(Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);if(v==null)return;if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(ms,150));else v.vibrate(ms);}catch(Exception ignored){}}
    private static void playExplosion(){int n=(int)(SR*.58);short[] pcm=new short[n];double phase=0,lp=0;for(int i=0;i<n;i++){double t=i/(double)SR,env=Math.exp(-5.3*t),noise=R.nextDouble()*2-1;lp=lp*.84+noise*.16;phase+=2*Math.PI*(78+18*Math.exp(-8*t))/SR;double s=Math.sin(phase)*.72+lp*.85;if(t<.045)s+=noise*(1-t/.045)*.75;pcm[i]=(short)(Math.max(-1,Math.min(1,s*env))*30000);}play(pcm);}
    private static void playSplash(){int n=(int)(SR*.42);short[] pcm=new short[n];double lp=0,phase=0;for(int i=0;i<n;i++){double t=i/(double)SR,env=Math.exp(-7.2*t),noise=R.nextDouble()*2-1;lp=lp*.58+noise*.42;double f=520-330*Math.min(1,t/.32);phase+=2*Math.PI*f/SR;double s=Math.sin(phase)*.32+lp*.78;if(t<.018)s+=noise*.9;pcm[i]=(short)(Math.max(-1,Math.min(1,s*env))*25500);}play(pcm);}
    private static void play(short[] pcm){try{AudioAttributes aa=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();AudioFormat af=new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();AudioTrack at=new AudioTrack.Builder().setAudioAttributes(aa).setAudioFormat(af).setBufferSizeInBytes(pcm.length*2).setTransferMode(AudioTrack.MODE_STATIC).build();at.write(pcm,0,pcm.length);at.play();try{Thread.sleep(pcm.length*1000L/SR+60);}catch(InterruptedException ignored){}at.stop();at.release();}catch(Exception ignored){}}
}
