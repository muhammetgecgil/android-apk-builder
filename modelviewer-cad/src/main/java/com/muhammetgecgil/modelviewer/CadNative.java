package com.muhammetgecgil.modelviewer;

public final class CadNative {
    static { System.loadLibrary("mgcad"); }
    public static native boolean loadCad(String path);
    public static native float[] getTriangles();
    public static native String getModelInfo();
    public static native String autoDimensions();
    public static native void clearModel();
}
