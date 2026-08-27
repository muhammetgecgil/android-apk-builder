package com.muhammetgecgil.turkradyo;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ProductGuard {
    private static final String PREF="product_guard_v1";
    private static volatile boolean installed=false;
    private ProductGuard(){}

    public static synchronized void install(Context c,String component){
        if(installed)return;
        installed=true;
        final Context app=c.getApplicationContext();
        final Thread.UncaughtExceptionHandler old=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t,e)->{
            try{recordCrash(app,component,t,e);}catch(Exception ignored){}
            if(old!=null)old.uncaughtException(t,e);
        });
    }

    public static void recordLaunch(Context c,String component){
        try{
            SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
            p.edit().putLong("lastLaunch",System.currentTimeMillis()).putString("lastComponent",component==null?"":component).putInt("launchCount",p.getInt("launchCount",0)+1).apply();
        }catch(Exception ignored){}
    }

    public static void recordCleanStop(Context c){
        try{c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putLong("lastCleanStop",System.currentTimeMillis()).apply();}catch(Exception ignored){}
    }

    private static void recordCrash(Context c,String component,Thread t,Throwable e){
        SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        JSONArray arr;try{arr=new JSONArray(p.getString("crashes","[]"));}catch(Exception x){arr=new JSONArray();}
        JSONArray out=new JSONArray();
        try{
            JSONObject o=new JSONObject();o.put("time",System.currentTimeMillis());o.put("component",component==null?"":component);o.put("thread",t==null?"":t.getName());o.put("type",e==null?"unknown":e.getClass().getSimpleName());o.put("message",e==null?"":String.valueOf(e.getMessage()));out.put(o);
            for(int i=0;i<arr.length()&&out.length()<20;i++)out.put(arr.opt(i));
            p.edit().putString("crashes",out.toString()).putLong("lastCrash",System.currentTimeMillis()).putInt("crashCount",p.getInt("crashCount",0)+1).apply();
        }catch(Exception ignored){}
    }

    public static String statusJson(Context c){
        try{
            SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
            JSONObject o=new JSONObject();o.put("launchCount",p.getInt("launchCount",0));o.put("crashCount",p.getInt("crashCount",0));o.put("lastLaunch",p.getLong("lastLaunch",0));o.put("lastCrash",p.getLong("lastCrash",0));o.put("lastCleanStop",p.getLong("lastCleanStop",0));o.put("crashes",new JSONArray(p.getString("crashes","[]")));return o.toString();
        }catch(Exception e){return"{}";}
    }
}
