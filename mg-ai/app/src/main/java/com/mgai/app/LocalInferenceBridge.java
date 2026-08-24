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
    public static native String generate(long handle, String prompt, int maxTokens, float temperature);
    public static native void destroyEngine(long handle);
}
