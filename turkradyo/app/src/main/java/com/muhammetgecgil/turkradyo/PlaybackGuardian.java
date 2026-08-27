package com.muhammetgecgil.turkradyo;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Persistent decision layer for safe automatic playback recovery. */
public final class PlaybackGuardian {
    private static final String P="playback_guardian_v1";
    private static final long RESUME_WINDOW_MS=2*60_000L;
    private static final int MAX_AUTO_RESUMES_PER_WINDOW=3;
    private PlaybackGuardian(){}

    public static void manualPause(Context c){
        prefs(c).edit().putBoolean("manualPause",true).putBoolean("interrupted",false)
                .putLong("changed",System.currentTimeMillis()).apply();
    }

    public static void manualPlay(Context c){
        prefs(c).edit().putBoolean("manualPause",false).putBoolean("interrupted",false)
                .putInt("windowAttempts",0).putLong("windowStart",0)
                .putLong("changed",System.currentTimeMillis()).apply();
    }

    public static void interrupted(Context c,String reason){
        SharedPreferences p=prefs(c);
        if(p.getBoolean("manualPause",false))return;
        p.edit().putBoolean("interrupted",true)
                .putString("reason",reason==null?"system":reason)
                .putLong("interruptedAt",System.currentTimeMillis()).apply();
    }

    /** Returns true only when an automatic resume is safe and not looping. */
    public static boolean mayAutoResume(Context c){
        SharedPreferences p=prefs(c);
        long now=System.currentTimeMillis();
        if(p.getBoolean("manualPause",false)||!p.getBoolean("interrupted",false))return false;
        long interruptedAt=p.getLong("interruptedAt",0);
        if(interruptedAt<=0||now-interruptedAt>=10*60_000L)return false;

        long windowStart=p.getLong("windowStart",0);
        int attempts=p.getInt("windowAttempts",0);
        if(windowStart<=0||now-windowStart>RESUME_WINDOW_MS){windowStart=now;attempts=0;}
        if(attempts>=MAX_AUTO_RESUMES_PER_WINDOW){
            p.edit().putBoolean("loopBlocked",true).putLong("loopBlockedAt",now).apply();
            return false;
        }
        p.edit().putLong("windowStart",windowStart).putInt("windowAttempts",attempts+1)
                .putInt("totalAttempts",p.getInt("totalAttempts",0)+1).putBoolean("loopBlocked",false).apply();
        return true;
    }

    public static void recovered(Context c){
        SharedPreferences p=prefs(c);
        p.edit().putBoolean("interrupted",false).putBoolean("loopBlocked",false)
                .putLong("recoveredAt",System.currentTimeMillis())
                .putInt("recoveries",p.getInt("recoveries",0)+1).apply();
    }

    public static String reason(Context c){return prefs(c).getString("reason","");}

    public static String statusJson(Context c){
        SharedPreferences p=prefs(c);
        try{
            int attempts=p.getInt("totalAttempts",0), ok=p.getInt("recoveries",0);
            JSONObject o=new JSONObject();
            o.put("enabled",true);
            o.put("manualPause",p.getBoolean("manualPause",false));
            o.put("interrupted",p.getBoolean("interrupted",false));
            o.put("reason",p.getString("reason",""));
            o.put("autoResumeAttempts",attempts);
            o.put("recoveries",ok);
            o.put("successPct",attempts>0?Math.round(ok*100f/attempts):100);
            o.put("windowAttempts",p.getInt("windowAttempts",0));
            o.put("loopBlocked",p.getBoolean("loopBlocked",false));
            o.put("loopBlockedAt",p.getLong("loopBlockedAt",0));
            o.put("lastRecoveredAt",p.getLong("recoveredAt",0));
            return o.toString();
        }catch(Exception e){return "{}";}
    }

    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}
}
