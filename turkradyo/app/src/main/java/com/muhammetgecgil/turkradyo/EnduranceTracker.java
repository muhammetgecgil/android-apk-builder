package com.muhammetgecgil.turkradyo;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Tracks long-running playback quality for release validation. */
public final class EnduranceTracker {
    private static final String P="endurance_v1";
    private EnduranceTracker(){}
    public static void sessionStarted(Context c){SharedPreferences p=p(c);long now=System.currentTimeMillis();p.edit().putLong("sessionStart",now).putLong("lastTick",now).putLong("maxContinuousMs",Math.max(p.getLong("maxContinuousMs",0),0)).apply();}
    public static void tick(Context c,boolean playing,int buffers,int reconnects,int repairs,int transitions){SharedPreferences p=p(c);long now=System.currentTimeMillis(),start=p.getLong("sessionStart",0);if(start==0){sessionStarted(c);start=now;}long continuous=Math.max(0,now-start);SharedPreferences.Editor e=p.edit().putLong("lastTick",now).putBoolean("playing",playing).putInt("buffers",buffers).putInt("reconnects",reconnects).putInt("repairFailures",repairs).putInt("networkTransitions",transitions).putLong("maxContinuousMs",Math.max(p.getLong("maxContinuousMs",0),continuous));if(continuous>=2*60*60_000L)e.putBoolean("passed2h",true);if(continuous>=6*60*60_000L)e.putBoolean("passed6h",true);if(continuous>=10*60*60_000L)e.putBoolean("passedOvernight",true);e.apply();}
    public static void sessionInterrupted(Context c){p(c).edit().putLong("sessionStart",0).apply();}
    public static String statusJson(Context c){try{SharedPreferences p=p(c);JSONObject o=new JSONObject();o.put("sessionStart",p.getLong("sessionStart",0));o.put("lastTick",p.getLong("lastTick",0));o.put("maxContinuousMs",p.getLong("maxContinuousMs",0));o.put("passed2h",p.getBoolean("passed2h",false));o.put("passed6h",p.getBoolean("passed6h",false));o.put("passedOvernight",p.getBoolean("passedOvernight",false));o.put("buffers",p.getInt("buffers",0));o.put("reconnects",p.getInt("reconnects",0));o.put("repairFailures",p.getInt("repairFailures",0));o.put("networkTransitions",p.getInt("networkTransitions",0));return o.toString();}catch(Exception e){return"{}";}}
    private static SharedPreferences p(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}
}
