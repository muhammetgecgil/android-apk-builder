package com.muhammetgecgil.turkradyo;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistent decision layer for safe automatic playback recovery. */
public final class PlaybackGuardian {
    private static final String P="playback_guardian_v1";
    private PlaybackGuardian(){}
    public static void manualPause(Context c){prefs(c).edit().putBoolean("manualPause",true).putBoolean("interrupted",false).putLong("changed",System.currentTimeMillis()).apply();}
    public static void manualPlay(Context c){prefs(c).edit().putBoolean("manualPause",false).putBoolean("interrupted",false).putLong("changed",System.currentTimeMillis()).apply();}
    public static void interrupted(Context c,String reason){SharedPreferences p=prefs(c);if(p.getBoolean("manualPause",false))return;p.edit().putBoolean("interrupted",true).putString("reason",reason==null?"system":reason).putLong("interruptedAt",System.currentTimeMillis()).apply();}
    public static boolean mayAutoResume(Context c){SharedPreferences p=prefs(c);if(p.getBoolean("manualPause",false)||!p.getBoolean("interrupted",false))return false;long t=p.getLong("interruptedAt",0);return t>0&&System.currentTimeMillis()-t<10*60_000L;}
    public static void recovered(Context c){prefs(c).edit().putBoolean("interrupted",false).putLong("recoveredAt",System.currentTimeMillis()).putInt("recoveries",prefs(c).getInt("recoveries",0)+1).apply();}
    public static String reason(Context c){return prefs(c).getString("reason","");}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}
}
