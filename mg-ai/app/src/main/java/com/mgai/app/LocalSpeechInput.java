package com.mgai.app;

import android.content.Context;
import android.content.Intent;

public final class LocalSpeechInput {
    private LocalSpeechInput(){}

    public static Intent intent(Context context){
        return new Intent(context,LocalWhisperCaptureActivity.class);
    }
}
