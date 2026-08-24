package com.mgai.app;

import android.content.Context;
import java.io.File;

public final class LocalWhisperSession {
    private final Context app;
    private final LocalAudioRecorder recorder=new LocalAudioRecorder();
    private long whisper=0;

    public LocalWhisperSession(Context c){app=c.getApplicationContext();}

    public boolean nativeReady(){return WhisperInferenceBridge.nativeAvailable();}
    public boolean modelReady(){return WhisperModelManager.ready(app);}
    public boolean isRecording(){return recorder.isRecording();}

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
        try{
            String text=WhisperInferenceBridge.transcribe(whisper,wav.getAbsolutePath(),"tr");
            if(text==null||text.trim().isEmpty())throw new IllegalStateException("whisper_empty_transcript");
            return text.trim();
        } finally {wav.delete();}
    }

    public synchronized void close(){
        if(whisper!=0){WhisperInferenceBridge.destroy(whisper);whisper=0;}
    }
}
