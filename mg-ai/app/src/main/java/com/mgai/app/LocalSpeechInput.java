package com.mgai.app;

import android.content.Intent;
import android.speech.RecognizerIntent;

import java.util.Locale;

public final class LocalSpeechInput {
    private LocalSpeechInput(){}

    public static Intent intent(){
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"tr-TR");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"tr-TR");
        i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT,"MG-AI için konuş");
        return i;
    }
}
