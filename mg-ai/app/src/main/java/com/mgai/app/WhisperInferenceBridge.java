package com.mgai.app;

public final class WhisperInferenceBridge {
    private static final boolean AVAILABLE;
    static {
        boolean ok;
        try { System.loadLibrary("mgwhisper"); ok = true; }
        catch (Throwable t) { ok = false; }
        AVAILABLE = ok;
    }
    private WhisperInferenceBridge(){}
    public static boolean nativeAvailable(){ return AVAILABLE; }
    public static native long create(String modelPath);
    public static native String transcribe(long handle, String wavPath, String language);
    public static native void destroy(long handle);
}
