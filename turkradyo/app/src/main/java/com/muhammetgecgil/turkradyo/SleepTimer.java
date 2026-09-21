package com.muhammetgecgil.turkradyo;

final class SleepTimer {
    private SleepTimer() {}
    static long remaining(android.content.SharedPreferences p){
        if(p.getLong("sleepDeadline",0)<=0)return 0;
        long elapsed=p.getLong("sleepElapsedDeadline",0);
        return elapsed>0?elapsed-android.os.SystemClock.elapsedRealtime():p.getLong("sleepDeadline",0)-System.currentTimeMillis();
    }
    static float effectiveVolume(float volume, long deadline, boolean fade, long now) {
        float base=Float.isFinite(volume)?Math.max(0f,Math.min(1f,volume)):1f;
        if(!fade||deadline<=0)return base;
        return base*Math.max(0f,Math.min(1f,(deadline-now)/60_000f));
    }
}
