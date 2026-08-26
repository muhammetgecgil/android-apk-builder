package com.mg.structuralai;

/** JNI contract for Open CASCADE exact CAD import. */
public final class NativeOcctBridge {
    private static final boolean AVAILABLE;
    static {
        boolean ok=false;
        try { System.loadLibrary("structural_occt"); ok=true; }
        catch(Throwable ignored){ ok=false; }
        AVAILABLE=ok;
    }
    private NativeOcctBridge(){}
    public static boolean isAvailable(){ return AVAILABLE; }

    // Native implementation will return a handle to an XDE document preserving assemblies/metadata.
    public static native long importCadFile(String absolutePath,String format);
    public static native String assemblyTreeJson(long handle);
    public static native String metadataJson(long handle);
    public static native String validityReportJson(long handle);
    public static native String tessellateToObj(long handle,double linearDeflection,double angularDeflection);
    public static native void release(long handle);
}
