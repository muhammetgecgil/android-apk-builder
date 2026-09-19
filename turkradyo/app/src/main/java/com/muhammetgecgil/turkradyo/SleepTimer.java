package com.muhammetgecgil.turkradyo;

final class SleepTimer {
    private SleepTimer() {}
    static float effectiveVolume(float volume, long deadline, boolean fade, long now) {
        float base=Float.isFinite(volume)?Math.max(0f,Math.min(1f,volume)):1f;
        if(!fade||deadline<=0)return base;
        return base*Math.max(0f,Math.min(1f,(deadline-now)/60_000f));
    }
}
