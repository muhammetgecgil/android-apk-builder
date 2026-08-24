package com.mgai.app;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

public final class LocalVoiceOutput {
    private static final String PREFS="mg_voice_output";
    private static final String KEY_ENABLED="enabled";
    private static final String KEY_RATE="rate";
    private static final String KEY_LAST="last_text";
    private static TextToSpeech tts;
    private static boolean ready=false;
    private static boolean initStarted=false;
    private static String pending="";
    private static float currentRate=0.95f;
    private static Context appContext;

    private LocalVoiceOutput(){}

    public static boolean enabled(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(KEY_ENABLED,true);}
    public static void setEnabled(Context c,boolean enabled){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED,enabled).apply();if(!enabled){ContinuousDialogManager.suppressNextAutoListen();BargeInController.stop();stop();}}
    public static float speechRate(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getFloat(KEY_RATE,0.95f);}
    public static void setSpeechRate(Context c,float rate){float r=Math.max(0.55f,Math.min(1.55f,rate));currentRate=r;c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putFloat(KEY_RATE,r).apply();synchronized(LocalVoiceOutput.class){try{if(tts!=null)tts.setSpeechRate(r);}catch(Throwable ignored){}}}
    public static String lastText(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_LAST,"");}
    public static void repeatLast(Context c){String s=lastText(c);if(!s.trim().isEmpty())speak(c,s);}

    public static boolean handleCommand(Context c,String text){
        if(c==null||text==null)return false;
        String n=text.trim().toLowerCase(new Locale("tr","TR"));
        if(n.equals("ses aç")||n.equals("sesli cevap aç")){setEnabled(c,true);return true;}
        if(n.equals("ses kapat")||n.equals("sesli cevap kapat")){setEnabled(c,false);return true;}
        if(n.equals("tekrar oku")||n.equals("cevabı tekrar oku")){repeatLast(c);return true;}
        if(n.equals("sesi durdur")||n.equals("okumayı durdur")){ContinuousDialogManager.suppressNextAutoListen();BargeInController.stop();stop();return true;}
        if(n.startsWith("konuşma hızı ")||n.startsWith("ses hızı ")){
            String raw=n.substring(n.lastIndexOf(' ')+1).replace(',','.');
            try{setSpeechRate(c,Float.parseFloat(raw));return true;}catch(Exception ignored){}
        }
        return false;
    }

    public static synchronized void speak(Context context,String text){
        if(context==null||text==null||text.trim().isEmpty()||!enabled(context))return;
        Context app=context.getApplicationContext();appContext=app;pending=text.trim();app.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_LAST,pending).apply();currentRate=speechRate(app);
        if(ready&&tts!=null){doSpeak(pending);pending="";return;}
        if(initStarted)return;initStarted=true;
        tts=new TextToSpeech(app,status->{synchronized(LocalVoiceOutput.class){
            initStarted=false;
            if(status!=TextToSpeech.SUCCESS){ready=false;VoiceSessionStateManager.reset();return;}
            Locale tr=new Locale("tr","TR");
            int result=tts.setLanguage(tr);
            ready=result!=TextToSpeech.LANG_MISSING_DATA&&result!=TextToSpeech.LANG_NOT_SUPPORTED;
            tts.setSpeechRate(currentRate);tts.setPitch(1.0f);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                @Override public void onStart(String id){
                    VoiceSessionStateManager.set(VoiceSessionStateManager.State.SPEAKING);
                    BargeInController.start(appContext,()->interruptAndListen(appContext));
                }
                @Override public void onDone(String id){
                    BargeInController.stop();VoiceSessionStateManager.set(VoiceSessionStateManager.State.IDLE);
                    ContinuousDialogManager.onSpeechDone(appContext);
                }
                @Override public void onError(String id){BargeInController.stop();VoiceSessionStateManager.reset();}
                @Override public void onStop(String id,boolean interrupted){BargeInController.stop();if(!interrupted)VoiceSessionStateManager.reset();}
            });
            if(ready&&!pending.isEmpty()){doSpeak(pending);pending="";}
        }});
    }

    private static void interruptAndListen(Context context){
        if(context==null)return;
        VoiceSessionStateManager.set(VoiceSessionStateManager.State.BARGE_IN);
        ContinuousDialogManager.suppressNextAutoListen();
        stop();
        Intent i=LocalSpeechInput.intent();
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try{context.getApplicationContext().startActivity(i);}catch(Throwable ignored){VoiceSessionStateManager.reset();}
    }

    public static synchronized boolean ready(){return ready;}
    public static synchronized void stop(){BargeInController.stop();try{if(tts!=null)tts.stop();}catch(Throwable ignored){}pending="";if(VoiceSessionStateManager.is(VoiceSessionStateManager.State.SPEAKING))VoiceSessionStateManager.reset();}
    public static synchronized void shutdown(){BargeInController.stop();try{if(tts!=null){tts.stop();tts.shutdown();}}catch(Throwable ignored){}tts=null;ready=false;initStarted=false;pending="";appContext=null;VoiceSessionStateManager.reset();}
    private static void doSpeak(String text){if(tts==null||!ready)return;try{tts.setSpeechRate(currentRate);}catch(Throwable ignored){}tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"mg-ai-response");}
}
