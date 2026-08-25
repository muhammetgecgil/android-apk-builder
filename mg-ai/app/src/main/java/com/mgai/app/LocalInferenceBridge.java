package com.mgai.app;

public final class LocalInferenceBridge {
    public static final class Metrics {
        public final long totalMs;
        public final long ttftMs;
        public final int generatedTokens;
        public final int promptTokens;
        public final double tokensPerSecond;
        Metrics(long totalMs,long ttftMs,int generatedTokens,int promptTokens){
            this.totalMs=totalMs;this.ttftMs=ttftMs;this.generatedTokens=generatedTokens;this.promptTokens=promptTokens;
            long decodeMs=Math.max(1,totalMs-Math.max(0,ttftMs));
            this.tokensPerSecond=generatedTokens<=1?0.0:((generatedTokens-1)*1000.0/decodeMs);
        }
        public String summary(){return String.format(java.util.Locale.US,"TTFT %d ms • Toplam %d ms • %d token • %.1f tok/sn • prompt %d",ttftMs,totalMs,generatedTokens,tokensPerSecond,promptTokens);}
    }

    private static boolean loaded=false;
    private static volatile Metrics lastMetrics=new Metrics(0,0,0,0);
    static {
        try { System.loadLibrary("mgllama"); loaded=true; }
        catch (Throwable ignored) { loaded=false; }
    }
    private LocalInferenceBridge() {}
    public static boolean nativeAvailable(){ return loaded; }
    public static native long createEngine(String modelPath, int contextSize, int threads);
    private static native String generateNative(long handle, String prompt, int maxTokens, float temperature);
    private static native long lastTotalMsNative(long handle);
    private static native long lastTtftMsNative(long handle);
    private static native int lastGeneratedTokensNative(long handle);
    private static native int lastPromptTokensNative(long handle);
    public static String generate(long handle, String prompt, int maxTokens, float temperature){
        VoiceSessionStateManager.set(VoiceSessionStateManager.State.THINKING);
        try {
            String out=generateNative(handle,prompt,maxTokens,temperature);
            lastMetrics=new Metrics(lastTotalMsNative(handle),lastTtftMsNative(handle),lastGeneratedTokensNative(handle),lastPromptTokensNative(handle));
            return out;
        } catch(Throwable t){
            VoiceSessionStateManager.reportError("LLM: "+t.getMessage());
            throw t;
        } finally {
            if(VoiceSessionStateManager.is(VoiceSessionStateManager.State.THINKING)) VoiceSessionStateManager.set(VoiceSessionStateManager.State.IDLE);
        }
    }
    public static Metrics lastMetrics(){return lastMetrics;}
    public static native void destroyEngine(long handle);
}
