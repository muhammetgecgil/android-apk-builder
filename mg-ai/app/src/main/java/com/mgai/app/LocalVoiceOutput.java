package com.mgai.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public final class LocalVoiceOutput {
    private static final String PREFS="mg_voice_output";
    private static final String KEY_ENABLED="enabled";
    private static TextToSpeech tts;
    private static boolean ready=false;
    private static boolean initStarted=false;
    private static String pending="";

    private LocalVoiceOutput(){}

    public static boolean enabled(Context c){
        return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(KEY_ENABLED,true);
    }

    public static void setEnabled(Context c, boolean enabled){
        c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED,enabled).apply();
        if(!enabled) stop();
    }

    public static synchronized void speak(Context context,String text){
        if(context==null || text==null || text.trim().isEmpty() || !enabled(context)) return;
        Context app=context.getApplicationContext();
        pending=text.trim();
        if(ready && tts!=null){
            doSpeak(pending);
            pending="";
            return;
        }
        if(initStarted) return;
        initStarted=true;
        tts=new TextToSpeech(app,status->{
            synchronized(LocalVoiceOutput.class){
                initStarted=false;
                if(status!=TextToSpeech.SUCCESS){ready=false;return;}
                Locale tr=new Locale("tr","TR");
                int result=tts.setLanguage(tr);
                ready=result!=TextToSpeech.LANG_MISSING_DATA && result!=TextToSpeech.LANG_NOT_SUPPORTED;
                tts.setSpeechRate(0.95f);
                tts.setPitch(1.0f);
                if(ready && !pending.isEmpty()){
                    doSpeak(pending);
                    pending="";
                }
            }
        });
    }

    public static synchronized boolean ready(){ return ready; }

    public static synchronized void stop(){
        try{ if(tts!=null) tts.stop(); }catch(Throwable ignored){}
        pending="";
    }

    public static synchronized void shutdown(){
        try{ if(tts!=null){tts.stop();tts.shutdown();} }catch(Throwable ignored){}
        tts=null;ready=false;initStarted=false;pending="";
    }

    private static void doSpeak(String text){
        if(tts==null || !ready) return;
        tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"mg-ai-response");
    }
}
