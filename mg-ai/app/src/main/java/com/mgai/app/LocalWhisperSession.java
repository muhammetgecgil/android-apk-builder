package com.mgai.app;

import android.content.Context;
import java.io.File;
import java.util.Locale;

public final class LocalWhisperSession {
    public static final class Metrics {
        public final long audioMs;
        public final long transcribeMs;
        public final double rtf;
        Metrics(long audioMs,long transcribeMs){
            this.audioMs=audioMs;
            this.transcribeMs=transcribeMs;
            this.rtf=audioMs>0?((double)transcribeMs/(double)audioMs):0.0;
        }
        public String summary(){return String.format(Locale.US,"Whisper %.2f sn ses • %.0f ms STT • RTF %.2f",audioMs/1000.0,(double)transcribeMs,rtf);}
    }

    private static volatile Metrics lastMetrics=new Metrics(0,0);
    private final Context app;
    private final LocalAudioRecorder recorder=new LocalAudioRecorder();
    private long whisper=0;

    public LocalWhisperSession(Context c){app=c.getApplicationContext();}

    public boolean nativeReady(){return WhisperInferenceBridge.nativeAvailable();}
    public boolean modelReady(){return WhisperModelManager.ready(app);}
    public boolean isRecording(){return recorder.isRecording();}
    public static Metrics lastMetrics(){return lastMetrics;}

    public void ensureModel(WhisperModelManager.Listener listener){WhisperModelManager.ensure(app,listener);}

    public synchronized void start() {
        if(!nativeReady())throw new IllegalStateException("whisper_native_runtime_unavailable");
        File model=WhisperModelManager.modelFile(app);
        if(!model.isFile())throw new IllegalStateException("whisper_model_not_installed");
        if(whisper==0){whisper=WhisperInferenceBridge.create(model.getAbsolutePath());if(whisper==0)throw new IllegalStateException("whisper_model_load_failed");}
        recorder.start();
    }

    public String stopAndTranscribeTurkish() throws Exception {
        if(!recorder.isRecording())throw new IllegalStateException("not_recording");
        File wav=new File(app.getCacheDir(),"mg_ai_voice_"+System.currentTimeMillis()+".wav");
        recorder.stopToWav(wav);
        long audioBytes=Math.max(0,wav.length()-44L);
        long audioMs=(long)((audioBytes/2.0/16000.0)*1000.0);
        long started=System.nanoTime();
        try{
            String text=WhisperInferenceBridge.transcribe(whisper,wav.getAbsolutePath(),"tr");
            long transcribeMs=Math.max(0,(System.nanoTime()-started)/1_000_000L);
            lastMetrics=new Metrics(audioMs,transcribeMs);
            if(text==null||text.trim().isEmpty())throw new IllegalStateException("whisper_empty_transcript");
            return text.trim();
        } finally {wav.delete();}
    }

    public synchronized void close(){
        if(whisper!=0){WhisperInferenceBridge.destroy(whisper);whisper=0;}
    }
}
