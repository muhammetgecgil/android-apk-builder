package com.mgai.app;

import android.content.Intent;

public final class LocalSpeechInput {
    public static final String ACTION_LOCAL_WHISPER="com.mgai.app.LOCAL_WHISPER";
    private LocalSpeechInput(){}

    public static Intent intent(){
        return new Intent(ACTION_LOCAL_WHISPER).setPackage("com.mgai.app");
    }
}
