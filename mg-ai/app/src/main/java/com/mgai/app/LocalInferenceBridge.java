package com.mgai.app;

public final class LocalInferenceBridge {
    private static boolean loaded=false;
    static {
        try { System.loadLibrary("mgllama"); loaded=true; }
        catch (Throwable ignored) { loaded=false; }
    }
    private LocalInferenceBridge() {}
    public static boolean nativeAvailable(){ return loaded; }
    public static native long createEngine(String modelPath, int contextSize, int threads);
    private static native String generateNative(long handle, String prompt, int maxTokens, float temperature);
    public static String generate(long handle, String prompt, int maxTokens, float temperature){
        VoiceSessionStateManager.set(VoiceSessionStateManager.State.THINKING);
        try {
            return generateNative(handle,prompt,maxTokens,temperature);
        } finally {
            if(VoiceSessionStateManager.is(VoiceSessionStateManager.State.THINKING)) VoiceSessionStateManager.set(VoiceSessionStateManager.State.IDLE);
        }
    }
    public static native void destroyEngine(long handle);
}
