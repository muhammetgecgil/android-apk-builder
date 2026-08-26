package com.mgecgil.seslirehber.core;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;

public final class VoiceCommandController implements RecognitionListener {
    public interface Listener { void onVoiceText(String text); void onVoiceError(String message); }
    private final SpeechRecognizer recognizer; private final Listener listener;
    public VoiceCommandController(Context context,Listener listener){this.listener=listener;recognizer=SpeechRecognizer.createSpeechRecognizer(context.getApplicationContext());recognizer.setRecognitionListener(this);}
    public void listenOnce(){Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"tr-TR").putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false).putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);recognizer.startListening(intent);}
    public void destroy(){recognizer.destroy();}
    @Override public void onResults(Bundle results){ArrayList<String> items=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(items!=null&&!items.isEmpty())listener.onVoiceText(items.get(0));else listener.onVoiceError("Ses anlaşılamadı.");}
    @Override public void onError(int error){listener.onVoiceError("Sesli komut alınamadı. Kod "+error);} @Override public void onReadyForSpeech(Bundle params){} @Override public void onBeginningOfSpeech(){} @Override public void onRmsChanged(float rmsdB){} @Override public void onBufferReceived(byte[] buffer){} @Override public void onEndOfSpeech(){} @Override public void onPartialResults(Bundle partialResults){} @Override public void onEvent(int eventType,Bundle params){}
}
